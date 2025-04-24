package com.example.bookingappbs.dto.address;

import jakarta.validation.constraints.NotBlank;

public record CreateAddressRequestDto(
        @NotBlank(message = "Country cannot be blank")
        String country,
        @NotBlank(message = "City cannot be blank")
        String city,
        @NotBlank(message = "Street cannot be blank")
        String street,
        @NotBlank(message = "House number cannot be blank")
        String house,
        Integer apartment
) {
}
