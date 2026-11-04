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
    private LocalDate datePrevueAccouchement;

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
    private LocalDateTime dateCreation;
}
