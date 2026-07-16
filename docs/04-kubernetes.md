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

## 3. Secret Manager CSI Sync

Secrets are injected securely without exposing static credentials in environment manifests:
1. **SecretProviderClass** (`secret-provider-class.yaml`): Declares the Secret Store CSI driver bindings.
2. **Mounting**: Pod containers mount the CSI volume. This triggers the CSI driver to query Secret Manager (validated using GKE Workload Identity IAM).
3. **Synchronizing**: The driver synchronizes the fetched values into standard Kubernetes `Secret` objects.
4. **Injection**: Spring Boot containers read these keys (`DB_PASSWORD`, `MONGODB_URI`, `JWT_SECRET`) directly as environment variables from the synced secret references.

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
