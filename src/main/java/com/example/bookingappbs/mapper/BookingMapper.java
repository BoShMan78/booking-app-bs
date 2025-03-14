package com.example.bookingappbs.mapper;

import com.example.bookingappbs.config.MapperConfig;
import com.example.bookingappbs.dto.booking.BookingDto;
import com.example.bookingappbs.dto.booking.CreateBookingRequestDto;
import com.example.bookingappbs.model.Accommodation;
import com.example.bookingappbs.model.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = MapperConfig.class)
public interface BookingMapper {
    @Mapping(source = "accommodation.id", target = "accommodationId")
    @Mapping(source = "user.id", target = "userId")
    BookingDto toDto(Booking booking);

    @Mapping(
            target = "accommodation",
            source = "accommodationId",
            qualifiedByName = "accommodationFromId")
    Booking toModel(CreateBookingRequestDto requestDto);

    @Named("accommodationFromId")
    default Accommodation accommodationFromId(Long accommodationId) {
        Accommodation accommodation = new Accommodation();
        accommodation.setId(accommodationId);
        return accommodation;
    }
}
