package sn.yay360.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class TelephoneSenegalValidator implements ConstraintValidator<TelephoneSenegal, String> {

    private static final Pattern PATTERN = Pattern.compile("^\\+221(70|71|75|76|77|78)[0-9]{7}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // laisser @NotBlank gérer le caractère obligatoire si besoin
        }
        return PATTERN.matcher(value).matches();
    }
}
