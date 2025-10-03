package com.ledgerline.transaction.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ledgerline.shared.exception.BusinessException;
import com.ledgerline.transaction.client.CustomerClient.CustomerSummary;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PaymentPolicyTest {

    private final TransactionService transactionService = mock(TransactionService.class);

    private final PaymentPolicy policy =
            new PaymentPolicy(
                    new BigDecimal(PaymentPolicy.DEFAULT_PER_TRANSACTION_LIMIT),
                    new BigDecimal(PaymentPolicy.DEFAULT_DAILY_LIMIT),
                    transactionService);

    private final CustomerSummary verifiedCustomer = new CustomerSummary(1L, 42L, "VERIFIED");

    @Test
    void rejectsPaymentsAboveThePerPaymentLimitWithCopAmount() {
        assertThatThrownBy(
                        () ->
                                policy.assertCustomerMayPay(
                                        verifiedCustomer, new BigDecimal("5000000.01")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("COP 5,000,000.00")
                .hasMessageNotContaining("R");
    }

    @Test
    void rejectsPaymentsThatExceedTheRemainingDailyLimitWithCopAmount() {
        when(transactionService.outgoingSince(eq(1L), any(Instant.class)))
                .thenReturn(new BigDecimal("8000000.00"));

        assertThatThrownBy(
                        () ->
                                policy.assertCustomerMayPay(
                                        verifiedCustomer, new BigDecimal("2000001.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("COP 2,000,000.00")
                .hasMessageNotContaining("R");
    }

    @Test
    void acceptsPaymentsWithinBothLimits() {
        when(transactionService.outgoingSince(eq(1L), any(Instant.class)))
                .thenReturn(new BigDecimal("8000000.00"));

        assertThatCode(
                        () ->
                                policy.assertCustomerMayPay(
                                        verifiedCustomer, new BigDecimal("2000000.00")))
                .doesNotThrowAnyException();
    }
}
