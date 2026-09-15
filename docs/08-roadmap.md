# Document 08: Phased Project Roadmap

This document outlines the execution plan for CloudNotes, divided into progressive development milestones.

---

## Milestone 1: Core GCP Infrastructure & Deployment (Complete)
* **Goal**: Establish the base cloud deployment with a fully containerized stack managed via IaC.
* **Prerequisites**: Containerized local services (Phase 1) complete.
* **Scope**:
  - [x] Terraform scripts for VPC, GKE Autopilot, Cloud SQL PostgreSQL, Secret Manager, and Artifact Registry.
  - [x] Helm Charts packaging `notes-api`, `import-worker`, Nginx frontend, Gateway load-balancer routing, and Secret Store CSI class.
  - [x] Deploy stack to live GCP project via `terraform apply` -> GitHub Actions CI/CD Helm deployment.
  - [x] Verified full build, test, container push, GKE credential retrieval, and Helm release lifecycle.

---

## Milestone 2: Operational Maturity & Observability
* **Goal**: Harden the deployment for scale, durability, and operational compliance.
* **Scope**:
  - [ ] **Workload Scaling**: Configure Horizontal Pod Autoscaler (HPA) and Pod Disruption Budgets (PDB) to manage replicas dynamically.
  - [ ] **Rollouts**: Enforce zero-downtime rolling updates with configured readiness, liveness, and startup probes.
  - [ ] **Observability**: Standardize JSON structured logging on the Spring Boot app and configure trace correlation IDs.
  - [ ] **Monitoring**: Set up GCP Cloud Monitoring dashboards (CPU/Memory utilization, DB pool connections) and critical metric alerts.

---

## Milestone 3: Distributed Processing Ingestion
* **Goal**: Decouple bulk processing workloads to isolate failure domains.
* **Scope**:
  - [x] Refactor bulk note imports from synchronous API threads into an asynchronous worker.
  - [x] Set up database tables (`import_job`, `import_job_item`) for durable job tracking.
  - [x] Integrate GCP Pub/Sub for publishing and subscribing to lightweight `notes.import.requested` notifications.
  - [x] Deploy the extracted `import-worker` independently to process bulk MongoDB writes.

---

## Milestone 4: Python AI Summarization Worker
* **Goal**: Introduce intelligence tagging and summarization inside notes.
* **Scope**:
  - [ ] Spin up `ai-worker` as a Python 3.11 service.
  - [ ] Bind `ai-worker` to a Pub/Sub topic listening for note update triggers.
  - [ ] Integrate Gemini API using structural JSON schema outputs to classify notes.
  - [ ] Update MongoDB note schemas with nested `ai` summary blocks.
