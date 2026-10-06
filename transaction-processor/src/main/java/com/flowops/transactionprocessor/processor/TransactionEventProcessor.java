package com.flowops.transactionprocessor.processor;

import com.flowops.common.exception.TransactionNotFoundException;
import com.flowops.common.model.Transaction;
import com.flowops.common.model.TransactionStatus;
import com.flowops.common.repository.TransactionRepository;
import com.flowops.transactionprocessor.checks.SanityChecks;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class TransactionEventProcessor {

    private final TransactionRepository transactionRepository;
    private final SanityChecks sanityChecks;

    public void process(String transactionId) throws Exception {
        log.info("Processing transaction: {}", transactionId);

        Transaction transaction = sanityChecks.checkIfExists(transactionId);
        if (transaction.getStatus().equals(TransactionStatus.COMPLETED)) {
            log.info("Transaction {} is already processed. Skipping.", transactionId);
            return;
        }
        sanityChecks.checkSanctions(transaction);

        // Mark transaction as processing
        transaction.setStatus(TransactionStatus.PROCESSING);
        transactionRepository.save(transaction);

        log.info("Transaction {} marked as PROCESSING", transactionId);

        // Simulate transaction processing
        simulateProcessing();

        // Mark transaction as completed
        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        log.info("Transaction {} marked as COMPLETED", transactionId);
    }

    private void simulateProcessing() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Transaction processing interrupted", e);
        }
    }
}
