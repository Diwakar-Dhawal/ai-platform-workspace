# Application Flow Diagrams

**Last updated:** 2026-08-20

This document describes the key flows in the Identity Service, including authentication, authorization, and account management.

---

## Authentication Flows

### Registration Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Frontend  │────▶│   Gateway   │────▶│   Identity  │────▶│ PostgreSQL  │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                           │                    │                    │
                           │  POST /auth/register                   │
                           │───────────────────▶│                    │
                           │                    │  Check duplicate   │
                           │                    │───────────────────▶│
                           │                    │◀───────────────────│
                           │                    │                    │
                           │                    │  Create user       │
                           │                    │───────────────────▶│
                           │                    │◀───────────────────│
                           │                    │                    │
                           │                    │  Assign USER role  │
                           │                    │───────────────────▶│
                           │                    │◀───────────────────│
                           │                    │                    │
                           │                    │  Generate tokens   │
                           │                    │───────────────────▶│
                           │                    │◀───────────────────│
                           │                    │                    │
                           │                    │  Send verification │
                           │                    │  email (console)   │
                           │                    │─────┐              │
                           │                    │◀────┘              │
                           │                    │                    │
                           │◀───────────────────│                    │
                           │  201 + JWT tokens  │                    │
                           │◀───────────────────│                    │
                           │                    │                    │
```

**Steps:**
1. User submits registration form
2. Gateway forwards to Identity Service
3. Identity checks for duplicate email/username
4. Identity creates user with encoded password
5. Identity assigns USER role to user for the client
6. Identity generates access token (15 min) + refresh token (7 days)
7. Identity sends verification email (logged to console in dev)
8. Returns JWT tokens to client

---

### Login Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Frontend  │────▶│   Gateway   │────▶│   Identity  │────▶│ PostgreSQL  │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                           │                    │                    │
                           │  POST /auth/login  │                    │
                           │───────────────────▶│                    │
                           │                    │  Find user by email│
                           │                    │───────────────────▶│
                           │                    │◀───────────────────│
                           │                    │                    │
                           │                    │  Check user status │
                           │                    │─────┐              │
                           │                    │◀────┘              │
                           │                    │                    │
                           │                    │  Verify password   │
                           │                    │─────┐              │
                           │                    │◀────┘              │
                           │                    │                    │
                           │                    │  Check user roles  │
                           │                    │───────────────────▶│
                           │                    │◀───────────────────│
                           │                    │                    │
                           │                    │  Generate tokens   │
                           │                    │───────────────────▶│
                           │                    │◀───────────────────│
                           │                    │                    │
                           │◀───────────────────│                    │
                           │  200 + JWT tokens  │                    │
                           │◀───────────────────│                    │
                           │                    │                    │
```

**Steps:**
1. User submits login credentials
2. Gateway forwards to Identity Service
3. Identity finds user by email
4. Identity checks user status (must be ACTIVE)
5. Identity verifies password with BCrypt
6. Identity checks user has roles for the client
7. Identity generates new tokens
8. Returns JWT tokens to client

---

### Token Refresh Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Frontend  │────▶│   Gateway   │────▶│   Identity  │────▶│ PostgreSQL  │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                           │                    │                    │
                           │  POST /auth/refresh│                    │
                           │───────────────────▶│                    │
                           │                    │  Find refresh token│
                           │                    │───────────────────▶│
                           │                    │◀───────────────────│
                           │                    │                    │
                           │                    │  Check if revoked  │
                           │                    │─────┐              │
                           │                    │◀────┘              │
                           │                    │                    │
                           │                    │  Check if expired  │
                           │                    │─────┐              │
                           │                    │◀────┘              │
                           │                    │                    │
                           │                    │  Revoke old token  │
                           │                    │───────────────────▶│
                           │                    │◀───────────────────│
                           │                    │                    │
                           │                    │  Generate new pair │
                           │                    │───────────────────▶│
                           │                    │◀───────────────────│
                           │                    │                    │
                           │◀───────────────────│                    │
                           │  200 + new tokens  │                    │
                           │◀───────────────────│                    │
                           │                    │                    │
```

**Steps:**
1. Frontend sends expired access token + refresh token
2. Gateway forwards to Identity Service
3. Identity finds refresh token in database
4. Identity checks if token is revoked
5. Identity checks if token is expired
6. Identity revokes old refresh token
7. Identity generates new token pair
8. Returns new tokens to client

---

### Logout Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Frontend  │────▶│   Gateway   │────▶│   Identity  │────▶│ PostgreSQL  │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                           │                    │                    │
                           │  POST /auth/logout │                    │
                           │  (with JWT)        │                    │
                           │───────────────────▶│                    │
                           │                    │  Verify JWT        │
                           │                    │─────┐              │
                           │                    │◀────┘              │
                           │                    │                    │
                           │                    │  Find refresh token│
                           │                    │───────────────────▶│
                           │                    │◀───────────────────│
                           │                    │                    │
                           │                    │  Revoke session    │
                           │                    │───────────────────▶│
                           │                    │◀───────────────────│
                           │                    │                    │
                           │◀───────────────────│                    │
                           │  200 OK            │                    │
                           │◀───────────────────│                    │
                           │                    │                    │
```

