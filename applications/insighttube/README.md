# InsightTube

> Chat with YouTube content using AI — paste any video, playlist, or channel URL and ask questions with timestamp-cited answers.

## What It Does

InsightTube extracts transcripts from YouTube content, generates embeddings, and lets you have a natural language conversation about the video content. Ask questions like:

- *"What does this video say about authentication?"*
- *"Find the part where they discuss database design"*
- *"Summarize the key takeaways from this lecture"*

---

## Architecture

InsightTube is an **application** within the AI Platform. It consumes platform services (Identity) and owns its own gateway, backend, and frontend.

```
                    ┌─────────────────────┐
                    │   Next.js Frontend   │
                    │     (port 3000)      │
                    └─────────┬───────────┘
                              │ HTTP
                    ┌─────────▼───────────┐
                    │  Spring Cloud GW     │
                    │     (port 8080)      │
                    └──┬─────────────┬────┘
                       │             │
          ┌────────────▼──┐   ┌──────▼──────────────┐
          │  Identity Svc  │   │   Tube Service       │
          │  (port 8081)   │   │   (port 8082)        │
          │  Platform svc  │   │   Application svc     │
          └────────────────┘   └──┬──────┬──────┬────┘
                                  │      │      │
                        ┌─────────▼┐  ┌──▼───┐  └───▼──────────┐
                        │PostgreSQL│  │Redis │  Transcript Svc  │
                        │ (5432)   │  │(6379)│  (port 8085)     │
                        └──────────┘  └──────┘  Python/FastAPI  │
                                                         │
                                                         ▼
                                                   YouTube API
```

### Service Responsibilities

| Service | What It Does | Who Owns It |
|---------|-------------|-------------|
| **Frontend** | Auth UI, chat interface, ingest modal | InsightTube |
| **Gateway** | Routing, CORS, JWT validation | InsightTube |
| **Tube Service** | Ingest orchestration, chat, session management | InsightTube |
| **Transcript Service** | YouTube transcript extraction with Redis caching | InsightTube |
| **Identity Service** | Registration, login, JWT, roles, email verification | **Platform** (shared) |
| **PostgreSQL** | Persistent storage | Infrastructure |
| **Redis** | Transcript caching | Infrastructure |

---

## End-to-End Flow

### 1. Authentication

```
User → Frontend → Gateway → Identity Service → PostgreSQL
                                    ↓
                              JWT + Refresh Token
                                    ↓
                          Frontend stores tokens
```

- **Registration**: Creates user + email verification token
- **Login**: Validates credentials, returns JWT + refresh token
- **Forgot Password**: Sends reset email via EmailService

### 2. Content Ingestion

```
User pastes YouTube URL
        ↓
Frontend → Gateway → Tube Service → Transcript Service → YouTube
        ↓                                      ↓
   SSE stream ←────────────────────── Redis (cache)
        ↓
   Tube Service → PostgreSQL
   (embeddings + vectors via AI Platform)
```

Steps: **Extract Transcript** → **Chunk** → **Embed** → **Store**

### 3. AI Chat

```
User asks question
        ↓
Frontend → Gateway → Tube Service → AI Platform → LLM (Gemini)
        ↓                                      ↓
   Vector search (pgvector)         Prompt construction
        ↓                                      ↓
   Context retrieval ←───────────── RAG pipeline
        ↓
   Answer with timestamp citations
```

---

## Features

- **YouTube Ingestion** — Video, playlist, and channel URLs
- **Real-time Progress** — SSE streaming with step-by-step indicators
- **AI Chat** — Natural language with timestamp citations
- **Session Management** — Rename, search, delete chat sessions
- **Auth Flow** — Registration, login, forgot password, email verification
- **Offline Detection** — Banner when offline, graceful error handling
- **Dark UI** — Professional warm rose/amber color scheme

---

## Quick Start

```bash
# Infrastructure
docker-compose up -d

# Backend services (3 terminals)
cd platform/identity-service && ./mvnw spring-boot:run
cd applications/insighttube/tube-service && ./mvnw spring-boot:run
cd applications/insighttube/transcript-service && python main.py

# Frontend
cd applications/insighttube/frontend && npm run dev
```

Open [http://localhost:3000](http://localhost:3000)

**Demo credentials**: `demo@insighttube.com` / `Demo@1234`

---

## Project Structure

```
applications/insighttube/
├── frontend/                  # Next.js 16 + React 19
│   └── src/
│       ├── app/               # Next.js App Router
│       ├── components/        # UI components
│       │   ├── auth/          # LoginPage (login, register, forgot password)
│       │   ├── chat/          # ChatArea, ChatInput, Message, MarkdownContent
│       │   ├── ingest/        # IngestModal, IngestBar (SSE progress)
│       │   ├── layout/        # AppLayout, NetworkStatusBanner
│       │   ├── sidebar/       # Session management sidebar
│       │   └── ui/            # shadcn/ui components
│       ├── lib/               # api.ts, types.ts, hooks.ts
│       └── stores/            # authStore, chatStore, ingestStore (Zustand)
├── gateway/                   # Spring Cloud Gateway (port 8080)
├── tube-service/              # Spring Boot (port 8082)
│   └── src/main/resources/    # application.yaml, Flyway migrations
└── transcript-service/        # Python FastAPI (port 8085)
    ├── main.py                # Transcript extraction + Redis caching
    └── requirements.txt
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Next.js 16, React 19, TypeScript, Tailwind CSS 4, Zustand |
| UI | shadcn/ui, Lucide Icons |
| Backend | Spring Boot 3, Spring Cloud Gateway, Spring Security + JWT |
| Transcript | Python FastAPI, youtube-transcript-api |
| Database | PostgreSQL 17 + pgvector, Redis 7 |
| AI | Gemini (via AI Platform service) |

---

## Screenshots

### Login
![Login](docs/screenshots/login.png)

### Welcome Screen
![Welcome](docs/screenshots/welcome.png)

### Ingesting Content
![Ingesting](docs/screenshots/ingesting.png)

### Chat Interface
![Chat](docs/screenshots/chat.png)
