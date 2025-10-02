package com.ledgerline.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ledgerline.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class DailyLimitServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    private final DailyLimitService service =
            new DailyLimitService(
                    new BigDecimal(DailyLimitService.DEFAULT_DAILY_LIMIT), jdbcTemplate);

    @Test
    void reserveReturnsTheBankDateWhenTheTotalFits() {
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), any(), any(), any()))
                .thenReturn(new BigDecimal("6000000.0000"));

        LocalDate date = service.reserve(1L, new BigDecimal("2000000.00"));

        assertThat(date).isEqualTo(LocalDate.now(java.time.ZoneId.of("America/Bogota")));
    }

    @Test
    void reserveRejectsThePaymentWhenTheAtomicTotalExceedsTheLimit() {
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), any(), any(), any()))
                .thenReturn(new BigDecimal("10000001.0000"));

        assertThatThrownBy(() -> service.reserve(1L, new BigDecimal("2000001.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("COP 2,000,000.00");
    }

    @Test
    void reserveRejectsAMissingCustomer() {
        assertThatThrownBy(() -> service.reserve(null, new BigDecimal("1000")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void releaseCompensatesTheReservation() {
        LocalDate date = LocalDate.now(java.time.ZoneId.of("America/Bogota"));
        BigDecimal amount = new BigDecimal("2000000.00");

        service.release(1L, date, amount);

        verify(jdbcTemplate).update(anyString(), eq(amount), eq(1L), eq(date));
    }

    @Test
    void releaseIgnoresAnIncompleteReservation() {
        service.release(null, null, null);

        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any());
    }
}
