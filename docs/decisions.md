# Architecture Decision Records (ADRs)

**Last updated:** 2026-08-20

This document captures the key architectural decisions made during the Identity Service implementation, the rationale behind them, and the trade-offs considered.

---

## ADR-001: Identity Stores Authentication Only

**Date:** 2026-07-29  
**Status:** Accepted  
**Decision Makers:** Diwak

### Context
The AI Platform needs a shared authentication service that multiple applications (InsightTube, PDFMind, InstaMind) can consume. We needed to decide what responsibilities belong to Identity vs. application services.

### Decision
Identity Service handles ONLY:
- Authentication (registration, login)
- Authorization (roles, permissions)
- Sessions (JWT, refresh tokens)
- Client management

Identity NEVER handles:
- User profiles
- Business data
- Application-specific logic

### Rationale
- **Separation of concerns**: Authentication is a cross-cutting concern; business logic is domain-specific
- **Independent scaling**: Auth traffic patterns differ from business logic
- **Security boundary**: One service manages all credentials; reduces attack surface
- **Reuse**: Same Identity Service works for InsightTube, PDFMind, and future apps

### Trade-offs
- ✅ Clean boundaries, reusable across applications
- ❌ More services to deploy and maintain
- ❌ Cross-service communication required for user data

### Alternatives Considered
1. **Monolithic auth in each app**: Rejected — code duplication, inconsistent security
2. **Shared library**: Rejected — violates ADR-003 (no shared Java modules)

---

## ADR-002: Every Application Owns Its Gateway

**Date:** 2026-07-29  
**Status:** Accepted  
**Decision Makers:** Diwak

### Context
Applications need to validate JWTs, enforce CORS, and route requests. We needed to decide where this logic lives.

### Decision
Each application has its own gateway:
- `applications/insighttube/gateway`
- `applications/pdfmind/gateway` (future)
- `applications/instamind/gateway` (future)

No platform-wide gateway exists.

### Rationale
- **Independent deployment**: Each app can update its gateway without affecting others
- **Policy flexibility**: Different apps may need different CORS, rate limiting, or routing rules
- **Security isolation**: Gateway compromise affects only one application
- **Team autonomy**: Each team owns their gateway

### Trade-offs
- ✅ Independent deployment and policy control
- ❌ Duplicate gateway code across applications
- ❌ More infrastructure to manage

### Alternatives Considered
1. **Platform gateway**: Rejected — single point of failure, coupling
2. **Shared gateway library**: Partially adopted — pattern/template, not code dependency

---

## ADR-003: No Shared Java Modules

**Date:** 2026-07-29  
**Status:** Accepted  
**Decision Makers:** Diwak

### Context
Services need to communicate. We needed to decide whether to share code or use APIs.

### Decision
Services communicate ONLY through HTTP APIs. No shared Java libraries, modules, or classes between services.

### Rationale
- **Loose coupling**: Services can evolve independently
- **Technology flexibility**: Services can use different languages/frameworks
- **Deployment independence**: No coordinated deployments needed
- **Clear contracts**: API contracts are explicit and versioned

### Trade-offs
- ✅ Maximum flexibility and independence
- ❌ More API calls (latency)
- ❌ Need to maintain API contracts
- ❌ Some code duplication (DTOs, mappers)

### Alternatives Considered
1. **Shared DTO library**: Rejected — creates coupling
2. **gRPC/protobuf**: Considered later — not for initial implementation

---

## ADR-004: Registration Always Assigns USER Role

**Date:** 2026-07-29  
**Status:** Accepted  
**Decision Makers:** Diwak

### Context
New users need a default role. We needed to decide what role is assigned automatically.

### Decision
- Registration always assigns `USER` role
- `ADMIN` roles are assigned ONLY through Administration APIs

### Rationale
- **Security**: Prevents privilege escalation
- **Audit trail**: Admin actions are explicit and traceable
- **Principle of least privilege**: New users get minimal permissions

### Trade-offs
- ✅ Secure by default
- ❌ Admin must manually assign roles
- ❌ More steps for user onboarding

### Alternatives Considered
1. **Configurable default role**: Rejected — complexity, potential misconfiguration
2. **Email-based role assignment**: Rejected — requires email verification first

---

