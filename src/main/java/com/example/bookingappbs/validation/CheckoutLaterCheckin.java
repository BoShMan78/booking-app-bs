package com.example.bookingappbs.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = CheckoutLaterValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckoutLaterCheckin {
    String message() default "Check-out date must be later than check-in date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
