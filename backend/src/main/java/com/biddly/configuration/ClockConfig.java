package com.biddly.configuration;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Services ask this Clock for "now" instead of calling Instant.now() directly,
 * so tests can freeze time and check rules such as "bids must arrive before endTime".
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
