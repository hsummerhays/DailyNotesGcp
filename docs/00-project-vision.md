# Document 00: Project Vision

## 1. Executive Summary
**CloudNotes** (DailyNotesGcp) is a production-grade, cloud-native reference application designed to demonstrate modern software engineering, Infrastructure as Code (IaC), container orchestration, and microservices architecture patterns on Google Cloud Platform (GCP).

While the application domains—note taking, user accounts, and bulk imports—are simple, they serve as vehicles to focus on complex operational challenges: GKE lifecycle management, secret synchronization, distributed messaging, horizontal scaling, and end-to-end observability.

---

## 2. Target Persona & Value Proposition
This repository serves as a **technical showcase** for engineering leaders, system architects, and hiring managers. It demonstrates:
* **Operator Mentality**: Managing clusters, scaling workloads, configuring ingress, and troubleshooting container faults.
* **Architectural Rigor**: Using appropriate messaging brokers, decoupling ingestion loops, and planning database topologies.
* **Security Discipline**: Rootless container layers, Workload Identity bindings, and automated secret retrieval.

---

## 3. Technology Stack Choice
* **Hosting Platform**: Google Kubernetes Engine (GKE) Autopilot for managed operational control and automatic node scaling.
* **Backend Framework**: Java 21 / Spring Boot 3.x (Spring Security, Spring Data JPA, Spring Data MongoDB).
* **Frontend**: React / TypeScript / Vite.
* **Relational DB**: Cloud SQL PostgreSQL 16 (user credentials, session tokens, and transaction states).
* **NoSQL DB**: MongoDB Atlas (scalable, schema-flexible document note storage).
* **Messaging Broker**: Google Cloud Pub/Sub (decoupling ingress intake from backend bulk operations).
* **IaC & Package Management**: Terraform & Helm.
