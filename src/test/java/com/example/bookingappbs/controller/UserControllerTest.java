package com.example.bookingappbs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bookingappbs.dto.user.UpdateCurrentUserRequestDto;
import com.example.bookingappbs.dto.user.UpdateUserRoleRequestDto;
import com.example.bookingappbs.dto.user.UserResponseDto;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.model.User.Role;
import com.example.bookingappbs.service.notification.TelegramService;
import com.example.bookingappbs.service.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.telegram.telegrambots.meta.TelegramBotsApi;

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;
    @MockBean
    private TelegramBotsApi telegramBotsApi;
    @MockBean
    private TelegramService telegramService;

    @Autowired
    private ObjectMapper objectMapper;

    private Long userId;
    private String email;
    private User mockUser;
    private UserResponseDto userResponseDto;
    private UpdateUserRoleRequestDto updateUserRoleRequestDto;
    private UpdateCurrentUserRequestDto updateCurrentUserRequestDto;

    @BeforeEach
    void setUp() {
        userId = 1L;
        email = "test@example.com";

        mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail(email);
        mockUser.setRole(Role.CUSTOMER);

        userResponseDto = new UserResponseDto(
                userId,
                email,
                "FirstName",
                "LastName",
                Role.CUSTOMER.name()
        );

        updateUserRoleRequestDto = new UpdateUserRoleRequestDto(Role.ADMIN);

        updateCurrentUserRequestDto = new UpdateCurrentUserRequestDto(
                "Updated",
                "LastName",
                "updated@example.com",
                "Password#123"
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                mockUser, null, mockUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUserRole_AdminUser_ShouldReturnOkAndUserResponse() throws Exception {
        // Given
        UserResponseDto adminResponseDto = new UserResponseDto(
                userId,
                null,
                null,
                null,
                updateUserRoleRequestDto.role().name()
        );

        //When
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "adminUser", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Mockito.when(userService.updateUserRole(eq(userId), any(UpdateUserRoleRequestDto.class)))
                .thenReturn(adminResponseDto);

        //Then
        mockMvc.perform(MockMvcRequestBuilders.put("/users/{id}/role", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserRoleRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.role").value(updateUserRoleRequestDto.role().name()));

        Mockito.verify(userService)
                .updateUserRole(eq(userId), any(UpdateUserRoleRequestDto.class));
    }


    @Test
    void getCurrentUser_AuthenticatedUser_ShouldReturnOkAndUserResponse() throws Exception {
        // Given
        Mockito.when(userService.getUser(mockUser)).thenReturn(userResponseDto);

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value(userResponseDto.firstName()))
                .andExpect(jsonPath("$.lastName").value(userResponseDto.lastName()))
                .andExpect(jsonPath("$.role").value(userResponseDto.role()));

        Mockito.verify(userService).getUser(mockUser);
    }

    @Test
    @WithMockUser(roles = {"CUSTOMER"})
    void updateCurrentUser_ValidRequest_ShouldReturnUpdatedUser() throws Exception {
        //Given
        UserResponseDto updatedResponseDto = new UserResponseDto(
                userId,
                updateCurrentUserRequestDto.email(),
                updateCurrentUserRequestDto.firstName(),
                updateCurrentUserRequestDto.lastName(),
                Role.CUSTOMER.toString()
        );

        // When
        Mockito.when(userService.updateCurrentUserPatch(eq(mockUser),
                any(UpdateCurrentUserRequestDto.class))).thenReturn(updatedResponseDto);

        //Then
        mockMvc.perform(MockMvcRequestBuilders.patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCurrentUserRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(updateCurrentUserRequestDto.email()))
                .andExpect(jsonPath("$.firstName").value(updateCurrentUserRequestDto.firstName()))
                .andExpect(jsonPath("$.lastName").value(updateCurrentUserRequestDto.lastName()));

        Mockito.verify(userService).updateCurrentUserPatch(eq(mockUser), any(UpdateCurrentUserRequestDto.class));
    }
}
