package com.example.bookingappbs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.example.bookingappbs.dto.user.UpdateCurrentUserRequestDto;
import com.example.bookingappbs.dto.user.UpdateUserRoleRequestDto;
import com.example.bookingappbs.dto.user.UserRegistrationRequestDto;
import com.example.bookingappbs.dto.user.UserResponseDto;
import com.example.bookingappbs.exception.EntityNotFoundException;
import com.example.bookingappbs.mapper.UserMapper;
import com.example.bookingappbs.model.Role;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.repository.RoleRepository;
import com.example.bookingappbs.repository.UserRepository;
import com.example.bookingappbs.service.user.UserServiceImpl;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @InjectMocks
    private UserServiceImpl userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserRegistrationRequestDto registrationRequestDto;
    private User userToSave;
    private User savedUser;
    private UserResponseDto expectedUserResponseDto;
    private UpdateUserRoleRequestDto updateUserRoleRequestDto;
    private User existingUser;
    private User updatedUserWithRole;
    private User currentUser;
    private UpdateCurrentUserRequestDto updateCurrentUserRequestDto;
    private Role customerRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        registrationRequestDto = new UserRegistrationRequestDto(
                "test@example.com",
                "Password#1",
                "Password#1",
                "John",
                "Doe"
        );
        customerRole = new Role("CUSTOMER");
        adminRole = new Role("ADMIN");
        userToSave = new User()
                .setEmail(registrationRequestDto.email())
                .setFirstName(registrationRequestDto.firstName())
                .setLastName(registrationRequestDto.lastName())
                .setRoles(new HashSet<>(Set.of(customerRole)));
        savedUser = new User()
                .setId(1L)
                .setEmail(registrationRequestDto.email())
                .setFirstName(registrationRequestDto.firstName())
                .setLastName(registrationRequestDto.lastName())
                .setPassword("encodedPassword")
                .setRoles(new HashSet<>(Set.of(customerRole)));
        expectedUserResponseDto = new UserResponseDto(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getRoles().stream().map(Object::toString).toList()
        );
        updateUserRoleRequestDto = new UpdateUserRoleRequestDto(1L);
        existingUser = new User()
                .setId(1L)
                .setEmail("test@example.com")
                .setFirstName("John")
                .setLastName("Doe")
                .setRoles(new HashSet<>(Set.of(customerRole)));
        updatedUserWithRole = new User()
                .setId(1L)
                .setEmail("test@example.com")
                .setFirstName("John")
                .setLastName("Doe")
                .setRoles(new HashSet<>(Set.of(adminRole)));
        currentUser = new User().setId(1L).setEmail("old@example.com");
        updateCurrentUserRequestDto = new UpdateCurrentUserRequestDto(
                "Jane",
                "Doe",
                "new@example.com",
                "NewPassword#1"
        );
    }

    @Test
    @DisplayName("Verify register() method works and saves new user")
    public void register_ValidRequestDto_ReturnsUserResponseDto() {
        // Given
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(userMapper.toModel(registrationRequestDto, passwordEncoder)).thenReturn(userToSave);
        when(passwordEncoder.encode(registrationRequestDto.password()))
                .thenReturn("encodedPassword");
        when(userRepository.save(userToSave)).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(expectedUserResponseDto);

        // When
        UserResponseDto actualDto = userService.register(registrationRequestDto);

        // Then
        assertThat(actualDto).isEqualTo(expectedUserResponseDto);
        verify(roleRepository, times(1)).findByName("CUSTOMER");
        verify(userMapper, times(1)).toModel(registrationRequestDto, passwordEncoder);
        verify(passwordEncoder, times(1)).encode(registrationRequestDto.password());
        verify(userRepository, times(1)).save(userToSave);
        verify(userMapper, times(1)).toDto(savedUser);
        verifyNoMoreInteractions(userRepository, userMapper, passwordEncoder);
    }

    @Test
    @DisplayName("register should throw EntityNotFoundException if 'CUSTOMER' role is not found")
    void register_CustomerRoleNotFound_ThrowsEntityNotFoundException() {
        // When
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.register(registrationRequestDto));

        // Then
        assertEquals("Role 'CUSTOMER' not found in database", exception.getMessage());
        verify(roleRepository).findByName("CUSTOMER");
        verifyNoInteractions(userRepository, userMapper, passwordEncoder);
    }

    @Test
    @DisplayName("Verify updateUserRole() method works and updates user role")
    public void updateUserRole_ValidIdAndRequestDto_ReturnsUpdatedUserResponseDto() {
        // Given
        when(userRepository.findByIdWithRoles(anyLong())).thenReturn(Optional.of(existingUser));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenReturn(updatedUserWithRole);
        when(userMapper.toDto(any(User.class))).thenReturn(new UserResponseDto(
                updatedUserWithRole.getId(),
                updatedUserWithRole.getEmail(),
                updatedUserWithRole.getFirstName(),
                updatedUserWithRole.getLastName(),
                updatedUserWithRole.getRoles().stream().map(Role::getName).toList()
        ));

        // When
        UserResponseDto actualDto = userService.updateUserRole(1L, updateUserRoleRequestDto);

        // Then
        assertThat(actualDto.roles()).contains(adminRole.getName());
        verify(userRepository, times(1)).findByIdWithRoles(1L);
        verify(roleRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(existingUser);
        verify(userMapper, times(1)).toDto(updatedUserWithRole);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    @DisplayName("Verify getUser() method works and returns UserResponseDto")
    public void getUser_ValidUser_ReturnsUserResponseDto() {
        // Given
        when(userRepository.findByIdWithRoles(currentUser.getId()))
                .thenReturn(Optional.of(existingUser));
        when(userMapper.toDto(existingUser)).thenReturn(new UserResponseDto(
                existingUser.getId(),
                existingUser.getEmail(),
                existingUser.getFirstName(),
                existingUser.getLastName(),
                existingUser.getRoles().stream().map(Object::toString).toList()
        ));

        // When
        UserResponseDto actualDto = userService.getUser(currentUser);

        // Then
        assertThat(actualDto).isEqualTo(expectedUserResponseDto);
        verify(userRepository, times(1)).findByIdWithRoles(currentUser.getId());
        verify(userMapper, times(1)).toDto(existingUser);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    @DisplayName("Verify updateCurrentUserPatch() method updates existing user fields")
    public void updateCurrentUserPatch_ValidRequestDto_ReturnsUpdatedUserResponseDto() {
        // Given
        User updatedLocalUser = new User()
                .setId(currentUser.getId())
                .setEmail(updateCurrentUserRequestDto.email())
                .setFirstName(updateCurrentUserRequestDto.firstName())
                .setLastName(updateCurrentUserRequestDto.lastName())
                .setPassword("newEncodedPassword")
                .setRoles(Set.of(customerRole));
        UserResponseDto expectedUpdatedDto = new UserResponseDto(
                updatedLocalUser.getId(),
                updatedLocalUser.getEmail(),
                updatedLocalUser.getFirstName(),
                updatedLocalUser.getLastName(),
                updatedLocalUser.getRoles().stream().map(Object::toString).toList()
        );

        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedLocalUser);
        when(userMapper.toDto(any(User.class))).thenReturn(expectedUpdatedDto);

        // When
        UserResponseDto actualDto = userService
                .updateCurrentUserPatch(currentUser, updateCurrentUserRequestDto);

        // Then
        assertThat(actualDto).isEqualTo(expectedUpdatedDto);
        verify(userRepository, times(1)).findById(currentUser.getId());
        verify(userMapper, times(1))
                .updateUserFromDto(updateCurrentUserRequestDto, existingUser, passwordEncoder);
        verify(userRepository, times(1)).save(existingUser);
        verify(userMapper, times(1)).toDto(updatedLocalUser); ;
        verifyNoMoreInteractions(userRepository, userMapper);
    }
}
