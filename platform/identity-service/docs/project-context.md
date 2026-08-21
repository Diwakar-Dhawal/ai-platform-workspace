# AI Service Platform

## Vision

Build a modular AI ecosystem where multiple applications share a common authentication platform while remaining independently deployable.

---

## Workspace Structure

```
ai-platform-workspace/
├── applications/
│   └── insighttube/
│       ├── frontend/          (planned)
│       ├── gateway/           ✅ Implemented — Spring Cloud Gateway 5.0.2
│       └── tube-service/      (planned)
├── platform/
│   ├── identity-service/      ✅ Implemented — Spring Boot 4.1.0
│   ├── ai-platform/           (planned)
│   └── chat-service/          (planned)
├── docs/                      Architecture docs, ADRs, roadmap
├── docker/                    Docker configs (planned)
└── docker-compose.yml         (planned)
```

---

## Current Status (2026-08-21)

### ✅ Phase 1: Identity Service — COMPLETE

Identity Service is a fully functional authentication platform:

- **Registration & Login** with BCrypt password hashing
- **JWT access tokens** (15 min) + **refresh tokens** (7 days)
- **Client-scoped roles** (USER, MANAGER, ADMIN)
- **Email verification** (console-based in dev)
- **Password reset** flow
- **Rate limiting** on auth endpoints
- **Flyway migrations** (V1: schema, V2: verification tokens, V3: seed data)
- **Integration tests** (Auth, Client, Role)
- **Spring Security** with stateless sessions

### ✅ Phase 2 (Milestones 1-2): InsightTube Gateway — COMPLETE

Gateway is a Spring Cloud Gateway (WebFlux) that:

- **Validates JWT tokens** at the gateway edge before forwarding
- **Public routes** for auth endpoints (register, login, refresh, forgot-password, reset-password, verify-email)
- **Protected routes** for all other identity endpoints
- **Forwards JWT claims** as headers to downstream services
- **CORS** configured for frontend dev servers
- **Consistent error handling** matching Identity Service format

### 🔲 Phase 2 (Milestones 3-4): Gateway hardening — NOT STARTED

- Resilient proxy behavior (timeouts, circuit breakers)
- Health/readiness checks
- Gateway integration tests
- Production-safe configuration

### 🔲 Phase 3-6: Not started

- Tube Service
- Frontend
- AI Platform
- Chat Service

---

## Architecture Rules

See `docs/architecture.md` for full details.

### Identity Service handles:
- Authentication, Authorization, Sessions, Roles

### Identity Service NEVER handles:
- User Profile, YouTube, AI, Tasks, Business logic

### Gateway responsibilities:
- Routing, JWT validation, Request forwarding

### Gateway NEVER contains:
- Business logic

### Integration contract:
- Services communicate ONLY through HTTP APIs
- Services NEVER share Java classes

---

## Authentication Flow

```
Frontend → Gateway (JWT validation) → Identity Service → PostgreSQL
```

1. Frontend sends request to gateway (`localhost:8080`)
2. Gateway validates JWT (for protected routes)
3. Gateway forwards to Identity Service (`localhost:8081`)
4. Identity Service processes and responds
5. Gateway returns response to frontend

### JWT Token Structure

The gateway validates and forwards these claims:

| Claim | Header | Description |
|-------|--------|-------------|
| `uid` | `X-User-Id` | User UUID |
| `uname` | `X-User-Username` | Username |
| `roles` | `X-User-Roles` | Comma-separated roles |
| `aud` | `X-Client-Id` | Client ID |
| `sid` | `X-Session-Id` | Session UUID |
| `typ` | — | Token type (ACCESS/REFRESH) |
| `ver` | — | Token version |

---

## Service Ports

| Service | Port | Status |
|---------|------|--------|
| PostgreSQL | 5432 | Running |
| Identity Service | 8081 | Running |
| Gateway | 8080 | Running |
| Tube Service | 8082 | Planned |

---

## Seeded Data

| Entity | Value |
|--------|-------|
| Client | `insighttube` |
| Admin | `admin@insighttube.com` / `password` |
| Roles | USER, MANAGER, ADMIN |

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `IDENTITY_DB_URL` | PostgreSQL URL | `jdbc:postgresql://localhost:5432/identity` |
| `IDENTITY_DB_USERNAME` | DB username | `postgres` |
| `IDENTITY_DB_PASSWORD` | DB password | `postgres` |
| `IDENTITY_JWT_SECRET` | HMAC signing key | Shared dev secret |
| `JAVA_TOOL_OPTIONS` | JVM args | Must include `-Duser.timezone=UTC` |

---

## Quick Start

See `docs/startup-guide.md` for detailed instructions.

```bash
# 1. Start Identity Service
cd platform/identity-service
JAVA_TOOL_OPTIONS="-Duser.timezone=UTC" ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 2. Start Gateway (in another terminal)
cd applications/insighttube/gateway
JAVA_TOOL_OPTIONS="-Duser.timezone=UTC" ./mvnw spring-boot:run

# 3. Test
curl -X POST http://localhost:8080/api/v1/identity/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@insighttube.com","password":"password","clientId":"insighttube"}'
```
