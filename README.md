# DailyNotesGcp (CloudNotes)

A cloud-native, multi-user note-taking application designed to be containerized and deployed on Google Kubernetes Engine (GKE).

The application is split into:
1. **notes-api** (`services/notes-api`): Java / Spring Boot REST API (Spring Security, JWT, PostgreSQL with Flyway migrations, MongoDB for note storage). Accepts bulk-import requests and publishes them to Pub/Sub rather than processing them inline.
2. **import-worker** (`services/import-worker`): Java / Spring Boot service that consumes bulk-import jobs from Google Cloud Pub/Sub and writes the resulting notes to MongoDB, independently of the API.
3. **common** (`services/common`): shared Gradle module holding the domain/persistence code (`ImportJob`, `NoteDocument`, Pub/Sub topic/subscription setup, etc.) used by both of the above.
4. **Frontend** (`frontend`): React / TypeScript / Vite / CSS.

All three Java modules build from a single root Gradle project (see `settings.gradle`), so builds and IDE imports start at the repo root, not inside `services/*`.

---

## Architecture Overview

- **PostgreSQL**: User accounts/authentication metadata, plus durable `import_job` / `import_job_item` state for bulk imports (all via Flyway-managed schema).
- **MongoDB**: The sole store for notes (title/content/archive state).
- **Google Cloud Pub/Sub**: Decouples bulk-import intake from processing. `notes-api` persists the job and publishes a lightweight `notes.import.requested` event; `import-worker` consumes it independently. Locally this runs against the `gcloud pubsub emulator` container in `docker-compose.yml`.
- **Docker Compose**: Orchestrates PostgreSQL, MongoDB, the Pub/Sub emulator, and (optionally) all three application containers for a fully containerized local stack.

### Asynchronous Bulk Import

1. `POST /api/notes/import` persists an `ImportJob` + one `ImportJobItem` per note (batched via `saveAll`) and publishes a `notes.import.requested` event containing only the `jobId`/`userId` — never the note payloads.
2. `import-worker` consumes the event and **atomically claims** the job with a conditional `UPDATE ... WHERE status = 'PENDING'`, so a redelivered or concurrently-handled copy of the same message can't be processed twice.
3. Notes are written to MongoDB in chunks of 1,000. Each note's ID is derived from its `ImportJobItem` ID (not a fresh random UUID), so re-running a chunk after a crash or redelivery **upserts** the same document instead of creating a duplicate.
4. `GET /api/notes/import/{taskId}` reports job status (`PENDING` → `PROCESSING`/`COMPLETED`/`FAILED`) and is restricted to the job's owner.

---

## Security & Tenant Isolation

- **HTTP-Only Cookies**: User authentication relies on JSON Web Tokens (JWT) stored in secure, `httpOnly`, and `SameSite` cookies, mitigating cross-site scripting (XSS) risks. 
- **Rootless Containers**: The frontend image uses `nginx-unprivileged:alpine`, running entirely as a non-root user (restricting it to port `8080` internally) to reduce the container's security exposure.
- **Same-Origin Reverse Proxy**: Nginx acts as a unified entry point, reverse-proxying `/api/` traffic directly to the backend over the internal network. This removes CORS requirements in production.
- **Security Headers**: Nginx is configured to inject security headers (`X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`) and disable caching for the app shell (`/index.html`).
- **Async Bulk Import Isolation**: Import jobs (`ImportJob`) are persisted with the owning user's identity and looked up via `findByIdAndUserId`, so `GET /api/notes/import/{taskId}` returns 403 for anyone other than the job's owner — enforced independently of which service (API or worker) touches the row.

---

## Local Development Setup

### Prerequisites
- Java 21 or higher
- Node.js 18 or higher (with npm)
- Docker & Docker Compose

### 1. Infrastructure (Postgres, MongoDB, Pub/Sub emulator)
Start the local dependencies with Docker Compose:
```bash
docker compose up -d postgres mongodb pubsub
```
This spins up:
- PostgreSQL on port `5433`
- MongoDB on port `27017`
- A Google Cloud Pub/Sub emulator on port `8089`

