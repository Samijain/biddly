package com.biddly.common;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, ApiError.of("NOT_FOUND", ex.getMessage(), now()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex) {
        return respond(HttpStatus.UNPROCESSABLE_CONTENT, ApiError.of(ex.getCode(), ex.getMessage(), now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return respond(HttpStatus.BAD_REQUEST,
                new ApiError("VALIDATION_FAILED", "One or more fields are invalid.", now(), fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return respond(HttpStatus.BAD_REQUEST,
                ApiError.of("MALFORMED_REQUEST", "The request body could not be read.", now()));
    }

    /** Lets the UI show "coming soon" for endpoints whose challenge has not been solved yet. */
    @ExceptionHandler(UnsupportedOperationException.class)
    ResponseEntity<ApiError> handleNotImplemented(UnsupportedOperationException ex) {
        return respond(HttpStatus.NOT_IMPLEMENTED, ApiError.of("NOT_IMPLEMENTED", ex.getMessage(), now()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR,
                ApiError.of("INTERNAL_ERROR", "Something went wrong. Please try again.", now()));
    }

    private Instant now() {
        return clock.instant();
    }

    private static ResponseEntity<ApiError> respond(HttpStatus status, ApiError body) {
        return ResponseEntity.status(status).body(body);
    }
}
