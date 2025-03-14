package com.example.bookingappbs.dto.accommodation;

import com.example.bookingappbs.model.Accommodation.Type;
import com.example.bookingappbs.model.Address;
import java.math.BigDecimal;
import java.util.List;

public record UpdateAccommodationRequestDto(
        Type type,
        Address location,
        String size,
        List<String> amenities,
        BigDecimal dailyRate,
        int availability
) {
}
