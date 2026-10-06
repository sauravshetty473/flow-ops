package com.flowops.transactionprocessor.checks;

import com.flowops.common.exception.TransactionNotFoundException;
import com.flowops.common.model.Transaction;
import com.flowops.common.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
public class SanityChecks {

    private final TransactionRepository transactionRepository;


    public Transaction checkIfExists(String transactionId) throws TransactionNotFoundException {
        return transactionRepository.findById(transactionId).orElseThrow(
                () -> new TransactionNotFoundException(transactionId)
        );
    }

    public void checkSanctions(Transaction transaction){
        // Temporary failure simulation
        if ("FAIL-TEST".equals(transaction.getCustomerId())) {
            throw new IllegalStateException("Simulated transaction failure");
        }
    }
}
