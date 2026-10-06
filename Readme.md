# FlowOps

There will be changes

## Event-Driven Transaction Processing Platform

**FlowOps** is a cloud-native, event-driven transaction processing platform built with **Java, Spring Boot, AWS Lambda, Amazon SQS, Amazon DynamoDB, API Gateway, React, Terraform and GitHub Actions**.

The platform demonstrates production-oriented backend engineering patterns including:

* Asynchronous event processing
* Idempotent transaction creation
* Queue-based workload decoupling
* Automatic retries
* Dead-letter queue (DLQ) based failure recovery
* Serverless compute
* Infrastructure as Code
* Automated CI/CD
* Cloud observability
* Failure simulation
* Horizontal scalability

The project is intentionally designed around AWS managed/serverless services so the complete system can be deployed with minimal infrastructure overhead.

---

# 1. Architecture

## High-Level Architecture

```text
                              INTERNET
                                  │
                                  ▼
                         ┌─────────────────┐
                         │   CloudFront    │
                         │   CDN / HTTPS   │
                         └────────┬────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │       S3        │
                         │ React Frontend  │
                         └─────────────────┘


                    React Application
                           │
                           │ HTTPS REST
                           ▼
                    ┌───────────────┐
                    │  API Gateway  │
                    └───────┬───────┘
                            │
                            │ Lambda Proxy
                            ▼
                 ┌──────────────────────┐
                 │  Transaction API     │
                 │  Spring Boot Lambda  │
                 └──────────┬───────────┘
                            │
                  ┌─────────┴─────────┐
                  │                   │
                  ▼                   ▼
          ┌───────────────┐     ┌───────────────┐
          │   DynamoDB    │     │      SQS      │
          │ Transactions  │     │  Processing   │
          └───────────────┘     └───────┬───────┘
                                        │
                                        │ Event
                                        ▼
                                ┌───────────────┐
                                │ Worker Lambda │
                                └───────┬───────┘
                                        │
                                        ▼
                                ┌───────────────┐
                                │   DynamoDB    │
                                │ Update Status │
                                └───────────────┘

                                        │
                             processing failures
                                        │
                                        ▼
                                ┌───────────────┐
                                │   SQS DLQ     │
                                └───────────────┘


        ┌──────────────────────────────────────────┐
        │              AWS OBSERVABILITY            │
        │                                           │
        │ CloudWatch Logs + Metrics + Alarms       │
        └──────────────────────────────────────────┘


        ┌──────────────────────────────────────────┐
        │           INFRASTRUCTURE / CI            │
        │                                           │
        │ Terraform → AWS                          │
        │ GitHub Actions → Test → Build → Deploy  │
        └──────────────────────────────────────────┘
```

---

# 2. Core Processing Flow

A transaction follows this lifecycle:

```text
                  POST /transactions
                          │
                          ▼
                    API Gateway
                          │
                          ▼
                  Transaction API
                          │
                ┌─────────┴─────────┐
                │                   │
                ▼                   ▼
          DynamoDB Write           SQS
          status=RECEIVED          message
                │                   │
                │                   ▼
                │             Worker Lambda
                │                   │
                │                   ▼
                │             status=PROCESSING
                │                   │
                │             business validation
                │                   │
                │             ┌─────┴─────┐
                │             │           │
                │           SUCCESS      FAILURE
                │             │           │
                │             ▼           ▼
                │        COMPLETED     SQS RETRY
                │                         │
                │                         ▼
                │                       retry
                │                         │
                │                  max retries reached
                │                         │
                │                         ▼
                │                        DLQ
                │
                └────────────────────────────────────
```

---

# 3. Transaction State Machine

Transactions move through a controlled state lifecycle.

```text
                    ┌───────────┐
                    │ RECEIVED  │
                    └─────┬─────┘
                          │
                          ▼
                    ┌───────────┐
                    │PROCESSING │
                    └─────┬─────┘
                          │
                 ┌────────┴────────┐
                 │                 │
                 ▼                 ▼
           ┌───────────┐     ┌───────────┐
           │ COMPLETED │     │   FAILED  │
           └───────────┘     └─────┬─────┘
                                   │
                                   ▼
                                SQS Retry
                                   │
                                   ▼
                             Worker Lambda
                                   │
                           max retries exceeded
                                   │
                                   ▼
                                  DLQ
```

Supported statuses:

