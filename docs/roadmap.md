# Canonical Implementation Roadmap

This roadmap is dependency-first. It preserves the documented boundaries: Identity owns authentication, authorization, roles, and sessions; InsightTube owns its gateway and backend business logic; platform services remain application-neutral; services integrate only through APIs.

**Blocker** milestones must be complete before the next phase begins. **Optional** milestones do not block the stated phase transition.

## Phase 1: Identity Contract and Production Foundation

**Outcome:** Identity is a secure, deployable authentication platform service with a published consumer contract.

### Milestones

1. **Create Identity configuration and migration baseline** - externalize credentials/secrets, add environment profiles, replace schema auto-update with versioned migrations. TD-001, TD-007. **Blocker:** Phase 2.
2. **Canonicalize workspace names and documentation sources** - TD-015, TD-025. **Blocker:** Phase 2.
3. **Establish client lifecycle and administration contract** - define client bootstrap/management and client-scoped role administration without changing ADR 004. TD-006, TD-010. **Blocker:** Phase 2.
4. **Enforce scoped token validation and client-safe responses** - validate issuer/audience/claims, return only current-client roles, remove production test routes, and standardize authorization errors. TD-002, TD-013, TD-018. **Blocker:** Phase 2.
5. **Harden account, refresh-token, and session lifecycle** - apply account status at authentication, make role changes revoke access, protect refresh rotation/replay, and define reliable logout/session expiry behavior. TD-003, TD-004, TD-005. **Blocker:** Phase 2.
6. **Publish and verify the Identity contract** - document endpoints, claims, errors, and session behavior; add security, integration, and concurrency coverage. TD-008, TD-010. **Blocker:** Phase 2.
7. **Package Identity for deployment** - add service deployment configuration and readiness guidance. TD-011. **Blocker:** Phase 2.

### Optional Milestones

- Remove unrelated binaries, protect repository hygiene, and replace generated onboarding material. TD-012, TD-024.
- Align package/component conventions and remove unused implementation artifacts. TD-023.

## Phase 2: InsightTube Gateway Boundary

**Outcome:** InsightTube has a secure application-owned entry point for Identity and Tube Service APIs.

### Milestones

1. **Define public InsightTube routes and browser policy** - document route prefixes, CORS policy, error format, and downstream addressing using the Phase 1 API contract. TD-009, TD-010, TD-017. **Blocker:** Phase 3.
2. **Implement JWT validation at the gateway** - validate Identity-issued tokens, including the InsightTube audience, before forwarding protected requests. TD-002, TD-009. **Blocker:** Phase 3.
3. **Add resilient proxy behavior** - add timeouts, downstream error handling, health/readiness checks, and gateway tests. TD-008, TD-017. **Blocker:** Phase 3.
4. **Deploy the gateway with production-safe configuration** - remove TRACE defaults and include the gateway in the workspace deployment model. TD-011, TD-021. **Blocker:** production integration, not Phase 3 foundation.

### Optional Milestones

- Add baseline correlation IDs, rate-limit telemetry, and authentication audit events. TD-021.
- Run early load tests and record the evidence needed for TD-019.

## Phase 3: InsightTube Tube Service

**Outcome:** InsightTube owns its business data, YouTube integration, OAuth flow, and application APIs behind its gateway.

### Milestones

1. **Define Tube Service ownership and API contract** - establish application data boundaries, OAuth callback topology, task lifecycle, and API schemas. TD-010, TD-016. **Blocker:** implementation milestones in this phase.
2. **Create the service foundation** - add the independently deployable service, its own database/migrations, health checks, and gateway route integration. TD-011, TD-016, TD-017. **Blocker:** OAuth and user-facing workflows.
3. **Implement YouTube OAuth and application data flows** - keep OAuth, external API calls, profiles, and business data in Tube Service only. TD-016. **Blocker:** Phase 4 frontend and Phase 5 AI consumer integration.
4. **Publish and test Tube Service integration contracts** - add gateway integration coverage and API documentation for frontend and AI Platform consumers. TD-008, TD-010, TD-017. **Blocker:** Phase 4 frontend and Phase 5 AI integration.

### Optional Milestones

- Add expanded operational telemetry once the service has real workflows. TD-021.

## Phase 4: InsightTube Frontend Integration

**Outcome:** The frontend consumes only InsightTube gateway APIs for authenticated Tube Service workflows and, when enabled, AI/chat features.

### Milestones

1. **Create frontend foundation and gateway-only API integration** - configure environments, route all browser traffic through the InsightTube gateway, and implement Identity session flows. **Blocker:** protected product workflows.
2. **Integrate Tube Service workflows** - implement YouTube connection, task submission, status, result, and error states against published gateway contracts. TD-010, TD-017. **Blocker:** production-ready core user journey.
3. **Add end-to-end coverage for the core user journey** - test authentication through gateway to Tube Service. TD-008. **Blocker:** production release.
4. **Integrate AI and chat features only when their Phase 5/6 contracts are complete.** **Blocker:** only for those optional product features.

### Optional Milestones

- Add active-session management UI after TD-020 is implemented.

## Phase 5: AI Platform Consumer Contract and Implementation

**Outcome:** AI Platform provides reusable, application-neutral APIs consumed by Tube Service through HTTP APIs only.

### Milestones

1. **Define the first application-neutral AI contract with Tube Service** - specify input, task, result, failure, and retention behavior without introducing Tube-specific domain logic. TD-010. **Blocker:** AI Platform implementation.
2. **Create the AI Platform service foundation** - add its independently deployable service, owned persistence, health checks, and authenticated API boundary. **Blocker:** AI task processing.
3. **Implement the first reusable AI task flow** - expose task submission/status APIs and integrate Tube Service as an API consumer. **Blocker:** AI-powered frontend workflows.
4. **Add contract and failure-mode tests** - verify API-only integration and provider failure behavior. TD-008, TD-010. **Blocker:** AI-powered frontend integration.

### Optional Milestones

- Add provider-cost, performance, and scale telemetry after an initial workload is available. TD-021.

## Phase 6: Chat Service Scope and Implementation

**Outcome:** Chat Service is implemented only after its platform role, tenants, and consumers are defined, without absorbing InsightTube business logic.

### Milestones

1. **Confirm Chat Service ownership and consumer contract** - define tenancy, retention, transport, and whether it consumes AI Platform through APIs. TD-010, TD-022. **Blocker:** Chat Service implementation.
2. **Create the Chat Service foundation** - add its own data model, migrations, authenticated APIs, and independent deployment configuration. **Blocker:** client integration.
3. **Implement conversation/message lifecycle and API tests** - maintain client/application isolation and document consumer integration. TD-008, TD-010, TD-022. **Blocker:** chat frontend integration.

### Optional Milestones

- Add session-management views and retention automation when multi-device usage requires them. TD-020.

## Deferred Scale Work

- Optimize repeated JWT parsing and per-request user lookups only after gateway load evidence shows the need. TD-019.
- Add refresh-token retention cleanup and session visibility before sustained multi-device usage. TD-020.
- Evolve role namespacing or permissions only when a second application demonstrates the need. TD-022.
- Remove redundant dependencies and generated project metadata during routine maintenance. TD-026.

## Current Recommended Focus

**Phase 1, Milestone 1: Create Identity configuration and migration baseline (TD-001, TD-007).**
