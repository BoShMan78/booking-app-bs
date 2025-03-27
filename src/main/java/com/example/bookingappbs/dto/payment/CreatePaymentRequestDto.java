package com.example.bookingappbs.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreatePaymentRequestDto(
        @NotNull(message = "Booking ID cannot be null")
        Long bookingId,
        @NotBlank(message = "Session URL cannot be blank")
        String sessionUrl,
        @NotBlank(message = "Session ID cannot be blank")
        String sessionId,
        @NotNull(message = "Amount to pay cannot be null")
        BigDecimal amountToPay
) {
}