> Prefer to run everything in containers instead? `docker compose up -d --build` also builds and starts `notes-api`, `import-worker`, and the frontend (ports `8085`, `8087`, and `8086` respectively) — skip straight to the [Frontend Setup](#4-frontend-setup) section if you go this route.

### 2. Notes API Setup
All Gradle commands run from the **repo root** (the multi-module build lives there, not inside `services/notes-api`):
```bash
./gradlew :services:notes-api:bootRun
```
The API server will run at `http://localhost:8080`, using the same Postgres/MongoDB/Pub/Sub defaults as `docker-compose.yml`.

### 3. Import Worker Setup
In a separate terminal, also from the repo root:
```bash
./gradlew :services:import-worker:bootRun
```
The worker has no HTTP surface of its own beyond Actuator (`http://localhost:8081/actuator/health`); it subscribes to Pub/Sub and processes bulk-import jobs created via the API above.

### 4. Frontend Setup
1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Run the Vite development server:
   ```bash
   npm run dev
   ```
The frontend application will be accessible at `http://localhost:5173`.

---

## Database Migrations
We use Flyway to manage relational database schema migrations. The migration scripts reside in:
- [services/notes-api/src/main/resources/db/migration/](services/notes-api/src/main/resources/db/migration/) — `V1` creates the `users` table, `V2` creates `import_job` / `import_job_item`.

---

## Configuration

Local dev config lives in `services/notes-api/src/main/resources/application.properties` and
`services/import-worker/src/main/resources/application.properties`; every value in each is
overridable via environment variable (matching the docker-compose defaults out of the box),
including `PUBSUB_EMULATOR_HOST` / `GCP_PROJECT_ID` for the local Pub/Sub emulator.

For a real deployment of the API, activate the `prod` profile
(`services/notes-api/src/main/resources/application-prod.properties`) via `SPRING_PROFILES_ACTIVE=prod`.
That profile has **no default values** - it fails fast at startup unless `DB_URL`, `DB_USER`,
`DB_PASSWORD`, `MONGODB_URI`, `JWT_SECRET`, and `CORS_ALLOWED_ORIGINS` are all supplied
(e.g. injected from GCP Secret Manager).

Auth is a short-lived JWT delivered as an httpOnly, SameSite cookie (not readable by JS,
not stored in localStorage). The frontend reads `VITE_API_BASE_URL` from
`frontend/.env` (see `frontend/.env.example`) to know where to send requests.

---

## Deployment (GKE)
This application is designed to run on Google Kubernetes Engine (GKE). Production infrastructure components can be provisioned using:
- **Terraform** (`infrastructure/terraform`) for GCP resources: GKE cluster, Artifact Registry, Cloud SQL (PostgreSQL), IAM/Workload Identity bindings, and Secret Manager. *(Authored; not yet applied against a live GCP project.)*
- **Helm** (`infrastructure/helm/cloudnotes`) for container deployments: `notes-api`, `import-worker`, and the frontend, each with their own `Deployment`/`Service`, plus a shared `Gateway`/`HTTPRoute` and Secret Store CSI provider class.

The Pub/Sub topic and subscription used for bulk imports are currently provisioned at application startup (see `PubSubConfig`) rather than via Terraform.

---

## TBD (To Be Done) / Future Roadmap

The following components and features are planned for future development to align with production best practices and advanced requirements. See [ROADMAP.md](ROADMAP.md) for the detailed, skill-tagged version of this plan.

### 1. Containerization & Orchestration
- [x] **Dockerfiles**: Implement production-ready, multi-stage Dockerfiles for both the Spring Boot backend and React frontend.
- [x] **Helm Charts**: Author Helm charts to manage Kubernetes deployments, services, ingress, and secret management on GKE.

### 2. Infrastructure as Code (IaC) & DevOps
- [x] **Terraform Configuration**: Create Terraform scripts to provision GCP infrastructure, including a GKE cluster, Artifact Registry, Cloud SQL (PostgreSQL), IAM, and Secret Manager.
- [ ] **CI/CD Pipelines**: Set up GitHub Actions or Google Cloud Build pipelines for automated testing, linting, Docker image building, and GKE deployment.

### 3. Architecture & Scale
- [x] **Messaging & Event Streaming**: Integrate Google Cloud Pub/Sub for asynchronous bulk-import processing, decoupled from the API into its own worker service.
- [ ] **High-Throughput Workloads**: Integrate caching (Redis/Memorystore) and API rate-limiting to support high-volume workloads.
- [ ] **Dead-Letter Handling**: Configure a Pub/Sub dead-letter topic and retry policy for the import subscription (currently relies on default nack/redelivery only).

### 4. AI & Generative AI Integration
- [ ] **LLM / Copilot Integration**: Incorporate AI capabilities such as AI-assisted search, notes categorization, or auto-summarization using Large Language Models (LLMs) and agentic frameworks.

### 5. API Security & Service Architecture
- [ ] **OAuth2**: Add an OAuth2 login path (e.g. Google) alongside the existing JWT-based auth.
- [x] **Microservices Split**: Extract the bulk-import worker into its own deployable unit (`import-worker`) communicating over Pub/Sub, rather than one monolith handling everything.
