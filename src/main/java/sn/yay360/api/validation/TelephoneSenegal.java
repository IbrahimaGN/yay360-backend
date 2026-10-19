package sn.yay360.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valide qu'un numéro de téléphone respecte le format mobile sénégalais strict :
 * +221 suivi de l'un des préfixes opérateurs (70, 71, 75, 76, 77, 78) puis 7 chiffres.
 * Un champ vide/null est considéré valide ; combiner avec @NotBlank si le champ est obligatoire.
 */
@Documented
@Constraint(validatedBy = TelephoneSenegalValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TelephoneSenegal {

    String message() default "Numéro de téléphone sénégalais invalide (format attendu : +221 suivi de 70, 71, 75, 76, 77 ou 78 puis 7 chiffres)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
