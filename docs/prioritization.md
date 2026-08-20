# Feature Prioritization Rationale

**Last updated:** 2026-08-20

This document explains why features were implemented in a specific order, the criteria used for prioritization, and the trade-offs considered.

---

## Prioritization Criteria

Features were prioritized based on:

1. **Unblocking dependencies** — What must exist before other features can be built?
2. **Security criticality** — What gaps pose immediate security risks?
3. **Development velocity** — What enables faster development of subsequent features?
4. **Production readiness** — What's required before the service can be deployed?
5. **User experience** — What improves the developer/user workflow?

---

## Priority Levels

| Level | Description | Example |
|-------|-------------|---------|
| **P0** | Critical — Blocks everything else | Registration, Login |
| **P1** | High — Required for Phase 2 | Client Management, Role Assignment |
| **P2** | Medium — Production readiness | Rate Limiting, Email Verification |
| **P3** | Low — Nice to have | API Documentation, Dockerfile |

---

## Implementation Order & Rationale

### Phase 1: Core Authentication (Already Done)

| Feature | Priority | Rationale |
|---------|----------|-----------|
| Registration | P0 | Foundation — users must exist |
| Login | P0 | Foundation — authentication required |
| JWT Tokens | P0 | Stateless auth for microservices |
| Refresh Tokens | P0 | Session management |
| Roles | P0 | Authorization foundation |
| Clients | P0 | Multi-application support |

**Why P0:** Without these, nothing works. Every other feature depends on authentication.

---

### Phase 2: Identity Service Completion (Completed)

| Feature | Priority | Rationale |
|---------|----------|-----------|
| Client Management API | P1 | Unblocks Phase 2 (Gateway) — clients must be creatable programmatically |
| Admin Role Assignment API | P1 | Unblocks authorization — admin must assign roles |
| Rate Limiting | P2 | Security — prevents brute force attacks |
| Email Verification | P2 | Production readiness — verify user emails |
| Password Reset | P2 | User experience — users can recover accounts |
| API Documentation | P3 | Developer experience — easier integration |
| Dockerfile | P3 | Deployment — containerized deployment |

---

## Detailed Rationale

### 1. Client Management API (P1)

**Why it came first:**
- Currently, clients are created via raw SQL — impossible for frontend integration
- Gateway integration (Phase 2) requires clients to exist
- Every other feature (registration, login) requires a valid `clientId`
- Without this, developers can't test the full flow

**What it enables:**
- Frontend can register new applications
- Automated testing can create test clients
- Gateway can validate client existence

**Trade-offs:**
- ✅ Unblocks Phase 2
- ✅ Enables automated testing
- ❌ More API surface to secure and maintain

---

### 2. Admin Role Assignment API (P1)

**Why it came second:**
- DTOs already existed (`AssignRolesRequest`, `RemoveRolesRequest`)
- Roles are assigned on registration, but no way to manage them
- Authorization is critical for multi-application security
- Required for admin workflows

**What it enables:**
- Admins can assign/remove roles
- User management workflows
- Audit trail for role changes

**Trade-offs:**
- ✅ Complete authorization management
- ✅ Uses existing DTOs
- ❌ Requires ADMIN role (must be seeded)

---

### 3. Rate Limiting (P2)

**Why it came third:**
- Security gap identified in review
- Auth endpoints are vulnerable to brute force
- Simple to implement (in-memory)
- No external dependencies required

**What it enables:**
- Protection against credential stuffing
- Protection against DDoS (basic)
- Configurable limits per endpoint

**Trade-offs:**
- ✅ Simple, no dependencies
- ✅ Configurable
- ❌ Single-instance only (not distributed)
- ❌ Lost on restart (in-memory)

**Future:**
- Redis-based rate limiting for production
- Distributed rate limiting across instances

---

### 4. Email Verification (P2)

**Why it came fourth:**
- Required for production (verify real emails)
- No email service configured yet
- Console logging is sufficient for development
- Enables frontend integration

**What it enables:**
- Full registration flow
- Email verification workflow
- Future: real email service integration

**Trade-offs:**
- ✅ Full flow testable
- ✅ No external dependencies
- ❌ No actual emails sent (console only)
- ❌ Requires manual token copy-paste

---

### 5. Password Reset (P2)

**Why it came fifth:**
- Required for production (user recovery)
- Uses same token mechanism as email verification
- Single-use tokens with expiry
- Invalidates all sessions on reset

