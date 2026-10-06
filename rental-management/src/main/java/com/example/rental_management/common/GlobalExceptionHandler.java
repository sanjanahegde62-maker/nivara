package com.example.rental_management.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global API error handler.
 *
 * <p>Provides a consistent JSON error envelope across all controllers and
 * prevents Spring Boot's default error page from exposing stack traces,
 * SQL messages, or internal implementation details.
 *
 * <p>The billing-domain {@code BillingExceptionHandler} remains active and
 * handles billing-specific exceptions (BillingAccessDeniedException,
 * ResourceNotFoundException, DuplicateChargeException, InvalidPaymentException)
 * as well as the generic IllegalArgumentException and IllegalStateException.
 * This handler covers everything not handled there.
 *
 * <p>Rules:
 * <ul>
 *   <li>No stack traces, SQL text, class names, or internal paths in responses.
 *   <li>Error messages are safe domain messages from the application itself.
 *   <li>Unexpected server errors log the cause at ERROR level but return a
 *       generic "An unexpected error occurred" message to clients.
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Validation errors (Bean Validation on @RequestBody) ───────────────────

    /**
     * Handles {@code @Valid} failures on request bodies — returns 400 with a
     * field-level breakdown so clients know exactly what to fix.
     * Field values are never included (passwords, secrets stay out of responses).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {
        String fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, fields.isEmpty() ? "Validation failed" : fields);
    }

    /**
     * Handles constraint violations from path/query parameter validation.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, detail.isEmpty() ? "Constraint violation" : detail);
    }

    // ── Security exceptions ────────────────────────────────────────────────────

    /**
     * Handles Spring Security access-denied exceptions (authenticated but
     * lacking the required role/permission) — returns 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "Access denied");
    }

    /**
     * Handles Spring Security authentication exceptions — returns 401.
     * These should normally be intercepted by the security filter chain entry
     * point, but this handler acts as a safety net.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    // ── Routing errors ─────────────────────────────────────────────────────────

    /**
     * Handles requests to routes that do not exist — returns 404 instead of
     * Spring Boot's default HTML error page.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "The requested resource was not found");
    }

    // ── Catch-all ──────────────────────────────────────────────────────────────

    /**
     * Handles any uncaught exception — returns 500 with a generic message.
     * The real exception is logged at ERROR level on the server; no internal
     * detail is exposed to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        // Log server-side so the error is visible in application logs.
        // The message to the client is deliberately generic.
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class)
                .error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    // ── Shared error builder ───────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
