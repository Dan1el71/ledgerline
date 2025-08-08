package com.ledgerline.shared.money;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyFormatterTest {

    @Test
    void formatsCopWithAnExplicitCurrencyCodeAndNoSymbol() {
        String formatted = MoneyFormatter.formatCop(new BigDecimal("5000000"));

        assertThat(formatted).isEqualTo("COP 5,000,000.00");
        assertThat(formatted).contains("COP").doesNotContain("R");
    }

    @Test
    void formatsOtherCurrenciesWithTheirCode() {
        assertThat(MoneyFormatter.format("usd", new BigDecimal("1234.5")))
                .isEqualTo("USD 1,234.50");
    }

    @Test
    void defaultsMissingCurrencyAndAmountToCopZero() {
        assertThat(MoneyFormatter.format(null, null)).isEqualTo("COP 0.00");
    }
}
