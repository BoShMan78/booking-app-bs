package com.example.bookingappbs.service.payment;

import com.example.bookingappbs.dto.accommodation.AccommodationDto;
import com.example.bookingappbs.dto.booking.BookingDto;
import com.example.bookingappbs.dto.payment.CreatePaymentRequestDto;
import com.example.bookingappbs.dto.payment.PaymentDto;
import com.example.bookingappbs.exception.EntityNotFoundException;
import com.example.bookingappbs.model.Payment;
import com.example.bookingappbs.model.Payment.Status;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.service.accommodation.AccommodationService;
import com.example.bookingappbs.service.booking.BookingService;
import com.example.bookingappbs.service.notification.NotificationService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

@Service
@RequiredArgsConstructor
public class PaymentProcessingServiceImpl implements PaymentProcessingService {
    @Value("${domain}")
    private String domain;
    @Value("${currency}")
    private String currency;
    @Value("${payment.success.message}")
    private String paymentSuccessMessage;
    @Value("${payment.pending.message}")
    private String paymentPendingMessage;
    @Value("${payment.error.message}")
    private String paymentErrorMessage;
    @Value("${payment.cancelled.message}")
    private String paymentCancelledMessage;
    @Value("${payment.not_expired.message}")
    private String paymentNotExpiredMessage;
    @Value("${payment.not_found_or_not_belong.message}")
    private String paymentNotFoundOrNotBelongMessage;
    @Value("${booking.not_found_or_not_belong.message}")
    private String bookingNotFoundOrNotBelongMessage;
    @Value("${booking.not_found.message}")
    private String bookingNotFoundMessage;
    @Value("${accommodation.not_found.message}")
    private String accommodationNotFoundMessage;

    private final StripeService stripeService;
    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final AccommodationService accommodationService;
    private final NotificationService notificationService;

    @Override
    public List<PaymentDto> getPaymentsForCurrentUser(Long userId, Pageable pageable) {
        return paymentService.getPaymentsForCurrentUser(userId, pageable);
    }

    @Override
    public List<PaymentDto> getAllPayments(Pageable pageable) {
        return paymentService.getAllPayments(pageable);
    }

    @Override
    @Transactional
    public PaymentDto createPaymentSession(User user, Long bookingId) throws StripeException {
        BookingDto bookingDto = bookingService.getBookingById(user, bookingId);
        if (bookingDto == null || !bookingDto.userId().equals(user.getId())) {
            throw new EntityNotFoundException(bookingNotFoundOrNotBelongMessage + bookingId);
        }

        long days = ChronoUnit.DAYS.between(bookingDto.checkInDate(), bookingDto.checkOutDate());
        AccommodationDto accommodationDto = accommodationService
                .findAccommodationById(bookingDto.accommodationId());
        if (accommodationDto == null) {
            throw new EntityNotFoundException(accommodationNotFoundMessage
                    + bookingDto.accommodationId());
        }
        BigDecimal totalAmount = accommodationDto.dailyRate().multiply(BigDecimal.valueOf(days));

        String sessionId = stripeService.createPaymentSession(bookingDto, totalAmount);
        String sessionUrl = stripeService.retrieveSession(sessionId).getUrl();

        CreatePaymentRequestDto paymentRequestDto = new CreatePaymentRequestDto(
                bookingId,
                sessionUrl,
                sessionId,
                totalAmount
        );
        return paymentService.save(paymentRequestDto);
    }

