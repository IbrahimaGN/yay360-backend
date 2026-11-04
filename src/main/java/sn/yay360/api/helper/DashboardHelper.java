package sn.yay360.api.helper;

import sn.yay360.api.dto.ActiviteEtablissementDto;
import sn.yay360.api.dto.DashboardDto;
import sn.yay360.api.model.NiveauGravite;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.StatutAlerte;
import sn.yay360.api.model.StatutGrossesse;
import sn.yay360.api.model.StatutRendezVous;
import sn.yay360.api.repo.AlerteMedicaleRepository;
import sn.yay360.api.repo.ConsultationRepository;
import sn.yay360.api.repo.EtablissementSanteRepository;
import sn.yay360.api.repo.GrossesseRepository;
import sn.yay360.api.repo.RendezVousRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tableau de bord de pilotage (§4.8). Les agrégations sont faites en base (GROUP BY) :
 * une dizaine de requêtes au total, quel que soit le volume de données (§6, pas de N+1).
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
    @Transactional(readOnly = true)
    public DashboardDto genererTableauDeBord() {
        DashboardDto dto = new DashboardDto();

        // --- Utilisateurs ---
        Map<String, Long> roles = enMap(utilisateurRepository.compterParRole());
        dto.setNombreFemmesEnceintes(roles.getOrDefault(RoleUtilisateur.FEMME_ENCEINTE, 0L));
        dto.setNombreMedecins(roles.getOrDefault(RoleUtilisateur.MEDECIN, 0L));
        dto.setNombreSagesFemmes(roles.getOrDefault(RoleUtilisateur.SAGE_FEMME, 0L));
        dto.setNombreAdmins(roles.getOrDefault(RoleUtilisateur.ADMIN, 0L));

        // --- Grossesses ---
        Map<String, Long> grossesses = enMap(grossesseRepository.compterParStatut());
        long actives = grossesses.getOrDefault(StatutGrossesse.EN_COURS, 0L);
        long aRisque = grossesseRepository.countByStatutAndGrossesseARisqueTrue(StatutGrossesse.EN_COURS);
        dto.setNombreGrossessesActives(actives);
        dto.setNombreGrossessesTerminees(grossesses.getOrDefault(StatutGrossesse.TERMINEE, 0L));
        dto.setNombreGrossessesInterrompues(grossesses.getOrDefault(StatutGrossesse.INTERROMPUE, 0L));
        dto.setNombreGrossessesARisque(aRisque);
        dto.setRatioGrossessesARisque(pourcentage(aRisque, actives));
        dto.setRepartitionGrossessesParTrimestre(repartitionParTrimestre());

        // --- Consultations & rendez-vous ---
        dto.setNombreConsultations(consultationRepository.count());
        Map<String, Long> rdv = completer(enMap(rendezVousRepository.compterParStatut()), List.of(
                StatutRendezVous.PLANIFIE, StatutRendezVous.CONFIRME, StatutRendezVous.TERMINE,
                StatutRendezVous.ANNULE, StatutRendezVous.ABSENT));
        dto.setRepartitionRendezVousParStatut(rdv);
        dto.setNombreRendezVous(rdv.values().stream().mapToLong(Long::longValue).sum());

        // --- Alertes ---
        Map<String, Long> alertes = enMap(alerteMedicaleRepository.compterParStatut());
        long resolues = alertes.getOrDefault(StatutAlerte.RESOLUE, 0L);
        long totalAlertes = alertes.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Long> gravites = completer(enMap(alerteMedicaleRepository.compterNonResoluesParGravite()),
                List.of(NiveauGravite.URGENT, NiveauGravite.ELEVE, NiveauGravite.MODERE));
        dto.setNombreAlertesNouvelles(alertes.getOrDefault(StatutAlerte.NOUVELLE, 0L));
        dto.setNombreAlertesPrisesEnCharge(alertes.getOrDefault(StatutAlerte.PRISE_EN_CHARGE, 0L));
        dto.setNombreAlertesResolues(resolues);
        dto.setNombreAlertesUrgentes(gravites.get(NiveauGravite.URGENT)); // urgentes NON résolues
        dto.setRepartitionAlertesNonResoluesParGravite(gravites);
        dto.setTauxResolutionAlertes(pourcentage(resolues, totalAlertes));
        dto.setDelaiMoyenPriseEnChargeMinutes(delaiMoyenPriseEnCharge());

        // --- Établissements ---
        List<ActiviteEtablissementDto> activite = etablissementSanteRepository.activiteEtablissementsActifs().stream()
                .map(l -> new ActiviteEtablissementDto((Long) l[0], (String) l[1], (String) l[2], (Long) l[3]))
                .toList();
        dto.setNombreEtablissementsActifs(activite.size());
        dto.setActiviteEtablissements(activite);

        dto.setDateGeneration(LocalDateTime.now());
        return dto;
    }

    private Map<String, Long> repartitionParTrimestre() {
        Map<String, Long> repartition = new LinkedHashMap<>();
        List.of("T1", "T2", "T3").forEach(t -> repartition.put(t, 0L));
        for (LocalDate ddr : grossesseRepository.datesDernieresReglesEnCours()) {
            repartition.merge(calcul.calculerTrimestre(calcul.calculerSemaineActuelle(ddr)), 1L, Long::sum);
        }
        return repartition;
    }

    private Double delaiMoyenPriseEnCharge() {
        return alerteMedicaleRepository.delaisPriseEnCharge().stream()
                .mapToLong(l -> Duration.between((LocalDateTime) l[0], (LocalDateTime) l[1]).toMinutes())
                .average()
                .stream().boxed().map(m -> Math.round(m * 10) / 10.0)
                .findFirst().orElse(null);
    }

    private static Map<String, Long> enMap(List<Object[]> lignes) {
        Map<String, Long> map = new LinkedHashMap<>();
        lignes.forEach(l -> map.put((String) l[0], (Long) l[1]));
        return map;
    }

    private static Map<String, Long> completer(Map<String, Long> valeurs, List<String> cles) {
        Map<String, Long> complet = new LinkedHashMap<>();
        cles.forEach(c -> complet.put(c, valeurs.getOrDefault(c, 0L)));
        return complet;
    }

    static double pourcentage(long partie, long total) {
        return total == 0 ? 0.0 : Math.round(partie * 1000.0 / total) / 10.0;
    }
}
