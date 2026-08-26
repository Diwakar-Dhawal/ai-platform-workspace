# AI Platform Workspace

> A modular AI ecosystem where multiple applications share a common authentication platform while remaining independently deployable.

![Platform Overview](docs/platform-overview.png)

---

## Vision

This repository is **not** a single product. It is an **AI Platform** capable of hosting multiple AI-powered applications.

- **Applications** are independent products.
- **Platform services** are shared infrastructure.

```
AI Platform Workspace
│
├── Platform Services (Reusable)
│   ├── identity-service      — Auth, JWT, roles, sessions
│   ├── chat-service           — (planned) Generic chat engine
│   └── ai-platform            — (planned) Embeddings, RAG, LLM providers
│
└── Applications
    └── insighttube/           — YouTube content chat
        ├── frontend           — Next.js 16 + React 19
        ├── gateway            — Spring Cloud Gateway
        ├── tube-service       — Spring Boot (ingest, chat, sessions)
        └── transcript-service — Python/FastAPI (YouTube transcripts)
```

---

## Architecture Principles

| Rule | Description |
|------|-------------|
| Platform never depends on apps | Platform services know nothing about InsightTube or any app |
| Apps consume platform APIs | Applications call Identity, AI Platform, Chat Service |
| Each app owns its own gateway | No shared gateway — each app has `applications/<app>/gateway/` |
| Gateway does only routing/JWT | No business logic in gateways |
| Every service owns its database | No shared DB between services |
| HTTP communication first | Event-driven (RabbitMQ/Kafka) comes later |
| Each service deploys independently | Docker, K8s, or VM — your choice |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Next.js 16, React 19, TypeScript, Tailwind CSS 4, Zustand |
| UI Components | shadcn/ui, Lucide Icons |
| Backend Services | Spring Boot 3, Spring Cloud Gateway, Spring Security + JWT |
| AI / ML | Python FastAPI, YouTube Transcript API |
| Database | PostgreSQL 17, Redis 7 |
| DevOps | Docker, Docker Compose |

---

## Getting Started

### Prerequisites

- Node.js 18+
- Java 17+ (for Spring Boot services)
- Python 3.10+ (for Transcript Service)
- Docker & Docker Compose

### Start Infrastructure

```bash
# From workspace root
docker-compose up -d
```

This starts PostgreSQL, pgAdmin, and Redis.

### Start Backend Services

```bash
# Identity Service (platform)
cd platform/identity-service
./mvnw spring-boot:run

# Tube Service (application)
cd applications/insighttube/tube-service
./mvnw spring-boot:run

# Transcript Service (application)
cd applications/insighttube/transcript-service
pip install -r requirements.txt
python main.py
```

### Start Frontend

```bash
cd applications/insighttube/frontend
npm install
npm run dev
```

Opens at [http://localhost:3000](http://localhost:3000).

---

## Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Frontend (dev) | 3000 | Next.js dev server |
| Gateway | 8080 | Spring Cloud Gateway |
| Identity Service | 8081 | Auth, registration, JWT |
| Tube Service | 8082 | Ingest, chat, sessions |
| AI Platform | 8084 | (planned) Embeddings, RAG |
| Transcript Service | 8085 | YouTube transcript extraction |
| PostgreSQL | 5432 | Database |
| pgAdmin | 5050 | Database admin UI |
| Redis | 6379 | Caching (transcripts) |

---

## Deployment

See [deployment.md](deployment.md) for hosting recommendations and deployment strategies.

---

## Documentation

| Document | Description |
|----------|-------------|
| [architecture.md](docs/architecture.md) | Architecture rules and boundaries |
| [project-context.md](docs/project-context.md) | Platform vision and structure |
| [api-contracts.md](docs/api-contracts.md) | API contract definitions |
| [coding-standards.md](docs/coding-standards.md) | Coding standards |
| [flow.md](docs/flow.md) | Data flow diagrams |
| [identity-service-configuration.md](docs/identity-service-configuration.md) | Identity Service setup |
| [roadmap.md](docs/roadmap.md) | Development roadmap |

---

## Current Phase

We are in **Phase 1 — Foundation**.

Completed:
- Workspace structure
- Identity Service (full auth flow)
- InsightTube Gateway
- Tube Service (ingest + chat)
- Transcript Service
- Frontend (auth, chat, ingest UI)
- PostgreSQL + Redis infrastructure

Next:
- Email verification enforcement on frontend
- Chat Service extraction from Tube Service
- AI Platform service (embeddings, RAG)
- Production deployment
- Future apps (PDFMind, InstaMind, WebsiteMind)

---

## License

Proprietary — Freebuff Platform
