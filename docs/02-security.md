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
* **CSRF Defense**: Enforces the Double-Submit Cookie pattern for mutating requests (POST, PUT, DELETE) using a `CookieCsrfTokenRepository.withHttpOnlyFalse()` and a custom `CsrfCookieFilter` to populate the `XSRF-TOKEN` cookie for React SPA consumption.
* **Persistent Session Renewal (Refresh Tokens)**: Implements database-backed refresh tokens saved in PostgreSQL (`RefreshToken`) and delivered via HTTP-only cookies. Requesting `/api/auth/refresh` rotates the token (invalidates the old, issues a new one with a fresh JWT) to defend against session hijacking and token replay attacks.
* **Rate Limiting**: Protects gateway endpoints against DDoS and credential stuffing with a thread-safe in-memory Token Bucket filter (`RateLimitingFilter`) keyed by client IP (using proxy-aware `X-Forwarded-For` chains).
* **HTTP Security Headers**: Enforces modern security headers in the Spring filter chain including strict frame options (`DENY`) and Content Security Policy (`default-src 'self'`).

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

---

## 4. Key Infrastructure Security Decisions

The following architectural constraints and infrastructure options have been identified for future security hardening:

### A. HTTP vs. HTTPS / Gateway TLS Configuration
* **Current State**: The Helm gateway (`cloudnotes-gateway`) is configured with a Port 80 HTTP listener for simple development/testing deployments. However, application cookies are configured with `cookie-secure=true`.
* **Decision**: In production, a Google-managed SSL Certificate must be created and attached to the GKE Gateway Class, and a Port 443 HTTPS listener must be configured in `gateway.yaml` to ensure cookie transport over encrypted TLS.

### B. CI/CD Authentication (Service Account Keys vs. Workload Identity Federation)
* **Current State**: GitHub Actions authenticates to Google Cloud using a static, long-lived Service Account JSON Key (`GCP_SA_KEY`) stored in GitHub Secrets.
* **Decision**: To follow security best practices and eliminate the risk of compromised long-lived keys, the deployment pipeline should be migrated to **Workload Identity Federation (WIF)**, which uses short-lived OpenID Connect (OIDC) tokens issued by GitHub Actions.

### C. Cloud SQL Networking & Enforced SSL
* **Current State**: The Cloud SQL database Instance has a public IP enabled (protected by Authorized Networks) and does not currently enforce SSL.
* **Decision**: Access should be restricted to GKE's private IP network using VPC Peering. In addition, `require_ssl=true` should be enabled on the Cloud SQL instance, enforcing all connections to go through the Cloud SQL Auth Proxy sidecar container which handles encryption and mutual TLS (mTLS) tunneling automatically.

