package com.biddly.common;

/** Thrown when we know who the caller is and the answer is still no (for example ACCOUNT_SUSPENDED). Mapped to HTTP 403. */
public class ForbiddenException extends RuntimeException {

    private final String code;

    public ForbiddenException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
