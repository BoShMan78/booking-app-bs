package com.example.bookingappbs.service.payment;

import com.example.bookingappbs.dto.accommodation.AccommodationDto;
import com.example.bookingappbs.dto.booking.BookingDto;
import com.example.bookingappbs.model.ChargeRequest;
import com.example.bookingappbs.service.accommodation.AccommodationService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class StripeService {
    private static final int LIMIT_FOR_USER_METADATA = 10;
    private static final String DESCRIPTION = "Оплата бронювання #";
    private final AccommodationService accommodationService;

    @Value("${stripe_secret_key}")
    private String secretKey;
    @Value("${domain}")
    private String domain;
    @Value("${currency}")
    private String currency;

    private final PaymentService paymentService;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public String createPaymentSession(BookingDto bookingDto, BigDecimal totalAmount) throws StripeException {
        UriComponentsBuilder successUriBuilder = UriComponentsBuilder.fromUriString(domain)
                .path("/payments/success")
                .queryParam("session_id", "{CHECKOUT_SESSION_ID}");
        String successUrl = successUriBuilder.build().toUriString();

        UriComponentsBuilder cancelUriBuilder = UriComponentsBuilder.fromUriString(domain)
                .path("/payments/cancel");
        String cancelUrl = cancelUriBuilder.build().toUriString();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency)
                                                .setUnitAmount(totalAmount
                                                        .multiply(BigDecimal.valueOf(100))
                                                        .longValue())
                                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                        .setName(DESCRIPTION + bookingDto.id())
                                                        .build())
                                                .build())
                                .build())
                .putMetadata("booking_id", bookingDto.id().toString())
                .putMetadata("user_id", bookingDto.userId().toString())
                .build();

        Session session = Session.create(params);
        return session.getId();
    }

    public List<Charge> getChargesForUserId(Long userId) throws StripeException {
        Map<String, Object> params = new HashMap<>();
        params.put("limit", LIMIT_FOR_USER_METADATA);
        params.put("metadata[user_id]", userId.toString());
        return Charge.list(params).getData();
    }

    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }
}
