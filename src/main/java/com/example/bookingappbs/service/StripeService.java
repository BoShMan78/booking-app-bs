package com.example.bookingappbs.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripeService {
    private static final int LIMIT_FOR_USER_METADATA = 10;
    private final NotificationService notificationService;

    @Value("${stripe_secret_key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public List<Charge> getChargesForUserId(Long userId) throws StripeException {
        Map<String, Object> params = new HashMap<>();
        params.put("limit", LIMIT_FOR_USER_METADATA);
        params.put("metadata[user_id]", userId.toString());
        return Charge.list(params).getData();
    }
}
