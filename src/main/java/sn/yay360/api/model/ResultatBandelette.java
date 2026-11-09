package sn.yay360.api.model;

import java.util.List;

/** Résultat semi-quantitatif d'une bandelette urinaire (protéinurie, glycosurie), du plus faible au plus fort. */
public final class ResultatBandelette {

    public static final String NEGATIVE = "NEGATIVE";
    public static final String TRACES = "TRACES";
    public static final String UNE_CROIX = "UNE_CROIX";
    public static final String DEUX_CROIX = "DEUX_CROIX";
    public static final String TROIS_CROIX = "TROIS_CROIX";

    /** Ordre croissant, pour comparer deux résultats. */
    public static final List<String> ORDRE = List.of(NEGATIVE, TRACES, UNE_CROIX, DEUX_CROIX, TROIS_CROIX);

    /** Expression régulière de validation des requêtes. */
    public static final String MOTIF = "NEGATIVE|TRACES|UNE_CROIX|DEUX_CROIX|TROIS_CROIX";

    private ResultatBandelette() {
    }

    /** Vrai si {@code resultat} vaut au moins {@code seuil} (faux si le résultat est absent ou inconnu). */
    public static boolean auMoins(String resultat, String seuil) {
        if (resultat == null) {
            return false; // List.of(...).indexOf(null) lève une NullPointerException
        }
        int i = ORDRE.indexOf(resultat);
        return i >= 0 && i >= ORDRE.indexOf(seuil);
    }

    /** Écriture clinique courante : « − », « traces », « + », « ++ », « +++ ». */
    public static String libelle(String resultat) {
        return switch (resultat == null ? "" : resultat) {
            case NEGATIVE -> "négative";
            case TRACES -> "traces";
            case UNE_CROIX -> "+";
            case DEUX_CROIX -> "++";
            case TROIS_CROIX -> "+++";
            default -> "non renseignée";
        };
    }
}