```text
RECEIVED
PROCESSING
COMPLETED
FAILED
```

---

# 4. Why Event-Driven Processing?

The API should not synchronously perform the entire transaction-processing workflow.

Instead:

```text
Client
  │
  ▼
API
  │
  ├── Persist transaction
  │
  └── Queue processing request
           │
           ▼
         SQS
           │
           ▼
        Worker
```

This provides several advantages:

### Loose coupling

The API does not need the processing worker to be immediately available.

### Independent scaling

The API and processing workload can scale independently.

### Failure isolation

A worker failure does not necessarily cause the client request to fail.

### Retry handling

SQS can retry failed processing attempts.

### Backpressure

SQS acts as a buffer when transaction arrival rate temporarily exceeds processing capacity.

---

# 5. Idempotency

Transaction creation is designed to be idempotent.

The client provides a unique:

```text
transactionId
```

Example:

```json
{
  "transactionId": "TXN-10001",
  "customerId": "CUST-100",
  "amount": 25000,
  "type": "PAYMENT"
}
```

The API performs a conditional DynamoDB write.

Conceptually:

```text
POST TXN-10001
       │
       ▼
Does transactionId exist?
       │
    ┌──┴──┐
    │     │
   YES    NO
    │     │
    ▼     ▼
 Return  Create
existing
```

This protects against duplicate client requests and is particularly important because distributed systems may encounter retries.

---

# 6. SQS Message Design

The queue contains a lightweight message rather than the complete transaction.

Example:

```json
{
  "transactionId": "TXN-10001"
}
```

The worker retrieves the current transaction from DynamoDB.

This provides a single source of truth for transaction state.

Instead of:

```text
SQS
 └── entire transaction
```

we use:

```text
SQS
 └── transactionId

DynamoDB
 └── transaction state
```

---

# 7. Failure Handling

FlowOps intentionally supports failure simulation.

For example, transactions above a configurable threshold can be rejected by the worker:

```text
Amount > configured threshold
        │
        ▼
Processing failure
```

The message is not acknowledged successfully.

SQS then makes the message available for another processing attempt.

Example:

```text
Attempt 1
   ↓
FAIL

Attempt 2
   ↓
FAIL

Attempt 3
   ↓
FAIL

Maximum receive count reached
   ↓
DLQ
```

The DLQ allows failed transactions to be investigated without blocking the primary queue.

---

# 8. Dead Letter Queue

Two queues are created:

```text
transaction-processing
transaction-processing-dlq
```

The relationship is:

```text
             transaction-processing
                       │
                       │ failure
                       ▼
                   retry
                       │
                       │ failure
                       ▼
                   retry
                       │
                       │ maxReceiveCount
                       ▼
             transaction-processing-dlq
```

The DLQ provides operational visibility into transactions that could not be processed automatically.

---

# 9. AWS Components

## Amazon API Gateway

Provides the public REST API endpoint.

Example:

```text
https://<api-id>.execute-api.<region>.amazonaws.com/prod
```

Responsibilities:

* HTTP routing
* Lambda integration
* CORS
* Request handling
* Public API entry point

---

## AWS Lambda — Transaction API

Runs the Spring Boot transaction API.

Responsibilities:

* Validate requests
* Create transactions
* Perform idempotency checks
* Persist transactions
* Publish SQS messages
* Retrieve transaction information

---

## Amazon DynamoDB

Stores transaction state.

Table:

```text
transactions
```

Primary key:

```text
transactionId
```

Example item:

```json
{
  "transactionId": "TXN-10001",
  "customerId": "CUST-100",
  "amount": 25000,
  "type": "PAYMENT",
  "status": "COMPLETED",
  "createdAt": "2026-10-03T10:30:00Z",
  "updatedAt": "2026-10-03T10:30:02Z"
}
```

---

## Amazon SQS

Provides asynchronous transaction processing.

Queue:

```text
transaction-processing
```

Responsibilities:

* Decouple API and worker
* Buffer workloads
* Retry failed processing
* Provide at-least-once delivery semantics

---

## AWS Lambda — Processing Worker

Triggered by SQS.

Responsibilities:

1. Read transaction ID
2. Retrieve transaction
3. Update status to `PROCESSING`
4. Execute business validation
5. Update status to `COMPLETED`
6. Throw an error if processing fails

---

## SQS Dead Letter Queue

Stores messages that cannot be processed after the configured number of attempts.

