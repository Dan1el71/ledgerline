package com.ledgerline.transaction.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledgerline.shared.exception.BusinessException;
import com.ledgerline.shared.exception.ForbiddenException;
import com.ledgerline.transaction.client.CustomerClient.CustomerSummary;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentPolicyTest {

    private final PaymentPolicy policy =
            new PaymentPolicy(new BigDecimal(PaymentPolicy.DEFAULT_PER_TRANSACTION_LIMIT));

    private final CustomerSummary verifiedCustomer = new CustomerSummary(1L, 42L, "VERIFIED");

    @Test
    void rejectsUnverifiedCustomers() {
        CustomerSummary pending = new CustomerSummary(1L, 42L, "PENDING");

        assertThatThrownBy(() -> policy.assertCustomerMayPay(pending, new BigDecimal("1000")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("verified");
    }

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
    void acceptsPaymentsWithinThePerPaymentLimit() {
        assertThatCode(
                        () ->
                                policy.assertCustomerMayPay(
                                        verifiedCustomer, new BigDecimal("5000000.00")))
                .doesNotThrowAnyException();
    }
}
