package sn.yay360.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Rendez-vous planifié par un soignant pour une patiente qu'il suit (cahier des charges §3). */
@Getter
@Setter
public class PlanifierRendezVousRequest {

    @NotNull(message = "La grossesse est obligatoire")
    private Long grossesseId;

    @NotNull(message = "La date et l'heure sont obligatoires")
    @Future(message = "La date du rendez-vous doit être dans le futur")
    private LocalDateTime dateHeure;

    @NotBlank(message = "Le type de rendez-vous est obligatoire")
    @Pattern(regexp = "CONSULTATION_PRENATALE|ECHOGRAPHIE|VACCINATION|EXAMEN_LABORATOIRE|AUTRE",
            message = "Type de rendez-vous inconnu")
    private String type;

    private Long etablissementId;

    @Size(max = 500, message = "Le motif ne doit pas dépasser 500 caractères")
    private String motif;
}
