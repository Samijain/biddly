package com.biddly.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of POST /api/v1/auth/login. Deliberately no format or length rules: sign-in must never explain why it
 * failed (R28), so every wrong combination gets the same 401.
 */
public record LoginRequest(@NotBlank String email, @NotBlank String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
