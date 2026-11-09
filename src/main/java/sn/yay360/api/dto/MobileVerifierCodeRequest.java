package sn.yay360.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.yay360.api.validation.TelephoneSenegal;

@Getter
@Setter
public class MobileVerifierCodeRequest {
    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @TelephoneSenegal
    private String telephone;

    @NotBlank(message = "Le code est obligatoire")
    @Pattern(regexp = "^\\d{6}$", message = "Le code contient 6 chiffres")
    private String code;
}
