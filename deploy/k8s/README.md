# ProCrush on Kubernetes (kind)

Local full ProCrush stack in **kind** (Kubernetes IN Docker): infrastructure, three backend services, and React frontend. External access — **http://127.10.0.10**.

Recommended local development setup. Cloud deployment — in [deploy/README.md](../README.md).

## Requirements

- [Docker](https://docs.docker.com/get-docker/) — allocate **≥ 8 GB RAM** in Docker Desktop settings
- [kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- **Hot-reload (optional):** JDK 17+, Node.js 20+ for `./gradlew run` / `npm run dev`

## Quick start

1. From the repository root:

   ```bash
   chmod +x deploy/k8s/scripts/*.sh
   ./deploy/k8s/scripts/kind-up.sh
   ```

   If cluster `procrush` was created before changing `listenAddress`, recreate it: `./deploy/k8s/scripts/kind-down.sh`, then `./deploy/k8s/scripts/kind-up.sh` again. `extraPortMappings` applies only when the kind node container is created.

   The script:
   - creates cluster `procrush` (if missing);
   - installs ingress-nginx;
   - builds 4 Docker images and loads them into kind (`kind load docker-image`);
   - applies manifests: `kubectl apply -k deploy/k8s/overlays/kind`.

   Before `kind-up`, copy [`deploy/k8s/base/secret.yaml.example`](./base/secret.yaml.example) to `secret.yaml` and set `LLM_API_KEY` (file is in `.gitignore`, do not commit).

2. Wait for pods to become ready:

   ```bash
   kubectl get pods -n procrush -w
   ```

3. Open http://127.10.0.10 — dev login (`AUTH_DEV_MODE=true`).

## Verification

| Check | Command / URL |
|-------|---------------|
| Pod status | `kubectl get pods -n procrush` |
| API logs | `kubectl logs -n procrush deploy/api -f` |
| Session (no cookie → 401) | `curl -s -o /dev/null -w "%{http_code}" http://127.10.0.10/api/auth/me` |
| Dev login | via UI at http://127.10.0.10 |

## Local endpoints

After `kind-up`, services are reachable from the host on dedicated loopback IPs (see [kind-config.yaml](./kind-config.yaml)):

| Service | Endpoint |
|---------|----------|
| Web (Ingress) | http://127.10.0.10 |
| PostgreSQL | `127.10.0.11:5432` |
| Matching PostgreSQL | `127.10.0.12:5432` |
| Redis | `127.10.0.13:6379` |
| RabbitMQ AMQP | `127.10.0.14:5672` |
| RabbitMQ UI | http://127.10.0.14:15672 (`procrush` / `procrush`) |
| Kafka | `127.10.0.15:9092` |

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

## Rebuild after code changes

```bash
./deploy/k8s/scripts/build-images.sh
kubectl rollout restart deployment -n procrush api personality matching frontend
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
./deploy/k8s/scripts/kind-down.sh
```

## Hot-reload without rebuilding images (optional)

Infrastructure is reachable from the host on loopback IPs immediately after `kind-up`. Set environment variables per [`configmap.yaml`](./base/configmap.yaml) and local `secret.yaml` (from [`secret.yaml.example`](./base/secret.yaml.example)). Different projects can use standard ports on their own `127.x.x.x` addresses simultaneously.

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
| `ImagePullBackOff` | Rebuild images: `./deploy/k8s/scripts/build-images.sh`; overlay sets `imagePullPolicy: Never` |
| API not becoming Ready | `kubectl logs -n procrush deploy/api`; often matching or Kafka still starting |
| Port 80 busy on `127.10.0.10` | Another service on same IP:port; change `listenAddress` / `hostPort` in [kind-config.yaml](./kind-config.yaml) |
| http://127.10.0.10 not opening | `kubectl get ingress -n procrush`; recreate cluster after changing `kind-config.yaml` |
| API/personality `Init:0/1` for long | RabbitMQ readiness — manifest uses `tcpSocket` + `publishNotReadyAddresses`; restart: `kubectl apply -k deploy/k8s/overlays/kind` |
| personality `Unhealthy` / 406 | HTTP probe on `/health` without JSON — uses `tcpSocket:8091` |
| `error: no matching resources found` for ingress | Re-run `kind-up` — script waits for admission jobs and deployment rollout |

## Related documentation

- [deploy/README.md](../README.md) — Railway deployment
- [backend/README.md](../../backend/README.md) — backend and infrastructure dependencies
- [frontend/README.md](../../frontend/README.md) — web client
