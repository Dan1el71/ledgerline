package com.ledgerline.reporting.repository;

import com.ledgerline.reporting.entity.StatementEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatementRepository extends JpaRepository<StatementEntity, Long> {

    List<StatementEntity> findByAccountIdOrderByGeneratedAtDesc(Long accountId);

    List<StatementEntity> findByCustomerIdOrderByGeneratedAtDesc(Long customerId);
}
