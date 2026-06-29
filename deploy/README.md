# ProCrush Deploy

Dockerfile'ы, манифесты Kubernetes и конфиги Railway для деплоя всех сервисов ProCrush.

## Содержимое каталога

| Путь | Назначение |
|------|------------|
| [`Dockerfile.api`](./Dockerfile.api) | Образ Ktor API |
| [`Dockerfile.personality`](./Dockerfile.personality) | Образ personality worker |
| [`Dockerfile.matching`](./Dockerfile.matching) | Образ matching service |
| [`Dockerfile.frontend`](./Dockerfile.frontend) | React + nginx |
| [`railway.*.toml`](./) | Конфиги Railway для personality, matching, frontend |
| [`k8s/`](./k8s/README.md) | Локальный полный стек в kind (Kubernetes) |

Конфиг API на Railway — [`railway.toml`](../railway.toml) в корне репозитория.

## Локальная разработка (kind)

Рекомендуемый способ поднять полный стек локально — Kubernetes в Docker через [kind](https://kind.sigs.k8s.io/). Подробности — в [k8s/README.md](./k8s/README.md).

```bash
chmod +x deploy/k8s/scripts/*.sh
./deploy/k8s/scripts/kind-up.sh
```

Откройте http://127.10.0.10 — dev-вход (`AUTH_DEV_MODE=true`).

## Деплой на Railway (GitHub)

В одном проекте Railway девять сервисов: **Postgres**, **Matching Postgres**, **Redis**, **RabbitMQ**, **Kafka**, **Backend** (Ktor API), **Personality**, **Matching**, **Frontend** (React + nginx). Пользователи открывают только URL фронтенда; nginx проксирует `/api/*` на backend по приватной сети Railway.

### Архитектура сервисов

| Сервис | Root Directory | Config file (от корня репо) |
|--------|----------------|----------------------------|
| Backend | **пусто** (корень репо) | `/railway.toml` |
| Personality | **пусто** | `/deploy/railway.personality.toml` |
| Matching | **пусто** | `/deploy/railway.matching.toml` |
| Frontend | **пусто** | `/deploy/railway.frontend.toml` |
| Postgres | — | — |
| Matching Postgres | — | — |
| Redis | — | — |
| RabbitMQ | — | — (Railway template / Docker image) |
| Kafka | — | — (Railway template / Redpanda / Upstash) |

Образы собираются **из корня репозитория** (backend — `backend/`; frontend — `deploy/Dockerfile.frontend`).

Для backend **не используйте** Railpack/Nixpacks auto-detect — только `builder = "DOCKERFILE"` в конфиге.

Имена сервисов в `${{...}}` **чувствительны к регистру** (например, `Backend`, `Frontend`, `Postgres`).

### Подключение GitHub

1. Создайте пустой репозиторий на GitHub (аккаунт, связанный с Railway).
2. Запушьте проект:

```powershell
cd C:\path\to\procrush
git remote add origin https://github.com/<user>/<repo>.git
git push -u origin master
```

Используйте `main` вместо `master`, если это ветка по умолчанию на GitHub.

### Настройка в Railway (один раз)

В проекте уже должен быть **Postgres**. Добавьте application-сервисы и подключите к каждому **тот же** GitHub-репозиторий и ветку.

#### Backend

1. **+ New** → **Empty Service** → имя `Backend`.
2. **Settings → Source**: GitHub-репозиторий и ветка.
3. **Settings → Root Directory**: **пусто**.
4. **Settings → Config file**: `/railway.toml`.
5. **Variables**:

   | Переменная | Значение |
   |------------|----------|
   | `DATABASE_URL` | `${{Postgres.DATABASE_URL}}` |
   | `REDIS_URL` | `${{Redis.REDIS_URL}}` |
   | `RABBITMQ_URL` | `${{RabbitMQ.RABBITMQ_URL}}` (или URL вашего RabbitMQ-сервиса) |
   | `WEB_ORIGIN` | `https://${{Frontend.RAILWAY_PUBLIC_DOMAIN}}` (после появления домена у frontend) |
   | `FRONTEND_URL` | то же, что `WEB_ORIGIN` |
   | `AUTH_DEV_MODE` | `false` (prod) или `true` (staging) |
   | `MATCHING_SERVICE_URL` | `http://${{Matching.RAILWAY_PRIVATE_DOMAIN}}:8092` |

6. Деплой (автоматически при push или **Deploy** в dashboard).
7. Публичный домен опционален (health: `GET /health`).

#### Personality

1. **+ New** → **Empty Service** → имя `Personality`.
2. **Settings → Source**: **тот же** репозиторий и ветка.
3. **Settings → Root Directory**: **пусто**.
4. **Settings → Config file**: `/deploy/railway.personality.toml`.
5. **Variables**:

   | Переменная | Значение |
   |------------|----------|
   | `DATABASE_URL` | `${{Postgres.DATABASE_URL}}` |
   | `REDIS_URL` | `${{Redis.REDIS_URL}}` |
   | `RABBITMQ_URL` | `${{RabbitMQ.RABBITMQ_URL}}` |
   | `LLM_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta/openai` |
   | `LLM_MODEL` | `gemini-3.1-flash-lite` |
   | `LLM_API_KEY` | ключ провайдера (Gemini / OpenRouter и т.д.) |
   | `WORKER_HEALTH_PORT` | `8091` локально; на Railway можно не задавать — используется `PORT` |

6. **Networking → Public Networking**: опционально (health: `GET /health` на порту worker).
7. Деплой.

#### Matching

1. **+ New** → **Empty Service** → имя `Matching`.
2. **Settings → Source**: **тот же** репозиторий и ветка.
3. **Settings → Root Directory**: **пусто**.
4. **Settings → Config file**: `/deploy/railway.matching.toml`.
5. **Variables**:

   | Переменная | Значение |
   |------------|----------|
   | `MATCHING_DATABASE_URL` | `${{Matching Postgres.DATABASE_URL}}` |
   | `KAFKA_BOOTSTRAP_SERVERS` | URL вашего Kafka-сервиса |
   | `REDIS_URL` | `${{Redis.REDIS_URL}}` |

6. Деплой.

#### Frontend

1. **+ New** → **Empty Service** → имя `Frontend`.
2. **Settings → Source**: **тот же** репозиторий и ветка.
3. **Settings → Root Directory**: **пусто**.
4. **Settings → Config file**: `/deploy/railway.frontend.toml`.
5. **Variables**:

   | Переменная | Значение |
   |------------|----------|
   | `BACKEND_UPSTREAM` | `${{Backend.RAILWAY_PRIVATE_DOMAIN}}:8080` |

   Используйте точные имена сервисов. **Не** используйте `${{Backend.PORT}}` — cross-service ссылки на `PORT` часто пустые, nginx падает с `invalid port in upstream`.

   API слушает порт `8080` (`deploy/Dockerfile.api`, Ktor по умолчанию без `PORT`).

6. **Networking → Public Networking**: **Generate Domain** (обязательно для пользователей).
7. Деплой.

#### После появления URL у frontend

Если `WEB_ORIGIN` / `FRONTEND_URL` не были заданы через `${{Frontend.RAILWAY_PUBLIC_DOMAIN}}` до создания домена — установите их и передеплойте **Backend**.

### Порядок деплоя

1. Postgres, Matching Postgres (если отдельно)
2. Redis, RabbitMQ, Kafka
3. Backend (`/health`, Flyway в логах)
4. Personality (`/health`, consumer в логах)
5. Matching (`/health`)
6. Frontend (публичный домен + `BACKEND_UPSTREAM`)
7. Повторный деплой Backend, если нужно обновить `WEB_ORIGIN` / `FRONTEND_URL`

### Проверка

| Проверка | Как |
|----------|-----|
| Health API | `GET /health` → `{"status":"ok","redis":"ok","rabbitmq":"ok"}` |
| Health Worker | `GET http://<worker-domain>/health` (порт сервиса на Railway) |
| Health Matching | `GET http://<matching-domain>/health` |
| Frontend | `https://<frontend-domain>/` |
| API через прокси | Вход при `AUTH_DEV_MODE=true` на backend |
| Сборка | В логах деплоя — **Dockerfile**, не **Railpack** |

### Railway vs локально

- В контейнерах нет `.env` — переменные задаются в Railway dashboard.
- Railway выставляет `PORT` для application-сервисов.
- `DATABASE_URL` от Postgres — `postgresql://...`; сервер добавляет JDBC `sslmode=require`.

## Связанная документация

- [k8s/README.md](./k8s/README.md) — локальный Kubernetes (kind)
- [backend/README.md](../backend/README.md) — бэкенд и инфраструктурные зависимости
- [frontend/README.md](../frontend/README.md) — веб-клиент
