# Terraform variable values for local GKE dev environment.
# Note: Always keep sensitive secrets out of source control. Use env vars (TF_VAR_db_password, etc.) in production/CI.
project_id  = "your-gcp-project-id"
region      = "us-central1"
environment = "dev"
db_password = "replace-with-secure-db-password"
mongodb_uri = "mongodb+srv://replace-with-your-atlas-connection-uri"
jwt_secret  = "replace-with-secure-jwt-signing-secret"
