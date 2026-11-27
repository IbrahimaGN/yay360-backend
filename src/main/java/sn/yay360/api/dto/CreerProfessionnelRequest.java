package sn.yay360.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import sn.yay360.api.validation.TelephoneSenegal;

import java.time.LocalDate;

/** Création d'un compte soignant ou administrateur par un administrateur (plateforme ou établissement). */
@Getter
@Setter
public class CreerProfessionnelRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    @Pattern(regexp = "^[\\p{L}'\\- ]+$", message = "Le nom ne doit contenir aucun chiffre ni caractère spécial")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    @Pattern(regexp = "^[\\p{L}'\\- ]+$", message = "Le prénom ne doit contenir aucun chiffre ni caractère spécial")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être une adresse valide")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Le téléphone est obligatoire")
    @TelephoneSenegal
    private String telephone;

    @NotNull(message = "La date de naissance est obligatoire")
    @Past(message = "La date de naissance doit être dans le passé")
    private LocalDate dateNaissance;

    @NotBlank(message = "Le rôle est obligatoire")
    @Pattern(regexp = "MEDECIN|SAGE_FEMME|ADMIN|ADMIN_ETABLISSEMENT",
            message = "Rôle invalide : MEDECIN, SAGE_FEMME, ADMIN ou ADMIN_ETABLISSEMENT")
    private String role;

    /**
     * Établissement de rattachement : obligatoire pour un administrateur d'établissement, facultatif
     * pour un soignant créé par l'administrateur de la plateforme. Ignoré quand l'auteur est un
     * administrateur d'établissement : le soignant est alors rattaché à son propre établissement.
     */
    private Long etablissementId;
}
