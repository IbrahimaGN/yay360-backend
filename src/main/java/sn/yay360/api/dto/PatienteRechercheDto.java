package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Résultat minimal d'une recherche de patiente par un soignant (téléphone ou email exact) :
 * permet de rattacher le dossier sans exposer de donnée clinique.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatienteRechercheDto {
    private Long grossesseId;
    private String prenom;
    private String nom;
    private Integer semaineActuelle;
    private String etablissementSuiviNom;
    /** true si le soignant qui cherche suit déjà cette grossesse. */
    private boolean dejaSuivie;
}
