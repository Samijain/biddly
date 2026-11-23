package com.biddly.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.biddly.TestcontainersConfiguration;
import com.biddly.auction.AuctionRepository;
import com.biddly.bid.BidRepository;
import com.biddly.user.UserRepository;

/** POST /api/v1/auth/register over HTTP, against a real PostgreSQL (Docker must be running). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuctionRepository auctionRepository;

    @Autowired
    BidRepository bidRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        bidRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createsAnAccountAndNeverReturnsThePassword() {
        var result = register("Priya Shah", "Priya.Shah@Example.com", "correct-horse-42");

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.email").isEqualTo("priya.shah@example.com");
        assertThat(result).bodyJson().extractingPath("$.displayName").isEqualTo("Priya Shah");
        assertThat(result).bodyJson().extractingPath("$.role").isEqualTo("USER");
        assertThat(result).bodyText().doesNotContainIgnoringCase("password");

        var stored = userRepository.findAll();
        assertThat(stored).singleElement().satisfies(user ->
                assertThat(passwordEncoder.matches("correct-horse-42", user.getPasswordHash())).isTrue());
    }

    @Test
    void refusesAnEmailThatAlreadyHasAnAccountInAnyCase() {
        assertThat(register("Priya Shah", "priya@example.com", "correct-horse-42")).hasStatus(HttpStatus.CREATED);

        var second = register("Someone Else", "PRIYA@example.com", "another-pass-99");

        assertThat(second).hasStatus(HttpStatus.CONFLICT);
        assertThat(second).bodyJson().extractingPath("$.code").isEqualTo("EMAIL_TAKEN");
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void refusesAPasswordLongerThan72Bytes() {
        var result = register("Priya Shah", "priya@example.com", "₹".repeat(25));

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("PASSWORD_TOO_LONG");
        assertThat(userRepository.count()).isZero();
    }

    /** Bean Validation on RegisterRequest: passes already, and must keep passing. */
    @Test
    void validatesTheFieldsBeforeAnythingElse() {
        var result = register(" ", "not-an-email", "short");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.fieldErrors.displayName").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.fieldErrors.email").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.fieldErrors.password").isEqualTo("must be 8 to 72 characters");
        assertThat(userRepository.count()).isZero();
    }

    private MvcTestResult register(String displayName, String email, String password) {
        var body = """
                {"displayName": "%s", "email": "%s", "password": "%s"}""".formatted(displayName, email, password);
        return mvc.post().uri("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }
}
