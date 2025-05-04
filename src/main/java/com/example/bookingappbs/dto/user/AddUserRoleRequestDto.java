package com.example.bookingappbs.dto.user;

import jakarta.validation.constraints.NotNull;

public record AddUserRoleRequestDto(
        @NotNull
        Long roleId
) {
}
