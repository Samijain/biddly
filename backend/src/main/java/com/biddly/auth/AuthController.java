package com.biddly.auth;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.biddly.auth.dto.AccountResponse;
import com.biddly.auth.dto.LoginRequest;
import com.biddly.auth.dto.RegisterRequest;
import com.biddly.auth.dto.TokenResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping(AuthController.BASE_PATH)
public class AuthController {

    static final String BASE_PATH = "/api/v1/auth";

    /** Sent only to the auth endpoints, never readable from JavaScript, never sent by other sites. */
    public static final String REFRESH_COOKIE = "biddly_refresh";

    private final AuthService authService;
    private final SessionService sessionService;
    private final boolean secureCookie;

    public AuthController(AuthService authService, SessionService sessionService,
            @Value("${biddly.auth.cookie-secure}") boolean secureCookie) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AccountResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return withSession(authService.login(request));
    }

    @PostMapping("/refresh")
    ResponseEntity<TokenResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        return withSession(sessionService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        sessionService.logout(refreshToken);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString()).build();
    }

    /** The only endpoint so far that needs an access token; SecurityConfig checks it before this method runs. */
    @GetMapping("/me")
    AccountResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.currentAccount(Long.valueOf(jwt.getSubject()));
    }

    private ResponseEntity<TokenResponse> withSession(IssuedSession session) {
        var cookie = refreshCookie(session.refreshToken(), SessionService.REFRESH_TOKEN_LIFETIME);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(session.accessToken(), session.accessTokenExpiresAt(), session.account()));
    }

    /** A max age of zero tells the browser to delete the cookie. */
    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path(BASE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