## ADR-005: Console-Based Email Verification

**Date:** 2026-08-20  
**Status:** Accepted  
**Decision Makers:** Diwak

### Context
Email verification is needed for production, but we don't have an email service configured yet.

### Decision
Implement full email verification flow with console logging instead of actual emails.

### Rationale
- **No external dependencies**: Works without SMTP configuration
- **Full flow testable**: Frontend can integrate immediately
- **Easy to swap later**: Just implement `EmailService` interface
- **Development friendly**: Developer sees "emails" in console

### Implementation
```java
@Service
@Profile({"local", "test", "dev"})
public class ConsoleEmailService implements EmailService {
    @Override
    public void sendVerificationEmail(String to, String username, String url) {
        log.info("📧 Email verification: {}", url);
    }
}
```

### Future
When email service is ready, implement `SmtpEmailService`:
```java
@Service
@Profile("prod")
public class SmtpEmailService implements EmailService {
    // Real SMTP implementation
}
```

---

## ADR-006: In-Memory Rate Limiting

**Date:** 2026-08-20  
**Status:** Accepted  
**Decision Makers:** Diwak

### Context
Auth endpoints need protection from brute force attacks. We needed to decide on rate limiting implementation.

### Decision
Use in-memory rate limiting with `ConcurrentHashMap` for initial implementation.

### Rationale
- **No external dependencies**: Works without Redis/Memcached
- **Simple implementation**: Easy to understand and maintain
- **Sufficient for single instance**: Works for development and small deployments
- **Easy to upgrade**: Can swap to Redis later

### Limits Configured
| Endpoint | Max Attempts | Window |
|----------|--------------|--------|
| Login | 5 | 5 minutes |
| Register | 3 | 1 hour |
| Refresh | 10 | 5 minutes |
| Forgot Password | 3 | 1 hour |

### Future
For production with multiple instances, upgrade to Redis-based rate limiting.

---

## ADR-007: H2 for Testing

**Date:** 2026-08-20  
**Status:** Accepted  
**Decision Makers:** Diwak

### Context
Integration tests need a database. We needed to decide between real PostgreSQL and in-memory.

### Decision
Use H2 in-memory database for tests with `@ActiveProfiles("test")`.

### Rationale
- **Speed**: H2 is much faster than PostgreSQL
- **Isolation**: Each test run starts fresh
- **No Docker required**: Tests run without Docker
- **CI/CD friendly**: Works in any environment

### Trade-offs
- ✅ Fast, isolated, no dependencies
- ❌ May behave differently than PostgreSQL
- ❌ Some PostgreSQL-specific features unavailable

### Future
Add integration tests with real PostgreSQL for migration testing.

---

## ADR-008: Flyway Manual Configuration

**Date:** 2026-08-20  
**Status:** Accepted  
**Decision Makers:** Diwak

### Context
Spring Boot 4.1 removed Flyway auto-configuration. We needed to run migrations manually.

### Decision
Use `BeanDefinitionRegistryPostProcessor` to run Flyway before Hibernate.

### Implementation
```java
@Configuration
public class FlywayConfig implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        // Add depends-on to entityManagerFactory
    }
    
    @Bean
    public FlywayInitializer flywayInitializer(DataSource dataSource) {
        // Run migrations
    }
}
```

### Rationale
- **Reliable ordering**: Migrations run before Hibernate validation
- **No schema conflicts**: Flyway owns schema, Hibernate validates
- **Works with Spring Boot 4.1**: Adapted to removed auto-config

---

## ADR-009: Test Profile Separation

**Date:** 2026-08-20  
**Status:** Accepted  
**Decision Makers:** Diwak

### Context
Tests need different configuration than local development (H2 vs PostgreSQL).

### Decision
Use separate profiles: `local`, `test`, `docker`, `prod`.

### Configuration
| Profile | Database | Purpose |
|---------|----------|---------|
| `local` | Docker PostgreSQL | Development |
| `test` | H2 in-memory | Unit/Integration tests |
| `docker` | Docker PostgreSQL | Docker deployment |
| `prod` | External PostgreSQL | Production |

### Rationale
- **Isolation**: Tests don't affect development data
- **Speed**: H2 is faster for tests
- **CI/CD**: Tests run without Docker
- **Environment parity**: Production-like config available

