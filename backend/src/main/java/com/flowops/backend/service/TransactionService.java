package com.flowops.backend.service;

import com.flowops.backend.dto.CreateTransactionRequest;
import com.flowops.backend.dto.TransactionResponse;
import com.flowops.common.exception.TransactionNotFoundException;
import com.flowops.common.model.Transaction;
import com.flowops.common.model.TransactionStatus;
import com.flowops.common.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventPublisher transactionEventPublisher;

    public TransactionResponse createTransaction(
            CreateTransactionRequest request){

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .customerId(request.customerId())
                .amount(request.amount())
                .currency(request.currency())
                .status(TransactionStatus.OPEN)
                .createdAt(Instant.now())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        transactionEventPublisher.publish(saved.getId());

        return toResponse(saved);
    }

    public TransactionResponse getTransaction(String id) throws TransactionNotFoundException {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() ->
                        new TransactionNotFoundException(id));

        return toResponse(transaction);
    }

    public List<TransactionResponse> getAllTransactions() {

        return transactionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TransactionResponse toResponse(Transaction transaction) {

        return TransactionResponse.builder()
                .id(transaction.getId())
                .customerId(transaction.getCustomerId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
