package com.flowops.backend.controller;

import com.flowops.backend.dto.CreateTransactionRequest;
import com.flowops.backend.dto.TransactionResponse;
import com.flowops.backend.exception.TransactionNotFoundException;
import com.flowops.backend.service.TransactionService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/createTransaction")
    public TransactionResponse createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        System.out.println(request);
        return transactionService.createTransaction(request);
    }

    @GetMapping("/listTransactions")
    public List<TransactionResponse> listTransactions() {
        return transactionService.getAllTransactions();
    }

    @GetMapping("/{id}/getDetails")
    public TransactionResponse getTransaction(@PathVariable String id) throws TransactionNotFoundException {
        return transactionService.getTransaction(id);
    }

    @GetMapping("/{id}/retry")
    public String retryTransaction(@PathVariable String id) {
        return String.format("retrying for transaction id %s", id);
    }
}
