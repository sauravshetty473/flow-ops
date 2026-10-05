package com.flowops.backend.repository;

import com.flowops.backend.model.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository{

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(String transactionId);

    boolean existsById(String transactionId);

    List<Transaction> findAll();
}
