# Living Technical Debt Backlog

This backlog is based on the current repository and its documented rules: platform services remain application-neutral, every application owns its gateway and backend, and services communicate through APIs rather than shared Java modules.

Status meanings:

- **Done:** Fully resolved. No further work required.
- **Accepted:** aligned remediation that should be planned in the named phase.
- **Partial:** Work has begun or some aspects are resolved, but the item is not fully complete.
- **Deferred:** valid debt intentionally postponed until the named phase or scale threshold.
- **Rejected:** reserved for proposals that would violate an ADR or CODEX.md. No current debt item is marked rejected; architecture-changing alternatives are excluded rather than added to this backlog.

---

## Must Fix Before Production

### TD-001: Replace committed secrets and local production defaults

**Status:** Partial
**Description:** Identity service profiles (docker, prod) now use environment variables exclusively. The local profile uses env vars with development defaults. However, `docker-compose.yml` still contains hardcoded default PostgreSQL and pgAdmin credentials.
**Done:**
- `application-docker.yml` requires `${IDENTITY_DB_URL}`, `${IDENTITY_DB_USERNAME}`, `${IDENTITY_DB_PASSWORD}`, `${IDENTITY_JWT_SECRET}` — no defaults
- `application-prod.yml` requires all credentials — no defaults
- `application-local.yml` uses env vars with sensible local defaults
**Remaining:**
- `docker-compose.yml` still contains hardcoded `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `PGADMIN_DEFAULT_EMAIL`, `PGADMIN_DEFAULT_PASSWORD`
**Why it matters:** Anyone with repository access can authenticate to default environments or forge HMAC-signed access tokens.
**Impact:** Security, deployment, operations.
**Complexity:** S
**Dependencies:** None.
**Recommended implementation phase:** Phase 1 - Finish Identity Service.
**Affects existing APIs:** No, except token invalidation when the signing key rotates.

### TD-002: Enforce JWT issuer, audience, and required claims at every resource boundary

**Status:** Partial
**Description:** Identity now issues `iss`, `aud`, token type, user, session, and version claims. The gateway validates issuer and token type. However, the Identity JWT filter does not validate issuer or audience, and the gateway does not validate audience.
**Done:**
- JWT includes all required claims (`iss`, `aud`, `typ`, `uid`, `uname`, `sid`, `roles`, `tokenVersion`)
- Gateway `JwtValidationGatewayFilterFactory` validates issuer and token type
- Gateway forwards claims as `X-User-*` headers to downstream services
**Remaining:**
- Identity `JwtAuthenticationFilter` does not validate `iss` or `aud` claims
- Gateway does not validate `aud` claim (could accept tokens for other applications)
**Why it matters:** A token minted for one application can be accepted by another consumer sharing the validator/key, breaking client isolation.
**Impact:** Security, multi-application scalability, authorization.
**Complexity:** M
**Dependencies:** Final client identifiers and JWT contract; TD-006; TD-010.
**Recommended implementation phase:** Phase 1 for the token contract, Phase 2 for gateway enforcement.
**Affects existing APIs:** Yes, invalid or incorrectly scoped tokens will be rejected.

### TD-003: Make account state and authorization changes revoke active access

**Status:** Partial
**Description:** Login rejects non-active users. `User.markDeleted()` revokes refresh tokens and increments token version. `logout-all` and `reset-password` also revoke all sessions. However, `CustomUserDetails.isEnabled()` always returns `true` regardless of user status.
**Done:**
- Login checks `UserStatus.ACTIVE` before allowing authentication
- `User.markDeleted()` revokes all refresh tokens
- `logout-all` revokes all refresh tokens + increments token version
- `reset-password` revokes all sessions + increments token version
**Remaining:**
- `CustomUserDetails.isEnabled()` is hardcoded to `true` — does not reflect actual `User.status`
- Role removal does not immediately revoke existing sessions (relies on token expiry or explicit logout-all)
**Why it matters:** Disabled, banned, deleted, or de-privileged users can retain access until their access token expires.
**Impact:** Security, authorization correctness.
**Complexity:** M
**Dependencies:** Account-status and role-administration lifecycle; TD-006.
**Recommended implementation phase:** Phase 1 - Finish Identity Service.
**Affects existing APIs:** Yes, affected sessions will receive authorization failures after state/role changes.

### TD-004: Harden refresh-token storage, rotation, and replay handling

**Status:** Partial
**Description:** Refresh tokens are now rotated (old token revoked, new token issued). Revoked and expired tokens are rejected. However, token values are stored in plaintext and concurrent refresh requests are not protected.
**Done:**
- Refresh token rotation: old token revoked before new one issued
- Revoked token check prevents reuse
- Expired token check with automatic revocation
- Reuse of revoked token during logout revokes entire session
**Remaining:**
- Refresh token values stored in plaintext (not hashed)
- No concurrent refresh protection (two simultaneous requests can both succeed)
- No replay detection beyond revocation check
**Why it matters:** A database leak gives direct session access, while token theft or racing refreshes can create multiple valid descendants.
**Impact:** Security, session management, data integrity.
**Complexity:** L
**Dependencies:** Database migration strategy; TD-007.
**Recommended implementation phase:** Phase 1 - Finish Identity Service.
**Affects existing APIs:** No request-shape change is required; replayed refresh tokens will begin failing and may revoke their session.

### TD-005: Define and enforce precise session/logout semantics

**Status:** Partial
**Description:** Single-session logout revokes all refresh tokens in the session. Logout-all revokes all refresh tokens and increments token version. Password reset revokes all sessions. However, the access token remains valid after logout.
**Done:**
- `POST /auth/logout` revokes all refresh tokens for the session
- `POST /auth/logout-all` revokes all refresh tokens + increments token version
- `POST /auth/reset-password` revokes all sessions + increments token version
- Logout accepts a refresh token (not access token)
**Remaining:**
- Access token remains valid until natural expiry after logout (short-lived tokens mitigate this)
- No absolute session lifetime enforced on refresh token rotation
- No session metadata (device, IP) tracked or exposed
**Why it matters:** The advertised logout behavior differs from actual access-token validity, and persistent sessions lack a bounded lifetime.
**Impact:** Security, user experience, session management.
**Complexity:** L
**Dependencies:** TD-004 and the agreed session policy.
**Recommended implementation phase:** Phase 1 - Finish Identity Service.
**Affects existing APIs:** Potentially, depending on the chosen logout authentication contract and session metadata returned.

### TD-006: Establish client lifecycle and client-scoped authorization administration

**Status:** Done ✅
**Description:** Full client lifecycle and client-scoped role administration are implemented.
**Implemented:**
- `POST /admin/clients` — create client (ADMIN only)
- `GET /admin/clients` — list all clients
- `GET /admin/clients/{id}` — get client by ID
- `GET /admin/clients/by-client-id/{clientId}` — get client by string ID
- `PUT /admin/clients/{id}` — update client
- `DELETE /admin/clients/{id}` — delete client
- `POST /admin/users/{userId}/clients/{clientId}/roles` — assign roles
- `DELETE /admin/users/{userId}/clients/{clientId}/roles` — remove roles
- `GET /admin/users/{userId}/clients/{clientId}/roles` — get user roles for client
- Client seeded via Flyway migration V3
- Registration requires a valid `clientId`
**Why it matters:** The authentication flow now operates without manual database mutation.
**Impact:** Functional correctness, authorization, multi-application onboarding.
**Affects existing APIs:** Yes, introduces documented administration endpoints.

### TD-007: Replace runtime schema updates with versioned migrations and complete environment profiles

**Status:** Done ✅
**Description:** Flyway migrations and complete environment profiles are implemented.
**Implemented:**
- `FlywayConfig.java` — manual Flyway configuration for Spring Boot 4.1+
- `V1__initial_identity_schema.sql` — initial schema
- `V2__add_verification_tokens.sql` — verification token support
- `V3__seed_initial_client_and_admin.sql` — seed data
- `application-local.yml` — local development with env vars + defaults
- `application-docker.yml` — Docker deployment, requires env vars
- `application-prod.yml` — Production, requires env vars, SQL logging disabled
- `application-test.yml` — H2 in-memory with Flyway disabled
- `ddl-auto` removed from non-test profiles
**Why it matters:** Schema changes are auditable and reproducible across environments.
**Impact:** Deployment reliability, security, data integrity.
**Affects existing APIs:** No.

### TD-008: Add security, integration, and concurrency test coverage

**Status:** Partial
**Description:** Integration tests now exist for auth, client, and role operations. However, security-specific, concurrency, and gateway tests are not yet implemented.
**Done:**
- `AuthIntegrationTest.java` — registration, login, token refresh, logout flows
- `ClientIntegrationTest.java` — client CRUD operations
- `RoleIntegrationTest.java` — role assignment and removal
- `TestDataSeeder.java` — test data setup
- H2-based test profile with high rate-limit thresholds
**Remaining:**
- No JWT validation edge-case tests (expired, wrong issuer, wrong audience, revoked)
- No concurrency/rotation race condition tests
- No gateway route/JWT filter tests
- No security-scanning or penetration-style tests
**Why it matters:** Security regressions and contract breaks will reach consumers without detection.
**Impact:** Security, reliability, maintainability.
**Complexity:** L
**Dependencies:** TD-002 through TD-007 define the expected behavior.
**Recommended implementation phase:** Phase 1 for remaining Identity tests; Phase 2 for gateway behavior.
**Affects existing APIs:** No.

### TD-009: Implement the documented InsightTube gateway security boundary

**Status:** Done ✅
**Description:** The gateway now validates JWTs, defines CORS policy, provides structured error responses, and routes all Identity endpoints.
**Implemented:**
- `JwtValidationGatewayFilterFactory` — validates signature, expiry, issuer, token type; forwards claims as headers
- `GatewayExceptionHandler` — consistent JSON error responses matching Identity format
- `application.yaml` — comprehensive route configuration with public/protected route separation
- CORS configured for `localhost:3000` and `localhost:5173`
- All Identity endpoints routed with proper rewrite rules
- Protected routes require JWT validation
- Actuator health and gateway endpoints exposed
**Why it matters:** The gateway is now a real security boundary, not just a passthrough.
**Impact:** Security, application isolation, observability.
**Affects existing APIs:** Yes, protected requests require valid JWTs.

### TD-010: Publish versioned API contracts before service integration

**Status:** Accepted
**Description:** `docs/api-contracts.md` is still empty. Identity endpoints, JWT claims, gateway route rewriting, and future platform-service APIs have no canonical consumer contract.
**Done:**
- ADRs document decisions (001-012)
- Swagger/OpenAPI annotations on Identity controllers
**Remaining:**
- `docs/api-contracts.md` is empty
- No canonical endpoint listing, request/response schemas, error codes, or JWT claims reference
**Why it matters:** API-only integration is required by ADR 003; without contracts, consumers will couple to implementation details.
**Impact:** Integration reliability, maintainability, multi-service delivery.
**Complexity:** M
**Dependencies:** TD-002, TD-005, and TD-006.
**Recommended implementation phase:** Phase 1 for Identity, then each service phase before consumer integration.
**Affects existing APIs:** No immediate runtime change; it formalizes existing and planned behavior.

### TD-011: Provide a deployable workspace configuration

**Status:** Partial
**Description:** Docker Compose provisions PostgreSQL and pgAdmin, but there are no service containers.
**Done:**
- `docker-compose.yml` with PostgreSQL 17 and pgAdmin
- Service profiles configured for Docker deployment (env vars)
**Remaining:**
- No Identity Service container in Compose
- No Gateway container in Compose
- No inter-service health/readiness checks
- No documented startup sequence or inter-service addressing
**Why it matters:** Independently deployable services are a documented principle, but the repository cannot currently demonstrate that deployment model.
**Impact:** Operations, integration, delivery confidence.
**Complexity:** L
**Dependencies:** TD-001, TD-007, and service-specific health/readiness definitions.
**Recommended implementation phase:** Phase 1 for Identity deployment; Phase 2 for gateway deployment.
**Affects existing APIs:** No.

### TD-012: Remove non-project binary artifacts and protect the repository root

**Status:** Done ✅
**Description:** Binary files removed and root `.gitignore` updated.
**Implemented:**
- Removed `ChatGPT Installer.exe`, `Freebuff-0.0.47-win-x64.exe`, `Microsoft.Services.Store.winmd`, `Diwakar_Dhawal_Resume.docx`
- Added root `.gitignore` with rules for binaries, IDE files, build outputs, OS files, and env files
**Why it matters:** Accidental binary commits inflate the repository, complicate reviews, and can introduce supply-chain or licensing risk.
**Impact:** Repository hygiene, security review, developer experience.
**Complexity:** S
**Dependencies:** None.
**Recommended implementation phase:** Phase 1 - workspace hygiene.
**Affects existing APIs:** No.

---

## Must Fix Before Next Service

### TD-013: Correct client-scoped authorization data returned by Identity

**Status:** Done ✅
**Description:** `UserMapper.toResponse()` now accepts a `Client` parameter and filters roles to only those assigned for that client. `TokenServiceImpl.issueTokens()` passes the current client. `RoleServiceImpl` already constructed client-scoped responses manually.
**Why it matters:** This leaks cross-application authorization information and encourages clients to treat globally returned roles as current-client permissions.
**Impact:** Client isolation, API correctness, frontend safety.
**Complexity:** S
**Dependencies:** TD-006 and TD-010.
**Recommended implementation phase:** Phase 1 - Finish Identity Service.
**Affects existing APIs:** Yes, `AuthResponse.user.roles` becomes correctly client-scoped.

### TD-014: Move JWT validation toward asymmetric keys and planned rotation

**Status:** Deferred
**Description:** The current HMAC design requires every validator to possess the signing secret. This does not safely support independently deployed gateways for multiple applications.
**Why it matters:** Compromise of any validator could enable token minting for the whole platform.
**Impact:** Security, multi-application scalability, operational resilience.
**Complexity:** L
**Dependencies:** TD-001, TD-002, and an identity key-distribution contract.
**Recommended implementation phase:** Phase 1 contract design; complete before a second application gateway is introduced.
**Affects existing APIs:** Yes, consumers must validate against the published verification key set.

### TD-015: Resolve the documentation source of truth and naming drift

**Status:** Partial
**Description:** Most architecture documentation is now in place, but `api-contracts.md` is empty and `project.txt` contains outdated information.
**Done:**
- `docs/decisions.md` — comprehensive ADRs (001-012)
- `docs/architecture.md` — architecture rules
- `docs/backlog.md` — this file
- `docs/roadmap.md` — phased development roadmap
- `docs/implementation-log.md` — implementation history
- `docs/philosophy.md` — design philosophy
- Consistent naming: `identity-service`, `gateway`, `tube-service`, `chat-service`
**Remaining:**
- `docs/api-contracts.md` is empty
- `project.txt` still describes Identity registration/JWT work as future even though it exists
**Why it matters:** Service ownership, deployment names, API routes, and future contracts will diverge if teams use different references.
**Impact:** Architecture governance, onboarding, maintainability.
**Complexity:** M
**Dependencies:** Product owner confirmation of canonical names.
**Recommended implementation phase:** Before Phase 2 - InsightTube Gateway.
**Affects existing APIs:** Potentially, if route or service identifiers are renamed before publication.

### TD-016: Define the application-owned Tube Service boundary before implementing OAuth or business data

**Status:** Accepted
**Description:** `tube-service` is a placeholder with only a `.gitkeep` file, while the documentation assigns OAuth, external APIs, business logic, and application data to the backend.
**Why it matters:** Beginning OAuth or YouTube work in Identity, gateway, or AI Platform would violate the documented boundaries and create difficult migration work.
**Impact:** Architecture compliance, data ownership, future maintainability.
**Complexity:** M
**Dependencies:** TD-010 and TD-015.
**Recommended implementation phase:** Phase 3 - Tube Service, before implementation begins.
**Affects existing APIs:** No existing API; it defines the contracts to introduce.

### TD-017: Add gateway routing and downstream failure contracts before frontend or Tube Service integration

**Status:** Done ✅
**Description:** The gateway now has comprehensive routing and error handling.
**Implemented:**
- Full route configuration in `application.yaml` with public/protected route separation
- JWT validation filter on protected routes
- `GatewayExceptionHandler` with consistent JSON error format
- Route rewriting from `/api/v1/identity/*` to `/identity-service/*`
- CORS configuration
- Actuator health endpoint
**Remaining (can be addressed during Phase 2-3):**
- No Tube Service routes (expected — Tube Service not yet implemented)
- No timeout configuration for downstream calls
- No rate limiting at gateway level
**Why it matters:** The gateway has a solid foundation for Identity integration.
**Impact:** Integration reliability, operations, user experience.
**Affects existing APIs:** Yes, establishes the public InsightTube route surface.

### TD-018: Remove production test endpoints and standardize authorization errors

**Status:** Done ✅
**Description:** Test endpoints have been removed and authorization errors are standardized.
**Implemented:**
- No `/test/*` endpoints in production source code
- `ForbiddenException` uses `ErrorCode.FORBIDDEN` (not `UNAUTHORIZED`)
- `GlobalExceptionHandler` handles `AccessDeniedException` with 403 status
- All exception classes use appropriate error codes
**Why it matters:** The deployed attack surface is clean and error semantics are consistent.
**Impact:** Security, API consistency, maintainability.
**Affects existing APIs:** Yes, non-production endpoints removed and error codes corrected.

---

## Can Wait Until Scale

### TD-019: Reduce per-request token parsing and database lookups

**Status:** Deferred
**Description:** The JWT filter parses/verifies the same token repeatedly through separate extractor calls and loads the user from the database for every authenticated request.
**Why it matters:** It raises cryptographic and database load as gateway traffic grows.
**Impact:** Performance, database capacity, latency.
**Complexity:** M
**Dependencies:** TD-002 and the final revocation/status consistency model.
**Recommended implementation phase:** After Phase 2 load testing, before high-volume adoption.
**Affects existing APIs:** No.

### TD-020: Add session visibility, retention, and refresh-token cleanup

**Status:** Deferred
**Description:** Refresh-token rows are retained indefinitely; device name and IP fields are present but never populated or exposed, and users cannot view/revoke individual sessions.
**Why it matters:** Token rotation will grow storage over time and limits account-security support workflows.
**Impact:** Performance, security operations, user experience.
**Complexity:** M
**Dependencies:** TD-004 and TD-005.
**Recommended implementation phase:** After Phase 2, before sustained multi-device usage.
**Affects existing APIs:** Yes, if session-management endpoints are introduced.

### TD-021: Add correlation, audit, rate-limit, and security telemetry

**Status:** Deferred
**Description:** No workspace-wide convention exists for correlation IDs, authentication audit events, rate-limit metrics, or security-alert handling. Gateway TRACE logging is enabled instead of structured production observability.
**Why it matters:** Diagnosing authentication abuse, gateway failures, and cross-service user journeys becomes difficult as services increase.
**Impact:** Operations, security monitoring, supportability.
**Complexity:** L
**Dependencies:** TD-009, TD-011, and TD-017.
**Recommended implementation phase:** Phase 2 baseline; expand when Tube Service is active.
**Affects existing APIs:** No.

### TD-022: Formalize client role namespacing and permission evolution

**Status:** Deferred
**Description:** Role names are globally unique even though assignments are per client, and authorization is role-only.
**Why it matters:** Future applications may need same-named roles with different meaning or finer-grained permissions.
**Impact:** Multi-application scalability, authorization maintainability.
**Complexity:** L
**Dependencies:** TD-006 and evidence from the first additional application.
**Recommended implementation phase:** Before the second application introduces distinct authorization requirements.
**Affects existing APIs:** Yes, if role or permission claims evolve.

---

## Nice to Have

### TD-023: Remove unused code and align package/component conventions

**Status:** Deferred
**Description:** `RoleMapper` is empty, `UserMapper` is a utility class with static methods and private constructor, and `HealthServiceImpl` uses field injection despite the documented constructor-injection standard.
**Remaining issues:**
- `RoleMapper.java` exists but is empty
- `UserMapper` is a static utility class (not a Spring component as originally described)
- `HealthServiceImpl` uses `@RequiredArgsConstructor` with `@Autowired` fields
**Why it matters:** These inconsistencies increase cognitive load and weaken the coding standards that future services are expected to follow.
**Impact:** Maintainability, onboarding, coding consistency.
**Complexity:** S
**Dependencies:** None.
**Recommended implementation phase:** Phase 1 cleanup, after production blockers.
**Affects existing APIs:** No.

### TD-024: Replace generated starter documentation with repository-specific onboarding

**Status:** Accepted
**Description:** The root `README.md` is empty and service `HELP.md` files are generated framework documentation.
**Why it matters:** Contributors do not have one concise entry point for local setup, service ownership, commands, or API references.
**Impact:** Developer experience, onboarding, delivery speed.
**Complexity:** S
**Dependencies:** TD-011 and TD-015.
**Recommended implementation phase:** Phase 1 workspace hygiene.
**Affects existing APIs:** No.

### TD-025: Normalize textual documentation encoding and terminology

**Status:** Partial
**Description:** `project.txt` still describes Identity registration/JWT work as future even though it exists. ADRs are now comprehensive.
**Done:**
- ADRs 001-012 document all major decisions
- Architecture, philosophy, and implementation-log docs are consistent
**Remaining:**
- `project.txt` contains outdated descriptions
- Some older documents may have encoding issues
**Why it matters:** It reduces confidence in the documentation and makes architecture onboarding less clear.
**Impact:** Documentation quality, maintainability.
**Complexity:** S
**Dependencies:** TD-015.
**Recommended implementation phase:** Alongside the documentation source-of-truth cleanup.
**Affects existing APIs:** No.

### TD-026: Remove redundant framework dependencies and generated project metadata

**Status:** Accepted
**Description:** Both implemented services retain IDE project metadata (`.idea/`) in the workspace.
**Why it matters:** The effect is currently low, but dependency/metadata noise makes builds and repository conventions harder to maintain.
**Impact:** Build hygiene, developer experience.
**Complexity:** S
**Dependencies:** None.
**Recommended implementation phase:** Routine maintenance after Phase 2.
**Affects existing APIs:** No.

---

## Summary

| TD | Status | Phase |
|----|--------|-------|
| TD-001 | **Partial** — Identity profiles done, docker-compose needs cleanup | Phase 1 |
| TD-002 | **Partial** — Claims added, gateway validates issuer; Identity filter missing iss/aud | Phase 1 |
| TD-003 | **Partial** — Login rejects inactive; CustomUserDetails.isEnabled() hardcoded true | Phase 1 |
| TD-004 | **Partial** — Rotation works; no token hashing, no concurrent refresh protection | Phase 1 |
| TD-005 | **Partial** — Logout revokes refresh tokens; access token still valid after logout | Phase 1 |
| TD-006 | **Done** ✅ | Phase 1 |
| TD-007 | **Done** ✅ | Phase 1 |
| TD-008 | **Partial** — Auth/client/role integration tests exist; security/concurrency tests missing | Phase 1 |
| TD-009 | **Done** ✅ | Phase 2 |
| TD-010 | **Accepted** — `api-contracts.md` empty | Phase 1 |
| TD-011 | **Partial** — DB provisioned; no service containers | Phase 1-2 |
| TD-012 | **Done** ✅ | Phase 1 |
| TD-013 | **Done** ✅ | Phase 1 |
| TD-014 | **Deferred** | Phase 1+ |
| TD-015 | **Partial** — Most docs done; api-contracts.md empty, project.txt outdated | Phase 1 |
| TD-016 | **Accepted** — tube-service placeholder | Phase 3 |
| TD-017 | **Done** ✅ (Identity routing) | Phase 2 |
| TD-018 | **Done** ✅ | Phase 1 |
