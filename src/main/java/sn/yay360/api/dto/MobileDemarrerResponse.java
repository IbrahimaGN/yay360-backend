package sn.yay360.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sn.yay360.api.validation.TelephoneSenegal;

/**
 * Réponse de l'étape 1 : {@code PIN} (compte avec PIN : saisir le PIN, aucun e-mail envoyé),
 * {@code CODE_ENVOYE} (compte sans PIN ou « PIN oublié » : code envoyé à son e-mail, affiché masqué)
 * ou {@code INSCRIPTION} (numéro inconnu : l'application affiche le formulaire d'inscription).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MobileDemarrerResponse {
    public static final String PIN = "PIN";
    public static final String CODE_ENVOYE = "CODE_ENVOYE";
    public static final String INSCRIPTION = "INSCRIPTION";

    private String etape;
    private String emailMasque;
    private String message;
}
