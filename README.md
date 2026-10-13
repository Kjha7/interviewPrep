# stock-trading-platform

Boilerplate implementation of the TradingApp low-level design: `com.tekion.interview.stockTradingPlatform`.

## Stack
- Spring Boot 3.3 / Java 17
- PostgreSQL — `UserAccount`, `UserCredentials`, `Wallet`, `trade_order` (hash-partitioned by `user_id`), `outbox_event`
- Cassandra — `portfolio` (eventually-consistent read projection)
- Redis — refresh tokens, idempotency keys
- Kafka + Debezium — outbox CDC → `PaymentConsumer` (settlement/execution) + `NotificationConsumer` (STOMP push), independent consumer groups on the same topic
- WebSocket — one central `MarketDataService` singleton to the exchange; a separate client-facing STOMP endpoint (`/ws/trading`) fed by the Kafka consumers

## Before first run
1. Create the Postgres schema: `docker/schema-partitioning.sql` (hand-rolled DDL, since `ddl-auto=validate` deliberately won't create the hash-partitioned `trade_order` table for you — wire this into Flyway/Liquibase for real use).
2. Set `wal_level = logical` on the Postgres server (required for Debezium).
3. Register the Debezium connector: `docker/debezium-connector-config.json` against your Kafka Connect instance.
4. Create the Cassandra keyspace/table for `portfolio` (schema-action is `none` — same reasoning as Postgres).
5. Override `app.security.jwt-secret` and `app.security.kyc-encryption-key` via env vars — the values in `application.properties` are dev-only placeholders.

```bash
mvn spring-boot:run
```

## What's real vs stubbed
**Real / wired end-to-end:**
- Register → login → JWT session token + Redis-backed refresh token, rotated on refresh
- Buy/sell idempotency via Redis `SETNX` (check-and-reserve / conflict / replay)
- Wallet fund reservation via Postgres CAS (`UPDATE ... WHERE balance >= ?`)
- Order + OutboxEvent written in one `@Transactional` method (the actual outbox pattern — Debezium does the publishing, the app never calls Kafka directly on the write path)
- Portfolio projection updates via Cassandra lightweight transactions (`IF version = ?`), with retry-on-contention
- IDOR check on `GET /users/{userId}/portfolio`

**Stubbed — clearly marked with `TODO` at the call site:**
- `ExchangeWebSocketHandler` — no real exchange wire protocol; `PaymentConsumer` fills orders at the last cached price instead of a real match
- Account lockout after N failed logins (counter is tracked, lockout window isn't applied)
- Reconnect-with-backoff on the exchange WebSocket
- The sell-side holdings check in `OrderServiceImpl` is a **best-effort pre-check against an eventually-consistent projection** — see the comment there. A correct implementation needs a `reservedQuantity` column on `Portfolio` mirroring `Wallet.reservedMinorUnits`; that's a schema change beyond this boilerplate pass and is flagged rather than silently done.

## Not included
- Spring Security filter chain (kept intentionally minimal — a single `OncePerRequestFilter` resolves the JWT into a request attribute; swap in full Spring Security + method-level authorization before this goes anywhere near production)
- Rate limiting on `/auth/**` and `/orders/**`
- Flyway/Liquibase migrations (raw SQL file only)
- Tests
