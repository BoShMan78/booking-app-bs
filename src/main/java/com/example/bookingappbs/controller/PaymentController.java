package com.example.bookingappbs.controller;

import com.example.bookingappbs.dto.accommodation.AccommodationDto;
import com.example.bookingappbs.dto.booking.BookingDto;
import com.example.bookingappbs.dto.payment.CreatePaymentRequestDto;
import com.example.bookingappbs.dto.payment.PaymentDto;
import com.example.bookingappbs.exception.EntityNotFoundException;
import com.example.bookingappbs.mapper.PaymentMapper;
import com.example.bookingappbs.model.Payment.Status;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.service.accommodation.AccommodationService;
import com.example.bookingappbs.service.booking.BookingService;
import com.example.bookingappbs.service.notification.NotificationService;
import com.example.bookingappbs.service.payment.PaymentService;
import com.example.bookingappbs.service.payment.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import io.swagger.v3.oas.annotations.Operation;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/payments")
@RequiredArgsConstructor
public class PaymentController {
    @Value("${domain}")
    private String domain;
    @Value("${currency}")
    private String currency;
    private static final String DESCRIPTION = "Оплата бронювання #";

    private final StripeService stripeService;
    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final PaymentMapper paymentMapper;
    private final NotificationService notificationService;
    private final AccommodationService accommodationService;

    @PostMapping
    @ResponseBody
    @Operation(summary = "Initiates payment sessions for a specific booking")
    public String createPaymentSession(
            @RequestParam("bookingId") Long bookingId,
            @AuthenticationPrincipal User user
    ) throws StripeException {
        BookingDto bookingDto = bookingService.getBookingById(user, bookingId);
        if (bookingDto == null || !bookingDto.userId().equals(user.getId())) {
            throw new EntityNotFoundException("Error: Reservation not found or does not belong to the user.");
        }

        long days = ChronoUnit.DAYS.between(bookingDto.checkOutDate(), bookingDto.checkInDate());
        AccommodationDto accommodationDto = accommodationService.findAccommodationById(bookingDto.accommodationId());
        BigDecimal totalAmount = accommodationDto.dailyRate().multiply(BigDecimal.valueOf(days));

        String sessionId = stripeService.createPaymentSession(bookingDto, totalAmount);

        CreatePaymentRequestDto paymentRequestDto = new CreatePaymentRequestDto(
                bookingId,
                null,
                sessionId,
                totalAmount
        );

        PaymentDto paymentDto = paymentService.save(paymentRequestDto);
        paymentService.updateSessionUrl(paymentDto.id(), stripeService.retrieveSession(sessionId).getUrl());

        return sessionId;
    }

    @GetMapping("/my")
    @Operation(summary = "Retrieves payment information for the authenticated user")
    public String getUserPayments(@AuthenticationPrincipal User user, Model model, Pageable pageable)
            throws StripeException {
        List<PaymentDto> paymentDtos = paymentService.getPaymentsForCurrentUser(user.getId(), pageable);
        model.addAttribute("payments", paymentDtos);
        return "user_payment_list";
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Retrieves all payment information (for admin)")
    public String getAllPayments(Model model, Pageable pageable) {
        List<PaymentDto> paymentDtos = paymentService.getAllPayments(pageable);
        model.addAttribute("payments", paymentDtos);
        return "all payments_list";
    }

    @GetMapping("/success")
    @Operation(summary = "Handles successful payment processing through Stripe redirection")
    public String success(@RequestParam("session_id") String sessionId, Model model) {
        try {
            Session session = stripeService.retrieveSession(sessionId);
            if (session.getPaymentStatus().equals("paid")) {
                Long sessionIdLong = Long.parseLong(sessionId);
                PaymentDto paymentDto = paymentService.findBySessionId(sessionIdLong);
                paymentService.updatePaymentStatus(paymentDto.id(), Status.PAID);
                model.addAttribute("message", "Payment successful!");
                notificationService.sendNotification("Payment successful for session " + sessionIdLong);
                return "payment_success";
            } else {
                model.addAttribute("message", "Payment pending or not successful for session " + sessionId);
                return "payment_pending";
            }
        } catch (StripeException e) {
            model.addAttribute("message", "Error retrieving payment session: " + e.getMessage());
            return "payment_error";
        }
    }

    @GetMapping("/cancel")
    @Operation(summary = "Manages payment cancellation and returns payment paused messages"
            + " during Stripe redirection")
    public String cancel(@RequestParam ("session_id") String sessionId, Model model) {
        model.addAttribute("message", "Payment cancelled for session " + sessionId);
        return "payment_cancel";
    }
}
