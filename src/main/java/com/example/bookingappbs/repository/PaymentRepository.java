package com.example.bookingappbs.repository;

import com.example.bookingappbs.model.Payment;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findByBooking_User_Id(long id, Pageable pageable);

    Optional<Payment> findBySessionId(String sessionId);
}
