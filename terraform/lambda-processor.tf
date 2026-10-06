resource "aws_iam_role" "lambda_transaction_processor" {
  name = "${var.project_name}-transaction-processor-lambda"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"

    Statement = [
      {
        Effect = "Allow"

        Principal = {
          Service = "lambda.amazonaws.com"
        }

        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
  }
}

resource "aws_cloudwatch_log_group" "lambda_transaction_processor" {
  name              = "/aws/lambda/${var.project_name}-transaction-processor"
  retention_in_days = 7

  tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
  }
}

resource "aws_iam_role_policy" "lambda_transaction_processor" {
  name = "${var.project_name}-transaction-processor-lambda"
  role = aws_iam_role.lambda_transaction_processor.id

  policy = jsonencode({
    Version = "2012-10-17"

    Statement = [
      {
        Effect = "Allow"

        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]

        Resource = "${aws_cloudwatch_log_group.lambda_transaction_processor.arn}:*"
      },

      {
        Effect = "Allow"

        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ]

        Resource = aws_sqs_queue.transactions.arn
      },

      {
        Effect = "Allow"

        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:DescribeTable"
        ]

        Resource = aws_dynamodb_table.transactions.arn
      }
    ]
  })
}

resource "aws_lambda_function" "transaction_processor" {
  function_name = "${var.project_name}-transaction-processor"

  role = aws_iam_role.lambda_transaction_processor.arn

  package_type = "Image"

  image_uri = "${aws_ecr_repository.transaction_processor.repository_url}:latest"

  architectures = ["x86_64"]

  memory_size = 1024
  timeout     = 60

  lifecycle {
    ignore_changes = [
      image_uri
    ]
  }

  tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
  }

  depends_on = [
    aws_cloudwatch_log_group.lambda_transaction_processor,
    aws_iam_role_policy.lambda_transaction_processor
  ]
}

resource "aws_lambda_event_source_mapping" "transaction_processor_sqs" {
  event_source_arn = aws_sqs_queue.transactions.arn
  function_name    = aws_lambda_function.transaction_processor.arn

  batch_size                         = 10
  function_response_types            = ["ReportBatchItemFailures"]
  enabled                            = true
  maximum_batching_window_in_seconds = 5

  depends_on = [
    aws_iam_role_policy.lambda_transaction_processor
  ]
}