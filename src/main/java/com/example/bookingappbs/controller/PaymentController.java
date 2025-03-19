package com.example.bookingappbs.controller;

import com.example.bookingappbs.model.ChargeRequest;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/payments")
@RequiredArgsConstructor
public class PaymentController {
    @Value("${domain}")
    private String domain;

    private final StripeService paymentService;

    @PostMapping
    @ResponseBody
    @Operation(summary = "Initiates payment sessions for booking transactions")
    public String createCheckoutSession(
            @RequestBody ChargeRequest chargeRequest,
            @AuthenticationPrincipal User user
    ) throws StripeException {
        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(domain + "/payments/success/")
                        .setCancelUrl(domain + "/payments/cancel/")
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency(chargeRequest
                                                                .getCurrency()
                                                                .toString())
                                                        .setUnitAmount(chargeRequest
                                                                .getAmount()
                                                                .longValue() * 100)
                                                        .setProductData(SessionCreateParams
                                                                .LineItem.PriceData
                                                                .ProductData.builder()
                                                                .setName(chargeRequest
                                                                        .getDescription())
                                                                .build())
                                                        .build())
                                        .build())
                        .putMetadata("user_id", user.getId().toString())
                        .build();
        Session session = Session.create(params);
        return session.getId();
    }

    @GetMapping
    @Operation(summary = "Retrieves payment information for users")
    public String getPayments(@AuthenticationPrincipal User user, Model model)
            throws StripeException {
        List<Charge> charges = paymentService.getChargesForUserId(user.getId());
        model.addAttribute("charges", charges);
        return "payment_list";
    }

    @GetMapping("/success")
    @Operation(summary = "Handles successful payment processing through Stripe redirection")
    public String success(Model model) {
        model.addAttribute("message", "Payment successful!");
        return "payment_success";
    }

    @GetMapping("/cancel")
    @Operation(summary = "Manages payment cancellation and returns payment paused messages"
            + " during Stripe redirection")
    public String cancel(Model model) {
        model.addAttribute("message", "Payment cancelled");
        return "payment_cancel";
    }
}
