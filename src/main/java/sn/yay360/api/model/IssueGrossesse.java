package sn.yay360.api.model;

import java.util.Map;

/**
 * Issue d'une grossesse, saisie par le médecin à la clôture.
 *
 * <p>Aucune issue « avortement provoqué » n'existe volontairement : au Sénégal, l'avortement provoqué est
 * pénalement réprimé (Code pénal, art. 305), sauf avortement thérapeutique. Enregistrer cette cause pourrait
 * exposer la patiente. Une perte avant 22 SA est saisie comme {@link #FAUSSE_COUCHE} (fausse couche ou
 * avortement incomplet), sans en préciser la cause, ce qui couvre aussi les soins après avortement.</p>
 *
 * <p>Seuil de 22 SA : début de la mortinaissance dans la CIM-11 (l'OMS utilise 28 SA pour les comparaisons
 * internationales).</p>
 */
public final class IssueGrossesse {

    public static final String NAISSANCE_VIVANTE = "NAISSANCE_VIVANTE";
    public static final String MORT_NE = "MORT_NE";
    public static final String FAUSSE_COUCHE = "FAUSSE_COUCHE";
    public static final String GROSSESSE_EXTRA_UTERINE = "GROSSESSE_EXTRA_UTERINE";
    public static final String MORT_FOETALE_IN_UTERO = "MORT_FOETALE_IN_UTERO";
    public static final String INTERRUPTION_THERAPEUTIQUE = "INTERRUPTION_THERAPEUTIQUE";

    /** Expression régulière de validation des requêtes. */
    public static final String MOTIF = "NAISSANCE_VIVANTE|MORT_NE|FAUSSE_COUCHE|GROSSESSE_EXTRA_UTERINE"
            + "|MORT_FOETALE_IN_UTERO|INTERRUPTION_THERAPEUTIQUE";

    /** Seuil (en SA révolues) séparant fausse couche et mortinaissance / mort fœtale. */
    public static final int SEUIL_SA = 22;

    /** Statut de la grossesse correspondant à chaque issue. */
    private static final Map<String, String> STATUT = Map.of(
            NAISSANCE_VIVANTE, StatutGrossesse.TERMINEE,
            MORT_NE, StatutGrossesse.TERMINEE,
            FAUSSE_COUCHE, StatutGrossesse.INTERROMPUE,
            GROSSESSE_EXTRA_UTERINE, StatutGrossesse.INTERROMPUE,
            MORT_FOETALE_IN_UTERO, StatutGrossesse.INTERROMPUE,
            INTERRUPTION_THERAPEUTIQUE, StatutGrossesse.INTERROMPUE);

    private IssueGrossesse() {
    }

    public static String statut(String issue) {
        String statut = STATUT.get(issue);
        if (statut == null) {
            throw new IllegalArgumentException("Issue de grossesse inconnue : " + issue);
        }
        return statut;
    }

    /**
     * Vérifie que l'issue est compatible avec le terme atteint à la date de fin.
     *
     * @return null si cohérent, sinon le message d'erreur à afficher au médecin
     */
    public static String incoherence(String issue, int semainesRevolues) {
        if (FAUSSE_COUCHE.equals(issue) && semainesRevolues >= SEUIL_SA) {
            return "Une fausse couche correspond à une perte avant " + SEUIL_SA + " SA (terme à la date de fin : "
                    + semainesRevolues + " SA). Choisissez « mort fœtale in utero » ou « enfant mort-né ».";
        }
        if ((MORT_NE.equals(issue) || MORT_FOETALE_IN_UTERO.equals(issue)) && semainesRevolues < SEUIL_SA) {
            return "Avant " + SEUIL_SA + " SA (terme à la date de fin : " + semainesRevolues
                    + " SA), la perte est enregistrée comme fausse couche.";
        }
        return null;
    }
}
