package com.ledgerline.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IdNumberValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"AB123456", "123456789", "X-123-4567", "P12345678", "  AB123456  "})
    void acceptsValidIds(String id) {
        assertThat(IdNumberValidator.isValid(id)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "12345", // too short
                "ABCDEFGH", // no digits
                "A12345678901234567890", // too long (21 characters)
                "111111", // a single repeated character
                "-123456", // leading hyphen
                "AB 123456", // embedded space
                "---123---" // only hyphens and digits around them
            })
    void rejectsInvalidIds(String id) {
        assertThat(IdNumberValidator.isValid(id)).isFalse();
    }
}
