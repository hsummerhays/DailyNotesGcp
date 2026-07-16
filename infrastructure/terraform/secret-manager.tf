resource "google_secret_manager_secret" "db_password" {
  secret_id = "cloudnotes-db-password"
  replication {
    auto {}
  }
  depends_on = [google_project_service.services]
}

resource "google_secret_manager_secret_version" "db_password" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = var.db_password
}

resource "google_secret_manager_secret" "mongodb_uri" {
  secret_id = "cloudnotes-mongodb-uri"
  replication {
    auto {}
  }
  depends_on = [google_project_service.services]
}

resource "google_secret_manager_secret_version" "mongodb_uri" {
  secret      = google_secret_manager_secret.mongodb_uri.id
  secret_data = var.mongodb_uri
}

resource "google_secret_manager_secret" "jwt_secret" {
  secret_id = "cloudnotes-jwt-secret"
  replication {
    auto {}
  }
  depends_on = [google_project_service.services]
}

resource "google_secret_manager_secret_version" "jwt_secret" {
  secret      = google_secret_manager_secret.jwt_secret.id
  secret_data = var.jwt_secret
}
