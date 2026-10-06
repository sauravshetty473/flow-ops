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

output "backend_ecr_repository_url" {
  value = aws_ecr_repository.backend.repository_url
}

output "transaction_processor_ecr_repository_url" {
  value = aws_ecr_repository.transaction_processor.repository_url
}

output "github_actions_role_arn" {
  value = aws_iam_role.github_actions.arn
}

output "backend_api_url" {
  value = aws_apigatewayv2_api.backend.api_endpoint
}

output "transaction_processor_lambda_name" {
  value = aws_lambda_function.transaction_processor.function_name
}