package com.ledgerline.transaction.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CurrencyValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void transferInEurIsRejected() {
        TransferRequest request =
                TransferRequest.builder()
                        .sourceAccountId(1L)
                        .targetAccountId(2L)
                        .amount(new BigDecimal("100.00"))
                        .currency("EUR")
                        .build();

        assertThat(validator.validate(request))
                .extracting(ConstraintViolation::getMessage)
                .contains("Only COP is supported");
    }

    @Test
    void depositInUsdIsRejected() {
        DepositRequest request =
                DepositRequest.builder()
                        .accountId(1L)
                        .amount(new BigDecimal("100.00"))
                        .currency("USD")
                        .build();

        assertThat(validator.validate(request))
                .extracting(ConstraintViolation::getMessage)
                .contains("Only COP is supported");
    }

    @Test
    void withdrawInEurIsRejected() {
        WithdrawRequest request =
                WithdrawRequest.builder()
                        .accountId(1L)
                        .amount(new BigDecimal("100.00"))
                        .currency("EUR")
                        .build();

        assertThat(validator.validate(request))
                .extracting(ConstraintViolation::getMessage)
                .contains("Only COP is supported");
    }

    @Test
    void requestsDefaultToCop() {
        TransferRequest request =
                TransferRequest.builder()
                        .sourceAccountId(1L)
                        .targetAccountId(2L)
                        .amount(new BigDecimal("100.00"))
                        .build();

        assertThat(request.getCurrency()).isEqualTo("COP");
        assertThat(validator.validate(request)).isEmpty();
    }
}
