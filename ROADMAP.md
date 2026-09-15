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
| **Microservices / Distributed** | ✅ Done | Extracted standalone Spring Boot worker for bulk imports (Phase 3) |
| **Containerization** | ✅ Done | Multi-stage production-ready Dockerfiles & docker-compose stack |
| **Messaging & Streaming** | ✅ Done | GCP Pub/Sub backed async bulk imports with PostgreSQL job state (Phase 2) |
| **Cloud-Native / GKE** | ✅ Done | Terraform IaC & Helm configs deployed to GKE Autopilot (Phase 5) |
| **CI/CD** | ✅ Done | GitHub Actions automated tests, multi-stage Docker build/push, and Helm deploy (Phase 5) |
| **AI / Generative AI** | ❌ Planned | Constrained Python worker for schema-validated note summaries (Phase 6) |

---

## Phased Execution Roadmap

### Phase 1 — Production Containerization (Immediate Focus)
*Goal: Establish a stable, containerized local base before splitting services.*
- [x] **Backend multi-stage Dockerfile**: Fast builds using cached dependencies and lightweight runner images (Eclipse Temurin JRE).
- [x] **Frontend multi-stage Dockerfile**: Build Vite assets and serve them using Nginx.
- [x] **Health and readiness endpoints**: Configure Spring Boot Actuator and Nginx status checks.
- [x] **Full-stack Docker Compose**: Update root `docker-compose.yml` to orchestrate backend, frontend, PostgreSQL, and MongoDB.
- [x] **Container-level integration testing**: Automate validation of the orchestrated containers.

### Phase 2 — Durable Asynchronous Processing
*Goal: Re-architect bulk import to use persistent job state and a messaging broker instead of in-memory queues.*
- [x] **PostgreSQL Job Registry**: Track import task records with an `import_job` table.
- [x] **Pub/Sub Integration**: Publish lightweight notification messages containing job identifiers rather than entire payloads.

### Phase 3 — Extract the Java Worker
*Goal: Separate concerns by deploying the message consumer in its own worker process.*
- [x] **Deployable Worker Application**: Move the import consumer into a separately deployable Spring Boot service.
- [x] **At-Least-Once Delivery**: Design consumer code to handle redeliveries safely (Pub/Sub ack/nack semantics).
- [x] **Idempotence**: Atomically claim a job (`UPDATE ... WHERE status = 'PENDING'`) before processing it, and derive each note's ID from its job item so re-running a chunk upserts rather than duplicates.
- [x] **Exponential Backoff & Dead-Letter Topic (DLT)**: Configured the primary subscription (`notes-import-sub`) with an exponential backoff policy and a dead-letter policy routing failures to `notes-import-dl-topic` after 5 attempts.
- [x] **Batch Writes**: MongoDB and Postgres item-status writes both go through `saveAll` in 1,000-item chunks. *(Concurrent thread-pool tuning is still single-threaded per job.)*
- [x] **Observability**: Implement structured logging, correlation IDs, and support graceful shutdown.

### Phase 4 — OAuth2 and Stronger API Security
- [x] **Google OpenID Connect (OIDC)**: Added `spring-boot-starter-oauth2-client` dependency and created `CustomOAuth2SuccessHandler` to issue standard JWT HTTP-only cookies on successful Google OIDC login.
- [x] **CSRF Defense**: Configured CookieCsrfTokenRepository (httpOnly=false) and CsrfCookieFilter for single-page applications.
- [x] **Session Renewal**: Implemented database-backed refresh tokens, automatic rotation on usage, HTTP-only cookie delivery, and secure revocation.
- [x] **Rate Limiting & Security Headers**: Implemented a thread-safe token bucket algorithm in a custom `RateLimitingFilter` keyed by client IP, and configured CSP/frame protection headers in Spring Security.
- [ ] **Workload Identity**: Prepare deployment configurations to authenticate with GCP resources securely without static service-account keys.

### Phase 5 — GKE, Terraform, and CI/CD
- [x] **Terraform Infrastructure (IaC)**: Provision GKE Autopilot cluster, Artifact Registry, Cloud SQL (PostgreSQL), IAM, and Secret Manager. Tested and verified against live GCP project (`daily-notes-gcp`).
- [x] **Helm Deployment**: Package services with `Deployment`, `Service`, `Gateway`/`HTTPRoute`, and a Secret Store CSI provider class. Successfully deployed to live GKE cluster.
- [x] **CI/CD Pipeline**: GitHub Actions workflow (`.github/workflows/deploy.yml`) runs tests, builds multi-stage Docker images, pushes to Artifact Registry, connects via `google-github-actions/get-gke-credentials`, and performs rolling Helm deployments. Verified end-to-end.

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
│   ├── common/             # Shared Gradle module (domain/persistence code for the two services below)
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
