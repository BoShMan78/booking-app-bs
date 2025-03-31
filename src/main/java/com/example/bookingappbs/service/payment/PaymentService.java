package com.example.bookingappbs.service.payment;

import com.example.bookingappbs.dto.payment.CreatePaymentRequestDto;
import com.example.bookingappbs.dto.payment.PaymentDto;
import com.example.bookingappbs.model.Payment;
import com.example.bookingappbs.model.Payment.Status;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    List<PaymentDto> getPaymentsForCurrentUser(Long userId, Pageable pageable);

    List<PaymentDto> getAllPayments(Pageable pageable);

    PaymentDto save(CreatePaymentRequestDto requestDto);

    PaymentDto findBySessionId(String sessionId);

    void updatePaymentStatus(String sessionId, Status status);

    void updateSessionUrl(Long sessionId, String url);

    PaymentDto findById(Long paymentId);

    void updateSessionIdAndUrl(Long paymentId, String sessionId, String sessionUrl);

    List<Payment> findByStatus(Status status);
}
