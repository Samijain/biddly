package com.biddly.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.biddly.TestcontainersConfiguration;
import com.biddly.auction.AuctionRepository;
import com.biddly.bid.BidRepository;
import com.biddly.user.User;
import com.biddly.user.UserRepository;
import com.biddly.user.UserStatus;

import jakarta.servlet.http.Cookie;

/** Sign-in, refresh, sign-out and the access token, over HTTP against a real PostgreSQL. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSessionIntegrationTest {

    private static final String PASSWORD = "correct-horse-42";
    private static final Pattern REFRESH_COOKIE = Pattern.compile(AuthController.REFRESH_COOKIE + "=([^;]*)");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuctionRepository auctionRepository;

    @Autowired
    BidRepository bidRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtService jwtService;

    private User priya;

    @BeforeEach
    void setUp() {
        bidRepository.deleteAll();
        auctionRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        priya = userRepository.save(new User("Priya Shah", "priya@example.com", passwordEncoder.encode(PASSWORD)));
    }

    // --- The access token. These pass already: Spring Security checks it before any controller runs. ---

    @Test
    void authMeNeedsAValidAccessToken() {
        assertThat(me(null)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(me("not-a-jwt")).hasStatus(HttpStatus.UNAUTHORIZED);

        var valid = jwtService.createAccessToken(priya, Instant.now()).value();
        assertThat(me(valid)).hasStatusOk().bodyJson().extractingPath("$.email").isEqualTo("priya@example.com");
    }

    /** Older than 15 minutes plus Spring's 60 seconds of allowed clock difference. */
    @Test
    void refusesAnExpiredAccessToken() {
        var expired = jwtService.createAccessToken(priya, Instant.now().minus(20, ChronoUnit.MINUTES)).value();

        assertThat(me(expired)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    /** Anyone can read and edit the payload; nobody without the key can make the signature match it again. */
    @Test
    void refusesAnAccessTokenWhosePayloadWasEdited() {
        var parts = jwtService.createAccessToken(priya, Instant.now()).value().split("\\.");
        var payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        var someoneElse = payload.replace("\"sub\":\"" + priya.getId() + "\"", "\"sub\":\"999\"");
        assertThat(someoneElse).isNotEqualTo(payload);
        var forged = parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(someoneElse.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];

        assertThat(me(forged)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    // --- Challenge 11b ---

    @Test
    void signInReturnsAnAccessTokenAndSetsAProtectedRefreshCookie() {
        var result = login("Priya@Example.com", PASSWORD);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
        assertThat(result).bodyJson().extractingPath("$.account.email").isEqualTo("priya@example.com");
        assertThat(result).bodyText().doesNotContainIgnoringCase("refresh");

        var setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("HttpOnly", "SameSite=Strict", "Path=/api/v1/auth", "Max-Age=1209600");
        assertThat(me(accessTokenOf(result))).hasStatusOk();
    }

    @Test
    void aFailedSignInGetsOneVagueAnswerAndNoCookie() {
        for (var result : new MvcTestResult[] { login("priya@example.com", "wrong-password"), login("nobody@example.com", PASSWORD) }) {
            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_CREDENTIALS");
            assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("Wrong email or password.");
            assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        }
        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    void aSuspendedAccountCannotSignIn() {
        ReflectionTestUtils.setField(priya, "status", UserStatus.SUSPENDED);
        userRepository.save(priya);

        var result = login("priya@example.com", PASSWORD);

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("ACCOUNT_SUSPENDED");
        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    void refreshSwapsTheCookieForANewOne() {
        var first = refreshTokenOf(login("priya@example.com", PASSWORD));

        var result = refresh(first);

        assertThat(result).hasStatusOk();
        var second = refreshTokenOf(result);
        assertThat(second).isNotEqualTo(first);
        assertThat(me(accessTokenOf(result))).hasStatusOk();
        assertThat(refresh(second)).hasStatusOk();
    }

    /** The old token comes back after rotation: it was copied. Both copies stop working. */
    @Test
    void aReusedRefreshTokenEndsTheWholeSession() {
        var first = refreshTokenOf(login("priya@example.com", PASSWORD));
        var second = refreshTokenOf(refresh(first));

        var reuse = refresh(first);

        assertThat(reuse).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(reuse).bodyJson().extractingPath("$.code").isEqualTo("INVALID_REFRESH_TOKEN");
        assertThat(refresh(second)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void signOutEndsTheSessionAndDeletesTheCookie() {
        var token = refreshTokenOf(login("priya@example.com", PASSWORD));

        var result = mvc.post().uri("/api/v1/auth/logout").cookie(new Cookie(AuthController.REFRESH_COOKIE, token)).exchange();

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
        assertThat(refresh(token)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void signOutWithoutASessionStillSucceeds() {
        assertThat(mvc.post().uri("/api/v1/auth/logout").exchange()).hasStatus(HttpStatus.NO_CONTENT);
    }

    private MvcTestResult login(String email, String password) {
        var body = """
                {"email": "%s", "password": "%s"}""".formatted(email, password);
        return mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private MvcTestResult refresh(String refreshToken) {
        return mvc.post().uri("/api/v1/auth/refresh").cookie(new Cookie(AuthController.REFRESH_COOKIE, refreshToken)).exchange();
    }

    private MvcTestResult me(String accessToken) {
        var request = mvc.get().uri("/api/v1/auth/me");
        if (accessToken != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return request.exchange();
    }

    private static String refreshTokenOf(MvcTestResult result) {
        var matcher = REFRESH_COOKIE.matcher(String.valueOf(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)));
        assertThat(matcher.find()).as("a refresh cookie was set").isTrue();
        return matcher.group(1);
    }

    private static String accessTokenOf(MvcTestResult result) {
        var json = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        var matcher = Pattern.compile("\"accessToken\":\"([^\"]+)\"").matcher(json);
        assertThat(matcher.find()).as("an access token in the body").isTrue();
        return matcher.group(1);
    }
}
