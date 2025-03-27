package com.example.bookingappbs.controller;

import com.example.bookingappbs.dto.booking.BookingDto;
import com.example.bookingappbs.dto.payment.CreatePaymentRequestDto;
import com.example.bookingappbs.dto.payment.PaymentDto;
import com.example.bookingappbs.mapper.PaymentMapper;
import com.example.bookingappbs.model.Payment.Status;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.service.booking.BookingService;
import com.example.bookingappbs.service.notification.NotificationService;
import com.example.bookingappbs.service.payment.PaymentService;
import com.example.bookingappbs.service.payment.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import io.swagger.v3.oas.annotations.Operation;

import java.math.BigDecimal;
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

    @PostMapping
    @ResponseBody
    @Operation(summary = "Initiates payment sessions for booking transactions")
    public String createCheckoutSession(
            @RequestBody @Valid CreatePaymentRequestDto requestDto,
            @AuthenticationPrincipal User user
    ) throws StripeException {
        BookingDto bookingDto = bookingService.getBookingById(user, requestDto.bookingId());
        if (bookingDto == null || !bookingDto.userId().equals(user.getId())) {
            throw new IllegalArgumentException("Error: Reservation not found or does not belong to the user.");
        }

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(domain + "/payments/success/?session_id={CHECKOUT_SESSION_ID}")
                        .setCancelUrl(domain + "/payments/cancel/")
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency(currency)
                                                        .setUnitAmount(requestDto.amountToPay()
                                                                .multiply(BigDecimal.valueOf(100))
                                                                .longValue())
                                                        .setProductData(SessionCreateParams
                                                                .LineItem.PriceData
                                                                .ProductData.builder()
                                                                .setName(DESCRIPTION + requestDto.bookingId())
                                                                .build())
                                                        .build())
                                        .build())
                        .putMetadata("user_id", user.getId().toString())
                        .putMetadata("booking_id", requestDto.bookingId().toString())
                        .build();
        Session session = Session.create(params);

        paymentService.save(requestDto);
        return session.getId();
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
        Long sesionIdLong = Long.valueOf(sessionId);
        PaymentDto paymentDto = paymentService.findBySessionId(sesionIdLong);

        if (paymentDto != null) {
            paymentService.updatePaymentStatus(sesionIdLong, Status.PAID);
            model.addAttribute("message", "Payment successful");
            notificationService.sendNotification("Payment successful for session " + sesionIdLong);
            return "payment success";
        } else {
            model.addAttribute("message",
                    "Payment not found for session " + sesionIdLong);
            return "payment_cancel";
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
