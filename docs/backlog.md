# Living Technical Debt Backlog

This backlog is based on the current repository and its documented rules: platform services remain application-neutral, every application owns its gateway and backend, and services communicate through APIs rather than shared Java modules.

Status meanings:

- **Accepted:** aligned remediation that should be planned in the named phase.
- **Deferred:** valid debt intentionally postponed until the named phase or scale threshold.
- **Rejected:** reserved for proposals that would violate an ADR or CODEX.md. No current debt item is marked rejected; architecture-changing alternatives are excluded rather than added to this backlog.

## Must Fix Before Production

### TD-001: Replace committed secrets and local production defaults

**Status:** Accepted  
**Description:** Identity configuration contains database credentials and a known JWT signing secret; Docker Compose also contains default PostgreSQL and pgAdmin credentials.  
**Why it matters:** Anyone with repository access can authenticate to default environments or forge HMAC-signed access tokens.  
**Impact:** Security, deployment, operations.  
**Complexity:** M  
**Dependencies:** Environment and secret-management approach; TD-007.  
**Recommended implementation phase:** Phase 1 - Finish Identity Service.  
**Affects existing APIs:** No, except token invalidation when the signing key rotates.

### TD-002: Enforce JWT issuer, audience, and required claims at every resource boundary

**Status:** Accepted  
**Description:** Identity issues `iss`, `aud`, token type, user, session, and version claims, but its filter only verifies the signature, expiry, type, and version. The gateway has no JWT validation.  
**Why it matters:** A token minted for one application can be accepted by another consumer sharing the validator/key, breaking client isolation.  
**Impact:** Security, multi-application scalability, authorization.  
**Complexity:** M  
**Dependencies:** Final client identifiers and JWT contract; TD-006; TD-010.  
**Recommended implementation phase:** Phase 1 for the token contract, Phase 2 for gateway enforcement.  
**Affects existing APIs:** Yes, invalid or incorrectly scoped tokens will be rejected.

### TD-003: Make account state and authorization changes revoke active access

**Status:** Accepted  
**Description:** Login rejects non-active users, but `CustomUserDetails` reports every account as enabled and non-locked. Token authorities are also copied from the JWT, so future role removal will not take effect until token expiry unless sessions are invalidated.  
**Why it matters:** Disabled, banned, deleted, or de-privileged users can retain access.  
**Impact:** Security, authorization correctness.  
**Complexity:** M  
**Dependencies:** Account-status and role-administration lifecycle; TD-006.  
**Recommended implementation phase:** Phase 1 - Finish Identity Service.  
**Affects existing APIs:** Yes, affected sessions will receive authorization failures after state/role changes.

### TD-004: Harden refresh-token storage, rotation, and replay handling

**Status:** Accepted  
**Description:** Refresh tokens are stored in plaintext. Rotation is not protected against concurrent refresh requests, and reuse of a revoked token does not revoke the corresponding session.  
**Why it matters:** A database leak gives direct session access, while token theft or racing refreshes can create multiple valid descendants.  
**Impact:** Security, session management, data integrity.  
**Complexity:** L  
**Dependencies:** Database migration strategy; TD-007.  
**Recommended implementation phase:** Phase 1 - Finish Identity Service.  
**Affects existing APIs:** No request-shape change is required; replayed refresh tokens will begin failing and may revoke their session.

### TD-005: Define and enforce precise session/logout semantics

**Status:** Accepted  
**Description:** Single-session logout revokes refresh tokens but does not invalidate the associated access token, and logout requires a valid access token despite receiving a refresh token. Refresh rotation has no absolute session lifetime.  
**Why it matters:** The advertised logout behavior differs from actual access-token validity, users cannot reliably end sessions after access-token expiry, and persistent sessions lack a bounded lifetime.  
**Impact:** Security, user experience, session management.  
**Complexity:** L  
**Dependencies:** TD-004 and the agreed session policy.  
**Recommended implementation phase:** Phase 1 - Finish Identity Service.  
**Affects existing APIs:** Potentially, depending on the chosen logout authentication contract and session metadata returned.

### TD-006: Establish client lifecycle and client-scoped authorization administration

**Status:** Accepted  
**Description:** Registration/login require a `Client`, but no client is seeded or managed. Role assignment/removal request DTOs exist without administration endpoints or a defined authorization model.  
**Why it matters:** The current authentication flow cannot operate without manual database mutation, and future admin behavior risks bypassing ADR 004.  
**Impact:** Functional correctness, authorization, multi-application onboarding.  
**Complexity:** L  
**Dependencies:** Client ownership, registration policy, and administration API contract.  
**Recommended implementation phase:** Phase 1 - Finish Identity Service.  
**Affects existing APIs:** Yes, introduces documented administration endpoints and may clarify client registration behavior.

### TD-007: Replace runtime schema updates with versioned migrations and complete environment profiles

