package com.example.bookingappbs.controller;

import com.example.bookingappbs.dto.user.UpdateCurrentUserRequestDto;
import com.example.bookingappbs.dto.user.UpdateUserRoleRequestDto;
import com.example.bookingappbs.dto.user.UserResponseDto;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User controller", description = "Managing authentication and user registration")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/users")
public class UserController {
    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/role")
    @Operation(summary = "Update user role",
            description = "Enables users to update their roles, providing role-based access")
    public UserResponseDto updateUserRole(@PathVariable @Positive Long id,
                                          @RequestBody @Valid UpdateUserRoleRequestDto requestDto) {
        return userService.updateUserRole(id, requestDto);
    }

    @GetMapping("/me")
    @Operation(summary = "Retrieves the profile information",
            description = "Retrieves the profile information for the currently logged-in user")
    public UserResponseDto getCurrentUser(@AuthenticationPrincipal User user) {
        return userService.getUser(user);
    }

    @PatchMapping("/me")
    @Operation(summary = "Update current user information",
            description = "Allows users to update their profile information")
    public UserResponseDto updateCurrentUser(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateCurrentUserRequestDto requestDto) {
        return userService.updateCurrentUserPatch(user, requestDto);
    }
}
