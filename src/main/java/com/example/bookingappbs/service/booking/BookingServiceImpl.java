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
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {
    private static final Logger logger = LogManager.getLogger(BookingServiceImpl.class);
    private static final String BOOKINGS_PAGE_KEY_PREFIX = "bookings";

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final AccommodationRepository accommodationRepository;
    private final NotificationService notificationService;
    private final RedisService redisService;
    private final AccommodationMapper accommodationMapper;
    private final PaymentService paymentService;
    private final BookingCacheKeyBuilder cacheKeyBuilder;
    private final BookingNotificationBuilder notificationBuilder;

    @Override
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

        clearBookingsCache();

        BookingDto bookingDto = bookingMapper.toDto(savedBooking);
        sendBookingNotification("New booking created", savedBooking, accommodation);

        logger.info("Booking saved successfully with ID: {}", savedBooking.getId());
        return bookingDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> getBookingsByUserAndStatus(
            Long userId,
            Status status,
            Pageable pageable
    ) {
        logger.info("Processing request to get bookings by user ID: {}, status: {}, "
                + "and pagination: {}", userId, status, pageable);
        String key = cacheKeyBuilder.buildBookingsPageKey(
                userId,
                status != null ? status.toString() : null,
                pageable
        );

        List<BookingDto> cachedBookings = findAllBookingsCache(key);
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

        saveToCacheDtos(key, bookingDtos);

        logger.info("Retrieved bookings from database, count: {}, and saved to cache with key: {}",
                bookingDtos.size(), key);
        return bookingDtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> getBookingsByUser(User user, Pageable pageable) {
        logger.info("Processing request to get bookings for user ID: {} with pagination: {}",
                user.getId(), pageable);
        String key = cacheKeyBuilder.buildBookingsPageKey(
                user.getId(),
                null,
                pageable
        );

        List<BookingDto> cachedBookings = findAllBookingsCache(key);
        if (cachedBookings != null && !cachedBookings.isEmpty()) {
            logger.info("Retrieved user bookings from cache with key: {}, count: {}",
                    key, cachedBookings.size());
            return cachedBookings;
        }

        Page<Booking> bookingsByUser = bookingRepository.getBookingsByUser(user, pageable);

        List<BookingDto> bookingDtos = bookingsByUser.stream()
                .map(bookingMapper::toDto)
                .toList();

        saveToCacheDtos(key, bookingDtos);

        logger.info("Retrieved user bookings from database, count: {}, "
                + "and saved to cache with key: {}", bookingDtos.size(), key);
        return bookingDtos;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDto getBookingById(User user, Long id) {
        logger.info("Processing request to get booking with ID: {} for user ID: {}",
                id, user.getId());

        Booking existedBooking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Cannot find Booking by id: "));
        BookingDto bookingDto = bookingMapper.toDto(existedBooking);

        logger.info("Booking with ID {} fetched from database and saved to cache.", id);
        return bookingDto;
    }

    @Override
    public BookingDto updateUserBookingById(
            User user,
            Long id,
            UpdateBookingRequestDto requestDto
    ) {
        logger.info("Processing request to update booking with ID: {} for user ID: {} "
                + "with data: {}", id, user.getId(), requestDto);
        Booking existedBooking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Booking with id " + id + " not found"));

        if (!existedBooking.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("User cannot update booking with ID: " + id);
        }
        if (requestDto.status() != null) {
            throw new AccessDeniedException("The user does not have permission to change "
                    + "the booking status. Please contact the administrator.");
        }
        return updateBookingDetails(existedBooking, requestDto, id, "user" + user.getId());
    }

    @Override
    public BookingDto updateBookingByAdmin(Long id, UpdateBookingRequestDto requestDto) {
        logger.info("Processing admin request to update booking with ID: {} "
                + "with data: {}", id, requestDto);
        Booking existedBooking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Booking with id " + id + " not found"));

        if (requestDto.status() != null) {
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
        return updateBookingDetails(existedBooking, requestDto, id, "admin");
    }

    private BookingDto updateBookingDetails(
            Booking existedBooking,
            UpdateBookingRequestDto requestDto,
            Long bookingId,
            String updatedBy
    ) {
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

        clearBookingsCache();

        logger.info("Booking with ID {} updated successfully by {}.", bookingId, updatedBy);
        return bookingDto;
    }

    @Override
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

        clearBookingsCache();

        sendBookingNotification("Booking canceled", booking, booking.getAccommodation());
        logger.info("Booking with ID {} successfully canceled.", id);
    }

    @Override
    public boolean canUserModifyBooking(User user, Long bookingId) {
        return bookingRepository.existsBookingByIdAndUser(bookingId, user);
    }

    @Override
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

                logger.info("Booking ID {} expired and processed.", booking.getId());
            }
        }
        clearBookingsCache();
        logger.info("Scheduled task: Finished checking for expired bookings.");
    }

    @Override
    @Transactional(readOnly = true)
    public AccommodationDto findAccommodationById(Long accommodationId) {
        logger.info("Processing request to find accommodation by ID: {}", accommodationId);
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new EntityNotFoundException("Cannot find accommodation with id "
                        + accommodationId));

        logger.info("Accommodation with ID {} found.", accommodationId);
        return accommodationMapper.toDto(accommodation);
    }

    @Async
    public void clearBookingsCache() {
        redisService.deletePattern(BOOKINGS_PAGE_KEY_PREFIX + "*");
    }

    private List<BookingDto> findAllBookingsCache(String key) {
        return redisService.findAll(key, BookingDto.class);
    }

    @Async
    public void saveToCacheDtos(String key, List<BookingDto> bookingDtos) {
        redisService.save(key, bookingDtos);
    }

    private void sendBookingNotification(
            String title,
            Booking booking,
            Accommodation accommodation
    ) {
        String message = notificationBuilder
                .buildBookingNotificationMessage(title, booking, accommodation);
        notificationService.sendNotification(message);
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
