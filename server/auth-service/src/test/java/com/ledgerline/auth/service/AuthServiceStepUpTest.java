package com.ledgerline.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ledgerline.auth.dto.StepUpResponse;
import com.ledgerline.auth.entity.UserEntity;
import com.ledgerline.auth.repository.UserRepository;
import com.ledgerline.shared.exception.UnauthorizedException;
import com.ledgerline.shared.security.StepUpConstants;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceStepUpTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

    private final AuthService authService =
            new AuthService(userRepository, jwtService, passwordEncoder, redisTemplate);

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

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void correctPasswordMintsAShortLivedToken() {
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123!", "hash")).thenReturn(true);
        when(jwtService.generateStepUpToken(user)).thenReturn("step-up-token");

        StepUpResponse response = authService.stepUp(42L, "Secret123!");

        assertThat(response.getStepUpToken()).isEqualTo("step-up-token");
        assertThat(response.getExpiresIn()).isEqualTo(StepUpConstants.TTL_SECONDS);
        verify(valueOperations).get("step-up:failures:42");
    }

    @Test
    void wrongPasswordCountsAFailureAndDoesNotMintAToken() {
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        when(valueOperations.increment("step-up:failures:42")).thenReturn(1L);

        assertThatThrownBy(() -> authService.stepUp(42L, "wrong"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("incorrect");
    }

    @Test
    void lockedUserCannotKeepGuessing() {
        when(valueOperations.get("step-up:failures:42")).thenReturn("5");

        assertThatThrownBy(() -> authService.stepUp(42L, "whatever"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Too many");
    }

    @Test
    void unknownUserCannotMintAToken() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());
        when(valueOperations.increment("step-up:failures:42")).thenReturn(1L);

        assertThatThrownBy(() -> authService.stepUp(42L, "whatever"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void disabledUserCannotMintAToken() {
        user.setEnabled(false);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(valueOperations.increment("step-up:failures:42")).thenReturn(1L);

        assertThatThrownBy(() -> authService.stepUp(42L, "whatever"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
