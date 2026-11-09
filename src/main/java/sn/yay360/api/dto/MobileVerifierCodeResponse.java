package sn.yay360.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.yay360.api.validation.TelephoneSenegal;

/** Code vérifié : jeton à usage unique (10 minutes) pour définir le PIN. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MobileVerifierCodeResponse {
    private String jetonPin;
    private String message;
}
