package com.biddly.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled} methods unless {@code biddly.scheduler.enabled=false} (the test profile). */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "biddly.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
