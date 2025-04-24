package com.example.bookingappbs.mapper;

import com.example.bookingappbs.config.MapperConfig;
import com.example.bookingappbs.dto.user.UpdateCurrentUserRequestDto;
import com.example.bookingappbs.dto.user.UserRegistrationRequestDto;
import com.example.bookingappbs.dto.user.UserResponseDto;
import com.example.bookingappbs.model.User;
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
    UserResponseDto toDto(User user);

    User toModel(UserRegistrationRequestDto requestDto);

    @Mapping(target = "email", source = "requestDto.email")
    @Mapping(target = "firstName", source = "requestDto.firstName")
    @Mapping(target = "lastName", source = "requestDto.lastName")
    @Mapping(target = "password", expression = "java(requestDto.password() != null "
            + "&& !requestDto.password().isEmpty() "
            + "? passwordEncoder.encode(requestDto.password()) : user.getPassword())")
    void updateUserFromDto(UpdateCurrentUserRequestDto requestDto, @MappingTarget User user,
                           @Context PasswordEncoder passwordEncoder);
}
