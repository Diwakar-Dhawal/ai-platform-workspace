# Local Development Startup Guide

**Last updated:** 2026-08-21

---

## Prerequisites

- Java 21
- Maven (or use `./mvnw` wrapper in each project)
- PostgreSQL running on `localhost:5432`
- Database: `identity` (create it if missing)

---

## Important: Timezone Setting

PostgreSQL on this machine rejects `Asia/Calcutta`. All services **must** be started with UTC timezone:

```bash
export JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"
```

Or pass it inline when starting each service (see commands below).

---

## Service Ports

| Service | Port | Description |
|---------|------|-------------|
| PostgreSQL | 5432 | Database |
| Identity Service | 8081 | Auth, JWT, users, roles, clients |
| Gateway | 8080 | InsightTube API gateway, JWT validation |
| Tube Service | 8082 | (planned) Business logic, YouTube OAuth |

---

## Service Startup Order

Services must be started in dependency order:

```
1. PostgreSQL (port 5432)
2. Identity Service (port 8081)
3. Gateway (port 8080)
4. Tube Service (port 8082) — when created
```

---

## Starting Identity Service

```bash
# From workspace root
cd platform/identity-service

# Start with local profile and UTC timezone
JAVA_TOOL_OPTIONS="-Duser.timezone=UTC" ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**Windows (Git Bash):**
```bash
cd platform/identity-service
JAVA_TOOL_OPTIONS="-Duser.timezone=UTC" cmd //c "start /b mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local > C:\tmp\identity-service.log 2>&1"
```

**Verify it's running:**
```bash
curl http://localhost:8081/identity-service/health
```

### Identity Service Profiles

| Profile | Description |
|---------|-------------|
| `local` | PostgreSQL on localhost, console email, dev JWT secret |
| `docker` | PostgreSQL via Docker networking |
| `prod` | Production config (external secrets) |

### Database Configuration (local profile)

| Property | Default Value |
|----------|---------------|
| URL | `jdbc:postgresql://localhost:5432/identity` |
| Username | `postgres` |
| Password | `postgres` |
| JWT Secret | `ThisIsAVeryLongDevelopmentSecretKey12345678901234567890` |
| JWT Issuer | `identity-service` |
| Context Path | `/identity-service` |

Environment variable overrides:
- `IDENTITY_DB_URL`
- `IDENTITY_DB_USERNAME`
- `IDENTITY_DB_PASSWORD`
- `IDENTITY_JWT_SECRET`

---

## Starting Gateway

```bash
# From workspace root
cd applications/insighttube/gateway

# Start with local profile and UTC timezone
JAVA_TOOL_OPTIONS="-Duser.timezone=UTC" ./mvnw spring-boot:run
```

**Windows (Git Bash):**
```bash
cd applications/insighttube/gateway
cmd //c "start /b mvnw.cmd spring-boot:run > C:\tmp\gateway.log 2>&1"
```

**Verify it's running:**
```bash
curl http://localhost:8080/actuator/health
```

### Gateway Configuration

The gateway shares the JWT secret with Identity Service:

| Property | Value |
|----------|-------|
| JWT Secret | Same as Identity Service (`IDENTITY_JWT_SECRET` env var) |
| JWT Issuer | `identity-service` |
| Gateway Port | `8080` |
| Identity Service URI | `http://localhost:8081` |

### Route Configuration

**Public routes** (no JWT required):
| Path | Destination |
|------|-------------|
| `POST /api/v1/identity/auth/register` | Identity → `/identity-service/auth/register` |
| `POST /api/v1/identity/auth/login` | Identity → `/identity-service/auth/login` |
| `POST /api/v1/identity/auth/refresh` | Identity → `/identity-service/auth/refresh` |
| `POST /api/v1/identity/auth/forgot-password` | Identity → `/identity-service/auth/forgot-password` |
| `POST /api/v1/identity/auth/reset-password` | Identity → `/identity-service/auth/reset-password` |
| `POST /api/v1/identity/auth/verify-email` | Identity → `/identity-service/auth/verify-email` |
| `GET /api/v1/identity/health` | Identity → `/identity-service/health` |

**Protected routes** (JWT required):
| Path | Destination |
|------|-------------|
| `POST /api/v1/identity/auth/logout` | Identity |
| `POST /api/v1/identity/auth/logout-all` | Identity |
| `GET /api/v1/identity/users/**` | Identity |
| `GET /api/v1/identity/clients/**` | Identity |
| `GET /api/v1/identity/roles/**` | Identity |
| `GET /api/v1/identity/**` (catch-all) | Identity |

### Headers Forwarded to Downstream

When a valid JWT is present, the gateway extracts claims and forwards:

| Header | Claim | Description |
|--------|-------|-------------|
| `X-User-Id` | `uid` | User UUID |
| `X-User-Username` | `uname` | Username |
| `X-User-Roles` | `roles` | Comma-separated roles |
| `X-Client-Id` | `aud` | Client ID from JWT audience |
| `X-Session-Id` | `sid` | Session UUID |

---

## Quick Smoke Test

```bash
# 1. Register a user through the gateway
curl -X POST http://localhost:8080/api/v1/identity/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","username":"testuser","password":"Test@1234","clientId":"insighttube"}'

# 2. Login
curl -X POST http://localhost:8080/api/v1/identity/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test@1234","clientId":"insighttube"}'

# 3. Use the accessToken from login response for protected endpoints
TOKEN="<paste accessToken here>"

# 4. Access protected endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/identity/auth/logout-all
```

---

## Seeded Data

The database is seeded on first migration (V3):

| Entity | ID | Credentials |
|--------|-----|-------------|
| Client | `insighttube` | — |
| Admin User | `admin@insighttube.com` | username: `admin`, password: `password` |

---

## Troubleshooting

### PostgreSQL: "invalid value for parameter TimeZone: Asia/Calcutta"

**Fix:** Always start services with `JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"`.

### Gateway returns "No static resource"

**Fix:** Ensure the gateway is using `spring.cloud.gateway.server.webflux.routes` (not `spring.cloud.gateway.routes` — the namespace changed in Spring Cloud 5.0.x).

### Identity Service: "Client not found" on register

**Fix:** Run the V3 migration or manually insert the `insighttube` client into the `clients` table.

### Gateway: 401 on all requests

**Fix:** Ensure `jwt.secret` in gateway matches the Identity Service secret. Both default to the same value, but environment variables can override independently.

---

## Project File Locations

```
ai-platform-workspace/
├── applications/
│   └── insighttube/
│       ├── gateway/           ← Gateway (port 8080)
│       │   ├── src/main/java/.../gateway/
│       │   │   ├── GatewayApplication.java
│       │   │   ├── filter/JwtValidationGatewayFilterFactory.java
│       │   │   └── error/GatewayExceptionHandler.java
│       │   └── src/main/resources/application.yaml
│       └── tube-service/      ← (planned)
├── platform/
│   └── identity-service/      ← Identity Service (port 8081)
│       ├── src/main/java/.../identity/
│       │   ├── controller/    (Auth, Client, Health, Role)
│       │   ├── service/       (interfaces + impl/)
│       │   ├── security/      (JWT filter, token generator)
│       │   ├── entity/        (User, Client, Role, etc.)
│       │   └── config/        (Security, Flyway, RateLimit)
│       └── src/main/resources/
│           ├── application.yaml
│           ├── application-local.yml
│           └── db/migration/  (V1, V2, V3)
└── docs/
    ├── startup-guide.md       ← This file
    ├── architecture.md
    ├── flow.md
    ├── roadmap.md
    └── ...
```
