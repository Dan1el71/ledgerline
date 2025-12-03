package com.ledgerline.transaction.security;

import com.ledgerline.shared.exception.StepUpRequiredException;
import com.ledgerline.shared.security.StepUpConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Verifies the re-authentication token minted by auth-service. transaction-service never accepts a
 * normal access token in its place: the token must carry {@code scope=step-up}, the {@code
 * transaction-service} audience and at most five minutes between issuance and expiry.
 */
@Service
public class StepUpTokenService {

    private static final long MAX_TTL_MILLIS = StepUpConstants.TTL_SECONDS * 1000;

    private final SecretKey signingKey;

    public StepUpTokenService(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set and at least 32 characters long");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Returns the subject when the token proves a recent confirmation for that user. */
    public String requireValidFor(String token, String expectedUserId) {
        if (token == null || token.isBlank()) {
            throw new StepUpRequiredException(
                    "Confirm your password to pay an account that is not yours");
        }
        try {
            Claims claims =
                    Jwts.parser()
                            .verifyWith(signingKey)
                            .requireAudience(StepUpConstants.AUDIENCE)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

            if (!StepUpConstants.SCOPE.equals(
                    claims.get(StepUpConstants.CLAIM_SCOPE, String.class))) {
                throw new StepUpRequiredException("The confirmation token is not valid here");
            }
            String subject = claims.getSubject();
            if (subject == null || !subject.equals(expectedUserId)) {
                throw new StepUpRequiredException("The confirmation belongs to a different user");
            }
            Date issuedAt = claims.getIssuedAt();
            Date expiration = claims.getExpiration();
            if (issuedAt == null || expiration == null) {
                throw new StepUpRequiredException("The confirmation token is incomplete");
            }
            long lifetime = expiration.getTime() - issuedAt.getTime();
            if (lifetime <= 0 || lifetime > MAX_TTL_MILLIS) {
                throw new StepUpRequiredException("The confirmation token lifetime is not valid");
            }
            return subject;
        } catch (JwtException | IllegalArgumentException e) {
            throw new StepUpRequiredException(
                    "Your confirmation expired. Enter your password again");
        }
    }
}
