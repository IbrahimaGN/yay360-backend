package sn.yay360.api.model;

/**
 * Constantes des rôles de la plateforme YAY360.
 * Les rôles sont stockés en String (pas d'enum JPA) pour rester
 * cohérent avec le pattern déjà utilisé sur YaY360 / ZENTO.
 */
public class RoleUtilisateur {

    public static final String ADMIN = "ADMIN";
    public static final String FEMME_ENCEINTE = "FEMME_ENCEINTE";
    public static final String CONJOINT = "CONJOINT";
    public static final String SAGE_FEMME = "SAGE_FEMME";
    public static final String MEDECIN = "MEDECIN";

    private RoleUtilisateur() {
        // classe utilitaire non instanciable
    }
}