---

## Amazon S3

Hosts the React production build.

```text
React source
    ↓
npm run build
    ↓
build/
    ↓
S3
```

---

## CloudFront

Provides CDN delivery and HTTPS access to the React application.

```text
Browser
   ↓
CloudFront
   ↓
S3
```

---

## CloudWatch

Used for:

* Lambda logs
* Processing errors
* API errors
* Processing latency
* Queue metrics
* Operational troubleshooting

---

# 10. Connectivity

## Frontend → API

The React application communicates with API Gateway over HTTPS.

```text
Browser
   │
   │ HTTPS
   ▼
CloudFront/S3
   │
   │ API HTTPS requests
   ▼
API Gateway
```

Example:

```javascript
fetch(`${API_URL}/api/v1/transactions`, {
  method: "POST",
  headers: {
    "Content-Type": "application/json"
  },
  body: JSON.stringify(transaction)
});
```

---

# 11. API Gateway → Lambda

API Gateway invokes the Spring Boot Lambda.

```text
POST /api/v1/transactions
          │
          ▼
     API Gateway
          │
          ▼
 Transaction Lambda
```

The Lambda converts the API Gateway request into the application's request model.

---

# 12. Lambda → DynamoDB

The API Lambda uses the AWS SDK to communicate with DynamoDB.

```text
Transaction API
      │
      │ AWS SDK
      ▼
 DynamoDB
```

Operations include:

```text
PutItem
GetItem
UpdateItem
Query/Scan
```

Conditional writes are used where required for idempotency.

---

# 13. Lambda → SQS

After successfully creating a transaction:

```text
Transaction Lambda
        │
        │ SendMessage
        ▼
       SQS
```

Message:

```json
{
  "transactionId": "TXN-10001"
}
```

---

# 14. SQS → Worker Lambda

SQS invokes the worker Lambda through an event source mapping.

```text
SQS
 │
 │ event
 ▼
Worker Lambda
```

The worker receives one or more messages depending on the configured batch size.

---

# 15. Worker Lambda → DynamoDB

The worker updates the transaction state:

```text
RECEIVED
   ↓
PROCESSING
   ↓
COMPLETED
```

On failure:

```text
PROCESSING
   ↓
FAILED
```

The message remains eligible for retry according to SQS/Lambda processing behavior.

---

# 16. API Endpoints

## Create Transaction

```http
POST /api/v1/transactions
```

Request:

```json
{
  "transactionId": "TXN-10001",
  "customerId": "CUST-100",
  "amount": 25000,
  "type": "PAYMENT"
}
```

Response:

```json
{
  "transactionId": "TXN-10001",
  "status": "RECEIVED"
}
```

---

## Get Transaction

```http
GET /api/v1/transactions/{transactionId}
```

Example:

```http
GET /api/v1/transactions/TXN-10001
```

Response:

```json
{
  "transactionId": "TXN-10001",
  "customerId": "CUST-100",
  "amount": 25000,
  "type": "PAYMENT",
  "status": "COMPLETED",
  "createdAt": "2026-10-03T10:30:00Z",
  "updatedAt": "2026-10-03T10:30:02Z"
}
```

---

## List Transactions

```http
GET /api/v1/transactions
```

Optional parameters:

```text
status
customerId
limit
```

Example:

```http
GET /api/v1/transactions?status=FAILED
```

---

## Retry Transaction

```http
POST /api/v1/transactions/{transactionId}/retry
```

This endpoint can be used to requeue a transaction after investigation.

---

# 17. Project Structure

```text
flowops/
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com.flowops/
│   │   │   │       ├── controller/
│   │   │   │       ├── service/
│   │   │   │       ├── repository/
│   │   │   │       ├── model/
│   │   │   │       ├── config/
│   │   │   │       └── exception/
│   │   │   └── resources/
│   │   └── test/
│   └── pom.xml
│
├── worker/
│   ├── src/
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   └── types/
│   ├── package.json
│   └── vite.config.js
│
├── infrastructure/
│   └── terraform/
│       ├── main.tf
│       ├── variables.tf
│       ├── outputs.tf
│       ├── iam.tf
│       ├── lambda.tf
│       ├── api_gateway.tf
│       ├── dynamodb.tf
│       ├── sqs.tf
│       ├── s3.tf
│       ├── cloudfront.tf
│       └── cloudwatch.tf
│
├── scripts/
│   └── load-test.py
│
├── docs/
│   └── architecture.md
│
└── .github/
    └── workflows/
        ├── backend.yml
        └── frontend.yml
```

