package com.example.bookingappbs.dto.accommodation;

import com.example.bookingappbs.dto.address.AddressDto;
import com.example.bookingappbs.model.Accommodation.Type;
import java.math.BigDecimal;
import java.util.List;

public record AccommodationDto(
        Long id,
        Type type,
        AddressDto location,
        String size,
        List<String> amenities,
        BigDecimal dailyRate,
        int availability
) {
}
