package com.example.bookingappbs.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bookingappbs.dto.user.AddUserRoleRequestDto;
import com.example.bookingappbs.dto.user.UpdateCurrentUserRequestDto;
import com.example.bookingappbs.model.Role;
import com.example.bookingappbs.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserControllerTest {
    protected MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long userId;
    private String email;
    private User mockUser;
    private AddUserRoleRequestDto addUserRoleRequestDto;
    private Role customerRole;

    @BeforeEach
    void setUp(@Autowired WebApplicationContext context,
            @Autowired DataSource dataSource) throws SQLException {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        teardown(dataSource);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("database/users/add-test-user.sql")
            );
        }

        userId = 1L;
        email = "test@example.com";

        customerRole = new Role("CUSTOMER");

        mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail(email);
        mockUser.setRoles(Set.of(customerRole));

        addUserRoleRequestDto = new AddUserRoleRequestDto(1L);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                mockUser, null, mockUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    static void teardown(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("database/users/drop-all-test-users.sql")
            );
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Add ADMIN role to user by ADMIN ID - ADMIN only")
    void addAdminToUser() throws Exception {
        // Given
        Long targetUserId = 1L;
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
        // When & Then
        mockMvc.perform(put("/users/{id}/role", targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addUserRoleRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(targetUserId.intValue())))
                .andExpect(jsonPath("$.roleIds").isArray())
                .andExpect(jsonPath("$.roleIds.length()", is(1)));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"CUSTOMER"})
    @DisplayName("Get current user - Authenticated")
    void getCurrentUser_AuthenticatedUser_ShouldReturnOkAndUserDetails() throws Exception {
        // When & Then
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId.intValue())))
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.firstName", is("Test")))
                .andExpect(jsonPath("$.lastName", is("User")))
                .andExpect(jsonPath("$.roleIds").isArray())
                .andExpect(jsonPath("$.roleIds[0]", is(2)));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"CUSTOMER"})
    @DisplayName("Update current user - Valid request")
    void updateCurrentUser_ValidRequest_ShouldReturnUpdatedUserDetails() throws Exception {
        // Given
        String updatedFirstName = "Updated";
        UpdateCurrentUserRequestDto updateDto = new UpdateCurrentUserRequestDto(
                updatedFirstName,
                "User",
                null,
                "newPassword#1"
        );

        // When & Then
        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId.intValue())))
                .andExpect(jsonPath("$.firstName", is(updatedFirstName)))
                .andExpect(jsonPath("$.lastName", is("User")))
                .andExpect(jsonPath("$.email", is("test@example.com")))
                .andExpect(jsonPath("$.roleIds").isArray())
                .andExpect(jsonPath("$.roleIds[0]", is(2)));
    }
}
