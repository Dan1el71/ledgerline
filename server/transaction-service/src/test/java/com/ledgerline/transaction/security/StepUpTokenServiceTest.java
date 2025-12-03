package com.ledgerline.transaction.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledgerline.shared.exception.StepUpRequiredException;
import com.ledgerline.shared.security.StepUpConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class StepUpTokenServiceTest {

    private static final String SECRET = "test-jwt-secret-that-is-long-enough-0123456789abcdef";
    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private final StepUpTokenService service = new StepUpTokenService(SECRET);

    @Test
    void acceptsAShortLivedTokenForTheSameUser() {
        String token = token("42", "step-up", StepUpConstants.AUDIENCE, 120);

        assertThat(service.requireValidFor(token, "42")).isEqualTo("42");
    }

    @Test
    void rejectsAnExpiredToken() {
        String token =
                tokenAt(
                        "42",
                        "step-up",
                        StepUpConstants.AUDIENCE,
                        Instant.now().minus(10, ChronoUnit.MINUTES),
                        Instant.now().minus(5, ChronoUnit.MINUTES));

        assertThatThrownBy(() -> service.requireValidFor(token, "42"))
                .isInstanceOf(StepUpRequiredException.class);
    }

    @Test
    void rejectsANormalAccessToken() {
        String token = token("42", "access", StepUpConstants.AUDIENCE, 120);

        assertThatThrownBy(() -> service.requireValidFor(token, "42"))
                .isInstanceOf(StepUpRequiredException.class);
    }

    @Test
    void rejectsATokenForAnotherAudience() {
        String token = token("42", "step-up", "account-service", 120);

        assertThatThrownBy(() -> service.requireValidFor(token, "42"))
                .isInstanceOf(StepUpRequiredException.class);
    }

    @Test
    void rejectsATokenForAnotherUser() {
        String token = token("7", "step-up", StepUpConstants.AUDIENCE, 120);

        assertThatThrownBy(() -> service.requireValidFor(token, "42"))
                .isInstanceOf(StepUpRequiredException.class)
                .hasMessageContaining("different user");
    }

    @Test
    void rejectsATokenMintedWithALongerLifetime() {
        String token = token("42", "step-up", StepUpConstants.AUDIENCE, 600);

        assertThatThrownBy(() -> service.requireValidFor(token, "42"))
                .isInstanceOf(StepUpRequiredException.class)
                .hasMessageContaining("lifetime");
    }

    private static String token(
            String subject, String scope, String audience, long lifetimeSeconds) {
        Instant now = Instant.now();
        return tokenAt(subject, scope, audience, now, now.plusSeconds(lifetimeSeconds));
    }

    private static String tokenAt(
            String subject, String scope, String audience, Instant issuedAt, Instant expiration) {
        return Jwts.builder()
                .subject(subject)
                .audience()
                .add(audience)
                .and()
                .claim("scope", scope)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(KEY)
                .compact();
    }
}
