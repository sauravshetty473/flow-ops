package com.flowops.backend.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
@AllArgsConstructor
@Slf4j
public class TransactionEventPublisher {

    private final SqsClient sqsClient;
    private final String queueUrl;

    public void publish(String transactionId) {
        log.info("Publishing transaction event: {}", transactionId);

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(transactionId)
                .build();

        sqsClient.sendMessage(request);
        log.info("Transaction event published successfully: {}", transactionId);
    }
}
