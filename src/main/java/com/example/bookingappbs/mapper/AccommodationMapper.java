package com.example.bookingappbs.mapper;

import com.example.bookingappbs.config.MapperConfig;
import com.example.bookingappbs.dto.accommodation.AccommodationDto;
import com.example.bookingappbs.dto.accommodation.CreateAccommodationRequestDto;
import com.example.bookingappbs.dto.accommodation.UpdateAccommodationRequestDto;
import com.example.bookingappbs.dto.address.AddressDto;
import com.example.bookingappbs.dto.address.CreateAddressRequestDto;
import com.example.bookingappbs.model.Accommodation;
import com.example.bookingappbs.model.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        config = MapperConfig.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AccommodationMapper {
    AccommodationDto toDto(Accommodation accommodation);

    AddressDto toDto(Address address);

    Address toModel(CreateAddressRequestDto requestDto);

    Accommodation toModel(CreateAccommodationRequestDto requestDto);

    @Mapping(target = "type", source = "requestDto.type")
    @Mapping(target = "location", source = "requestDto.location")
    @Mapping(target = "size", source = "requestDto.size")
    @Mapping(target = "amenities", source = "requestDto.amenities")
    @Mapping(target = "dailyRate", source = "requestDto.dailyRate")
    @Mapping(target = "availability", expression =
            "java(requestDto.availability() != null && requestDto.availability() != 0 ? "
                    + "requestDto.availability() : accommodation.getAvailability())")
    void updateAccommodationFromDto(UpdateAccommodationRequestDto requestDto,
                                    @MappingTarget Accommodation accommodation);
}
