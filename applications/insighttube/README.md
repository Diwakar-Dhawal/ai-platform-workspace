# InsightTube

> Chat with YouTube content using AI — ask questions, find timestamps, and explore videos conversationally.

InsightTube lets you paste a YouTube video, playlist, or channel URL and instantly start a conversation about its content. Powered by AI embeddings and retrieval, it provides timestamp-cited answers drawn directly from the video transcripts.

![InsightTube Screenshot](docs/screenshots/preview.png)

---

## Features

- **YouTube Ingestion** — paste any video, playlist, or channel URL and InsightTube will extract transcripts, chunk them, generate embeddings, and store them for retrieval.
- **AI Chat** — ask natural-language questions about the ingested content and get answers with timestamp citations.
- **Real-time Progress** — see live progress as content is extracted, chunked, embedded, and stored (via SSE streaming).
- **Session Management** — rename, search, and delete chat sessions from the sidebar.
- **Responsive Dark UI** — professional dark theme with warm rose/amber accent palette.
- **Offline Resilience** — clear banner when offline, graceful error handling on failed requests.
- **Email/Password Auth** — registration, login, forgot password, and email verification via the Identity Service.

---

## Architecture

```
┌──────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Next.js     │────▶│  Gateway (8080)  │────▶│ Identity Service │
│  Frontend    │     │                  │     │    (8081)        │
│  (3000)      │     └──────────────────┘     └─────────────────┘
│              │
│              │────▶ Tube Service (8082)
│              │
│              │────▶ Transcript Service (8000)
└──────────────┘
```

| Service              | Port | Description                                      |
|----------------------|------|--------------------------------------------------|
| Frontend             | 3000 | Next.js 16 app (React 19)                        |
| Gateway              | 8080 | Spring Cloud Gateway                             |
| Identity Service     | 8081 | Spring Boot — auth, registration, email verify   |
| Tube Service         | 8082 | Spring Boot — ingest, chat, sessions             |
| Transcript Service   | 8000 | Python/FastAPI — YouTube transcript extraction   |

---

## Screenshots

### Login Page
![Login](docs/screenshots/login.png)

### Welcome Screen
![Welcome](docs/screenshots/welcome.png)

### Ingesting Content
![Ingesting](docs/screenshots/ingesting.png)

### Chat Interface
![Chat](docs/screenshots/chat.png)

---

## Getting Started

### Prerequisites

- Node.js 18+
- Java 17+ (for Spring Boot services)
- Python 3.10+ (for Transcript Service)
- Docker & Docker Compose (optional)

### Frontend (Development)

```bash
cd applications/insighttube/frontend
npm install
npm run dev
```

The dev server runs at **http://localhost:3000** and proxies API calls to the backend services.

### Backend Services

See `docker-compose.yml` at the project root for running all services together, or run each service individually:

```bash
# Identity Service
cd platform/identity-service
./mvnw spring-boot:run

# Tube Service
cd applications/insighttube/tube-service
./mvnw spring-boot:run

# Transcript Service
cd applications/insighttube/transcript-service
pip install -r requirements.txt
python main.py
```

### Docker

```bash
docker-compose up --build
```

---

## Tech Stack

| Layer      | Technology                                                    |
|------------|---------------------------------------------------------------|
| Frontend   | Next.js 16, React 19, TypeScript, Tailwind CSS 4, Zustand    |
| UI         | shadcn/ui, Lucide Icons, Sonner (toasts)                     |
| Backend    | Spring Boot 3, Spring Cloud Gateway, Spring Security + JWT    |
| AI         | Python FastAPI, YouTube Transcript API                       |
| Database   | PostgreSQL, pgvector                                          |

---

## Environment

| Variable            | Default                          | Description                    |
|---------------------|----------------------------------|--------------------------------|
| `NEXT_PUBLIC_API`   | (empty — uses proxy rewrites)    | Backend API base URL           |

The frontend uses Next.js API rewrites to proxy:
- `/tube-service/*` → `http://localhost:8082/tube-service/*`
- `/identity-service/*` → `http://localhost:8081/identity-service/*`
- `/api/v1/content/*` → `http://localhost:8082/api/v1/content/*`

---

## Project Structure

```
applications/insighttube/
├── frontend/                  # Next.js frontend
│   └── src/
│       ├── app/               # Next.js App Router pages
│       ├── components/
│       │   ├── auth/          # LoginPage
│       │   ├── chat/          # ChatArea, ChatInput, Message, MarkdownContent
│       │   ├── ingest/        # IngestModal, IngestBar
│       │   ├── layout/        # AppLayout, NetworkStatusBanner
│       │   ├── sidebar/       # Sidebar
│       │   └── ui/            # shadcn/ui components
│       ├── lib/               # api.ts, types.ts, hooks.ts, utils.ts
│       └── stores/            # authStore, chatStore, ingestStore
├── gateway/                   # Spring Cloud Gateway
├── transcript-service/        # Python FastAPI
└── tube-service/              # Spring Boot content service
```

---

## License

Proprietary — Freebuff Platform
