package sn.yay360.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.yay360.api.validation.TelephoneSenegal;

/** Jeton de rafraîchissement transmis pour prolonger ou fermer une session mobile. */
@Getter
@Setter
public class MobileJetonRequest {
    @NotBlank(message = "Le jeton de session est obligatoire")
    private String jetonRafraichissement;
}
