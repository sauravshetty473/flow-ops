package com.flowops.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class SqsConfig {

    @Bean
    public String transactionQueueUrl(
            @Value("${flowops.sqs.transaction-queue-url}") String queueUrl) {
        return queueUrl;
    }

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.AP_SOUTH_1)
                .build();
    }
}
