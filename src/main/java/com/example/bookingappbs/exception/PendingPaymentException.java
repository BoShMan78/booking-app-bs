package com.example.bookingappbs.exception;

public class PendingPaymentException extends RuntimeException {
    public PendingPaymentException(String message) {
        super(message);
    }
}
