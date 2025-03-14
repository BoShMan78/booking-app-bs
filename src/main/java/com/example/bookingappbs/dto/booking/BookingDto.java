package com.example.bookingappbs.dto.booking;

import java.time.LocalDate;

public record BookingDto(
        Long id,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Long accommodationId,
        Long userId,
        String status
) {
}
