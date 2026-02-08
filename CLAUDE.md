You are a senior Java architect.

Design and implement a new microservice called "shortliner-analytics"
for a URL shortener system.

Technology stack:

- Java 25
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL
- Kafka Consumer
- Docker
- Gradle

Purpose:
This service collects and analyzes click statistics for shortened URLs.

Architecture:

- This service consumes events from Kafka topic: shortliner.clicks
- Each event represents one redirect/click
- Events are produced by the main shortliner service

Event structure (JSON):
{
"shortCode": "abc123",
"userId": "uuid-or-null",
"timestamp": "ISO-8601",
"ip": "string",
"userAgent": "string",
"referrer": "string"
}

Responsibilities:

1. Consume Kafka messages
2. Persist analytics data in PostgreSQL
3. Expose REST API for statistics

Database schema:
Design proper JPA entities and tables for:

- ClickEvent
- AggregatedStats (daily statistics per shortCode)

API endpoints:

- GET /api/analytics/{shortCode}/daily
- GET /api/analytics/{shortCode}/summary
- GET /api/analytics/user/{userId}

Functional requirements:

- High write performance
- Idempotent Kafka consumption
- Proper error handling
- Retry on Kafka failure
- Pagination for large responses

Non-functional requirements:

- Clean architecture
- Layered structure
- DTO mapping
- Validation
- Logging
- OpenAPI documentation

Deliverables:

- Full project structure
- Gradle config
- application.yml
- Dockerfile
- docker-compose.yml
- Kafka consumer config
- JPA entities
- Controllers
- Services
- Repositories
- Example queries
- Sample curl commands

Explain architectural decisions.
