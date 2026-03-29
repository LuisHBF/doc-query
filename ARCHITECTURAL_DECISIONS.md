# Architectural Decisions — doc-query

← [Back to README](README.md)

This document captures the architectural context, system design, and the reasoning behind every significant technical decision made in this project. It is intended for engineers who want to understand not just what was built, but why.

---

## Context

doc-query is a portfolio project built to demonstrate production-grade microservices patterns on the Java/Spring ecosystem. The constraints that shaped every decision:

- **Local-first** — no external APIs at runtime; everything runs on the developer's machine via Docker Compose
- **Demonstrable patterns** — the architecture is intentionally over-engineered relative to the problem size, in order to showcase patterns that matter at scale: event-driven processing, domain-driven design, distributed observability
- **Spring-native** — the entire stack stays within the Spring ecosystem to demonstrate deep familiarity with it
- **Single developer** — no multi-team coordination requirements; decisions that would otherwise require org-level governance (e.g. database-per-service) were made with that context in mind

---

## System Architecture

```
                         ┌─────────────────────────────────────────┐
                         │           Docker Host Network            │
                         │                                          │
  Browser ──────────────►│  api-gateway :8080                       │
                         │  ┌────────────────────────────────────┐  │
                         │  │ Spring Security (JWT filter)       │  │
                         │  │ Bucket4j (rate limit filter)       │  │
                         │  │ Spring Cloud Gateway MVC (router)  │  │
                         │  └──────────┬─────────────────────────┘  │
                         │             │ Eureka lb://                │
                         │     ┌───────┴────────┐                   │
                         │     ▼                ▼                   │
                         │  document-service  query-service         │
                         │     :8081          :8083                 │
                         │     │                │                   │
                         │     │ AMQP           │ HTTP              │
                         │     ▼                │                   │
                         │  RabbitMQ :5672      │                   │
                         │     │                │                   │
                         │     ▼                ▼                   │
                         │  embedding-service  Ollama :11434        │
                         │     :8082           (LLaMA + nomic)      │
                         │     │                                    │
                         │     ▼                                    │
                         │  PostgreSQL+pgvector :5432               │
                         │                                          │
                         │  ── Observability ──────────────────── ──│
                         │  Prometheus :9090  Loki :3100            │
                         │  Tempo :4317/:3200 Grafana :3000         │
                         │  Eureka :8761                            │
                         └─────────────────────────────────────────┘

  All services register with Eureka on startup.
  api-gateway resolves lb://document-service and lb://query-service dynamically.
  All services emit OTel traces to Tempo and structured logs to Loki.
  Prometheus scrapes /actuator/prometheus from all four services.
```

---

## Messaging Architecture

### RabbitMQ Topology

All events flow through a single **topic exchange**: `docquery.events`.

```
                         docquery.events (Topic Exchange)
                                │
              ┌─────────────────┼──────────────────────┐
              │                 │                       │
    routing: document.parsed    │             routing: document.indexed
              │                 │                       │
              ▼                 │                       ▼
    ┌──────────────────┐        │             ┌──────────────────────┐
    │  document.parsed │        │             │   document.indexed   │
    │  (main queue)    │        │             │   (main queue)       │
    └────────┬─────────┘        │             └──────────────────────┘
             │ on reject        │
             ▼                  │ routing: document.indexing.started
    ┌──────────────────┐        │
    │ document.parsed  │        ▼
    │   .retry         │  ┌─────────────────────────┐
    │  TTL: 30s        │  │ document.indexing.started│
    └────────┬─────────┘  │ (main queue)             │
             │ on expire  └─────────────────────────┘
             ▼
    ┌──────────────────┐
    │  document.parsed │
    │    .dlq          │  ← final resting place after 3 failures
    └──────────────────┘
```

### Event Flow

**1. Document ingestion:**
```
document-service ──► DocumentParsedEvent ──► document.parsed queue
                                                     │
                                         embedding-service (consumer)
                                                     │
                                         ├─► DocumentIndexingStartedEvent ──► document.indexing.started
                                         └─► [generate embeddings, persist]
                                                     │
                                         ├─► DocumentIndexedEvent ──► document.indexed
                                         └─► document-service (consumer, updates status to INDEXED)
```

