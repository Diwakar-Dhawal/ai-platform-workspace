# Coding Standards

Java 21

Spring Boot 4.x

Package naming

config
controller
dto
entity
repository
security
service
util

Constructor injection only.

No field injection.

No static utility abuse.

Controllers remain thin.

Business logic belongs in services.

Repositories only access data.

Never mix concerns.

Response format

ApiResponse

Error format

ErrorResponse

Naming

Use descriptive names.

Avoid abbreviations.

Consistency is preferred over cleverness.