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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
        return bookingService.save(user, requestDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(
            summary = "Retrieves bookings based on user ID and their status",
            description = "Available for managers"
    )
    public List<BookingDto> getBookingsByUserIdAndStatus(
            @RequestParam Long userId,
            @RequestParam Status status,
            @ParameterObject @PageableDefault Pageable pageable
    ) {
        return bookingService.getBookingsByUserAndStatus(userId, status, pageable);
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
        return bookingService.getBookingsByUser(user, pageable);
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
        return bookingService.getBookingById(user, id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @bookingServiceImpl.canUserModifyBooking(#user, #id)")
    @Operation(
            summary = "Update booking details",
            description = "Allows users to update their booking details. "
                    + "Admin allows to update any user details and status"
    )
    public BookingDto updateBookingById(
            @AuthenticationPrincipal User user,
            @PathVariable @Positive Long id,
            @RequestBody UpdateBookingRequestDto requestDto
    ) {
        return bookingService.updateBookingById(user, id, requestDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @bookingServiceImpl.canUserModifyBooking(#user, #id)")
    @Operation(
            summary = "Enables the cancellation of bookings",
            description = "Allows user to cancel their booking. Admin allows to cancel any bookings"
    )
    public void deleteAccommodationById(
            @AuthenticationPrincipal User user,
            @PathVariable @Positive Long id
    ) {
        bookingService.deleteBookingById(user, id);
    }
}
