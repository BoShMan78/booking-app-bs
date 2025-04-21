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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

@Service
@RequiredArgsConstructor
public class PaymentProcessingServiceImpl implements PaymentProcessingService {
    private static final Logger logger = LogManager.getLogger(PaymentProcessingServiceImpl.class);

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
    @Value("${attribute.message}")
    private String attributeMessage;

    private final StripeService stripeService;
    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final AccommodationService accommodationService;
    private final NotificationService notificationService;

    @Override
    public List<PaymentDto> getPaymentsForCurrentUser(Long userId, Pageable pageable) {
        logger.info("Processing request to get payments for user ID: {} with pagination: {}",
                userId, pageable);
        List<PaymentDto> payments = paymentService.getPaymentsForCurrentUser(userId, pageable);

        logger.info("Retrieved {} payments for user ID: {}.", payments.size(), userId);
        return payments;
    }

    @Override
    public List<PaymentDto> getAllPayments(Pageable pageable) {
        logger.info("Processing request to get all payments with pagination: {}", pageable);
        List<PaymentDto> allPayments = paymentService.getAllPayments(pageable);

        logger.info("Retrieved {} payments in total.", allPayments.size());
        return allPayments;
    }

    @Override
    @Transactional
    public PaymentDto createPaymentSession(User user, Long bookingId) throws StripeException {
        logger.info("Processing request to create payment session for booking ID: {} "
                + "by user ID: {}", bookingId, user.getId());
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

        PaymentDto savedPayment = paymentService.save(paymentRequestDto);
        logger.info("Payment information saved for booking ID {}: {}", bookingId, savedPayment);
        return savedPayment;
    }

    @Override
    @Transactional
    public String handlePaymentSuccess(String sessionId, Model model) {
        logger.info("Handling successful payment for Stripe session ID: {}", sessionId);
        try {
            Session session = stripeService.retrieveSession(sessionId);
            if (session.getPaymentStatus().equals(Status.PAID.toString())) {
                PaymentDto paymentDto = paymentService.findBySessionId(sessionId);
                paymentService.updatePaymentStatus(sessionId, Status.PAID);
                model.addAttribute(attributeMessage, paymentSuccessMessage);

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
                notificationService.sendNotification(telegramMessage);

                logger.info("Payment success notification sent for session ID: {}", sessionId);
                return "payment_success";
            } else {
                logger.warn("Stripe session {} payment status is PENDING.", sessionId);
                model.addAttribute(attributeMessage, paymentPendingMessage + sessionId);
                return "payment_pending";
            }
        } catch (StripeException e) {
            logger.error("Error retrieving Stripe session {}: {}", sessionId, e.getMessage());
            model.addAttribute(attributeMessage, paymentErrorMessage + e.getMessage());
            return "payment_error";
        } catch (EntityNotFoundException e) {
            logger.error("Entity not found during payment success handling for session ID {}: {}",
                    sessionId, e.getMessage());
            model.addAttribute(attributeMessage, paymentErrorMessage + e.getMessage());
            return "payment_error";
        }
    }

    @Override
    public String getPaymentCancelledMessage(String sessionId) {
        logger.info("Returning cancellation message for Stripe session ID: {}", sessionId);
        return paymentCancelledMessage + sessionId + ". "
                + "You can attempt the payment again later. "
                + "Please note that the payment session is typically valid for 24 hours.";
    }

    @Override
    @Transactional
    public String renewPaymentSession(Long paymentId, User user, Model model) {
        logger.info("Processing request to renew payment session with ID: {} by user ID: {}",
                paymentId, user.getId());
        PaymentDto paymentDto = paymentService.findById(paymentId);

        if (paymentDto == null || !paymentDto.bookingId().equals(
                bookingService.getBookingById(user, paymentDto.bookingId()).id())) {
            throw new EntityNotFoundException(paymentNotFoundOrNotBelongMessage + paymentId);
        }

        if (!paymentDto.status().equals(Status.EXPIRED.toString())) {
            logger.warn("Payment with ID {} is not expired.", paymentId);
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
            logger.error("Error creating new payment session for payment ID {}: {}",
                    paymentId, e.getMessage());
            model.addAttribute("message",
                    "Error creating new payment session: " + e.getMessage());
            return "payment_error";
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkExpiredSessions() {
        logger.info("Scheduled task: Checking for expired Stripe sessions.");
        List<Payment> pendingPayments = paymentService.findByStatus(Status.PENDING);
        logger.info("Found {} pending payments to check.", pendingPayments.size());
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
        logger.info("Scheduled task: Finished checking for expired Stripe sessions.");
    }
}
