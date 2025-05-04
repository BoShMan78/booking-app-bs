package com.example.bookingappbs.validation;

import com.example.bookingappbs.dto.booking.DateRange;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CheckoutLaterValidator
        implements ConstraintValidator<CheckoutLaterCheckin, DateRange> {
    @Override
    public boolean isValid(DateRange dateRange, ConstraintValidatorContext context) {
        if (dateRange.checkInDate() == null || dateRange.checkOutDate() == null) {
            return true;
        }
        return dateRange.checkOutDate().isAfter(dateRange.checkInDate());
    }
}
