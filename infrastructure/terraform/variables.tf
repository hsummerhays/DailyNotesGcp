variable "project_id" {
  type        = string
  description = "The GCP project ID to deploy resources into"
}

variable "region" {
  type        = string
  default     = "us-central1"
  description = "The primary region for resources"
}

variable "environment" {
  type        = string
  default     = "dev"
  description = "Deployment environment name (e.g. dev, prod)"
}

variable "db_password" {
  type        = string
  sensitive   = true
  description = "The password for the Cloud SQL PostgreSQL database user"
}

variable "mongodb_uri" {
  type        = string
  sensitive   = true
  description = "The connection string for the MongoDB Atlas cluster"
}

variable "jwt_secret" {
  type        = string
  sensitive   = true
  description = "The secret key used for signing JWTs in Spring Boot"
}
