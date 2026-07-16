resource "google_artifact_registry_repository" "cloudnotes" {
  location      = var.region
  repository_id = "cloudnotes"
  description   = "Docker repository for CloudNotes images"
  format        = "DOCKER"

  depends_on = [google_project_service.services]
}
