variable "aws_region" {
  description = "AWS region where FlowOps infrastructure will be deployed"
  type        = string
  default     = "ap-south-1"
}

variable "project_name" {
  description = "Project name used for AWS resource naming"
  type        = string
  default     = "flowops"
}