---

## ADR-010: AccessDeniedException Handler Returns 403

**Date:** 2026-08-20  
**Status:** Accepted  
**Decision Makers:** Diwak (via Codebuff)

### Context
When a user with insufficient roles (e.g., USER trying to access ADMIN endpoints) hits a `@PreAuthorize("hasRole('ADMIN')")` method, Spring Security throws `AccessDeniedException`. Without a handler in `GlobalExceptionHandler`, this fell through to the generic `Exception` handler, returning **500 Internal Server Error** instead of **403 Forbidden**.

### Decision
Add a dedicated `@ExceptionHandler(AccessDeniedException.class)` in `GlobalExceptionHandler` that returns HTTP 403 with a proper error response.

### Rationale
- **Correctness**: 403 accurately represents "authenticated but not authorized"
- **Security**: Avoids leaking 500 errors for expected authorization failures
- **Client experience**: Frontend can distinguish "not logged in" (401) from "not allowed" (403)

### Trade-offs
- ✅ Correct HTTP status codes
- ✅ Consistent error response format
- ❌ One more exception handler to maintain

---

## ADR-011: Forgot/Reset Password Prevents Email Enumeration

**Date:** 2026-08-20  
**Status:** Accepted  
**Decision Makers:** Diwak (via Codebuff)

### Context
The forgot-password endpoint could leak whether an email is registered by returning different responses for valid vs. invalid emails.

### Decision
The `forgot-password` endpoint **always returns 200 OK** regardless of whether the email exists. The actual token generation and email sending only happens if the user exists, but the response is identical.

### Rationale
- **Security**: Prevents attackers from enumerating valid email addresses
- **OWASP recommendation**: Standard practice for password reset flows
- **User experience**: Consistent response regardless of input

### Implementation
```java
public void forgotPassword(ForgotPasswordRequest request) {
    // Only generates token if user exists
    userRepository.findByEmail(request.email()).ifPresent(user -> {
        // ... generate token, send email
    });
    // Always returns success — no exception thrown
}
```

---

## ADR-012: Swagger/OpenAPI with springdoc-openapi

**Date:** 2026-08-20  
**Status:** Accepted  
**Decision Makers:** Diwak (via Codebuff)

### Context
API documentation was missing. Needed auto-generated docs for frontend integration and testing.

### Decision
Use `springdoc-openapi-starter-webmvc-ui` for Swagger UI and OpenAPI spec generation.

### Configuration
- Swagger UI: `/identity-service/swagger-ui.html`
- OpenAPI spec: `/identity-service/v3/api-docs`
- Annotations: `@Tag` on controllers, `@Operation` on endpoints

### Rationale
- **Auto-generated**: Docs stay in sync with code
- **Interactive testing**: Swagger UI allows direct API calls
- **Standard format**: OpenAPI 3.0 is widely supported
- **Minimal config**: Works with sensible defaults

### Known Issue
SpringDoc's `io.swagger.v3.oas.annotations.responses.ApiResponse` name clashes with the project's `com.aiservice.platform.identity.dto.response.ApiResponse`. Resolved by not importing SpringDoc's `@ApiResponse` annotation.

### Trade-offs
- ✅ Developer experience
- ✅ Auto-generated from code
- ❌ Not required for functionality
- ❌ Adds dependency (~3MB)

---

## Summary of Key Decisions

| # | Decision | Rationale | Trade-off |
|---|----------|-----------|-----------|
| 001 | Auth-only Identity | Clean boundaries | More services |
| 002 | App-owned gateways | Independent deployment | Duplicate code |
| 003 | No shared modules | Loose coupling | More API calls |
| 004 | USER role on registration | Security | Manual admin |
| 005 | Console email | No dependencies | No real emails |
| 006 | In-memory rate limit | Simple | Single instance |
| 007 | H2 for tests | Speed | Different behavior |
| 008 | Manual Flyway | Works with Boot 4.1 | More code |
| 009 | Profile separation | Isolation | More configs |
| 010 | AccessDeniedException → 403 | Correctness | Handler maintenance |
| 011 | Forgot-password prevents enumeration | Security | Always 200 |
| 012 | SpringDoc/OpenAPI | Developer experience | Extra dependency |
