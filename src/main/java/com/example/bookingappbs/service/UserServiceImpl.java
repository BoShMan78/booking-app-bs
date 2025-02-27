package com.example.bookingappbs.service;

import com.example.bookingappbs.dto.user.UserRegistrationRequestDto;
import com.example.bookingappbs.dto.user.UserResponseDto;
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

        //                saveUser(
        //                user.getEmail(),
        //                user.getFirstName(),
        //                user.getLastName(),
        //                user.getPassword(),
        //                user.getRole().name().toString(),
        //                user.isDeleted());
    }
}
