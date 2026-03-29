# doc-query

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Status](https://img.shields.io/badge/status-work%20in%20progress-yellow)](https://github.com/LuisHBF/doc-query)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

**A local-first document intelligence platform powered by RAG, Spring Boot, and Ollama.**

Upload your PDFs. Ask questions in natural language. Get answers grounded in your documents — entirely on your own machine, with no external APIs and no data leaving your environment.

---

## Overview

doc-query is a Retrieval-Augmented Generation (RAG) platform built with a microservices architecture. You upload a PDF, the system processes it asynchronously — extracting text, splitting it into chunks, and generating vector embeddings — and then lets you have a natural language conversation with the document's content.

Every component runs on your own hardware: the LLM, the embedding model, the vector database, the message broker, and the observability stack. Nothing reaches the internet after initial setup.

---

## Tech Stack

| Category | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 3.4.1 |
| Build | Maven | 3.9.x |
| LLM abstraction | Spring AI | 1.x |
| LLM runtime | Ollama | latest |
| Chat model | LLaMA 3.2 3B Q4 | — |
| Embedding model | nomic-embed-text v1.5 | — |
| Database | PostgreSQL | 16 |
| Vector search | pgvector | — |
| Migrations | Flyway | — |
| Message broker | RabbitMQ | 3.13 |
| Service discovery | Spring Cloud Netflix Eureka | 2023.0.x |
| API Gateway | Spring Cloud Gateway MVC | 2023.0.x |
| Rate limiting | Bucket4j | — |
| PDF parsing | Apache Tika | — |
| Tracing | OpenTelemetry + Tempo | — |
| Metrics | Micrometer + Prometheus | — |
| Logging | Loki + loki4j | — |
| Dashboards | Grafana | 10.4.0 |

---

## Modules

| Module | Port | Description |
|---|---|---|
| **eureka-server** | 8761 | Service registry. All services register themselves on startup and send periodic heartbeats. The gateway resolves service addresses dynamically via `lb://service-name` instead of hardcoded URLs. |
| **api-gateway** | 8080 | Single external entry point. Handles JWT authentication, rate limiting, and dynamic request routing to internal services via Spring Cloud Gateway MVC. |
| **document-service** | 8081 | Handles file uploads, text extraction with Apache Tika, chunking, and async event publishing to RabbitMQ. Owns the document lifecycle state machine (UPLOADED → PARSED → INDEXED). |
| **embedding-service** | 8082 | Consumes parsed document events from RabbitMQ, generates vector embeddings in batch using nomic-embed-text via Ollama, and persists them to pgvector. |
| **query-service** | 8083 | Embeds user questions, performs cosine similarity search against pgvector, assembles prompts with retrieved context and conversation history, and streams LLM responses via SSE. |
| **commons** | — | Shared library compiled into all services. Contains shared DTOs and web utilities (e.g. `ErrorResponse`). |

---

## Service Discovery

All services register with **Eureka Server** on startup using their `spring.application.name` as the service ID. The api-gateway resolves downstream addresses dynamically via `lb://service-name` — no hardcoded URLs anywhere in the routing layer.

```
eureka-server :8761
    ↑ registers          ↑ registers          ↑ registers          ↑ registers
api-gateway         document-service     embedding-service      query-service

api-gateway routes:
  /documents/**      →  lb://document-service   (resolved by Eureka)
  /documents/*/chat  →  lb://query-service       (resolved by Eureka)
```

**Why not Traefik or Consul?** This project is Spring-native by design. Spring Cloud Netflix Eureka integrates directly with the Spring Boot autoconfiguration model and requires zero infrastructure beyond a JVM process. Adding Traefik would replace the gateway with a black box; Consul adds operational overhead without meaningful benefit at this scale.

**Why Spring Cloud Gateway MVC instead of reactive SCG?** The gateway owns user authentication and requires JPA for the `users` table. The reactive SCG variant (Netty) is incompatible with blocking JPA. Spring Cloud Gateway MVC preserves the Virtual Threads + Tomcat model used across the entire platform while still providing declarative routing and Eureka integration.

---

## Rate Limiting

All requests entering the system pass through a **Bucket4j** rate limit filter in the api-gateway, applied after JWT authentication so the key is always the authenticated user's email (or the client IP for unauthenticated requests like `/auth/**`).

Two independent tiers protect different resource classes:

| Tier | Applies to | Limit |
|---|---|---|
| Chat | `POST /documents/*/chat` | 10 requests / minute |
| General | All other endpoints | 60 requests / minute |

Each user (or IP) gets their own token bucket stored in memory. Buckets are created on first request and refill greedily every minute.

When a bucket is exhausted the gateway returns immediately with no downstream call:

```
HTTP 429 Too Many Requests
Retry-After: <seconds until refill>

{"error": "Too Many Requests", "retryAfter": 42}
```

Allowed requests carry an `X-Rate-Limit-Remaining` header so clients can track how many tokens are left.

**Why in-memory?** This is a single-instance deployment. A distributed store (Redis) would add operational complexity without benefit until horizontal scaling is needed.

---

## Observability

The platform ships a fully integrated observability stack. Every component is pre-configured and starts with `docker compose up -d` — no manual setup required.

### Metrics — Prometheus + Grafana

All four Spring Boot services expose `/actuator/prometheus`. Prometheus scrapes each one every 15 seconds.

Three dashboards are **auto-provisioned** into Grafana and available immediately on first boot:

| Dashboard | What it shows |
|---|---|
| **Services Overview** | Request rate, error rate (4xx/5xx %), average and max latency, JVM heap, CPU usage, live thread count, active requests — per service, with a multi-select filter |
| **RAG Pipeline** | Documents uploaded, RabbitMQ messages published/consumed/rejected, DLQ activity, chat request rate broken down by HTTP status, 429 rate-limited requests over time, document-service latency per endpoint |
| **Logs & Traces** | Log volume and error rate per minute per service (from Loki), and a live filterable log stream with `Service` and `Level` dropdowns |

Access Grafana at **http://localhost:3000** (admin / admin).

### Distributed Tracing — OpenTelemetry + Tempo

Every request is instrumented with OpenTelemetry. Trace context (`traceId`, `spanId`) is propagated across service boundaries and included in every log line:

```
11:42:03.512  INFO [document-service,4f3a1c8b9d2e7f6a,8c1d4e2f3a9b5c7d] ...
```

From the **Logs & Traces** dashboard, clicking a `traceId` in the log stream opens the full distributed trace in Tempo automatically (the Loki → Tempo derived field is pre-configured).

### Logging — Loki + loki4j

All services ship structured logs to Loki via the `loki4j` appender. Each log entry carries `app`, `level`, and `host` labels, enabling fine-grained filtering in Grafana without parsing raw text.

### Infrastructure URLs

| Service | URL |
|---|---|
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| RabbitMQ Management | http://localhost:15672 |
| Eureka Dashboard | http://localhost:8761 |

---

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9.x
- Docker Desktop (allocate at least 6 GB RAM)
- A GPU with 8+ GB VRAM is recommended for fast inference, but CPU-only works too

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

Starts PostgreSQL, RabbitMQ, Ollama, Prometheus, Loki, Tempo, and Grafana. Wait 30–60 seconds for all containers to be healthy.

### Step 3 — Pull the AI models (one-time)

```bash
docker exec -it docquery-ollama ollama pull llama3.2:3b
docker exec -it docquery-ollama ollama pull nomic-embed-text
```

### Step 4 — Start the Spring Boot services

Start `eureka-server` first, then the remaining services in any order. Each service registers itself with Eureka on startup and retries automatically if the registry is not yet available.

Run each in a separate terminal from the repo root:

```bash
mvn -pl eureka-server spring-boot:run
mvn -pl api-gateway spring-boot:run
mvn -pl document-service spring-boot:run
mvn -pl embedding-service spring-boot:run
mvn -pl query-service spring-boot:run
```

| Service | Port | Health |
|---|---|---|
| eureka-server | 8761 | http://localhost:8761 |
| api-gateway | 8080 | http://localhost:8080/actuator/health |
| document-service | 8081 | http://localhost:8081/actuator/health |
| embedding-service | 8082 | http://localhost:8082/actuator/health |
| query-service | 8083 | http://localhost:8083/actuator/health |

### Step 5 — Open the frontend

Open `frontend/index.html` directly in your browser. No build step required.

Register an account, log in, upload a PDF, wait for it to reach `INDEXED` status, then start chatting.
