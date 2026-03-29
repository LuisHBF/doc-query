# doc-query

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![LLaMA](https://img.shields.io/badge/LLaMA-3.2%203B-blueviolet?logo=meta&logoColor=white)](https://ollama.com/library/llama3.2)
[![pgvector](https://img.shields.io/badge/pgvector-PostgreSQL%2016-4169E1?logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-FF6600?logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Status](https://img.shields.io/badge/status-work%20in%20progress-yellow)](https://github.com/LuisHBF/doc-query)

**A local-first RAG platform — upload PDFs, ask questions in natural language, get answers grounded in your documents. No external APIs. No data leaving your machine.**

> Built by [LuisHBF](https://github.com/LuisHBF)

---

## What is this?

You upload a PDF. The system extracts text, splits it into overlapping chunks, generates vector embeddings, and stores them in PostgreSQL with pgvector. When you ask a question, it embeds your query, retrieves the most relevant chunks via cosine similarity, and streams a grounded LLM response back to you in real time.

From an engineering perspective, this is a production-grade microservices system built to demonstrate patterns that matter at scale: event-driven async processing with RabbitMQ, domain-driven design with a State Machine for document lifecycle, clean hexagonal architecture, distributed tracing with OpenTelemetry, and a full observability stack — all running 100% locally via Docker Compose.

---

## Architecture at a Glance

```
 Browser / Frontend
        │
        ▼
 ┌─────────────────────────────────────────────────────────────┐
 │                    api-gateway  :8080                       │
 │   JWT Auth  │  Rate Limiting (Bucket4j)  │  SCG MVC Router  │
 └──────────────────────┬──────────────────────────────────────┘
                        │  lb://  (Eureka)
          ┌─────────────┼──────────────────┐
          ▼                                ▼
 ┌─────────────────┐             ┌──────────────────┐
 │ document-service│             │  query-service   │
 │     :8081       │             │     :8083        │
 │                 │             │                  │
 │ Upload → Parse  │             │ Embed question   │
 │ Chunk → Publish │             │ pgvector search  │
 └────────┬────────┘             │ LLM → SSE stream │
          │ RabbitMQ             └──────────────────┘
          │ document.parsed
          ▼
 ┌──────────────────────┐
 │  embedding-service   │
 │       :8082          │
 │                      │
 │  Consume event       │
 │  nomic-embed-text    │
 │  Store → pgvector    │
 └──────────────────────┘

 ─────────────────────────────────────────────────────────────
  Infrastructure (Docker Compose)
  PostgreSQL+pgvector :5432  │  RabbitMQ :5672/:15672
  Ollama :11434              │  Eureka :8761
  Prometheus :9090           │  Loki :3100
  Tempo :4317/:3200          │  Grafana :3000
```

---

## Tech Stack

| Category | Technology | Purpose |
|---|---|---|
| Language | Java 21 | Virtual Threads for high-concurrency without reactive complexity |
| Framework | Spring Boot 3.4.1 | Application foundation across all services |
| Build | Maven multi-module | Mono-repo with shared parent BOM |
| AI Abstraction | Spring AI 1.0 | Vendor-agnostic interface for LLM and embedding models |
| LLM Runtime | Ollama | Local model serving — no cloud dependency |
| Chat Model | LLaMA 3.2 3B Q4 | Runs on 4 GB VRAM — practical for local hardware |
| Embedding Model | nomic-embed-text v1.5 | 768-dimensional embeddings, 8192-token context |
| Database | PostgreSQL 16 + pgvector | Relational data + HNSW vector index in one engine |
| Migrations | Flyway | Versioned schema evolution |
| Message Broker | RabbitMQ 3.13 | Async decoupling of ingestion pipeline |
| Service Discovery | Eureka (Spring Cloud) | Dynamic routing — no hardcoded service URLs |
| API Gateway | Spring Cloud Gateway MVC | JWT auth, rate limiting, load-balanced routing |
| Rate Limiting | Bucket4j | Token bucket per user/IP, in-memory |
| PDF Parsing | Apache Tika | Format-agnostic text extraction |
| Tracing | OpenTelemetry + Tempo | Distributed trace propagation across services |
| Metrics | Micrometer + Prometheus | HTTP, JVM and business metrics |
| Logging | Loki + loki4j | Structured logs correlated with traces via traceId |
| Dashboards | Grafana 10.4 | 3 auto-provisioned dashboards |
| Infra | Docker Compose | Full stack in a single command |

---

## Key Engineering Highlights

- **Domain State Machine** — Document lifecycle (`UPLOADED → PARSING → PARSED → INDEXING → INDEXED / FAILED`) enforced at the domain layer with the State pattern. Invalid transitions throw `IllegalStateException` — impossible to reach an inconsistent state.
- **Fully local RAG pipeline** — Two Ollama models work in tandem: `nomic-embed-text` for embeddings, `LLaMA 3.2 3B` for generation. HNSW index with cosine similarity in pgvector. Zero external API calls at runtime.
- **Event-driven ingestion with resilient messaging** — Upload triggers an async pipeline via RabbitMQ. Each consumer uses manual acknowledgment, a retry queue with 30s TTL, and a DLQ after 3 failed attempts — no silent message loss.
- **Hexagonal architecture per service** — Domain, application and infrastructure layers are strictly separated. Domain entities have no JPA annotations; persistence models are mapped explicitly. The domain is testable without Spring.
- **JWT with userId claim** — The authenticated user's UUID is embedded directly in the JWT payload. Downstream services extract it from the `X-Api-Gateway-User-Id` header — no database roundtrip per request to validate ownership.
- **SSE streaming via Virtual Threads** — LLM responses stream token-by-token via `SseEmitter`. The query-service spawns a `newVirtualThreadPerTaskExecutor` for each streaming request, so the Tomcat thread is released immediately while the blocking Ollama call runs on a virtual thread.
- **Full observability stack** — OTel traces propagated across all services. `traceId` injected in every log line. Loki → Tempo clickable correlation in Grafana. Three pre-built dashboards provisioned at container startup.
- **Rate limiting at the gateway** — Bucket4j token buckets per authenticated user (or IP). Two tiers: 60 req/min general, 10 req/min for chat. 429 responses include `Retry-After` — never forwards overload downstream.

---

## Quick Start

```bash
# 1. Clone
git clone https://github.com/LuisHBF/doc-query.git && cd doc-query

# 2. Start infrastructure
cd docker && docker compose up -d

# 3. Pull AI models (one-time, ~2 GB)
docker exec -it docquery-ollama ollama pull llama3.2:3b
docker exec -it docquery-ollama ollama pull nomic-embed-text

# 4. Start services (each in a separate terminal)
mvn -pl eureka-server spring-boot:run   # start this one first
mvn -pl api-gateway spring-boot:run
mvn -pl document-service spring-boot:run
mvn -pl embedding-service spring-boot:run
mvn -pl query-service spring-boot:run

# 5. Open frontend/index.html in your browser
```

> **Requirements:** Java 21, Maven 3.9+, Docker Desktop (6 GB RAM minimum). GPU with 8+ GB VRAM recommended for fast inference.

---

## Service Overview

| Service | Port | Responsibility | Key Technologies |
|---|---|---|---|
| **eureka-server** | 8761 | Service registry and health dashboard | Spring Cloud Netflix Eureka |
| **api-gateway** | 8080 | Auth, rate limiting, dynamic routing | Spring Security, JWT, Bucket4j, SCG MVC |
| **document-service** | 8081 | Upload, parse, chunk, publish events | Apache Tika, RabbitMQ, Flyway, JPA |
| **embedding-service** | 8082 | Consume events, generate and store embeddings | Spring AI, RabbitMQ, pgvector |
| **query-service** | 8083 | Semantic search, prompt assembly, SSE streaming | Spring AI, pgvector, SseEmitter |
| **commons** | — | Shared DTOs and web utilities | — |

---

## RAG Pipeline

### Ingestion Flow

```
POST /documents (multipart)
        │
        ▼
  document-service
  ├── Extract text (Apache Tika)
  ├── Split into chunks (500 words, 50-word overlap)
  ├── Persist chunks to PostgreSQL
  ├── Status: UPLOADED → PARSING → PARSED
  └── Publish DocumentParsedEvent → RabbitMQ
              │
              ▼
      embedding-service
      ├── Consume event (manual ack)
      ├── Load all chunks from DB
      ├── Generate embeddings (nomic-embed-text via Ollama)
      ├── Store embedding vectors in pgvector
      └── Status: INDEXING → INDEXED
                  │
                  ▼
       Document ready for querying
```

### Query Flow

```
POST /documents/{id}/chat  {"message": "What is..."}
        │
        ▼
  query-service
  ├── Embed user question (nomic-embed-text)
  ├── Cosine similarity search (pgvector HNSW)
  ├── Retrieve top-K relevant chunks
  ├── Load conversation history
  ├── Assemble prompt (system + context + history + question)
  └── Stream LLM response token-by-token (SSE)
              │
              ▼
     SseEmitter → browser (real-time streaming)
```

---

## Work in Progress

| ✅ Implemented | 🚧 Planned |
|---|---|
| Full RAG pipeline (ingest + query) | k6 load testing scripts |
| Domain State Machine for document lifecycle | mTLS between internal services |
| Event-driven ingestion with DLQ and retry | Secret management (Vault / env injection) |
| JWT authentication at the gateway | Multi-tenancy support |
| Rate limiting (Bucket4j, 2 tiers) | Horizontal scaling with Redis-backed rate limit |
| SSE streaming for LLM responses | CI/CD pipeline |
| Conversation history per document | Kubernetes manifests |
| Distributed tracing (OTel + Tempo) | |
| Structured logs (Loki + loki4j) | |
| 3 auto-provisioned Grafana dashboards | |
| 236 tests across all modules | |

---

## Want to go deeper?

[**ARCHITECTURAL_DECISIONS.md**](ARCHITECTURAL_DECISIONS.md) covers the full technical depth: system architecture diagrams, RabbitMQ topology, the domain state machine design, a RAG pipeline deep dive (chunking strategy, pgvector HNSW index, prompt engineering), security architecture, and 13 Architectural Decision Records (ADRs) with context, rationale and trade-offs for every major design choice.

---

## Author

Built by [LuisHBF](https://github.com/LuisHBF)
