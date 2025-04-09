package com.example.bookingappbs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.bookingappbs.dto.payment.PaymentDto;
import com.example.bookingappbs.model.Payment.Status;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.model.User.Role;
import com.example.bookingappbs.service.payment.PaymentProcessingService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PaymentControllerTest {
    @MockBean
    private PaymentProcessingService paymentProcessingService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext context;

    private User testUser;
    private User adminUser;
    private PaymentDto paymentDto;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testUser = new User()
                .setId(1L)
                .setEmail("user@example.com")
                .setPassword("password")
                .setFirstName("John")
                .setLastName("Doe")
                .setRole(Role.CUSTOMER);

        adminUser = new User()
                .setId(2L)
                .setEmail("admin@example.com")
                .setPassword("admin")
                .setFirstName("Admin")
                .setLastName("User")
                .setRole(Role.ADMIN);

        paymentDto = new PaymentDto(
                10L,
                Status.PENDING.toString(),
                1L,
                "session_url_123",
                "session_id_123",
                BigDecimal.valueOf(100.00)
        );
        pageable = PageRequest.of(0, 10);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private Authentication getAuthenticationForUser(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    @Test
    @DisplayName("POST /payments - Induce payment session")
    void createPaymentSession_ValidBookingId_ReturnsPaymentDto() throws Exception {
        // Given
        Long bookingId = 5L;

        //When
        when(paymentProcessingService.createPaymentSession(eq(testUser), eq(bookingId)))
                .thenReturn(paymentDto);

        // Then
        mockMvc.perform(post("/payments")
                        .param("bookingId", bookingId.toString())
                        .with(authentication(getAuthenticationForUser(testUser)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(paymentDto.id()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.amountToPay")
                        .value(paymentDto.amountToPay().doubleValue()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.sessionId")
                        .value(paymentDto.sessionId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.bookingId")
                        .value(paymentDto.bookingId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.sessionUrl")
                        .value(paymentDto.sessionUrl()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status")
                        .value(paymentDto.status()));

        verify(paymentProcessingService)
                .createPaymentSession(eq(testUser), eq(bookingId));
    }

    @Test
    @DisplayName("GET /payments/my - Get current user payments")
    void getUserPayments_AuthenticatedUser_ReturnsPaymentList() throws Exception {
        // Given
        List<PaymentDto> userPayments = List.of(paymentDto);

        //When
        when(paymentProcessingService.getPaymentsForCurrentUser(
                eq(testUser.getId()),
                eq(pageable))
        ).thenReturn(userPayments);

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/payments/my")
                        .with(authentication(getAuthenticationForUser(testUser)))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.model().attribute("payments", userPayments));

        verify(paymentProcessingService)
                .getPaymentsForCurrentUser(eq(testUser.getId()), eq(pageable));
    }

    @Test
    @DisplayName("GET /payments - Get all payments (ADMIN only)")
    void getAllPayments_AdminUser_ReturnsPaymentList() throws Exception {
        // Given
        List<PaymentDto> allPayments = List.of(paymentDto);

        //When
        when(paymentProcessingService.getAllPayments(eq(pageable))).thenReturn(allPayments);

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/payments")
                        .with(authentication(getAuthenticationForUser(adminUser)))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.model().attribute("payments", allPayments));

        verify(paymentProcessingService).getAllPayments(eq(pageable));
    }

    @Test
    @DisplayName("GET /payments/success - Successful payment proceeds")
    void success_ValidSessionId_ReturnsSuccessView() throws Exception {
        // Given
        String sessionId = "session_success_123";
        String successMessage = "Payment successful!";

        //When
        when(paymentProcessingService.handlePaymentSuccess(eq(sessionId), any()))
                .thenReturn("payment_success");

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/payments/success")
                        .param("session_id", sessionId)
                        .with(authentication(getAuthenticationForUser(testUser)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(view().name("payment_success"));

        verify(paymentProcessingService).handlePaymentSuccess(eq(sessionId), any());
    }

    @Test
    @DisplayName("GET /payments/cancel - Payment cancelling proceeds")
    void cancel_ValidSessionId_ReturnsCancelViewWithMessage() throws Exception {
        // Given
        String sessionId = "session_cancel_456";
        String cancelMessage = "Payment was cancelled.";

        //When
        when(paymentProcessingService.getPaymentCancelledMessage(eq(sessionId)))
                .thenReturn(cancelMessage);

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/payments/cancel")
                        .param("session_id", sessionId)
                        .with(authentication(getAuthenticationForUser(testUser)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.model().attribute("message", cancelMessage))
                .andExpect(view().name("payment_cancel"));

        verify(paymentProcessingService).getPaymentCancelledMessage(eq(sessionId));
    }

    @Test
    @DisplayName("POST /payments/renew/{paymentId} - Payment not expired")
    void renewPaymentSession_NotExpiredPayment_ReturnsPaymentInfoView() throws Exception {
        // Given
        Long paymentId = 15L;
        String message = "Payment is not expired";

        //When
        when(paymentProcessingService.renewPaymentSession(
                eq(paymentId),
                eq(testUser),
                any(Model.class)))
                .thenReturn("payment_info");

        // Then
        mockMvc.perform(post("/payments/renew/{paymentId}", paymentId)
                        .with(authentication(getAuthenticationForUser(testUser))))
                .andExpect(status().isOk())
                .andExpect(view().name("payment_info"));

        verify(paymentProcessingService)
                .renewPaymentSession(eq(paymentId), eq(testUser), any(Model.class));
    }

}