**2. Retry strategy:**
- Consumer uses **manual acknowledgment** — message is not removed from the queue until explicitly acked
- On processing failure: message is `nack`'d with `requeue=false`, dead-lettered to `document.parsed.retry`
- Retry queue has **30s TTL** — message expires and is dead-lettered back to the main queue (`document.parsed`)
- The consumer tracks attempt count via the `x-death` header injected by RabbitMQ on each dead-letter cycle
- After **3 attempts**: the consumer **explicitly publishes** to `docquery.events` with routing key `document.parsed.dlq` (bound to the DLQ queue), then acks the original message — document status set to `FAILED`. The DLQ routing is a consumer-side decision, not a broker-side TTL expiry.

**Why manual ack?** Auto-ack removes the message as soon as it is delivered, before processing completes. If the service crashes mid-processing, the message is silently lost. Manual ack guarantees at-least-once delivery.

---

## Domain Design

### Document State Machine

The document lifecycle is modeled as a finite state machine using the **State pattern**. Each state is a separate class that implements only the transitions valid from that state.

```
          ┌──────────────┐
  upload  │   UPLOADED   │
 ────────►│              │
          └──────┬───────┘
                 │ startParsing()
                 ▼
          ┌──────────────┐
          │   PARSING    │
          └──────┬───────┘
                 │ finishParsing()
                 ▼
          ┌──────────────┐
          │    PARSED    │
          └──────┬───────┘
                 │ startIndexing()
                 ▼
          ┌──────────────┐
          │   INDEXING   │
          └──────┬───────┘
                 │ finishIndexing()
                 ▼
          ┌──────────────┐
          │   INDEXED    │  ← terminal
          └──────────────┘

  Any state ──► fail() ──► FAILED (terminal)
```

Any invalid transition (e.g. calling `startIndexing()` on an `UPLOADED` document) throws `IllegalStateException` at the domain layer — the invalid state is unreachable by construction, not by validation.

### Clean Architecture Layers

Each service follows a strict layered structure:

```
domain/              Pure business logic — no Spring, no JPA, no framework
  ├── Document.java         Domain entity (plain Java, no annotations)
  ├── DocumentStatus.java   Enum of valid states
  └── state/                State pattern implementations

application/         Orchestration — depends only on domain and port interfaces
  └── DocumentProcessingService.java

infrastructure/      Framework-specific adapters — implements ports
  ├── api/                  REST controllers
  ├── persistence/          JPA entities and repositories
  ├── messaging/            RabbitMQ publishers and consumers
  └── parsing/              Apache Tika adapter
```

**Domain entity vs JPA entity:** `Document.java` is a plain Java object with no JPA annotations. `DocumentEntity.java` maps to the database. This separation means domain logic is testable without a database and without Spring — unit tests run in milliseconds.

---

## RAG Pipeline — Technical Deep Dive

### What is RAG?

Retrieval-Augmented Generation solves a core LLM limitation: models don't know about your private documents, and their training data has a knowledge cutoff. RAG augments the LLM's prompt with retrieved context from your own data at query time, grounding answers in the actual document content rather than hallucinated plausibility.

### Two Models, Two Jobs

| Model | Role | Why |
|---|---|---|
| `nomic-embed-text v1.5` | Encode text → 768-dimensional vector | Optimized for semantic similarity — fast, deterministic, no creativity needed |
| `LLaMA 3.2 3B Q4` | Generate natural language answer | Needs creativity, instruction following, language fluency |

Using the embedding model for generation (or vice versa) would produce worse results. They are optimized for fundamentally different tasks.

### Chunking Strategy

- **Method:** Word-based sliding window (not character-based)
- **Chunk size:** 500 words (~600–700 tokens)
- **Overlap:** 50 words between consecutive chunks

**Why words, not characters?** Token boundaries in transformer models align with words, not characters. Character-based chunking can split a word mid-token, corrupting the semantic unit fed into the embedding model.

**Why 500 words?** `nomic-embed-text` supports up to 8192 tokens. 500 words (~650 tokens) leaves room for the model's special tokens and stays well within limits. Larger chunks carry more context per vector but dilute similarity scores; smaller chunks are more precise but lose surrounding context.

**Why overlap?** A sentence that spans a chunk boundary would be split — with no overlap, queries about that sentence would find neither chunk confidently. 50-word overlap ensures boundary content is represented in at least one complete chunk.

