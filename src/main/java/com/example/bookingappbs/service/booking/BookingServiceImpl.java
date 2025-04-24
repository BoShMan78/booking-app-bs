package com.example.bookingappbs.service.booking;

import com.example.bookingappbs.dto.accommodation.AccommodationDto;
import com.example.bookingappbs.dto.booking.BookingDto;
import com.example.bookingappbs.dto.booking.CreateBookingRequestDto;
import com.example.bookingappbs.dto.booking.UpdateBookingRequestDto;
import com.example.bookingappbs.exception.AccessDeniedException;
import com.example.bookingappbs.exception.EntityNotFoundException;
import com.example.bookingappbs.exception.PendingPaymentException;
import com.example.bookingappbs.mapper.AccommodationMapper;
import com.example.bookingappbs.mapper.BookingMapper;
import com.example.bookingappbs.model.Accommodation;
import com.example.bookingappbs.model.Booking;
import com.example.bookingappbs.model.Booking.Status;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.model.User.Role;
import com.example.bookingappbs.repository.AccommodationRepository;
import com.example.bookingappbs.repository.BookingRepository;
import com.example.bookingappbs.service.RedisService;
import com.example.bookingappbs.service.notification.NotificationService;
import com.example.bookingappbs.service.payment.PaymentService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private static final Logger logger = LogManager.getLogger(BookingServiceImpl.class);
    private static final String BOOKINGS_PAGE_KEY_PREFIX = "bookings";
    private static final String BOOKING_KEY_PREFIX = "booking::";

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final AccommodationRepository accommodationRepository;
    private final NotificationService notificationService;
    private final RedisService redisService;
    private final AccommodationMapper accommodationMapper;
    private final PaymentService paymentService;

    @Override
    @Transactional
    public BookingDto save(User user, CreateBookingRequestDto requestDto) {
        logger.info("Processing request to save a new booking for user ID: {} with data: {}",
                user.getId(), requestDto);
        long pendingPaymentsCount = paymentService.countPendingPaymentsForUser(user.getId());
        if (pendingPaymentsCount > 0) {
            throw new PendingPaymentException("There are already pending payments for the user");
        }

        Accommodation accommodation = accommodationRepository
                .findById(requestDto.accommodationId())
                .orElseThrow(() -> new EntityNotFoundException("Cannot find accommodation by id: "
                        + requestDto.accommodationId()));

        validateAvailability(accommodation, requestDto.checkInDate(), requestDto.checkOutDate());

        Booking booking = bookingMapper.toModel(requestDto);
        booking.setStatus(Status.PENDING);
        booking.setUser(user);
        booking.setAccommodation(accommodation);

        Booking savedBooking = bookingRepository.save(booking);
        BookingDto bookingDto = bookingMapper.toDto(savedBooking);

        cacheBooking(savedBooking.getId(), bookingDto);

        sendBookingNotification("New booking created", savedBooking, accommodation);

        logger.info("Booking saved successfully with ID: {}", savedBooking.getId());
        return bookingDto;
    }

    @Override
    public List<BookingDto> getBookingsByUserAndStatus(
            Long userId,
            Status status,
            Pageable pageable
    ) {
        logger.info("Processing request to get bookings by user ID: {}, status: {}, "
                + "and pagination: {}", userId, status, pageable);
        StringBuilder cacheKeyBuilder = new StringBuilder(BOOKINGS_PAGE_KEY_PREFIX);
        if (userId != null) {
            cacheKeyBuilder.append("::user::").append(userId);
        }
        if (status != null) {
            cacheKeyBuilder.append("::status::").append(status);
        }
        cacheKeyBuilder.append("::page::").append(pageable.getPageNumber())
                .append("::size::").append(pageable.getPageSize())
                .append("::sort::").append(pageable.getSort());
        String key = cacheKeyBuilder.toString();

        List<BookingDto> cachedBookings = redisService.findAll(key, BookingDto.class);
        if (cachedBookings != null && !cachedBookings.isEmpty()) {
            logger.info("Retrieved bookings from cache with key: {}, count: {}",
                    key, cachedBookings.size());
            return cachedBookings;
        }

        Page<Booking> bookings = bookingRepository
                .findByUserIdAndStatusOptional(userId, status, pageable);
        List<BookingDto> bookingDtos = bookings.stream()
                .map(bookingMapper::toDto)
                .toList();

        redisService.save(key, bookingDtos);

        logger.info("Retrieved bookings from database, count: {}, and saved to cache with key: {}",
                bookingDtos.size(), key);
        return bookingDtos;
    }

    @Override
    public List<BookingDto> getBookingsByUser(User user, Pageable pageable) {
        logger.info("Processing request to get bookings for user ID: {} with pagination: {}",
                user.getId(), pageable);
        String key = BOOKINGS_PAGE_KEY_PREFIX + "::user::" + user.getId()
                + "::page::" + pageable.getPageNumber()
                + "::size::" + pageable.getPageSize()
                + "::sort::" + pageable.getSort();
        List<BookingDto> cachedBookings = redisService.findAll(key, BookingDto.class);
        if (cachedBookings != null && !cachedBookings.isEmpty()) {
            logger.info("Retrieved user bookings from cache with key: {}, count: {}",
                    key, cachedBookings.size());
            return cachedBookings;
        }

        Page<Booking> bookingsByUser = bookingRepository.getBookingsByUser(user, pageable);

        List<BookingDto> bookingDtos = bookingsByUser.stream()
                .map(bookingMapper::toDto)
                .toList();

        redisService.save(key, bookingDtos);

        logger.info("Retrieved user bookings from database, count: {}, "
                + "and saved to cache with key: {}", bookingDtos.size(), key);
        return bookingDtos;
    }

    @Override
    public BookingDto getBookingById(User user, Long id) {
        logger.info("Processing request to get booking with ID: {} for user ID: {}",
                id, user.getId());
        String key = BOOKING_KEY_PREFIX + id;
        BookingDto bookingDto = redisService.find(key, BookingDto.class);
        if (bookingDto == null) {
            logger.info("Booking with ID {} not found in cache. Fetching from database.", id);
            Booking existedBooking = bookingRepository.findById(id)
                    .orElseThrow(() ->
                            new EntityNotFoundException("Cannot find Booking by id: "));
            bookingDto = bookingMapper.toDto(existedBooking);

            redisService.save(key, bookingDto);
            logger.info("Booking with ID {} fetched from database and saved to cache.", id);
        } else {
            logger.info("Booking with ID {} found in cache.", id);
        }
        return bookingDto;
    }

    @Override
    @Transactional
    public BookingDto updateBookingById(User user, Long id, UpdateBookingRequestDto requestDto) {
        logger.info("Processing request to update booking with ID: {} for user ID: {} "
                + "with data: {}", id, user.getId(), requestDto);
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
                Optional.of(requestDto.status()).ifPresent(status -> {
                    try {
                        existedBooking.setStatus(Status.valueOf(status));
                        logger.info("Booking ID {} status updated to: {}", id, status);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid status " + status);
                    }
                });
            }
        }

        LocalDate newCheckInDate = Optional.ofNullable(requestDto.checkInDate())
                .orElse(existedBooking.getCheckInDate());
        LocalDate newCheckOutDate = Optional.ofNullable(requestDto.checkOutDate())
                .orElse(existedBooking.getCheckOutDate());

        validateDates(newCheckInDate, newCheckOutDate);
        validateAvailabilityExcludingBooking(existedBooking, newCheckInDate, newCheckOutDate);

        existedBooking.setCheckInDate(newCheckInDate);
        existedBooking.setCheckOutDate(newCheckOutDate);

        Booking savedBooking = bookingRepository.save(existedBooking);
        BookingDto bookingDto = bookingMapper.toDto(savedBooking);

        cacheBooking(id, bookingDto);

        logger.info("Booking with ID {} updated successfully.", id);
        return bookingDto;
    }

    @Override
    @Transactional
    public void deleteBookingById(User user, Long id) {
        logger.info("Processing request to delete booking with ID: {} for user ID: {}",
                id, user.getId());
        Booking booking = bookingRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Booking with id " + id + " not found")
                        );

        if (booking.getStatus() == Status.CANCELED || booking.isDeleted()) {
            throw new IllegalArgumentException("Booking is already canceled or deleted");
        }

        booking.setStatus(Status.CANCELED);
        bookingRepository.save(booking);
        bookingRepository.delete(booking);

        redisService.deletePattern(BOOKINGS_PAGE_KEY_PREFIX + "*");

        sendBookingNotification("Booking canceled", booking, booking.getAccommodation());
        logger.info("Booking with ID {} successfully canceled.", id);
    }

    @Override
    public boolean canUserModifyBooking(User user, Long bookingId) {
        return bookingRepository.existsBookingByIdAndUser(bookingId, user);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void checkAndExpiredBooking() {
        logger.info("Scheduled task: Checking for expired bookings.");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Booking> expiredBookings = bookingRepository
                .findByStatusIsNotAndCheckOutDateLessThanEqual(Status.CANCELED, yesterday);

        if (expiredBookings.isEmpty()) {
            logger.info("No expired bookings found today.");
            notificationService.sendNotification("No expired bookings today!");
        } else {
            logger.info("Found {} expired bookings.", expiredBookings.size());
            for (Booking booking : expiredBookings) {
                booking.setStatus(Status.EXPIRED);
                bookingRepository.save(booking);

                sendBookingNotification(
                        "Booking expired and accommodation released",
                        booking,
                        booking.getAccommodation()
                );

                redisService.delete(BOOKING_KEY_PREFIX + booking.getId());
                logger.info("Booking ID {} expired and processed.", booking.getId());
            }
        }
        redisService.deletePattern(BOOKINGS_PAGE_KEY_PREFIX + "*");
        logger.info("Scheduled task: Finished checking for expired bookings.");
    }

    @Override
    public AccommodationDto findAccommodationById(Long accommodationId) {
        logger.info("Processing request to find accommodation by ID: {}", accommodationId);
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new EntityNotFoundException("Cannot find accommodation with id "
                        + accommodationId));

        logger.info("Accommodation with ID {} found.", accommodationId);
        return accommodationMapper.toDto(accommodation);
    }

    private void cacheBooking(Long id, BookingDto dto) {
        redisService.deletePattern(BOOKINGS_PAGE_KEY_PREFIX + "*");
        redisService.save(BOOKING_KEY_PREFIX + id, dto);
    }

    private void sendBookingNotification(
            String title,
            Booking booking,
            Accommodation accommodation
    ) {
        notificationService.sendNotification(title + ": \n"
                + "Booking ID: " + booking.getId() + "\n"
                + "Accommodation ID: " + accommodation.getId() + "\n"
                + "Type: " + accommodation.getType() + "\n"
                + "Location: " + accommodation.getLocation().getStreet() + " "
                + accommodation.getLocation().getHouse() + ", "
                + accommodation.getLocation().getCity() + ", "
                + accommodation.getLocation().getCountry() + "\n"
                + "Check-in Date: " + booking.getCheckInDate() + "\n"
                + "Check-out Date: " + booking.getCheckOutDate());
    }

    private void validateAvailability(
            Accommodation accommodation,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        int overlappingBookings = bookingRepository.countOverLappingBookings(
                accommodation,
                checkInDate,
                checkOutDate,
                List.of(Status.CANCELED, Status.EXPIRED));
        if (overlappingBookings >= accommodation.getAvailability()) {
            throw new IllegalArgumentException("No available units for the selected dates. "
                    + "Max capacity: " + accommodation.getAvailability());
        }
    }

    private void validateDates(LocalDate checkIndate, LocalDate checkOutDate) {
        if (checkIndate.isAfter(checkOutDate.minusDays(1))) {
            throw new IllegalArgumentException("Check-in date must be before check-out date");
        }
    }

    private void validateAvailabilityExcludingBooking(
            Booking booking,
            LocalDate newCheckInDate,
            LocalDate newCheckOutDate
    ) {
        int overlappingBookings = bookingRepository.countOverlappingBookingsExcludingCurrent(
                booking.getAccommodation(),
                newCheckInDate,
                newCheckOutDate,
                List.of(Status.CANCELED, Status.EXPIRED),
                booking.getId()
        );

        if (overlappingBookings >= booking.getAccommodation().getAvailability()) {
            throw new IllegalArgumentException("No available units for the new dates");
        }
    }
}
