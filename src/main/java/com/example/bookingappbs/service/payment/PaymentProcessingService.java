package com.example.bookingappbs.service.payment;

import com.example.bookingappbs.dto.payment.PaymentDto;
import com.example.bookingappbs.model.User;
import com.stripe.exception.StripeException;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;

public interface PaymentProcessingService {
    List<PaymentDto> getPaymentsForCurrentUser(Long userId, Pageable pageable);

    List<PaymentDto> getAllPayments(Pageable pageable);

    PaymentDto createPaymentSession(User user, Long bookingId) throws StripeException;

    String handlePaymentSuccess(String sessionId, Model model);

    String getPaymentCancelledMessage(String sessionId);

    String renewPaymentSession(Long paymentId, User user, Model model);
}
