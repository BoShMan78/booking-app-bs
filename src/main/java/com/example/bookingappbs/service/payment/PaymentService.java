package com.example.bookingappbs.service.payment;

import com.example.bookingappbs.dto.payment.CreatePaymentRequestDto;
import com.example.bookingappbs.dto.payment.PaymentDto;
import com.example.bookingappbs.model.Payment;
import com.example.bookingappbs.model.Payment.Status;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentService {
    List<PaymentDto> getPaymentsForCurrentUser(Long userId, Pageable pageable);

    List<PaymentDto> getAllPayments(Pageable pageable);

    PaymentDto save(CreatePaymentRequestDto requestDto);

    PaymentDto findBySessionId(Long sessionId);

    void updatePaymentStatus(Long sessionId, Status status);
}
