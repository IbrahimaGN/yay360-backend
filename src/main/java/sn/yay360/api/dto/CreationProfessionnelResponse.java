package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Compte créé. Sans serveur d'email, le mot de passe temporaire est affiché une seule fois à
 * l'administrateur pour qu'il le transmette ; avec un SMTP, il n'est envoyé qu'au professionnel.
 * Tant qu'il n'est pas changé, le compte ne peut que changer son mot de passe.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreationProfessionnelResponse {
    private UtilisateurDto utilisateur;
    private String motDePasseTemporaire;
    private boolean emailEnvoye;
}
