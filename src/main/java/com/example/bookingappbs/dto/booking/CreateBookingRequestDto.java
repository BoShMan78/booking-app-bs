package com.example.bookingappbs.dto.booking;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateBookingRequestDto(
        //TODO: check all DTO validation annotaions
        @NotNull(message = "Check-in date can not be null")
        LocalDate checkInDate,
        @NotNull(message = "Check-out date can not be null")
        LocalDate checkOutDate,
        @NotNull(message = "Accommodation Id can not be null")
        Long accommodationId
) {
}
