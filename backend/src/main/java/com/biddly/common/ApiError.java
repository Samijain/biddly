package com.biddly.common;

import java.time.Instant;
import java.util.Map;

/**
 * The single error shape returned by every endpoint, so the frontend only has to handle one format.
 * {@code fieldErrors} is empty unless request validation failed.
 */
public record ApiError(
        String code,
        String message,
        Instant timestamp,
        Map<String, String> fieldErrors) {

    public static ApiError of(String code, String message, Instant timestamp) {
        return new ApiError(code, message, timestamp, Map.of());
    }
}
