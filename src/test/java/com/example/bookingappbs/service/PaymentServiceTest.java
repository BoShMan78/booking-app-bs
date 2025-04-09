package com.example.bookingappbs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bookingappbs.dto.payment.CreatePaymentRequestDto;
import com.example.bookingappbs.dto.payment.PaymentDto;
import com.example.bookingappbs.mapper.PaymentMapper;
import com.example.bookingappbs.model.Booking;
import com.example.bookingappbs.model.Payment;
import com.example.bookingappbs.model.Payment.Status;
import com.example.bookingappbs.model.User;
import com.example.bookingappbs.repository.BookingRepository;
import com.example.bookingappbs.repository.PaymentRepository;
import com.example.bookingappbs.service.payment.PaymentServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {
    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private BookingRepository bookingRepository;

    private Long testUserId;
    private Long testBookingId;
    private Long testPaymentId;
    private String testSessionId;
    private Pageable testPageable;
    private User testUser;
    private Booking testBooking;
    private Payment testPayment;
    private PaymentDto testPaymentDto;
    private CreatePaymentRequestDto testCreatePaymentRequestDto;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testBookingId = 10L;
        testPaymentId = 20L;
        testSessionId = "test_session_id";
        testPageable = PageRequest.of(0, 10);

        testUser = new User();
        testUser.setId(testUserId);

        testBooking = new Booking();
        testBooking.setId(testBookingId);
        testBooking.setUser(testUser);

        testPayment = new Payment();
        testPayment.setId(testPaymentId);
        testPayment.setBooking(testBooking);
        testPayment.setSessionId(testSessionId);
        testPayment.setStatus(Status.PENDING);
        testPayment.setAmountToPay(BigDecimal.valueOf(100.00));

        testPaymentDto = new PaymentDto(
                testPaymentId,
                Status.PENDING.name(),
                testBookingId,
                "test_session_url",
                testSessionId,
                BigDecimal.valueOf(100.00)
        );

        testCreatePaymentRequestDto = new CreatePaymentRequestDto(
                testBookingId,
                "test_session_url",
                testSessionId,
                BigDecimal.valueOf(100.00)
        );
    }

    @Test
    @DisplayName("Verify getPaymentsForCurrentUser() method works")
    void getPaymentsForCurrentUser_ValidUserId_ReturnsPaymentDtos() {
        //Given
        List<Payment> payments = List.of(testPayment);
        Page<Payment> paymentPage = new PageImpl<>(payments, testPageable, payments.size());

        //When
        when(paymentRepository.findByBooking_User_Id(testUserId, testPageable)).thenReturn(paymentPage);
        when(paymentMapper.toDto(testPayment)).thenReturn(testPaymentDto);

        List<PaymentDto> actualPayments = paymentService.getPaymentsForCurrentUser(testUserId, testPageable);

        //Then
        assertEquals(List.of(testPaymentDto), actualPayments);
        verify(paymentRepository).findByBooking_User_Id(testUserId, testPageable);
        verify(paymentMapper).toDto(testPayment);
    }

    @Test
    @DisplayName("Verify getAllPayments() method works")
    void getAllPayments_ReturnsAllPaymentDtos() {
        //Given
        List<Payment> payments = List.of(testPayment);
        Page<Payment> paymentPage = new PageImpl<>(payments, testPageable, payments.size());

        //When
        when(paymentRepository.findAll(testPageable)).thenReturn(paymentPage);
        when(paymentMapper.toDto(testPayment)).thenReturn(testPaymentDto);

        List<PaymentDto> actualPayments = paymentService.getAllPayments(testPageable);

        //Then
        assertEquals(List.of(testPaymentDto), actualPayments);
        verify(paymentRepository).findAll(testPageable);
        verify(paymentMapper).toDto(testPayment);
    }

    @Test
    @DisplayName("Verify save() method works")
    void save_ValidRequestDto_ReturnsSavedPaymentDto() {
        //Given
        Payment paymentToSave = new Payment();
        paymentToSave.setBooking(testBooking);
        paymentToSave.setSessionId(testSessionId);
        paymentToSave.setAmountToPay(BigDecimal.valueOf(100.00));
        paymentToSave.setStatus(Status.PENDING);

        //When
        when(paymentMapper.toModel(testCreatePaymentRequestDto)).thenReturn(paymentToSave);
        when(bookingRepository.findById(testBookingId)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(paymentMapper.toDto(testPayment)).thenReturn(testPaymentDto);

        PaymentDto actualPaymentDto = paymentService.save(testCreatePaymentRequestDto);

        //Then
        assertEquals(testPaymentDto, actualPaymentDto);
        assertEquals(Status.PENDING, paymentToSave.getStatus());
        verify(paymentMapper).toModel(testCreatePaymentRequestDto);
        verify(bookingRepository).findById(testBookingId);
        verify(paymentRepository).save(paymentToSave);
        verify(paymentMapper).toDto(testPayment);
    }

    @Test
    @DisplayName("Verify findBySessionId() method works")
    void findBySessionId_ExistingSessionId_ReturnsPaymentDto() {
        //When
        when(paymentRepository.findBySessionId(testSessionId)).thenReturn(Optional.of(testPayment));
        when(paymentMapper.toDto(testPayment)).thenReturn(testPaymentDto);

        PaymentDto actualPaymentDto = paymentService.findBySessionId(testSessionId);

        //Then
        assertEquals(testPaymentDto, actualPaymentDto);
        verify(paymentRepository).findBySessionId(testSessionId);
        verify(paymentMapper).toDto(testPayment);
    }

    @Test
    @DisplayName("Verify updatePaymentStatus() method works")
    void updatePaymentStatus_ExistingSessionId_UpdatesStatus() {
        //When
        when(paymentRepository.findBySessionId(testSessionId)).thenReturn(Optional.of(testPayment));
        paymentService.updatePaymentStatus(testSessionId, Status.PAID);

        //Then
        assertEquals(Status.PAID, testPayment.getStatus());
        verify(paymentRepository).findBySessionId(testSessionId);
        verify(paymentRepository).save(testPayment);
    }

    @Test
    @DisplayName("Verify updateSessionUrl() method works")
    void updateSessionUrl_ExistingPaymentId_UpdatesSessionUrl() {
        //Given
        String newUrl = "new_session_url";

        //When
        when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));
        paymentService.updateSessionUrl(testPaymentId, newUrl);

        //Then
        assertEquals(newUrl, testPayment.getSessionUrl());
        verify(paymentRepository).findById(testPaymentId);
        verify(paymentRepository).save(testPayment);
    }

    @Test
    @DisplayName("Verify findById() method works")
    void findById_ExistingPaymentId_ReturnsPaymentDto() {
        //When
        when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));
        when(paymentMapper.toDto(testPayment)).thenReturn(testPaymentDto);

        PaymentDto actualPaymentDto = paymentService.findById(testPaymentId);

        //Then
        assertEquals(testPaymentDto, actualPaymentDto);
        verify(paymentRepository).findById(testPaymentId);
        verify(paymentMapper).toDto(testPayment);
    }

    @Test
    @DisplayName("Verify updateSessionIdAndUrl() method works")
    void updateSessionIdAndUrl_ExistingPaymentId_UpdatesSessionIdAndUrl() {
        //Given
        String newSessionId = "new_session_id";
        String newSessionUrl = "new_session_url";

        //When
        when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayment));
        paymentService.updateSessionIdAndUrl(testPaymentId, newSessionId, newSessionUrl);

        //Then
        assertEquals(newSessionId, testPayment.getSessionId());
        assertEquals(newSessionUrl, testPayment.getSessionUrl());
        verify(paymentRepository).findById(testPaymentId);
        verify(paymentRepository).save(testPayment);
    }

    @Test
    @DisplayName("Verify findByStatus() method works")
    void findByStatus_ExistingStatus_ReturnsListOfPayments() {
        //Given
        List<Payment> expectedPayments = List.of(testPayment);

        //When
        when(paymentRepository.findByStatus(Status.PENDING)).thenReturn(expectedPayments);

        List<Payment> actualPayments = paymentService.findByStatus(Status.PENDING);

        //Then
        assertEquals(expectedPayments, actualPayments);
        verify(paymentRepository).findByStatus(Status.PENDING);
    }

    @Test
    @DisplayName("Verify countPendingPaymentsForUser() method works")
    void countPendingPaymentsForUser_ValidUserId_ReturnsCount() {
        //Given
        long expectedCount = 1L;

        //When
        when(paymentRepository.countByBooking_User_IdAndStatus(testUserId, Status.PENDING)).thenReturn(expectedCount);

        long actualCount = paymentService.countPendingPaymentsForUser(testUserId);

        //Then
        assertEquals(expectedCount, actualCount);
        verify(paymentRepository).countByBooking_User_IdAndStatus(testUserId, Status.PENDING);
    }
}
