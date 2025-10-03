package com.ledgerline.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledgerline.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = "eureka.client.enabled=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DailyLimitService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DailyLimitServiceConcurrencyTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("ledgerline_transaction_test")
                    .withUsername("ledgerline")
                    .withPassword("ledgerline_secret");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired private DailyLimitService dailyLimitService;

    @Autowired private PlatformTransactionManager transactionManager;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUsage() {
        jdbcTemplate.update("DELETE FROM daily_limit_usage WHERE customer_id IN (99, 100)");
    }

    @Test
    void twoConcurrentPaymentsCannotBothSpendTheSameRemainingLimit() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> pay =
                    () -> {
                        start.await();
                        try {
                            tx.executeWithoutResult(
                                    status ->
                                            dailyLimitService.reserve(
                                                    99L, new BigDecimal("6000000.00")));
                            return true;
                        } catch (BusinessException e) {
                            return false;
                        }
                    };

            Future<Boolean> first = pool.submit(pay);
            Future<Boolean> second = pool.submit(pay);
            start.countDown();

            List<Boolean> results =
                    List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));

            assertThat(results).containsExactlyInAnyOrder(true, false);
            assertThat(usedAmount(99L)).isEqualByComparingTo("6000000.0000");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void aFailedPaymentReleasesItsReservation() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(
                status -> dailyLimitService.reserve(100L, new BigDecimal("9000000.00")));
        tx.executeWithoutResult(
                status ->
                        dailyLimitService.release(
                                100L,
                                java.time.LocalDate.now(java.time.ZoneId.of("America/Bogota")),
                                new BigDecimal("3000000.00")));

        assertThat(usedAmount(100L)).isEqualByComparingTo("6000000.0000");
        tx.executeWithoutResult(
                status -> dailyLimitService.reserve(100L, new BigDecimal("4000000.00")));
        assertThat(usedAmount(100L)).isEqualByComparingTo("10000000.0000");
    }

    private BigDecimal usedAmount(Long customerId) {
        return jdbcTemplate.queryForObject(
                "SELECT used_amount FROM daily_limit_usage WHERE customer_id = ?",
                BigDecimal.class,
                customerId);
    }
}
