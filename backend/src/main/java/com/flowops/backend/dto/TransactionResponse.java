package com.flowops.backend.dto;

import com.flowops.backend.model.TransactionStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record TransactionResponse(

        String id,
        String customerId,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        Instant createdAt
) { }