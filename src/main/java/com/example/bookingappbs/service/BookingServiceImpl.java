package com.example.bookingappbs.service;

import com.example.bookingappbs.dto.booking.BookingDto;
import com.example.bookingappbs.dto.booking.CreateBookingRequestDto;
import com.example.bookingappbs.dto.booking.UpdateBookingRequestDto;
import com.example.bookingappbs.exception.AccessDeniedException;
import com.example.bookingappbs.exception.EntityNotFoundException;
import com.example.bookingappbs.mapper.BookingMapper;
import com.example.bookingappbs.model.Accommodation;
import com.example.bookingappbs.model.Booking;
import com.example.bookingappbs.model.Booking.Status;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.model.User.Role;
import com.example.bookingappbs.repository.AccommodationRepository;
import com.example.bookingappbs.repository.BookingRepository;
import com.example.bookingappbs.repository.UserRepository;
import java.util.List;
import java.util.Optional;
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

    @Override
    public BookingDto getBookingById(User user, Long id) {
        Booking existedBooking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Cannot find Booking by id: "));
        if (user.equals(existedBooking.getUser())) {
            return bookingMapper.toDto(existedBooking);
        } else {
            throw new AccessDeniedException("You don't have permission to access this booking. "
                    + "Please enter id number of your booking");
        }
    }

    @Override
    public BookingDto updateBookingById(User user, Long id, UpdateBookingRequestDto requestDto) {
        Booking existedBooking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Booking with id " + id + " not found"));

        if (user.getRole().equals(Role.CUSTOMER) && !existedBooking.getUser().equals(user)) {
            throw new EntityNotFoundException("Current user does not have booking with id " + id);
        }

        if (requestDto.status() != null) {
            if (user.getRole().equals(Role.CUSTOMER)) {
                throw new AccessDeniedException(
                        "The user does not have permission to change the booking status. "
                                + "Please contact the administrator.");
            }
            if (user.getRole().equals(Role.ADMIN)) {
                Optional.ofNullable(requestDto.status()).ifPresent(status -> {
                    try {
                        existedBooking.setStatus(Status.valueOf(status));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid status " + status);
                    }
                });
            }
        }
        Optional.ofNullable(requestDto.checkInDate()).ifPresent(existedBooking::setCheckInDate);
        Optional.ofNullable(requestDto.checkOutDate()).ifPresent(existedBooking::setCheckOutDate);
        Booking savedBooking = bookingRepository.save(existedBooking);
        return bookingMapper.toDto(savedBooking);
    }

    @Override
    public void deleteBookingById(User user, Long id) {
        if (user.getRole().equals(Role.ADMIN)) {
            bookingRepository.deleteById(id);
        } else {
            Booking existedBooking = bookingRepository.findById(id)
                    .orElseThrow(() ->
                            new EntityNotFoundException("Booking with id " + id + " not found"));
            if (existedBooking.getUser().equals(user)) {
                bookingRepository.delete(existedBooking);
            } else {
                throw new AccessDeniedException("Current user does not have booking with id " + id);
            }
        }
    }
}
