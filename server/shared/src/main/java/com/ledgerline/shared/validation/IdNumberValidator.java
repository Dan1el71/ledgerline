package com.ledgerline.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class IdNumberValidator implements ConstraintValidator<IdNumber, String> {

    private static final Pattern FORMAT =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{4,18}[A-Za-z0-9]");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || isValid(value);
    }

    /**
     * Accepts a generic government or document ID: 6-20 alphanumeric characters or hyphens, at
     * least one digit, no leading or trailing hyphen and not a single repeated character.
     */
    public static boolean isValid(String id) {
        if (id == null) {
            return false;
        }
        String value = id.trim();
        if (value.isEmpty() || !FORMAT.matcher(value).matches()) {
            return false;
        }
        if (value.chars().noneMatch(Character::isDigit)) {
            return false;
        }
        return value.chars().distinct().count() > 1;
    }
}
