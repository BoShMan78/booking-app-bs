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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static final Logger logger = LogManager.getLogger(UserController.class);

    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/role")
    @Operation(summary = "Update user role",
            description = "Enables users to update their roles, providing role-based access")
    public UserResponseDto updateUserRole(
            @PathVariable @Positive Long id,
            @RequestBody @Valid UpdateUserRoleRequestDto requestDto
    ) {
        logger.info("Processing request to update role for user ID: {}. New role: {}",
                id, requestDto.role());
        UserResponseDto updatedUser = userService.updateUserRole(id, requestDto);

        logger.info("Role for user ID {} successfully updated to: {}", id, updatedUser.role());
        return updatedUser;
    }

    @GetMapping("/me")
    @Operation(summary = "Retrieves the profile information",
            description = "Retrieves the profile information for the currently logged-in user")
    public UserResponseDto getCurrentUser(@AuthenticationPrincipal User user) {
        logger.info("Retrieving profile information for current user ID: {}", user.getId());
        UserResponseDto currentUser = userService.getUser(user);

        logger.info("Profile information retrieved successfully for user ID: {}", user.getId());
        return currentUser;
    }

    @PatchMapping("/me")
    @Operation(summary = "Update current user information",
            description = "Allows users to update their profile information")
    public UserResponseDto updateCurrentUser(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateCurrentUserRequestDto requestDto
    ) {
        logger.info("Processing request to update profile for current user ID: {}. "
                + "Update data: {}", user.getId(), requestDto);
        UserResponseDto updatedUser = userService.updateCurrentUserPatch(user, requestDto);

        logger.info("Profile information for user ID {} successfully updated.", user.getId());
        return updatedUser;
    }
}
