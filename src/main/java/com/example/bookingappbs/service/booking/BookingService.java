package com.example.bookingappbs.service.booking;

import com.example.bookingappbs.dto.accommodation.AccommodationDto;
import com.example.bookingappbs.dto.booking.BookingDto;
import com.example.bookingappbs.dto.booking.CreateBookingRequestDto;
import com.example.bookingappbs.dto.booking.UpdateBookingRequestDto;
import com.example.bookingappbs.model.Booking.Status;
import com.example.bookingappbs.model.User;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface BookingService {
    BookingDto save(User user, CreateBookingRequestDto requestDto);

    List<BookingDto> getBookingsByUserAndStatus(Long userId, Status status, Pageable pageable);

    List<BookingDto> getBookingsByUser(User user, Pageable pageable);

    BookingDto getBookingById(User user, Long id);

    BookingDto updateUserBookingById(User user, Long id, UpdateBookingRequestDto requestDto);

    BookingDto updateBookingByAdmin(Long id, UpdateBookingRequestDto requestDto);

    void deleteBookingById(User user, Long id);

    boolean canUserModifyBooking(User user, Long bookingId);

    void checkAndExpiredBooking();

    AccommodationDto findAccommodationById(Long accommodationId);
}
