package com.example.bookingappbs.dto.user;

import com.example.bookingappbs.model.User.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequestDto(
        @NotNull
        Role role
) {
}
