package com.flowops.transactionprocessor.consumer;

import com.flowops.transactionprocessor.processor.TransactionEventProcessor;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
@AllArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final SqsClient sqsClient;
    private final String transactionQueueUrl;
    private final TransactionEventProcessor transactionEventProcessor;

    @PostConstruct
    public void start() {
        Thread.ofVirtual().start(this::consume);
    }

    private void consume() {
        log.info("Transaction event consumer started");

        while (true) {

            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(transactionQueueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(20)
                    .build();

            var response = sqsClient.receiveMessage(request);

            for (Message message : response.messages()) {

                String transactionId = message.body();

                log.info(
                        "Received transaction event: {}",
                        transactionId
                );

                try{
                    transactionEventProcessor.process(transactionId);
                    deleteMessage(message);
                } catch (Exception e) {
                    log.error("Failed to process transaction: {}", transactionId);
                }
            }
        }
    }

    private void deleteMessage(Message message) {

        DeleteMessageRequest request = DeleteMessageRequest.builder()
                .queueUrl(transactionQueueUrl)
                .receiptHandle(message.receiptHandle())
                .build();

        sqsClient.deleteMessage(request);
    }
}
