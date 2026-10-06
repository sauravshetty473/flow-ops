package com.flowops.transactionprocessor.consumer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.flowops.transactionprocessor.TransactionProcessorApplication;
import com.flowops.transactionprocessor.processor.TransactionEventProcessor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TransactionProcessorLambdaHandler
        implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private final TransactionEventProcessor transactionProcessor;

    public TransactionProcessorLambdaHandler() {
        this.transactionProcessor = new SpringApplicationBuilder(
                TransactionProcessorApplication.class)
                .profiles("lambda")
                .web(WebApplicationType.NONE)
                .run()
                .getBean(TransactionEventProcessor.class);

        log.info("Transaction Processor Lambda initialized");
    }
    @Override
    public SQSBatchResponse handleRequest(
            SQSEvent event,
            Context context) {

        List<SQSBatchResponse.BatchItemFailure> failures =
                new ArrayList<>();

        log.info(
                "Received SQS batch with {} messages",
                event.getRecords().size()
        );

        for (SQSEvent.SQSMessage message : event.getRecords()) {

            String transactionId = message.getBody();

            try {
                log.info(
                        "Processing transaction {}",
                        transactionId
                );

                transactionProcessor.process(transactionId);

                log.info(
                        "Successfully processed transaction {}",
                        transactionId
                );

            } catch (Exception e) {

                log.error(
                        "Failed to process transaction {}",
                        transactionId,
                        e
                );

                failures.add(
                        new SQSBatchResponse.BatchItemFailure(
                                message.getMessageId()
                        )
                );
            }
        }

        return new SQSBatchResponse(failures);
    }
}