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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDto register(UserRegistrationRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.email())) {
            throw new RegistrationException(
                    "User with email: " + requestDto.email() + " already exist");
        }

        User user = userMapper.toModel(requestDto);
        user.setPassword(passwordEncoder.encode(requestDto.password()));
        user.setRole(Role.CUSTOMER);

        User savedUser = userRepository.save(user);

        return userMapper.toDto(savedUser);
    }

    @Override
    public UserResponseDto updateUserRole(Long id, UpdateUserRoleRequestDto requestDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Can't find user by id: " + id));

        user.setRole(requestDto.role());
        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Override
    public UserResponseDto getUser(User user) {
        User userFromDB = userRepository.findById(user.getId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Can't find user by id: " + user.getId()));
        return userMapper.toDto(userFromDB);
    }

    @Override
    public UserResponseDto updateCurrentUserPatch(User currentUser,
                                                  UpdateCurrentUserRequestDto requestDto) {
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
        return userMapper.toDto(savedUser);
    }
}
