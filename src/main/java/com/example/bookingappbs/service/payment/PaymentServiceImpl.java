package com.example.bookingappbs.service.payment;

import com.example.bookingappbs.dto.payment.CreatePaymentRequestDto;
import com.example.bookingappbs.dto.payment.PaymentDto;
import com.example.bookingappbs.exception.EntityNotFoundException;
import com.example.bookingappbs.mapper.PaymentMapper;
import com.example.bookingappbs.model.Booking;
import com.example.bookingappbs.model.Payment;
import com.example.bookingappbs.model.Payment.Status;
import com.example.bookingappbs.repository.BookingRepository;
import com.example.bookingappbs.repository.PaymentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final Logger logger = LogManager.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final BookingRepository bookingRepository;

    @Override
    public List<PaymentDto> getPaymentsForCurrentUser(Long userId, Pageable pageable) {
        logger.info("Getting payments for user ID: {} with pagination: {}", userId, pageable);
        Page<Payment> payments = paymentRepository.findByBooking_User_Id(userId, pageable);
        List<PaymentDto> paymentDtos = payments.stream()
                .map(paymentMapper::toDto)
                .toList();

        logger.info("Retrieved {} payments for user ID: {}.", paymentDtos.size(), userId);
        return paymentDtos;
    }

    @Override
    public List<PaymentDto> getAllPayments(Pageable pageable) {
        logger.info("Getting all payments with pagination: {}", pageable);
        Page<Payment> payments = paymentRepository.findAll(pageable);
        List<PaymentDto> paymentDtos = payments.stream()
                .map(paymentMapper::toDto)
                .toList();

        logger.info("Retrieved {} payments in total.", paymentDtos.size());
        return paymentDtos;
    }

    @Override
    @Transactional
    public PaymentDto save(CreatePaymentRequestDto requestDto) {
        logger.info("Saving payment with request: {}", requestDto);
        Payment payment = paymentMapper.toModel(requestDto);
        payment.setStatus(Status.PENDING);
        Booking booking = bookingRepository.findById(requestDto.bookingId()).orElseThrow(
                () -> new EntityNotFoundException("Booking not found with id: "
                        + requestDto.bookingId()));
        payment.setBooking(booking);
        Payment savedPayment = paymentRepository.save(payment);

        logger.info("Payment saved successfully with ID: {}", savedPayment.getId());
        return paymentMapper.toDto(savedPayment);
    }

    @Override
    public PaymentDto findBySessionId(String sessionId) {
        logger.info("Finding payment by session ID: {}", sessionId);
        Payment payment = paymentRepository.findBySessionId(sessionId).orElseThrow(
                () -> new EntityNotFoundException("Cannot find session Id:" + sessionId));
        PaymentDto dto = paymentMapper.toDto(payment);

        logger.info("Payment found with session ID: {}", sessionId);
        return dto;
    }

    @Override
    @Transactional
    public void updatePaymentStatus(String sessionId, Status status) {
        logger.info("Updating payment status for session ID: {} to: {}", sessionId, status);
        Payment payment = paymentRepository.findBySessionId(sessionId).orElseThrow(() ->
                new EntityNotFoundException("Cannot find payment with session Id:" + sessionId));
        payment.setStatus(status);
        paymentRepository.save(payment);
        logger.info("Payment status updated successfully for session ID: {}", sessionId);
    }

    @Override
    @Transactional
    public void updateSessionUrl(Long id, String url) {
        logger.info("Updating session URL for payment ID: {} to: {}", id, url);
        Payment payment = paymentRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Cannot find payment with session Id:" + id));
        payment.setSessionUrl(url);
        paymentRepository.save(payment);
        logger.info("Session URL updated successfully for payment ID: {}", id);
    }

    @Override
    public PaymentDto findById(Long paymentId) {
        logger.info("Finding payment by ID: {}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Cannot find payment with id: " + paymentId));
        PaymentDto dto = paymentMapper.toDto(payment);

        logger.info("Payment found with ID: {}", paymentId);
        return dto;
    }

    @Override
    @Transactional
    public void updateSessionIdAndUrl(Long paymentId, String sessionId, String sessionUrl) {
        logger.info("Updating session ID and URL for payment ID: {} to session ID: {} and URL: {}",
                paymentId, sessionId, sessionUrl);
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() ->
                new EntityNotFoundException("Cannot find payment with id: " + paymentId));
        payment.setSessionId(sessionId);
        payment.setSessionUrl(sessionUrl);
        paymentRepository.save(payment);
        logger.info("Session ID and URL updated successfully for payment ID: {}", paymentId);
    }

    @Override
    public List<Payment> findByStatus(Status status) {
        logger.info("Finding payments by status: {}", status);
        List<Payment> payments = paymentRepository.findByStatus(status);

        logger.info("Retrieved {} payments with status: {}.", payments.size(), status);
        return payments;
    }

    @Override
    public long countPendingPaymentsForUser(Long userId) {
        logger.info("Counting pending payments for user ID: {}", userId);
        long count = paymentRepository.countByBooking_User_IdAndStatus(userId, Status.PENDING);

        logger.info("Found {} pending payments for user ID: {}.", count, userId);
        return count;
    }
}
