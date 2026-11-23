package com.biddly.common;

/**
 * Thrown when a request clashes with data that already exists (for example EMAIL_TAKEN). Mapped to HTTP 409.
 * A rule broken by the request itself is a {@link BusinessRuleException} (422) instead.
 */
public class ConflictException extends RuntimeException {

    private final String code;

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
