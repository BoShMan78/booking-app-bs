package com.example.bookingappbs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.example.bookingappbs.dto.booking.BookingDto;
import com.example.bookingappbs.service.payment.StripeService;
import com.stripe.model.Charge;
import com.stripe.model.ChargeCollection;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class StripeServiceTest {
    @InjectMocks
    private StripeService stripeService;

    private String testDomain;
    private String testCurrency;
    private String testDescriptionPrefix;
    private int testLimit;
    private String testSecretKey;
    private Long testUserId;
    private BookingDto testBookingDto;
    private BigDecimal testTotalAmount;
    private String testSessionId;
    private String testChargeId;

    @BeforeEach
    void setUp() {
        testDomain = "https://test.domain";
        testCurrency = "usd";
        testDescriptionPrefix = "Booking #";
        testLimit = 10;
        testSecretKey = "test_key";

        ReflectionTestUtils.setField(stripeService, "secretKey", testSecretKey);
        ReflectionTestUtils.setField(stripeService, "domain", testDomain);
        ReflectionTestUtils.setField(stripeService, "currency", testCurrency);
        ReflectionTestUtils.setField(stripeService, "descriptionPrefix", testDescriptionPrefix);
        ReflectionTestUtils.setField(stripeService, "limitForUserMetadata", testLimit);

        stripeService.init();

        testUserId = 1L;
        testBookingDto = new BookingDto(
                10L,
                null,
                null,
                null,
                testUserId,
                null
        );
        testTotalAmount = BigDecimal.valueOf(100.00);
        testSessionId = "cs_test_123";
        testChargeId = "ch_test_123";
    }

    @Test
    void createPaymentSession_ShouldReturnSessionId_WhenCalledWithValidData() throws Exception {
        // Given
        BigDecimal totalAmount = new BigDecimal("100.00");
        String expectedSessionId = "test_session_id";

        //When
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session sessionMock = mock(Session.class);
            when(sessionMock.getId()).thenReturn(expectedSessionId);

            SessionCreateParams[] capturedParams = new SessionCreateParams[1];

            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenAnswer(invocation -> {
                        capturedParams[0] = invocation.getArgument(0);
                        return sessionMock;
                    });

            String result = stripeService.createPaymentSession(testBookingDto, totalAmount);

            // Then
            assertEquals(expectedSessionId, result);

            mockedSession.verify(() -> Session.create(any(SessionCreateParams.class)));

            assertNotNull(capturedParams[0]);
            assertTrue(capturedParams[0].getSuccessUrl().contains("/payments/success"));
            assertTrue(capturedParams[0].getCancelUrl().contains("/payments/cancel"));
        }
    }

    @Test
    void getChargesForUserId_ShouldReturnListOfCharges_WhenCalledWithValidUserId()
            throws Exception {
        // Given
        ChargeCollection chargeListMock = mock(ChargeCollection.class);
        List<Charge> mockCharges = List.of(mock(Charge.class), mock(Charge.class));

        //When
        try (MockedStatic<Charge> mockedCharge = mockStatic(Charge.class)) {
            mockedCharge.when(() -> Charge.list(anyMap())).thenReturn(chargeListMock);
            when(chargeListMock.getData()).thenReturn(mockCharges);

            List<Charge> result = stripeService.getChargesForUserId(testUserId);

            // Then
            assertEquals(mockCharges.size(), result.size());
            assertEquals(mockCharges, result);

            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            mockedCharge.verify(() -> Charge.list(captor.capture()));

            Map<String, Object> capturedParams = captor.getValue();
            assertEquals(String.valueOf(testUserId), capturedParams.get("metadata[user_id]"));
            assertEquals(testLimit, capturedParams.get("limit"));
        }
    }

    @Test
    void retrieveSession_ShouldReturnSession_WhenSessionIdIsValid() throws Exception {
        // Given
        Session sessionMock = mock(Session.class);

        //When
        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.retrieve(testSessionId)).thenReturn(sessionMock);

            Session result = stripeService.retrieveSession(testSessionId);

            // Then
            assertNotNull(result);
            assertEquals(sessionMock, result);

            mockedSession.verify(() -> Session.retrieve(testSessionId));
        }
    }
}
