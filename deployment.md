# Deployment Guide

> Where and how to host the AI Platform and InsightTube — from development to production.

---

## Current Stack Requirements

| Service | Runtime | RAM (min) | Disk | Notes |
|---------|---------|-----------|------|-------|
| Frontend | Node.js 18+ | 256 MB | 50 MB | Static build, no server state |
| Gateway | Java 17+ | 512 MB | 200 MB | Spring Cloud Gateway |
| Identity Service | Java 17+ | 512 MB | 200 MB | Spring Boot + PostgreSQL |
| Tube Service | Java 17+ | 1 GB | 500 MB | Ingest + chat + pgvector |
| Transcript Service | Python 3.10+ | 256 MB | 100 MB | FastAPI + Redis |
| PostgreSQL | — | 512 MB | 5 GB | With pgvector extension |
| Redis | — | 128 MB | 1 GB | Transcript caching |

**Total minimum**: ~3.5 GB RAM, ~7 GB disk

This is a **low-processing project** — no GPU required, no heavy compute. The AI calls go to external LLM APIs (Gemini), so the platform itself is just routing and storage.

---

## Deployment Options

### Option 1: Single VPS (Recommended for Start)

**Best for**: Early stage, low traffic, budget-conscious

| Provider | Spec | Monthly Cost | Why |
|----------|------|-------------|-----|
| **Hetzner CX32** | 4 vCPU, 8 GB RAM, 80 GB | ~€8/mo | Best price-performance in EU |
| **DigitalOcean Basic** | 4 vCPU, 8 GB RAM, 160 GB | ~$40/mo | Simple, good docs |
| **AWS Lightsail** | 4 vCPU, 8 GB RAM | ~$40/mo | AWS ecosystem access |
| **Vultr Regular** | 4 vCPU, 8 GB RAM | $40/mo | Good global coverage |

**Why a single VPS?**
- Cheapest option for this scale
- All services communicate on localhost (lowest latency)
- Simple debugging — everything in one place
- Docker Compose handles service orchestration
- No need for Kubernetes complexity at this stage

**Setup**:

```bash
# On the VPS
# 1. Install Docker + Docker Compose
curl -fsSL https://get.docker.com | sh

# 2. Clone the repo
git clone https://github.com/Diwakar-Dhawal/ai-platform-workspace.git
cd ai-platform-workspace

# 3. Create .env with production values
cat > .env << EOF
POSTGRES_DB=ai_platform
POSTGRES_USER=app_user
POSTGRES_PASSWORD=<strong-password>
PGADMIN_DEFAULT_EMAIL=admin@example.com
PGADMIN_DEFAULT_PASSWORD=<strong-password>
EOF

# 4. Start infrastructure
docker-compose up -d

# 5. Build and run each service
# (Use Dockerfiles in each service directory)
```

---

### Option 2: Docker Compose (All-in-One Container)

**Best for**: Simplified deployment, reproducible environments

Create a root `docker-compose.prod.yml` that runs everything:

```yaml
# docker-compose.prod.yml
services:
  postgres:
    image: pgvector/pgvector:pg17
    environment:
      POSTGRES_DB: ai_platform
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes
    ports:
      - "6379:6379"

  identity-service:
    build: ./platform/identity-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/ai_platform
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      IDENTITY_JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8081:8081"
    depends_on:
      - postgres

  tube-service:
    build: ./applications/insighttube/tube-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tube_service
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      IDENTITY_JWT_SECRET: ${JWT_SECRET}
      TRANSCRIPT_SERVICE_URL: http://transcript-service:8085
      AI_PLATFORM_URL: http://host.docker.internal:8084/ai-platform
    ports:
      - "8082:8082"
    depends_on:
      - postgres
      - identity-service

  transcript-service:
    build: ./applications/insighttube/transcript-service
    environment:
      REDIS_URL: redis://redis:6379
    ports:
      - "8085:8085"
    depends_on:
      - redis

  gateway:
    build: ./applications/insighttube/gateway
    environment:
      IDENTITY_JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"
    depends_on:
      - identity-service
      - tube-service

  frontend:
    build:
      context: ./applications/insighttube/frontend
      dockerfile: Dockerfile
    ports:
      - "3000:3000"
    depends_on:
      - gateway

volumes:
  postgres-data:
  redis-data:
```

**Why Docker Compose over Kubernetes?**
- This is a low-traffic app — Kubernetes overhead isn't justified
- Docker Compose is simpler to debug and maintain
- One `docker-compose up` starts everything
- Easy to migrate to Kubernetes later if needed

---

### Option 3: Cloud PaaS (Easiest, More Expensive)

**Best for**: Zero DevOps, fast iteration

| Platform | Best For | Cost Estimate |
|----------|----------|--------------|
| **Railway** | Full-stack, auto-deploy from GitHub | ~$20-50/mo |
| **Render** | Free tier for small apps, auto-scaling | ~$25-75/mo |
| **Fly.io** | Global edge deployment | ~$20-40/mo |
| **Vercel** (frontend only) + Railway (backend) | Split deployment | ~$20-40/mo |

**Railway approach** (simplest):

```yaml
# railway.json
{
  "build": {
    "builder": "DOCKERFILE",
    "dockerfilePath": "Dockerfile"
  },
  "deploy": {
    "startCommand": "java -jar target/*.jar",
    "healthcheckPath": "/actuator/health"
  }
}
```

- Deploy each service as a separate Railway service
- Use Railway's managed PostgreSQL
- Auto-deploy on push to `main`

