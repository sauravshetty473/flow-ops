package com.flowops.backend.exception;

public class TransactionNotFoundException extends Exception{

    public TransactionNotFoundException(String transactionId) {
        super("Transaction not found: " + transactionId);
    }
}
