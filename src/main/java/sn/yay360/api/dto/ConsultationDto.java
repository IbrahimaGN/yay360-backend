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
public class ConsultationDto {

    private Long id;
    private Long grossesseId;
    private Long femmeEnceinteId;
    private String femmeEnceinteNomComplet;
    private Long professionnelId;
    private String professionnelNomComplet;
    private Long rendezVousId;
    private String etablissementNom;
    private LocalDateTime dateConsultation;

    private Double poidsKg;
    private String tensionArterielle;
    private Double temperatureC;
    private Double hauteurUterineCm;
    private Integer battementsCoeurFoetal;

    private String observations;
    private String diagnostic;
    private String traitement;
    private String prescriptions;
    private LocalDateTime dateCreation;
    /** Signaux détectés automatiquement par le moteur de règles sur les constantes. */
    private java.util.List<String> signauxCliniques;
}
