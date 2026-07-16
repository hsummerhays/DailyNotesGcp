resource "google_sql_database_instance" "postgres" {
  name             = "cloudnotes-db-${var.environment}"
  database_version = "POSTGRES_16"
  region           = var.region

  settings {
    tier = "db-f1-micro" # Smallest tier for dev development to keep costs minimal
    
    ip_configuration {
      ipv4_enabled = true # Enabled public IP to allow connection from GKE via Cloud SQL Auth Proxy
    }
  }

  deletion_protection = false # Disable deletion protection for development environments

  depends_on = [google_project_service.services]
}

resource "google_sql_database" "database" {
  name     = "cloudnotes"
  instance = google_sql_database_instance.postgres.name
}

resource "google_sql_user" "db_user" {
  name     = "cloudnotes_user"
  instance = google_sql_database_instance.postgres.name
  password = var.db_password
}
