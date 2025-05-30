package com.example.bookingappbs.repository;

import com.example.bookingappbs.model.Accommodation;
import com.example.bookingappbs.model.Booking;
import com.example.bookingappbs.model.Booking.Status;
import com.example.bookingappbs.model.User;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT b FROM Booking b WHERE(:userId IS NULL OR b.user.id = :userId) "
            + "AND (:status IS NULL OR b.status = :status)")
    Page<Booking> findByUserIdAndStatusOptional(@Param("userId") Long userId,
                                                @Param("status") Status status,
                                                Pageable pageable);

    Page<Booking> getBookingsByUser(User user, Pageable pageable);

    boolean existsBookingByIdAndUser(Long id, User user);

    List<Booking> findByStatusIsNotAndCheckOutDateLessThanEqual(
            Status status, LocalDate checkOutDate);

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.accommodation = :accommodation
            AND b.status NOT IN :excludedStatuses
            AND b.checkInDate < :checkOutDate
            AND b.checkOutDate > :checkInDate
            """)
    int countOverLappingBookings(
            @Param("accommodation") Accommodation accommodation,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("excludedStatuses") List<Status> exludedStatuses
    );

    @Query("""
                SELECT COUNT(b) FROM Booking b
                WHERE b.accommodation = :accommodation
                AND b.status NOT IN :excludedStatuses
                AND b.id != :excludedBookingId
                AND b.checkInDate < :checkOutDate
                AND b.checkOutDate > :checkInDate
            """)
    int countOverlappingBookingsExcludingCurrent(
            @Param("accommodation") Accommodation accommodation,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("excludedStatuses") List<Status> excludedStatuses,
            @Param("excludedBookingId") Long excludedBookingId
    );
}
