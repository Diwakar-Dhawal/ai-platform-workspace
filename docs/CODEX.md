# Instructions for Codex

Always follow architecture.md.

Never violate ADR decisions.

Never introduce shared Java modules.

Identity Service only handles authentication.

Profiles belong to applications.

Gateways belong to applications.

Business logic belongs to backend services.

When implementing:

1. Keep controllers thin.
2. Put logic in services.
3. Follow existing naming.
4. Never change architecture decisions.
5. Ask for clarification instead of guessing.

Prefer consistency over new patterns.