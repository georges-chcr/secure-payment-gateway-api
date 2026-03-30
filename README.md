# Fraud Detection System (Microservices)

A production-ready, event-driven fraud detection platform built on a microservices architecture. Real-time payment transactions are scored by a Python machine learning engine and gated by a reactive Java API — all orchestrated through Docker Compose.

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                        Docker Network                            │
│                                                                  │
│  ┌─────────────────────┐       ┌──────────────────────────────┐  │
│  │  payment-gateway    │──────▶│     fraud-ml-engine          │  │
│  │  Java / Spring Boot │       │     Python / FastAPI         │  │
│  │  WebFlux (reactive) │       │     scikit-learn model       │  │
│  │  :8080              │       │     :8000                    │  │
│  └────────┬────────────┘       └──────────────────────────────┘  │
│           │                                                       │
│  ┌────────▼────────────┐       ┌──────────────────────────────┐  │
│  │  PostgreSQL 15      │       │  fraud-dashboard             │  │
│  │  fraud_db           │       │  Angular (frontend)          │  │
│  │  :5432              │       └──────────────────────────────┘  │
│  └─────────────────────┘                                         │
└──────────────────────────────────────────────────────────────────┘
```

| Component | Technology | Role |
|-----------|------------|------|
| `payment-gateway` | Java 21, Spring Boot, WebFlux | Reactive REST API — validates, persists, and routes transactions |
| `fraud-ml-engine` | Python 3.11, FastAPI, scikit-learn | ML inference service — returns fraud probability score |
| `fraud-dashboard` | Angular 17 | Operations dashboard for monitoring flagged transactions |
| `postgres-db` | PostgreSQL 15 | Persistent store for transactions and audit logs |
| Messaging (roadmap) | Apache Kafka | Async event streaming for high-throughput pipelines |

---

## Repository Layout

```
secure-payment-gateway-api/      ← this repo (Java gateway + orchestration)
├── src/                         ← Spring Boot application source
├── fraud-dashboard/             ← Angular frontend
├── docker-compose.yml           ← Full-stack orchestration entry point
└── Dockerfile                   ← Gateway container image

../fraud-detection-ml-engine/    ← sibling repo (Python ML engine)
├── api.py                       ← FastAPI application
├── models/                      ← Trained scikit-learn artifacts
└── Dockerfile                   ← ML engine container image
```

---

## 🚀 Quick Start (Docker)

### Prerequisites
- Docker >= 24 and Docker Compose V2
- Git

### 1 — Clone both repositories side by side

```bash
# Create a shared workspace directory
mkdir fraud-system && cd fraud-system

# Clone the gateway + orchestration repo (this one)
git clone <url-of-this-repo> secure-payment-gateway-api

# Clone the ML engine repo next to it
git clone <url-of-ml-repo> fraud-detection-ml-engine
```

The final layout must look like:

```
fraud-system/
├── secure-payment-gateway-api/   ← contains docker-compose.yml
└── fraud-detection-ml-engine/
```

### 2 — Start the full stack

```bash
cd secure-payment-gateway-api
docker compose up -d --build
```

Docker Compose will build both service images and start all containers.
The gateway will wait for PostgreSQL and the ML engine to pass their health checks before accepting traffic.

### 3 — Verify services are up

```bash
docker compose ps

# Gateway health
curl http://localhost:8080/actuator/health

# ML engine health
curl http://localhost:8000/health
```

### Stop

```bash
docker compose down          # stop containers, keep volumes
docker compose down -v       # stop containers and remove volumes
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `postgres-db` | PostgreSQL hostname (Docker service name) |
| `DB_NAME` | `fraud_db` | Database name |
| `DB_USER` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `ML_ENGINE_URL` | `http://fraud-ml-engine:8000` | Internal ML service URL |

---

## License

MIT