**Steps:**
1. Frontend sends refresh token + access token (in header)
2. Gateway validates access token
3. Identity finds refresh token
4. Identity revokes all tokens in the session
5. Returns success

**Note:** Access token remains valid until expiry. Refresh token is revoked immediately.

---

## Email Verification Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Console   │────▶│   Identity  │────▶│ PostgreSQL  │
└─────────────┘     └─────────────┘     └─────────────┘
       │                    │                    │
       │  User registers    │                    │
       │───────────────────▶│                    │
       │                    │  Generate token    │
       │                    │───────────────────▶│
       │                    │◀───────────────────│
       │                    │                    │
       │  📧 Email verification:                │
       │  http://localhost:8081/.../verify-email?token=abc-123
       │◀───────────────────│                    │
       │                    │                    │
       │  User clicks link  │                    │
       │  (copies token)    │                    │
       │                    │                    │
       │  POST /auth/verify-email                │
       │───────────────────▶│                    │
       │                    │  Find token        │
       │                    │───────────────────▶│
       │                    │◀───────────────────│
       │                    │                    │
       │                    │  Mark email verified│
       │                    │───────────────────▶│
       │                    │◀───────────────────│
       │                    │                    │
       │                    │  Mark token used   │
       │                    │───────────────────▶│
       │                    │◀───────────────────│
       │                    │                    │
       │◀───────────────────│                    │
       │  200 OK            │                    │
       │                    │                    │
```

**Steps:**
1. User registers → verification token created
2. Console shows verification URL with token
3. User copies token from console
4. User calls `/auth/verify-email` with token
5. Identity marks email as verified
6. Identity marks token as used (single-use)

---

## Password Reset Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Frontend  │────▶│   Console   │────▶│   Identity  │────▶│ PostgreSQL  │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
       │                    │                    │                    │
       │  POST /auth/forgot-password            │                    │
       │───────────────────────────────────────▶│                    │
       │                                        │  Find user by email│
       │                                        │───────────────────▶│
       │                                        │◀───────────────────│
       │                                        │                    │
       │                                        │  Delete old tokens │
       │                                        │───────────────────▶│
       │                                        │◀───────────────────│
       │                                        │                    │
       │                                        │  Generate new token│
       │                                        │───────────────────▶│
       │                                        │◀───────────────────│
       │                                        │                    │
       │                                        │  📧 Password reset │
       │  🔑 http://localhost:8081/.../reset-password?token=xyz-789 │
       │◀───────────────────────────────────────│                    │
       │                                        │                    │
       │  POST /auth/reset-password             │                    │
       │  { token: "xyz-789", newPassword: "..." }                  │
       │───────────────────────────────────────▶│                    │
       │                                        │  Find token        │
       │                                        │───────────────────▶│
       │                                        │◀───────────────────│
       │                                        │                    │
       │                                        │  Update password   │
       │                                        │───────────────────▶│
       │                                        │◀───────────────────│
       │                                        │                    │
       │                                        │  Mark token used   │
       │                                        │───────────────────▶│
       │                                        │◀───────────────────│
       │                                        │                    │
       │                                        │  Invalidate all    │
       │                                        │  refresh tokens    │
       │                                        │───────────────────▶│
       │                                        │◀───────────────────│
       │                                        │                    │
       │◀───────────────────────────────────────│                    │
       │  200 OK                                │                    │
       │                                        │                    │
```

**Steps:**
1. User requests password reset with email
2. Identity finds user by email
3. Identity deletes any existing reset tokens
4. Identity generates new reset token (1 hour expiry)
5. Console shows reset URL with token
6. User copies token and calls `/auth/reset-password`
7. Identity updates password
8. Identity invalidates all refresh tokens (logout everywhere)

---

## Client Management Flow

### Create Client

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Admin UI  │────▶│   Identity  │────▶│ PostgreSQL  │
└─────────────┘     └─────────────┘     └─────────────┘
       │                    │                    │
       │  POST /admin/clients                   │
       │  (with ADMIN JWT)  │                    │
       │───────────────────▶│                    │
       │                    │  Verify ADMIN role │
       │                    │─────┐              │
       │                    │◀────┘              │
       │                    │                    │
       │                    │  Check duplicate   │
       │                    │───────────────────▶│
       │                    │◀───────────────────│
       │                    │                    │
       │                    │  Create client     │
       │                    │───────────────────▶│
       │                    │◀───────────────────│
       │                    │                    │
       │◀───────────────────│                    │
       │  201 + Client data │                    │
       │                    │                    │