---

# 18. Local Development

## Prerequisites

Install:

```text
Java 21
Maven
Node.js
npm
AWS CLI
Terraform
Git
```

Configure AWS:

```bash
aws configure
```

Verify:

```bash
aws sts get-caller-identity
```

---

# 19. Backend Development

Navigate to:

```bash
cd backend
```

Run tests:

```bash
./mvnw test
```

Run locally:

```bash
./mvnw spring-boot:run
```

The API will be available at:

```text
http://localhost:8080
```

---

# 20. Frontend Development

```bash
cd frontend
npm install
npm run dev
```

The development server will normally be available at:

```text
http://localhost:5173
```

Configure:

```text
VITE_API_URL
```

Example:

```text
VITE_API_URL=http://localhost:8080
```

---

# 21. AWS Deployment

AWS infrastructure is managed using Terraform.

Navigate to:

```bash
cd infrastructure/terraform
```

Initialize Terraform:

```bash
terraform init
```

Validate:

```bash
terraform validate
```

Review:

```bash
terraform plan
```

Deploy:

```bash
terraform apply
```

Terraform creates:

```text
DynamoDB
SQS
DLQ
Lambda functions
IAM roles
API Gateway
S3 bucket
CloudFront distribution
CloudWatch resources
```

---

# 22. Terraform Outputs

After deployment:

```bash
terraform output
```

Expected outputs include values such as:

```text
api_url
frontend_bucket
cloudfront_url
transaction_table
processing_queue
dead_letter_queue
```

The React application's API URL is configured using the API Gateway output.

---

# 23. Frontend Deployment

Build:

```bash
cd frontend
npm install
npm run build
```

Upload the generated build:

```bash
aws s3 sync dist/ s3://<frontend-bucket>
```

Invalidate CloudFront:

```bash
aws cloudfront create-invalidation \
  --distribution-id <distribution-id> \
  --paths "/*"
```

The application is then available through the CloudFront URL.

---

# 24. Backend Deployment

The backend is packaged and deployed as a Lambda-compatible artifact.

Typical flow:

```text
Spring Boot
    ↓
Maven build
    ↓
Lambda artifact
    ↓
AWS Lambda
    ↓
API Gateway
```

Worker deployment:

```text
Worker
   ↓
Build
   ↓
Lambda
   ↓
SQS event source mapping
```

---

# 25. CI/CD

GitHub Actions automates deployment.

## Backend pipeline

```text
Git Push
   │
   ▼
GitHub Actions
   │
   ├── Checkout
   ├── Setup Java
   ├── Run tests
   ├── Build
   ├── Package
   └── Deploy Lambda
```

## Frontend pipeline

```text
Git Push
   │
   ▼
GitHub Actions
   │
   ├── Checkout
   ├── npm install
   ├── Tests
   ├── Build
   ├── Deploy S3
   └── CloudFront invalidation
```

The objective is:

```text
git push
   ↓
automated validation
   ↓
automated deployment
```

---

# 26. Infrastructure as Code

All AWS resources are defined through Terraform.

Example:

```text
terraform plan
```

shows the proposed infrastructure changes.

Deployment:

```bash
terraform apply
```

Cleanup:

```bash
terraform destroy
```

This makes the environment reproducible and prevents dependence on manually configured AWS resources.

---

# 27. IAM

The application follows least-privilege principles where practical.

The API Lambda requires access to:

```text
DynamoDB
SQS
CloudWatch Logs
```

The worker Lambda requires:

```text
DynamoDB
SQS
CloudWatch Logs
```

The deployment identity requires the permissions necessary to provision and deploy the infrastructure.

AWS credentials and secrets should **never** be committed to Git.

---

# 28. Environment Configuration

Configuration should be separated by environment.

Example:

```text
dev
prod
```

Configuration values include:

```text
AWS_REGION
TRANSACTION_TABLE
PROCESSING_QUEUE_URL
DLQ_URL
API_BASE_URL
FAILURE_THRESHOLD
```

For local development, use environment variables or a local configuration file that is excluded from Git.

