# ProCrush Backend

Kotlin-бэкенд: Ktor HTTP API, фоновые worker'ы и доменная логика. Три deployable-приложения (API, personality, matching) и библиотечные модули.

## Структура модулей

| Путь | Gradle-модуль | Назначение |
|------|---------------|------------|
| [`contracts/`](./contracts/src/main/kotlin) | `:backend:contracts` | DTO доменного слоя, события, порты и чистая доменная логика (синхронизируются с OpenAPI через `ApiMappers` в `api`) |
| [`config/`](./config/src/main/kotlin) | `:backend:config` | Чтение env и типизированные настройки приложений |
| [`platform/`](./platform) | `:backend:platform:*` | Redis, RabbitMQ, Kafka, LLM, Flyway main DB (`persistence`) |
| [`domain/`](./domain) | `:backend:domain:*` | Bounded contexts: auth, seeker, employer, survey, matching, personality |
| [`api/`](./api/src/main/kotlin) | `:backend:api` | Ktor HTTP API, Spektor-generated routes/DTO в `build/`, handlers, composition root |
| [`personality/`](./personality) | `:backend:personality` | Deployable app: RabbitMQ consumer + health endpoint |
| [`domain/personality/`](./domain/personality) | `:backend:domain:personality-lib` | Библиотека домена (координатор, publisher, worker-логика) |
| [`matching/`](./matching) | `:backend:matching` | Kafka consumer + HTTP read API, отдельная БД матчинга |

## Deployable-приложения

### API (`:backend:api`)

Главный HTTP-сервис: аутентификация, опросы, профили, проксирование рекомендаций из matching, SSE-уведомления.

```bash
./gradlew :backend:api:run
```

Health: `GET /health` → `{"status":"ok","redis":"ok","rabbitmq":"ok"}`.

### Personality worker (`:backend:personality`)

Потребляет очередь `personality.generation`, вызывает LLM, сохраняет личностный профиль.

```bash
./gradlew :backend:personality:run
```

| Критерий | Реализация |
|----------|------------|
| Publisher в API | `PersonalityGenerationCoordinator` → `PersonalityJobPublisher` |
| Consumer только в worker | `AppContext` не стартует consumer; `WorkerContext` — да |
| Retry + DLQ | до 3 попыток, затем `personality.generation.dlq` |
| Distributed lock + dedup | Redis lock и `PersonalityMessageDedup` |
| SSE / pub-sub | `RedisPersonalityStatusNotifier` + SSE в API |

**Известный пробел:** нет end-to-end теста «publish → consume → READY».

### Matching service (`:backend:matching`)

Потребляет доменные события из Kafka, пересчитывает рекомендации, пишет в отдельную PostgreSQL (`procrush_matching`). API читает рекомендации по HTTP (`MATCHING_SERVICE_URL` обязателен).

```bash
./gradlew :backend:matching:run
```

Health: `GET /health` (порт по умолчанию `8092`).

## Инфраструктурные зависимости

### PostgreSQL (обязателен)

Основная БД (`procrush`). Схема и справочные данные — в Flyway-миграциях (`platform/persistence/src/main/kotlin/db/migration/`) и seed (`platform/persistence/src/main/resources/db/seed/init_inserts.sql`).

Отдельная БД matching: `backend/matching/src/main/kotlin/db/migration/`.

### Redis (обязателен)

**Redis** — in-memory хранилище; используется для:

- кэша рекомендаций (cache-aside, TTL 10 мин);
- distributed lock при LLM-генерации личностного профиля (держит worker);
- кэша сессий (PostgreSQL остаётся source of truth);
- pub/sub для SSE-уведомлений о новых откликах и статусе генерации профиля (работает при нескольких инстансах API).

### RabbitMQ (обязателен)

**RabbitMQ** — брокер сообщений: API кладёт задачу «сгенерировать личностный профиль» в очередь `personality.generation`; worker забирает задачу и вызывает LLM. При ошибках после 3 попыток сообщение попадает в DLQ `personality.generation.dlq`.

### Kafka (обязателен для matching)

**Kafka** — event log для пересчёта матчинга. API и personality публикуют доменные события; matching потребляет их и обновляет проекции в своей БД.

## Аутентификация

Используются **httpOnly session cookies**.

| Endpoint | Описание |
|----------|----------|
| `POST /api/auth/dev/login` | Dev-вход (требует `AUTH_DEV_MODE=true`) |
| `GET /api/auth/me` | Текущий пользователь |
| `POST /api/auth/logout` | Выход |
| `POST /api/auth/complete-registration` | Выбор роли (неизменяемо) |

Полный список REST-эндпоинтов — в [openapi/README.md](../openapi/README.md).

## Локальная разработка (hot-reload)

**Требования:** JDK 17+, инфраструктура из kind (см. [deploy/k8s/README.md](../deploy/k8s/README.md)).

Переменные окружения — в [`deploy/k8s/base/configmap.yaml`](../deploy/k8s/base/configmap.yaml) и локальном `secret.yaml` (шаблон: [`secret.yaml.example`](../deploy/k8s/base/secret.yaml.example)).

```bash
./gradlew :backend:api:run
./gradlew :backend:personality:run
./gradlew :backend:matching:run
```

После `git clone` перед работой с handlers: один раз `./gradlew :backend:api:compileKotlin`, чтобы IDE увидела generated sources в `build/`. В IntelliJ: *Build and run using → Gradle*.

Переменные LLM для personality: `LLM_BASE_URL`, `LLM_MODEL` — в configmap; `LLM_API_KEY` — в `secret.yaml`.

## Связанная документация

- [openapi/README.md](../openapi/README.md) — контракт REST API
- [i18n/README.md](../i18n/README.md) — коды ошибок
- [deploy/k8s/README.md](../deploy/k8s/README.md) — локальный стек в Kubernetes
- [deploy/README.md](../deploy/README.md) — деплой на Railway
