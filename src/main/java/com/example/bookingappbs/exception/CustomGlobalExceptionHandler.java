package com.example.bookingappbs.exception;

import com.stripe.exception.StripeException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class CustomGlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<String> errors = ex.getBindingResult().getAllErrors().stream()
                .map(this::getErrorMessage)
                .toList();
        return buildResponseEntity(HttpStatus.BAD_REQUEST, "Validation error", errors);
    }

    private String getErrorMessage(ObjectError e) {
        if (e instanceof FieldError) {
            String field = ((FieldError) e).getField();
            String message = e.getDefaultMessage();
            return field + ": " + message;
        }
        return e.getDefaultMessage();
    }

    @ExceptionHandler(RegistrationException.class)
    public ResponseEntity<Object> handleRegistrationException(
            RegistrationException exception,
            WebRequest request
    ) {
        return buildResponseEntity(HttpStatus.CONFLICT, "Registration failed",
                List.of("Bad Request"), exception.getMessage());
    }

    @ExceptionHandler(StripeException.class)
    public String handleStripeException(Model model, StripeException exception) {
        model.addAttribute("error", exception.getMessage());
        return "result";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(
            AccessDeniedException exception,
            WebRequest request
    ) {
        return buildResponseEntity(HttpStatus.FORBIDDEN, "Access denied", List.of("Bad Request"),
                exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFoundException(
            EntityNotFoundException exception,
            WebRequest request
    ) {
        return buildResponseEntity(HttpStatus.NOT_FOUND, "Entity not found", List.of("Bad Request"),
                exception.getMessage());
    }

    @ExceptionHandler(PendingPaymentException.class)
    public ResponseEntity<Object> handlePaymentException(
            PendingPaymentException exception,
            WebRequest request
    ) {
        return buildResponseEntity(HttpStatus.CONFLICT, "Conflict with pending payments",
                List.of(exception.getMessage()), exception.getMessage());
    }

    private ResponseEntity<Object> buildResponseEntity(
            HttpStatus status,
            String error,
            List<String> errors
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("errors", errors);
        return new ResponseEntity<>(body, status);
    }

    private ResponseEntity<Object> buildResponseEntity(
            HttpStatus status,
            String error,
            List<String> errors,
            String message
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("errors", errors);
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}
