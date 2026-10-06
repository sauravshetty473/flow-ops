package com.flowops.common.repository;

import com.flowops.common.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTransactionRepository
        extends JpaRepository<Transaction, String> {
}