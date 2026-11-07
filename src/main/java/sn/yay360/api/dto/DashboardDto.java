package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDto {

    // --- Utilisateurs ---
    private long nombreFemmesEnceintes;
    private long nombreMedecins;
    private long nombreSagesFemmes;
    private long nombreAdmins;

    // --- Grossesses ---
    private long nombreGrossessesActives;
    private long nombreGrossessesTerminees;
    private long nombreGrossessesInterrompues;
    private long nombreGrossessesARisque;
    private Map<String, Long> repartitionGrossessesParTrimestre;

    // --- Consultations & rendez-vous ---
    private long nombreConsultations;
    private long nombreRendezVous;
    private Map<String, Long> repartitionRendezVousParStatut;

    // --- Alertes ---
    private long nombreAlertesNouvelles;
    private long nombreAlertesPrisesEnCharge;
    private long nombreAlertesResolues;
    private long nombreAlertesUrgentes;
    private Map<String, Long> repartitionAlertesNonResoluesParGravite;

    // --- Indicateurs de pilotage (cahier des charges §4.8) ---
    /** Grossesses à risque en cours / grossesses en cours, en %. */
    private double ratioGrossessesARisque;
    /** Alertes résolues / alertes déclarées, en %. */
    private double tauxResolutionAlertes;
    /** Délai moyen entre la déclaration d'une alerte et sa prise en charge, en minutes. */
    private Double delaiMoyenPriseEnChargeMinutes;

    // --- Établissements ---
    private long nombreEtablissementsActifs;
    private List<ActiviteEtablissementDto> activiteEtablissements;

    private LocalDateTime dateGeneration;
}