### pgvector: HNSW Index

```sql
CREATE INDEX idx_document_chunks_embedding
    ON document_chunks
    USING hnsw (embedding vector_cosine_ops);
```

- **HNSW** (Hierarchical Navigable Small World): approximate nearest neighbor algorithm. Builds a multi-layer graph for fast traversal at query time. Trades a small accuracy loss for orders-of-magnitude speed improvement over exact search.
- **`vector_cosine_ops`**: cosine similarity, which measures the angle between vectors regardless of magnitude. Appropriate for normalized embeddings where directional similarity (semantic meaning) matters more than raw magnitude.

### Prompt Engineering

The query-service assembles a structured prompt before calling the LLM:

```
[SYSTEM]
You are a helpful assistant. Answer questions based ONLY on the provided context.
If the answer is not in the context, say you don't know.
Always respond in the same language as the user's question.

[CONTEXT]
<top-K retrieved chunks from pgvector>

[HISTORY]
<prior turns in the conversation>

[USER]
<current question>
```

The system prompt enforces grounding (no hallucinations beyond the context), handles multilingual queries gracefully, and keeps prior conversation turns for context continuity.

### SSE Streaming

The query-service returns a `SseEmitter` that writes each token as it arrives from the Ollama streaming API. This allows the browser to render the response progressively instead of waiting for the full completion.

**Why `SseEmitter` instead of WebFlux?** Introducing WebFlux (Project Reactor) for a single endpoint would require switching the entire query-service to a reactive model — a significant complexity tax. Instead, `SseEmitter` is used with a `newVirtualThreadPerTaskExecutor`: the Tomcat request thread is released immediately, and the blocking Ollama stream runs on a virtual thread. This achieves the same concurrency benefit with no reactive paradigm shift. See ADR-04.

---

## Security Architecture

### JWT Authentication Flow

```
  POST /auth/login
        │
        ▼
  api-gateway (JwtService)
  ├── Validate credentials against users table
  ├── Generate JWT:
  │     sub: user@email.com
  │     userId: <UUID>
  │     exp: now + 24h
  └── Return token to client

  Subsequent requests:
  Authorization: Bearer <token>
        │
        ▼
  JwtAuthenticationFilter (api-gateway)
  ├── Validate signature + expiration
  ├── Extract userId from claims
  └── Inject X-Api-Gateway-User-Id: <UUID> header

        │ lb:// routing
        ▼
  downstream service (document-service / query-service)
  └── @CurrentUserId reads X-Api-Gateway-User-Id header
      └── Used for ownership validation
```

### Why userId as a JWT Claim?

The standard pattern stores only the subject (`sub: email`) in the JWT, then performs a database lookup per request to resolve the user's UUID. This adds a DB roundtrip to every authenticated request.

By embedding `userId` directly as a claim, downstream services receive the UUID in a header — zero additional queries. The userId is signed into the token at login time and cannot be tampered with without invalidating the signature.

### Header Trust Model

The `X-Api-Gateway-User-Id` header is injected exclusively by the api-gateway after signature verification. Internal services trust this header because:

1. No service is directly reachable from outside (all traffic flows through the gateway)
2. The header is set by the gateway only after a valid JWT is verified
3. Any request that bypasses the gateway cannot set this header through a legitimate path

**Limitation:** This trust relies on Docker network isolation. In a production environment, mTLS between services would replace this assumption with cryptographic proof.

### Ownership Validation

Every endpoint that accesses a document (fetch, delete, chat, history) validates that the `userId` in the request matches `document.ownerId` in the database. This check happens at the application layer, not just at the routing layer.

---

## Architectural Decision Records

---

### ADR-01: RabbitMQ over Kafka

**Status:** Accepted

**Context:** The ingestion pipeline requires async decoupling between document-service (producer) and embedding-service (consumer). A message broker is needed.

**Decision:** Use RabbitMQ 3.13.

**Rationale:** The workload is moderate volume with low throughput requirements (one message per uploaded document). RabbitMQ's native support for dead-letter exchanges, per-queue TTL, and message routing via exchanges maps directly to the retry/DLQ pattern needed here. The management UI at port 15672 also provides immediate observability for a local development setup.

