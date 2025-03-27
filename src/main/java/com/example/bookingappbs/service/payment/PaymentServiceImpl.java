package com.example.bookingappbs.service.payment;

import com.example.bookingappbs.dto.payment.CreatePaymentRequestDto;
import com.example.bookingappbs.dto.payment.PaymentDto;
import com.example.bookingappbs.exception.EntityNotFoundException;
import com.example.bookingappbs.mapper.PaymentMapper;
import com.example.bookingappbs.model.Payment;
import com.example.bookingappbs.model.Payment.Status;
import com.example.bookingappbs.repository.PaymentRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Override
    public List<PaymentDto> getPaymentsForCurrentUser(Long userId, Pageable pageable) {
        Page<Payment> payments = paymentRepository.findByBooking_User_Id(userId, pageable);
        return payments.stream()
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentDto> getAllPayments(Pageable pageable) {
        Page<Payment> payments = paymentRepository.findAll(pageable);
        return payments.stream()
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentDto save(CreatePaymentRequestDto requestDto) {
        Payment payment = paymentMapper.toModel(requestDto);
        payment.setStatus(Status.PENDING);
        return paymentMapper.toDto(paymentRepository.save(payment));
    }

    @Override
    public PaymentDto findBySessionId(Long sessionId) {
        Payment payment = paymentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Cannot find session Id:" + sessionId));

        return paymentMapper.toDto(payment);
    }

    @Override
    public void updatePaymentStatus(Long sessionId, Status status) {
        Payment payment = paymentRepository.findBySessionId(sessionId).orElseThrow(() ->
                new EntityNotFoundException("Cannot find payment with session Id:" + sessionId));
        payment.setStatus(status);
        paymentRepository.save(payment);
    }

    @Override
    public void updateSessionUrl(Long id, String url) {
        Payment payment = paymentRepository.findBySessionId(id).orElseThrow(
                () -> new EntityNotFoundException("Cannot find payment with session Id:" + id));
        payment.setSessionUrl(url);
        paymentRepository.save(payment);
    }
}
