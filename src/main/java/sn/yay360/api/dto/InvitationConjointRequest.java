package sn.yay360.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvitationConjointRequest {

    @NotBlank(message = "L'email du conjoint est obligatoire")
    @Email(message = "L'email doit être une adresse valide")
    private String email;
}