**Trade-offs:** RabbitMQ does not provide the log-based persistence and replay semantics of Kafka. Once a message is consumed and acked, it is gone.

**Alternatives considered:** Kafka was rejected because its operational overhead (ZooKeeper or KRaft, partition management, consumer group coordination) is disproportionate to the throughput of this workload. Kafka's strength — ordered, replayable, high-throughput event logs — is not a requirement here.

---

### ADR-02: pgvector over a dedicated vector database

**Status:** Accepted

**Context:** Embeddings must be stored and searched by cosine similarity. This requires a vector-capable store.

**Decision:** Use the `pgvector` extension on PostgreSQL 16.

**Rationale:** Document metadata, user data, and vector embeddings all live in the same PostgreSQL instance. This eliminates an entire service from the infrastructure (no separate Qdrant, Weaviate, or Pinecone process), removes a network hop from every query, and keeps transactions atomic across relational and vector data. The HNSW index provides sub-millisecond approximate nearest-neighbor search at the scale of this project.

**Trade-offs:** Dedicated vector databases provide additional features (payload filtering, named collections, quantization, sharding) that pgvector lacks or implements partially. At tens of millions of vectors, a dedicated store would outperform pgvector.

**Alternatives considered:** Qdrant and Weaviate were evaluated. Both are excellent but add operational complexity without benefit at this scale. Pinecone was excluded immediately — it is a cloud service, violating the local-first constraint.

---

### ADR-03: Separate embedding model from chat model

**Status:** Accepted

**Context:** The RAG pipeline requires both semantic encoding (query → vector) and natural language generation (context + query → answer).

**Decision:** Use two distinct Ollama models: `nomic-embed-text v1.5` for embeddings and `LLaMA 3.2 3B Q4` for generation.

**Rationale:** These tasks have fundamentally different optimization targets. Embedding models are trained to produce geometrically meaningful vector representations where semantically similar texts are close together. Generative models are trained to predict the next token fluently. Using a generative model for embeddings produces lower-quality vectors; using an embedding model for generation produces incoherent text.

**Trade-offs:** Requires pulling two models (~2 GB total). Two model processes in Ollama consume more VRAM than one.

**Alternatives considered:** Some generative models can produce embeddings via their hidden states. This was rejected as a worse embedding quality for the same (or more) resource cost.

---

### ADR-04: Virtual Thread executor for SSE streaming over Spring WebFlux

**Status:** Accepted

**Context:** The query-service streams LLM responses token-by-token via `SseEmitter`. The underlying Ollama call is blocking: it holds a thread open for the entire duration of the stream. With a standard Tomcat thread pool, each concurrent streaming request consumes one platform thread for its full duration — a problem at any meaningful concurrency level. Spring WebFlux (Project Reactor) would handle this non-blockingly but at significant complexity cost.

