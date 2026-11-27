package sn.yay360.api.helper;

import sn.yay360.api.dto.ActiviteEtablissementDto;
import sn.yay360.api.dto.DashboardDto;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.model.NiveauGravite;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.StatutAlerte;
import sn.yay360.api.model.StatutGrossesse;
import sn.yay360.api.model.StatutRendezVous;
import sn.yay360.api.model.Utilisateur;
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
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tableau de bord de pilotage (§4.8) : des nombres agrégés, jamais un dossier.
 *
 * <ul>
 *   <li>Administrateur de la plateforme : toute la plateforme.</li>
 *   <li>Administrateur d'établissement : le même tableau, limité à son établissement (grossesses dont il est
 *       l'établissement de suivi, rendez-vous et consultations qui s'y tiennent, soignants qui y exercent).</li>
 * </ul>
 *
 * Les agrégations sont faites en base (GROUP BY) : une dizaine de requêtes au total, quel que soit le
 * volume de données (§6, pas de N+1).
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
    private final PerimetreAdminHelper perimetre;

    /** Données brutes d'un périmètre : toute la plateforme ou un seul établissement. */
    private record Donnees(Map<String, Long> roles, Map<String, Long> grossesses, long grossessesARisque,
                           List<LocalDate> datesDernieresRegles, long consultations, Map<String, Long> rendezVous,
                           Map<String, Long> alertes, Map<String, Long> gravites, List<Object[]> delais,
                           List<ActiviteEtablissementDto> activite) {
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'ADMIN_ETABLISSEMENT')")
    @Transactional(readOnly = true)
    public DashboardDto genererTableauDeBord(String email) {
        Utilisateur auteur = perimetre.auteur(email);
        Donnees donnees = PerimetreAdminHelper.estAdminEtablissement(auteur)
                ? donneesEtablissement(perimetre.etablissementAdministre(auteur))
                : donneesPlateforme();
        return construire(donnees);
    }

    private Donnees donneesPlateforme() {
        return new Donnees(
                enMap(utilisateurRepository.compterParRole()),
                enMap(grossesseRepository.compterParStatut()),
                grossesseRepository.countByStatutAndGrossesseARisqueTrue(StatutGrossesse.EN_COURS),
                grossesseRepository.datesDernieresReglesEnCours(),
                consultationRepository.count(),
                enMap(rendezVousRepository.compterParStatut()),
                enMap(alerteMedicaleRepository.compterParStatut()),
                enMap(alerteMedicaleRepository.compterNonResoluesParGravite()),
                alerteMedicaleRepository.delaisPriseEnCharge(),
                activite(null));
    }

    private Donnees donneesEtablissement(EtablissementSante etablissement) {
        Long id = etablissement.getId();
        Map<String, Long> roles = new LinkedHashMap<>();
        etablissement.getProfessionnels().forEach(p -> roles.merge(p.getRole(), 1L, Long::sum));
        roles.put(RoleUtilisateur.FEMME_ENCEINTE, grossesseRepository.compterPatientesDansEtablissement(id));
        roles.put(RoleUtilisateur.ADMIN, 1L); // un seul administrateur par établissement (V9)
        return new Donnees(
                roles,
                enMap(grossesseRepository.compterParStatutDansEtablissement(id)),
                grossesseRepository.countByStatutAndGrossesseARisqueTrueAndEtablissementSuiviId(StatutGrossesse.EN_COURS, id),
                grossesseRepository.datesDernieresReglesEnCoursDansEtablissement(id),
                consultationRepository.countByEtablissementId(id),
                enMap(rendezVousRepository.compterParStatutDansEtablissement(id)),
                enMap(alerteMedicaleRepository.compterParStatutDansEtablissement(id)),
                enMap(alerteMedicaleRepository.compterNonResoluesParGraviteDansEtablissement(id)),
                alerteMedicaleRepository.delaisPriseEnChargeDansEtablissement(id),
                activite(id));
    }

    /** Activité des établissements actifs ; limitée à un seul établissement si un id est donné. */
    private List<ActiviteEtablissementDto> activite(Long etablissementId) {
        return etablissementSanteRepository.activiteEtablissementsActifs().stream()
                .filter(l -> etablissementId == null || etablissementId.equals(l[0]))
                .map(l -> new ActiviteEtablissementDto((Long) l[0], (String) l[1], (String) l[2], (Long) l[3]))
                .toList();
    }

    private DashboardDto construire(Donnees d) {
        DashboardDto dto = new DashboardDto();

        // --- Utilisateurs ---
        dto.setNombreFemmesEnceintes(d.roles().getOrDefault(RoleUtilisateur.FEMME_ENCEINTE, 0L));
        dto.setNombreMedecins(d.roles().getOrDefault(RoleUtilisateur.MEDECIN, 0L));
        dto.setNombreSagesFemmes(d.roles().getOrDefault(RoleUtilisateur.SAGE_FEMME, 0L));
        dto.setNombreAdmins(d.roles().getOrDefault(RoleUtilisateur.ADMIN, 0L));

        // --- Grossesses ---
        long actives = d.grossesses().getOrDefault(StatutGrossesse.EN_COURS, 0L);
        dto.setNombreGrossessesActives(actives);
        dto.setNombreGrossessesTerminees(d.grossesses().getOrDefault(StatutGrossesse.TERMINEE, 0L));
        dto.setNombreGrossessesInterrompues(d.grossesses().getOrDefault(StatutGrossesse.INTERROMPUE, 0L));
        dto.setNombreGrossessesARisque(d.grossessesARisque());
        dto.setRatioGrossessesARisque(pourcentage(d.grossessesARisque(), actives));
        dto.setRepartitionGrossessesParTrimestre(repartitionParTrimestre(d.datesDernieresRegles()));

        // --- Consultations & rendez-vous ---
        dto.setNombreConsultations(d.consultations());
        Map<String, Long> rdv = completer(d.rendezVous(), List.of(
                StatutRendezVous.PLANIFIE, StatutRendezVous.CONFIRME, StatutRendezVous.TERMINE,
                StatutRendezVous.ANNULE, StatutRendezVous.ABSENT));
        dto.setRepartitionRendezVousParStatut(rdv);
        dto.setNombreRendezVous(rdv.values().stream().mapToLong(Long::longValue).sum());

        // --- Alertes ---
        long resolues = d.alertes().getOrDefault(StatutAlerte.RESOLUE, 0L);
        long totalAlertes = d.alertes().values().stream().mapToLong(Long::longValue).sum();
        Map<String, Long> gravites = completer(d.gravites(),
                List.of(NiveauGravite.URGENT, NiveauGravite.ELEVE, NiveauGravite.MODERE));
        dto.setNombreAlertesNouvelles(d.alertes().getOrDefault(StatutAlerte.NOUVELLE, 0L));
        dto.setNombreAlertesPrisesEnCharge(d.alertes().getOrDefault(StatutAlerte.PRISE_EN_CHARGE, 0L));
        dto.setNombreAlertesResolues(resolues);
        dto.setNombreAlertesUrgentes(gravites.get(NiveauGravite.URGENT)); // urgentes NON résolues
        dto.setRepartitionAlertesNonResoluesParGravite(gravites);
        dto.setTauxResolutionAlertes(pourcentage(resolues, totalAlertes));
        dto.setDelaiMoyenPriseEnChargeMinutes(delaiMoyenMinutes(d.delais()));

        // --- Établissements ---
        dto.setNombreEtablissementsActifs(d.activite().size());
        dto.setActiviteEtablissements(d.activite());

        dto.setDateGeneration(LocalDateTime.now());
        return dto;
    }

    private Map<String, Long> repartitionParTrimestre(List<LocalDate> datesDernieresRegles) {
        Map<String, Long> repartition = new LinkedHashMap<>();
        List.of("T1", "T2", "T3").forEach(t -> repartition.put(t, 0L));
        for (LocalDate ddr : datesDernieresRegles) {
            repartition.merge(calcul.calculerTrimestre(calcul.calculerSemaineActuelle(ddr)), 1L, Long::sum);
        }
        return repartition;
    }

    /** Moyenne, en minutes arrondies au dixième, des couples [déclaration, prise en charge] ; null si aucun. */
    static Double delaiMoyenMinutes(List<Object[]> delais) {
        return delais.stream()
                .mapToLong(l -> Duration.between(((LocalDateTime) l[0]).atZone(ZoneId.systemDefault()),
                        ((LocalDateTime) l[1]).atZone(ZoneId.systemDefault())).toMinutes())
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
