package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlerteDto {

    private Long id;
    private Long grossesseId;
    private Long femmeEnceinteId;
    private String femmeEnceinteNomComplet;
    private String femmeEnceinteTelephone;
    private String typeSymptome;
    private String description;
    private String niveauGravite;
    private String statut;
    /** Soignant qui a pris l'alerte en charge. */
    private String professionnelNomComplet;
    /** Soignant qui a résolu l'alerte ; null si non résolue ou résolue avant l'enregistrement de cette information. */
    private String resoluParNomComplet;
    private String notesPriseEnCharge;
    private LocalDateTime dateDeclaration;
    private LocalDateTime dateResolution;
    private String recommandation;
    private String origine;
    /** Règles de triage appliquées (intelligence explicable). */
    private java.util.List<String> justificationTriage;
    private String avertissement = "Ces recommandations ne remplacent jamais l'avis d'un professionnel de santé.";
}
