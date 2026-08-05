# Railway

App services are **image-backed**. They pull the same GHCR images as Render (`ghcr.io/<owner>/procrush-<service>:master`). Railway does **not** build from a Dockerfile in this repo (legacy `railway.toml` / `deploy/railway.*.toml` Dockerfile builders were removed).

## One-time project setup

Create one Railway project with infrastructure plus four app services.

### Infrastructure

| Service | How |
|---------|-----|
| Postgres | Railway Postgres plugin → `DATABASE_URL` for Backend |
| Matching Postgres | Second Postgres → `MATCHING_DATABASE_URL` for Matching |
| Redis | Railway Redis / Key Value → `REDIS_URL` |
| RabbitMQ | Template or Docker image (`rabbitmq:3-management`) → `RABBITMQ_URL` |
| Kafka | Template / Redpanda / managed Kafka → `KAFKA_BOOTSTRAP_SERVERS` |

Users only open the **Frontend** public URL; nginx proxies `/api/*` to the Backend over the Railway private network.

### App services (Docker Image source)

For each of **Backend**, **Personality**, **Matching**, **Frontend**:

1. **+ New** → **Docker Image** (empty service, then set source to image).
2. Image path (this repo):

   | Service | Image |
   |---------|-------|
   | Backend (api) | `ghcr.io/nikalezah/procrush-api:master` |
   | Personality | `ghcr.io/nikalezah/procrush-personality:master` |
   | Matching | `ghcr.io/nikalezah/procrush-matching:master` |
   | Frontend | `ghcr.io/nikalezah/procrush-frontend:master` |

3. Packages are public with the public GitHub repo — no registry credentials required.
4. Set variables below, then deploy once so the service exists before CI redeploys.

### Variables

#### Backend

| Variable | Value |
|----------|-------|
| `DATABASE_URL` | `${{Postgres.DATABASE_URL}}` |
| `REDIS_URL` | `${{Redis.REDIS_URL}}` |
| `RABBITMQ_URL` | `${{RabbitMQ.RABBITMQ_URL}}` (or your broker URL) |
| `WEB_ORIGIN` | `https://${{Frontend.RAILWAY_PUBLIC_DOMAIN}}` |
| `FRONTEND_URL` | same as `WEB_ORIGIN` |
| `AUTH_DEV_MODE` | `false` (prod) or `true` (staging) |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap |
| `KAFKA_MATCHING_RESULTS_TOPIC` | `procrush.matching.results` (optional) |
| `KAFKA_MATCHING_RESULTS_CONSUMER_GROUP` | `api-matching-results` (optional) |

Health: `GET /health`. Public domain optional.

#### Personality

| Variable | Value |
|----------|-------|
| `RABBITMQ_URL` | `${{RabbitMQ.RABBITMQ_URL}}` |
| `LLM_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta/openai` |
| `LLM_MODEL` | `gemini-3.1-flash-lite` |
| `LLM_API_KEY` | provider key |
| `WORKER_HEALTH_PORT` | optional on Railway — `PORT` is used |

#### Matching

| Variable | Value |
|----------|-------|
| `MATCHING_DATABASE_URL` | `${{Matching Postgres.DATABASE_URL}}` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap |
| `REDIS_URL` | `${{Redis.REDIS_URL}}` |

#### Frontend

| Variable | Value |
|----------|-------|
| `BACKEND_UPSTREAM` | `${{Backend.RAILWAY_PRIVATE_DOMAIN}}:8080` |

Do **not** use `${{Backend.PORT}}` — cross-service `PORT` references are often empty and nginx fails with `invalid port in upstream`. API listens on `8080` inside the image.

Generate a **public domain** for Frontend. After it exists, redeploy Backend if `WEB_ORIGIN` / `FRONTEND_URL` were empty.

### Deployment order

1. Postgres, Matching Postgres  
2. Redis, RabbitMQ, Kafka  
3. Backend → Personality → Matching → Frontend  
4. Redeploy Backend if public frontend URL bindings need updating  

## CI redeploy

On push to `master`, Actions builds/pushes changed images (including `:master`), then runs:

```bash
railway redeploy --service <service-id> --yes
```

with `RAILWAY_TOKEN` (project token for the target environment). Configure GitHub secrets:

| Secret | Purpose |
|--------|---------|
| `RAILWAY_TOKEN` | Project token |
| `RAILWAY_SERVICE_ID_API` | Backend service id |
| `RAILWAY_SERVICE_ID_PERSONALITY` | Personality service id |
| `RAILWAY_SERVICE_ID_MATCHING` | Matching service id |
| `RAILWAY_SERVICE_ID_FRONTEND` | Frontend service id |

Redeploy pulls the updated `:master` digest. No Dockerfile builder settings remain in-repo.

## Verification

| Check | How |
|-------|-----|
| API health | `GET /health` → redis/rabbitmq ok |
| Worker / matching | `/health` on service port |
| Frontend | `https://<frontend-domain>/` |
| API via proxy | Login with staging `AUTH_DEV_MODE` if enabled |
| Deploy source | Dashboard shows **Docker Image**, not Dockerfile/Railpack |

## Railway vs local

- No `.env` in containers — set variables in the Railway dashboard.
- Railway injects `PORT` for application services.
- Postgres `DATABASE_URL` is `postgresql://...`; the server adds JDBC `sslmode=require` when needed.
