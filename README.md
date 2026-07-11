# Distributed Token Rate Limiter

This project is a distributed rate-limiting microservice built with Spring Boot. It lets you configure request limits for API clients and check whether incoming requests should be allowed before they reach your backend. It demonstrates clean architecture, pluggable rate-limiting algorithms, and practical backend engineering with Docker, Flyway, role-based security, and Prometheus monitoring.

## Features

- **Per-client rate limits** configured through a REST API.
- **Two algorithms**, chosen per client: `TOKEN_BUCKET` and `SLIDING_WINDOW`.
- **Distributed by design** — limit state lives in a shared PostgreSQL database, so multiple app instances enforce the same limit.
- **Role-based auth** — `ADMIN` manages client config, `OPS` reads actuator/metrics.
- **Swagger UI** for exploring the API.
- **Correlation IDs** on every request + a consistent JSON error contract.
- **Prometheus metrics** for observability.
- **Flyway** migrations manage the database schema.

## Tech Stack

| Area        | Choice                                   |
|-------------|------------------------------------------|
| Language    | Java 21                                  |
| Framework   | Spring Boot 3.3 (Web, Data JPA, Security, Actuator) |
| Database    | PostgreSQL + Spring Data JPA             |
| Migrations  | Flyway                                   |
| Docs        | springdoc-openapi (Swagger UI)           |
| Metrics     | Micrometer + Prometheus                  |
| Tests       | JUnit 5, Mockito, Spring Boot Test       |
| Build       | Maven                                    |

## Architecture

A request flows through a thin controller into a service that delegates the decision to a pluggable strategy:

```
 Client / API gateway
        │  POST /api/v1/rate-limit/check  { "clientKey": "..." }
        ▼
┌──────────────────────────────────────────────────────────────────┐
│  RateLimitController                                             │
│      │                                                           │
│      ▼                                                           │
│  RateLimitService                                                │
│    - load ClientConfiguration (algorithm, rps, burst)            │
│    - pick strategy by algorithm                                  │
│      │                                                           │
│      ▼                                                           │
│  RateLimitStrategy  (TokenBucketStrategy | SlidingWindowStrategy)│
│    - read + update state in PostgreSQL                           │
│    - return ALLOW / DENY + remaining tokens                      │
└──────────────────────────────────────────────────────────────────┘
        │
        ▼
 HTTP 200 (ALLOW)  or  HTTP 429 + Retry-After (DENY)
```

**Why a strategy pattern?** Adding a new algorithm (e.g. Fixed Window) means writing one new `RateLimitStrategy` implementation — no changes to the controller or service. `RateLimitService` collects every strategy bean into a `Map<RateLimitAlgorithm, RateLimitStrategy>` and dispatches by the client's configured algorithm.

## The two algorithms

Both are configured with the same two numbers:

- `requestsPerSecond` — the sustained/refill rate.
- `burstCapacity` — the maximum number of requests allowed in a burst / window.

### Token Bucket
A bucket holds tokens. Every request spends one token. Tokens **refill continuously** at `requestsPerSecond`, up to `burstCapacity`.

- Allows short bursts (up to `burstCapacity`).
- After the burst is spent, requests trickle back in at the refill rate.
- State stored as **one row per client** (`bucket_state`).

### Sliding Window
Every allowed request records a timestamp. A request is allowed only if fewer than `burstCapacity` requests happened in the **rolling window** (`window = burstCapacity / requestsPerSecond` seconds).

- Strict "at most N requests per rolling window" — no saving up allowance.
- A new slot opens only when the oldest in-window request ages out.
- State stored as **one row per request** (`sliding_window_request`), pruned over time.

### How they differ (concrete example)
Two clients with identical limits `requestsPerSecond=1, burstCapacity=3`, 10 rapid requests each:

| Request | Token Bucket | Sliding Window |
|---------|--------------|----------------|
| 1–3     | ALLOW        | ALLOW          |
| 4–9     | DENY         | DENY           |
| 10      | **ALLOW** (a token refilled after ~1s) | DENY (still inside the 3s window) |

**Takeaway:** Token Bucket recovers after ~1 second (one token refilled), while Sliding Window keeps you blocked until the full 3-second window passes. Same steady rate — very different burst recovery.

## API Endpoints

All endpoints are under `/api/v1`. Authentication is HTTP Basic (`ADMIN` / `OPS` users). The rate-limit check itself is public.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST   | `/clients` | ADMIN | Create a client config |
| GET    | `/clients` | ADMIN | List all client configs |
| GET    | `/clients/{clientKey}` | ADMIN | Get one config |
| PUT    | `/clients/{clientKey}` | ADMIN | Update a config (e.g. switch algorithm) |
| DELETE | `/clients/{clientKey}` | ADMIN | Delete a config |
| POST   | `/rate-limit/check` | public | Evaluate a request for a client |
| GET    | `/actuator/health` | public | Health check |
| GET    | `/actuator/info` | public | App info |
| GET    | `/actuator/metrics` | OPS | Metrics (Prometheus: `/actuator/prometheus`) |
| GET    | `/swagger-ui.html` | ADMIN | Swagger UI |

