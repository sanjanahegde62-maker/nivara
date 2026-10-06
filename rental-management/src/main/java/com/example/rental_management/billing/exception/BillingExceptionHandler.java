package com.example.rental_management.billing.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Translates billing domain exceptions into clean JSON error responses.
 * Stack traces are never included in the response body.
 */
@RestControllerAdvice
public class BillingExceptionHandler {

    @ExceptionHandler(BillingAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(BillingAccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateChargeException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateChargeException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPayment(InvalidPaymentException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        if ("rent charges can only be created for an active lease".equals(ex.getMessage())) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        }
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "The billing operation could not be completed.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage();
        if ("rentChargeId is required".equals(message) || "paymentMethod is required".equals(message)) {
            return error(HttpStatus.BAD_REQUEST, message);
        }
        return error(HttpStatus.BAD_REQUEST, "Invalid billing request.");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        ));
    }
}
