package com.example.bookingappbs.mapper;

import com.example.bookingappbs.config.MapperConfig;
import com.example.bookingappbs.dto.user.UpdateCurrentUserRequestDto;
import com.example.bookingappbs.dto.user.UserRegistrationRequestDto;
import com.example.bookingappbs.dto.user.UserResponseDto;
import com.example.bookingappbs.model.Role;
import com.example.bookingappbs.model.User;
import java.util.List;
import java.util.Set;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;

@Mapper(
        config = MapperConfig.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {
    @Mapping(target = "roles",
            expression = "java(mapRoles(user.getRoles()))")
    UserResponseDto toDto(User user);

    default List<String> mapRoles(Set<Role> roles) {
        return roles.stream().map(Role::getName).toList();
    }

    @Mapping(target = "password",
            expression = "java(encodePassword(requestDto.password(), passwordEncoder))")
    User toModel(UserRegistrationRequestDto requestDto, @Context PasswordEncoder passwordEncoder);

    default String encodePassword(String rawPassword, PasswordEncoder encoder) {
        return encoder.encode(rawPassword);
    }

    @Mapping(target = "email", source = "requestDto.email")
    @Mapping(target = "firstName", source = "requestDto.firstName")
    @Mapping(target = "lastName", source = "requestDto.lastName")
    @Mapping(target = "password",
            expression = "java(updatePasswordIfPresent(requestDto.password(), user.getPassword(),"
                    + "passwordEncoder))")
    void updateUserFromDto(UpdateCurrentUserRequestDto requestDto, @MappingTarget User user,
                           @Context PasswordEncoder passwordEncoder);

    default String updatePasswordIfPresent(String newPassword, String currentPassword,
                                           PasswordEncoder encoder) {
        return newPassword != null && !newPassword.isEmpty()
                ? encoder.encode(newPassword)
                : currentPassword;
    }
}
