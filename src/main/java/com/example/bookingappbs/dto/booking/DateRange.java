package com.example.bookingappbs.dto.booking;

import java.time.LocalDate;

public interface DateRange {
    LocalDate checkInDate();

    LocalDate checkOutDate();
}