    @Override
    @Transactional
    public String handlePaymentSuccess(String sessionId, Model model) {
        try {
            Session session = stripeService.retrieveSession(sessionId);
            if (session.getPaymentStatus().equals(Status.PAID.toString())) {
                PaymentDto paymentDto = paymentService.findBySessionId(sessionId);
                paymentService.updatePaymentStatus(sessionId, Status.PAID);
                model.addAttribute("message", paymentSuccessMessage);

                BookingDto bookingDto = bookingService
                        .getBookingById(null, paymentDto.bookingId());
                AccommodationDto accommodationDto = accommodationService
                        .findAccommodationById(bookingDto.accommodationId());
                String telegramMessage = String.format(
                        "✅ Successful Payment!\n\n"
                                + "Payment ID: %d\n"
                                + "Booking ID: %d\n"
                                + "Accommodation: %s\n"
                                + "Check-in Date: %s\n"
                                + "Check-out Date: %s\n"
                                + "Amount: %.2f %s\n"
                                + "Stripe Session ID: %s",
                        paymentDto.id(),
                        bookingDto.id(),
                        accommodationDto.type(),
                        bookingDto.checkInDate().toString(),
                        bookingDto.checkOutDate().toString(),
                        paymentDto.amountToPay(),
                        currency,
                        sessionId
                );
                notificationService.sendNotification("Payment successful for session #"
                        + sessionId);

                return "payment_success";
            } else {
                model.addAttribute("message", paymentPendingMessage + sessionId);
                return "payment_pending";
            }
        } catch (StripeException e) {
            model.addAttribute("message", paymentErrorMessage + e.getMessage());
            return "payment_error";
        }
    }

    @Override
    public String getPaymentCancelledMessage(String sessionId) {
        return paymentCancelledMessage + sessionId + ". "
                + "You can attempt the payment again later. "
                + "Please note that the payment session is typically valid for 24 hours.";
    }

    @Override
    @Transactional
    public String renewPaymentSession(Long paymentId, User user, Model model) {
        PaymentDto paymentDto = paymentService.findById(paymentId);

        if (paymentDto == null || !paymentDto.bookingId().equals(
                bookingService.getBookingById(user, paymentDto.bookingId()).id())) {
            throw new EntityNotFoundException(paymentNotFoundOrNotBelongMessage + paymentId);
        }

        if (!paymentDto.status().equals(Status.EXPIRED.toString())) {
            model.addAttribute("message", paymentNotExpiredMessage);
            return "payment_info";
        }

        BookingDto bookingDto = bookingService.getBookingById(user, paymentDto.bookingId());
        if (bookingDto == null) {
            throw new EntityNotFoundException(bookingNotFoundMessage + paymentDto.bookingId());
        }

        AccommodationDto accommodationDto = accommodationService
                .findAccommodationById(bookingDto.accommodationId());
        if (accommodationDto == null) {
            throw new EntityNotFoundException(accommodationNotFoundMessage
                    + bookingDto.accommodationId());
        }

        long days = ChronoUnit.DAYS.between(bookingDto.checkInDate(), bookingDto.checkOutDate());
        BigDecimal totalAmount = accommodationDto.dailyRate().multiply(BigDecimal.valueOf(days));

        try {
            String newSessionId = stripeService.createPaymentSession(bookingDto, totalAmount);
            Session newSession = stripeService.retrieveSession(newSessionId);
            String newSessionUrl = newSession.getUrl();

            paymentService.updateSessionIdAndUrl(paymentId, newSessionId, newSessionUrl);

            return "redirect:" + newSessionUrl;
        } catch (StripeException e) {
            model.addAttribute("message",
                    "Error creating new payment session: " + e.getMessage());
            return "payment_error";
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkExpiredSessions() {
        List<Payment> pendingPayments = paymentService.findByStatus(Status.PENDING);
        for (Payment payment : pendingPayments) {
            try {
                Session session = stripeService.retrieveSession(payment.getSessionId());
                if (session.getExpiresAt() != null
                        && Instant.ofEpochSecond(session.getExpiresAt()).isBefore(Instant.now())) {
                    paymentService.updatePaymentStatus(payment.getSessionId(), Status.EXPIRED);
                }
            } catch (StripeException e) {
                throw new RuntimeException("Error retrieving stripe session: "
                        + payment.getId() + ": " + e.getMessage());
            }
        }
    }
}
