package com.example.bookingappbs.controller;

import com.example.bookingappbs.dto.payment.PaymentDto;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.service.payment.PaymentProcessingService;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentProcessingService paymentProcessingService;

    @PostMapping
    @ResponseBody
    @Operation(summary = "Initiates payment sessions for a specific booking")
    public PaymentDto createPaymentSession(
            @RequestParam("bookingId") Long bookingId,
            @AuthenticationPrincipal User user
    ) throws StripeException {
        return paymentProcessingService.createPaymentSession(user, bookingId);
    }

    @GetMapping("/my")
    @Operation(
            summary = "Retrieves payment information for the authenticated user",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "HTML page with user payments",
                            content = @Content(mediaType = "text/html")
                    )
            }
    )
    public String getUserPayments(
            @AuthenticationPrincipal User user,
            Model model,
            Pageable pageable
    ) {
        List<PaymentDto> paymentDtos = paymentProcessingService
                .getPaymentsForCurrentUser(user.getId(), pageable);
        model.addAttribute("payments", paymentDtos);
        return "user_payment_list";
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Retrieves all payment information (for admin)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "HTML page with all payments",
                            content = @Content(mediaType = "text/html")
                    )
            }
    )
    public String getAllPayments(Model model, Pageable pageable) {
        List<PaymentDto> paymentDtos = paymentProcessingService.getAllPayments(pageable);
        model.addAttribute("payments", paymentDtos);
        return "all_payments_list";
    }

    @GetMapping(value = "/success", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(
            summary = "Handles successful payment processing through Stripe redirection",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Success HTML view",
                            content = @Content(mediaType = "text/html")
                    )
            }
    )
    public String success(@RequestParam("session_id") String sessionId, Model model) {
        return paymentProcessingService.handlePaymentSuccess(sessionId, model);
    }

    @GetMapping(value = "/cancel", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(
            summary = "Manages payment cancellation and returns payment paused messages during"
                    + " Stripe redirection",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Cancel HTML view",
                            content = @Content(mediaType = "text/html")
                    )
            }
    )
    public String cancel(@RequestParam ("session_id") String sessionId, Model model) {
        model.addAttribute("message",
                paymentProcessingService.getPaymentCancelledMessage(sessionId));
        return "payment_cancel";
    }

    @PostMapping("/renew/{paymentId}")
    @Operation(
            summary = "Renews an expired payment session",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "HTML view for renewed session",
                            content = @Content(mediaType = "text/html")
                    )
            }
    )
    public String renewPaymentSession(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal User user,
            Model model
    ) {
        return paymentProcessingService.renewPaymentSession(paymentId, user, model);
    }
}
