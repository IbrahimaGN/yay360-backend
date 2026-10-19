package sn.yay360.api.dto;

import sn.yay360.api.validation.TelephoneSenegal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    @Pattern(regexp = "^[\\p{L}'\\- ]+$", message = "Le nom ne doit contenir aucun chiffre ni caractère spécial")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    @Pattern(regexp = "^[\\p{L}'\\- ]+$", message = "Le prénom ne doit contenir aucun chiffre ni caractère spécial")
    private String prenom;

    @TelephoneSenegal
    private String telephone;
}
