package com.example.bookingappbs.service.user;

import com.example.bookingappbs.dto.user.AddUserRoleRequestDto;
import com.example.bookingappbs.dto.user.UpdateCurrentUserRequestDto;
import com.example.bookingappbs.dto.user.UserRegistrationRequestDto;
import com.example.bookingappbs.dto.user.UserResponseDto;
import com.example.bookingappbs.exception.EntityNotFoundException;
import com.example.bookingappbs.mapper.UserMapper;
import com.example.bookingappbs.model.Role;
import com.example.bookingappbs.model.Role.RoleNames;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.model.UserRole;
import com.example.bookingappbs.repository.RoleRepository;
import com.example.bookingappbs.repository.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class UserServiceImpl implements UserService {
    private static final Logger logger = LogManager.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Override
    public UserResponseDto register(UserRegistrationRequestDto requestDto) {
        logger.info("Processing registration for user with email: {}", requestDto.email());

        Role role = roleRepository.findByName(RoleNames.CUSTOMER).orElseThrow(() ->
                new EntityNotFoundException("Role '" + UserRole.CUSTOMER.name()
                        + "' not found in database"));
        User user = userMapper.toModel(requestDto, passwordEncoder);
        user.setPassword(passwordEncoder.encode(requestDto.password()));
        user.setRoles(Set.of(role));

        User savedUser = userRepository.save(user);
        UserResponseDto dto = userMapper.toDto(savedUser);

        logger.info("User registered successfully with ID: {}", savedUser.getId());
        return dto;
    }

    @Override
    public UserResponseDto addAdminRoleToUser(Long id, AddUserRoleRequestDto requestDto) {
        logger.info("Updating role for user ID: {} to role ID: {}", id, requestDto.roleId());
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new EntityNotFoundException("Can't find user by id: " + id));
        Role role = roleRepository.findById(requestDto.roleId())
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: "
                        + requestDto.roleId()));

        user.getRoles().clear();
        user.getRoles().add(role);

        User savedUser = userRepository.save(user);
        UserResponseDto dto = userMapper.toDto(savedUser);

        logger.info("User role updated successfully for user ID: {}", id);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUser(User user) {
        logger.info("Retrieving user information for user ID: {}", user.getId());
        User userFromDB = userRepository.findByIdWithRoles(user.getId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Can't find user by id: " + user.getId()));
        UserResponseDto dto = userMapper.toDto(userFromDB);

        logger.info("User information retrieved successfully for user ID: {}", user.getId());
        return dto;
    }

    @Override
    public UserResponseDto updateCurrentUserPatch(User currentUser,
                                                  UpdateCurrentUserRequestDto requestDto) {
        logger.info("Updating profile for user ID: {} with data: {}", currentUser.getId(),
                requestDto);
        User existingUser = userRepository.findById(currentUser.getId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Can't find current user in DB"));

        userMapper.updateUserFromDto(requestDto, existingUser, passwordEncoder);

        User savedUser = userRepository.save(existingUser);
        UserResponseDto dto = userMapper.toDto(savedUser);

        logger.info("User profile updated successfully for user ID: {}", currentUser.getId());
        return dto;
    }
}
