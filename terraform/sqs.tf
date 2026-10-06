resource "aws_sqs_queue" "transaction_dlq" {
  name = "${var.project_name}-transaction-dlq"

  message_retention_seconds = 1209600 # 14 days

  tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
  }
}

resource "aws_sqs_queue" "transactions" {
  name = "${var.project_name}-transactions"

  visibility_timeout_seconds = 360

  message_retention_seconds = 345600 # 4 days

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.transaction_dlq.arn
    maxReceiveCount     = 3
  })

  tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
  }
}