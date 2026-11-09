package sn.yay360.api.helper;

import sn.yay360.api.dto.ConsultationDto;
import sn.yay360.api.dto.EnregistrerConsultationRequest;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.mapper.ConsultationMapper;
import sn.yay360.api.model.Consultation;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.RendezVous;
import sn.yay360.api.model.StatutRendezVous;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.model.AlerteMedicale;
import sn.yay360.api.model.NiveauGravite;
import sn.yay360.api.model.StatutAlerte;
import sn.yay360.api.model.TypeNotification;
import sn.yay360.api.repo.AlerteMedicaleRepository;
import sn.yay360.api.repo.ConsultationRepository;
import sn.yay360.api.repo.RendezVousRepository;
import sn.yay360.api.repo.UtilisateurRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Contient toute la logique métier liée à l'enregistrement des consultations prénatales (§4.5).
 */
@Component
@RequiredArgsConstructor
public class ConsultationHelper {

    private final ConsultationRepository consultationRepository;
    private final RendezVousRepository rendezVousRepository;
    private final GrossesseHelper grossesseHelper;
    private final ConsultationMapper consultationMapper;
    private final DossierFamilialHelper dossierFamilialHelper;
    private final AccesDossierHelper acces;
    private final MoteurReglesCliniques moteur;
    private final CalculObstetricalHelper calcul;
    private final AlerteMedicaleRepository alerteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationHelper notificationHelper;

    static final String TYPE_CONSTANTES_ANORMALES = "CONSTANTES_ANORMALES";
    static final String ORIGINE_ANALYSE_AUTOMATIQUE = "ANALYSE_AUTOMATIQUE";

