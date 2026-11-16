package com.biddly.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * R25: an amount in whole rupees. {@code 1000} and {@code 1000.00} are valid; {@code 1000.50} is not.
 * {@code @Digits(fraction = 0)} can't express this, because it also rejects {@code 1000.00}.
 * Null is valid; combine with {@code @NotNull}.
 */
@Documented
@Constraint(validatedBy = WholeRupees.Validator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface WholeRupees {

    String message() default "must be in whole rupees";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<WholeRupees, BigDecimal> {
        @Override
        public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
            return value == null || value.signum() == 0 || value.stripTrailingZeros().scale() <= 0;
        }
    }
}
