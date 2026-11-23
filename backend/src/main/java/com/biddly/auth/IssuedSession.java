package com.biddly.auth;

import java.time.Instant;

import com.biddly.auth.dto.AccountResponse;

/** A new session: the controller puts {@code refreshToken} in the cookie and the rest in the body. */
public record IssuedSession(String accessToken, Instant accessTokenExpiresAt, String refreshToken,
        Instant refreshTokenExpiresAt, AccountResponse account) {

    @Override
    public String toString() {
        return "IssuedSession[account=" + account + ", accessTokenExpiresAt=" + accessTokenExpiresAt + ", tokens=***]";
    }
}
