package sn.yay360.api.helper;

import sn.yay360.api.dto.AnnulerRendezVousRequest;
import sn.yay360.api.dto.ChangerStatutRendezVousRequest;
import sn.yay360.api.dto.ModifierRendezVousRequest;
import sn.yay360.api.dto.PlanifierRendezVousRequest;
import sn.yay360.api.dto.PrendreRendezVousRequest;
import sn.yay360.api.dto.RendezVousDto;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.mapper.RendezVousMapper;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.RendezVous;
import sn.yay360.api.model.StatutRendezVous;
import sn.yay360.api.model.TypeNotification;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.RendezVousRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contient toute la logique métier liée à la prise et au suivi des rendez-vous (§4.6).
 */
@Component
@RequiredArgsConstructor
public class RendezVousHelper {

    static final List<String> STATUTS_ACTIFS = List.of(StatutRendezVous.PLANIFIE, StatutRendezVous.CONFIRME);

    /** Transitions autorisées : ANNULE, TERMINE et ABSENT sont des états finaux. */
    static final Map<String, Set<String>> TRANSITIONS = Map.of(
            StatutRendezVous.PLANIFIE, Set.of(StatutRendezVous.CONFIRME, StatutRendezVous.ANNULE,
                    StatutRendezVous.TERMINE, StatutRendezVous.ABSENT),
            StatutRendezVous.CONFIRME, Set.of(StatutRendezVous.ANNULE, StatutRendezVous.TERMINE,
                    StatutRendezVous.ABSENT)
    );

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm");

    private final RendezVousRepository rendezVousRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final GrossesseHelper grossesseHelper;
    private final RendezVousMapper rendezVousMapper;
    private final DossierFamilialHelper dossierFamilialHelper;
    private final AccesDossierHelper acces;
    private final NotificationHelper notificationHelper;

    // ==================== PATIENTE / CONJOINT ====================

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    @Transactional
    public RendezVousDto prendre(String emailFemme, PrendreRendezVousRequest request) {
        Grossesse grossesse = request.getGrossesseId() == null
                ? grossesseHelper.trouverActiveParEmail(emailFemme)
                : grossesseHelper.trouverParId(request.getGrossesseId());
        Utilisateur demandeur = acces.utilisateur(emailFemme);
        if (!acces.estFamille(demandeur, grossesse)) {
            throw new OperationNonAutoriseeException("Vous n'avez pas accès à ce dossier");
        }
        GrossesseHelper.verifierEnCours(grossesse);

        EtablissementSante etablissement = request.getEtablissementId() != null
                ? grossesseHelper.trouverEtablissementActif(request.getEtablissementId())
                : grossesse.getEtablissementSuivi();
        Utilisateur professionnel = null;
        if (request.getProfessionnelId() != null) {
            professionnel = trouverSoignantActif(request.getProfessionnelId());
            if (etablissement != null && !acces.estMembre(professionnel, etablissement.getId())) {
                throw new OperationNonAutoriseeException("Ce professionnel n'exerce pas dans l'établissement choisi");
            }
            verifierDisponibilite(professionnel, request.getDateHeure(), null);
        }

        // Premier rendez-vous dans un établissement : il devient l'établissement de suivi
        if (grossesse.getEtablissementSuivi() == null && etablissement != null) {
            grossesse.setEtablissementSuivi(etablissement);
        }

        RendezVous rdv = new RendezVous();
        rdv.setGrossesse(grossesse);
        rdv.setFemmeEnceinte(grossesse.getFemmeEnceinte());
        rdv.setDateHeure(request.getDateHeure());
        rdv.setType(request.getType());
        rdv.setMotif(request.getMotif());
        rdv.setStatut(StatutRendezVous.PLANIFIE);
        rdv.setProfessionnel(professionnel);
        rdv.setEtablissement(etablissement);

        return rendezVousMapper.versDto(rendezVousRepository.save(rdv));
    }

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    @Transactional(readOnly = true)
    public List<RendezVousDto> mesRendezVous(String emailFemme) {
        Utilisateur femme = dossierFamilialHelper.trouverFemmeAssociee(emailFemme);
        return versDtos(rendezVousRepository.findByFemmeEnceinteIdOrderByDateHeureDesc(femme.getId()));
    }

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    @Transactional(readOnly = true)
    public List<RendezVousDto> mesRendezVousAVenir(String emailFemme) {
        Utilisateur femme = dossierFamilialHelper.trouverFemmeAssociee(emailFemme);
        return versDtos(rendezVousRepository
                .findByFemmeEnceinteIdAndDateHeureAfterOrderByDateHeureAsc(femme.getId(), LocalDateTime.now()));
    }

    // ==================== PROFESSIONNELS ====================

