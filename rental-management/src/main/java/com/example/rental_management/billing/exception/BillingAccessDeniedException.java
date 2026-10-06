package com.example.rental_management.billing.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the caller does not have permission to access a billing resource.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class BillingAccessDeniedException extends RuntimeException {
    public BillingAccessDeniedException(String message) {
        super(message);
    }
}
