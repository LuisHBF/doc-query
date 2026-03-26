# doc-query

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
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
| Framework | Spring Boot | 3.3.5 |
| Build | Maven | 3.9.x |
| LLM abstraction | Spring AI | 1.x |
| LLM runtime | Ollama | latest |
| Chat model | LLaMA 3.2 3B Q4 | — |
| Embedding model | nomic-embed-text v1.5 | — |
| Database | PostgreSQL | 16 |
| Vector search | pgvector | — |
| Migrations | Flyway | — |
| Message broker | RabbitMQ | 3.13 |
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
| **api-gateway** | 8080 | Single external entry point. Handles JWT authentication, rate limiting, and dynamic request routing to internal services. |
| **document-service** | 8081 | Handles file uploads, text extraction with Apache Tika, chunking, and async event publishing to RabbitMQ. Owns the document lifecycle state machine (UPLOADED → PARSED → INDEXED). |
| **embedding-service** | 8082 | Consumes parsed document events from RabbitMQ, generates vector embeddings in batch using nomic-embed-text via Ollama, and persists them to pgvector. |
| **query-service** | 8083 | Embeds user questions, performs cosine similarity search against pgvector, assembles prompts with retrieved context and conversation history, and streams LLM responses via SSE. |
| **commons** | — | Shared library compiled into all services. Contains shared DTOs and web utilities (e.g. `ErrorResponse`). |

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

Run each in a separate terminal from the repo root:

```bash
mvn -pl document-service spring-boot:run
mvn -pl api-gateway spring-boot:run
mvn -pl embedding-service spring-boot:run
mvn -pl query-service spring-boot:run
```

Wait for each service to log `Started ... Application` before starting the next.

| Service | Port | Health |
|---|---|---|
| api-gateway | 8080 | http://localhost:8080/actuator/health |
| document-service | 8081 | http://localhost:8081/actuator/health |
| embedding-service | 8082 | http://localhost:8082/actuator/health |
| query-service | 8083 | http://localhost:8083/actuator/health |

### Step 5 — Open the frontend

Open `frontend/index.html` directly in your browser. No build step required.

Register an account, log in, upload a PDF, wait for it to reach `INDEXED` status, then start chatting.
