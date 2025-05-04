package com.example.bookingappbs.service.user;

import com.example.bookingappbs.dto.user.AddUserRoleRequestDto;
import com.example.bookingappbs.dto.user.UpdateCurrentUserRequestDto;
import com.example.bookingappbs.dto.user.UserRegistrationRequestDto;
import com.example.bookingappbs.dto.user.UserResponseDto;
import com.example.bookingappbs.model.User;

public interface UserService {
    UserResponseDto register(UserRegistrationRequestDto requestDto);

    UserResponseDto addAdminRoleToUser(Long id, AddUserRoleRequestDto requestDto);

    UserResponseDto getUser(User user);

    UserResponseDto updateCurrentUserPatch(User user, UpdateCurrentUserRequestDto requestDto);
}
