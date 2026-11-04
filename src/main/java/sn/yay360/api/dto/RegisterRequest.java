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
public class RegisterRequest {

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

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Le mot de passe doit contenir au moins une lettre et un chiffre")
    private String password;

    @NotBlank(message = "Le téléphone est obligatoire")
    @TelephoneSenegal
    private String telephone;

    @NotNull(message = "La date de naissance est obligatoire")
    @Past(message = "La date de naissance doit être dans le passé")
    private LocalDate dateNaissance;

    /**
     * Facultatif : l'inscription publique crée toujours un compte FEMME_ENCEINTE.
     * Les comptes professionnels sont créés par un administrateur (POST /utilisateurs/professionnels).
     */
    @Pattern(regexp = "FEMME_ENCEINTE",
            message = "L'inscription en ligne est réservée aux femmes enceintes")
    private String role;
}
