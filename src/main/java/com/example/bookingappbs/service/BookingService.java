package com.example.bookingappbs.service;

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

    BookingDto updateBookingById(User user, Long id, UpdateBookingRequestDto requestDto);

    void deleteBookingById(User user, Long id);

    boolean canUserModifyBooking(User user, Long bookingId);
}