**Decision:** Use `Executors.newVirtualThreadPerTaskExecutor()` in `RagService` to run the blocking Ollama stream on a Java 21 Virtual Thread. The Tomcat thread that accepted the HTTP request returns to the pool immediately after handing off to the virtual thread executor.

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> { /* blocking Ollama stream */ });
}
```

**Rationale:** Virtual Threads are scheduled by the JVM onto carrier threads when blocking occurs. A Virtual Thread blocked on I/O is parked and the carrier thread is freed — achieving the same concurrency benefit as WebFlux without the reactive programming model. This is a targeted application of Virtual Threads at the one point where blocking I/O is held for a long duration (streaming), not a platform-wide setting.

**Trade-offs:** The rest of the services run standard Tomcat thread pools. Virtual Threads are not globally enabled (`spring.threads.virtual.enabled` is not set). This is a deliberate scope decision — the SSE endpoint is the only place where long-lived blocking I/O per request is a concern.

**Alternatives considered:** Spring WebFlux with `Flux<ChatResponse>` from Spring AI was evaluated. It solves the same problem but requires the entire query-service to adopt the reactive model (`Mono<T>`, `Flux<T>`, `WebClient`, reactive repositories), making debugging harder and integration testing significantly more complex. The tradeoff is not justified when a single `newVirtualThreadPerTaskExecutor()` achieves the same result with no paradigm shift.

---

### ADR-05: Spring AI as LLM abstraction layer

**Status:** Accepted

**Context:** The system needs to call both a chat model and an embedding model. These calls could be made directly via Ollama's HTTP API.

**Decision:** Use Spring AI 1.0 as an abstraction layer over Ollama.

**Rationale:** Spring AI provides a vendor-agnostic `ChatModel` and `EmbeddingModel` interface. Switching from Ollama to OpenAI (or any other provider) requires a dependency change and configuration update — no code changes in the business logic. It also handles request/response mapping, streaming via `Flux<ChatResponse>`, and Spring Boot autoconfiguration.

**Trade-offs:** Spring AI is still at 1.0 and the API surface has changed significantly between milestones. Dependency on a young library carries upgrade risk.

**Alternatives considered:** Direct Ollama REST API calls via `RestClient`. This was rejected because it would couple the application code to Ollama's specific request/response format, making future model provider changes a refactoring exercise rather than a configuration change.

---

### ADR-06: 202 Accepted + polling over WebSocket for document ingestion

**Status:** Accepted

**Context:** Document processing (parse → embed → index) is asynchronous and takes variable time depending on document size and hardware. The client needs to know when the document is ready.

**Decision:** `POST /documents` returns `202 Accepted` immediately. The client polls `GET /documents/{id}` to observe the status field progress from `UPLOADED` to `INDEXED`.

**Rationale:** WebSocket would require maintaining a persistent bidirectional connection per upload, adding server-side state management and client-side reconnection logic. The polling model is simpler, stateless, and adequate for a process that takes seconds to minutes. The document status field (`UPLOADED → PARSING → PARSED → INDEXING → INDEXED`) gives the client meaningful progress information on each poll.

**Trade-offs:** Polling introduces latency between the actual state transition and the client's awareness of it. With a reasonable poll interval (e.g. 2s), this is imperceptible.

**Alternatives considered:** WebSocket and SSE push for upload status. Both were rejected for adding complexity without meaningful user experience benefit in a single-user local deployment.

---

### ADR-07: Domain State Machine for document lifecycle

**Status:** Accepted

**Context:** Documents transition through 6 states. Processing logic in several services needs to update this state. Without explicit enforcement, it is possible to produce invalid transitions (e.g. moving from `UPLOADED` to `INDEXED` directly) through bugs.

**Decision:** Implement the lifecycle using the State pattern. Each state is a separate class. Each class only implements the methods (transitions) that are valid from that state; all others throw `IllegalStateException`.

**Rationale:** Making invalid transitions impossible at the type level (rather than guarding with conditionals in the application layer) means the constraint cannot be bypassed by accident. It also makes the valid transitions explicit and self-documenting — each state class declares exactly what can happen next.

**Trade-offs:** More classes than a simple enum-with-switch approach. The overhead is low and the benefit (compile-time enforced constraints) is significant.

**Alternatives considered:** A switch statement on a `DocumentStatus` enum was the obvious simpler approach. Rejected because it disperses transition logic across multiple places and requires every caller to handle every possible state.

---

### ADR-08: Two-event indexing flow (IndexingStarted + Indexed)

**Status:** Accepted

**Context:** When the embedding-service completes embedding a document, the document-service needs to update the document status to `INDEXED`. Two approaches were considered: direct REST call back to document-service, or publishing a `DocumentIndexedEvent`.

**Decision:** Use two events: `DocumentIndexingStartedEvent` (published by embedding-service when it starts) and `DocumentIndexedEvent` (published when it finishes). The document-service consumes both to advance the state machine.

**Rationale:** A REST callback from embedding-service to document-service would introduce a direct runtime dependency between the two services in the wrong direction (embedding-service would need to discover document-service). Publishing events maintains the unidirectional dependency: document-service owns the document lifecycle and observes events from downstream processors.

**Trade-offs:** More events to manage. The document-service must listen to two queues for document status updates.

**Alternatives considered:** Direct REST callback was rejected for introducing tight coupling. A single `DocumentIndexedEvent` (without `IndexingStarted`) was also considered but rejected because it prevents the status from showing `INDEXING` during the embedding process — which would leave users seeing `PARSED` with no progress indicator.

---

### ADR-09: JWT userId claim to avoid database roundtrip

**Status:** Accepted

**Context:** Downstream services (document-service, query-service) need the authenticated user's UUID to perform ownership validation. The standard approach stores only `sub: email` in the JWT and resolves the UUID via a database lookup.

**Decision:** Embed `userId: <UUID>` as an additional claim in the JWT at login time. The gateway extracts it and injects it as the `X-Api-Gateway-User-Id` header.

**Rationale:** The user's UUID is stable — it never changes after account creation. Embedding it in the token is safe. This eliminates one database query per authenticated request in every downstream service, which matters at scale.

**Trade-offs:** If a user account is deleted, a JWT issued before deletion will still carry a valid userId until expiry (24h by default). Mitigated by short expiry windows and token revocation lists if needed.

**Alternatives considered:** User info endpoint (OAuth2 introspection pattern) — every service calls the gateway to resolve the token. This solves the stale token problem but adds a synchronous network hop and makes the gateway a bottleneck. Rejected for this scale.

---

### ADR-10: Mono-repo Maven multi-module structure

**Status:** Accepted

**Context:** The project consists of 6 related services plus a shared library. They could be in separate repositories or in a single mono-repo.

**Decision:** Single Git repository with a Maven multi-module structure and a parent `pom.xml` as BOM.

**Rationale:** At this stage (one developer, tight coupling between modules), a mono-repo dramatically simplifies development: a single `mvn test` runs all tests, shared dependency versions are declared once in the parent BOM, and cross-service refactors are atomic commits. The parent BOM ensures consistent versions of Spring Boot, Spring Cloud, Spring AI, and Testcontainers across all modules.

**Trade-offs:** A true microservices deployment pipeline (separate CI/CD per service, independent versioning) is harder with a mono-repo. Acceptable for a portfolio project.

**Alternatives considered:** Separate repositories per service. Rejected because it multiplies the overhead of dependency management and local development setup without providing meaningful benefit at this scale.

---

### ADR-11: Domain model separated from JPA persistence model

**Status:** Accepted

**Context:** Java frameworks (JPA, Jackson) encourage annotating domain classes directly. This is fast to write but couples the domain model to infrastructure concerns.

**Decision:** Maintain a strict separation between domain entities (plain Java, no annotations) and JPA entities (annotated with `@Entity`, `@Column`, etc). Explicit mapping between the two is performed in the infrastructure layer.

**Rationale:** The domain model expresses business concepts and rules. Infrastructure annotations (column names, fetch strategies, cascade types) are persistence concerns that should not pollute the domain. A plain Java domain object is testable without a database context and without Spring. Domain rules can be verified with pure unit tests in milliseconds.

**Trade-offs:** More classes and explicit mapping code. The maintenance overhead is justified by the testability and clarity benefits.

**Alternatives considered:** Annotating domain entities directly with `@Entity`. Rejected because it violates the dependency rule (domain layer depending on infrastructure framework) and makes domain logic harder to test in isolation.

---

### ADR-12: Dynamic route registry in API Gateway

**Status:** Accepted

**Context:** The api-gateway must route requests to document-service and query-service. Routes could be hardcoded (static URLs) or resolved dynamically via service discovery.

**Decision:** Use Spring Cloud Gateway MVC with `lb://service-name` URIs resolved via Eureka. All services register themselves with Eureka on startup.

