package sn.yay360.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.yay360.api.validation.TelephoneSenegal;

/** Inscription mobile d'une femme enceinte : le compte est créé après vérification du code reçu par e-mail. */
@Getter
@Setter
public class MobileInscriptionRequest {
    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    @Pattern(regexp = "^[\\p{L}'\\- ]+$", message = "Le nom ne doit contenir aucun chiffre ni caractère spécial")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    @Pattern(regexp = "^[\\p{L}'\\- ]+$", message = "Le prénom ne doit contenir aucun chiffre ni caractère spécial")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être une adresse valide contenant un @")
    @Size(max = 150, message = "L'email ne doit pas dépasser 150 caractères")
    private String email;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @TelephoneSenegal
    private String telephone;

    @NotNull(message = "La date de naissance est obligatoire")
    @Past(message = "La date de naissance doit être dans le passé")
    private java.time.LocalDate dateNaissance;
}