**Why PaaS over VPS?**
- Zero server management
- Auto-scaling (though rarely needed for this project)
- Built-in HTTPS, logs, metrics
- Faster iteration cycle

**Why NOT PaaS (for this project)?**
- More expensive at scale
- Less control over networking
- SSE (Server-Sent Events) can be tricky on some PaaS platforms
- Multi-service architecture means multiple billable services

---

### Option 4: Vercel (Frontend) + Cloud (Backend)

**Best for**: Global CDN for frontend, separate backend hosting

- **Frontend**: Deploy to Vercel (free for personal projects)
- **Backend services**: Deploy to Railway, Fly.io, or a VPS

```javascript
// next.config.ts — update for production
const nextConfig = {
  async rewrites() {
    return [
      // In production, point to your deployed backend
      {
        source: "/tube-service/:path*",
        destination: "https://your-backend.railway.app/tube-service/:path*",
      },
      {
        source: "/identity-service/:path*",
        destination: "https://your-backend.railway.app/identity-service/:path*",
      },
    ];
  },
};
```

**Why this split?**
- Vercel's edge network is excellent for frontend performance
- Backend services need more persistent connections (DB, Redis)
- Independent scaling — frontend scales to zero, backend stays up

---

## Recommended Deployment Strategy

Given that this is a **low-processing project** in **early development**:

### Phase 1: Development (Now)
- Local Docker Compose for infrastructure
- Run services locally with `mvnw spring-boot:run`
- Frontend on `localhost:3000`

### Phase 2: Staging
- **Single VPS** (Hetzner CX32 or DigitalOcean 4GB)
- Docker Compose for all services
- Caddy or Nginx as reverse proxy with HTTPS
- Cost: ~€8-40/month

### Phase 3: Production (When Needed)
- **Split deployment**: Vercel (frontend) + VPS or Railway (backend)
- OR **single VPS** with Docker Compose (simpler, still fine at this scale)
- Add monitoring (UptimeRobot, Grafana)
- Cost: ~$20-60/month

### Phase 4: Scale (If Traffic Grows)
- Kubernetes if multiple instances needed
- Separate databases per service
- CDN for static assets
- Consider: AWS ECS, Google Cloud Run, or managed K8s

---

## Why NOT Kubernetes (Yet)

| Factor | Docker Compose | Kubernetes |
|--------|---------------|-----------|
| Setup complexity | Low | High |
| Learning curve | Days | Weeks |
| Cost | VPS price | VPS + overhead |
| Scaling | Manual (add VPS) | Auto-scaling |
| Service discovery | Built-in | Needs config |
|适合 This project? | **Yes** | Overkill |

Kubernetes makes sense when:
- You need horizontal scaling (multiple instances)
- You have 10+ microservices
- You need zero-downtime deployments
- You have a DevOps team

None of these apply yet.

---

## Environment Variables

Create a `.env` file at the workspace root:

```bash
# Database
POSTGRES_DB=ai_platform
POSTGRES_USER=app_user
POSTGRES_PASSWORD=<change-this>

# pgAdmin
PGADMIN_DEFAULT_EMAIL=admin@example.com
PGADMIN_DEFAULT_PASSWORD=<change-this>

# JWT (must be the same across all services)
IDENTITY_JWT_SECRET=<at-least-64-char-random-string>

# YouTube (optional — for playlist/channel metadata)
YOUTUBE_API_KEY=

# AI Platform (when ready)
AI_PLATFORM_SECRET=
```

---

## HTTPS Setup

For production, use Caddy as a reverse proxy:

```bash
# Install Caddy
apt install -y caddy

# Caddyfile
cat > /etc/caddy/Caddyfile << 'EOF'
yourdomain.com {
    reverse_proxy localhost:3000
}

api.yourdomain.com {
    reverse_proxy localhost:8080
}
EOF

# Caddy auto-provisions HTTPS via Let's Encrypt
systemctl restart caddy
```

---

## Database Migrations

The Tube Service uses Flyway for schema management:

```bash
# Run migrations on startup (already configured in application.yaml)
# Or manually:
cd applications/insighttube/tube-service
./mvnw flyway:migrate
```

Identity Service migrations are in:
```
platform/identity-service/src/main/resources/db/migration/
```

---

## Monitoring

For a low-traffic project, minimal monitoring is fine:

| Tool | Purpose | Cost |
|------|---------|------|
| **UptimeRobot** | Uptime monitoring | Free (50 monitors) |
| **Sentry** (free tier) | Error tracking | Free |
| **Grafana + Prometheus** | Metrics (optional) | Free |
| **Docker logs** | Debugging | Free |

```bash
# Quick health checks
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8081/identity-service/health  # Identity
curl http://localhost:8082/tube-service/health  # Tube
curl http://localhost:8085/health  # Transcript
```

---

## Backup Strategy

```bash
# PostgreSQL backup (daily cron)
pg_dump -U app_user ai_platform | gzip > backup_$(date +%Y%m%d).sql.gz

# Redis backup (already persisting with AOF)
# Just backup the redis data volume
docker exec ai-platform-redis redis-cli BGSAVE
```

---

## Summary

| Stage | Where | Cost | Complexity |
|-------|-------|------|-----------|
| Development | Local | $0 | Low |
| Staging | Single VPS | €8-40/mo | Low |
| Production | VPS or PaaS | $20-60/mo | Medium |
| Scale | K8s or multi-region | $100+/mo | High |

**Start with a single VPS.** It's the simplest, cheapest, and most educational option for this stage of the project. Migrate to more complex infrastructure only when the project demands it.
