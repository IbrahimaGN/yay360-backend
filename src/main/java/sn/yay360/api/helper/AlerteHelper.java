package sn.yay360.api.helper;

import sn.yay360.api.dto.AlerteDto;
import sn.yay360.api.dto.DeclarerSymptomeRequest;
import sn.yay360.api.dto.PriseEnChargeRequest;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.mapper.AlerteMapper;
import sn.yay360.api.model.AlerteMedicale;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.NiveauGravite;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.StatutAlerte;
import sn.yay360.api.model.StatutGrossesse;
import sn.yay360.api.model.TypeNotification;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.AlerteMedicaleRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Détecte et gère les alertes médicales déclenchées par les symptômes déclarés (§4.7).
 * Cycle de vie strict : NOUVELLE → PRISE_EN_CHARGE → RESOLUE.
 */
@Component
@RequiredArgsConstructor
public class AlerteHelper {

    private final AlerteMedicaleRepository alerteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final GrossesseHelper grossesseHelper;
    private final NotificationHelper notificationHelper;
    private final AlerteMapper alerteMapper;
    private final DossierFamilialHelper dossierFamilialHelper;
    private final AccesDossierHelper acces;
    private final MoteurReglesCliniques moteur;

    static final String ORIGINE_PATIENTE = "PATIENTE";

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    @Transactional
    public AlerteDto declarerSymptome(String emailFemme, DeclarerSymptomeRequest request) {
        Utilisateur femme = dossierFamilialHelper.trouverFemmeAssociee(emailFemme);
        Grossesse grossesse = request.getGrossesseId() == null
                ? grossesseHelper.trouverActiveParEmail(emailFemme)
                : grossesseHelper.trouverParId(request.getGrossesseId());
        if (!dossierFamilialHelper.aAccesAuDossier(femme, grossesse.getFemmeEnceinte())) {
            throw new OperationNonAutoriseeException("Vous n'avez pas accès à ce dossier");
        }
        if (!StatutGrossesse.EN_COURS.equals(grossesse.getStatut())) {
            throw new OperationNonAutoriseeException("Cette grossesse ne permet pas de déclarer un nouveau signalement");
        }

        // Triage contextualisé : table de base ajustée selon le terme, le risque et la récurrence
        int semaine = semainesRevolues(grossesse);
        long alertesRecentes = alerteRepository.countByGrossesseIdAndDateDeclarationAfter(
                grossesse.getId(), LocalDateTime.now().minusDays(7));
        MoteurReglesCliniques.ResultatTriage triage = moteur.trierSymptome(request.getTypeSymptome(),
                semaine, Boolean.TRUE.equals(grossesse.getGrossesseARisque()), alertesRecentes);
        String niveauGravite = triage.niveau();
        String recommandation = genererRecommandation(niveauGravite);

        AlerteMedicale alerte = new AlerteMedicale();
        alerte.setGrossesse(grossesse);
        alerte.setFemmeEnceinte(grossesse.getFemmeEnceinte());
        alerte.setTypeSymptome(request.getTypeSymptome());
        alerte.setDescription(request.getDescription());
        alerte.setNiveauGravite(niveauGravite);
        alerte.setOrigine(ORIGINE_PATIENTE);
        alerte.setJustificationTriage(String.join("\n", triage.justifications()));
        alerte.setStatut(StatutAlerte.NOUVELLE);
        AlerteMedicale enregistree = alerteRepository.save(alerte);

        notificationHelper.envoyer(grossesse.getFemmeEnceinte(), grossesse, TypeNotification.ALERTE_MEDICALE,
                "Alerte enregistrée", recommandation);
        notifierSoignants(grossesse, enregistree);

        return alerteMapper.versDto(enregistree, recommandation);
    }

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    @Transactional(readOnly = true)
    public List<AlerteDto> mesAlertes(String emailFemme) {
        Utilisateur femme = dossierFamilialHelper.trouverFemmeAssociee(emailFemme);
        return versDtos(alerteRepository.findByFemmeEnceinteIdOrderByDateDeclarationDesc(femme.getId()));
    }

