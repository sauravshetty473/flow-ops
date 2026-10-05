package com.flowops.backend.repository;

import com.flowops.backend.model.Transaction;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("aws")
public class DynamoDbTransactionRepository implements TransactionRepository{

    private final DynamoDbTable<Transaction> table;

    public DynamoDbTransactionRepository(
            DynamoDbEnhancedClient enhancedClient) {

        this.table = enhancedClient.table(
                "flowops-transactions",
                TableSchema.fromBean(Transaction.class)
        );
    }

    @Override
    public Transaction save(Transaction transaction) {
        table.putItem(transaction);
        return transaction;
    }

    @Override
    public Optional<Transaction> findById(String transactionId) {
        Transaction transaction = table.getItem(r ->
                r.key(k -> k.partitionValue(transactionId))
        );

        return Optional.ofNullable(transaction);
    }

    @Override
    public boolean existsById(String transactionId) {
        return false;
    }

    @Override
    public List<Transaction> findAll() {
        return table
                .scan()
                .items()
                .stream()
                .toList();
    }
}
