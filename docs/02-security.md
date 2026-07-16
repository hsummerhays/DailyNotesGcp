# Document 02: Security and Compliance Model

This document outlines the security controls, authentication standards, and data separation strategies implemented within **CloudNotes**.

---

## 1. Authentication & Session Transit
Authentication is fully stateless and token-based, operating with defensive design principles:

* **JWT Credentials**: Authentication claims are encoded into JSON Web Tokens (JWT) signed using a secure secret key.
* **HttpOnly Cookies**: Session tokens are transmitted back to the client inside an HTTP response header setting `httpOnly` and `SameSite` cookie controls:
  * **HttpOnly**: Binds the cookie value entirely to network transactions, preventing malicious Javascript or XSS injection vectors from extracting the active session token.
  * **SameSite**: Enforces strict browser cookie containment policies to guard against Cross-Site Request Forgery (CSRF).
  * **Secure**: Enforced to guarantee that tokens are only sent over encrypted SSL/TLS channels in production environments.

---

## 2. Runtime Privilege Isolation

We implement **least privilege** defaults across both the hosting runtime and database access layers:

### A. Rootless Container Runtime
To prevent container escape exploits from gaining root-level host system privileges:
* The React frontend container uses `nginxinc/nginx-unprivileged:alpine`. The web server executes under a non-root, non-privileged system user account, restricted from binding to privileged port spaces (using internal port `8080`).
* The Spring Boot services (`notes-api` and `import-worker`) run as a dedicated, non-privileged system user:
  ```dockerfile
  RUN addgroup -S spring && adduser -S spring -G spring
  USER spring
  ```

### B. Workload Identity
Rather than packaging hardcoded GCP service-account keys or saving JSON credentials inside container files:
* We configure GKE **Workload Identity**. 
* The Kubernetes Service Account `cloudnotes-backend` is mapped directly to a restricted Google Service Account (GSA) using IAM trust bindings. The GSA is granted access only to the necessary resources:
  * **Cloud SQL Client** (`roles/cloudsql.client`) to route PostgreSQL traffic.
  * **Secret Manager Secret Accessor** (`roles/secretmanager.secretAccessor`) to fetch DB passwords and connection strings.
  * **Pub/Sub Publisher & Subscriber** (`roles/pubsub.publisher`, `roles/pubsub.subscriber`) to route message events.

### C. Database Connection Proxy
In GKE deployments, database connections to Google Cloud SQL bypass exposure to public internet endpoints using the **Cloud SQL Auth Proxy** running as a secure localhost sidecar container inside the application pods.

---

## 3. Multi-Tenant Separation

We guarantee complete logical tenant isolation at the service API layer:
* **Task Ownership**: All bulk note import actions and status query calls check and enforce identity ownership.
* Looking up an `ImportJob` is restricted by appending the authenticated caller's identity:
  ```java
  importJobRepository.findByIdAndUserId(jobId, ownerEmail);
  ```
  If a user attempts to query a job belonging to a different identity, the application immediately throws an `AccessDeniedException` resulting in a `403 Forbidden` response.
