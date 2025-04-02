package com.example.bookingappbs.dto.address;

public record AddressDto(
        Long id,
        String country,
        String city,
        String street,
        String house,
        Integer apartment
) {
}
