# Phased Development Roadmap

This roadmap follows the documented boundaries: identity owns authentication, each application owns its gateway and business services, and services integrate through APIs rather than shared Java modules.

## Phase 1: Finish Identity Service

**Objective:** Make the identity service production-ready as the shared authentication and authorization platform service.

**Services involved:** `platform/identity-service`, PostgreSQL, Docker configuration.

**Dependencies:** None within the workspace. The service is the prerequisite for authenticated application flows.

**Prerequisites:** Confirm the client-registration and client-administration model, supported environments, secret-management approach, and API contracts.

**Deliverables:**

- Versioned database migrations and a repeatable role/client seeding strategy.
- Complete environment-specific configuration with secrets supplied externally.
- Correct client-scoped role responses and authorization error semantics.
- Client management and administrative role-assignment flows, protected by authorization.
- Automated tests for registration, login, refresh-token rotation, logout, logout-all, role isolation, JWT validation, and failure cases.
- Published identity API contract and operational guidance.

**Risks:** Missing client records currently prevent normal registration/login flows; hardcoded credentials and schema auto-update are unsafe outside local development; incomplete tests make token and authorization regressions likely.

**Estimated complexity:** High.

## Phase 2: InsightTube Gateway

**Objective:** Establish InsightTube's gateway as the application entry point for routing and authentication enforcement.

**Services involved:** `applications/insighttube/gateway`, `platform/identity-service`, future `applications/insighttube/tube-service`.

**Dependencies:** Phase 1 authentication contract, JWT claims, issuer/audience conventions, and client identifier must be stable.

**Prerequisites:** Define InsightTube API route prefixes, CORS policy, service-discovery or deployment addressing strategy, and the gateway's exact JWT-validation responsibility.

**Deliverables:**

- JWT validation aligned with the finalized identity contract.
- Routes for identity and Tube Service, with route rewriting documented and tested.
- Consistent unauthorized/forbidden responses, correlation identifiers, request logging, and health/readiness checks.
- Gateway integration tests covering public routes, protected routes, invalid/expired tokens, and downstream failures.
- Production-safe logging levels and configuration profiles.

**Risks:** Implementing token validation before Phase 1 settles token contracts will create rework; forwarding requests without enforcement violates the documented gateway responsibility; overly broad route rewriting can expose unintended endpoints.

**Estimated complexity:** Medium.

## Phase 3: Tube Service

**Objective:** Build InsightTube's business backend for YouTube integration, user-owned application data, and asynchronous processing.

**Services involved:** `applications/insighttube/tube-service`, `applications/insighttube/gateway`, `platform/identity-service`, PostgreSQL, YouTube APIs, future `platform/ai-platform`.

**Dependencies:** Stable Phase 1 identity claims and Phase 2 protected gateway routes. Google/YouTube OAuth credentials and API quota decisions are required.

**Prerequisites:** Define the Tube Service domain model, data ownership, YouTube OAuth consent/callback design, task lifecycle, retention policy, quota handling, and external API contract.

**Deliverables:**

- Service bootstrap, database schema/migrations, and application-owned profile/data model.
- YouTube OAuth connection flow and secure token storage/refresh handling.
- Video/channel ingestion, metadata retrieval, task orchestration, and user-scoped APIs.
- Error handling, quota/backoff behavior, audit logging, and integration tests with external APIs mocked.
- API contract for the frontend and AI Platform integration.

**Risks:** OAuth redirect topology across frontend, gateway, and service must be designed before implementation; YouTube quotas and long-running analysis require resilient asynchronous workflows; putting profile or Tube data in identity would violate ADR 001.

**Estimated complexity:** High.

## Phase 4: AI Platform

**Objective:** Implement the reusable AI capability platform, exposing APIs that applications consume without sharing internal code.

**Services involved:** `platform/ai-platform`, `applications/insighttube/tube-service`, model-provider APIs, storage and task-processing infrastructure.

**Dependencies:** A concrete first consumer contract from Tube Service and a decision on synchronous versus asynchronous task execution. It does not depend on the InsightTube frontend.

**Prerequisites:** Select model providers, define cost/usage limits, data-classification and retention rules, prompt/version management, task status model, and failure/retry policy.

**Deliverables:**

- AI Platform service foundation with versioned, application-neutral APIs.
- Authenticated request model, request validation, task submission/status APIs, and provider abstraction.
- Reliable processing, retries, idempotency, usage/cost telemetry, and structured failure responses.
- Security controls for application data, operational dashboards/health checks, and contract tests with Tube Service.
- Documentation for consumers, including limits, timeouts, and result schemas.

**Risks:** Building generic abstractions before a validated Tube Service use case can over-engineer the platform; AI cost, latency, provider outages, and sensitive content require explicit controls; direct database coupling to application data would violate ADR 003.

