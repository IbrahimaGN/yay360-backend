package sn.yay360.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EtablissementRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le type est obligatoire")
    private String type;

    @NotBlank(message = "L'adresse est obligatoire")
    private String adresse;

    private String ville;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^\\+221[0-9]{9}$",
            message = "Le numéro doit être un numéro sénégalais valide (+221 suivi de 9 chiffres)")
    private String telephone;

    @Email(message = "L'email doit être une adresse valide contenant un @")
    private String email;

    private Double latitude;
    private Double longitude;
}
