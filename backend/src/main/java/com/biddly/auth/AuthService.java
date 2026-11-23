package com.biddly.auth;

import com.biddly.common.BusinessRuleException;
import com.biddly.common.ConflictException;
import com.biddly.common.ForbiddenException;
import com.biddly.common.UnauthorizedException;
import com.biddly.user.User;
import com.biddly.user.UserStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biddly.auth.dto.AccountResponse;
import com.biddly.auth.dto.LoginRequest;
import com.biddly.auth.dto.RegisterRequest;
import com.biddly.user.UserRepository;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, SessionService sessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
    }

    /**
     * CHALLENGE 11b: sign in with an email and a password, and start a session.
     *
     * <ul>
     *   <li>The email is matched in any letter case.</li>
     *   <li>Unknown email, wrong password, an account without a password (such as the Google-only
     *       accounts of 11d) or a password BCrypt can't handle: all get the same 401 {@code INVALID_CREDENTIALS},
     *       "Wrong email or password." (R28). Never a 500, and never a hint about which part was wrong.</li>
     *   <li>Right password but a {@code SUSPENDED} account: 403 {@code ACCOUNT_SUSPENDED}, and no session.</li>
     *   <li>Otherwise: {@code sessionService.issue(user)}.</li>
     * </ul>
     */
    @Transactional
    public IssuedSession login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(AuthService::wrongCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()))
            throw wrongCredentials();
        if (user.getStatus() == UserStatus.SUSPENDED)
            throw new ForbiddenException("ACCOUNT_SUSPENDED", "Your account has been suspended");

        return sessionService.issue(user);
    }

    /** One answer for every failed sign-in, so nobody can tell which part was wrong (R28). */
    private static UnauthorizedException wrongCredentials() {
        return new UnauthorizedException("INVALID_CREDENTIALS", "Wrong email or password.");
    }

    /** Ready for you: the account behind a verified access token (GET /auth/me). */
    @Transactional(readOnly = true)
    public AccountResponse currentAccount(Long userId) {
        return userRepository.findById(userId)
                .map(AccountResponse::from)
                .orElseThrow(() -> new UnauthorizedException("INVALID_ACCESS_TOKEN", "This account no longer exists."));
    }

    /**
     * CHALLENGE 11a: create an account with an email and a password.
     *
     * <ul>
     *   <li>The email is stored in lower case and the display name without surrounding spaces.</li>
     *   <li>A password longer than 72 bytes in UTF-8 is refused: 422 {@code PASSWORD_TOO_LONG} (R28).
     *       Bean Validation has already checked 8 to 72 <em>characters</em>.</li>
     *   <li>An email that already has an account, in any letter case, is refused: 409 {@code EMAIL_TAKEN} (R27).
     *       Two registrations racing for the same email must give the same answer, never a 500.</li>
     *   <li>Only the BCrypt hash is stored (R28). The new account is a {@code USER}.</li>
     * </ul>
     */
    @Transactional
    public AccountResponse register(RegisterRequest request) {
        String name = request.displayName().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if(request.password().getBytes(StandardCharsets.UTF_8).length > 72){
            throw new BusinessRuleException("PASSWORD_TOO_LONG","Password too long");
        }
        if(userRepository.existsByEmailIgnoreCase(email)){
            throw new ConflictException("EMAIL_TAKEN","Email is already taken");
        }
        String passwordHash = passwordEncoder.encode(request.password());
        User newUser = new User(name, email, passwordHash);
        User savedUser;
        try{
            savedUser = userRepository.save(newUser);
        }catch(DataIntegrityViolationException ex){
            if (ex.getMessage() != null && ex.getMessage().contains("uq_users_email_lower")) {
                throw new ConflictException("EMAIL_TAKEN", "Email is already taken");
            }
            throw ex;
        }

        return AccountResponse.from(savedUser);
    }
}
