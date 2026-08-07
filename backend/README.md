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
| [`personality/`](./personality) | `:backend:personality` | Deployable compute app: RabbitMQ command → LLM → result |
| [`domain/personality-messaging/`](./domain/personality-messaging) | `:backend:domain:personality-messaging` | Thin RabbitMQ command/result publishers shared by API and worker |
| [`domain/personality-lib/`](./domain/personality-lib) | `:backend:domain:personality-lib` | API-side personality library (coordinator, result consumer, apply, Redis SSE/lock) |
| [`matching/`](./matching) | `:backend:matching` | Kafka consumer + score publisher, separate matching DB |

## Deployable applications

### API (`:backend:api`)

Main HTTP service: authentication, surveys, profiles, local recommendation read-model (scores from Kafka), SSE notifications.

```bash
./gradlew :backend:api:run
```

Health: `GET /health` (alias for `/health/ready`), `GET /health/live`, `GET /health/ready`, `GET /metrics`.

### Personality worker (`:backend:personality`)

Consumes thick generation commands from `personality.generation`, calls the LLM, validates the output, and publishes a result to `personality.generation.results`. No Postgres, Redis, or Kafka.

Kotlin/Native `linuxX64` executable (no JVM app target). Uses `ktor-client-curl` for HTTPS LLM calls.
Local runs go through kind:

```bash
./gradlew linkPersonalityExecutable
./gradlew kindUp
```

`kindUp` depends on `linkPersonalityExecutable`. On **Windows** that task links inside
Linux Docker (mingw `ld.gold` cannot resolve curl's static OpenSSL). On Linux/CI it runs
`:backend:personality:linkReleaseExecutableLinuxX64` directly.

Binary: `backend/personality/build/bin/linuxX64/releaseExecutable/personality.kexe`.

| Criterion | Implementation |
|-----------|------------------|
| Command publisher in API | `PersonalityGenerationCoordinator` → `PersonalityCommandPublisher` (thick snapshot) |
| Compute only in worker | `PersonalityCommandConsumer` → LLM + validate → `PersonalityResultPublisher` |
| Result consumer in API | `PersonalityResultConsumer` → persist profile, Redis SSE, Kafka `seeker.personality_ready` |
| Retry + DLQ | up to 3 attempts on the command queue, then FAILED result + `personality.generation.dlq` |
| Generation lock | Redis lock acquired by API on enqueue, released by result consumer |
| SSE / pub-sub | `RedisPersonalityStatusNotifier` + SSE in API |

**Known gap:** no end-to-end test for "publish → consume → READY".

### Matching service (`:backend:matching`)

Consumes domain events from Kafka, recalculates scores, writes score pairs to a separate PostgreSQL (`procrush_matching`), and publishes `match.results_updated` to Kafka (`procrush.matching.results`). Display/PII fields are not stored here — the API joins cards from its own tables.

```bash
./gradlew :backend:matching:run
```

Health: `GET /health` (default port `8092`).

## Observability

API and matching use the shared module [`platform/observability`](./platform/observability) (Logback JSON/text, Micrometer, OpenTelemetry). Personality uses a thin in-process layer under [`personality/.../observability`](./personality/src/nativeMain/kotlin/jobs/procrush/personality/observability) — no OTel/Tempo spans, no Logback/Micrometer. `OTEL_*` in the shared configmap is a no-op for personality.

All three apps expose the same HTTP endpoints:

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
| `LOG_FORMAT` | `text` | `text` or `json` (JSON includes correlation fields) |
| `OTEL_ENABLED` | `false` | Enable OpenTelemetry OTLP export (api/matching only; ignored by personality) |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4317` | Tempo/OTLP collector gRPC endpoint (api/matching only) |
| `ENVIRONMENT` | `local` | Common metric label |
| `APP_VERSION` / `GIT_SHA` | `dev` | Reported in health responses |

### Correlation

HTTP requests accept/propagate `X-Request-Id`. The same ID flows through RabbitMQ personality commands/results (`X-Request-Id` header + `correlationId` on the payload) and Kafka matching events (`correlationId` in envelope). Personality diagnostics are logs by `requestId` plus Prometheus metrics — not Tempo traces.

### Local kind stack

With `LOG_FORMAT=json`, `OTEL_ENABLED=true` in [`deploy/k8s/base/configmap.yaml`](../deploy/k8s/base/configmap.yaml):

- Grafana: http://127.10.0.16:3000 (`admin` / `admin`)
- Prometheus: http://127.10.0.17:9090
- Tempo OTLP: `tempo:4317` inside cluster (api/matching traces only)

See [deploy/k8s/README.md](../deploy/k8s/README.md) for alert rules and port-forward options.

## Infrastructure dependencies

### PostgreSQL (required)

Main DB (`procrush`). Schema and reference data — in Flyway migrations (`platform/persistence/src/main/kotlin/db/migration/`) and seed (`platform/persistence/src/main/resources/db/seed/init_inserts.sql`).

Separate matching DB: `backend/matching/src/main/kotlin/db/migration/`.

### Redis (required)

**Redis** — in-memory store used for:

- recommendation cache (cache-aside, TTL 10 min);
- distributed lock during personality generation (held by the API while a command is in flight);
- session cache (PostgreSQL remains source of truth);
- pub/sub for SSE notifications about match interests, recommendation invalidation, and profile generation status (works with multiple API instances).

### RabbitMQ (required)

**RabbitMQ** — message broker for personality compute: the API enqueues a thick generation command on `personality.generation`; the personality worker returns a result on `personality.generation.results`. After 3 failed attempts the command goes to DLQ `personality.generation.dlq`.

### Kafka (required for matching)

**Kafka** — event log for matching. The API publishes domain profile events (`procrush.matching.events`), including `seeker.personality_ready` after applying a personality result; matching recalculates scores and publishes `match.results_updated` (`procrush.matching.results`); the API consumes results into `match_scores`.

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

**Requirements:** JDK 25 (Gradle toolchain; matches `deploy/docker/Dockerfile.*`), infrastructure from kind (see [deploy/k8s/README.md](../deploy/k8s/README.md)).

Environment variables — in [`deploy/k8s/base/configmap.yaml`](../deploy/k8s/base/configmap.yaml) and local `secret.yaml` (template: [`secret.yaml.example`](../deploy/k8s/base/secret.yaml.example)).

```bash
./gradlew :backend:api:run
./gradlew :backend:matching:run
# Personality is Native-only — run via kind (see above), not :backend:personality:run
```

To verify changes **inside kind** (local build → thin image → conditional rollout), use `./gradlew kindUp`. Details — [deploy/k8s/README.md](../deploy/k8s/README.md#iterative-development-gradle).

After `git clone`, before working with handlers: run `./gradlew :backend:api:compileKotlin` once so the IDE sees generated sources in `build/`. In IntelliJ: *Build and run using → Gradle*.

LLM variables for personality: `LLM_BASE_URL`, `LLM_MODEL` — in configmap; `LLM_API_KEY` — in `secret.yaml`.

## Related documentation

- [openapi/README.md](../openapi/README.md) — REST API contract
- [i18n/README.md](../i18n/README.md) — error codes
- [deploy/k8s/README.md](../deploy/k8s/README.md) — local Kubernetes stack
- [deploy/README.md](../deploy/README.md) — Railway deployment
