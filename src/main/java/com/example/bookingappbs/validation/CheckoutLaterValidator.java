package com.example.bookingappbs.validation;

import com.example.bookingappbs.model.Booking;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CheckoutLaterValidator implements ConstraintValidator<CheckoutLaterCheckin, Booking> {
    @Override
    public boolean isValid(Booking booking, ConstraintValidatorContext context) {
        if (booking.getCheckInDate() == null || booking.getCheckOutDate() == null) {
            return true;
        }
        return booking.getCheckOutDate().isAfter(booking.getCheckInDate());
    }
}
