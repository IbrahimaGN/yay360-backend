package sn.yay360.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PrendreRendezVousRequest {

    @NotNull(message = "La date et l'heure sont obligatoires")
    @Future(message = "La date du rendez-vous doit être dans le futur")
    private LocalDateTime dateHeure;

    private Long grossesseId;

    public Long getGrossesseId() {
        return grossesseId;
    }

    @NotBlank(message = "Le type de rendez-vous est obligatoire")
    @jakarta.validation.constraints.Pattern(
            regexp = "CONSULTATION_PRENATALE|ECHOGRAPHIE|VACCINATION|EXAMEN_LABORATOIRE|AUTRE",
            message = "Type de rendez-vous inconnu")
    private String type;

    private Long professionnelId;

    private Long etablissementId;

    @jakarta.validation.constraints.Size(max = 500, message = "Le motif ne doit pas dépasser 500 caractères")
    private String motif;
}
