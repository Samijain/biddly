package com.biddly.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/v1/auth/register. {@code @Size} counts characters; BCrypt's limit is 72 <em>bytes</em>,
 * which the service checks (R28).
 */
public record RegisterRequest(
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull @Size(min = 8, max = 72, message = "must be 8 to 72 characters") String password) {

    /** A record's default toString prints every field, and request objects end up in logs. */
    @Override
    public String toString() {
        return "RegisterRequest[displayName=" + displayName + ", email=" + email + ", password=***]";
    }
}
