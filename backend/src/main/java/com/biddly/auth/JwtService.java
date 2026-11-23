package com.biddly.auth;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.biddly.configuration.JwtConfig;
import com.biddly.user.User;

/** Creates Biddly's access tokens. Paste one into https://jwt.io to see that anyone can read the claims. */
@Service
public class JwtService {

    public static final Duration ACCESS_TOKEN_LIFETIME = Duration.ofMinutes(15);

    public record AccessToken(String value, Instant expiresAt) {
    }

    private final JwtEncoder encoder;

    public JwtService(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public AccessToken createAccessToken(User user, Instant now) {
        var expiresAt = now.plus(ACCESS_TOKEN_LIFETIME);
        var claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER)
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("name", user.getDisplayName())
                .claim("role", user.getRole().name())
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        var token = encoder.encode(JwtEncoderParameters.from(header, claims));
        return new AccessToken(token.getTokenValue(), expiresAt);
    }
}
