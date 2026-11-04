package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Réponse au changement de mot de passe : le client doit remplacer son jeton par celui-ci. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangementMotDePasseResponse {
    private String message;
    private String token;
}
