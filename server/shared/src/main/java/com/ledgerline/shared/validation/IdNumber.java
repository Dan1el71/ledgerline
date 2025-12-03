package com.ledgerline.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A generic identity-document number: 6-20 letters, digits or hyphens, with at least one digit. */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IdNumberValidator.class)
public @interface IdNumber {
    String message() default "Must be a valid ID number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
