# Shortliner Analytics

Microservice that collects and analyzes click statistics for shortened URLs.
Consumes redirect events from Kafka, persists them in PostgreSQL, and exposes a REST API for querying analytics data.

## Tech Stack

- Java 25
- Spring Boot 3.5
- Spring Data JPA + Hibernate
- PostgreSQL 17
- Apache Kafka
- Flyway (database migrations)
- SpringDoc OpenAPI (Swagger UI)
- Docker

## Getting Started

### Prerequisites

- Java 25+
- Docker
- Access to PostgreSQL and Kafka instances

### Configuration

Copy the example below into a `.env` file in the project root:

```env
# Server
SERVER_PORT=8082

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=shortliner_analytics
DB_USERNAME=postgres
DB_PASSWORD=postgres
HIKARI_MAX_POOL_SIZE=20
HIKARI_MIN_IDLE=5

# JPA / Hibernate
HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
HIBERNATE_BATCH_SIZE=50

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Analytics
AGGREGATION_INTERVAL_MS=300000

# Logging
LOG_LEVEL_APP=DEBUG
LOG_LEVEL_KAFKA=INFO

# Observability
TRACING_SAMPLING_PROBABILITY=1.0
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318/v1/traces
OTEL_TRACING_EXPORT_ENABLED=false
LOGGING_STRUCTURED_FORMAT_CONSOLE=
```

### Run Locally

```bash
./gradlew bootRun
```

### Run with Docker

```bash
docker-compose up --build
```

The service joins the external `shortliner-net` Docker network, so PostgreSQL and Kafka should be reachable from that
network.

## Database Migrations

Schema is managed by Flyway. Migrations run automatically on startup. Scripts live in:

```
src/main/resources/db/migration/
  V1__create_click_events_table.sql
  V2__create_aggregated_stats_table.sql
```

Hibernate is set to `validate` mode -- it verifies entities match the schema but never modifies it.

## Kafka

The service listens on the `shortliner.clicks` topic. Each message represents a single redirect event.

**Expected message format (JSON):**

```json
{
  "shortCode": "abc123",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-02-08T14:30:00Z",
  "ip": "203.0.113.42",
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
  "referrer": "https://example.com/page"
}
```

Consumption is idempotent -- duplicate events (same shortCode + timestamp + ip) are detected by hash and silently
ignored.

## API Reference

Base URL: `http://localhost:8082`

### Get Daily Statistics

Returns paginated daily click counts for a short URL.

```
GET /api/analytics/{shortCode}/daily
```

```bash
curl -s "http://localhost:8082/api/analytics/abc123/daily?page=0&size=7" | jq
```

```json
{
  "content": [
    {
      "shortCode": "abc123",
      "date": "2026-02-08",
      "clickCount": 142,
      "uniqueVisitors": 98
    },
    {
      "shortCode": "abc123",
      "date": "2026-02-07",
      "clickCount": 87,
      "uniqueVisitors": 63
    }
  ],
  "totalElements": 30,
  "totalPages": 5,
  "size": 7,
  "number": 0
}
```

| Parameter   | Type  | Default | Description           |
|-------------|-------|---------|-----------------------|
| `shortCode` | path  | --      | The short URL code    |
| `page`      | query | `0`     | Page number (0-based) |
| `size`      | query | `30`    | Results per page      |
| `sort`      | query | --      | e.g. `date,desc`      |

---

### Get Summary

Returns total lifetime statistics for a short URL.

```
GET /api/analytics/{shortCode}/summary
```

```bash
curl -s "http://localhost:8082/api/analytics/abc123/summary" | jq
```

```json
{
  "shortCode": "abc123",
  "totalClicks": 4821,
  "uniqueVisitors": 3104,
  "firstClick": "2026-01-10T08:15:30Z",
  "lastClick": "2026-02-08T14:30:00Z"
}
```

---

### Get User Analytics

Returns paginated analytics across all short URLs owned by a user.

```
GET /api/analytics/user/{userId}
```

```bash
curl -s "http://localhost:8082/api/analytics/user/550e8400-e29b-41d4-a716-446655440000?page=0&size=5" | jq
```

