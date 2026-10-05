package com.flowops.backend.repository;

import com.flowops.backend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTransactionRepository
        extends JpaRepository<Transaction, String> {
}