package com.biddly.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import com.biddly.TestcontainersConfiguration;
import com.biddly.auction.AuctionRepository;
import com.biddly.bid.BidRepository;
import com.biddly.user.UserRepository;

/**
 * Several sign-ups for one email, sent at the same moment over real HTTP.
 *
 * To make the race happen every time, hashing pauses for {@link #HASH_PAUSE_MS}. Every request then gets past
 * the "is this email taken?" check before any of them has saved its account.
 */
@Import({ TestcontainersConfiguration.class, RegistrationRaceIntegrationTest.SlowHashConfig.class })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RegistrationRaceIntegrationTest {

    private static final int COPIES = 4;
    private static final long HASH_PAUSE_MS = 500;

    @TestConfiguration(proxyBeanMethods = false)
    static class SlowHashConfig {
        @Bean
        @Primary
        PasswordEncoder slowPasswordEncoder() {
            var bcrypt = new BCryptPasswordEncoder(4);
            return new PasswordEncoder() {
                @Override
                public String encode(CharSequence rawPassword) {
                    try {
                        Thread.sleep(HASH_PAUSE_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return bcrypt.encode(rawPassword);
                }

                @Override
                public boolean matches(CharSequence rawPassword, String encodedPassword) {
                    return bcrypt.matches(rawPassword, encodedPassword);
                }
            };
        }
    }

    private record Outcome(int status, String body) {
        boolean hasCode(String code) {
            return body != null && body.contains("\"code\":\"" + code + "\"");
        }
    }

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuctionRepository auctionRepository;

    @Autowired
    BidRepository bidRepository;

    @BeforeEach
    void setUp() {
        bidRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void givesOneAccountAndTellsTheOthersTheEmailIsTaken() throws Exception {
        var client = RestClient.create("http://localhost:" + port);
        var startingGun = new CountDownLatch(1);
        List<Outcome> outcomes = new ArrayList<>();
        try (var pool = Executors.newFixedThreadPool(COPIES)) {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (int i = 0; i < COPIES; i++) {
                var body = Map.of("displayName", "Priya " + i, "email", "priya@example.com", "password", "correct-horse-42");
                futures.add(pool.submit(() -> {
                    startingGun.await();
                    return client.post()
                            .uri("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body)
                            .exchange((request, response) -> new Outcome(response.getStatusCode().value(), response.bodyTo(String.class)));
                }));
            }
            startingGun.countDown();
            for (var future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
        }

        assertThat(outcomes).filteredOn(o -> o.status() == 201).hasSize(1);
        assertThat(outcomes).filteredOn(o -> o.status() == 409 && o.hasCode("EMAIL_TAKEN")).hasSize(COPIES - 1);
        assertThat(userRepository.count()).isEqualTo(1);
    }
}
