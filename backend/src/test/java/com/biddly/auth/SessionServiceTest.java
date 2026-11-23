package com.biddly.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.biddly.common.UnauthorizedException;
import com.biddly.user.User;

/** Pure unit tests: no Spring, no database. Time is frozen and the JWT is a stand-in string. */
class SessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");

    private RefreshTokenRepository refreshTokenRepository;
    private SessionService service;
    private final User priya = user(7L, "Priya Shah");

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        var jwtService = mock(JwtService.class);
        when(jwtService.createAccessToken(any(User.class), any(Instant.class)))
                .thenAnswer(invocation -> new JwtService.AccessToken("access-token-" + invocation.getArgument(0, User.class).getId(),
                        invocation.getArgument(1, Instant.class).plus(JwtService.ACCESS_TOKEN_LIFETIME)));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.empty());
        service = new SessionService(refreshTokenRepository, jwtService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Nested
    class Issue {

        @Test
        void storesOnlyAHashOfTheRefreshToken() {
            var session = service.issue(priya);

            var stored = savedToken();
            assertThat(stored.getTokenHash()).isEqualTo(TokenHasher.sha256(session.refreshToken())).isNotEqualTo(session.refreshToken());
            assertThat(stored.getUser()).isSameAs(priya);
            assertThat(stored.getExpiresAt()).isEqualTo(NOW.plus(SessionService.REFRESH_TOKEN_LIFETIME));
            assertThat(session.accessToken()).isEqualTo("access-token-7");
            assertThat(session.accessTokenExpiresAt()).isEqualTo(NOW.plus(JwtService.ACCESS_TOKEN_LIFETIME));
            assertThat(session.account().email()).isEqualTo("priya@example.com");
        }

        /** 32 random bytes are 43 characters of URL-safe Base64. */
        @Test
        void givesEverySessionADifferentRandomToken() {
            var first = service.issue(priya).refreshToken();
            var second = service.issue(priya).refreshToken();

            assertThat(first).hasSize(43).isNotEqualTo(second);
        }
    }

    @Nested
    class Refresh {

        @Test
        void revokesTheOldTokenAndStartsANewSession() {
            var old = storedToken("old-token", NOW.plusSeconds(60));

            var session = service.refresh("old-token");

            assertThat(old.isRevoked()).isTrue();
            assertThat(old.getRevokedAt()).isEqualTo(NOW);
            assertThat(session.refreshToken()).isNotEqualTo("old-token");
            assertThat(savedToken().getTokenHash()).isEqualTo(TokenHasher.sha256(session.refreshToken()));
            assertThat(session.account().id()).isEqualTo(7L);
        }

        @Test
        void refusesAMissingToken() {
            assertInvalidRefreshToken(null);
            assertInvalidRefreshToken("");
        }

        @Test
        void refusesATokenWeNeverIssued() {
            assertInvalidRefreshToken("made-up-token");
        }

        /** Expired at exactly expiresAt. An expired token is just old, not stolen: no other session ends. */
        @Test
        void refusesAnExpiredToken() {
            storedToken("old-token", NOW);

            assertInvalidRefreshToken("old-token");
            verify(refreshTokenRepository, never()).revokeAllForUser(anyLong(), any());
        }

        /** A revoked token coming back means two parties hold it. We can't tell which is the real user. */
        @Test
        void treatsAReusedTokenAsStolenAndEndsEverySessionOfThatUser() {
            var old = storedToken("old-token", NOW.plusSeconds(60));
            old.revoke(NOW.minusSeconds(30));

            assertInvalidRefreshToken("old-token");
            verify(refreshTokenRepository).revokeAllForUser(7L, NOW);
        }

        private void assertInvalidRefreshToken(String rawToken) {
            assertThatThrownBy(() -> service.refresh(rawToken))
                    .isInstanceOfSatisfying(UnauthorizedException.class, ex -> assertThat(ex.getCode()).isEqualTo("INVALID_REFRESH_TOKEN"));
            verify(refreshTokenRepository, never()).save(any());
        }
    }

    @Nested
    class Logout {

        @Test
        void revokesTheToken() {
            var token = storedToken("my-token", NOW.plusSeconds(60));

            service.logout("my-token");

            assertThat(token.getRevokedAt()).isEqualTo(NOW);
        }

        @Test
        void neverFails() {
            var alreadyRevoked = storedToken("old-token", NOW.plusSeconds(60));
            alreadyRevoked.revoke(NOW.minusSeconds(30));

            assertThatCode(() -> {
                service.logout(null);
                service.logout("");
                service.logout("made-up-token");
                service.logout("old-token");
            }).doesNotThrowAnyException();
            assertThat(alreadyRevoked.getRevokedAt()).isEqualTo(NOW.minusSeconds(30));
            verify(refreshTokenRepository, never()).revokeAllForUser(anyLong(), any());
        }
    }

    private RefreshToken storedToken(String rawToken, Instant expiresAt) {
        var token = new RefreshToken(priya, TokenHasher.sha256(rawToken), NOW.minusSeconds(3600), expiresAt);
        when(refreshTokenRepository.findByTokenHashForUpdate(TokenHasher.sha256(rawToken))).thenReturn(Optional.of(token));
        return token;
    }

    private RefreshToken savedToken() {
        var captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private static User user(Long id, String name) {
        var user = new User(name, "priya@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