    /**
     * Admin : toutes les alertes du statut. Soignant : alertes des patientes qu'il suit,
     * plus les alertes NOUVELLES des patientes sans suivi (aucune urgence ne reste orpheline).
     */
    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    @Transactional(readOnly = true)
    public List<AlerteDto> listerParStatut(String email, String statut) {
        if (!List.of(StatutAlerte.NOUVELLE, StatutAlerte.PRISE_EN_CHARGE, StatutAlerte.RESOLUE).contains(statut)) {
            throw new IllegalArgumentException("Statut d'alerte inconnu : " + statut);
        }
        Utilisateur utilisateur = acces.utilisateur(email);
        if (AccesDossierHelper.estAdmin(utilisateur)) {
            return versDtos(alerteRepository.findByStatutOrderByDateDeclarationDesc(statut));
        }
        List<AlerteMedicale> alertes = new ArrayList<>(
                alerteRepository.findByStatutAndGrossesseIdInOrderByDateDeclarationDesc(statut, idsSuivies(utilisateur)));
        if (StatutAlerte.NOUVELLE.equals(statut)) {
            alerteRepository.findNouvellesSansSuivi().stream()
                    .filter(a -> !alertes.contains(a))
                    .forEach(alertes::add);
        }
        return versDtos(alertes);
    }

