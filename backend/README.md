# ProCrush Backend

Kotlin backend: Ktor HTTP API, background workers, and domain logic. Three deployable applications (API, personality, matching) and library modules.

## Module structure

| Path | Gradle module | Purpose |
|------|---------------|---------|
| [`contracts/`](./contracts/src/main/kotlin) | `:backend:contracts` | Domain DTOs, events, ports, and pure domain logic (synced with OpenAPI via `ApiMappers` in `api`) |
| [`config/`](./config/src/main/kotlin) | `:backend:config` | Env reading and typed application settings |
| [`platform/`](./platform) | `:backend:platform:*` | Redis, RabbitMQ, Kafka, LLM, Flyway main DB (`persistence`), observability |
| [`domain/`](./domain) | `:backend:domain:*` | Bounded contexts: auth, seeker, employer, survey, matching, personality |
| [`api/`](./api/src/main/kotlin) | `:backend:api` | Ktor HTTP API, Spektor-generated routes/DTOs in `build/`, handlers, composition root |
| [`personality/`](./personality) | `:backend:personality` | Deployable app: RabbitMQ consumer + health endpoint |
| [`domain/personality/`](./domain/personality) | `:backend:domain:personality-lib` | Domain library (coordinator, publisher, worker logic) |
| [`matching/`](./matching) | `:backend:matching` | Kafka consumer + HTTP read API, separate matching DB |

## Deployable applications

### API (`:backend:api`)

Main HTTP service: authentication, surveys, profiles, proxying recommendations from matching, SSE notifications.

```bash
./gradlew :backend:api:run
```

Health: `GET /health` (alias for `/health/ready`), `GET /health/live`, `GET /health/ready`, `GET /metrics`.

### Personality worker (`:backend:personality`)

Consumes the `personality.generation` queue, calls the LLM, saves the personality profile.

```bash
./gradlew :backend:personality:run
```

| Criterion | Implementation |
|-----------|------------------|
| Publisher in API | `PersonalityGenerationCoordinator` → `PersonalityJobPublisher` |
| Consumer only in worker | `AppContext` does not start the consumer; `WorkerContext` does |
| Retry + DLQ | up to 3 attempts, then `personality.generation.dlq` |
| Distributed lock + dedup | Redis lock and `PersonalityMessageDedup` |
| SSE / pub-sub | `RedisPersonalityStatusNotifier` + SSE in API |

**Known gap:** no end-to-end test for "publish → consume → READY".

### Matching service (`:backend:matching`)

Consumes domain events from Kafka, recalculates recommendations, writes to a separate PostgreSQL (`procrush_matching`). The API reads recommendations over HTTP (`MATCHING_SERVICE_URL` is required).

```bash
./gradlew :backend:matching:run
```

Health: `GET /health` (default port `8092`).

## Observability

Shared module: [`platform/observability`](./platform/observability). Three deployable apps expose the same endpoints.

| Endpoint | Purpose |
|----------|---------|
| `GET /health/live` | Liveness (process only) |
| `GET /health/ready` | Readiness (dependencies + consumers) |
| `GET /health` | Backward-compatible readiness summary |
| `GET /metrics` | Prometheus scrape target |

### Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVICE_NAME` | per app | `api`, `personality`, or `matching` |
| `LOG_FORMAT` | `text` | `text` or `json` (JSON adds MDC fields) |
| `OTEL_ENABLED` | `false` | Enable OpenTelemetry OTLP export |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4317` | Tempo/OTLP collector gRPC endpoint |
| `ENVIRONMENT` | `local` | Common metric label |
| `APP_VERSION` / `GIT_SHA` | `dev` | Reported in health responses |

### Correlation

HTTP requests accept/propagate `X-Request-Id`. The same ID flows through RabbitMQ personality jobs and Kafka matching events (`correlationId` in envelope).

### Local kind stack

With `LOG_FORMAT=json`, `OTEL_ENABLED=true` in [`deploy/k8s/base/configmap.yaml`](../deploy/k8s/base/configmap.yaml):

- Grafana: http://127.10.0.16:3000 (`admin` / `admin`)
- Prometheus: http://127.10.0.17:9090
- Tempo OTLP: `tempo:4317` inside cluster

See [deploy/k8s/README.md](../deploy/k8s/README.md) for alert rules and port-forward options.

## Infrastructure dependencies

### PostgreSQL (required)

Main DB (`procrush`). Schema and reference data — in Flyway migrations (`platform/persistence/src/main/kotlin/db/migration/`) and seed (`platform/persistence/src/main/resources/db/seed/init_inserts.sql`).

Separate matching DB: `backend/matching/src/main/kotlin/db/migration/`.

### Redis (required)

**Redis** — in-memory store used for:

- recommendation cache (cache-aside, TTL 10 min);
- distributed lock during LLM personality profile generation (held by the worker);
- session cache (PostgreSQL remains source of truth);
- pub/sub for SSE notifications about new responses and profile generation status (works with multiple API instances).

### RabbitMQ (required)

**RabbitMQ** — message broker: the API enqueues a "generate personality profile" job on `personality.generation`; the worker picks it up and calls the LLM. After 3 failed attempts the message goes to DLQ `personality.generation.dlq`.

### Kafka (required for matching)

**Kafka** — event log for matching recalculation. The API and personality publish domain events; matching consumes them and updates projections in its DB.

## Authentication

**httpOnly session cookies** are used.

| Endpoint | Description |
|----------|-------------|
| `POST /api/auth/dev/login` | Dev login (requires `AUTH_DEV_MODE=true`) |
| `GET /api/auth/me` | Current user |
| `POST /api/auth/logout` | Sign out |
| `POST /api/auth/complete-registration` | Role selection (immutable) |

Full REST endpoint list — in [openapi/README.md](../openapi/README.md).

## Local development (hot-reload)

**Requirements:** JDK 25 (Gradle toolchain; matches `deploy/Dockerfile.*`), infrastructure from kind (see [deploy/k8s/README.md](../deploy/k8s/README.md)).

Environment variables — in [`deploy/k8s/base/configmap.yaml`](../deploy/k8s/base/configmap.yaml) and local `secret.yaml` (template: [`secret.yaml.example`](../deploy/k8s/base/secret.yaml.example)).

```bash
./gradlew :backend:api:run
./gradlew :backend:personality:run
./gradlew :backend:matching:run
```

To verify changes **inside kind** (local build → thin image → conditional rollout), use `./gradlew kindUp`. Details — [deploy/k8s/README.md](../deploy/k8s/README.md#iterative-development-gradle).

After `git clone`, before working with handlers: run `./gradlew :backend:api:compileKotlin` once so the IDE sees generated sources in `build/`. In IntelliJ: *Build and run using → Gradle*.

LLM variables for personality: `LLM_BASE_URL`, `LLM_MODEL` — in configmap; `LLM_API_KEY` — in `secret.yaml`.

## Related documentation

- [openapi/README.md](../openapi/README.md) — REST API contract
- [i18n/README.md](../i18n/README.md) — error codes
- [deploy/k8s/README.md](../deploy/k8s/README.md) — local Kubernetes stack
- [deploy/README.md](../deploy/README.md) — Railway deployment
