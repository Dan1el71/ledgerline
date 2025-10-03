package com.ledgerline.transaction.service;

import com.ledgerline.shared.exception.BusinessException;
import com.ledgerline.shared.exception.ForbiddenException;
import com.ledgerline.shared.money.MoneyFormatter;
import com.ledgerline.transaction.client.CustomerClient.CustomerSummary;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Customer-facing payment rules: verified identity and the per-payment limit. The daily limit is
 * not checked here: it is reserved atomically by {@link DailyLimitService} in the same transaction
 * that records the instruction, which removes the check-then-act race between concurrent payments.
 */
@Component
public class PaymentPolicy {

    static final String DEFAULT_PER_TRANSACTION_LIMIT = "5000000";

    private final BigDecimal perTransactionLimit;

    public PaymentPolicy(
            @Value("${payments.limits.per-transaction:" + DEFAULT_PER_TRANSACTION_LIMIT + "}")
                    BigDecimal perTransactionLimit) {
        this.perTransactionLimit = perTransactionLimit;
    }

    public void assertCustomerMayPay(CustomerSummary customer, BigDecimal amount) {
        if (!customer.verified()) {
            throw new ForbiddenException(
                    "Your identity must be verified before you can move money");
        }
        if (amount.compareTo(perTransactionLimit) > 0) {
            throw new BusinessException(
                    "Amount exceeds the per-payment limit of "
                            + MoneyFormatter.formatCop(perTransactionLimit));
        }
    }
}
