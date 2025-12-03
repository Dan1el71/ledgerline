package com.ledgerline.shared.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Formats monetary amounts for customer-facing text using an explicit ISO currency code.
 *
 * <p>Ledgerline is COP-only, and the product always writes the code ({@code COP 5,000,000.00})
 * instead of a symbol so a peso amount can never be mistaken for another currency. The format is
 * fixed to English number grouping, matching the language of the UI and API messages.
 */
public final class MoneyFormatter {

    public static final String CURRENCY_CODE = "COP";

    private static final Locale FORMAT_LOCALE = Locale.US;

    private MoneyFormatter() {}

    public static String format(String currency, BigDecimal amount) {
        String code =
                currency == null || currency.isBlank()
                        ? CURRENCY_CODE
                        : currency.trim().toUpperCase(Locale.ROOT);
        BigDecimal value =
                Objects.requireNonNullElse(amount, BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP);

        NumberFormat number = NumberFormat.getNumberInstance(FORMAT_LOCALE);
        number.setGroupingUsed(true);
        number.setMinimumFractionDigits(2);
        number.setMaximumFractionDigits(2);
        return code + " " + number.format(value);
    }

    public static String formatCop(BigDecimal amount) {
        return format(CURRENCY_CODE, amount);
    }
}
