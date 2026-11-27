package sn.yay360.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Affectation d'un soignant existant à un établissement, désigné par son id ou par son email.
 * L'email permet à un administrateur d'établissement d'ajouter un soignant déjà inscrit ailleurs
 * sans pouvoir parcourir la liste des comptes de la plateforme.
 */
@Getter
@Setter
public class AffecterProfessionnelRequest {

    private Long professionnelId;

    @Email(message = "L'email doit être une adresse valide")
    @Size(max = 150)
    private String email;
}
