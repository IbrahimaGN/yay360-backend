package sn.yay360.api.helper;

import sn.yay360.api.dto.ActiviteEtablissementDto;
import sn.yay360.api.dto.DashboardDto;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.NiveauGravite;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.StatutAlerte;
import sn.yay360.api.model.StatutGrossesse;
import sn.yay360.api.model.StatutRendezVous;
import sn.yay360.api.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agrège les statistiques destinées au tableau de bord administrateur (§5.12).
 * Ne contient aucune logique métier propre : compile les données déjà gérées
 * par les autres modules (Utilisateur, Grossesse, Consultation, RendezVous, Alerte, Établissement).
 */
@Component
@RequiredArgsConstructor
public class DashboardHelper {

    private final UtilisateurRepository utilisateurRepository;
    private final GrossesseRepository grossesseRepository;
    private final ConsultationRepository consultationRepository;
    private final RendezVousRepository rendezVousRepository;
    private final AlerteMedicaleRepository alerteMedicaleRepository;
    private final EtablissementSanteRepository etablissementSanteRepository;
    private final CalculObstetricalHelper calcul;

    @PreAuthorize("hasAuthority('ADMIN')")
    public DashboardDto genererTableauDeBord() {
        DashboardDto dto = new DashboardDto();

        // --- Utilisateurs ---
        dto.setNombreFemmesEnceintes(utilisateurRepository.countByRole(RoleUtilisateur.FEMME_ENCEINTE));
        dto.setNombreMedecins(utilisateurRepository.countByRole(RoleUtilisateur.MEDECIN));
        dto.setNombreSagesFemmes(utilisateurRepository.countByRole(RoleUtilisateur.SAGE_FEMME));
        dto.setNombreAdmins(utilisateurRepository.countByRole(RoleUtilisateur.ADMIN));

        // --- Grossesses ---
        dto.setNombreGrossessesActives(grossesseRepository.countByStatut(StatutGrossesse.EN_COURS));
        dto.setNombreGrossessesTerminees(grossesseRepository.countByStatut(StatutGrossesse.TERMINEE));
        dto.setNombreGrossessesInterrompues(grossesseRepository.countByStatut(StatutGrossesse.INTERROMPUE));
        dto.setNombreGrossessesARisque(grossesseRepository.countByGrossesseARisqueTrue());
        dto.setRepartitionGrossessesParTrimestre(calculerRepartitionParTrimestre());

        // --- Consultations & rendez-vous ---
        dto.setNombreConsultations(consultationRepository.count());
        dto.setNombreRendezVous(rendezVousRepository.count());
        dto.setRepartitionRendezVousParStatut(Map.of(
                StatutRendezVous.PLANIFIE, rendezVousRepository.countByStatut(StatutRendezVous.PLANIFIE),
                StatutRendezVous.CONFIRME, rendezVousRepository.countByStatut(StatutRendezVous.CONFIRME),
                StatutRendezVous.TERMINE, rendezVousRepository.countByStatut(StatutRendezVous.TERMINE),
                StatutRendezVous.ANNULE, rendezVousRepository.countByStatut(StatutRendezVous.ANNULE),
                StatutRendezVous.ABSENT, rendezVousRepository.countByStatut(StatutRendezVous.ABSENT)
        ));

        // --- Alertes ---
        dto.setNombreAlertesNouvelles(alerteMedicaleRepository.countByStatut(StatutAlerte.NOUVELLE));
        dto.setNombreAlertesPrisesEnCharge(alerteMedicaleRepository.countByStatut(StatutAlerte.PRISE_EN_CHARGE));
        dto.setNombreAlertesResolues(alerteMedicaleRepository.countByStatut(StatutAlerte.RESOLUE));
        dto.setNombreAlertesUrgentes(
                (long) alerteMedicaleRepository.findByNiveauGraviteOrderByDateDeclarationDesc(NiveauGravite.URGENT).size());

        // --- Établissements ---
        List<EtablissementSante> etablissementsActifs = etablissementSanteRepository.findByActifTrue();
        dto.setNombreEtablissementsActifs(etablissementsActifs.size());
        dto.setActiviteEtablissements(calculerActiviteEtablissements(etablissementsActifs));

        dto.setDateGeneration(LocalDateTime.now());
        return dto;
    }

    private Map<String, Long> calculerRepartitionParTrimestre() {
        Map<String, Long> repartition = new HashMap<>();
        repartition.put("T1", 0L);
        repartition.put("T2", 0L);
        repartition.put("T3", 0L);

        List<Grossesse> grossessesActives = grossesseRepository.findByStatut(StatutGrossesse.EN_COURS);
        for (Grossesse grossesse : grossessesActives) {
            int semaine = calcul.calculerSemaineActuelle(grossesse.getDateDernieresRegles());
            String trimestre = calcul.calculerTrimestre(semaine);
            repartition.merge(trimestre, 1L, Long::sum);
        }
        return repartition;
    }

    private List<ActiviteEtablissementDto> calculerActiviteEtablissements(List<EtablissementSante> etablissements) {
        return etablissements.stream()
                .map(e -> new ActiviteEtablissementDto(
                        e.getId(),
                        e.getNom(),
                        e.getVille(),
                        rendezVousRepository.countByEtablissementId(e.getId())
                ))
                .toList();
    }
}