**Estimated complexity:** High.

## Phase 5: Chat Service

**Objective:** Build a platform chat capability with clear ownership, APIs, and lifecycle semantics.

**Services involved:** `platform/chat-service`, `platform/identity-service`, optional `platform/ai-platform`, application gateways and consumers.

**Dependencies:** Phase 1 identity contract. Its initial scope must be defined before selecting whether it depends on AI Platform. It can be developed independently of Tube Service once its consumer contract is approved.

**Prerequisites:** Decide whether chat is provider-neutral infrastructure or an InsightTube-specific capability; define tenancy/application scoping, conversation/message retention, real-time transport, moderation, and notification requirements.

**Deliverables:**

- Chat service domain model, migrations, authentication/authorization integration, and versioned APIs.
- Conversation and message lifecycle with application/tenant isolation.
- Chosen delivery model (request-response, polling, SSE, or WebSocket) with connection and authorization handling.
- Optional AI response orchestration through AI Platform only by API contract.
- Contract, security, load, and failure-mode tests; operational documentation.

**Risks:** The current purpose and consumers are undefined, making premature implementation highly likely to create a second application backend; real-time connections require a scalable session and deployment design; shared conversation data must not be stored in identity.

**Estimated complexity:** High.

## Phase 6: Frontend Integration

**Objective:** Deliver the InsightTube user experience through the application gateway and validated backend contracts.

**Services involved:** `applications/insighttube/frontend`, `applications/insighttube/gateway`, `applications/insighttube/tube-service`, `platform/identity-service`, `platform/ai-platform` where enabled.

**Dependencies:** Phases 1-3 are required for the core authenticated InsightTube experience. Phase 4 is required only for AI-powered features. Phase 5 is required only if chat is included in the initial frontend scope.

**Prerequisites:** Confirm frontend technology, design system, authentication/session handling, OAuth redirect URLs, error/loading conventions, accessibility target, and product workflows.

**Deliverables:**

- Frontend foundation and environment configuration that calls only the InsightTube gateway.
- Registration/login/logout/session refresh flows and protected-route behavior.
- YouTube connection, video/task submission, status/results, and recoverable error experiences.
- AI and chat screens only after their APIs and UX requirements are finalized.
- End-to-end tests for the primary user journey and deployment documentation.

**Risks:** Direct frontend calls to platform services would bypass the documented application-gateway boundary; implementing screens before contracts stabilize will cause churn; OAuth callback handling needs one agreed public URL model.

**Estimated complexity:** High.

## Parallel Work

- Phase 1 database migration, configuration, test, and API-documentation tracks can proceed in parallel once the client model is agreed.
- Phase 2 route/CORS/observability work can start while Phase 1 is finishing, but JWT enforcement must wait for the Phase 1 token contract.
- Phase 3 domain modeling, YouTube API research, mocked integration tests, and API design can proceed before Phase 2 completes; protected end-to-end integration cannot.
- Phase 4 discovery, provider evaluation, cost/security policy, and API design can run alongside Phase 3. Implementation should use a concrete Tube Service contract.
- Phase 5 product and architecture discovery can run alongside Phases 3-4. Implementation can begin after Phase 1 once chat's intended consumers and scope are approved.
- Phase 6 UX research, information architecture, and static design can begin alongside Phases 3-5. Authenticated integration must wait for its dependent APIs.

## Completion Gates

- Do not begin protected gateway enforcement before Phase 1 defines and tests JWT claims, audiences, token versioning, and client-scoped authorization behavior.
- Do not begin Tube Service production OAuth integration before the gateway route and public callback topology are agreed.
- Do not begin AI Platform production implementation before at least one concrete consumer contract, task lifecycle, and data-retention policy are approved.
- Do not begin Chat Service implementation before its platform-versus-application ownership and tenancy model are decided.
- Do not begin frontend integration against unstable service endpoints; consume versioned gateway-exposed contracts only.

## Architectural Risks In The Ordering

- Identity is a critical path: its missing client bootstrap/admin lifecycle and incomplete operational hardening can block all authenticated consumers.
- The existing gateway currently routes but does not enforce JWT validation, so calling it "complete" before its security contract is implemented would create a false security boundary.
- Tube Service should establish the first AI Platform use case, but AI Platform must remain application-neutral; designing it solely around one video workflow risks coupling the platform to InsightTube.
- Chat Service has no documented consumer or product boundary. Scheduling it as a full platform service before its scope is decided risks unnecessary infrastructure and duplicate application logic.
- Documentation currently has naming and source-of-truth drift. Resolve `tube-service` versus `backend`, `chat-service` versus `chat-platform`, and the tracked-versus-new documentation set before publishing contracts or deployment guidance.
