package com.example.bookingappbs.repository;

import com.example.bookingappbs.model.Accommodation;
import com.example.bookingappbs.model.Booking;
import com.example.bookingappbs.model.Booking.Status;
import com.example.bookingappbs.model.User;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByUserIdAndStatus(Long userId, Status status, Pageable pageable);

    Page<Booking> getBookingsByUser(User user, Pageable pageable);

    Optional<Booking> findByIdAndUser(Long id, User user);

    boolean existsBookingByIdAndUser(Long id, User user);

    boolean existsByAccommodationAndCheckInDateLessThanAndCheckOutDateGreaterThan(
            Accommodation accommodation,
            LocalDate checkOutDate,
            LocalDate checkInDate
    );

    boolean existsByAccommodationAndCheckInDateLessThanAndCheckOutDateGreaterThanAndIdNot(
            Accommodation accommodation,
            LocalDate checkOutDate,
            LocalDate checkInDate,
            Long id
    );
}
