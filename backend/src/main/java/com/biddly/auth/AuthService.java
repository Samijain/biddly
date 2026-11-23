package com.biddly.auth;

import com.biddly.common.BusinessRuleException;
import com.biddly.common.ConflictException;
import com.biddly.user.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biddly.auth.dto.AccountResponse;
import com.biddly.auth.dto.RegisterRequest;
import com.biddly.user.UserRepository;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
