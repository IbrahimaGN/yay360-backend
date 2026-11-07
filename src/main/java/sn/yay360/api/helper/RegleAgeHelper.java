package sn.yay360.api.helper;

import sn.yay360.api.model.RoleUtilisateur;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

/**
 * Règles d'âge du cahier des charges (§4.1) : 13 ans pour une femme enceinte,
 * 18 ans pour le conjoint et l'administrateur, 21 ans pour un soignant diplômé.
 */
public final class RegleAgeHelper {

    private static final Map<String, Integer> AGE_MINIMUM_PAR_ROLE = Map.of(
            RoleUtilisateur.FEMME_ENCEINTE, 13,
            RoleUtilisateur.CONJOINT, 18,
            RoleUtilisateur.MEDECIN, 21,
            RoleUtilisateur.SAGE_FEMME, 21,
            RoleUtilisateur.ADMIN, 18
    );

    private static final int AGE_MAXIMUM_PLAUSIBLE = 120;

    private RegleAgeHelper() {
    }

    public static void valider(String role, LocalDate dateNaissance) {
        if (dateNaissance == null) {
            throw new IllegalArgumentException("La date de naissance est obligatoire");
        }
        int age = Period.between(dateNaissance, LocalDate.now()).getYears();

        if (age > AGE_MAXIMUM_PLAUSIBLE) {
            throw new IllegalArgumentException("La date de naissance semble invalide");
        }

        Integer ageMinimum = AGE_MINIMUM_PAR_ROLE.get(role);
        if (ageMinimum != null && age < ageMinimum) {
            throw new IllegalArgumentException(
                    "L'âge minimum requis pour le rôle " + role + " est de " + ageMinimum + " ans");
        }
    }
}
