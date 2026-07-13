# ProCrush on Kubernetes (kind)

Local full ProCrush stack in **kind** (Kubernetes IN Docker): infrastructure, three backend services, and React frontend. External access — **http://127.10.0.10**.

Recommended local development setup. Cloud deployment — in [deploy/README.md](../README.md).

## Requirements

- [Docker](https://docs.docker.com/get-docker/) — allocate **≥ 8 GB RAM** in Docker Desktop settings
- [kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- **JDK 25** and **Node.js 20+** — for `./gradlew kindUp` and optional hot-reload (`./gradlew run` / `npm run dev`)

## Quick start

1. Copy [`deploy/k8s/base/secret.yaml.example`](./base/secret.yaml.example) to `secret.yaml` and set `LLM_API_KEY` (file is in `.gitignore`, do not commit).

2. From the repository root:

   ```bash
   ./gradlew kindUp
   ```

   If cluster `procrush` was created before changing `listenAddress`, recreate it: `./gradlew kindDown`, then `./gradlew kindUp` again. `extraPortMappings` applies only when the kind node container is created.

   `kindUp`:
   - creates cluster `procrush` (if missing), or starts stopped kind node containers (e.g. after Docker quit) and waits for the API;
   - installs ingress-nginx (if not ready);
   - builds thin app images (`deploy/Dockerfile.*.dev`) only when artifacts changed, loads them into kind;
   - applies manifests when the namespace is missing or kustomize sources changed: `kubectl apply -k deploy/k8s/overlays/kind`;
   - restarts a Deployment only if its image was rebuilt **and** the Deployment already existed (first apply does not restart).

3. Wait for pods to become ready:

   ```bash
   kubectl get pods -n procrush -w
   ```

4. Open http://127.10.0.10 — dev login (`AUTH_DEV_MODE=true`).

## Verification

| Check | Command / URL |
|-------|---------------|
| Pod status | `kubectl get pods -n procrush` |
| API logs | `kubectl logs -n procrush deploy/api -f` |
| Session (no cookie → 401) | `curl -s -o /dev/null -w "%{http_code}" http://127.10.0.10/api/auth/me` |
| Dev login | via UI at http://127.10.0.10 |

## Local endpoints

After `kindUp`, services are reachable from the host on dedicated loopback IPs (see [kind-config.yaml](./kind-config.yaml)):

| Service | Endpoint |
|---------|----------|
| Web (Ingress) | http://127.10.0.10 |
| PostgreSQL | `127.10.0.11:5432` |
| Matching PostgreSQL | `127.10.0.12:5432` |
| Redis | `127.10.0.13:6379` |
| RabbitMQ AMQP | `127.10.0.14:5672` |
| RabbitMQ UI | http://127.10.0.14:15672 (`procrush` / `procrush`) |
| Kafka | `127.10.0.15:9092` |
| Grafana | http://127.10.0.16:3000 (`admin` / `admin`) |
| Prometheus | http://127.10.0.17:9090 |

## Observability (kind overlay)

The kind overlay deploys Tempo, Prometheus, Alertmanager, and Grafana in the `procrush` namespace.

| Check | Command / URL |
|-------|---------------|
| Metrics targets | http://127.10.0.17:9090/targets |
| API metrics | `kubectl port-forward -n procrush svc/api 8080:8080` → `GET /metrics` |
| Traces | Grafana → Explore → Tempo datasource |
| Alerts | Prometheus → Alerts (rules in `deploy/k8s/overlays/kind/monitoring/prometheus/deployment.yaml`) |

Configured alerts:

- `PersonalityDlqNotEmpty` — messages in `personality.generation.dlq`
- `MatchingDlqRate` — matching Kafka DLQ counter increasing
- `MatchingConsumerDown` / `PersonalityConsumerDown` — consumer gauges at 0
- `ServiceNotReady` — Prometheus scrape target down

Recreate the kind cluster after changing `kind-config.yaml` port mappings for Grafana/Prometheus.

API check (via Ingress): `GET http://127.10.0.10/api/...` or health directly:

```bash
kubectl port-forward -n procrush svc/api 8080:8080
# GET http://localhost:8080/health
```

Matching check:

```bash
kubectl port-forward -n procrush svc/matching 8092:8092
# GET http://localhost:8092/health
```

## Authentication

Dev login is enabled in the kind stack (`AUTH_DEV_MODE=true`). **httpOnly session cookies** are used.

| Endpoint | Description |
|----------|-------------|
| `POST /api/auth/dev/login` | Dev login (requires `AUTH_DEV_MODE=true`) |
| `GET /api/auth/me` | Current user |
| `POST /api/auth/logout` | Sign out |
| `POST /api/auth/complete-registration` | Role selection (immutable) |

## In-cluster infrastructure

### Redis

**Redis** — in-memory store for recommendation cache, distributed lock during LLM generation, session cache, and pub/sub for SSE. From host: `127.10.0.13:6379`.

### RabbitMQ

**RabbitMQ** — message broker for the `personality.generation` queue. UI: http://127.10.0.14:15672. After 3 failed attempts messages go to DLQ `personality.generation.dlq`.

### Kafka + matching

**Kafka** — event log for matching recalculation. Services `kafka`, `matching-postgres`, `matching` in namespace `procrush`. The API reads recommendations from matching over HTTP (`MATCHING_SERVICE_URL` in [configmap.yaml](./base/configmap.yaml)).

## Database schema

Migrations and seed — in `backend/platform/persistence/` (main DB) and `backend/matching/` (matching DB). Resetting the namespace recreates data from Flyway and seed scripts.

## Architecture

```text
Browser → 127.10.0.10:80
    → Ingress (nginx)
        /api/*  → Service api:8080
        /*      → Service frontend:8080

Inside cluster (in-cluster DNS):
  postgres, matching-postgres, redis, rabbitmq, kafka
  api, personality, matching
```

Manifests: [base/](./base/) + overlay [overlays/kind/](./overlays/kind/) (Kustomize).

## Iterative development (Gradle)

Use the same entry point after the first bootstrap:

```bash
./gradlew kindUp
```

Local Gradle/npm builds feed thin `deploy/Dockerfile.*.dev` images. Hash gates skip docker build, `kind load`, and rollout when packaged artifacts are unchanged. Manifest apply runs only when kustomize sources change (or the namespace is missing). Rollout restart runs only when an image was rebuilt and the Deployment already existed.

| Change | What redeploys |
|--------|----------------|
| `backend/api/...` | api (if installDist artifact hash changed) |
| `i18n/locales/...` | frontend |
| `backend/contracts/...` | backend installDist for dependents; image/rollout only if artifact hash changed |
| `i18n/error-codes.yaml` | `generateI18n`, then services whose artifacts change |
| `openapi/...` | api + frontend (when their artifacts change) |
| `deploy/k8s/base` / `overlays/kind` | `kubectl apply -k` only |

Tasks are defined in the root [`build.gradle.kts`](../../build.gradle.kts). Cache files live in `.kind-deploy-cache/` (gitignored); the cache is cleared when the cluster is created or deleted via Gradle.

These tasks disable Gradle configuration cache for the invocation (Spektor / shell-out to docker are not CC-compatible). Build cache and the artifact hash gate still apply.

## Rebuild after code changes

```bash
./gradlew kindUp
```

## RabbitMQ UI (optional)

Open http://127.10.0.14:15672 — login `procrush` / `procrush`.

## Reset data

```bash
kubectl delete namespace procrush
kubectl apply -k deploy/k8s/overlays/kind
```

Or delete PVCs only:

```bash
kubectl delete pvc -n procrush --all
kubectl rollout restart statefulset -n procrush postgres matching-postgres
kubectl rollout restart deployment -n procrush redis rabbitmq kafka
```

## Stop cluster

```bash
./gradlew kindDown
```

## Hot-reload without rebuilding images (optional)

Infrastructure is reachable from the host on loopback IPs immediately after `kindUp`. Set environment variables per [`configmap.yaml`](./base/configmap.yaml) and local `secret.yaml` (from [`secret.yaml.example`](./base/secret.yaml.example)). Different projects can use standard ports on their own `127.x.x.x` addresses simultaneously.

| Application | Command | URL |
|-------------|---------|-----|
| React | `cd frontend && npm run dev` | http://localhost:8081 |
| API | `./gradlew :backend:api:run` | :8080 |
| Personality | `./gradlew :backend:personality:run` | :8091 |
| Matching | `./gradlew :backend:matching:run` | :8092 |

More on backend modules — [backend/README.md](../../backend/README.md).

## Troubleshooting

| Symptom | What to check |
|---------|---------------|
| `ImagePullBackOff` | Rebuild: `./gradlew kindUp` (or `./gradlew kindDown` then `kindUp` if the cluster was recreated outside Gradle); overlay sets `imagePullPolicy: Never` |
| API not becoming Ready | `kubectl logs -n procrush deploy/api`; often matching or Kafka still starting |
| Port 80 busy on `127.10.0.10` | Another service on same IP:port; change `listenAddress` / `hostPort` in [kind-config.yaml](./kind-config.yaml) |
| http://127.10.0.10 not opening | `kubectl get ingress -n procrush`; recreate cluster after changing `kind-config.yaml` |
| API/personality `Init:0/1` for long | RabbitMQ readiness — manifest uses `tcpSocket` + `publishNotReadyAddresses`; restart: `kubectl apply -k deploy/k8s/overlays/kind` |
| personality `Unhealthy` / 406 | HTTP probe on `/health` without JSON — uses `tcpSocket:8091` |
| `error: no matching resources found` for ingress | Re-run `./gradlew kindUp` — it waits for admission jobs and deployment rollout |
| Missing `secret.yaml` | Copy from [`secret.yaml.example`](./base/secret.yaml.example) before `kindUp` |
| `kindUp` fails after quitting Docker | Cluster still registered but nodes stopped — re-run `./gradlew kindUp` (starts containers). If API never becomes ready: `./gradlew kindDown && ./gradlew kindUp` |

## Related documentation

- [deploy/README.md](../README.md) — Railway deployment
- [backend/README.md](../../backend/README.md) — backend and infrastructure dependencies
- [frontend/README.md](../../frontend/README.md) — web client
