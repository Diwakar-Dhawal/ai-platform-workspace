# Architecture Rules

## Identity

Identity handles

- Authentication
- Authorization
- Sessions
- Roles

Identity never handles

- User Profile
- YouTube
- AI
- Tasks
- Business logic

---

## Gateways

Every application owns its own gateway.

No platform gateway exists.

Gateway responsibilities

- Routing
- JWT validation
- Request forwarding

No business logic.

---

## Backend

Business logic only.

OAuth belongs here.

External APIs belong here.

Database belongs here.

---

## Platform Services

Platform services expose APIs.

Applications consume APIs.

Services never communicate through shared Java classes.