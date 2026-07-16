# Document 01: Architecture Design

## 1. System Topology
The application is structured into decoupled components to isolate failure domains and enable independent scaling.

```mermaid
graph TD
    subgraph Client Layer
        Browser[Vite React Frontend]
    end

    subgraph Routing Layer
        Gateway[GKE Gateway / HTTPRoute]
    end

    subgraph Service Layer
        API[notes-api Service]
        Worker[import-worker Service]
    end

    subgraph Messaging Layer
        PubSub[(Google Cloud Pub/Sub)]
    end

    subgraph Database Layer
        Postgres[(PostgreSQL Cloud SQL)]
        Mongo[(MongoDB Atlas)]
    end

    Browser -->|HTTPS| Gateway
    Gateway -->|/*| Browser
    Gateway -->|/api/*| API
    
    API -->|Read/Write Auth & Jobs| Postgres
    API -->|Publish Job Notification| PubSub
    
    PubSub -->|Subscribe to Import Event| Worker
    Worker -->|Fetch Job Payload & Claim| Postgres
    Worker -->|Bulk Insert Notes| Mongo
    
    API -->|CRUD Notes| Mongo
```

---

## 2. Ingestion Sequence Flow
Below is the sequence flow for an asynchronous bulk import task.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant API as notes-api
    participant DB as PostgreSQL
    participant Broker as Pub/Sub
    participant Worker as import-worker
    participant NoSQL as MongoDB

    User->>API: POST /api/notes/import (JSON list)
    activate API
    API->>DB: Save parent ImportJob (PENDING)
    API->>DB: Save child ImportJobItems (PENDING)
    API->>Broker: Publish notes.import.requested (jobId)
    API-->>User: Return jobId & 202 Accepted
    deactivate API

    activate Worker
    Broker->>Worker: Consume notes.import.requested (jobId)
    Worker->>DB: UPDATE import_job SET status='PROCESSING' WHERE id=jobId AND status='PENDING'
    Note over Worker,DB: Atomic claim prevents concurrent double-processing
    
    Worker->>DB: Fetch child ImportJobItems
    Worker->>NoSQL: Bulk insert NoteDocuments
    Worker->>DB: UPDATE import_job SET status='COMPLETED', processed_items=total
    Worker->>Broker: Acknowledge (ack) message
    deactivate Worker
```

---

## 3. Architectural Decision Records (ADRs)

### ADR 01: Decoupling Bulk Note Ingest
* **Status**: Approved
* **Context**: Bulk importing notes can result in large payloads and heavy database write volume. Processing these synchronously within the API gateway request thread blocks the HTTP thread pool, degrades API responsiveness, and risks thread exhaustion.
* **Decision**: We use an asynchronous ingest pattern. The API persists the payload to PostgreSQL as raw child job records and publishes a lightweight event (containing only IDs) to GCP Pub/Sub. A standalone worker service consumes the queue.
* **Consequences**:
  * API requests return immediately with `202 Accepted`.
  * Failure to write to MongoDB does not block the front-end user session.
  * Worker capacity can be scaled up/down independently of the API gateway.

### ADR 02: Dual Database Strategy (Polyglot Persistence)
* **Status**: Approved
* **Context**: User auth and job metadata require relational constraints, transaction isolation (ACID), and schema enforcement. Notes, however, are document-oriented, have high write/read volumes, and could eventually expand to include unstructured data (such as AI summarization metadata).
* **Decision**: We store user profiles and import jobs in PostgreSQL (Cloud SQL). We store note text collections in MongoDB (Atlas).
* **Consequences**:
  * Prevents bloating the relational database with document contents.
  * Enables fast horizontal scale for note collections while maintaining transactional safety for auth records.
