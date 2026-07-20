# Terraform variable values for local GKE dev environment.
# Note: Always keep sensitive secrets out of source control. Use env vars (TF_VAR_db_password, etc.) in production/CI.
project_id  = "daily-notes-gcp"
region      = "us-central1"
environment = "dev"
db_password = "placeholder-db-password-change-me"
mongodb_uri = "mongodb+srv://placeholder-atlas-connection-uri-change-me"
jwt_secret  = "placeholder-jwt-signing-secret-change-me-32bytes-long"