### Create a client
```json
POST /api/v1/clients
{
  "clientKey": "my-client",
  "algorithm": "TOKEN_BUCKET",
  "requestsPerSecond": 1,
  "burstCapacity": 3
}
```

### Check the limit
```json
POST /api/v1/rate-limit/check
{ "clientKey": "my-client" }

// 200 ALLOW  -> { "status":"ALLOW", "remainingTokens":2, "limit":3, ... }
// 429 DENY   -> { "status":"DENY",  "resetAfterMillis":1000, ... }
```

## Getting Started

### Option A — Docker (recommended)
Requires Docker + Docker Compose (Docker Desktop includes both).

```bash
docker compose up --build
```

- The app is compiled inside a container (no local Java/Maven needed). Two containers start — `postgres` (database) and `rateshield` (app). Flyway creates the schema on first boot, and the app waits for a healthy database before starting.
- App: http://localhost:8080 — Health: `/actuator/health`, API docs: `/swagger-ui.html`.
- Default accounts: `admin` / `admin123` (ADMIN), `ops` / `ops123` (OPS).
- Stop with `docker compose down` (PostgreSQL data persists in a named volume).

Ports `8080` (app) and `5432` (Postgres) must be free on the host.

### Option B — Local (Maven + your own PostgreSQL)
Requires Java 21, Maven, and a PostgreSQL database `rateshield` at `localhost:5432` (default user `postgres` / password `admin`).

```bash
mvn spring-boot:run
```

- Default accounts: `admin` / `change-me-admin` (ADMIN), `ops` / `change-me-ops` (OPS).
- Add `-Dspring-boot.run.profiles=dev` for `ddl-auto: update` (Hibernate auto-syncs the schema while developing).
- Override any setting via environment variables — the production config ships no defaults:
  `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `OPS_USERNAME`, `OPS_PASSWORD`.

### Quick test (copy-paste)
With the app running (Docker: `docker compose up --build`), the commands below exercise the full flow. Credentials are the Docker defaults — `admin / admin123` (ADMIN) and `ops / ops123` (OPS). *(Local Maven run: use `change-me-admin` / `change-me-ops` instead.)*

**1. Create a client** (requires ADMIN):
```bash
curl -u admin:admin123 -X POST http://localhost:8080/api/v1/clients \
  -H 'Content-Type: application/json' \
  -d '{"clientKey":"demo","algorithm":"TOKEN_BUCKET","requestsPerSecond":1,"burstCapacity":2}'
```

**2. Send rate-limit checks** (public — no auth). Expect `200 200 429 429 429`:
```bash
for i in $(seq 1 5); do
  curl -s -o /dev/null -w "%{http_code} " -X POST http://localhost:8080/api/v1/rate-limit/check \
    -H 'Content-Type: application/json' -d '{"clientKey":"demo"}'
done
```

**3. Read the metrics** (requires OPS):
```bash
curl -u ops:ops123 http://localhost:8080/actuator/prometheus | grep ratelimit
```

Or explore the API interactively at http://localhost:8080/swagger-ui.html (log in as `admin`).

**Postman:** import [`postman/RateShield.postman_collection.json`](postman/RateShield.postman_collection.json) — every endpoint is pre-wired with the Docker credentials (ADMIN on client config, public check, OPS on metrics).

## Security model

- **HTTP Basic auth** over an in-memory user store (configured per environment).
- `ADMIN` role: manage client configurations (`/api/v1/clients/**`, Swagger).
- `OPS` role: read actuator/metrics.
- Stateless sessions (`SessionCreationPolicy.STATELESS`).
- Public: health, info, and the rate-limit check endpoint.
- The `prod` profile triggers `CredentialHardeningConfig`, which **refuses to start** if the default credentials are still in use.

> The service issues no JWTs; callers authenticate with Basic credentials. A JWT issuer was intentionally omitted to keep the project simple and easy to explain.

## Testing

- **Unit tests** for the algorithm logic: `TokenBucketStrategyTest`, `SlidingWindowStrategyTest` (Mockito; the strategies take `now` as a parameter, so tests are deterministic and clock-independent).
- **Integration test** `SecurityRouteIntegrationTest` boots the full Spring context against a **local PostgreSQL** instance and verifies the security rules.

```bash
mvn test
```

> **JDK note:** targets Java 21. On newer JDKs (e.g. 24) Mockito/Byte Buddy may need `net.bytebuddy.experimental=true` for tests that mock concrete classes — already configured in the Surefire plugin.

## Project structure

```
src/main/java/com/rateshield/
├── controller/      # REST controllers (clients, rate-limit)
├── service/         # RateLimitService, ClientConfigurationService
├── strategy/        # RateLimitStrategy + TokenBucket / SlidingWindow impls
├── repository/      # Spring Data JPA repositories
├── entity/          # JPA entities (ClientConfiguration, BucketState, ...)
├── config/          # SecurityConfig and other configuration
├── exception/       # GlobalExceptionHandler + custom exceptions
├── dto/             # request/response records
├── enums/           # RateLimitAlgorithm, RateLimitStatus
└── web/             # CorrelationFilter (request tracing)

src/main/resources/db/migration/   # Flyway SQL migrations
src/test/java/...                  # unit + integration tests
```