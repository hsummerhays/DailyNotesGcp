# Document 04: Kubernetes Orchestration & Helm Packaging

This document describes how the applications are packaged, scheduled, and configured inside GKE.

---

## 1. GKE Autopilot Architecture

We utilize GKE Autopilot clusters, which automatically manage node provisioning, autoscaling, updates, and cluster security baselines. Since nodes are managed dynamically:
* Pod resource requests must match GKE Autopilot scheduling limits (e.g. minimum ratio of CPU to memory).
* Pods are scheduled across multiple zones automatically for high availability.

---

## 2. Ingress & Routing via Gateway API

Rather than using older, legacy Kubernetes `Ingress` controllers, we use the modern **Kubernetes Gateway API** class `gke-l7-gxlb`.

* **Gateway** (`gateway.yaml`): Declares the physical HTTP load balancer listener at port 80.
* **HTTPRoute** (`httproute.yaml`): Directs incoming paths:
  * `/api/*` requests bypass the frontend entirely and route directly to the `cloudnotes-backend` service.
  * All other requests (`/*`) route to the `cloudnotes-frontend` service.

---

## 3. Secret Management & In-Cluster Services

Secrets are managed and injected securely into the backend and worker pods:
1. **Kubernetes Secret** (`backend-secret.yaml`): Generates the `cloudnotes-backend-secret-sync` secret from `.Values.backend.secrets` passed via Helm (injected at deploy time from CI/CD repository secrets).
2. **Key Injection**: Spring Boot containers read these keys (`DB_PASSWORD`, `MONGODB_URI`, `JWT_SECRET`) directly as environment variables from `secretKeyRef` bindings on `cloudnotes-backend-secret-sync`.
3. **In-Cluster MongoDB**: For development/testing environments, `mongodb.yaml` provisions an in-cluster MongoDB StatefulSet/Service with persistent storage (`mongodb://mongodb:27017/cloudnotes`).


---

## 4. Deployment Verification Checks

To verify that the application is running reliably after a Helm installation:
1. Get pod status:
   ```bash
   kubectl get pods -n default
   ```
2. Verify Gateway endpoint IP allocation:
   ```bash
   kubectl get gateway cloudnotes-gateway
   ```
3. Test health probes (Actuator):
   * Backend Liveness: `/actuator/health/liveness`
   * Backend Readiness: `/actuator/health/readiness`
   * Frontend: `/health`
