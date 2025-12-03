package com.ledgerline.transaction.service;

import com.ledgerline.shared.exception.BusinessException;
import com.ledgerline.shared.exception.ForbiddenException;
import com.ledgerline.shared.money.MoneyFormatter;
import com.ledgerline.transaction.client.CustomerClient.CustomerSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Customer-facing payment rules: verified identity, per-payment and daily outgoing limits. */
@Component
public class PaymentPolicy {

    private static final ZoneId BANK_ZONE = ZoneId.of("America/Bogota");

    static final String DEFAULT_PER_TRANSACTION_LIMIT = "5000000";
    static final String DEFAULT_DAILY_LIMIT = "10000000";

    private final BigDecimal perTransactionLimit;
    private final BigDecimal dailyLimit;
    private final TransactionService transactionService;

    public PaymentPolicy(
            @Value("${payments.limits.per-transaction:" + DEFAULT_PER_TRANSACTION_LIMIT + "}")
                    BigDecimal perTransactionLimit,
            @Value("${payments.limits.daily:" + DEFAULT_DAILY_LIMIT + "}") BigDecimal dailyLimit,
            TransactionService transactionService) {
        this.perTransactionLimit = perTransactionLimit;
        this.dailyLimit = dailyLimit;
        this.transactionService = transactionService;
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
        Instant startOfDay = LocalDate.now(BANK_ZONE).atStartOfDay(BANK_ZONE).toInstant();
        BigDecimal usedToday = transactionService.outgoingSince(customer.id(), startOfDay);
        if (usedToday.add(amount).compareTo(dailyLimit) > 0) {
            throw new BusinessException(
                    "Amount exceeds your remaining daily limit of "
                            + MoneyFormatter.formatCop(
                                    dailyLimit.subtract(usedToday).max(BigDecimal.ZERO)));
        }
    }
}
