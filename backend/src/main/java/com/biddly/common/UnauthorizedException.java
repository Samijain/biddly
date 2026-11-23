package com.biddly.common;

/**
 * Thrown when the caller can't prove who they are (for example INVALID_CREDENTIALS or INVALID_REFRESH_TOKEN).
 * Mapped to HTTP 401. Missing or invalid access tokens never get here: Spring Security answers those itself.
 */
public class UnauthorizedException extends RuntimeException {

    private final String code;

    public UnauthorizedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
