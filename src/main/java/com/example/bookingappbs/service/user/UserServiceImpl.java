package com.example.bookingappbs.service.user;

import com.example.bookingappbs.dto.user.UpdateCurrentUserRequestDto;
import com.example.bookingappbs.dto.user.UpdateUserRoleRequestDto;
import com.example.bookingappbs.dto.user.UserRegistrationRequestDto;
import com.example.bookingappbs.dto.user.UserResponseDto;
import com.example.bookingappbs.exception.EntityNotFoundException;
import com.example.bookingappbs.exception.RegistrationException;
import com.example.bookingappbs.mapper.UserMapper;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.model.User.Role;
import com.example.bookingappbs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private static final Logger logger = LogManager.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponseDto register(UserRegistrationRequestDto requestDto) {
        logger.info("Processing registration for user with email: {}", requestDto.email());
        if (userRepository.existsByEmail(requestDto.email())) {
            throw new RegistrationException(
                    "User with email: " + requestDto.email() + " already exist");
        }

        User user = userMapper.toModel(requestDto);
        user.setPassword(passwordEncoder.encode(requestDto.password()));
        user.setRole(Role.CUSTOMER);

        User savedUser = userRepository.save(user);
        UserResponseDto dto = userMapper.toDto(savedUser);

        logger.info("User registered successfully with ID: {}", savedUser.getId());
        return dto;
    }

    @Override
    @Transactional
    public UserResponseDto updateUserRole(Long id, UpdateUserRoleRequestDto requestDto) {
        logger.info("Updating role for user ID: {} to: {}", id, requestDto.role());
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Can't find user by id: " + id));

        user.setRole(requestDto.role());
        User savedUser = userRepository.save(user);
        UserResponseDto dto = userMapper.toDto(savedUser);

        logger.info("User role updated successfully for user ID: {}", id);
        return dto;
    }

    @Override
    public UserResponseDto getUser(User user) {
        logger.info("Retrieving user information for user ID: {}", user.getId());
        User userFromDB = userRepository.findById(user.getId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Can't find user by id: " + user.getId()));
        UserResponseDto dto = userMapper.toDto(userFromDB);

        logger.info("User information retrieved successfully for user ID: {}", user.getId());
        return dto;
    }

    @Override
    @Transactional
    public UserResponseDto updateCurrentUserPatch(User currentUser,
                                                  UpdateCurrentUserRequestDto requestDto) {
        logger.info("Updating profile for user ID: {} with data: {}", currentUser.getId(),
                requestDto);
        User existingUser = userRepository.findById(currentUser.getId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Can't find current user in DB"));

        if (requestDto.email() != null && !existingUser.getEmail().equals(requestDto.email())) {
            if (userRepository.existsByEmail(requestDto.email())) {
                throw new RegistrationException("User with email: " + requestDto.email()
                        + " already exist");
            }
            existingUser.setEmail(requestDto.email());
        }
        if (requestDto.firstName() != null) {
            existingUser.setFirstName(requestDto.firstName());
        }
        if (requestDto.lastName() != null) {
            existingUser.setLastName(requestDto.lastName());
        }
        if (requestDto.password() != null && !requestDto.password().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(requestDto.password()));
        }
        User savedUser = userRepository.save(existingUser);
        UserResponseDto dto = userMapper.toDto(savedUser);

        logger.info("User profile updated successfully for user ID: {}", currentUser.getId());
        return dto;
    }
}