```json
{
  "content": [
    {
      "shortCode": "abc123",
      "clickCount": 4821,
      "lastClick": "2026-02-08T14:30:00Z"
    },
    {
      "shortCode": "xyz789",
      "clickCount": 312,
      "lastClick": "2026-02-07T22:10:45Z"
    }
  ],
  "totalElements": 12,
  "totalPages": 3,
  "size": 5,
  "number": 0
}
```

| Parameter | Type  | Default | Description           |
|-----------|-------|---------|-----------------------|
| `userId`  | path  | --      | The user UUID         |
| `page`    | query | `0`     | Page number (0-based) |
| `size`    | query | `20`    | Results per page      |

## Swagger UI

Interactive API documentation is available at:

```
http://localhost:8082/swagger-ui.html
```

OpenAPI spec (JSON):

```
http://localhost:8082/api-docs
```

## Observability

### Metrics (Prometheus)

Metrics are exposed in Prometheus exposition format at:

```
http://localhost:8082/actuator/prometheus
```

Point a Prometheus scrape config at this path (default scrape interval is fine; the endpoint is cheap to hit).
Standard JVM/HTTP/Kafka-consumer metrics are included automatically via Micrometer, plus these
service-specific metrics:

| Metric                            | Type    | Description                                                    |
|------------------------------------|---------|------------------------------------------------------------------|
| `click_events_received_total`      | counter | Click events received from the `shortliner.clicks` Kafka topic  |
| `click_events_persisted_total`     | counter | Click events deduplicated and persisted to `click_events`       |
| `click_events_duplicate_total`     | counter | Click events skipped because their event hash already exists    |
| `click_events_processing_errors_total` | counter | Click events that failed processing and were not acknowledged |
| `aggregation_duration_seconds`     | timer   | Duration of each scheduled daily-stats aggregation run          |
| `aggregation_entries_processed_total` | counter | Rows upserted into `aggregated_stats` per aggregation run     |

All metrics carry an `application=shortliner-analytics` tag so they can be distinguished from other services in a
shared Grafana dashboard.

Other actuator endpoints exposed for monitoring: `/actuator/health` (liveness/readiness probes) and
`/actuator/metrics`.

### Tracing

Distributed tracing uses Micrometer Tracing with the OpenTelemetry bridge, exporting spans via OTLP/HTTP. Point
`OTEL_EXPORTER_OTLP_ENDPOINT` at your collector (Tempo, Jaeger, an OTel Collector, etc.) -- it defaults to
`http://localhost:4318/v1/traces`. Export is opt-in: set `OTEL_TRACING_EXPORT_ENABLED=true` once a collector is
actually reachable, otherwise spans are generated (for log correlation) but not shipped anywhere. Sampling rate is
controlled by `TRACING_SAMPLING_PROBABILITY` (`1.0` = trace everything; lower this in high-traffic environments).

Spans are created for incoming HTTP requests, incoming Kafka messages (this service only consumes), and the
scheduled aggregation job. If the upstream producer of `shortliner.clicks` also propagates trace context (the
`shortliner` service does, via `spring.kafka.template.observation-enabled=true`), consumption of a click event
continues the same trace, giving an end-to-end view across services.

This mirrors the observability setup in the sibling `shortliner` service -- same dependencies, same env var
names, same opt-in export gate -- so a shared Prometheus/Grafana/Tempo stack can scrape and correlate both
services identically.

### Logs

Every log line includes the active trace and span ID via MDC (`[shortliner-analytics,<traceId>,<spanId>]`) once
tracing is on the classpath, with no extra config needed. By default logs are plain text for a readable local
console; set `LOGGING_STRUCTURED_FORMAT_CONSOLE=logstash` or `ecs` to switch to structured JSON output for
ingestion into a log aggregator (e.g. Loki, ELK). Log levels are controlled by `LOG_LEVEL_APP` and
`LOG_LEVEL_KAFKA`.

## Project Structure

```
src/main/java/com/shortliner/analytics/
  config/           Kafka consumer configuration
  consumer/         Kafka event listener
  controller/       REST API endpoints
  dto/              Request/response records
  entity/           JPA entities (ClickEvent, AggregatedStats)
  repository/       Spring Data JPA repositories
  service/          Business logic and aggregation

src/main/resources/
  application.properties    Spring Boot configuration
  db/migration/             Flyway SQL scripts
```