**Status:** Accepted  
**Description:** Identity uses `ddl-auto: update`, SQL logging is enabled, and local/docker/production profile files are empty.  
**Why it matters:** Automatic schema mutation is not auditable or safe for deployed environments, and configuration is not reproducible across environments.  
**Impact:** Deployment reliability, security, data integrity.  
**Complexity:** M  
**Dependencies:** TD-001; database ownership decision for Identity.  
**Recommended implementation phase:** Phase 1 - Finish Identity Service.  
**Affects existing APIs:** No.

### TD-008: Add security, integration, and concurrency test coverage

**Status:** Accepted  
**Description:** Identity and gateway each contain only a context-load test. Authentication, JWT validation, role isolation, refresh rotation, logout, route behavior, and failure cases are untested.  
**Why it matters:** Security regressions and contract breaks will reach consumers without detection.  
**Impact:** Security, reliability, maintainability.  
**Complexity:** L  
**Dependencies:** TD-002 through TD-007 define the expected behavior.  
**Recommended implementation phase:** Phase 1 for Identity; Phase 2 for gateway behavior.  
**Affects existing APIs:** No.

### TD-009: Implement the documented InsightTube gateway security boundary

**Status:** Accepted  
**Description:** The gateway currently forwards the identity route only. It does not validate JWTs, define CORS, rate-limit requests, or provide structured gateway logging, although these are documented gateway responsibilities.  
**Why it matters:** Calling the gateway a security boundary before it enforces the identity contract creates a false sense of protection.  
**Impact:** Security, application isolation, observability.  
**Complexity:** L  
**Dependencies:** TD-002; final InsightTube route and deployment topology.  
**Recommended implementation phase:** Phase 2 - InsightTube Gateway.  
**Affects existing APIs:** Yes, protected requests will require valid, InsightTube-audience JWTs and browser origins will be constrained.

### TD-010: Publish versioned API contracts before service integration

**Status:** Accepted  
**Description:** `docs/api-contracts.md` is empty. Identity endpoints, JWT claims, gateway route rewriting, and future platform-service APIs have no canonical consumer contract.  
**Why it matters:** API-only integration is required by ADR 003; without contracts, consumers will couple to implementation details.  
**Impact:** Integration reliability, maintainability, multi-service delivery.  
**Complexity:** M  
**Dependencies:** TD-002, TD-005, and TD-006.  
**Recommended implementation phase:** Phase 1 for Identity, then each service phase before consumer integration.  
**Affects existing APIs:** No immediate runtime change; it formalizes existing and planned behavior.

### TD-011: Provide a deployable workspace configuration

**Status:** Accepted  
**Description:** Compose provisions only PostgreSQL and pgAdmin. There are no service containers, production deployment configuration, readiness strategy, or documented inter-service addressing.  
**Why it matters:** Independently deployable services are a documented principle, but the repository cannot currently demonstrate that deployment model.  
**Impact:** Operations, integration, delivery confidence.  
**Complexity:** L  
**Dependencies:** TD-001, TD-007, and service-specific health/readiness definitions.  
**Recommended implementation phase:** Phase 1 for Identity deployment; Phase 2 for gateway deployment.  
**Affects existing APIs:** No.

### TD-012: Remove non-project binary artifacts and protect the repository root

**Status:** Accepted  
**Description:** `applications/insighttube` contains an installer executable and a Windows metadata binary unrelated to the documented application. The root `.gitignore` is empty.  
**Why it matters:** Accidental binary commits inflate the repository, complicate reviews, and can introduce supply-chain or licensing risk.  
**Impact:** Repository hygiene, security review, developer experience.  
**Complexity:** S  
**Dependencies:** None.  
**Recommended implementation phase:** Phase 1 - workspace hygiene.  
**Affects existing APIs:** No.

## Must Fix Before Next Service

### TD-013: Correct client-scoped authorization data returned by Identity

**Status:** Accepted  
**Description:** `UserMapper` returns roles from every client assignment, even when the login/token response is for one client.  
**Why it matters:** This leaks cross-application authorization information and encourages clients to treat globally returned roles as current-client permissions.  
**Impact:** Client isolation, API correctness, frontend safety.  
**Complexity:** S  
**Dependencies:** TD-006 and TD-010.  
**Recommended implementation phase:** Phase 1 - Finish Identity Service.  
**Affects existing APIs:** Yes, `AuthResponse.user.roles` becomes correctly client-scoped.

### TD-014: Move JWT validation toward asymmetric keys and planned rotation

**Status:** Accepted  
**Description:** The current HMAC design requires every validator to possess the signing secret. This does not safely support independently deployed gateways for multiple applications.  
**Why it matters:** Compromise of any validator could enable token minting for the whole platform.  
**Impact:** Security, multi-application scalability, operational resilience.  
**Complexity:** L  
**Dependencies:** TD-001, TD-002, and an identity key-distribution contract.  
**Recommended implementation phase:** Phase 1 contract design; complete before a second application gateway is introduced.  
**Affects existing APIs:** Yes, consumers must validate against the published verification key set.

### TD-015: Resolve the documentation source of truth and naming drift

