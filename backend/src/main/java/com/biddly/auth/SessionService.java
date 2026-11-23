package com.biddly.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import com.biddly.common.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biddly.auth.dto.AccountResponse;
import com.biddly.user.User;

/**
 * Sessions: a short-lived access JWT plus a long-lived refresh token. Password sign-in (11b) and Google sign-in (11d)
 * both end in {@link #issue}.
 */
@Service
public class SessionService {

    public static final Duration REFRESH_TOKEN_LIFETIME = Duration.ofDays(14);

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public SessionService(RefreshTokenRepository refreshTokenRepository, JwtService jwtService, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    /** Ready for you: starts a new session for a user whose identity has already been proven. */
    @Transactional
    public IssuedSession issue(User user) {
        var now = clock.instant();
        var rawRefreshToken = newRawToken();
        var refreshExpiresAt = now.plus(REFRESH_TOKEN_LIFETIME);
        refreshTokenRepository.save(new RefreshToken(user, TokenHasher.sha256(rawRefreshToken), now, refreshExpiresAt));
        var accessToken = jwtService.createAccessToken(user, now);
        return new IssuedSession(accessToken.value(), accessToken.expiresAt(), rawRefreshToken, refreshExpiresAt,
                AccountResponse.from(user));
    }

    /**
     * CHALLENGE 11b: swap a refresh token for a new session (rotation).
     *
     * <ul>
     *   <li>No token, or one we never issued: 401 {@code INVALID_REFRESH_TOKEN}.</li>
     *   <li>Expired: the same 401.</li>
     *   <li>Already revoked: someone is <em>reusing</em> an old token, so it may have been stolen. End every
     *       session of that user, then answer the same 401. The revocations must survive the 401.</li>
     *   <li>Otherwise: revoke this token and {@link #issue} a new session for its user. The old token never
     *       works again.</li>
     * </ul>
     *
     * Look tokens up by hash with {@code findByTokenHashForUpdate}: two refreshes with the same token must queue up.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public IssuedSession refresh(String rawRefreshToken) {
        if (rawRefreshToken == null)
            throw invalidRefreshToken();
        RefreshToken fetchToken = refreshTokenRepository.findByTokenHashForUpdate(TokenHasher.sha256(rawRefreshToken)).orElseThrow(SessionService::invalidRefreshToken);
        var now = clock.instant();
        if(fetchToken.isRevoked()){
            refreshTokenRepository.revokeAllForUser(fetchToken.getUser().getId(),now);
            throw invalidRefreshToken();
        }
        if(fetchToken.isExpired(now))
           throw invalidRefreshToken();

        fetchToken.revoke(now);
        return issue(fetchToken.getUser());
    }

    /** One answer for every incorrect refresh token **/
    private static UnauthorizedException invalidRefreshToken() {
        return new UnauthorizedException("INVALID_REFRESH_TOKEN", "Your session has ended. Please sign in again.");
    }
    /**
     * CHALLENGE 11b: sign out by revoking this refresh token. Never fails: no token, an unknown token or an
     * already revoked one simply leaves nothing to do.
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null)
            return;
        refreshTokenRepository.findByTokenHashForUpdate(TokenHasher.sha256(rawRefreshToken)).ifPresent(token -> token.revoke(clock.instant()));
    }

    /** 256 bits from a cryptographically secure generator, URL-safe so it can sit in a cookie as is. */
    private String newRawToken() {
        var bytes = new byte[REFRESH_TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
