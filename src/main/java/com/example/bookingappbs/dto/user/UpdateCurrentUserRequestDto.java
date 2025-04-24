package com.example.bookingappbs.dto.user;

import com.example.bookingappbs.validation.EmailUnique;
import com.example.bookingappbs.validation.RegexConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record UpdateCurrentUserRequestDto(
        String firstName,
        String lastName,
        @Email(message = "Email is not valid",
                regexp = RegexConstants.EMAIL_REGEX)
        @EmailUnique
        String email,
        @Pattern(regexp = RegexConstants.PASSWORD_REGEX)
        String password
) {
}
