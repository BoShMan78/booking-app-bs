package com.example.bookingappbs.dto.user;

import jakarta.validation.constraints.Pattern;

public record UpdateCurrentUserRequestDto(
        String firstName,
        String lastName,
        String email,
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)"
                + "(?=.*[#$@!%&*?])[A-Za-z\\d#$@!%&*?]{8,}$")
        String password
) {
}
