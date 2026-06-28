# ProCrush on Kubernetes (kind)

Локальный полный стек ProCrush в **kind** (Kubernetes IN Docker): инфраструктура, три backend-сервиса и React-frontend. Снаружи — **http://127.10.0.10** (без записей в hosts).

## Требования

- [Docker](https://docs.docker.com/get-docker/) — выделите **≥ 8 GB RAM** в настройках Docker Desktop
- [kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)

## Быстрый старт

1. Из корня репозитория:

   **Windows (PowerShell):**

   ```powershell
   .\deploy\k8s\scripts\kind-up.ps1
   ```

   Если кластер `procrush` был создан до смены `listenAddress`, пересоздайте его: `.\deploy\k8s\scripts\kind-down.ps1`, затем снова `.\deploy\k8s\scripts\kind-up.ps1`. Настройка `extraPortMappings` применяется только при создании контейнера ноды kind.

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

   Перед `kind-up` укажите `LLM_API_KEY` в [`deploy/k8s/base/secret.yaml`](./base/secret.yaml) (файл в git с пустым значением — подставьте ключ локально, не коммитьте).

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
| RabbitMQ UI | http://127.10.0.14:15672 |
| Kafka | `127.10.0.15:9092` |

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

```powershell
.\deploy\k8s\scripts\build-images.ps1
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

```powershell
.\deploy\k8s\scripts\kind-down.ps1
```

## Hot-reload без пересборки образов (опционально)

Инфраструктура доступна с хоста по loopback-IP сразу после `kind-up`; запускайте приложения через Gradle/npm с переменными из [env.example](../../env.example). Разные проекты могут одновременно использовать стандартные порты на своих `127.x.x.x` адресах.

## Устранение неполадок

| Симптом | Что проверить |
|---------|----------------|
| `ImagePullBackOff` | Пересоберите образы: `build-images.ps1`; в overlay задано `imagePullPolicy: Never` |
| API не становится Ready | `kubectl logs -n procrush deploy/api`; часто matching или Kafka ещё стартуют |
| Порт 80 занят на `127.10.0.10` | Другой сервис на том же IP:порт; смените `listenAddress` / `hostPort` в [kind-config.yaml](./kind-config.yaml) |
| http://127.10.0.10 не открывается | `kubectl get ingress -n procrush`; пересоздайте кластер после смены `kind-config.yaml` |
| API/personality `Init:0/1` долго | RabbitMQ readiness — в манифесте `tcpSocket` + `publishNotReadyAddresses`; перезапустите: `kubectl apply -k deploy/k8s/overlays/kind` |
| personality `Unhealthy` / 406 | HTTP probe на `/health` без JSON — используется `tcpSocket:8091` |
| `error: no matching resources found` при ingress | Перезапустите `kind-up` — скрипт ждёт admission jobs и rollout deployment |
