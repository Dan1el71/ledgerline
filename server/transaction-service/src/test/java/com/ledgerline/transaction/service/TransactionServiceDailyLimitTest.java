package com.ledgerline.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ledgerline.shared.exception.BusinessException;
import com.ledgerline.transaction.client.AccountClient;
import com.ledgerline.transaction.client.AccountClient.JournalResponse;
import com.ledgerline.transaction.dto.TransactionResponse;
import com.ledgerline.transaction.entity.Transaction;
import com.ledgerline.transaction.entity.TransactionStatus;
import com.ledgerline.transaction.entity.TransactionType;
import com.ledgerline.transaction.mapper.TransactionMapper;
import com.ledgerline.transaction.repository.LedgerEntryRepository;
import com.ledgerline.transaction.repository.OutboxEventRepository;
import com.ledgerline.transaction.repository.TransactionRepository;
import com.ledgerline.transaction.service.TransactionService.Instruction;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Confirms the reservation is booked with the instruction and released on a definitive failure. */
class TransactionServiceDailyLimitTest {

    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final LedgerEntryRepository ledgerEntryRepository = mock(LedgerEntryRepository.class);
    private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
    private final AccountClient accountClient = mock(AccountClient.class);
    private final TransactionMapper mapper = mock(TransactionMapper.class);
    private final DailyLimitService dailyLimitService = mock(DailyLimitService.class);

    private final TransactionService service =
            new TransactionService(
                    transactionRepository,
                    ledgerEntryRepository,
                    outboxEventRepository,
                    accountClient,
                    mapper,
                    new ObjectMapper().registerModule(new JavaTimeModule()),
                    dailyLimitService,
                    new NoopTransactionManager());

    @Test
    void executeReservesTheDailyLimitInsideTheSameTransactionAsTheInstruction() {
        BigDecimal amount = new BigDecimal("2000000.00");
        LocalDate usageDate = LocalDate.now(java.time.ZoneId.of("America/Bogota"));
        AtomicReference<Transaction> saved = new AtomicReference<>();

        when(transactionRepository.findByIdempotencyKey("daily-key")).thenReturn(Optional.empty());
        when(dailyLimitService.reserve(7L, amount)).thenReturn(usageDate);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(
                        invocation -> {
                            Transaction transaction = invocation.getArgument(0);
                            saved.set(transaction);
                            return transaction;
                        });
        when(transactionRepository.findById(anyString()))
                .thenAnswer(
                        invocation ->
                                Optional.ofNullable(saved.get())
                                        .filter(t -> t.getId().equals(invocation.getArgument(0))));
        when(transactionRepository.findByIdForUpdate(anyString()))
                .thenAnswer(
                        invocation ->
                                Optional.ofNullable(saved.get())
                                        .filter(t -> t.getId().equals(invocation.getArgument(0))));
        when(accountClient.transfer(
                        anyString(), anyLong(), anyLong(), any(), anyString(), any(), any()))
                .thenReturn(journal());
        when(mapper.toTransactionResponse(any(), any())).thenReturn(new TransactionResponse());

        service.execute(instruction("daily-key", amount, true));

        assertThat(saved.get().getLimitUsageDate()).isEqualTo(usageDate);
        assertThat(saved.get().getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(dailyLimitService).reserve(7L, amount);
    }

    @Test
    void aRejectedReservationDoesNotPersistAnInstruction() {
        BigDecimal amount = new BigDecimal("2000000.00");
        when(transactionRepository.findByIdempotencyKey("over-limit")).thenReturn(Optional.empty());
        when(dailyLimitService.reserve(7L, amount)).thenThrow(new BusinessException("daily limit"));

        assertThatThrownBy(() -> service.execute(instruction("over-limit", amount, true)))
                .isInstanceOf(BusinessException.class);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void aFailedLedgerCallReleasesTheReservation() {
        Transaction pending =
                Transaction.builder()
                        .id("txn-failed")
                        .type(TransactionType.TRANSFER)
                        .status(TransactionStatus.PENDING)
                        .sourceAccountId(10L)
                        .targetAccountId(20L)
                        .sourceCustomerId(7L)
                        .amount(new BigDecimal("2000000.00"))
                        .currency("COP")
                        .idempotencyKey("failed-key")
                        .limitUsageDate(LocalDate.now(java.time.ZoneId.of("America/Bogota")))
                        .build();

        when(transactionRepository.findById("txn-failed")).thenReturn(Optional.of(pending));
        when(transactionRepository.findByIdForUpdate("txn-failed"))
                .thenReturn(Optional.of(pending));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountClient.transfer(
                        anyString(), anyLong(), anyLong(), any(), anyString(), any(), any()))
                .thenThrow(new BusinessException("insufficient funds"));

        service.advance("txn-failed", false);

        assertThat(pending.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(pending.isLimitReleased()).isTrue();
        verify(dailyLimitService)
                .release(7L, pending.getLimitUsageDate(), new BigDecimal("2000000.00"));
    }

    private static Instruction instruction(String key, BigDecimal amount, boolean enforce) {
        return new Instruction(
                TransactionType.TRANSFER,
                key,
                10L,
                20L,
                7L,
                8L,
                amount,
                "COP",
                "test",
                "42",
                enforce);
    }

    private static JournalResponse journal() {
        return new JournalResponse(
                UUID.randomUUID(), "TRANSFER", "TRF-1", "COP", Instant.now(), false, List.of());
    }

    /** TransactionTemplate only needs begin/commit/rollback; the repositories are mocked. */
    private static final class NoopTransactionManager implements PlatformTransactionManager {
        @Override
        public org.springframework.transaction.TransactionStatus getTransaction(
                TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(org.springframework.transaction.TransactionStatus status) {
            // no-op
        }

        @Override
        public void rollback(org.springframework.transaction.TransactionStatus status) {
            // no-op
        }
    }
}
