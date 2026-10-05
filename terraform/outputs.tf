output "dynamodb_table_name" {
  description = "DynamoDB transactions table name"
  value       = aws_dynamodb_table.transactions.name
}

output "sqs_queue_url" {
  description = "FlowOps transaction SQS queue URL"
  value       = aws_sqs_queue.transactions.url
}

output "sqs_queue_arn" {
  description = "FlowOps transaction SQS queue ARN"
  value       = aws_sqs_queue.transactions.arn
}

output "sqs_dlq_url" {
  description = "FlowOps transaction DLQ URL"
  value       = aws_sqs_queue.transaction_dlq.url
}