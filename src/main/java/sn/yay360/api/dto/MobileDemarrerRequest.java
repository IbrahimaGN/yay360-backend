package sn.yay360.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.yay360.api.validation.TelephoneSenegal;

/** Étape 1 de la connexion mobile : le numéro de téléphone. */
@Getter
@Setter
public class MobileDemarrerRequest {
    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @TelephoneSenegal
    private String telephone;
}
