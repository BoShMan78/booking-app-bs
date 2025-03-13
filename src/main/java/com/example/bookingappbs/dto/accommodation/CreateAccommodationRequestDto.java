package com.example.bookingappbs.dto.accommodation;

import com.example.bookingappbs.model.Address;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record CreateAccommodationRequestDto(
        @NotBlank(message = "Type cannot be blank")
        String type,
        @NotNull(message = "Location cannot be null")
        Address location,
        String size,
        List<String> amenities,
        @NotNull(message = "Daily rate cannot be null")
        @DecimalMin(value = "0.0", inclusive = false, message = "Daily rate must be greater than 0")
        BigDecimal dailyRate,
        @Min(value = 1, message = "Availability must be at least 1")
        int availability
) {
}