Never commit:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
private keys
tokens
passwords
```

---

# 29. Observability

CloudWatch is used for application and infrastructure observability.

Important signals:

```text
API invocation count
API errors
Lambda duration
Lambda errors
Lambda throttles
SQS message count
DLQ message count
Worker failures
```

Application logs should contain structured information such as:

```text
transactionId
status
processing duration
failure reason
retry attempt
```

Example:

```text
INFO Transaction processing started transactionId=TXN-10001

INFO Transaction processing completed
     transactionId=TXN-10001
     durationMs=184

ERROR Transaction processing failed
      transactionId=TXN-10002
      reason=RISK_VALIDATION_FAILED
```

---

# 30. Failure Simulation

The platform includes controlled failure simulation.

Example rule:

```text
amount > configured threshold
```

causes the worker to fail.

This allows demonstration of:

```text
Transaction
    ↓
SQS
    ↓
Worker
    ↓
Failure
    ↓
Retry
    ↓
Retry
    ↓
Retry
    ↓
DLQ
```

This should only be used as a demonstration mechanism and not as real transaction validation logic.

---

# 31. Load Testing

The repository includes:

```text
scripts/load-test.py
```

The script generates transaction requests against the deployed API.

Example:

```bash
python scripts/load-test.py \
  --url <API_URL> \
  --count 1000 \
  --concurrency 20
```

Metrics to capture:

```text
Total requests
Successful requests
Failed requests
Average latency
p50 latency
p95 latency
p99 latency
Transactions completed
Messages remaining in SQS
Messages moved to DLQ
```

Performance claims in documentation should only be based on actual measurements.

---

# 32. Cost Optimization

The architecture intentionally avoids infrastructure with persistent compute or high baseline cost.

The project does not require:

```text
EC2
ECS/Fargate
Application Load Balancer
NAT Gateway
RDS
ElastiCache
```

The primary services are managed/serverless services:

```text
Lambda
SQS
DynamoDB
API Gateway
S3
CloudFront
CloudWatch
```

For development:

* Keep Lambda usage low.
* Use small DynamoDB capacity.
* Avoid unnecessary CloudWatch log retention.
* Do not create NAT gateways.
* Destroy unused environments.
* Monitor AWS Billing regularly.
* Set an AWS Budget alert.

The project is intended to remain within available AWS free-tier/credit allowances, but AWS pricing and free-tier eligibility can change. Actual costs depend on account age, region, usage and current AWS pricing.

---

# 33. Security Considerations

Current implementation focuses on demonstrating distributed-system architecture.

Production extensions would include:

```text
Authentication
Authorization
AWS Cognito
API Gateway authorizers
AWS WAF
Secrets Manager
KMS encryption
Private networking
VPC endpoints
CloudTrail
Security Hub
```

Sensitive information should not be stored directly in source code or GitHub Actions configuration.

---

# 34. Scalability

The architecture separates ingestion from processing:

```text
                API
                 │
                 ▼
                SQS
                 │
       ┌─────────┼─────────┐
       ▼         ▼         ▼
    Worker    Worker    Worker
       │         │         │
       └─────────┼─────────┘
                 ▼
              DynamoDB
```

This allows processing capacity to be increased independently from API traffic.

SQS also provides buffering during temporary spikes in transaction volume.

Potential future scaling mechanisms include:

```text
Queue-depth based scaling
Lambda reserved concurrency
DynamoDB capacity scaling
Batch processing
SQS FIFO where ordering is required
```

---

# 35. Reliability

The system is designed around several reliability principles.

### Decoupling

API and processing workers are independent.

### Retry

Transient processing failures can be retried.

### DLQ

Repeated failures are isolated.

### Idempotency

Duplicate requests do not create duplicate transactions.

### Managed services

AWS handles much of the underlying infrastructure availability.

---

# 36. Consistency Considerations

The system intentionally accepts asynchronous processing.

Immediately after transaction creation:

```text
status = RECEIVED
```

The transaction may remain in that state briefly while waiting for processing.

Therefore:

```text
POST transaction
       ↓
RECEIVED
       ↓
eventually
       ↓
PROCESSING
       ↓
