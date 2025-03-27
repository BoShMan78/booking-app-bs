package com.example.bookingappbs.mapper;

import com.example.bookingappbs.config.MapperConfig;
import com.example.bookingappbs.dto.payment.CreatePaymentRequestDto;
import com.example.bookingappbs.dto.payment.PaymentDto;
import com.example.bookingappbs.model.Payment;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface PaymentMapper {
    PaymentDto toDto(Payment payment);

    Payment toModel(CreatePaymentRequestDto requestDto);
}
