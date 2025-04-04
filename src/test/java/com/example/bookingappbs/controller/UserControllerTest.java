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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.telegram.telegrambots.meta.TelegramBotsApi;

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

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUserRole_AdminUser_ShouldReturnOkAndUserResponse() throws Exception {
        // Given
        Long userId = 1L;
        UpdateUserRoleRequestDto requestDto = new UpdateUserRoleRequestDto(Role.ADMIN);
        UserResponseDto responseDto = new UserResponseDto(
                userId,
                null,
                null,
                null,
                requestDto.role().name()
        );

        //When
        Mockito.when(userService.updateUserRole(eq(userId), any(UpdateUserRoleRequestDto.class)))
                .thenReturn(responseDto);

        //Then
        mockMvc.perform(MockMvcRequestBuilders.put("/users/{id}/role", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.role").value(requestDto.role().name()));

        Mockito.verify(userService)
                .updateUserRole(eq(userId), any(UpdateUserRoleRequestDto.class));
    }


    @Test
    void getCurrentUser_AuthenticatedUser_ShouldReturnOkAndUserResponse() throws Exception {
        // Given
        Long userId = 2L;
        String email = "user@example.com";
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail(email);
        mockUser.setRole(Role.CUSTOMER);

        UserResponseDto responseDto = new UserResponseDto(
                mockUser.getId(),
                mockUser.getEmail(),
                null,
                null,
                mockUser.getRole().name()
        );


        // When
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                mockUser, null, mockUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Mockito.when(userService.getUser(mockUser)).thenReturn(responseDto);

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(email));

        Mockito.verify(userService).getUser(mockUser);
    }

    @Test
    @WithMockUser(roles = {"CUSTOMER"})
    void updateCurrentUser_ValidRequest_ShouldReturnUpdatedUser() throws Exception {
        //Given
        Long userId = 1L;
        String email = "user@example.com";

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail(email);
        mockUser.setRole(Role.CUSTOMER);

        UpdateCurrentUserRequestDto requestDto = new UpdateCurrentUserRequestDto(
                "Updated",
                "LastName",
                "email@example.com",
                "Password#123"
        );

        UserResponseDto responseDto = new UserResponseDto(
                userId,
                requestDto.email(),
                requestDto.firstName(),
                requestDto.lastName(),
                Role.CUSTOMER.toString()
        );

        // When
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                mockUser, null, mockUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Mockito.when(userService.updateCurrentUserPatch(eq(mockUser),
                        any(UpdateCurrentUserRequestDto.class))).thenReturn(responseDto);

        //Then
        mockMvc.perform(MockMvcRequestBuilders.patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(requestDto.email()))
                .andExpect(jsonPath("$.firstName").value(requestDto.firstName()))
                .andExpect(jsonPath("$.lastName").value(requestDto.lastName()));

        Mockito.verify(userService).updateCurrentUserPatch(eq(mockUser), any(UpdateCurrentUserRequestDto.class));
    }
}
