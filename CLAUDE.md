# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.shortliner.analytics.ShortlinerAnalyticsApplicationTests"

# Build
./gradlew build

# Run with Docker (requires .env file and external shortliner-net network)
docker-compose up --build
```

Tests use H2 in-memory via the `test` Spring profile (`src/test/resources/application-test.yml`). Kafka autoconfiguration is excluded from tests — no broker needed.

Running locally requires PostgreSQL and Kafka. Copy the env vars from the README into a `.env` file; the minimum required are `DB_HOST`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, and `KAFKA_BOOTSTRAP_SERVERS`.

## Architecture

This is a read-only analytics consumer microservice in the Shortliner URL-shortener system. It has no write API — all data enters through Kafka.

**Data flow:**

1. `ClickEventConsumer` listens on the `shortliner.clicks` Kafka topic (group `shortliner-analytics`, manual ACK, 3 concurrent consumers, 3 retries with 1 s backoff).
2. `AnalyticsService.processClickEvent` deduplicates by computing a SHA-256 hash of `shortCode + timestamp + ip + userAgent` and skipping events whose hash already exists in `click_events.event_hash`.
3. `AggregationService.aggregateDailyStats` runs on a fixed schedule (default 5 min, controlled by `AGGREGATION_INTERVAL_MS`) and upserts per-day totals from `click_events` into `aggregated_stats`.

**Two DB tables (managed by Flyway; Hibernate is in `validate` mode — never modifies schema):**
- `click_events` — raw event log, one row per deduplicated redirect event
- `aggregated_stats` — pre-aggregated daily totals (click count + unique visitors per short code per day), written only by the scheduler

**REST API** (`AnalyticsController`) exposes three read-only endpoints:
- `GET /api/analytics/{shortCode}/daily` — paginated daily stats from `aggregated_stats`
- `GET /api/analytics/{shortCode}/summary` — lifetime totals computed live from `click_events`
- `GET /api/analytics/user/{userId}` — paginated per-short-code totals for a user, computed live via JPQL `GROUP BY`

`ShortCodeNotFoundException` → 404 via `GlobalExceptionHandler`.

`AnalyticsMapper` converts between `ClickEventDto` (Kafka payload) and `ClickEvent` (entity), and from `AggregatedStats` to `DailyStatsResponse`.
