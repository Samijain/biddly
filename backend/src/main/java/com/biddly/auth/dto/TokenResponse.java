package com.biddly.auth.dto;

import java.time.Instant;

/** What sign-in and refresh return in the body. The refresh token travels only in its HttpOnly cookie. */
public record TokenResponse(String accessToken, String tokenType, Instant expiresAt, AccountResponse account) {

    public TokenResponse(String accessToken, Instant expiresAt, AccountResponse account) {
        this(accessToken, "Bearer", expiresAt, account);
    }
}