```

---

### Role Assignment Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Admin UI  │────▶│   Identity  │────▶│ PostgreSQL  │
└─────────────┘     └─────────────┘     └─────────────┘
       │                    │                    │
       │  POST /admin/users/{id}/clients/{clientId}/roles
       │  (with ADMIN JWT)  │                    │
       │───────────────────▶│                    │
       │                    │  Verify ADMIN role │
       │                    │─────┐              │
       │                    │◀────┘              │
       │                    │                    │
       │                    │  Find user         │
       │                    │───────────────────▶│
       │                    │◀───────────────────│
       │                    │                    │
       │                    │  Find client       │
       │                    │───────────────────▶│
       │                    │◀───────────────────│
       │                    │                    │
       │                    │  Find role         │
       │                    │───────────────────▶│
       │                    │◀───────────────────│
       │                    │                    │
       │                    │  Assign role       │
       │                    │───────────────────▶│
       │                    │◀───────────────────│
       │                    │                    │
       │◀───────────────────│                    │
       │  200 + User data   │                    │
       │  (with updated     │                    │
       │   roles)           │                    │
       │                    │                    │
```

---

## Rate Limiting Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│   Gateway   │────▶│   Identity  │
└─────────────┘     └─────────────┘     └─────────────┘
       │                    │                    │
       │  POST /auth/login  │                    │
       │───────────────────▶│                    │
       │                    │                    │
       │                    │  RateLimitFilter   │
       │                    │─────┐              │
       │                    │     │ Check limit  │
       │                    │◀────┘              │
       │                    │                    │
       │                    │  ┌─────────────┐   │
       │                    │  │ Allowed?    │   │
       │                    │  └──────┬──────┘   │
       │                    │         │          │
       │                    │    Yes  │  No      │
       │                    │         │          │
       │                    │         ▼          │
       │                    │  ┌─────────────┐   │
       │                    │  │ Process     │   │
       │                    │  │ Request     │   │
       │                    │  └─────────────┘   │
       │                    │                    │
       │                    │  ┌─────────────┐   │
       │                    │  │ 429 Too     │   │
       │                    │  │ Many        │   │
       │                    │  │ Requests    │   │
       │                    │  └─────────────┘   │
       │                    │                    │
       │◀───────────────────│                    │
       │  429 or 200        │                    │
       │                    │                    │
```

**Rate Limits:**
| Endpoint | Max Requests | Window |
|----------|--------------|--------|
| `/auth/login` | 5 | 5 minutes |
| `/auth/register` | 3 | 1 hour |
| `/auth/refresh` | 10 | 5 minutes |
| `/auth/forgot-password` | 3 | 1 hour |

---

## JWT Token Lifecycle

```
                    ┌─────────────┐
                    │   User      │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │  Register   │
                    │  or Login   │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │  Generate   │
                    │  Tokens     │
                    └──────┬──────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
   ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
   │  Access     │  │  Refresh    │  │  Verify     │
   │  Token      │  │  Token      │  │  Email      │
   │  (15 min)   │  │  (7 days)   │  │  Token      │
   └──────┬──────┘  └──────┬──────┘  │  (24 hours) │
          │                │         └──────┬──────┘
          │                │                │
   ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
   │  Use for    │  │  Store in   │  │  Verify     │
   │  API calls  │  │  database   │  │  email      │
   └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
          │                │                │
          │                │         ┌──────▼──────┐
          │                │         │  Mark as    │
          │                │         │  used       │
          │                │         └─────────────┘
          │                │
   ┌──────▼──────┐  ┌──────▼──────┐
   │  Expires    │  │  Refresh    │
   │  (401)      │  │  (new pair) │
   └──────┬──────┘  └──────┬──────┘
          │                │
          │         ┌──────▼──────┐
          │         │  Revoke old │
          │         │  token      │
          │         └─────────────┘
          │
   ┌──────▼──────┐
   │  Logout     │
   │  (revoke)   │
   └─────────────┘
```

---

## Summary of Flows

| Flow | Complexity | Key Features |
|------|------------|--------------|
| Registration | Medium | Duplicate check, role assignment, email verification |
| Login | Medium | Status check, password verify, role check |
| Token Refresh | Low | Revoke old, issue new pair |
| Logout | Low | Revoke session tokens |
| Email Verification | Low | Single-use token, 24h expiry |
| Password Reset | Medium | Single-use token, 1h expiry, invalidate all sessions |
| Client Management | Low | CRUD operations, ADMIN required |
| Role Assignment | Medium | Client-scoped, idempotent |
| Rate Limiting | Low | IP-based, configurable limits |
