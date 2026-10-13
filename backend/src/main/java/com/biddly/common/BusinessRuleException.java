package com.biddly.common;

/**
 * Thrown when a request is well-formed but breaks a business rule
 * (for example BID_TOO_LOW or INVALID_TIME_WINDOW). Mapped to HTTP 422.
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
