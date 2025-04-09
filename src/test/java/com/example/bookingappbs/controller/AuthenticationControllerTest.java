package com.example.bookingappbs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.bookingappbs.dto.user.UserLoginRequestDto;
import com.example.bookingappbs.dto.user.UserLoginResponseDto;
import com.example.bookingappbs.dto.user.UserRegistrationRequestDto;
import com.example.bookingappbs.dto.user.UserResponseDto;
import com.example.bookingappbs.exception.RegistrationException;
import com.example.bookingappbs.model.User.Role;
import com.example.bookingappbs.security.AuthenticationService;
import com.example.bookingappbs.service.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthenticationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private UserService userService;
    @MockBean
    private AuthenticationService authenticationService;

    private UserRegistrationRequestDto registrationDto;
    private UserResponseDto registrationResponseDto;
    private UserLoginRequestDto loginDto;
    private UserLoginResponseDto loginResponseDto;
    private RegistrationException registrationException;
    private RuntimeException loginException;

    @BeforeEach
    void setUp() {
        registrationDto = new UserRegistrationRequestDto(
                "test@example.com",
                "Password#1",
                "Password#1",
                "John",
                "Doe"
        );
        registrationResponseDto = new UserResponseDto(
                1L,
                "test@example.com",
                "John",
                "Doe",
                Role.CUSTOMER.toString()
        );
        loginDto = new UserLoginRequestDto("test@example.com", "Password#1");
        loginResponseDto = new UserLoginResponseDto("test-jwt-token");
    }

    @Test
    @DisplayName("POST /register - successful registration")
    void register_ValidInput_ReturnsCreatedUserResponse() throws Exception {
        // When
        when(userService.register(any(UserRegistrationRequestDto.class)))
                .thenReturn(registrationResponseDto);

        // Then
        mockMvc.perform(MockMvcRequestBuilders.post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("test@example.com"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value("John"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value("Doe"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value(Role.CUSTOMER.name()));

        Mockito.verify(userService).register(any(UserRegistrationRequestDto.class));
    }

    @Test
    @DisplayName("POST /login - successful login")
    void login_ValidCredentials_ReturnsLoginResponse() throws Exception {
        // When
        when(authenticationService.authenticate(any(UserLoginRequestDto.class)))
                .thenReturn(loginResponseDto);

        // Then
        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.token").value("test-jwt-token"));

        Mockito.verify(authenticationService).authenticate(any(UserLoginRequestDto.class));
    }

}
