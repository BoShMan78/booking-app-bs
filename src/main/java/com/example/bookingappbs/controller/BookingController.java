package com.example.bookingappbs.controller;

import com.example.bookingappbs.dto.booking.BookingDto;
import com.example.bookingappbs.dto.booking.CreateBookingRequestDto;
import com.example.bookingappbs.model.Booking.Status;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
}
