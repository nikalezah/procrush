# ProCrush on Kubernetes (kind)

Локальный полный стек ProCrush в **kind** (Kubernetes IN Docker): инфраструктура, три backend-сервиса и React-frontend. Снаружи — **http://127.10.0.10**.

Рекомендуемый способ локальной разработки. Облачный деплой — в [deploy/README.md](../README.md).

## Требования

- [Docker](https://docs.docker.com/get-docker/) — выделите **≥ 8 GB RAM** в настройках Docker Desktop
- [kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- **Hot-reload (опционально):** JDK 17+, Node.js 20+ для `./gradlew run` / `npm run dev`

## Быстрый старт

1. Из корня репозитория:

   ```bash
   chmod +x deploy/k8s/scripts/*.sh
   ./deploy/k8s/scripts/kind-up.sh
   ```

   Если кластер `procrush` был создан до смены `listenAddress`, пересоздайте его: `./deploy/k8s/scripts/kind-down.sh`, затем снова `./deploy/k8s/scripts/kind-up.sh`. Настройка `extraPortMappings` применяется только при создании контейнера ноды kind.

   Скрипт:
   - создаёт кластер `procrush` (если ещё нет);
   - устанавливает ingress-nginx;
   - собирает 4 Docker-образа и загружает их в kind (`kind load docker-image`);
   - применяет манифесты: `kubectl apply -k deploy/k8s/overlays/kind`.

   Перед `kind-up` скопируйте [`deploy/k8s/base/secret.yaml.example`](./base/secret.yaml.example) в `secret.yaml` и укажите `LLM_API_KEY` (файл в `.gitignore`, не коммитьте).

2. Дождитесь готовности pod'ов:

   ```bash
   kubectl get pods -n procrush -w
   ```

3. Откройте http://127.10.0.10 — dev-вход (`AUTH_DEV_MODE=true`).

## Проверка

| Проверка | Команда / URL |
|----------|----------------|
| Статус pod'ов | `kubectl get pods -n procrush` |
| Логи API | `kubectl logs -n procrush deploy/api -f` |
| Сессия (без cookie → 401) | `curl -s -o /dev/null -w "%{http_code}" http://127.10.0.10/api/auth/me` |
| Dev-login | через UI на http://127.10.0.10 |

## Локальные endpoint'ы

После `kind-up` сервисы доступны с хоста по выделенным loopback-IP (см. [kind-config.yaml](./kind-config.yaml)):

| Сервис | Endpoint |
|--------|----------|
| Web (Ingress) | http://127.10.0.10 |
| PostgreSQL | `127.10.0.11:5432` |
| Matching PostgreSQL | `127.10.0.12:5432` |
| Redis | `127.10.0.13:6379` |
| RabbitMQ AMQP | `127.10.0.14:5672` |
| RabbitMQ UI | http://127.10.0.14:15672 (`procrush` / `procrush`) |
| Kafka | `127.10.0.15:9092` |

Проверка API (через Ingress): `GET http://127.10.0.10/api/...` или health напрямую:

```bash
kubectl port-forward -n procrush svc/api 8080:8080
# GET http://localhost:8080/health
```

Проверка matching:

```bash
kubectl port-forward -n procrush svc/matching 8092:8092
# GET http://localhost:8092/health
```

## Аутентификация

В kind-стеке включён dev-вход (`AUTH_DEV_MODE=true`). Используются **httpOnly session cookies**.

| Endpoint | Описание |
|----------|----------|
| `POST /api/auth/dev/login` | Dev-вход (требует `AUTH_DEV_MODE=true`) |
| `GET /api/auth/me` | Текущий пользователь |
| `POST /api/auth/logout` | Выход |
| `POST /api/auth/complete-registration` | Выбор роли (неизменяемо) |

## Инфраструктура в кластере

### Redis

**Redis** — in-memory хранилище для кэша рекомендаций, distributed lock при LLM-генерации, кэша сессий и pub/sub для SSE. С хоста: `127.10.0.13:6379`.

### RabbitMQ

**RabbitMQ** — брокер сообщений для очереди `personality.generation`. UI: http://127.10.0.14:15672. При ошибках после 3 попыток сообщение попадает в DLQ `personality.generation.dlq`.

### Kafka + matching

**Kafka** — event log для пересчёта матчинга. Сервисы `kafka`, `matching-postgres`, `matching` в namespace `procrush`. API читает рекомендации из matching по HTTP (`MATCHING_SERVICE_URL` в [configmap.yaml](./base/configmap.yaml)).

## Схема БД

Миграции и seed — в `backend/platform/persistence/` (основная БД) и `backend/matching/` (БД матчинга). При сбросе namespace данные пересоздаются из Flyway и seed-скриптов.

## Архитектура

```text
Браузер → 127.10.0.10:80
    → Ingress (nginx)
        /api/*  → Service api:8080
        /*      → Service frontend:8080

Внутри кластера (in-cluster DNS):
  postgres, matching-postgres, redis, rabbitmq, kafka
  api, personality, matching
```

Манифесты: [base/](./base/) + overlay [overlays/kind/](./overlays/kind/) (Kustomize).

## Пересборка после изменений кода

```bash
./deploy/k8s/scripts/build-images.sh
kubectl rollout restart deployment -n procrush api personality matching frontend
```

## RabbitMQ UI (опционально)

Откройте http://127.10.0.14:15672 — логин `procrush` / `procrush`.

## Сброс данных

```bash
kubectl delete namespace procrush
kubectl apply -k deploy/k8s/overlays/kind
```

Или удалить только PVC:

```bash
kubectl delete pvc -n procrush --all
kubectl rollout restart statefulset -n procrush postgres matching-postgres
kubectl rollout restart deployment -n procrush redis rabbitmq kafka
```

## Остановка кластера

```bash
./deploy/k8s/scripts/kind-down.sh
```

## Hot-reload без пересборки образов (опционально)

Инфраструктура доступна с хоста по loopback-IP сразу после `kind-up`. Задайте переменные окружения по [`configmap.yaml`](./base/configmap.yaml) и локальному `secret.yaml` (из [`secret.yaml.example`](./base/secret.yaml.example)). Разные проекты могут одновременно использовать стандартные порты на своих `127.x.x.x` адресах.

| Приложение | Команда | URL |
|------------|---------|-----|
| React | `cd frontend && npm run dev` | http://localhost:8081 |
| API | `./gradlew :backend:api:run` | :8080 |
| Personality | `./gradlew :backend:personality:run` | :8091 |
| Matching | `./gradlew :backend:matching:run` | :8092 |

Подробнее о бэкенд-модулях — [backend/README.md](../../backend/README.md).

## Устранение неполадок

| Симптом | Что проверить |
|---------|----------------|
| `ImagePullBackOff` | Пересоберите образы: `./deploy/k8s/scripts/build-images.sh`; в overlay задано `imagePullPolicy: Never` |
| API не становится Ready | `kubectl logs -n procrush deploy/api`; часто matching или Kafka ещё стартуют |
| Порт 80 занят на `127.10.0.10` | Другой сервис на том же IP:порт; смените `listenAddress` / `hostPort` в [kind-config.yaml](./kind-config.yaml) |
| http://127.10.0.10 не открывается | `kubectl get ingress -n procrush`; пересоздайте кластер после смены `kind-config.yaml` |
| API/personality `Init:0/1` долго | RabbitMQ readiness — в манифесте `tcpSocket` + `publishNotReadyAddresses`; перезапустите: `kubectl apply -k deploy/k8s/overlays/kind` |
| personality `Unhealthy` / 406 | HTTP probe на `/health` без JSON — используется `tcpSocket:8091` |
| `error: no matching resources found` при ingress | Перезапустите `kind-up` — скрипт ждёт admission jobs и rollout deployment |

## Связанная документация

- [deploy/README.md](../README.md) — деплой на Railway
- [backend/README.md](../../backend/README.md) — бэкенд и инфраструктурные зависимости
- [frontend/README.md](../../frontend/README.md) — веб-клиент
