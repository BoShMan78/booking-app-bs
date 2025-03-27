package com.example.bookingappbs.dto.payment;

import java.math.BigDecimal;

public record PaymentDto(
        Long id,
        String status,
        Long bookingId,
        String sessionUrl,
        String sessionId,
        BigDecimal amountToPay
) {
}
