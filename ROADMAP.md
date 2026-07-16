# CloudNotes Roadmap

This document tracks the project's technical roadmap against the stack of skills and
architecture patterns it's meant to demonstrate. It's more detailed than the README's
"TBD" checklist: each initiative here is tagged with the specific
skill/technology it exercises, so progress can be judged against the target list, not
just against "does the app work."

Last updated: 2026-07-16.

---

## Skills coverage snapshot

| Skill / technology | Status | Notes |
|---|---|---|
| Java + Spring Boot | ✅ Done | Layered architecture (api/application/domain/infrastructure), Spring Security |
| RESTful API design | ✅ Done | Full CRUD, validation, proper status codes, centralized exception handling |
| SQL database (PostgreSQL) | ✅ Done | Flyway-managed schema, JPA, used for user/auth data |
| MongoDB / NoSQL | ✅ Done (basic) | Backs note storage; no indexes/aggregation pipelines yet - stretch goal |
| JWT authentication & authorization | ✅ Done | httpOnly cookie delivery, per-resource ownership checks, role-based `@PreAuthorize` |
| OAuth2 | ❌ Not started | Explicitly named in the target skill list; currently only custom JWT exists |
| Microservices / distributed systems | ❌ Not started | Currently a single monolith |
| High-volume / high-throughput processing | ⚠️ Weak | `BulkImportService` exists but uses an in-memory task registry - doesn't survive a restart or scale past one instance |
| Containerization | ⚠️ Partial | docker-compose runs the *databases*; no Dockerfile for the app itself yet |
| Cloud-native / GKE | ❌ Not started | README describes intent; no manifests/Helm chart exist |
| GCP specifics (Secret Manager, Cloud SQL, Workload Identity) | ⚠️ Partial | `application-prod.properties` is env-var-driven and Secret-Manager-ready, but nothing GCP-specific is wired up |
| CI/CD | ❌ Not started | No pipeline exists |
| Messaging / event streaming | ❌ Not started | Bonus skill; pairs naturally with the high-throughput gap above |
| AI agents / LLMs / generative AI | ❌ Not started | Bonus skill |
| Python | ❌ Not started | Bonus skill; smallest payoff unless deliberately targeted |

---

## Phased plan

Each phase name is tagged with the skill(s) it's meant to demonstrate.

### Phase 1 - OAuth2 `[OAuth2, API security]`
- [ ] Add a second login path via Spring Security's OAuth2 Client (e.g. "Sign in with Google"), alongside the existing JWT/password flow.
- [ ] Document the difference between the two auth paths and when each applies.

### Phase 2 - Messaging-backed high-throughput processing `[Messaging, High-throughput]`
- [ ] Replace `BulkImportService`'s in-memory task registry with a durable queue (Kafka or GCP Pub/Sub) and a consumer that persists task state to a datastore.
- [ ] This also fixes a real correctness bug flagged earlier: task status currently lives in a single pod's memory and is lost on restart or missed by other replicas.

### Phase 3 - Microservices split `[Microservices, Distributed systems]`
- [ ] Extract the queue consumer from Phase 2 into its own deployable service, so two services communicate over a broker instead of one process handling everything.
- [ ] Document the service boundary and why it was drawn there.

### Phase 4 - Containerization & CI/CD `[Containerization, Cloud-native, DevOps, CI/CD]`
- [ ] Production-ready multi-stage Dockerfiles for backend and frontend.
- [ ] A docker-compose that runs the *entire* stack (not just the databases) for local dev/demo.
- [ ] Kubernetes manifests or a Helm chart (Deployment, Service, Ingress, secrets).
- [ ] GitHub Actions pipeline: build, test, lint, image build/push.

### Phase 5 - GCP / GKE specifics `[GCP, GKE]`
- [ ] Terraform for GKE cluster, VPC, Cloud SQL, Memorystore.
- [ ] Workload Identity instead of static credentials.
- [ ] Wire `application-prod.properties` values to real Secret Manager entries.

### Phase 6 - AI/LLM bonus feature `[LLM, Generative AI]`
- [ ] A small, real feature - e.g. AI-assisted note summarization or categorization via an LLM API call from the backend.

### Phase 7 (optional) - Python `[Python]`
- [ ] Only worth doing deliberately - e.g. write the Phase 2/3 queue consumer in Python instead of Java, if diversifying language exposure matters more than architectural consistency.

---

## Out of scope for this roadmap

Feature work that would improve the app as a *product* (rich text editing, sharing/collaboration,
mobile support, etc.) isn't tracked here unless it happens to also demonstrate a target skill -
this document exists to prioritize portfolio value, not product completeness.
