package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GrossesseDto {

    private Long id;
    private Long femmeEnceinteId;
    private String femmeEnceinteNomComplet;
    private LocalDate dateDernieresRegles;
    /** Estimation (règle de Naegele : dernières règles + 280 jours), pas une date certaine. */
    private LocalDate datePrevueAccouchement;
    /** Période d'accouchement à terme : 37 SA + 0 j … */
    private LocalDate debutPeriodeTerme;
    /** … à 41 SA + 6 j. */
    private LocalDate finPeriodeTerme;

    // --- Champs calculés dynamiquement (jamais stockés) ---
    private Integer ageGestationnelJours;
    private Integer semaineActuelle;
    private String trimestre;
    private Integer joursRestants;

    private String statut;
    private Boolean grossesseARisque;
    private String motifRisque;
    private Integer nombreGrossessesPrecedentes;
    private String antecedents;
    private Long etablissementSuiviId;
    private String etablissementSuiviNom;
    private LocalDate dateFin;
    /** Issue de la grossesse close ; masquée (null) pour le conjoint. */
    private String issue;
    private LocalDateTime dateCreation;
}
