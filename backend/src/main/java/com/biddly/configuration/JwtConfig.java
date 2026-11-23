package com.biddly.configuration;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Biddly signs its own access tokens with HS256: one shared secret both signs and verifies, and only this backend
 * holds it. Every backend copy must use the same secret, or a token from one copy fails on another.
 */
@Configuration(proxyBeanMethods = false)
public class JwtConfig {

    public static final String ISSUER = "biddly";

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;

    public JwtConfig(@Value("${biddly.auth.jwt.secret}") String secret) {
        var bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("biddly.auth.jwt.secret (JWT_SECRET) must be at least 32 bytes for HS256");
        }
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    /** Checks the signature, the expiry (with Spring's default 60 s of clock skew) and that we issued it. */
    @Bean
    JwtDecoder jwtDecoder() {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }
}
