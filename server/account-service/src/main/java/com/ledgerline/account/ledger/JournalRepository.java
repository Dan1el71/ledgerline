package com.ledgerline.account.ledger;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalRepository extends JpaRepository<JournalEntity, UUID> {

    Optional<JournalEntity> findByIdempotencyKey(String idempotencyKey);
}
