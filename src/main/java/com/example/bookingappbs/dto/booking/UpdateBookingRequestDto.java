package com.example.bookingappbs.dto.booking;

import jakarta.validation.constraints.FutureOrPresent;
import java.time.LocalDate;

public record UpdateBookingRequestDto(
        @FutureOrPresent(message = "Check-in date must be today or in the future")
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Long accommodationId,
        String status
) {
}