COMPLETED
```

The API does not promise that a newly submitted transaction is immediately completed.

This is a deliberate consequence of asynchronous architecture.

---

# 37. Design Trade-offs

## Why SQS instead of synchronous processing?

Synchronous processing would increase API latency and couple the API to the availability of the processing component.

SQS provides buffering and retry behavior.

---

## Why DynamoDB?

The primary access pattern is transaction lookup and state updates by transaction ID.

DynamoDB provides a managed, highly scalable key-value/document data model suitable for this access pattern.

---

## Why Lambda?

The workload is event-driven and potentially variable.

Lambda removes the need to operate persistent application servers for the API and worker.

---

## Why Terraform?

Terraform provides:

* Reproducibility
* Version-controlled infrastructure
* Environment consistency
* Reviewable infrastructure changes
* Automated provisioning

---

## Why not Kubernetes?

Kubernetes would add significant operational complexity without improving the demonstration's core objective.

The project is specifically designed to demonstrate managed/serverless AWS architecture.

---

# 38. Future Improvements

Potential production extensions:

### Authentication

```text
Cognito
    ↓
API Gateway Authorizer
```

### Event-driven notifications

```text
Transaction completed
        ↓
EventBridge
        ↓
Notification service
```

### Audit trail

```text
Transaction
    ↓
DynamoDB Streams
    ↓
Audit processor
```

### Distributed tracing

```text
API Gateway
    ↓
Lambda
    ↓
SQS
    ↓
Worker
```

with AWS X-Ray/OpenTelemetry.

### Advanced monitoring

Create CloudWatch dashboards for:

```text
Transaction throughput
Error rate
Processing latency
Queue depth
DLQ growth
Lambda duration
```

### Multi-region deployment

```text
Region A
    │
    ├── API
    ├── DynamoDB
    └── SQS

Region B
    │
    ├── API
    ├── DynamoDB
    └── SQS
```

---

# 39. Demo Scenario

The recommended demo consists of three transactions.

## Transaction 1 — Successful

```text
Amount: ₹5,000

RECEIVED
   ↓
PROCESSING
   ↓
COMPLETED
```

---

## Transaction 2 — Failed and Retried

```text
Amount: ₹150,000

RECEIVED
   ↓
PROCESSING
   ↓
FAILED
   ↓
RETRY
   ↓
FAILED
   ↓
RETRY
```

---

## Transaction 3 — DLQ

Force repeated processing failure:

```text
RECEIVED
   ↓
PROCESSING
   ↓
FAIL
   ↓
RETRY #1
   ↓
FAIL
   ↓
RETRY #2
   ↓
FAIL
   ↓
RETRY #3
   ↓
DLQ
```

Then demonstrate manual replay:

```text
DLQ
 ↓
Retry API
 ↓
SQS
 ↓
Worker
 ↓
COMPLETED
```

---

# 40. Engineering Principles Demonstrated

This project demonstrates:

```text
✓ REST API design
✓ Spring Boot
✓ Java
✓ Serverless architecture
✓ Event-driven architecture
✓ Asynchronous processing
✓ Queue-based decoupling
✓ Idempotency
✓ Retry strategies
✓ Dead-letter queues
✓ Failure isolation
✓ DynamoDB data modeling
✓ AWS Lambda
✓ Amazon SQS
✓ API Gateway
✓ S3
✓ CloudFront
✓ CloudWatch
✓ Terraform
✓ CI/CD
✓ Docker/build automation
✓ Load testing
✓ Operational observability
```

---

# 41. Resume Description

### FlowOps — Event-Driven Transaction Processing Platform

**Java, Spring Boot, AWS Lambda, SQS, DynamoDB, API Gateway, React, Terraform, GitHub Actions**

* Built a cloud-native event-driven transaction processing platform using Spring Boot, AWS Lambda, SQS and DynamoDB, implementing asynchronous processing and idempotent transaction handling.
* Designed retry and dead-letter queue workflows for failure isolation and recovery, with CloudWatch-based logging and operational monitoring.
* Automated AWS infrastructure provisioning with Terraform and application deployments using GitHub Actions, with a React dashboard deployed through S3 and CloudFront.

Add quantified performance metrics only after running the load test.

---

# 42. Repository

```text
https://github.com/<username>/flowops
```

Recommended repository sections:

```text
README.md
architecture/
backend/
worker/
frontend/
infrastructure/
scripts/
.github/
```

The README should include the architecture diagram near the top so an engineer can understand the system before reading the implementation.

---

# 43. One-Line Summary

> **FlowOps is an event-driven, serverless transaction processing platform demonstrating asynchronous workflows, idempotency, failure recovery, infrastructure as code, CI/CD and cloud observability using AWS and Java/Spring Boot.**
