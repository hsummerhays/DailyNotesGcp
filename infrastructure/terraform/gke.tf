resource "google_container_cluster" "primary" {
  name     = "cloudnotes-cluster-${var.environment}"
  location = var.region

  # Enable Autopilot mode
  enable_autopilot = true

  # Release channel recommendation
  release_channel {
    channel = "REGULAR"
  }

  # Enable Workload Identity (GKE Autopilot has it enabled by default, but keeping it explicit is best practice)
  workload_identity_config {
    workload_pool = "${var.project_id}.svc.id.goog"
  }

  depends_on = [google_project_service.services]
}
