package com.example.bookingappbs.controller;

import com.example.bookingappbs.dto.user.UserLoginRequestDto;
import com.example.bookingappbs.dto.user.UserLoginResponseDto;
import com.example.bookingappbs.dto.user.UserRegistrationRequestDto;
import com.example.bookingappbs.dto.user.UserResponseDto;
import com.example.bookingappbs.exception.RegistrationException;
import com.example.bookingappbs.security.AuthenticationService;
import com.example.bookingappbs.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Endpoints for registration and login")
@RequiredArgsConstructor
@RestController
public class AuthenticationController {
    private static final Logger logger = LogManager.getLogger(AuthenticationController.class);

    private final UserService userService;
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @Operation(summary = "User registration",
            description = "Allows users to register a new account")
    public UserResponseDto register(@Valid @RequestBody UserRegistrationRequestDto requestDto)
            throws RegistrationException {
        logger.info("Processing registration request for user: {}", requestDto.email());
        UserResponseDto registeredUser = userService.register(requestDto);

        logger.info("User with email {} successfully registered.", registeredUser.email());
        return registeredUser;
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Grants JWT tokens to authenticated users")
    public UserLoginResponseDto login(@Valid @RequestBody UserLoginRequestDto requestDto) {
        logger.info("Processing login request for user: {}", requestDto.email());
        UserLoginResponseDto loginResponseDto = authenticationService.authenticate(requestDto);

        logger.info("User {} successfully logged in.", requestDto.email());
        return loginResponseDto;
    }
}
