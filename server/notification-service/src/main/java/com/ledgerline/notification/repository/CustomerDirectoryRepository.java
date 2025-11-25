package com.ledgerline.notification.repository;

import com.ledgerline.notification.entity.CustomerDirectoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerDirectoryRepository extends JpaRepository<CustomerDirectoryEntry, Long> {}
