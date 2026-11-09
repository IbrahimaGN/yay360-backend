package sn.yay360.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.yay360.api.validation.TelephoneSenegal;

@Getter
@Setter
public class MobileDefinirPinRequest {
    @NotBlank(message = "Le jeton de vérification est obligatoire")
    private String jetonPin;

    @NotBlank(message = "Le PIN est obligatoire")
    @Pattern(regexp = "^\\d{4}$", message = "Le PIN contient 4 chiffres")
    private String pin;

    /** true : session longue (jeton de rafraîchissement de 30 jours, renouvelé à chaque ouverture). */
    private boolean resterConnecte;
}