    /** Alertes URGENTES non résolues. */
    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    @Transactional(readOnly = true)
    public List<AlerteDto> listerUrgentes(String email) {
        Utilisateur utilisateur = acces.utilisateur(email);
        if (AccesDossierHelper.estAdmin(utilisateur)) {
            return versDtos(alerteRepository.findByNiveauGraviteAndStatutNotOrderByDateDeclarationDesc(
                    NiveauGravite.URGENT, StatutAlerte.RESOLUE));
        }
        List<AlerteMedicale> alertes = new ArrayList<>(alerteRepository
                .findByNiveauGraviteAndStatutNotAndGrossesseIdInOrderByDateDeclarationDesc(
                        NiveauGravite.URGENT, StatutAlerte.RESOLUE, idsSuivies(utilisateur)));
        alerteRepository.findNouvellesSansSuivi().stream()
                .filter(a -> NiveauGravite.URGENT.equals(a.getNiveauGravite()) && !alertes.contains(a))
                .forEach(alertes::add);
        return versDtos(alertes);
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    @Transactional
    public AlerteDto prendreEnCharge(String emailProfessionnel, Long id) {
        AlerteMedicale alerte = trouverParId(id);
        Utilisateur professionnel = acces.utilisateur(emailProfessionnel);
        if (!acces.suit(professionnel, alerte.getGrossesse()) && !acces.estSansSuivi(alerte.getGrossesse())) {
            throw new OperationNonAutoriseeException("Cette alerte concerne une patiente suivie par une autre équipe");
        }
        if (!StatutAlerte.NOUVELLE.equals(alerte.getStatut())) {
            throw new OperationNonAutoriseeException(
                    "Cette alerte n'est plus nouvelle (statut : " + alerte.getStatut() + ")");
        }

        alerte.setStatut(StatutAlerte.PRISE_EN_CHARGE);
        alerte.setProfessionnelPriseEnCharge(professionnel);
        alerte.setDatePriseEnCharge(LocalDateTime.now());
        AlerteMedicale miseAJour = alerteRepository.save(alerte);

        notificationHelper.envoyer(alerte.getFemmeEnceinte(), alerte.getGrossesse(), TypeNotification.ALERTE_MEDICALE,
                "Votre alerte est prise en charge",
                titreProfessionnel(professionnel) + " " + professionnel.getPrenom() + " " + professionnel.getNom()
                        + " a pris en charge votre alerte et vous contactera si nécessaire.");

        return alerteMapper.versDto(miseAJour, genererRecommandation(miseAJour.getNiveauGravite()));
    }

    /** Résolution : par le soignant qui a pris l'alerte en charge ou un soignant qui suit la patiente. */
    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    @Transactional
    public AlerteDto resoudre(String emailProfessionnel, Long id, PriseEnChargeRequest request) {
        AlerteMedicale alerte = trouverParId(id);
        Utilisateur professionnel = acces.utilisateur(emailProfessionnel);
        if (!StatutAlerte.PRISE_EN_CHARGE.equals(alerte.getStatut())) {
            throw new OperationNonAutoriseeException(
                    "Seule une alerte prise en charge peut être résolue (statut : " + alerte.getStatut() + ")");
        }
        boolean estPreneur = alerte.getProfessionnelPriseEnCharge() != null
                && alerte.getProfessionnelPriseEnCharge().getId().equals(professionnel.getId());
        if (!estPreneur && !acces.suit(professionnel, alerte.getGrossesse())) {
            throw new OperationNonAutoriseeException("Vous ne suivez pas cette patiente");
        }

        alerte.setStatut(StatutAlerte.RESOLUE);
        alerte.setNotesPriseEnCharge(request.getNotesPriseEnCharge().trim());
        alerte.setDateResolution(LocalDateTime.now());
        AlerteMedicale miseAJour = alerteRepository.save(alerte);

        notificationHelper.envoyer(alerte.getFemmeEnceinte(), alerte.getGrossesse(), TypeNotification.ALERTE_MEDICALE,
                "Votre alerte a été traitée",
                "Votre signalement a été traité par l'équipe soignante. N'hésitez pas à signaler tout nouveau symptôme.");

        return alerteMapper.versDto(miseAJour, genererRecommandation(miseAJour.getNiveauGravite()));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public AlerteDto obtenirParId(String email, Long id) {
        AlerteMedicale alerte = trouverParId(id);
        Utilisateur utilisateur = acces.utilisateur(email);
        boolean alerteOrpheline = AccesDossierHelper.estProfessionnel(utilisateur)
                && StatutAlerte.NOUVELLE.equals(alerte.getStatut())
                && acces.estSansSuivi(alerte.getGrossesse());
        if (!alerteOrpheline) {
            acces.verifierLecture(utilisateur, alerte.getGrossesse());
        }
        return alerteMapper.versDto(alerte, genererRecommandation(alerte.getNiveauGravite()));
    }

    // --- internes ---

    /**
     * Notifie les soignants qui suivent la patiente (avec ses coordonnées). Si personne ne la suit,
     * tous les soignants actifs reçoivent une alerte SANS donnée identifiante (minimisation des
     * données de santé) et peuvent la prendre en charge.
     */
    private void notifierSoignants(Grossesse grossesse, AlerteMedicale alerte) {
        String niveau = alerte.getNiveauGravite();
        Utilisateur femme = grossesse.getFemmeEnceinte();
        int semaine = semainesRevolues(grossesse);
        List<Utilisateur> soignants = utilisateurRepository.findSoignantsSuivant(grossesse.getId());

        if (!soignants.isEmpty()) {
            String titre = (NiveauGravite.URGENT.equals(niveau) ? "⚠ Alerte URGENTE — " : "Nouvelle alerte — ")
                    + femme.getPrenom() + " " + femme.getNom();
            String message = "Symptôme : " + alerte.getTypeSymptome() + " (gravité " + niveau + "), "
                    + semaine + " SA." + (femme.getTelephone() != null ? " Tél : " + femme.getTelephone() : "");
            soignants.forEach(s -> notificationHelper.envoyerSansCopieConjoint(
                    s, grossesse, TypeNotification.ALERTE_MEDICALE, titre, message));
            return;
        }

        String titre = (NiveauGravite.URGENT.equals(niveau) ? "⚠ Alerte URGENTE" : "Nouvelle alerte")
                + " — patiente sans suivi";
        String message = "Symptôme : " + alerte.getTypeSymptome() + " (gravité " + niveau + "), " + semaine
                + " SA. Aucune équipe ne suit cette patiente : ouvrez l'alerte n°" + alerte.getId()
                + " pour la prendre en charge.";
        utilisateurRepository.findByRoleInAndActifTrue(List.of(RoleUtilisateur.MEDECIN, RoleUtilisateur.SAGE_FEMME))
                .forEach(s -> notificationHelper.envoyerSansCopieConjoint(
                        s, grossesse, TypeNotification.ALERTE_MEDICALE, titre, message));
    }

    /** Semaines d'aménorrhée révolues (SA), unité des règles cliniques. */
    private int semainesRevolues(Grossesse grossesse) {
        return grossesseHelper.versDtoCalcule(grossesse).getAgeGestationnelJours() / 7;
    }

    private List<Long> idsSuivies(Utilisateur professionnel) {
        List<Long> ids = grossesseHelper.grossessesVisibles(professionnel).stream().map(Grossesse::getId).toList();
        return ids.isEmpty() ? List.of(-1L) : ids;
    }

    private List<AlerteDto> versDtos(List<AlerteMedicale> alertes) {
        return alertes.stream()
                .map(a -> alerteMapper.versDto(a, genererRecommandation(a.getNiveauGravite())))
                .toList();
    }

    private static String titreProfessionnel(Utilisateur professionnel) {
        return RoleUtilisateur.MEDECIN.equals(professionnel.getRole()) ? "Dr" : "La sage-femme";
    }

    static String genererRecommandation(String niveauGravite) {
        return switch (niveauGravite) {
            case NiveauGravite.URGENT -> "Consultez en urgence un professionnel de santé ou rendez-vous immédiatement à l'établissement de santé le plus proche.";
            case NiveauGravite.ELEVE -> "Prenez rapidement contact avec votre professionnel de santé pour un avis médical.";
            default -> "Surveillez l'évolution de ce symptôme et signalez tout changement à votre professionnel de santé.";
        };
    }

    private AlerteMedicale trouverParId(Long id) {
        return alerteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte introuvable avec l'id : " + id));
    }
}
