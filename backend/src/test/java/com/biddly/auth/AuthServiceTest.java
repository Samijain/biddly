package com.biddly.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.biddly.auth.dto.LoginRequest;
import com.biddly.auth.dto.RegisterRequest;
import com.biddly.common.BusinessRuleException;
import com.biddly.common.ConflictException;
import com.biddly.common.ForbiddenException;
import com.biddly.common.UnauthorizedException;
import com.biddly.user.Role;
import com.biddly.user.User;
import com.biddly.user.UserRepository;
import com.biddly.user.UserStatus;

/** Pure unit tests: no Spring, no database. BCrypt is real, with the lowest cost (4) so the tests stay fast. */
class AuthServiceTest {

    private static final String PASSWORD = "correct-horse-42";

    private UserRepository userRepository;
    private SessionService sessionService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private AuthService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        sessionService = mock(SessionService.class);
        service = new AuthService(userRepository, passwordEncoder, sessionService);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void storesABcryptHashAndNeverThePassword() {
        service.register(new RegisterRequest("Priya Shah", "priya@example.com", PASSWORD));

        var saved = savedUser();
        assertThat(saved.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPasswordHash())).isTrue();
    }

    @Test
    void givesTwoAccountsWithTheSamePasswordDifferentHashes() {
        service.register(new RegisterRequest("Priya Shah", "priya@example.com", PASSWORD));
        service.register(new RegisterRequest("Arjun Rao", "arjun@example.com", PASSWORD));

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(captor.capture());
        var hashes = captor.getAllValues().stream().map(User::getPasswordHash).toList();
        assertThat(hashes.get(0)).isNotEqualTo(hashes.get(1));
    }

    @Test
    void storesTheEmailInLowerCaseAndTrimsTheName() {
        var account = service.register(new RegisterRequest("  Priya Shah  ", "Priya.Shah@Example.COM", PASSWORD));

        assertThat(savedUser().getEmail()).isEqualTo("priya.shah@example.com");
        assertThat(savedUser().getDisplayName()).isEqualTo("Priya Shah");
        assertThat(account.email()).isEqualTo("priya.shah@example.com");
    }

    @Test
    void makesTheNewAccountAUser() {
        var account = service.register(new RegisterRequest("Priya Shah", "priya@example.com", PASSWORD));

        assertThat(account.role()).isEqualTo(Role.USER);
    }

    @Test
    void refusesAnEmailThatAlreadyHasAnAccount() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("Priya Shah", "priya@example.com", PASSWORD)))
                .isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.getCode()).isEqualTo("EMAIL_TAKEN"));
        verify(userRepository, never()).save(any());
    }

    /** Another registration for the same email committed between our check and our insert. */
    @Test
    void turnsALostRaceForTheEmailIntoEmailTaken() {
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"uq_users_email_lower\""));

        assertThatThrownBy(() -> service.register(new RegisterRequest("Priya Shah", "priya@example.com", PASSWORD)))
                .isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.getCode()).isEqualTo("EMAIL_TAKEN"));
    }

    /** 25 characters pass @Size, but "₹" is 3 bytes in UTF-8, so this is 75 bytes: past BCrypt's 72. */
    @Test
    void refusesAPasswordLongerThan72Bytes() {
        var password = "₹".repeat(25);
        assertThat(password.getBytes(StandardCharsets.UTF_8)).hasSize(75);

        assertThatThrownBy(() -> service.register(new RegisterRequest("Priya Shah", "priya@example.com", password)))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> assertThat(ex.getCode()).isEqualTo("PASSWORD_TOO_LONG"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void acceptsAPasswordOfExactly72Bytes() {
        var password = "a".repeat(72);

        service.register(new RegisterRequest("Priya Shah", "priya@example.com", password));

        assertThat(passwordEncoder.matches(password, savedUser().getPasswordHash())).isTrue();
    }

    private User savedUser() {
        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    class Login {

        private User priya;
        private final IssuedSession session = new IssuedSession("access-token", Instant.parse("2026-11-23T10:15:00Z"),
                "refresh-token", Instant.parse("2026-12-07T10:00:00Z"), null);

        @BeforeEach
        void setUp() {
            priya = new User("Priya Shah", "priya@example.com", passwordEncoder.encode(PASSWORD));
            when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
            when(userRepository.findByEmailIgnoreCase("priya@example.com")).thenReturn(Optional.of(priya));
            when(sessionService.issue(priya)).thenReturn(session);
        }

        @Test
        void startsASessionForTheRightPassword() {
            assertThat(service.login(new LoginRequest("priya@example.com", PASSWORD))).isSameAs(session);
        }

        @Test
        void refusesAWrongPassword() {
            assertInvalidCredentials(new LoginRequest("priya@example.com", "wrong-password"));
        }

        @Test
        void givesAnUnknownEmailTheSameAnswer() {
            assertInvalidCredentials(new LoginRequest("nobody@example.com", PASSWORD));
        }

        /** Like a Google-only account in 11d. */
        @Test
        void givesAnAccountWithoutAPasswordTheSameAnswer() {
            var googleOnly = new User("Arjun Rao", "arjun@example.com");
            when(userRepository.findByEmailIgnoreCase("arjun@example.com")).thenReturn(Optional.of(googleOnly));

            assertInvalidCredentials(new LoginRequest("arjun@example.com", PASSWORD));
        }

        /** 75 bytes. Spring's BCrypt answers "no match" here (only encode throws); it must stay a plain 401. */
        @Test
        void givesAPasswordBcryptCannotHandleTheSameAnswer() {
            assertInvalidCredentials(new LoginRequest("priya@example.com", "₹".repeat(25)));
        }

        @Test
        void refusesASuspendedAccountEvenWithTheRightPassword() {
            ReflectionTestUtils.setField(priya, "status", UserStatus.SUSPENDED);

            assertThatThrownBy(() -> service.login(new LoginRequest("priya@example.com", PASSWORD)))
                    .isInstanceOfSatisfying(ForbiddenException.class, ex -> assertThat(ex.getCode()).isEqualTo("ACCOUNT_SUSPENDED"));
            verify(sessionService, never()).issue(any());
        }

        /** Only someone who knows the password learns that the account is suspended. */
        @Test
        void checksThePasswordBeforeMentioningASuspension() {
            ReflectionTestUtils.setField(priya, "status", UserStatus.SUSPENDED);

            assertInvalidCredentials(new LoginRequest("priya@example.com", "wrong-password"));
        }

        private void assertInvalidCredentials(LoginRequest request) {
            assertThatThrownBy(() -> service.login(request))
                    .isInstanceOfSatisfying(UnauthorizedException.class, ex -> {
                        assertThat(ex.getCode()).isEqualTo("INVALID_CREDENTIALS");
                        assertThat(ex.getMessage()).isEqualTo("Wrong email or password.");
                    });
            verify(sessionService, never()).issue(any());
        }
    }
}