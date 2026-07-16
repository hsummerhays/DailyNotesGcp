# DailyNotesGcp (CloudNotes)

A cloud-native, multi-user note-taking application designed to be containerized and deployed on Google Kubernetes Engine (GKE).

The application is split into:
1. **Backend**: Java / Spring Boot (REST API, Spring Security, JWT, PostgreSQL with Flyway migration, and MongoDB for NoSQL storage).
2. **Frontend**: React / TypeScript / Vite / CSS.

---

## Architecture Overview

- **PostgreSQL**: Used for user accounts and authentication metadata (via Flyway-managed schema).
- **MongoDB**: Used as the sole store for notes (title/content/archive state).
- **Docker Compose**: Orchestrates local PostgreSQL and MongoDB database containers.

---

## Security & Tenant Isolation

- **HTTP-Only Cookies**: User authentication relies on JSON Web Tokens (JWT) stored in secure, `httpOnly`, and `SameSite` cookies, mitigating cross-site scripting (XSS) risks. 
- **CORS Restraints**: Configured to restrict access using explicit origins via `app.cors.allowed-origins` config properties, preventing unauthorized websites from executing cross-origin credentialed requests.
- **Async Bulk Import Isolation**: The backend supports asynchronous bulk note imports. Import tasks (`ImportTaskStatus`) are tied directly to the authenticated user (`principal.getName()`), preventing unauthorized users from accessing task statuses of other users.

---

## Local Development Setup

### Prerequisites
- Java 21 or higher
- Node.js 18 or higher (with npm)
- Docker & Docker Compose

### 1. Database Infrastructure
Start the local PostgreSQL and MongoDB databases using Docker Compose:
```bash
docker compose up -d
```
This spins up:
- PostgreSQL on port `5433`
- MongoDB on port `27017`

### 2. Backend Setup
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Build the Spring Boot application:
   ```bash
   ./gradlew build
   ```
3. Run the application:
   ```bash
   ./gradlew bootRun
   ```
The backend API server will run at `http://localhost:8080`.

### 3. Frontend Setup
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
- [backend/src/main/resources/db/migration/](backend/src/main/resources/db/migration/)

---

## Configuration

Local dev config lives in `backend/src/main/resources/application.properties`; every value
there is overridable via environment variable (matching the docker-compose defaults out of
the box). For any real deployment, activate the `prod` profile
(`backend/src/main/resources/application-prod.properties`) via `SPRING_PROFILES_ACTIVE=prod`.
That profile has **no default values** - it fails fast at startup unless `DB_URL`, `DB_USER`,
`DB_PASSWORD`, `MONGODB_URI`, `JWT_SECRET`, and `CORS_ALLOWED_ORIGINS` are all supplied
(e.g. injected from GCP Secret Manager).

Auth is a short-lived JWT delivered as an httpOnly, SameSite cookie (not readable by JS,
not stored in localStorage). The frontend reads `VITE_API_BASE_URL` from
`frontend/.env` (see `frontend/.env.example`) to know where to send requests.

---

## Deployment (GKE)
This application is designed to run on Google Kubernetes Engine (GKE). Production infrastructure components can be provisioned using:
- **Terraform** for GCP resources (GKE cluster, VPC, IAM, Google Cloud SQL, Memorystore).
- **Helm** charts for container deployments.

---

## TBD (To Be Done) / Future Roadmap

The following components and features are planned for future development to align with production best practices and advanced requirements. See [ROADMAP.md](ROADMAP.md) for the detailed, skill-tagged version of this plan.

### 1. Containerization & Orchestration
- [ ] **Dockerfiles**: Implement production-ready, multi-stage Dockerfiles for both the Spring Boot backend and React frontend.
- [ ] **Helm Charts**: Author Helm charts to manage Kubernetes deployments, services, ingress, and secret management on GKE.

### 2. Infrastructure as Code (IaC) & DevOps
- [ ] **Terraform Configuration**: Create Terraform scripts to provision GCP infrastructure, including a GKE cluster, VPC network, Cloud SQL (PostgreSQL), and Memorystore (Redis/MongoDB).
- [ ] **CI/CD Pipelines**: Set up GitHub Actions or Google Cloud Build pipelines for automated testing, linting, Docker image building, and GKE deployment.

### 3. Architecture & Scale
- [ ] **High-Throughput Workloads**: Integrate caching (Redis/Memorystore) and API rate-limiting to support high-volume workloads.
- [ ] **Messaging & Event Streaming**: Integrate Google Cloud Pub/Sub or Apache Kafka to support asynchronous message processing or event-driven microservices patterns.

### 4. AI & Generative AI Integration
- [ ] **LLM / Copilot Integration**: Incorporate AI capabilities such as AI-assisted search, notes categorization, or auto-summarization using Large Language Models (LLMs) and agentic frameworks.

### 5. API Security & Service Architecture
- [ ] **OAuth2**: Add an OAuth2 login path (e.g. Google) alongside the existing JWT-based auth.
- [ ] **Microservices Split**: Extract a service (e.g. the bulk-import worker) into its own deployable unit communicating over the messaging layer above, rather than one monolith handling everything.

