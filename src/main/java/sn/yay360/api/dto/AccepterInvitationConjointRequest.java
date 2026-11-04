package sn.yay360.api.dto;

import sn.yay360.api.validation.TelephoneSenegal;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AccepterInvitationConjointRequest {

    @NotBlank
    private String token;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[\\p{L}'\\- ]+$")
    private String nom;

    @NotBlank
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[\\p{L}'\\- ]+$")
    private String prenom;

    @NotBlank
    @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Le mot de passe doit contenir au moins une lettre et un chiffre")
    private String password;

    @NotBlank
    @TelephoneSenegal
    private String telephone;

    @NotNull
    @Past
    private LocalDate dateNaissance;
}
