package com.example.bookingappbs.dto.booking;

import com.example.bookingappbs.validation.CheckoutLaterCheckin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@CheckoutLaterCheckin
public record CreateBookingRequestDto(
        @NotNull(message = "Check-in date cannot be null")
        @FutureOrPresent(message = "Check-in date must be today or in the future")
        LocalDate checkInDate,
        @NotNull(message = "Check-out date cannot be null")
        LocalDate checkOutDate,
        @NotNull(message = "Accommodation ID cannot be null")
        Long accommodationId
) implements DateRange {
}
