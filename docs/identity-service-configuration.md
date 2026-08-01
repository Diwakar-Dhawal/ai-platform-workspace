# Identity Service Configuration

Identity Service uses Flyway migrations and requires an explicit Spring profile. It no longer stores credentials or the JWT signing secret in tracked configuration.

## Required Environment Variables

| Variable | Purpose |
| --- | --- |
| `IDENTITY_DB_URL` | PostgreSQL JDBC URL, including the UTC option when required. |
| `IDENTITY_DB_USERNAME` | Identity database username. |
| `IDENTITY_DB_PASSWORD` | Identity database password. |
| `IDENTITY_JWT_SECRET` | HMAC signing secret. It must satisfy the existing JWT implementation's key-length requirement. |

Start Identity with one of `local`, `docker`, or `prod`, for example: `SPRING_PROFILES_ACTIVE=local`.

## Database Migration

On a fresh database, Flyway applies `V1__initial_identity_schema.sql` before JPA validates the schema. Hibernate no longer creates or updates tables at runtime.

For an existing database created by the previous `ddl-auto: update` configuration, do not enable automatic baselining. First verify that its schema matches the V1 migration, then baseline it at version `1` with the approved Flyway operational workflow. Back up production data before any migration operation.

## Docker Compose Variables

Docker Compose now requires `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `PGADMIN_DEFAULT_EMAIL`, and `PGADMIN_DEFAULT_PASSWORD`. Supply them through the environment or an untracked `.env` file.
