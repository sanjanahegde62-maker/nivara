package com.example.rental_management.billing.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateChargeException extends RuntimeException {
    public DuplicateChargeException(String message) {
        super(message);
    }
}
