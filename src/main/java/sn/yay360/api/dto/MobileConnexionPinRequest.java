package sn.yay360.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.yay360.api.validation.TelephoneSenegal;

@Getter
@Setter
public class MobileConnexionPinRequest {
    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @TelephoneSenegal
    private String telephone;

    @NotBlank(message = "Le PIN est obligatoire")
    @Pattern(regexp = "^\\d{4}$", message = "Le PIN contient 4 chiffres")
    private String pin;

    private boolean resterConnecte;
}
