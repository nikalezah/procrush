# ProCrush on Kubernetes (kind)

Локальный полный стек ProCrush в **kind** (Kubernetes IN Docker): инфраструктура, три backend-сервиса и React-frontend. Снаружи — **один URL**: `http://procrush.local` (порт 80 хоста).

## Требования

- [Docker](https://docs.docker.com/get-docker/) — выделите **≥ 8 GB RAM** в настройках Docker Desktop
- [kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)

## Быстрый старт

1. Добавьте в файл hosts (от имени администратора):

   ```
   127.10.0.10 procrush.local
   127.10.0.11 postgres.procrush.local
   127.10.0.12 matching-postgres.procrush.local
   127.10.0.13 redis.procrush.local
   127.10.0.14 rabbitmq.procrush.local
   127.10.0.15 kafka.procrush.local
   ```

   Windows: `C:\Windows\System32\drivers\etc\hosts`

2. Из корня репозитория:

   **Windows (PowerShell):**

   ```powershell
   .\deploy\k8s\scripts\kind-up.ps1
   ```

   Если кластер `procrush` был создан до перехода на адрес `127.10.0.10`, пересоздайте его: `.\deploy\k8s\scripts\kind-down.ps1`, затем снова `.\deploy\k8s\scripts\kind-up.ps1`. Настройка `listenAddress` применяется только при создании контейнера ноды kind.

   **Linux / macOS:**

   ```bash
   chmod +x deploy/k8s/scripts/*.sh
   ./deploy/k8s/scripts/kind-up.sh
   ```

   Скрипт:
   - создаёт кластер `procrush` (если ещё нет);
   - устанавливает ingress-nginx;
   - собирает 4 Docker-образа и загружает их в kind (`kind load docker-image`);
   - применяет манифесты: `kubectl apply -k deploy/k8s/overlays/kind`.

3. Дождитесь готовности pod'ов:

   ```bash
   kubectl get pods -n procrush -w
   ```

4. Откройте http://procrush.local — dev-вход (`AUTH_DEV_MODE=true`).

## Проверка

| Проверка | Команда / URL |
|----------|----------------|
| Статус pod'ов | `kubectl get pods -n procrush` |
| Логи API | `kubectl logs -n procrush deploy/api -f` |
| Сессия (без cookie → 401) | `curl -s -o /dev/null -w "%{http_code}" http://procrush.local/api/auth/me` |
| Dev-login | через UI на http://procrush.local |

## Локальные endpoint'ы инфраструктуры

После `kind-up` сервисы доступны с хоста без `kubectl port-forward`:

| Сервис | Endpoint |
|--------|----------|
| PostgreSQL | `postgres.procrush.local:5432` |
| Matching PostgreSQL | `matching-postgres.procrush.local:5432` |
| Redis | `redis.procrush.local:6379` |
| RabbitMQ AMQP | `rabbitmq.procrush.local:5672` |
| RabbitMQ UI | http://rabbitmq.procrush.local:15672 |
| Kafka | `kafka.procrush.local:9092` |

## Архитектура

```text
Браузер :80
    → Ingress (nginx)
        /api/*  → Service api:8080
        /*      → Service frontend:8080

Внутри кластера (не на хосте):
  postgres, matching-postgres, redis, rabbitmq, kafka
  api, personality, matching
```

Манифесты: [base/](./base/) + overlay [overlays/kind/](./overlays/kind/) (Kustomize).

## Пересборка после изменений кода

```powershell
.\deploy\k8s\scripts\build-images.ps1
kubectl rollout restart deployment -n procrush api personality matching frontend
```

## RabbitMQ UI (опционально)

Откройте http://rabbitmq.procrush.local:15672 — логин `procrush` / `procrush`.

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

```powershell
.\deploy\k8s\scripts\kind-down.ps1
```

## Hot-reload без пересборки образов (опционально)

Инфраструктура доступна с хоста через выделенные loopback-IP сразу после `kind-up`; запускайте приложения через Gradle/npm с переменными из [env.example](../../env.example). Это позволяет нескольким локальным проектам одновременно использовать стандартные порты, например `5432` для PostgreSQL, без конфликтов.

## Устранение неполадок

| Симптом | Что проверить |
|---------|----------------|
| `ImagePullBackOff` | Пересоберите образы: `build-images.ps1`; в overlay задано `imagePullPolicy: Never` |
| API не становится Ready | `kubectl logs -n procrush deploy/api`; часто matching или Kafka ещё стартуют |
| Порт 80 занят | Остановите другой сервис на :80 или измените `hostPort` в [kind-config.yaml](./kind-config.yaml) |
| `procrush.local` не открывается | Запись в hosts; `kubectl get ingress -n procrush` |
| API/personality `Init:0/1` долго | RabbitMQ readiness — в манифесте `tcpSocket` + `publishNotReadyAddresses`; перезапустите: `kubectl apply -k deploy/k8s/overlays/kind` |
| personality `Unhealthy` / 406 | HTTP probe на `/health` без JSON — используется `tcpSocket:8091` |
| `error: no matching resources found` при ingress | Перезапустите `kind-up` — скрипт ждёт admission jobs и rollout deployment |
