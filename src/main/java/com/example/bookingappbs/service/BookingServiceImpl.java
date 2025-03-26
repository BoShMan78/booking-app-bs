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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final AccommodationRepository accommodationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RedisService redisService;
    private final RedisTemplate<String, Object> redisTemplate;

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

        boolean isAccommodationBooked = bookingRepository
                .existsByAccommodationAndCheckInDateLessThanAndCheckOutDateGreaterThan(
                        accommodation, booking.getCheckOutDate(), booking.getCheckInDate()
                );

        if (isAccommodationBooked) {
            throw new IllegalArgumentException("Accommodation with id "
                    + accommodation.getId() + " is already booked for the selected dates");
        }

        Booking savedBooking = bookingRepository.save(booking);
        BookingDto dto = bookingMapper.toDto(savedBooking);

        redisService.deletePattern("bookings::all::*");
        redisService.deletePattern("bookings::user::*");
        redisService.save("booking::" + savedBooking.getId(), dto);

        notificationService.sendNotification(
                "New booking created: \n"
                        + "Booking ID: " + booking.getId() + "\n"
                        + "Accommodation ID: " + accommodation.getId() + "\n"
                        + "Type: " + accommodation.getType() + "\n"
                        + "Location: " + accommodation.getLocation().getStreet() + " "
                        + accommodation.getLocation().getHouse() + ", "
                        + accommodation.getLocation().getCity() + ", "
                        + accommodation.getLocation().getCountry() + "\n"
                        + "Check-in Date: " + booking.getCheckInDate() + "\n"
                        + "Check-out Date: " + booking.getCheckOutDate()
        );

        return dto;
    }

    @Override
    public List<BookingDto> getBookingsByUserAndStatus(
            Long userId,
            Status status,
            Pageable pageable
    ) {
        String key = "bookings::user::" + userId + "::status::" + status
                + "::page::" + pageable.getPageNumber()
                + "::size::" + pageable.getPageSize()
                + "::sort::" + pageable.getSort();
        List<BookingDto> cachedBookings = redisService.findAll(key, BookingDto.class);
        if (cachedBookings != null && cachedBookings.size() > 0) {
            return cachedBookings;
        }

        Page<Booking> byUserIdAndStatus = bookingRepository
                .findByUserIdAndStatus(userId, status, pageable);

        List<BookingDto> bookingDtos = byUserIdAndStatus.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());

        redisService.save(key, bookingDtos);

        return bookingDtos;
    }

    @Override
    public List<BookingDto> getBookingsByUser(User user, Pageable pageable) {
        String key = "bookings::user::" + user.getId()
                + "::page::" + pageable.getPageNumber()
                + "::size::" + pageable.getPageSize()
                + "::sort::" + pageable.getSort();
        List<BookingDto> cachedBookings = redisService.findAll(key, BookingDto.class);
        if (cachedBookings != null && cachedBookings.size() > 0) {
            return cachedBookings;
        }

        Page<Booking> bookingsByUser = bookingRepository.getBookingsByUser(user, pageable);

        List<BookingDto> bookingDtos = bookingsByUser.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());

        redisService.save(key, bookingDtos);

        return bookingDtos;
    }

    @Override
    public BookingDto getBookingById(User user, Long id) {
        String key = "booking::" + id;
        BookingDto bookingDto = (BookingDto) redisService.find(key, BookingDto.class);
        if (bookingDto == null) {
            Booking existedBooking = bookingRepository.findById(id)
                    .orElseThrow(() ->
                            new EntityNotFoundException("Cannot find Booking by id: "));
            bookingDto = bookingMapper.toDto(existedBooking);

            redisService.save(key, bookingDto);
        }
        return bookingDto;
    }

    @Override
    public BookingDto updateBookingById(User user, Long id, UpdateBookingRequestDto requestDto) {
        Booking existedBooking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Booking with id " + id + " not found"));

        if (requestDto.status() != null) {
            if (user.getRole().equals(Role.CUSTOMER)) {
                throw new AccessDeniedException(
                        "The user does not have permission to change the booking status. "
                                + "Please contact the administrator.");
            }
            if (user.getRole().equals(Role.ADMIN)) {
                if (existedBooking.getStatus() == Status.CANCELED
                        || existedBooking.getStatus() == Status.EXPIRED) {
                    throw new IllegalArgumentException("Cannot update booking with status "
                            + existedBooking.getStatus());
                }
                Optional.ofNullable(requestDto.status()).ifPresent(status -> {
                    try {
                        existedBooking.setStatus(Status.valueOf(status));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid status " + status);
                    }
                });
            }
        }

        if (requestDto.checkOutDate() != null) {
            if (requestDto.checkInDate() != null
                    && requestDto.checkOutDate().isBefore(requestDto.checkInDate().plusDays(1))) {
                throw new IllegalArgumentException("Check-out date must be after check-in date");
            } else if (requestDto.checkInDate() == null
                    && requestDto.checkOutDate()
                    .isBefore(existedBooking.getCheckInDate()
                    .plusDays(1))) {
                throw new IllegalArgumentException("Check-out date must be after check-in date");
            }
            existedBooking.setCheckOutDate(requestDto.checkOutDate());
        }

        if (requestDto.checkInDate() != null || requestDto.checkOutDate() != null) {
            Accommodation accommodation = existedBooking.getAccommodation();
            LocalDate newCheckInDate = requestDto.checkInDate() != null
                    ? requestDto.checkInDate() : existedBooking.getCheckInDate();
            LocalDate newCheckOutDate = requestDto.checkOutDate() != null
                    ? requestDto.checkOutDate() : existedBooking.getCheckOutDate();
            boolean isAccommodationBooked = bookingRepository
                    .existsByAccommodationAndCheckInDateLessThanAndCheckOutDateGreaterThanAndIdNot(
                            accommodation, newCheckOutDate, newCheckInDate, id
                    );

            if (isAccommodationBooked) {
                throw new IllegalArgumentException("Accommodation with id "
                        + accommodation.getId() + " is already booked for the selected dates");
            }
        }

        Booking savedBooking = bookingRepository.save(existedBooking);
        BookingDto bookingDto = bookingMapper.toDto(savedBooking);

        redisService.deletePattern("bookings::all::*");
        redisService.deletePattern("bookings::user::*");
        redisService.save("booking::" + id, bookingDto);

        return bookingDto;
    }

    @Override
    public void deleteBookingById(User user, Long id) {
        Booking booking = bookingRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Booking with id " + id + " not found")
                        );
        if (booking.getStatus() == Status.CANCELED) {
            throw new IllegalArgumentException("Booking with id " + id + " is already canceled");
        }

        booking.setStatus(Status.CANCELED);
        bookingRepository.save(booking);

        redisService.deletePattern("bookings::all::*");
        redisService.deletePattern("bookings::user::*");
        redisService.delete("booking::" + id);

        Accommodation accommodation = booking.getAccommodation();
        notificationService.sendNotification(
                "Booking canceled:  \n"
                        + "Booking ID: " + booking.getId() + "\n"
                        + "Accommodation ID: " + accommodation.getId() + "\n"
                        + "Type: " + accommodation.getType() + "\n"
                        + "Location: " + accommodation.getLocation().getStreet() + " "
                        + accommodation.getLocation().getHouse() + ", "
                        + accommodation.getLocation().getCity() + ", "
                        + accommodation.getLocation().getCountry() + "\n"
                        + "Check-in Date: " + booking.getCheckInDate() + "\n"
                        + "Check-out Date: " + booking.getCheckOutDate()
        );
    }

    @Override
    public boolean canUserModifyBooking(User user, Long bookingId) {
        return bookingRepository.existsBookingByIdAndUser(bookingId, user);
    }

    @Override
    @Scheduled(cron = "0 0 0 * * *")
    public void checkAndExpiredBooking() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Booking> expiredBookings = bookingRepository
                .findByStatusIsNotAndCheckOutDateLessThanEqual(Status.CANCELED, yesterday);

        if (expiredBookings.isEmpty()) {
            notificationService.sendNotification("No expired bookings today!");
        } else {
            for (Booking booking : expiredBookings) {
                booking.setStatus(Status.EXPIRED);
                bookingRepository.save(booking);
                Accommodation accommodation = booking.getAccommodation();
                notificationService.sendNotification(
                        "Booking expired and accommodation released:\n"
                                + "Booking ID: " + booking.getId() + "\n"
                                + "Accommodation ID: " + accommodation.getId() + "\n"
                                + "Type: " + accommodation.getType() + "\n"
                                + "Location: " + accommodation.getLocation().getStreet() + " "
                                + accommodation.getLocation().getHouse() + ", "
                                + accommodation.getLocation().getCity() + ", "
                                + accommodation.getLocation().getCountry() + "\n"
                                + "Check-in Date: " + booking.getCheckInDate() + "\n"
                                + "Check-out Date: " + booking.getCheckOutDate()
                );
            }
        }

    }
}
