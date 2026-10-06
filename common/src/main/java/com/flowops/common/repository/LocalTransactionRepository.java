package com.flowops.common.repository;

import com.flowops.common.model.Transaction;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("local")
@AllArgsConstructor
public class LocalTransactionRepository implements TransactionRepository{
    private final SpringDataTransactionRepository repository;


    @Override
    public Transaction save(Transaction transaction) {
        return repository.save(transaction);
    }

    @Override
    public Optional<Transaction> findById(String transactionId) {
        return repository.findById(transactionId);
    }

    @Override
    public boolean existsById(String transactionId) {
        return repository.existsById(transactionId);
    }

    @Override
    public List<Transaction> findAll() {
        return repository.findAll();
    }
}
