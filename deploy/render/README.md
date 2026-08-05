# Render

App services are **image-backed**: they pull `ghcr.io/<owner>/procrush-*` built by [`.github/workflows/deploy-master.yml`](../../.github/workflows/deploy-master.yml). CI does not ask Render to build from a Dockerfile.

## Blueprint

[`blueprint.yaml`](./blueprint.yaml) defines:

| Kind | Name | Notes |
|------|------|-------|
| `web` + `runtime: image` | `api`, `frontend` | Public HTTP |
| `worker` + `runtime: image` | `personality`, `matching` | Background / internal HTTP |
| Postgres | `postgres`, `matching-postgres` | Main + matching DBs |
| Key Value | `redis` | Redis-compatible |
| `pserv` + `runtime: image` | `rabbitmq`, `kafka` | Stock images; not built by ProCrush CI |

Secrets use `sync: false` (set once in the dashboard). Database and Key Value URLs use `fromDatabase` / `fromService`.

### Apply

1. In the Render Dashboard → **New** → **Blueprint**.
2. Point at this repo and `deploy/render/blueprint.yaml` (or copy/rename to `render.yaml` at the root if you prefer Render’s default path).
3. Fill prompts for secrets (`LLM_API_KEY`, RabbitMQ/Kafka credentials, etc.).
4. Confirm image URLs match your GHCR owner (`ghcr.io/nikalezah/procrush-*` in this repo).

Images stay on the moving `:master` tag. After each successful GHCR push, Actions hits the per-service **deploy hook** so Render pulls the new digest.

## Deploy hooks (CI)

For each app service: **Settings → Deploy Hook** → copy the URL into the matching GitHub Actions secret:

| Service | Secret |
|---------|--------|
| api | `RENDER_DEPLOY_HOOK_API` |
| personality | `RENDER_DEPLOY_HOOK_PERSONALITY` |
| matching | `RENDER_DEPLOY_HOOK_MATCHING` |
| frontend | `RENDER_DEPLOY_HOOK_FRONTEND` |

## Infra notes

- **Postgres ×2** — main app DB and matching DB.
- **Key Value** — Redis URL for API/matching (`REDIS_URL`).
- **RabbitMQ / Kafka** — private Docker services in the Blueprint using public images (`rabbitmq:3-management`, `apache/kafka` or Redpanda). Tune credentials and advertised listeners in the dashboard; ProCrush CI only rebuilds the four app images.

## Verification

| Check | How |
|-------|-----|
| API | `GET https://<api>/health` |
| Personality / Matching | worker logs + health on service port |
| Frontend | public URL; `/api/*` proxied via `BACKEND_UPSTREAM` |
| Redeploy | Actions job “Redeploy Render” after an image push |