**Status:** Accepted  
**Description:** Current documents use both `tube-service` and `backend`, and both `chat-service` and `chat-platform`. ADR 001 is duplicated, older tracked documents are deleted in the worktree, and new architecture documents are untracked.  
**Why it matters:** Service ownership, deployment names, API routes, and future contracts will diverge if teams use different references.  
**Impact:** Architecture governance, onboarding, maintainability.  
**Complexity:** M  
**Dependencies:** Product owner confirmation of canonical names.  
**Recommended implementation phase:** Before Phase 2 - InsightTube Gateway.  
**Affects existing APIs:** Potentially, if route or service identifiers are renamed before publication.

### TD-016: Define the application-owned Tube Service boundary before implementing OAuth or business data

**Status:** Accepted  
**Description:** `tube-service` is a placeholder, while the documentation assigns OAuth, external APIs, business logic, and application data to the backend.  
**Why it matters:** Beginning OAuth or YouTube work in Identity, gateway, or AI Platform would violate the documented boundaries and create difficult migration work.  
**Impact:** Architecture compliance, data ownership, future maintainability.  
**Complexity:** M  
**Dependencies:** TD-010 and TD-015.  
**Recommended implementation phase:** Phase 3 - Tube Service, before implementation begins.  
**Affects existing APIs:** No existing API; it defines the contracts to introduce.

### TD-017: Add gateway routing and downstream failure contracts before frontend or Tube Service integration

**Status:** Accepted  
**Description:** The gateway has a single localhost identity route, TRACE logging, and no Tube Service route, timeout policy, downstream error translation, or route tests.  
**Why it matters:** The frontend must use the application gateway, and unbounded or undocumented proxy behavior makes all consumer failures harder to diagnose.  
**Impact:** Integration reliability, operations, user experience.  
**Complexity:** M  
**Dependencies:** TD-009, TD-010, TD-015.  
**Recommended implementation phase:** Phase 2 - InsightTube Gateway.  
**Affects existing APIs:** Yes, establishes the public InsightTube route surface and its error behavior.

### TD-018: Remove production test endpoints and standardize authorization errors

**Status:** Accepted  
**Description:** Identity exposes `/test/*` endpoints in production source, including a public endpoint. `ForbiddenException` also uses the `UNAUTHORIZED` error code despite producing HTTP 403.  
**Why it matters:** Test routes increase the deployed attack surface, and inconsistent error semantics make gateway/frontend authorization handling unreliable.  
**Impact:** Security, API consistency, maintainability.  
**Complexity:** S  
**Dependencies:** TD-008 for replacement coverage.  
**Recommended implementation phase:** Phase 1 - Finish Identity Service.  
**Affects existing APIs:** Yes, removes non-production endpoints and corrects a response error code.

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

## Nice to Have

### TD-023: Remove unused code and align package/component conventions

**Status:** Deferred  
**Description:** `RoleMapper` is empty, `TestTZ` is production source, `UserMapper` is an injected Spring component with static methods and a private constructor, and `HealthServiceImpl` uses field injection despite the documented constructor-injection standard.  
**Why it matters:** These inconsistencies increase cognitive load and weaken the coding standards that future services are expected to follow.  
**Impact:** Maintainability, onboarding, coding consistency.  
**Complexity:** S  
**Dependencies:** None.  
**Recommended implementation phase:** Phase 1 cleanup, after production blockers.  
**Affects existing APIs:** No.

### TD-024: Replace generated starter documentation with repository-specific onboarding

**Status:** Deferred  
**Description:** The root `README.md` is empty and service `HELP.md` files are generated framework documentation.  
**Why it matters:** Contributors do not have one concise entry point for local setup, service ownership, commands, or API references.  
**Impact:** Developer experience, onboarding, delivery speed.  
**Complexity:** S  
**Dependencies:** TD-011 and TD-015.  
**Recommended implementation phase:** Phase 1 workspace hygiene.  
**Affects existing APIs:** No.

### TD-025: Normalize textual documentation encoding and terminology

**Status:** Deferred  
**Description:** Several documents contain malformed symbols and arrows, and `project.txt` still describes Identity registration/JWT work as future even though it exists.  
**Why it matters:** It reduces confidence in the documentation and makes architecture onboarding less clear.  
**Impact:** Documentation quality, maintainability.  
**Complexity:** S  
**Dependencies:** TD-015.  
**Recommended implementation phase:** Alongside the documentation source-of-truth cleanup.  
**Affects existing APIs:** No.

### TD-026: Remove redundant framework dependencies and generated project metadata

**Status:** Deferred  
**Description:** Identity declares both the web starter and the Web MVC starter, and both implemented services retain IDE project metadata in the workspace.  
**Why it matters:** The effect is currently low, but dependency/metadata noise makes builds and repository conventions harder to maintain.  
**Impact:** Build hygiene, developer experience.  
**Complexity:** S  
**Dependencies:** None.  
**Recommended implementation phase:** Routine maintenance after Phase 2.  
**Affects existing APIs:** No.
