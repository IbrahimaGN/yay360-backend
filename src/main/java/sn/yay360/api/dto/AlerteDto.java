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
    private String professionnelNomComplet;
    private String notesPriseEnCharge;
    private LocalDateTime dateDeclaration;
    private LocalDateTime dateResolution;
    private String recommandation;
    private String avertissement = "Ces recommandations ne remplacent jamais l'avis d'un professionnel de santé.";
}
