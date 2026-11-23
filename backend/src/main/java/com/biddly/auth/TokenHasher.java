package com.biddly.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 for refresh tokens, as 64 hex characters. Fast hashing is fine here, unlike for passwords: a refresh token
 * is 256 random bits, so there is nothing to guess. See Challenge 11b, question 2.
 */
final class TokenHasher {

    private TokenHasher() {
    }

    static String sha256(String rawToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every Java runtime has SHA-256", e);
        }
    }
}
