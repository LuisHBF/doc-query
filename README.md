# doc-query

```
  _            _
 | |          | |
 | | ___   ___| |    __ _ _   _  ___ _ __ _   _
 | |/ _ \ / __| |   / _` | | | |/ _ \ '__| | | |
 | | (_) | (__| |__| (_| | |_| |  __/ |  | |_| |
 |_|\___/ \___|_____\__, |\__,_|\___|_|   \__, |
  doc-query          __/ |                 __/ |
                    |___/                 |___/
```

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Status](https://img.shields.io/badge/status-work%20in%20progress-yellow)](https://github.com/LuisHBF/doc-query)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

**A local-first document intelligence platform powered by RAG, Spring Boot, and Ollama.**

Upload your PDFs. Ask questions in natural language. Get answers grounded in your documents — entirely on your own machine, with no external APIs and no data leaving your environment.

Built by [LuisHBF](https://github.com/LuisHBF) as a software architecture portfolio project.

---

> **⚠️ Work in Progress**
>
> The core RAG pipeline, JWT authentication, async document processing, SSE streaming, observability stack, and frontend are implemented and functional. Integration tests, Grafana dashboards, and load tests are still being developed. See the [Work in Progress](#work-in-progress) section for the full breakdown.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Asynchronous Messaging](#asynchronous-messaging)
- [RAG Pipeline](#rag-pipeline)
- [Security](#security)
- [Tech Stack](#tech-stack)
- [Key Technical Decisions](#key-technical-decisions)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Observability](#observability)
- [Project Structure](#project-structure)
- [Work in Progress](#work-in-progress)
- [License](#license)
- [Author](#author)

---

## Overview

doc-query is a Retrieval-Augmented Generation (RAG) platform built with a microservices architecture. You upload a PDF, the system processes it asynchronously — extracting text, splitting it into chunks, and generating vector embeddings — and then lets you have a natural language conversation with the document's content.

The key design constraint is **100% local operation**. Every component runs on your own hardware: the LLM, the embedding model, the vector database, the message broker, and the observability stack. Nothing reaches the internet after initial setup.

The project was built as a portfolio piece to demonstrate architectural thinking across several domains: clean microservices design, asynchronous event-driven processing, LLM integration, production-grade observability, and end-to-end security.

---

## Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────┐
│                     public-net                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Browser / Frontend (index.html)                 │   │
│  └──────────────────┬───────────────────────────────┘   │
│                     │ HTTP :8080                         │
│  ┌──────────────────▼───────────────────────────────┐   │
│  │  api-gateway                                     │   │
│  │  JWT validation · Rate limiting · Routing        │   │
│  └──┬────────────────────────┬─────────────────┬────┘   │
└─────┼────────────────────────┼─────────────────┼────────┘
      │           internal-net │                 │
      │ HTTP :8081             │ HTTP :8083      │
┌─────▼──────────┐    ┌────────▼──────────┐      │
│ document-      │    │ query-service     │      │
│ service        │    │                   │      │
│ Upload         │    │ RAG pipeline      │      │
│ Parsing        │    │ SSE streaming     │      │
│ Chunking       │    │ Conversation      │      │
│ Publishing     │    │ history           │      │
└────────┬───────┘    └────────┬──────────┘      │
         │                     │                  │
         │   ┌─────────────────┘                  │
         │   │                                    │
┌────────▼───▼──────────┐   ┌───────────────────▼──┐
│ RabbitMQ 3.13         │   │ PostgreSQL 16          │
│ docquery.events topic │   │ + pgvector             │
│ exchange              │   │                        │
└────────┬──────────────┘   └───────────────────────┘
         │ document.parsed                     ▲
         │ HTTP :8082                          │
┌────────▼──────────────┐                     │
│ embedding-service     │─────────────────────┘
│ Batch embeddings      │   pgvector writes
│ pgvector persistence  │
└────────┬──────────────┘
         │
┌────────▼──────────────┐
│ Ollama                │
│ LLaMA 3.2 3B Q4       │
│ nomic-embed-text v1.5 │
└───────────────────────┘
```

### Services and Responsibilities

| Service | Port | Responsibilities | Key Technologies |
|---|---|---|---|
| api-gateway | 8080 | Single external entry point. JWT authentication, rate limiting, dynamic request routing to internal services. | Spring Security, JJWT, Bucket4j, RestClient |
| document-service | 8081 | Handles file uploads, text extraction, chunking, and event publishing. Owns the document lifecycle state machine. | Apache Tika, Spring AMQP, JPA, Flyway |
| embedding-service | 8082 | Consumes parsed document events, generates vector embeddings in batch, persists to pgvector. | Spring AI, Spring AMQP, pgvector |
| query-service | 8083 | Embeds user questions, performs similarity search, assembles prompts, streams LLM responses. Manages conversation history. | Spring AI, pgvector, SSE |
| commons | — | Shared DTOs, domain events, and web utilities (ErrorResponse). Compiled as an internal Maven module. | — |

### Clean Architecture

Every service follows the same internal package structure, enforcing a strict dependency rule: outer layers depend on inner layers, never the reverse.

```
br.com.docquery.{service}/
├── domain/              # Entities, repository interfaces, domain events.
│                        # Zero dependencies on Spring or external libraries.
│
├── usecase/             # Use case interfaces (ports). Defines inputs (Commands)
│                        # and outputs (Responses) as internal records.
│
├── application/         # AppService classes implementing the use case interfaces.
│                        # Orchestrates domain logic. No HTTP, no JPA, no AMQP.
│
├── infrastructure/      # Adapters: JPA repositories, RabbitMQ publishers/consumers,
│                        # Spring AI integrations, Flyway migrations.
│
└── api/                 # HTTP controllers. Translates HTTP ↔ use case Commands/Responses.
                         # Validates input. Delegates everything else to application layer.
```

This structure means that changing the database from PostgreSQL to anything else only touches the `infrastructure` package. Changing the HTTP framework only touches `api`. The domain and use case layers are framework-free and independently testable.

### Document State Machine

The document lifecycle is modelled as an explicit state machine. Invalid transitions are rejected at the domain level.

```
                    ┌─────────┐
                    │UPLOADED │
                    └────┬────┘
                         │ parsing starts
                    ┌────▼────┐
                    │ PARSING │
                    └────┬────┘
                         │ text extracted, chunks saved
                    ┌────▼────┐
                    │ PARSED  │
                    └────┬────┘
                         │ embedding starts
                    ┌────▼────┐
                    │INDEXING │
                    └────┬────┘
                         │ all embeddings persisted
                    ┌────▼────┐
                    │ INDEXED │ ← ready for chat
                    └─────────┘

     Any state except INDEXED can transition to:
                    ┌────────┐
                    │ FAILED │
                    └────────┘
```

The query-service validates that a document is in the `INDEXED` state before accepting any chat or history request, returning `409 Conflict` otherwise.

---

## Asynchronous Messaging

### Exchange and Events

All inter-service events flow through a single RabbitMQ **topic exchange** named `docquery.events`. This allows consumers to subscribe to specific routing key patterns without coupling producers to queue names.

| Publisher | Event | Routing Key |
|---|---|---|
| document-service | DocumentParsedEvent | `document.parsed` |
| embedding-service | DocumentIndexingStartedEvent | `document.indexing.started` |
| embedding-service | DocumentIndexedEvent | `document.indexed` |

### Queue Topology

Each event has a full retry stack: a main queue, a retry queue with a 30-second TTL, and a dead-letter queue (DLQ).

| Queue | Routing Key | Consumer | DLQ | Retry TTL |
|---|---|---|---|---|
| document.parsed | document.parsed | embedding-service | document.parsed.dlq | 30s |
| document.parsed.retry | document.parsed.retry | — (TTL requeue) | — | 30s |
| document.indexed | document.indexed | document-service | document.indexed.dlq | 30s |
| document.indexing.started | document.indexing.started | document-service | document.indexing.started.dlq | 30s |

### Resilience Pattern

Every consumer uses **manual acknowledgement**. On processing failure, the message is negatively acknowledged and routed to the retry queue. After the TTL expires, RabbitMQ routes it back to the main queue via its dead-letter configuration. This cycle repeats until Spring AMQP's retry exhaustion policy sends the message to the DLQ.

A `DlqMonitorConsumer` in each service listens to the DLQ, increments a Micrometer counter tagged by queue name, logs a warning with the message body, and acknowledges. This makes DLQ depth visible in Prometheus/Grafana without messages accumulating silently.

---

## RAG Pipeline

### What is RAG?

A large language model (LLM) is trained on a fixed dataset and has no knowledge of your private documents. Retrieval-Augmented Generation solves this by fetching relevant excerpts from your documents at query time and injecting them into the prompt as context. The model then answers based on what it was just given, not on its training data.

The practical result: the LLM can accurately answer questions about any document you upload, and its answers are grounded in the actual text rather than hallucinated.

### Document Ingestion Flow

```
POST /documents (multipart/form-data)
         │
         ▼
[api-gateway]
  Validate JWT → extract userId → rate limit → forward
         │
         ▼
[document-service]
  1. Persist document record       (status: UPLOADED)
  2. Apache Tika: extract raw text
  3. Chunker: split ~500 tokens, ~50 token overlap
  4. Persist chunks to PostgreSQL  (status: PARSED)
  5. Publish DocumentParsedEvent → RabbitMQ
  6. Return 202 Accepted + { "id": "<documentId>" }
         │
         ▼
[RabbitMQ: document.parsed queue]
         │
         ▼
[embedding-service consumer]
  1. Publish DocumentIndexingStartedEvent  (status: INDEXING)
  2. Fetch all chunks for documentId
  3. nomic-embed-text via Ollama: batch embed
  4. Persist vector(768) per chunk to pgvector
  5. Publish DocumentIndexedEvent          (status: INDEXED)
```

### Query Flow

```
POST /documents/{id}/chat { "question": "What is this document about?" }
         │
         ▼
[query-service]
  1. Validate document ownership + INDEXED status
  2. Embed question with nomic-embed-text
  3. pgvector cosine similarity search → top-K chunks
  4. Load last N conversation turns from history
  5. Assemble prompt:
       SYSTEM  → answer only from the provided context
       CONTEXT → [chunk1 text][chunk2 text]...[chunkK text]
       HISTORY → [previous Q/A pairs]
       USER    → current question
  6. Stream to LLaMA 3.2 via Spring AI ChatClient
  7. SSE token stream → client
  8. Persist Q/A + retrieved chunk IDs to conversation_history
         │
         ▼
[client receives text/event-stream]
  data: The document describes
  data:  a recipe for avocado
  data:  chocolate cake...
  event: done
  data: [DONE]
```

### Why Two Models?

**nomic-embed-text v1.5** is a purpose-built embedding model trained specifically for semantic similarity tasks. It produces 768-dimensional vectors that capture semantic meaning efficiently. Using an LLM to generate embeddings produces inferior results because LLMs are optimised for next-token prediction, not similarity geometry.

Crucially, the **same model must be used for both indexing and querying**. Embeddings only make sense relative to the vector space they were generated in. Indexing with model A and querying with model B would make similarity search meaningless.

**LLaMA 3.2 3B Q4** is used exclusively for generation — reading the assembled context and producing a fluent answer. It never touches the vector search path.

### System Prompt Design

The system prompt explicitly instructs the model to answer only from the provided context and to acknowledge when an answer cannot be found. This prevents hallucination from the model's training data contaminating answers that should be grounded in the uploaded document.

---

## Security

### JWT Authentication Flow

```
1. Client → POST /auth/login → { email, password }
2. api-gateway validates credentials, issues JWT
   JWT payload: { sub: email, userId: <UUID>, exp: ... }
3. Client → any request + Authorization: Bearer <token>
4. api-gateway SecurityFilter:
   - Validates JWT signature and expiry
   - Extracts userId from claims
   - Injects X-Api-Gateway-User-Id: <UUID> header
   - Forwards to internal service
5. Internal service reads X-Api-Gateway-User-Id
   (never sees or validates the JWT itself)
```

### Responsibility Separation

Internal services — document-service, embedding-service, query-service — have no knowledge of JWT. They receive a pre-validated `userId` from the gateway via a trusted header. This keeps authentication logic in one place and prevents token validation drift across services.

The `X-Api-Gateway-User-Id` header is **safe** because the gateway unconditionally overwrites any value the client sends. A client cannot forge a different `userId` by injecting the header themselves; the gateway strips and replaces it on every forwarded request.

### Network Isolation

The Docker Compose configuration defines two networks:

- **public-net**: only the api-gateway is attached. This is the only service reachable from outside the Docker environment.
- **internal-net**: all services, including the gateway. Internal services can only receive traffic from the gateway, not directly from the host machine or any external source.

This means that even if a vulnerability existed in document-service, it would not be reachable from outside the Docker environment without first going through the gateway's authentication layer.

---

## Tech Stack

| Category | Technology | Version | Rationale |
|---|---|---|---|
| Language | Java | 21 | Virtual Threads for IO-bound concurrency without reactive complexity |
| Framework | Spring Boot | 3.3.5 | Production-grade auto-configuration, security, actuator |
| Build | Maven | 3.9.x | Mono-repo multi-module support; standard Java ecosystem tool |
| LLM abstraction | Spring AI | 1.x | Provider-agnostic ChatClient and EmbeddingModel APIs |
| LLM runtime | Ollama | latest | Local GPU inference, model management, REST API |
| Chat model | LLaMA 3.2 3B Q4 | — | Fits in 4GB VRAM, strong reasoning for its size |
| Embedding model | nomic-embed-text v1.5 | — | State-of-the-art open-source embedding model, 768 dimensions |
| Database | PostgreSQL | 16 | Reliable relational store with pgvector extension |
| Vector search | pgvector | — | HNSW cosine index; unifies metadata and vectors in one system |
| Migrations | Flyway | — | Version-controlled schema; `ddl-auto: validate` enforced |
| Message broker | RabbitMQ | 3.13 | Topic exchange, DLQ, TTL retry — native fit for task queue pattern |
| Rate limiting | Bucket4j | — | Token bucket algorithm, in-memory, zero external dependencies |
| PDF parsing | Apache Tika | — | Robust text extraction from PDF and other document formats |
| Tracing | OpenTelemetry + Tempo | — | Distributed trace propagation across service boundaries |
| Metrics | Micrometer + Prometheus | — | JVM, HTTP, custom business metrics |
| Logging | Loki + loki4j | — | Structured log aggregation without Logstash/Elasticsearch |
| Dashboards | Grafana | 10.4.0 | Unified view of logs, traces, and metrics |
| Integration tests | JUnit 5 + Testcontainers | — | Real infrastructure in tests; no mocks for DB or broker |
| Load tests | k6 | — | JavaScript-based, scriptable HTTP load testing |

---

## Key Technical Decisions

### RabbitMQ over Kafka

The processing pattern here is a task queue: one producer, one consumer, message consumed once and discarded. RabbitMQ's native DLQ, TTL, and manual ack support map directly to these requirements. Kafka would introduce KRaft/Zookeeper operational overhead, partition management, and consumer offset tracking — none of which add value for a single-consumer pipeline.

**Trade-off accepted:** no event replay. Re-indexing a document requires re-publishing the event manually. Acceptable for this use case.

### pgvector over a Dedicated Vector Database

Keeping metadata and vectors in the same PostgreSQL database means a document deletion cascades correctly to chunks and embeddings in a single transaction. There is no synchronisation problem between two separate systems. pgvector's HNSW index delivers sub-millisecond similarity search at the scale this project operates.

**Trade-off accepted:** at very high scale (hundreds of millions of vectors), a dedicated vector store like Qdrant would outperform. That scale is not a concern for a portfolio project.

### nomic-embed-text Separate from the LLM

Embedding models are trained specifically for semantic similarity: their vector spaces are well-behaved for nearest-neighbour search. LLM-generated embeddings have inferior geometry for this purpose. Keeping embedding and generation on separate models, both served by the same Ollama instance, gives the best of both with acceptable VRAM overhead (~2.3 GB total, well within the RTX 4070 Super's 12 GB).

**Trade-off accepted:** two models must be pulled and kept loaded. The same model must be used for indexing and querying — switching embedding models requires re-indexing all documents.

### Java 21 Virtual Threads over WebFlux

Document parsing and embedding are IO-bound workloads: waiting on file reads, database writes, and Ollama HTTP calls. Virtual Threads give near-reactive throughput by multiplexing thousands of suspended threads onto a small OS thread pool. The code remains imperative and easy to follow. Spring Boot 3.2+ enables this with `spring.threads.virtual.enabled: true`.

**Trade-off accepted:** Virtual Threads can pin OS threads when executing inside `synchronized` blocks in certain older libraries. Dependencies should be audited for known pinning issues.

### Spring AI as LLM Abstraction

Spring AI's `ChatClient` and `EmbeddingModel` interfaces decouple business logic from the specific model provider. Switching from Ollama to OpenAI, AWS Bedrock, or any other supported provider is a configuration change, not a code change.

**Trade-off accepted:** Spring AI 1.x is still maturing. Some advanced Ollama-specific features may require dropping to lower-level HTTP calls.

### 202 Accepted + Polling over WebSocket

Embedding a multi-page PDF can take 30–120 seconds depending on hardware. Holding an HTTP connection open for that duration is inappropriate. Returning `202 Accepted` with a document ID lets the client poll `GET /documents/{id}` every few seconds to check the status. This is simple, stateless, and requires no special infrastructure.

**Trade-off accepted:** polling introduces latency between the document becoming ready and the client discovering it. WebSocket push would be more responsive but adds connection management complexity that is not warranted in v1.

### Domain State Machine for Document Lifecycle

The document's progression through UPLOADED → PARSING → PARSED → INDEXING → INDEXED (with FAILED as a possible terminal state from any step) is explicitly modelled at the domain level. Transition logic lives in the entity, not scattered across service methods. Invalid transitions throw domain exceptions that propagate to 4xx HTTP responses at the API layer.

**Trade-off accepted:** more code than a plain status string update, but prevents a class of bugs where documents get stuck in intermediate states due to out-of-order event processing.

### Two-Event Indexing Flow (IndexingStarted + Indexed)

When the embedding-service picks up a `document.parsed` event, it first publishes `document.indexing.started` (which updates the document status to INDEXING in document-service) before beginning the embedding work. This prevents the frontend from showing `PARSED` for the entire duration of embedding. The final `document.indexed` event updates the status to `INDEXED`.

**Trade-off accepted:** an extra round-trip through the message broker. The operational clarity is worth it.

### JWT Claims with userId

The JWT token carries the `userId` as a claim alongside the `email` subject. This means the api-gateway can extract and forward the user's UUID to internal services without making a database call on every request. Internal services trust the gateway-injected header.

**Trade-off accepted:** if a user account is deleted, their JWT remains valid until expiry. Acceptable for a portfolio project; production systems would add a token revocation list.

### Clean Architecture with Strict Layer Separation

Every service enforces `domain → usecase → application → infrastructure/api` with no upward dependencies. Use case interfaces define their own Command and Response records, so the application layer has no dependency on HTTP or JPA types.

**Trade-off accepted:** more boilerplate than a straightforward layered architecture. The benefit is that each layer is independently testable and replaceable.

---

## Prerequisites

| Requirement | Minimum | Recommended |
|---|---|---|
| Java | 21 | 21 |
| Maven | 3.9.x | 3.9.x |
| Docker Desktop RAM allocation | 6 GB | 8+ GB |
| GPU | — | NVIDIA with 8+ GB VRAM |
| NVIDIA Container Toolkit | — | Required for GPU inference |

**Hardware reference (development machine):** AMD Ryzen 7 5800X, 64 GB RAM, RTX 4070 Super 12 GB VRAM. Both models (LLaMA 3.2 3B Q4 + nomic-embed-text) run entirely on GPU with ~2.3 GB VRAM.

Without a GPU, Ollama will use CPU inference. Embedding generation will be significantly slower (minutes instead of seconds for large documents), but everything will still work.

Other tools you will need:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- IntelliJ IDEA (or any IDE with Maven support)
- curl, Postman, or the included frontend for API interaction

---

## Getting Started

### Step 1 — Clone the repository

```bash
git clone https://github.com/LuisHBF/doc-query.git
cd doc-query
```

### Step 2 — Start the infrastructure

```bash
cd docker
docker compose up -d
```

This starts: PostgreSQL 16 with pgvector, RabbitMQ 3.13, Ollama, Prometheus, Loki, Tempo, and Grafana. Allow 30–60 seconds for all health checks to pass. You can verify with:

```bash
docker compose ps
```

All services should show `healthy` or `running`.

### Step 3 — Pull the LLM models

This is a one-time operation. Models are stored in a Docker volume and persist across restarts.

```bash
docker exec -it docquery-ollama ollama pull llama3.2:3b
docker exec -it docquery-ollama ollama pull nomic-embed-text
```

`llama3.2:3b` is approximately 2.0 GB. `nomic-embed-text` is approximately 274 MB. Download time depends on your internet connection.

### Step 4 — Start the Spring Boot services

Start the services in this order to ensure Flyway migrations run correctly before dependent services attempt to read the schema.

Run each from IntelliJ by opening the respective `Application.java`, or from the terminal:

```bash
# From the repo root, in separate terminals

mvn -pl document-service spring-boot:run
mvn -pl api-gateway spring-boot:run
mvn -pl embedding-service spring-boot:run
mvn -pl query-service spring-boot:run
```

Wait for each to log `Started ... Application` before proceeding. The services connect to the infrastructure running in Docker; default configuration uses `localhost` for all connections.

| Service | Port | Health endpoint |
|---|---|---|
| api-gateway | 8080 | http://localhost:8080/actuator/health |
| document-service | 8081 | http://localhost:8081/actuator/health |
| embedding-service | 8082 | http://localhost:8082/actuator/health |
| query-service | 8083 | http://localhost:8083/actuator/health |

### Step 5 — Open the frontend

Open `frontend/index.html` directly in your browser. No build step or web server is required.

### Step 6 — Try it out

1. **Register** a new account on the Register tab.
2. **Login** with your credentials. The JWT token is stored in memory for the session.
3. **Upload** a PDF using the drag-and-drop area or the file picker. Only PDFs up to 10 MB are accepted.
4. **Watch the status** update in the document list: UPLOADED → PARSING → PARSED → INDEXING → INDEXED. This typically takes 15–60 seconds depending on document size and hardware.
5. **Click the document** once it reaches INDEXED status to open the chat panel.
6. **Ask a question** about the document's content. The answer streams back token by token.

---

## API Reference

All endpoints below are accessed through the api-gateway at `http://localhost:8080`. Endpoints marked as **Auth required** expect an `Authorization: Bearer <token>` header.

### Auth

| Method | Path | Auth | Description | Body |
|---|---|---|---|---|
| POST | /auth/register | No | Create a new user account. Returns the new user's UUID. | `{ "email": "user@example.com", "password": "secret123" }` |
| POST | /auth/login | No | Validate credentials. Returns a JWT token and expiry. | `{ "email": "user@example.com", "password": "secret123" }` |

### Documents

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | /documents | Yes | Upload a PDF (multipart/form-data, field name: `file`). Returns `202 Accepted` with `{ "id": "<documentId>" }`. |
| GET | /documents | Yes | List all documents for the authenticated user. Returns an array with id, filename, status, size, and timestamps. |
| GET | /documents/{id} | Yes | Get metadata and current status for a specific document. |
| DELETE | /documents/{id} | Yes | Delete a document and all associated chunks, embeddings, and conversation history. |

### Chat

| Method | Path | Auth | Description | Body |
|---|---|---|---|---|
| POST | /documents/{id}/chat | Yes | Ask a question. Returns `text/event-stream`. Responds with `409` if document status is not `INDEXED`. | `{ "question": "What is this document about?" }` |
| GET | /documents/{id}/history | Yes | Retrieve paginated conversation history for a document. | — |
| DELETE | /documents/{id}/history | Yes | Delete all conversation history for a document. | — |

---

## Observability

The full observability stack runs locally alongside the application services.

| Tool | URL | Credentials | Purpose |
|---|---|---|---|
| Grafana | http://localhost:3000 | admin / admin | Unified dashboards for logs, traces, and metrics |
| Prometheus | http://localhost:9090 | — | Raw metrics scraping and querying |
| RabbitMQ Management | http://localhost:15672 | guest / guest | Queue depths, message rates, consumer status |

### What to Monitor

**Pipeline health:** check the `rabbitmq.dlq.messages` counter tagged by queue name. Any increment indicates a message that failed all retries and requires manual investigation.

**Query performance:** the time from chat request to first SSE token is dominated by the pgvector similarity search and LLM time-to-first-token. Monitor HTTP request duration on `/documents/{id}/chat`.

**Queue depth:** in RabbitMQ Management, watch `document.parsed` queue depth during uploads. It should drain within seconds on healthy hardware. A growing depth indicates the embedding-service is falling behind.

**Distributed traces:** in Grafana, use the Tempo datasource to trace a full request from the api-gateway through to query-service, including the time spent in pgvector and Ollama calls.

---

## Project Structure

```
doc-query/
├── CLAUDE.md                          # Operational context for Claude Code
├── BLUEPRINT.md                       # Architecture decisions and detailed reference
├── pom.xml                            # Maven parent POM (manages all module versions)
│
├── commons/                           # Shared library compiled into all services
│   └── src/main/java/.../commons/
│       └── web/ErrorResponse.java     # Standard error response record
│
├── api-gateway/                       # Port 8080 — external entry point
│   └── src/main/java/.../gateway/
│       ├── auth/                      # Register, login, JWT generation
│       ├── routing/                   # RouteRegistry, RequestForwarder, RoutingController
│       ├── security/                  # JwtService, SecurityConfig, JwtFilter
│       └── exception/                 # GlobalExceptionHandler
│
├── document-service/                  # Port 8081 — document lifecycle
│   └── src/main/java/.../document/
│       ├── domain/                    # Document, Chunk entities, repo interfaces
│       ├── usecase/                   # UploadDocument, ListDocuments, DeleteDocument
│       ├── application/               # AppService implementations
│       ├── infrastructure/
│       │   ├── messaging/             # RabbitMQConfig, publishers, DlqMonitorConsumer
│       │   └── persistence/           # JPA repositories
│       └── api/                       # DocumentController
│
├── embedding-service/                 # Port 8082 — vector embedding
│   └── src/main/java/.../embedding/
│       └── infrastructure/
│           ├── messaging/             # RabbitMQ consumer, DlqMonitorConsumer
│           └── ai/                    # Spring AI EmbeddingModel integration
│
├── query-service/                     # Port 8083 — RAG and chat
│   └── src/main/java/.../query/
│       ├── domain/                    # ConversationHistory, repo interfaces
│       ├── usecase/                   # ChatWithDocument, GetHistory, DeleteHistory
│       ├── application/               # AppService implementations
│       ├── infrastructure/
│       │   ├── ai/                    # ChatClient, EmbeddingModel, prompt assembly
│       │   └── persistence/           # JPA, DocumentOwnershipValidator
│       └── api/                       # ChatController, HistoryController
│
├── docker/
│   ├── docker-compose.yml             # Full infrastructure + Spring Boot services
│   ├── prometheus.yml                 # Scrape configs for all services
│   ├── tempo.yaml                     # Distributed tracing configuration
│   └── grafana/provisioning/          # Auto-provisioned datasources
│
└── frontend/
    ├── index.html                     # Single-page application shell
    ├── style.css                      # Dark theme, responsive layout
    └── app.js                         # Auth, document management, SSE chat
```

---

## Work in Progress

### ✅ Implemented

- Full RAG pipeline (ingestion, embedding, similarity search, generation)
- JWT authentication with userId claim propagation
- Asynchronous document processing via RabbitMQ with DLQ retry pattern
- SSE streaming responses from LLaMA 3.2
- Conversation history (persist, retrieve, delete)
- Document lifecycle state machine (UPLOADED → INDEXED, FAILED)
- Dynamic API Gateway routing via RouteRegistry (configurable, no hardcoded if/else)
- Ownership validation — users can only access their own documents
- File type and size validation (PDF only, 10 MB limit)
- Global exception handling with structured ErrorResponse across all services
- Observability stack: Prometheus, Grafana, Loki, Tempo, OpenTelemetry
- Multi-stage Docker builds for all four services
- Docker network isolation (public-net / internal-net)
- DLQ monitoring with Micrometer counters
- Frontend: authentication, upload, polling, SSE chat, conversation history

### 🚧 In Progress

- Testcontainers integration tests for all services
- k6 load tests for the upload and chat endpoints
- Grafana dashboards (pipeline health, query performance, queue depth)
- OpenAPI documentation with SpringDoc
- Flyway migration scripts (schema defined, migration files pending)

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

---

## Author

Built with ☕ and curiosity by [LuisHBF](https://github.com/LuisHBF).

This is a portfolio project demonstrating microservices architecture, event-driven design, LLM integration, and production-grade observability — built entirely with open-source tools running locally.

Feedback, issues, and suggestions are welcome via [GitHub Issues](https://github.com/LuisHBF/doc-query/issues).
