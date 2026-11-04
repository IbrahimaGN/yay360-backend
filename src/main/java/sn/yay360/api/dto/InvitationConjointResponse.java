package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Résultat d'une invitation du conjoint. Le code est renvoyé à la femme qui invite :
 * elle peut le transmettre elle-même (SMS, WhatsApp) si l'email n'est pas configuré.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvitationConjointResponse {
    private String message;
    private String code;
    private LocalDateTime dateExpiration;
    private boolean emailEnvoye;
}
