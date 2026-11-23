package com.biddly.auth.dto;

import java.time.Instant;

import com.biddly.user.Role;
import com.biddly.user.User;

/** The signed-up account. Has no password field at all, so the hash can never leak into a response. */
public record AccountResponse(Long id, String displayName, String email, Role role, Instant createdAt) {

    public static AccountResponse from(User user) {
        return new AccountResponse(user.getId(), user.getDisplayName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
