# ProCrush Deploy

Dockerfiles, Kubernetes manifests, and Railway configs for deploying all ProCrush services.

## Directory contents

| Path | Purpose |
|------|---------|
| [`Dockerfile.api`](./Dockerfile.api) | Ktor API image |
| [`Dockerfile.personality`](./Dockerfile.personality) | Personality worker image |
| [`Dockerfile.matching`](./Dockerfile.matching) | Matching service image |
| [`Dockerfile.frontend`](./Dockerfile.frontend) | React + nginx |
| [`railway.*.toml`](./) | Railway configs for personality, matching, frontend |
| [`k8s/`](./k8s/README.md) | Local full stack in kind (Kubernetes) |

API config on Railway — [`railway.toml`](../railway.toml) at the repository root.

## Local development (kind)

The recommended way to run the full stack locally is Kubernetes in Docker via [kind](https://kind.sigs.k8s.io/). Details — in [k8s/README.md](./k8s/README.md).

```bash
chmod +x deploy/k8s/scripts/*.sh
./deploy/k8s/scripts/kind-up.sh
```

Open http://127.10.0.10 — dev login (`AUTH_DEV_MODE=true`).

## Railway deployment (GitHub)

One Railway project with nine services: **Postgres**, **Matching Postgres**, **Redis**, **RabbitMQ**, **Kafka**, **Backend** (Ktor API), **Personality**, **Matching**, **Frontend** (React + nginx). Users only open the frontend URL; nginx proxies `/api/*` to the backend over Railway private network.

### Service architecture

| Service | Root Directory | Config file (from repo root) |
|---------|----------------|------------------------------|
| Backend | **empty** (repo root) | `/railway.toml` |
| Personality | **empty** | `/deploy/railway.personality.toml` |
| Matching | **empty** | `/deploy/railway.matching.toml` |
| Frontend | **empty** | `/deploy/railway.frontend.toml` |
| Postgres | — | — |
| Matching Postgres | — | — |
| Redis | — | — |
| RabbitMQ | — | — (Railway template / Docker image) |
| Kafka | — | — (Railway template / Redpanda / Upstash) |

Images are built **from the repository root** (backend — `backend/`; frontend — `deploy/Dockerfile.frontend`).

For backend **do not use** Railpack/Nixpacks auto-detect — only `builder = "DOCKERFILE"` in config.

Service names in `${{...}}` are **case-sensitive** (e.g. `Backend`, `Frontend`, `Postgres`).

### Connect GitHub

1. Create an empty repository on GitHub (account linked to Railway).
2. Push the project:

```powershell
cd C:\path\to\procrush
git remote add origin https://github.com/<user>/<repo>.git
git push -u origin master
```

Use `main` instead of `master` if that is the default branch on GitHub.

### Railway setup (one-time)

The project should already have **Postgres**. Add application services and connect each to the **same** GitHub repository and branch.

#### Backend

1. **+ New** → **Empty Service** → name `Backend`.
2. **Settings → Source**: GitHub repo and branch.
3. **Settings → Root Directory**: **empty**.
4. **Settings → Config file**: `/railway.toml`.
5. **Variables**:

   | Variable | Value |
   |----------|-------|
   | `DATABASE_URL` | `${{Postgres.DATABASE_URL}}` |
   | `REDIS_URL` | `${{Redis.REDIS_URL}}` |
   | `RABBITMQ_URL` | `${{RabbitMQ.RABBITMQ_URL}}` (or your RabbitMQ service URL) |
   | `WEB_ORIGIN` | `https://${{Frontend.RAILWAY_PUBLIC_DOMAIN}}` (after frontend domain exists) |
   | `FRONTEND_URL` | same as `WEB_ORIGIN` |
   | `AUTH_DEV_MODE` | `false` (prod) or `true` (staging) |
   | `MATCHING_SERVICE_URL` | `http://${{Matching.RAILWAY_PRIVATE_DOMAIN}}:8092` |

6. Deploy (automatic on push or **Deploy** in dashboard).
7. Public domain optional (health: `GET /health`).

#### Personality

1. **+ New** → **Empty Service** → name `Personality`.
2. **Settings → Source**: **same** repo and branch.
3. **Settings → Root Directory**: **empty**.
4. **Settings → Config file**: `/deploy/railway.personality.toml`.
5. **Variables**:

   | Variable | Value |
   |----------|-------|
   | `DATABASE_URL` | `${{Postgres.DATABASE_URL}}` |
   | `REDIS_URL` | `${{Redis.REDIS_URL}}` |
   | `RABBITMQ_URL` | `${{RabbitMQ.RABBITMQ_URL}}` |
   | `LLM_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta/openai` |
   | `LLM_MODEL` | `gemini-3.1-flash-lite` |
   | `LLM_API_KEY` | provider key (Gemini / OpenRouter, etc.) |
   | `WORKER_HEALTH_PORT` | `8091` locally; on Railway optional — `PORT` is used |

6. **Networking → Public Networking**: optional (health: `GET /health` on worker port).
7. Deploy.

#### Matching

1. **+ New** → **Empty Service** → name `Matching`.
2. **Settings → Source**: **same** repo and branch.
3. **Settings → Root Directory**: **empty**.
4. **Settings → Config file**: `/deploy/railway.matching.toml`.
5. **Variables**:

   | Variable | Value |
   |----------|-------|
   | `MATCHING_DATABASE_URL` | `${{Matching Postgres.DATABASE_URL}}` |
   | `KAFKA_BOOTSTRAP_SERVERS` | your Kafka service URL |
   | `REDIS_URL` | `${{Redis.REDIS_URL}}` |

6. Deploy.

#### Frontend

1. **+ New** → **Empty Service** → name `Frontend`.
2. **Settings → Source**: **same** repo and branch.
3. **Settings → Root Directory**: **empty**.
4. **Settings → Config file**: `/deploy/railway.frontend.toml`.
5. **Variables**:

   | Variable | Value |
   |----------|-------|
   | `BACKEND_UPSTREAM` | `${{Backend.RAILWAY_PRIVATE_DOMAIN}}:8080` |

   Use exact service names. **Do not** use `${{Backend.PORT}}` — cross-service `PORT` references are often empty and nginx fails with `invalid port in upstream`.

   API listens on port `8080` (`deploy/Dockerfile.api`, Ktor default without `PORT`).

6. **Networking → Public Networking**: **Generate Domain** (required for users).
7. Deploy.

#### After frontend URL is available

If `WEB_ORIGIN` / `FRONTEND_URL` were not set via `${{Frontend.RAILWAY_PUBLIC_DOMAIN}}` before the domain was created — set them and redeploy **Backend**.

### Deployment order

1. Postgres, Matching Postgres (if separate)
2. Redis, RabbitMQ, Kafka
3. Backend (`/health`, Flyway in logs)
4. Personality (`/health`, consumer in logs)
5. Matching (`/health`)
6. Frontend (public domain + `BACKEND_UPSTREAM`)
7. Redeploy Backend if `WEB_ORIGIN` / `FRONTEND_URL` need updating

### Verification

| Check | How |
|-------|-----|
| API health | `GET /health` → `{"status":"ok","redis":"ok","rabbitmq":"ok"}` |
| Worker health | `GET http://<worker-domain>/health` (service port on Railway) |
| Matching health | `GET http://<matching-domain>/health` |
| Frontend | `https://<frontend-domain>/` |
| API via proxy | Login with `AUTH_DEV_MODE=true` on backend |
| Build | Deploy logs show **Dockerfile**, not **Railpack** |

### Railway vs local

- Containers have no `.env` — variables are set in the Railway dashboard.
- Railway sets `PORT` for application services.
- `DATABASE_URL` from Postgres is `postgresql://...`; the server adds JDBC `sslmode=require`.

## Related documentation

- [k8s/README.md](./k8s/README.md) — local Kubernetes (kind)
- [backend/README.md](../backend/README.md) — backend and infrastructure dependencies
- [frontend/README.md](../frontend/README.md) — web client