    /**
     * Enregistre une consultation. Le soignant doit suivre la patiente, la grossesse doit être en
     * cours, et le rendez-vous éventuel doit appartenir à cette grossesse et être encore actif :
     * il passe alors automatiquement à TERMINE (principe de compensation).
     */
    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    @Transactional
    public ConsultationDto enregistrer(String emailProfessionnel, EnregistrerConsultationRequest request) {
        Utilisateur professionnel = acces.utilisateur(emailProfessionnel);
        Grossesse grossesse = grossesseHelper.trouverParId(request.getGrossesseId());
        acces.verifierSoin(professionnel, grossesse);
        GrossesseHelper.verifierEnCours(grossesse);

        RendezVous rdv = null;
        if (request.getRendezVousId() != null) {
            rdv = rendezVousRepository.findById(request.getRendezVousId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Rendez-vous introuvable avec l'id : " + request.getRendezVousId()));
            if (!rdv.getGrossesse().getId().equals(grossesse.getId())) {
                throw new OperationNonAutoriseeException("Ce rendez-vous ne concerne pas cette grossesse");
            }
            if (!RendezVousHelper.STATUTS_ACTIFS.contains(rdv.getStatut())) {
                throw new OperationNonAutoriseeException(
                        "Ce rendez-vous ne peut pas être honoré (statut : " + rdv.getStatut() + ")");
            }
        }

        EtablissementSante etablissement = null;
        if (request.getEtablissementId() != null) {
            etablissement = grossesseHelper.trouverEtablissementActif(request.getEtablissementId());
            if (!acces.estMembre(professionnel, etablissement.getId())) {
                throw new OperationNonAutoriseeException("Vous n'exercez pas dans cet établissement");
            }
        } else if (rdv != null) {
            etablissement = rdv.getEtablissement();
        }

        Consultation consultation = new Consultation();
        consultation.setGrossesse(grossesse);
        consultation.setFemmeEnceinte(grossesse.getFemmeEnceinte());
        consultation.setProfessionnel(professionnel);
        consultation.setRendezVous(rdv);
        consultation.setEtablissement(etablissement);
        consultation.setPoidsKg(request.getPoidsKg());
        consultation.setTensionArterielle(request.getTensionArterielle());
        consultation.setTemperatureC(request.getTemperatureC());
        consultation.setHauteurUterineCm(request.getHauteurUterineCm());
        consultation.setBattementsCoeurFoetal(request.getBattementsCoeurFoetal());
        consultation.setFrequenceCardiaqueMaternelle(request.getFrequenceCardiaqueMaternelle());
        consultation.setProteinurie(request.getProteinurie());
        consultation.setGlycosurie(request.getGlycosurie());
        consultation.setObservations(request.getObservations());
        consultation.setDiagnostic(request.getDiagnostic());
        consultation.setTraitement(request.getTraitement());
        consultation.setPrescriptions(request.getPrescriptions());

        if (rdv != null) {
            rdv.setStatut(StatutRendezVous.TERMINE);
            rendezVousRepository.save(rdv);
        }
        if (grossesse.getEtablissementSuivi() == null && etablissement != null) {
            grossesse.setEtablissementSuivi(etablissement);
        }

        // Analyse automatique des constantes par le moteur de règles cliniques
        List<MoteurReglesCliniques.SignalClinique> signaux = analyserConstantes(grossesse, request);
        if (!signaux.isEmpty()) {
            consultation.setSignauxCliniques(String.join("\n", signaux.stream()
                    .map(s -> s.code() + "|" + s.niveau() + "|" + s).toList()));
        }
        Consultation enregistree = consultationRepository.save(consultation);
        if (!signaux.isEmpty()) {
            tracerAlerteAutomatique(grossesse, professionnel, signaux);
        }
        return consultationMapper.versDto(enregistree);
    }

    private List<MoteurReglesCliniques.SignalClinique> analyserConstantes(Grossesse grossesse,
                                                                         EnregistrerConsultationRequest request) {
        // Semaines d'aménorrhée révolues : unité des seuils cliniques (20 SA, 24–38 SA…)
        int semaine = calcul.calculerAgeGestationnelJours(grossesse.getDateDernieresRegles()) / 7;
        Consultation precedente = consultationRepository
                .findFirstByGrossesseIdOrderByDateConsultationDesc(grossesse.getId()).orElse(null);
        Double poidsPrecedent = precedente != null ? precedente.getPoidsKg() : null;
        Long jours = precedente != null
                ? ChronoUnit.DAYS.between(precedente.getDateConsultation().toLocalDate(), LocalDate.now())
                : null;
        List<MoteurReglesCliniques.SignalClinique> signaux = new java.util.ArrayList<>(
                moteur.analyserConstantes(semaine, request.getTensionArterielle(), request.getTemperatureC(),
                        request.getBattementsCoeurFoetal(), request.getHauteurUterineCm(), request.getPoidsKg(),
                        poidsPrecedent, jours));
        signaux.addAll(moteur.analyserConstantesComplementaires(semaine, request.getTensionArterielle(),
                request.getFrequenceCardiaqueMaternelle(), request.getProteinurie(), request.getGlycosurie()));
        return signaux;
    }

    /**
     * Trace les constantes anormales sous forme d'alerte déjà prise en charge par le soignant présent
     * (historique, tableau de bord). Si un signal est URGENT, le reste de l'équipe est prévenu.
     */
    private void tracerAlerteAutomatique(Grossesse grossesse, Utilisateur professionnel,
                                         List<MoteurReglesCliniques.SignalClinique> signaux) {
        String niveau = MoteurReglesCliniques.niveauMax(signaux);
        AlerteMedicale alerte = new AlerteMedicale();
        alerte.setGrossesse(grossesse);
        alerte.setFemmeEnceinte(grossesse.getFemmeEnceinte());
        alerte.setTypeSymptome(TYPE_CONSTANTES_ANORMALES);
        alerte.setDescription("Constantes anormales détectées lors de la consultation.");
        alerte.setNiveauGravite(niveau);
        alerte.setOrigine(ORIGINE_ANALYSE_AUTOMATIQUE);
        alerte.setJustificationTriage(String.join("\n", signaux.stream().map(Object::toString).toList()));
        alerte.setStatut(StatutAlerte.PRISE_EN_CHARGE);
        alerte.setProfessionnelPriseEnCharge(professionnel);
        alerte.setDatePriseEnCharge(LocalDateTime.now());
        alerteRepository.save(alerte);

        if (NiveauGravite.URGENT.equals(niveau)) {
            Utilisateur femme = grossesse.getFemmeEnceinte();
            utilisateurRepository.findSoignantsSuivant(grossesse.getId()).stream()
                    .filter(s -> !s.getId().equals(professionnel.getId()))
                    .forEach(s -> notificationHelper.envoyerSansCopieConjoint(s, grossesse,
                            TypeNotification.ALERTE_MEDICALE,
                            "⚠ Constantes anormales — " + femme.getPrenom() + " " + femme.getNom(),
                            String.join(" ", signaux.stream().map(MoteurReglesCliniques.SignalClinique::message).toList())));
        }
    }

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    @Transactional(readOnly = true)
    public List<ConsultationDto> mesConsultations(String emailFemme) {
        Utilisateur femme = dossierFamilialHelper.trouverFemmeAssociee(emailFemme);
        return consultationRepository.findByFemmeEnceinteIdOrderByDateConsultationDesc(femme.getId()).stream()
                .map(consultationMapper::versDto)
                .toList();
    }

    /** Contenu clinique : réservé aux soignants qui suivent la patiente (pas à l'admin). */
    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    @Transactional(readOnly = true)
    public List<ConsultationDto> parGrossesse(String email, Long grossesseId) {
        Grossesse grossesse = grossesseHelper.trouverParId(grossesseId);
        acces.verifierLectureClinique(acces.utilisateur(email), grossesse);
        return consultationRepository.findByGrossesseIdOrderByDateConsultationDesc(grossesseId).stream()
                .map(consultationMapper::versDto)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    @Transactional(readOnly = true)
    public List<ConsultationDto> mesConsultationsProfessionnel(String emailProfessionnel) {
        Utilisateur professionnel = acces.utilisateur(emailProfessionnel);
        return consultationRepository.findByProfessionnelIdOrderByDateConsultationDesc(professionnel.getId()).stream()
                .map(consultationMapper::versDto)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT', 'MEDECIN', 'SAGE_FEMME')")
    @Transactional(readOnly = true)
    public ConsultationDto obtenirParId(String email, Long id) {
        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation introuvable avec l'id : " + id));
        acces.verifierLectureClinique(acces.utilisateur(email), consultation.getGrossesse());
        return consultationMapper.versDto(consultation);
    }
}
