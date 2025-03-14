package com.example.bookingappbs.service;

import com.example.bookingappbs.dto.booking.BookingDto;
import com.example.bookingappbs.dto.booking.CreateBookingRequestDto;
import com.example.bookingappbs.exception.EntityNotFoundException;
import com.example.bookingappbs.mapper.BookingMapper;
import com.example.bookingappbs.model.Accommodation;
import com.example.bookingappbs.model.Booking;
import com.example.bookingappbs.model.Booking.Status;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.repository.AccommodationRepository;
import com.example.bookingappbs.repository.BookingRepository;
import com.example.bookingappbs.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final AccommodationRepository accommodationRepository;
    private final UserRepository userRepository;

    @Override
    public BookingDto save(User user, CreateBookingRequestDto requestDto) {
        Booking booking = bookingMapper.toModel(requestDto);
        booking.setStatus(Status.PENDING);
        booking.setUser(user);

        Accommodation accommodation = accommodationRepository
                .findById(booking.getAccommodation().getId())
                .orElseThrow(() -> new EntityNotFoundException("Cannot find accommodation by id: "
                        + booking.getAccommodation().getId()));
        booking.setAccommodation(accommodation);
        return bookingMapper.toDto(bookingRepository.save(booking));
    }

    @Override
    public List<BookingDto> getBookingsByUserAndStatus(
            Long userId,
            Status status,
            Pageable pageable
    ) {
        Page<Booking> byUserIdAndStatus = bookingRepository
                .findByUserIdAndStatus(userId, status, pageable);

        return byUserIdAndStatus.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDto> getBookingsByUser(User user, Pageable pageable) {
        Page<Booking> bookingsByUser = bookingRepository.getBookingsByUser(user, pageable);

        return bookingsByUser.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }
}
