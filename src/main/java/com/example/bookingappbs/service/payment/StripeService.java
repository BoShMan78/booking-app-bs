package com.example.bookingappbs.service.payment;

import com.example.bookingappbs.dto.booking.BookingDto;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Transactional
public class StripeService {
    private static final Logger logger = LogManager.getLogger(StripeService.class);

    @Value("${stripe_secret_key}")
    private String secretKey;
    @Value("${domain}")
    private String domain;
    @Value("${currency}")
    private String currency;
    @Value("${payment.description.prefix}")
    private String descriptionPrefix;
    @Value("${limit.for.user.metadata}")
    private int limitForUserMetadata;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public String createPaymentSession(BookingDto bookingDto, BigDecimal totalAmount)
            throws StripeException {
        logger.info("Creating Stripe payment session for booking ID: {}, amount: {} {}",
                bookingDto.id(), totalAmount, currency);
        SessionCreateParams params = buildSessionCreateParams(bookingDto, totalAmount);
        Session session = Session.create(params);

        logger.info("Stripe payment session created successfully. Session ID: {}",
                session.getId());
        return session.getId();
    }

    @Transactional(readOnly = true)
    public List<Charge> getChargesForUserId(Long userId) throws StripeException {
        logger.info("Retrieving Stripe charges for user ID: {}", userId);
        Map<String, Object> params = new HashMap<>();
        params.put("limit", limitForUserMetadata);
        params.put("metadata[user_id]", userId.toString());
        List<Charge> charges = Charge.list(params).getData();

        logger.info("Retrieved {} charges for user ID: {}.", charges.size(), userId);
        return charges;
    }

    @Transactional(readOnly = true)
    public Session retrieveSession(String sessionId) throws StripeException {
        logger.info("Retrieving Stripe session with ID: {}", sessionId);
        Session session = Session.retrieve(sessionId);
        logger.info("Stripe session {} retrieved successfully. Status: {}",
                sessionId, session.getStatus());
        return session;
    }

    private SessionCreateParams buildSessionCreateParams(
            BookingDto bookingDto,
            BigDecimal totalAmount
    ) {
        String successUrl = buildSuccessUrl(bookingDto.id());
        String cancelUrl = buildCancelUrl();
        SessionCreateParams.LineItem lineItem = buildLineItem(bookingDto, totalAmount);
        Map<String, String> metadata = buildMetadata(bookingDto);

        return SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(lineItem)
                .putMetadata("booking_id", bookingDto.id().toString())
                .putMetadata("user_id", bookingDto.userId().toString())
                .build();
    }

    private String buildSuccessUrl(Long id) {
        UriComponentsBuilder successUriBuilder = UriComponentsBuilder.fromUriString(domain)
                .path("/payments/success")
                .queryParam("session_id", "{CHECKOUT_SESSION_ID}");
        String successUrl = successUriBuilder.build().toUriString();

        logger.debug("Stripe success URL: {}", successUrl);
        return successUrl;
    }

    private String buildCancelUrl() {
        UriComponentsBuilder cancelUriBuilder = UriComponentsBuilder.fromUriString(domain)
                .path("/payments/cancel");
        String cancelUrl = cancelUriBuilder.build().toUriString();

        logger.debug("Stripe cancel URL: {}", cancelUrl);
        return cancelUrl;
    }

    private LineItem buildLineItem(BookingDto bookingDto, BigDecimal totalAmount) {
        SessionCreateParams.LineItem.PriceData.ProductData productData = SessionCreateParams
                .LineItem
                .PriceData
                .ProductData.builder()
                .setName(descriptionPrefix + bookingDto.id())
                .build();
        logger.debug("Stripe product data: {}", productData);

        SessionCreateParams.LineItem.PriceData priceData = SessionCreateParams
                .LineItem
                .PriceData.builder()
                .setCurrency(currency)
                .setUnitAmount(totalAmount.multiply(BigDecimal.valueOf(100)).longValue())
                .setProductData(productData)
                .build();
        logger.debug("Stripe price data: {}", priceData);

        SessionCreateParams.LineItem lineItem = SessionCreateParams
                .LineItem.builder()
                .setQuantity(1L)
                .setPriceData(priceData)
                .build();

        logger.debug("Stripe line item: {}", lineItem);
        return lineItem;
    }

    private Map<String, String> buildMetadata(BookingDto bookingDto) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("booking_id", bookingDto.id().toString());
        metadata.put("user_id", bookingDto.userId().toString());

        logger.debug("Stripe metadata: {}", metadata);
        return metadata;
    }
}