**What it enables:**
- User account recovery
- Security (force password change)
- Session management (logout everywhere)

**Trade-offs:**
- ✅ Complete account recovery flow
- ✅ Invalidates all sessions (secure)
- ❌ No actual emails sent (console only)

---

### 6. API Documentation (P3)

**Why it came sixth:**
- Nice to have for development
- Can be added later without breaking changes
- SpringDoc/OpenAPI is easy to integrate

**What it enables:**
- Auto-generated API docs
- Swagger UI for testing
- Frontend integration guide

**Trade-offs:**
- ✅ Developer experience
- ✅ Auto-generated from code
- ❌ Not required for functionality

---

### 7. Dockerfile (P3)

**Why it came last:**
- Deployment is important but not blocking
- Can be added when ready to deploy
- Local development works without Docker

**What it enables:**
- Containerized deployment
- Docker Compose integration
- CI/CD pipeline

**Trade-offs:**
- ✅ Production deployment
- ✅ Consistent environments
- ❌ Not required for development

---

## What Was NOT Prioritized (And Why)

### Asymmetric JWT Keys (TD-014)

**Why deferred:**
- Current HMAC approach works for single-service
- Asymmetric keys add complexity (key management, rotation)
- Required before second application gateway (not yet)
- Can be added later without breaking changes

**When to implement:**
- Before InsightTube gateway goes to production
- When multiple gateways need to validate tokens

---

### Account Lockout (TD-003)

**Why deferred:**
- Rate limiting provides basic protection
- Account lockout requires user notification (email)
- More complex UX (locked accounts need unlock flow)
- Can be added later

**When to implement:**
- When production has real users
- When email service is configured

---

### Session Management UI (TD-020)

**Why deferred:**
- No frontend yet
- Users can't view/revoke sessions
- Nice to have for user experience
- Can be added when frontend is built

**When to implement:**
- Phase 4 (Frontend Integration)
- When user dashboard is implemented

---

### Correlation IDs (TD-021)

**Why deferred:**
- Useful for distributed tracing
- Not critical for single-service
- Can be added with gateway integration
- More valuable with multiple services

**When to implement:**
- Phase 2 (Gateway Integration)
- When cross-service tracing is needed

---

## Decision Matrix

| Feature | Security | Velocity | Production | UX | Priority |
|---------|----------|----------|------------|-----|----------|
| Client Management | ⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ | **P1** |
| Role Assignment | ⭐⭐ | ⭐⭐ | ⭐⭐ | ⭐⭐ | **P1** |
| Rate Limiting | ⭐⭐⭐ | ⭐ | ⭐⭐⭐ | ⭐ | **P2** |
| Email Verification | ⭐⭐ | ⭐⭐ | ⭐⭐⭐ | ⭐⭐ | **P2** |
| Password Reset | ⭐⭐ | ⭐ | ⭐⭐⭐ | ⭐⭐⭐ | **P2** |
| API Documentation | ⭐ | ⭐⭐ | ⭐ | ⭐⭐⭐ | **P3** |
| Dockerfile | ⭐ | ⭐ | ⭐⭐⭐ | ⭐ | **P3** |

**⭐ = Low impact, ⭐⭐ = Medium impact, ⭐⭐⭐ = High impact**

---

## Current Status

### Completed (All Phases)

| Feature | Status | Tests |
|---------|--------|-------|
| Client Management API | ✅ Complete | 15 tests |
| Admin Role Assignment API | ✅ Complete | 10 tests |
| Rate Limiting | ✅ Complete | Configurable |
| Email Verification | ✅ Complete | Console mode |
| Password Reset | ✅ Complete | Console mode |
| API Documentation (Swagger) | ✅ Complete | Accessible at `/swagger-ui.html` |

### Remaining in Phase 1

| Feature | Priority | Estimated Effort |
|---------|----------|------------------|
| Dockerfile | P3 | Small |
| Refresh Token Hardening | P2 | Medium |
| Session Management | P2 | Medium |

---

## Summary

**Why this order:**
1. **Client Management first** — Unblocks everything
2. **Role Assignment second** — Complete authorization
3. **Rate Limiting third** — Security basics
4. **Email/Password Reset fourth** — User experience
5. **Documentation fifth** — Developer experience
6. **Dockerfile last** — Nice to have

**Key insight:** Dependencies drive priority. Client Management was P1 because it unblocks Phase 2 (Gateway), which unblocks Phase 3 (Tube Service), which unblocks Phase 4 (Frontend).
