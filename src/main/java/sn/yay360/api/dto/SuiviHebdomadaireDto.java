package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * Vue "intelligente" de l'accompagnement de la femme enceinte pour la semaine en cours :
 * regroupe le développement du bébé, les changements de la mère, les objectifs de santé
 * ainsi que les conseils personnalisés issus de la bibliothèque de conseils (§5.4).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SuiviHebdomadaireDto {

    private Long grossesseId;
    private Integer semaineActuelle;
    private String trimestre;
    private Integer ageGestationnelJours;
    private LocalDate datePrevueAccouchement;
    private Integer joursRestants;
    private Boolean grossesseARisque;

    private String developpementBebe;
    private String changementsMere;
    private String objectifsSante;
    private String conseilsAlimentation;
    private String conseilsHydratation;
    private String exercicesPhysiques;
    private String conseilsSommeil;
    private String precautions;
    private String examensRecommandes;
    private String signesAlerte;

    private List<ConseilDto> conseilsPersonnalises;

    private String avertissement = "Ces recommandations ne remplacent jamais l'avis d'un professionnel de santé.";
}
