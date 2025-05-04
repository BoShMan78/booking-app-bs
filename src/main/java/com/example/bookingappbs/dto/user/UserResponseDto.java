package com.example.bookingappbs.dto.user;

import java.util.List;

public record UserResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        List<Long> roleIds
) {
}
