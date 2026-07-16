# CloudNotes Roadmap & Distributed System Plan

This document details the refined technical roadmap for CloudNotes, prioritizing changes that maximize portfolio and interview value. The target sequence focuses on turning a secure CRUD application into a highly scalable, event-driven, cloud-native distributed system.

---

## Interview-Ready Description
> **CloudNotes** is a containerized, event-driven note platform built with Java, Spring Boot, React, PostgreSQL, and MongoDB. The public API supports JWT and OAuth2-based authentication with per-user authorization. High-volume imports are processed asynchronously by independently scalable Spring Boot workers through Google Cloud Pub/Sub, using durable job state, idempotent consumers, retries, and dead-letter handling. The system is deployed to GKE through Terraform, Helm, and GitHub Actions. A Python AI worker provides schema-validated note summarization, categorization, and action-item extraction through an LLM integration.

---

## Skills Coverage Snapshots

| Skill / Technology | Status | Notes / Plan |
|---|---|---|
| **Java & Spring Boot** | ✅ Done | Layered architecture, Spring Security, Validation |
| **RESTful API Design** | ✅ Done | Complete CRUD, validation, centralized error handling |
| **SQL (PostgreSQL)** | ✅ Done | User accounts & authentication metadata, Flyway schema migrations |
| **MongoDB / NoSQL** | ⚠️ Basic | Backs note storage. Planned compound indexing, text search, optimistic locking, and cursor pagination |
| **JWT & API Security** | ✅ Done | httpOnly/SameSite cookies, per-resource ownership verification |
| **OAuth2 / OIDC** | ❌ Planned | Support Google OpenID Connect alongside JWT/passwords (Phase 4) |
| **Microservices / Distributed** | ❌ Planned | Move consumer worker to a separately deployable Spring Boot app (Phase 3) |
| **Containerization** | ❌ Planned | Multi-stage production-ready Dockerfiles & docker-compose stack (Phase 1) |
| **Messaging & Streaming** | ❌ Planned | Pub/Sub backed async bulk import processing with job records (Phase 2) |
| **Cloud-Native / GKE** | ❌ Planned | Terraform IaC & Helm deployment charts (Phase 5) |
| **CI/CD** | ❌ Planned | GitHub Actions automated tests, build, scan, and deploy (Phase 5) |
| **AI / Generative AI** | ❌ Planned | Constrained Python worker for schema-validated note summaries (Phase 6) |

---

## Phased Execution Roadmap

### Phase 1 — Production Containerization (Immediate Focus)
*Goal: Establish a stable, containerized local base before splitting services.*
- [ ] **Backend multi-stage Dockerfile**: Fast builds using cached dependencies and lightweight runner images (Eclipse Temurin JRE).
- [ ] **Frontend multi-stage Dockerfile**: Build Vite assets and serve them using Nginx.
- [ ] **Health and readiness endpoints**: Configure Spring Boot Actuator and Nginx status checks.
- [ ] **Full-stack Docker Compose**: Update root `docker-compose.yml` to orchestrate backend, frontend, PostgreSQL, and MongoDB.
- [ ] **Container-level integration testing**: Automate validation of the orchestrated containers.

### Phase 2 — Durable Asynchronous Processing
*Goal: Re-architect bulk import to use persistent job state and a messaging broker instead of in-memory queues.*
- [ ] **PostgreSQL Job Registry**: Track import task records with an `import_job` table:
  - `id` (UUID), `user_id` (String), `status` (PENDING, PROCESSING, COMPLETED, FAILED), `source_file_name`, `total_items`, `processed_items`, `failed_items`, `idempotency_key`, `created_at`, `started_at`, `completed_at`, `error_message`.
- [ ] **Pub/Sub Integration**: Publish lightweight notification messages containing job identifiers rather than entire payloads:
  ```json
  {
    "eventType": "notes.import.requested",
    "eventVersion": 1,
    "jobId": "c13c...",
    "userId": "913...",
    "objectLocation": "gs://cloudnotes-imports/...",
    "correlationId": "5ab..."
  }
  ```

