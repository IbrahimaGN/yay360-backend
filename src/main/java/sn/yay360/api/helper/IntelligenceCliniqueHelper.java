package sn.yay360.api.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.yay360.api.dto.EvaluationRisqueDto;
import sn.yay360.api.dto.PlanCpnDto;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.NiveauGravite;
import sn.yay360.api.repo.AlerteMedicaleRepository;
import sn.yay360.api.repo.ConsultationRepository;
import sn.yay360.api.repo.RendezVousRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Expose les services d'aide à la décision du moteur de règles cliniques :
 * plan des consultations prénatales (OMS) et évaluation des facteurs de risque.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntelligenceCliniqueHelper {

    private final GrossesseHelper grossesseHelper;
    private final AccesDossierHelper acces;
    private final MoteurReglesCliniques moteur;
    private final CalculObstetricalHelper calcul;
    private final ConsultationRepository consultationRepository;
    private final RendezVousRepository rendezVousRepository;
    private final AlerteMedicaleRepository alerteRepository;

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    public PlanCpnDto monPlanCpn(String email) {
        return planCpn(grossesseHelper.trouverActiveParEmail(email));
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public PlanCpnDto planCpnParId(String email, Long grossesseId) {
        Grossesse grossesse = grossesseHelper.trouverParId(grossesseId);
        acces.verifierLecture(acces.utilisateur(email), grossesse);
        return planCpn(grossesse);
    }

    /** Évaluation clinique : réservée aux soignants qui suivent la patiente. */
    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    public EvaluationRisqueDto evaluerRisque(String email, Long grossesseId) {
        Grossesse grossesse = grossesseHelper.trouverParId(grossesseId);
        acces.verifierLectureClinique(acces.utilisateur(email), grossesse);

        List<String> codesSignaux = consultationRepository.signauxParGrossesse(grossesseId).stream()
                .flatMap(bloc -> Arrays.stream(bloc.split("\n")))
                .map(ligne -> ligne.split("\\|")[0])
                .toList();
        List<String> facteurs = moteur.facteursDeRisque(
                grossesse.getFemmeEnceinte().getDateNaissance(), grossesse.getDateDernieresRegles(),
                grossesse.getNombreGrossessesPrecedentes(), grossesse.getAntecedents(), codesSignaux,
                alerteRepository.countByGrossesseIdAndNiveauGravite(grossesseId, NiveauGravite.URGENT));
        boolean aRisque = Boolean.TRUE.equals(grossesse.getGrossesseARisque());
        return new EvaluationRisqueDto(grossesseId, aRisque, facteurs, !aRisque && !facteurs.isEmpty(),
                "Suggestion automatique : la décision appartient au médecin.");
    }

    public PlanCpnDto planCpn(Grossesse grossesse) {
        List<LocalDate> consultations = consultationRepository.datesParGrossesse(grossesse.getId()).stream()
                .map(LocalDateTime::toLocalDate).toList();
        List<LocalDate> rendezVous = rendezVousRepository.datesActivesParGrossesse(grossesse.getId()).stream()
                .map(LocalDateTime::toLocalDate).toList();
        List<MoteurReglesCliniques.ContactCpn> contacts = moteur.planCpn(
                grossesse.getDateDernieresRegles(), consultations, rendezVous, LocalDate.now());
        long realises = contacts.stream().filter(c -> MoteurReglesCliniques.CPN_REALISE.equals(c.statut())).count();
        long enRetard = contacts.stream().filter(c -> MoteurReglesCliniques.CPN_EN_RETARD.equals(c.statut())).count();
        return new PlanCpnDto(grossesse.getId(), calcul.calculerSemaineActuelle(grossesse.getDateDernieresRegles()),
                contacts, realises, enRetard, "OMS 2016 : 8 contacts prénatals (12, 20, 26, 30, 34, 36, 38 et 40 SA)");
    }
}
