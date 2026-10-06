package com.flowops.common.repository;

import com.flowops.common.model.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository{

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(String transactionId);

    boolean existsById(String transactionId);

    List<Transaction> findAll();
}
