package com.example.bookingappbs.controller;

import com.example.bookingappbs.dto.booking.BookingDto;
import com.example.bookingappbs.dto.booking.CreateBookingRequestDto;
import com.example.bookingappbs.dto.booking.UpdateBookingRequestDto;
import com.example.bookingappbs.model.Booking.Status;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.service.booking.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Managing users' bookings", description = "CRUD for Booking")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/bookings")
public class BookingController {
    private static final Logger logger = LogManager.getLogger(BookingController.class);

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create new accommodation booking",
            description = "Permits the creation of new accommodation bookings"
    )
    public BookingDto createBooking(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid CreateBookingRequestDto requestDto
    ) {
        logger.info("Processing request to create a new booking for user ID: {}. Request: {}",
                user.getId(), requestDto);
        BookingDto savedBooking = bookingService.save(user, requestDto);

        logger.info("Booking successfully created with ID: {}", savedBooking.id());
        return savedBooking;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(
            summary = "Retrieves bookings based on user ID and their status",
            description = "Available for managers. Allows filtering by user ID and/or status."
    )
    public List<BookingDto> getBookingsByUserIdAndStatus(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Status status,
            @ParameterObject @PageableDefault Pageable pageable
    ) {
        logger.info("Received request to get bookings by user ID: {} and status: {}. "
                + "Pagination: {}", userId, status, pageable);
        List<BookingDto> bookings = bookingService
                .getBookingsByUserAndStatus(userId, status, pageable);

        logger.info("Retrieved {} bookings.", bookings.size());
        return bookings;
    }

    @GetMapping("/my")
    @Operation(
            summary = "Retrieves user bookings",
            description = "Retrieves current user bookings"
    )
    public List<BookingDto> getBookingsByUserId(
            @AuthenticationPrincipal User user,
            @ParameterObject @PageableDefault Pageable pageable
    ) {
        logger.info("Received request to get bookings for current user ID: {}. Pagination: {}",
                user.getId(), pageable);
        List<BookingDto> userBookings = bookingService.getBookingsByUser(user, pageable);

        logger.info("Retrieved {} bookings for user ID: {}.", userBookings.size(), user.getId());
        return userBookings;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @bookingServiceImpl.canUserModifyBooking(#user, #id)")
    @Operation(
            summary = "Provides information about a specific booking",
            description = "Provides information about a specific booking by booking id"
    )
    public BookingDto getBookingById(
            @AuthenticationPrincipal User user,
            @PathVariable @Positive Long id
    ) {
        logger.info("Received request to get booking with ID: {} by user ID: {}.",
                id, user.getId());
        BookingDto bookingDto = bookingService.getBookingById(user, id);

        logger.info("Booking information with ID {} successfully retrieved.", id);
        return bookingDto;
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@bookingServiceImpl.canUserModifyBooking(#user, #id)")
    @Operation(
            summary = "Update user's booking details",
            description = "Allows authenticated users to update their own booking details "
                    + "(excluding status)."
    )
    public BookingDto updateUserBookingById(
            @AuthenticationPrincipal User user,
            @PathVariable @Positive Long id,
            @RequestBody UpdateBookingRequestDto requestDto
    ) {
        logger.info("Received request to update booking with ID: {} by user ID: {}. "
                + "Update data: {}", id, user.getId(), requestDto);
        BookingDto bookingDto = bookingService.updateUserBookingById(user, id, requestDto);

        logger.info("Booking with ID {} successfully updated.", id);
        return bookingDto;
    }

    @PatchMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update any booking details (Admin only)",
            description = "Allows administrators to update any booking details, including status."
    )
    public BookingDto updateBookingByAdmin(
            @AuthenticationPrincipal User admin,
            @PathVariable @Positive Long id,
            @RequestBody UpdateBookingRequestDto requestDto
    ) {
        logger.info("Received admin request to update booking with ID: {}. "
                + "Update data: {}", id, requestDto);
        BookingDto bookingDto = bookingService.updateBookingByAdmin(id, requestDto);

        logger.info("Booking with ID {} successfully updated by admin {}.", id, admin.getId());
        return bookingDto;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @bookingServiceImpl.canUserModifyBooking(#user, #id)")
    @Operation(
            summary = "Enables the cancellation of bookings",
            description = "Allows user to cancel their booking. Admin allows to cancel any bookings"
    )
    public ResponseEntity<Void> deleteAccommodationById(
            @AuthenticationPrincipal User user,
            @PathVariable @Positive Long id
    ) {
        logger.info("Received request to delete booking with ID: {} by user ID: {}.",
                id, user.getId());
        bookingService.deleteBookingById(user, id);

        logger.info("Booking with ID {} successfully deleted.", id);
        return ResponseEntity.noContent().build();
    }
}
