# Document 06: High-Throughput & Performance Design

This document details the strategies and benchmarks planned to support high-throughput, scaleable performance.

---

## 1. Batch Write Optimization (MongoDB)
* **Problem**: Saving notes one by one introduces excessive database network round-trip overhead.
* **Solution**: The `import-worker` service consumes items in bulk and performs batched insertions. We use Spring Data's `BulkOperations` (or MongoDB bulk writes):
  ```java
  BulkOperations bulkOps = mongoTemplate.bulkOps(BulkMode.UNORDERED, NoteDocument.class);
  bulkOps.insert(listOfNotes);
  bulkOps.execute();
  ```
  This reduces database write operations into single TCP segments of 1,000 documents, greatly maximizing throughput.

---

## 2. Memorystore (Redis) Caching
* **Read Heavy Pattern**: Notes are frequently read but updated less often.
* **Caching Layer**: We introduce a caching layer for active user workspaces:
  * Cache annotations: `@Cacheable(value = "notes", key = "#ownerEmail")`
  * Cache eviction: On note creation, update, or archive, evict the user's specific note list cache entry (`@CacheEvict`).
* **Session Cache**: JWT signature and session verification can also be cached inside Redis to avoid repeatedly querying PostgreSQL.

---

## 3. Rate Limiting (API Gateway)
To protect Spring Boot from scraping scripts or DDoS abuse, we add rate-limiting rules at the entry point:
* **GKE Gateway Rules**: Apply Cloud Armor policies or GKE Gateway rate-limit annotations based on client IP.
* **Spring Boot Bucket4j**: Add endpoint rate-limiting filter enforcing limits (e.g. 60 requests per minute per IP for normal endpoints, 5 requests per minute for bulk import triggers).
