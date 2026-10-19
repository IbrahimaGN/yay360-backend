package sn.yay360.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnregistrerConsultationRequest {

    @NotNull(message = "L'id de la grossesse est obligatoire")
    private Long grossesseId;

    private Long rendezVousId;

    private Long etablissementId;

    @DecimalMin(value = "20.0", message = "Le poids semble invalide")
    @DecimalMax(value = "250.0", message = "Le poids semble invalide")
    private Double poidsKg;

    private String tensionArterielle;

    // Bornes larges pour ne jamais bloquer une valeur cliniquement anormale
    // (fièvre élevée, hypothermie) : seules les erreurs de saisie grossières sont rejetées.
    @DecimalMin(value = "25.0", message = "La température semble invalide")
    @DecimalMax(value = "45.0", message = "La température semble invalide")
    private Double temperatureC;

    @DecimalMin(value = "0.0", message = "La hauteur utérine ne peut pas être négative")
    @DecimalMax(value = "50.0", message = "La hauteur utérine semble invalide")
    private Double hauteurUterineCm;

    @Min(value = 0, message = "Les battements du cœur fœtal ne peuvent pas être négatifs")
    @Max(value = 300, message = "Les battements du cœur fœtal semblent invalides")
    private Integer battementsCoeurFoetal;

    private String observations;
    private String diagnostic;
    private String traitement;
    private String prescriptions;
}
