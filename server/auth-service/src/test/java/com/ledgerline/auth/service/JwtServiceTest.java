package com.ledgerline.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledgerline.auth.entity.UserEntity;
import com.ledgerline.shared.security.StepUpConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-jwt-secret-that-is-long-enough-for-hs256-0123456789";

    private final JwtService jwtService = new JwtService(SECRET, 900_000, 604_800_000);

    private final UserEntity user =
            UserEntity.builder()
                    .id(42L)
                    .email("ada@ledgerline.local")
                    .passwordHash("hash")
                    .firstName("Ada")
                    .lastName("Lovelace")
                    .role("CUSTOMER")
                    .enabled(true)
                    .build();

    @Test
    void accessTokenCarriesIdentityAndType() {
        Claims claims = jwtService.validateToken(jwtService.generateAccessToken(user));

        assertThat(jwtService.extractUserId(claims)).isEqualTo("42");
        assertThat(jwtService.extractEmail(claims)).isEqualTo("ada@ledgerline.local");
        assertThat(jwtService.extractRole(claims)).isEqualTo("CUSTOMER");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(jwtService.extractJti(claims)).isNotBlank();
    }

    @Test
    void refreshTokenIsMarkedAsRefresh() {
        Claims claims = jwtService.validateToken(jwtService.generateRefreshToken(user));

        assertThat(claims.get("type", String.class)).isEqualTo("refresh");
    }

    @Test
    void stepUpTokenIsScopedToTransactionServiceAndShortLived() {
        Claims claims = jwtService.validateToken(jwtService.generateStepUpToken(user));

        assertThat(jwtService.extractUserId(claims)).isEqualTo("42");
        assertThat(claims.get(StepUpConstants.CLAIM_SCOPE, String.class))
                .isEqualTo(StepUpConstants.SCOPE);
        assertThat(claims.getAudience()).containsExactly(StepUpConstants.AUDIENCE);
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isBetween(1L, StepUpConstants.TTL_SECONDS * 1000);
    }

    @Test
    void aTokenSignedWithAnotherKeyIsRejected() {
        JwtService other = new JwtService(SECRET + "-different", 900_000, 604_800_000);

        assertThatThrownBy(() -> jwtService.validateToken(other.generateAccessToken(user)))
                .isInstanceOf(JwtException.class);
    }
}
