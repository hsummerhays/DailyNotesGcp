# Google Service Account for GKE backend workload identity
resource "google_service_account" "backend" {
  account_id   = "cloudnotes-backend-${var.environment}"
  display_name = "GKE CloudNotes Backend Service Account"

  depends_on = [google_project_service.services]
}

# Grant Cloud SQL Client access to the backend GSA
resource "google_project_iam_member" "sql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.backend.email}"
}

# Grant Secret Manager Secret Accessor role on all three secrets to the backend GSA
resource "google_secret_manager_secret_iam_member" "db_password_accessor" {
  secret_id = google_secret_manager_secret.db_password.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.backend.email}"
}

resource "google_secret_manager_secret_iam_member" "mongodb_uri_accessor" {
  secret_id = google_secret_manager_secret.mongodb_uri.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.backend.email}"
}

resource "google_secret_manager_secret_iam_member" "jwt_secret_accessor" {
  secret_id = google_secret_manager_secret.jwt_secret.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.backend.email}"
}

# Bind GSA to Kubernetes Service Account (KSA) using Workload Identity
resource "google_service_account_iam_member" "workload_identity" {
  service_account_id = google_service_account.backend.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.project_id}.svc.id.goog[default/cloudnotes-backend]"
}
