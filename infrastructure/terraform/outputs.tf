output "gke_cluster_name" {
  value       = google_container_cluster.primary.name
  description = "The name of the GKE cluster"
}

output "gke_cluster_location" {
  value       = google_container_cluster.primary.location
  description = "The location of the GKE cluster"
}

output "db_instance_connection_name" {
  value       = google_sql_database_instance.postgres.connection_name
  description = "The connection name of the Cloud SQL instance (used in SQL Auth Proxy)"
}

output "artifact_registry_repo" {
  value       = "${google_artifact_registry_repository.cloudnotes.location}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.cloudnotes.repository_id}"
  description = "The Docker repository base URL for Artifact Registry"
}

output "backend_gsa_email" {
  value       = google_service_account.backend.email
  description = "The Google Service Account email used by the GKE backend pod"
}
