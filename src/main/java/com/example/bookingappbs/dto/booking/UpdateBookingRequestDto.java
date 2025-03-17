package com.example.bookingappbs.dto.booking;

import java.time.LocalDate;

public record UpdateBookingRequestDto(
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Long accommodationId,
        String status
) {
}
