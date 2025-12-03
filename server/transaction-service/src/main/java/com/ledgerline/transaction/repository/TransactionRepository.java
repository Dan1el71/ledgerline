package com.ledgerline.transaction.repository;

import com.ledgerline.transaction.entity.Transaction;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository
        extends JpaRepository<Transaction, String>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * Locks the instruction while its final state and daily-limit compensation are written, so two
     * recovery workers cannot release the same reservation twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdForUpdate(@Param("id") String id);

    @Query(
            value =
                    "SELECT id FROM transactions WHERE status = 'PENDING'"
                            + " AND updated_at < NOW() - INTERVAL '10 seconds'"
                            + " ORDER BY updated_at LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<String> claimStalePending(@Param("limit") int limit);
}
