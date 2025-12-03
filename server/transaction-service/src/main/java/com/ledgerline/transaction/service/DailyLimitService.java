package com.ledgerline.transaction.service;

import com.ledgerline.shared.exception.BusinessException;
import com.ledgerline.shared.money.MoneyFormatter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Atomic daily outgoing-limit counter. The upsert takes a row lock for the (customer, day) pair, so
 * two concurrent transfers cannot both read the same unused amount and then both pass the limit.
 * The increment runs on the caller's JDBC connection, in the same transaction as the PENDING
 * transaction row: if the payment is rejected or the database write fails, the reservation rolls
 * back.
 */
@Service
public class DailyLimitService {

    private static final ZoneId BANK_ZONE = ZoneId.of("America/Bogota");

    static final String DEFAULT_DAILY_LIMIT = "10000000";

    private static final String RESERVE_SQL =
            """
            INSERT INTO daily_limit_usage
                (customer_id, usage_date, used_amount)
            VALUES (?, ?, ?)
            ON CONFLICT (customer_id, usage_date)
            DO UPDATE SET
                used_amount = daily_limit_usage.used_amount + EXCLUDED.used_amount,
                updated_at = NOW()
            RETURNING used_amount
            """;

    private static final String RELEASE_SQL =
            """
            UPDATE daily_limit_usage
            SET used_amount = GREATEST(used_amount - ?, 0),
                updated_at = NOW()
            WHERE customer_id = ? AND usage_date = ?
            """;

    private final BigDecimal dailyLimit;
    private final JdbcTemplate jdbcTemplate;

    public DailyLimitService(
            @Value("${payments.limits.daily:" + DEFAULT_DAILY_LIMIT + "}") BigDecimal dailyLimit,
            JdbcTemplate jdbcTemplate) {
        this.dailyLimit = dailyLimit;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Adds the amount to today's usage and returns the date it was booked under. Throws (and rolls
     * the transaction back) when the new total exceeds the daily limit.
     */
    public LocalDate reserve(Long customerId, BigDecimal amount) {
        if (customerId == null) {
            throw new BusinessException("Could not resolve the customer for this payment");
        }
        LocalDate usageDate = LocalDate.now(BANK_ZONE);
        BigDecimal total =
                jdbcTemplate.queryForObject(
                        RESERVE_SQL, BigDecimal.class, customerId, usageDate, amount);

        if (total == null || total.compareTo(dailyLimit) > 0) {
            BigDecimal usedBefore = total == null ? BigDecimal.ZERO : total.subtract(amount);
            BigDecimal remaining = dailyLimit.subtract(usedBefore).max(BigDecimal.ZERO);
            throw new BusinessException(
                    "Amount exceeds your remaining daily limit of "
                            + MoneyFormatter.formatCop(remaining));
        }
        return usageDate;
    }

    /** Compensates a reservation when the transfer definitively fails. */
    public void release(Long customerId, LocalDate usageDate, BigDecimal amount) {
        if (customerId == null || usageDate == null || amount == null) {
            return;
        }
        jdbcTemplate.update(RELEASE_SQL, amount, customerId, usageDate);
    }
}