**Rationale:** Hardcoded URLs (`http://localhost:8081`) break the moment a service moves, scales, or restarts on a different port. Eureka-based discovery means the gateway never needs to know where a service is running — only what its name is. This is the correct baseline for any system that might eventually scale horizontally.

**Trade-offs:** Adds the Eureka server as an availability dependency. If Eureka is down and the gateway's local cache expires, routing fails.

**Alternatives considered:** Static URL configuration. Rejected because it does not demonstrate service discovery patterns and breaks in any deployment beyond a single fixed host.

---

### ADR-13: Single shared database over database-per-service

**Status:** Accepted

**Context:** Microservices purists advocate for database-per-service to ensure full isolation between services: independent schema evolution, independent scaling, no shared-state coupling. This project deliberately uses a single PostgreSQL instance shared across all services.

**Decision:** Use a single PostgreSQL instance. Each service owns its own tables by convention and does not query another service's tables directly.

**Separation by convention:**
- `api-gateway` → `users`
- `document-service` → `documents`, `document_chunks`
- Services communicate via events (RabbitMQ) or HTTP, never via cross-service SQL joins

**Rationale:** Database-per-service is the correct pattern for teams that need to evolve, deploy, and scale services independently. The operational cost — managing multiple database instances, cross-service data consistency via sagas or event sourcing, no foreign keys across services — is only justified when the team structure or scale demands it. This project has one developer and a controlled scope. Logical isolation (separate tables, no cross-service queries) provides meaningful separation without the operational overhead.