### Phase 3 — Extract the Java Worker
*Goal: Separate concerns by deploying the message consumer in its own worker process.*
- [ ] **Deployable Worker Application**: Move the import consumer into a separately deployable Spring Boot service.
- [ ] **At-Least-Once Delivery**: Design consumer code to handle redeliveries safely.
- [ ] **Idempotence**: Guarantee idempotent writes using unique compound keys and job status assertions.
- [ ] **Exponential Backoff & Dead-Letter Topic (DLT)**: Handle transient failures gracefully and route persistent poison messages to a dead-letter queue.
- [ ] **Batch Writes & Concurrency**: Support high-throughput writes to MongoDB and tune concurrent thread pools.
- [ ] **Observability**: Implement structured logging, correlation IDs, and support graceful shutdown.

### Phase 4 — OAuth2 and Stronger API Security
- [ ] **Google OpenID Connect (OIDC)**: Add Google-based authentication Client to work alongside the cookie-based JWT flow.
- [ ] **CSRF Defense**: Strengthen anti-CSRF protection suited for cookie-based setups.
- [ ] **Session Renewal**: Implement refresh tokens or secure session-renewal mechanisms.
- [ ] **Rate Limiting & Security Headers**: Guard the public-facing API gateway against DDoS and script attacks.
- [ ] **Workload Identity**: Prepare deployment configurations to authenticate with GCP resources securely without static service-account keys.

### Phase 5 — GKE, Terraform, and CI/CD
- [ ] **Terraform Infrastructure (IaC)**: Provision VPC, GKE cluster, Artifact Registry, Cloud SQL (PostgreSQL), Pub/Sub, Cloud Storage, and Secret Manager.
- [ ] **Helm Deployment**: Package services with `Deployment`, `Service`, `Ingress`, `ConfigMaps`, and resource request/limit controls (HPA, PDB).
- [ ] **CI/CD Pipeline**: GitHub Actions to run tests, scan images for vulnerabilities, push to Artifact Registry, and trigger GKE rolling deployments.

### Phase 6 — Python AI Worker
- [ ] **AI Summarization Worker**: Create a lightweight Python service consuming from a dedicated topic.
- [ ] **Constrained Prompt Strategy**: Load notes, select prompt templates based on size/language, generate metadata, and validate against JSON Schema.
- [ ] **Metadata Capture**: Store AI outputs under a nested `ai` schema block in MongoDB and log token usage, model, and prompt versions.

---

## MongoDB Enhancements
To demonstrate advanced NoSQL expertise:
- [ ] **Compound Indexing**: Index on `(ownerId, archived, updatedAt)` to optimize note retrieval.
- [ ] **Text Search**: Add text indexes for full-text search capability.
- [ ] **Optimistic Locking**: Map a version field (`version` / `@Version`) to manage concurrent updates.
- [ ] **Cursor Pagination**: Implement cursor-based pagination for high-volume scrolling instead of offset-based pagination.
- [ ] **Testcontainers**: Integrate database integration tests using Testcontainers.

---

## High-Throughput Demonstration Scenario
Create a repeatable load test run to measure and prove system performance:
1. Import **100,000 generated notes** in bounded batches.
2. Scale from one worker replica to four.
3. Verify **batch MongoDB writes** (1,000-note chunks).
4. Simulate worker failures (killing a container midway) and ensure recovery without data duplication.
5. Capture real processing throughput and latency metrics and write them directly into the README.

---

## Targeted Directory Structure
```
DailyNotesGcp/
├── services/
│   ├── notes-api/          # Java/Spring Boot API Gateway
│   ├── import-worker/      # Java/Spring Boot Bulk Import Worker
│   └── ai-worker/          # Python AI Summarization Worker
├── frontend/               # React/TypeScript Vite application
├── contracts/
│   ├── asyncapi/           # Messaging contract definitions
│   └── json-schema/        # Data validation schemas
├── infrastructure/
│   ├── terraform/          # IaC for Google Cloud Platform (GCP)
│   └── helm/               # Helm charts for GKE deployment
├── load-tests/             # Load testing scripts (K6/JMeter)
├── docker-compose.yml      # Orchestrates all local services
├── .github/workflows/      # CI/CD pipelines
├── README.md
├── ARCHITECTURE.md
├── SECURITY.md
└── ROADMAP.md
```
