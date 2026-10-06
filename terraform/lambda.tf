data "aws_ecr_image" "backend" {
  repository_name = aws_ecr_repository.backend.name
  image_tag       = var.backend_image_tag
}

resource "aws_iam_role" "lambda_backend" {
  name = "${var.project_name}-backend-lambda"

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

resource "aws_cloudwatch_log_group" "backend_lambda" {
  name              = "/aws/lambda/${var.project_name}-backend"
  retention_in_days = 7

  tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
  }
}

resource "aws_iam_role_policy" "lambda_backend" {
  name = "${var.project_name}-backend-lambda-policy"
  role = aws_iam_role.lambda_backend.id

  policy = jsonencode({
    Version = "2012-10-17"

    Statement = [

      # CloudWatch Logs
      {
        Effect = "Allow"

        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]

        Resource = "${aws_cloudwatch_log_group.backend_lambda.arn}:*"
      },

      # DynamoDB
      {
        Effect = "Allow"

        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:Scan",
          "dynamodb:DescribeTable"
        ]

        Resource = aws_dynamodb_table.transactions.arn
      },

      # SQS
      {
        Effect = "Allow"

        Action = [
          "sqs:SendMessage"
        ]

        Resource = aws_sqs_queue.transactions.arn
      }
    ]
  })
}

resource "aws_lambda_function" "backend" {
  function_name = "${var.project_name}-backend"

  role = aws_iam_role.lambda_backend.arn

  package_type = "Image"

  image_uri = "${aws_ecr_repository.backend.repository_url}@${data.aws_ecr_image.backend.image_digest}"

  architectures = ["x86_64"]

  memory_size = 1024
  timeout     = 30

  environment {
    variables = {
      SPRING_PROFILES_ACTIVE = "aws"

      FLOWOPS_SQS_TRANSACTION_QUEUE_URL = aws_sqs_queue.transactions.url

      AWS_LWA_PORT                 = "8080"
      AWS_LWA_READINESS_CHECK_PATH = "/"
    }
  }

  tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
  }

  depends_on = [
    aws_cloudwatch_log_group.backend_lambda
  ]
}

resource "aws_apigatewayv2_api" "backend" {
  name          = "${var.project_name}-backend-api"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = ["*"]

    allow_methods = [
      "GET",
      "POST",
      "PUT",
      "PATCH",
      "DELETE",
      "OPTIONS"
    ]

    allow_headers = [
      "content-type"
    ]
  }

  tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
  }
}

resource "aws_apigatewayv2_integration" "backend" {
  api_id = aws_apigatewayv2_api.backend.id

  integration_type   = "AWS_PROXY"
  integration_uri    = aws_lambda_function.backend.invoke_arn
  integration_method = "POST"

  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "backend" {
  api_id = aws_apigatewayv2_api.backend.id

  route_key = "$default"

  target = "integrations/${aws_apigatewayv2_integration.backend.id}"
}

resource "aws_apigatewayv2_stage" "backend" {
  api_id = aws_apigatewayv2_api.backend.id

  name        = "$default"
  auto_deploy = true
}

resource "aws_lambda_permission" "api_gateway" {
  statement_id  = "AllowApiGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.backend.function_name
  principal     = "apigateway.amazonaws.com"

  source_arn = "${aws_apigatewayv2_api.backend.execution_arn}/*/*"
}