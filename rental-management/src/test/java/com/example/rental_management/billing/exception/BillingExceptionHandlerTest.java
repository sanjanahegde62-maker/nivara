package com.example.rental_management.billing.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class BillingExceptionHandlerTest {
    private final BillingExceptionHandler handler = new BillingExceptionHandler();

    @Test
    void unexpectedArgumentMessageIsHiddenButKnownValidationMessageRemains() {
        assertEquals("Invalid billing request.", handler.handleIllegalArgument(
                new IllegalArgumentException("SQL: select password from users at /srv/app" )).getBody().get("message"));
        assertEquals("rentChargeId is required", handler.handleIllegalArgument(
                new IllegalArgumentException("rentChargeId is required")).getBody().get("message"));
    }

    @Test
    void unexpectedStateMessageIsHiddenAndKnownBusinessMessageRemains() {
        assertEquals("The billing operation could not be completed.", handler.handleIllegalState(
                new IllegalStateException("database connection failed at /secret" )).getBody().get("message"));
        assertEquals("rent charges can only be created for an active lease", handler.handleIllegalState(
                new IllegalStateException("rent charges can only be created for an active lease")).getBody().get("message"));
    }

    @Test
    void explicitDomainMessagesRemainVisibleWithoutStackTrace() {
        var response = handler.handleInvalidPayment(new InvalidPaymentException("payment amount must be greater than zero"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("payment amount must be greater than zero", response.getBody().get("message"));
        assertFalse(response.getBody().containsKey("trace"));
    }
}