    /** Un soignant planifie un rendez-vous pour une patiente qu'il suit (§3 : « planifie »). */
    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    @Transactional
    public RendezVousDto planifier(String emailProfessionnel, PlanifierRendezVousRequest request) {
        Utilisateur professionnel = acces.utilisateur(emailProfessionnel);
        Grossesse grossesse = grossesseHelper.trouverParId(request.getGrossesseId());
        acces.verifierSoin(professionnel, grossesse);
        GrossesseHelper.verifierEnCours(grossesse);
        verifierDisponibilite(professionnel, request.getDateHeure(), null);

        EtablissementSante etablissement = request.getEtablissementId() != null
                ? grossesseHelper.trouverEtablissementActif(request.getEtablissementId())
                : grossesse.getEtablissementSuivi();
        if (etablissement != null && !acces.estMembre(professionnel, etablissement.getId())) {
            throw new OperationNonAutoriseeException("Vous n'exercez pas dans cet établissement");
        }

        RendezVous rdv = new RendezVous();
        rdv.setGrossesse(grossesse);
        rdv.setFemmeEnceinte(grossesse.getFemmeEnceinte());
        rdv.setProfessionnel(professionnel);
        rdv.setEtablissement(etablissement);
        rdv.setDateHeure(request.getDateHeure());
        rdv.setType(request.getType());
        rdv.setMotif(request.getMotif());
        rdv.setStatut(StatutRendezVous.CONFIRME);
        RendezVous enregistre = rendezVousRepository.save(rdv);

        notificationHelper.envoyer(grossesse.getFemmeEnceinte(), grossesse, TypeNotification.RAPPEL_CONSULTATION,
                "Nouveau rendez-vous le " + request.getDateHeure().format(FORMAT),
                "Un rendez-vous (" + request.getType() + ") a été planifié pour vous"
                        + (etablissement != null ? " à " + etablissement.getNom() : "") + ".");
        return rendezVousMapper.versDto(enregistre);
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    @Transactional(readOnly = true)
    public List<RendezVousDto> rendezVousDuProfessionnel(String emailProfessionnel) {
        Utilisateur professionnel = acces.utilisateur(emailProfessionnel);
        return versDtos(rendezVousRepository.findByProfessionnelIdOrderByDateHeureAsc(professionnel.getId()));
    }

    /** Admin : tous (supervision). Soignant : rendez-vous des patientes qu'il suit. */
    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    @Transactional(readOnly = true)
    public List<RendezVousDto> listerParStatut(String email, String statut) {
        if (!TRANSITIONS.containsKey(statut) && !List.of(StatutRendezVous.ANNULE, StatutRendezVous.TERMINE,
                StatutRendezVous.ABSENT).contains(statut)) {
            throw new IllegalArgumentException("Statut de rendez-vous inconnu : " + statut);
        }
        Utilisateur utilisateur = acces.utilisateur(email);
        if (AccesDossierHelper.estAdmin(utilisateur)) {
            return versDtos(rendezVousRepository.findByStatutOrderByDateHeureAsc(statut));
        }
        List<Long> ids = new ArrayList<>(grossesseHelper.grossessesVisibles(utilisateur).stream()
                .map(Grossesse::getId).toList());
        if (ids.isEmpty()) {
            ids.add(-1L);
        }
        return versDtos(rendezVousRepository.findByStatutAndGrossesseIdInOrderByDateHeureAsc(statut, ids));
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    @Transactional(readOnly = true)
    public List<RendezVousDto> listerParGrossesse(String email, Long grossesseId) {
        Grossesse grossesse = grossesseHelper.trouverParId(grossesseId);
        acces.verifierLecture(acces.utilisateur(email), grossesse);
        return versDtos(rendezVousRepository.findByGrossesseIdOrderByDateHeureDesc(grossesseId));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public RendezVousDto obtenirParId(String email, Long id) {
        RendezVous rdv = trouverParId(id);
        acces.verifierLecture(acces.utilisateur(email), rdv.getGrossesse());
        return rendezVousMapper.versDto(rdv);
    }

    /** Report : par la patiente, son conjoint ou un soignant qui la suit. */
    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT', 'MEDECIN', 'SAGE_FEMME')")
    @Transactional
    public RendezVousDto modifier(String email, Long id, ModifierRendezVousRequest request) {
        RendezVous rdv = trouverParId(id);
        Utilisateur utilisateur = acces.utilisateur(email);
        verifierEcriture(utilisateur, rdv);
        verifierModifiable(rdv);
        if (rdv.getProfessionnel() != null) {
            verifierDisponibilite(rdv.getProfessionnel(), request.getDateHeure(), rdv.getId());
        }

        rdv.setDateHeure(request.getDateHeure());
        rdv.setStatut(StatutRendezVous.PLANIFIE); // un report doit être reconfirmé
        rdv.setRappelEnvoye(false);
        if (request.getMotif() != null) {
            rdv.setMotif(request.getMotif());
        }
        RendezVous enregistre = rendezVousRepository.save(rdv);
        if (AccesDossierHelper.estProfessionnel(utilisateur)) {
            notifierPatiente(rdv, "Rendez-vous reporté au " + request.getDateHeure().format(FORMAT),
                    "Votre rendez-vous a été déplacé par l'équipe soignante.");
        }
        return rendezVousMapper.versDto(enregistre);
    }

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT', 'MEDECIN', 'SAGE_FEMME')")
    @Transactional
    public RendezVousDto annuler(String email, Long id, AnnulerRendezVousRequest request) {
        RendezVous rdv = trouverParId(id);
        Utilisateur utilisateur = acces.utilisateur(email);
        verifierEcriture(utilisateur, rdv);
        verifierModifiable(rdv);

        rdv.setStatut(StatutRendezVous.ANNULE);
        rdv.setNotesAnnulation(request != null ? request.getNotesAnnulation() : null);
        RendezVous enregistre = rendezVousRepository.save(rdv);
        if (AccesDossierHelper.estProfessionnel(utilisateur)) {
            notifierPatiente(rdv, "Rendez-vous du " + rdv.getDateHeure().format(FORMAT) + " annulé",
                    "Votre rendez-vous a été annulé par l'équipe soignante. Un nouveau rendez-vous vous sera proposé.");
        }
        return rendezVousMapper.versDto(enregistre);
    }

    /** Changement de statut par un soignant qui suit la patiente, selon la matrice de transitions. */
    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    @Transactional
    public RendezVousDto changerStatut(String emailProfessionnel, Long id, ChangerStatutRendezVousRequest request) {
        RendezVous rdv = trouverParId(id);
        acces.verifierSoin(acces.utilisateur(emailProfessionnel), rdv.getGrossesse());

        String nouveau = request.getStatut();
        if (!TRANSITIONS.getOrDefault(rdv.getStatut(), Set.of()).contains(nouveau)) {
            throw new OperationNonAutoriseeException(
                    "Transition interdite : " + rdv.getStatut() + " → " + nouveau);
        }
        boolean constatApresRdv = StatutRendezVous.TERMINE.equals(nouveau) || StatutRendezVous.ABSENT.equals(nouveau);
        if (constatApresRdv && rdv.getDateHeure().isAfter(LocalDateTime.now())) {
            throw new OperationNonAutoriseeException("Un rendez-vous futur ne peut pas être marqué " + nouveau);
        }
        rdv.setStatut(nouveau);
        return rendezVousMapper.versDto(rendezVousRepository.save(rdv));
    }

    // --- internes ---

    RendezVous trouverParId(Long id) {
        return rendezVousRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable avec l'id : " + id));
    }

    private void verifierEcriture(Utilisateur utilisateur, RendezVous rdv) {
        if (acces.estFamille(utilisateur, rdv.getGrossesse()) || acces.suit(utilisateur, rdv.getGrossesse())) {
            return;
        }
        throw new OperationNonAutoriseeException("Vous n'avez pas accès à ce rendez-vous");
    }

    private void verifierModifiable(RendezVous rdv) {
        if (!STATUTS_ACTIFS.contains(rdv.getStatut())) {
            throw new OperationNonAutoriseeException(
                    "Ce rendez-vous ne peut plus être modifié (statut : " + rdv.getStatut() + ")");
        }
    }

    private void verifierDisponibilite(Utilisateur professionnel, LocalDateTime dateHeure, Long rdvIgnore) {
        boolean occupe = rendezVousRepository.existsByProfessionnelIdAndDateHeureAndStatutIn(
                professionnel.getId(), dateHeure, STATUTS_ACTIFS);
        if (occupe && (rdvIgnore == null || !trouverParId(rdvIgnore).getDateHeure().equals(dateHeure))) {
            throw new OperationNonAutoriseeException("Ce professionnel a déjà un rendez-vous à cette heure");
        }
    }

    private Utilisateur trouverSoignantActif(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professionnel introuvable avec l'id : " + id));
        if (!AccesDossierHelper.estProfessionnel(utilisateur) || !Boolean.TRUE.equals(utilisateur.getActif())) {
            throw new OperationNonAutoriseeException("Le professionnel choisi doit être un médecin ou une sage-femme actif");
        }
        return utilisateur;
    }

    private void notifierPatiente(RendezVous rdv, String titre, String message) {
        notificationHelper.envoyer(rdv.getFemmeEnceinte(), rdv.getGrossesse(), TypeNotification.RAPPEL_CONSULTATION,
                titre, message);
    }

    private List<RendezVousDto> versDtos(List<RendezVous> rendezVous) {
        return rendezVous.stream().map(rendezVousMapper::versDto).toList();
    }
}
