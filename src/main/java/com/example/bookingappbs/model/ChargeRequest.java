package com.example.bookingappbs.model;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class ChargeRequest {
    private String description;
    private BigDecimal amount;
    private Currency currency;
    private String stripeEmail;
    private String stripeToken;

    public enum Currency {
        EUR, USD, UAH
    }
}