**Trade-offs:** Schema coupling is real. A migration to `documents` in document-service must not break anything the embedding-service or query-service depends on. In practice, this is managed by convention and code review.

**Migration path to database-per-service:** If this project were to grow to multiple teams or required independent scaling:
1. Each service gets its own PostgreSQL instance (or schema with separate credentials)
2. Cross-service data synchronization moves to event sourcing (publish state changes as events, consumers maintain their own read models)
3. Multi-step operations spanning services (e.g. "delete document and all its embeddings") move to the Saga pattern with compensating transactions

**Alternatives considered:** Database-per-service from day one. Rejected because the complexity is disproportionate to the project's current scope and team size.

---

## Observability Strategy

### Trace Propagation

All services are instrumented with OpenTelemetry via `micrometer-tracing-bridge-otel`. The W3C TraceContext propagation standard is used — each HTTP request carries `traceparent` and `tracestate` headers, allowing Tempo to reconstruct the full distributed trace across service hops.

```
Browser ──► api-gateway ──► document-service ──► [RabbitMQ] ──► embedding-service
   │              │                │                                     │
   └──────────────┴────────────────┴─────────────────────────────────────┘
                         Single traceId across all hops
```

### Log-Trace Correlation

Every log line includes `traceId` and `spanId` via MDC injection from the OTel bridge:

```
11:42:03.512  INFO [document-service,4f3a1c8b9d2e7f6a,8c1d4e2f3a9b5c7d] ...
                                      └── traceId         └── spanId
```

Loki is configured with a **derived field** that matches the `traceId` regex in log lines and renders it as a clickable link to the corresponding Tempo trace. This means: from a log entry about an error, one click opens the full distributed trace of the request that caused it.

### Metrics

Micrometer emits:
- **HTTP server metrics** — request rate, error rate, latency (count/sum/max) per service and endpoint
- **JVM metrics** — heap usage by memory area, live thread count, GC pause times
- **RabbitMQ metrics** — messages published, consumed, rejected, not-acknowledged
- **Process metrics** — CPU usage per service

Three Grafana dashboards are provisioned automatically from `docker/grafana/provisioning/dashboards/`:
- **Services Overview** — cross-service request rate, latency, error rate, JVM health
- **RAG Pipeline** — document ingestion counters, RabbitMQ throughput, 429 rate
- **Logs & Traces** — Loki log volume, error rate, and live filterable log stream

---

## Known Limitations & Future Improvements

| Area | Current State | Production Improvement |
|---|---|---|
| **mTLS** | Internal services trust `X-Api-Gateway-User-Id` based on network isolation | Mutual TLS between all internal services; service mesh (Istio/Linkerd) |
| **Secret management** | JWT secret and DB credentials in `application.yml` | HashiCorp Vault or Kubernetes Secrets with external-secrets-operator |
| **Rate limit storage** | In-memory per JVM process | Redis-backed Bucket4j for multi-instance deployments |
| **Token revocation** | JWTs are valid until expiry regardless of logout | Short-lived access tokens + refresh tokens; revocation list in Redis |
| **Multi-tenancy** | Single-tenant; userId provides row-level isolation | Proper tenant isolation at the schema or database level |
| **Embedding batching** | All chunks per document in one Ollama call | Configurable batch size with backpressure for large documents |
| **Schema migrations** | Flyway runs on document-service startup | Separate migration job; migration + rollback pipeline in CI |
| **No CI/CD** | Manual build and run | GitHub Actions: build, test, Docker build, deploy |
| **No horizontal scaling** | Eureka + single instance per service | K8s HPA; distributed rate limiting; leader election for RabbitMQ consumers |
| **pgvector HNSW params** | Default HNSW parameters (m=16, ef_construction=64) | Tune for recall/speed tradeoff based on measured query latency |
