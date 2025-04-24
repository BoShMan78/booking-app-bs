package com.example.bookingappbs.dto.user;

import com.example.bookingappbs.validation.EmailUnique;
import com.example.bookingappbs.validation.FieldMatch;
import com.example.bookingappbs.validation.RegexConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@FieldMatch(firstString = "password",
        secondString = "repeatPassword",
        message = "The password fields must match")
public record UserRegistrationRequestDto(
        @NotBlank
        @Email(message = "Email is not valid",
                regexp = RegexConstants.EMAIL_REGEX)
        @EmailUnique
        String email,
        @NotBlank
        @Pattern(regexp = RegexConstants.PASSWORD_REGEX)
        String password,
        @NotBlank
        String repeatPassword,
        @NotBlank
        String firstName,
        @NotBlank
        String lastName
) {
}
