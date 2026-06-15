# ProCrush

Платформа подбора кандидатов: соискатель проходит личностные опросы, получает интерпретированный профиль; работодатель создаёт профили вакансий и просматривает кандидатов.

## Логика работы сервиса

### Роли

После входа пользователь один раз выбирает роль (`SEEKER` или `EMPLOYER`) через `POST /api/auth/complete-registration`. Сменить роль нельзя.

| Роль | Что делает в MVP |
|------|------------------|
| **Соискатель (SEEKER)** | Проходит группы личностных тестов, получает сформированный профиль |
| **Работодатель (EMPLOYER)** | Просматривает дашборд и списки (полный матчинг — в развитии) |

### Группы тестов

Опросы делятся на две группы:

1. **Тест 1 (`core`)** — восемь последовательных методик (открытые вопросы, выбор качеств, DISC, дилеммы, Белбин и др.). Шаги можно пересматривать, пока вся группа не завершена.
2. **Тест 2 (`64qn`)** — личностный опросник на 64 вопроса (шкала 0–4). Открывается только после полного завершения группы `core`.

Правила блокировки и навигации между шагами — в `server/.../survey/SurveyFlowRules.kt`.

### Цепочка «тесты → расчёты → интерпретация»

```mermaid
flowchart LR
    A[Ответы пользователя] --> B[Валидация]
    B --> C[Расчёт по ключам]
    C --> D[calculated_results в БД]
    D --> E[Контекст для LLM]
    E --> F[Интерпретация]
    F --> G[Личностный профиль]
```

**1. Тесты.** Соискатель отвечает на вопросы в веб-клиенте. Ответы сохраняются по мере заполнения; при завершении опроса сервер проверяет полноту и корректность (`SurveyAnswerValidator`).

**2. Расчёты.** Для каждого опроса в БД хранятся ключи подсчёта (`survey_keys`). `SurveyScoringService` применяет нужную логику (`open_text`, `matrix`, `direct_sum`, `formula`) и записывает структурированный JSON в `survey_results.calculated_results`. Примеры: суммы по осям DISC, роли Белбина, нормализованные баллы шкалы 0–4.

**3. Интерпретация.** Когда завершены обе группы тестов, запускается асинхронная генерация профиля (`PersonalityProfileService`):

- собирается контекст: ответы, результаты расчётов и глоссарий терминов (`SurveyService.buildLlmContext`);
- LLM получает системный промпт с требуемой JSON-схемой (`PersonalityPromptBuilder`);
- ответ валидируется и сохраняется (`PersonalityProfileValidator`, `SeekerPersonalProfileRepository`).

Статусы профиля: `NOT_READY` → `PROCESSING` → `READY` или `FAILED` (с возможностью повтора).

## Выбор архитектуры

### Kotlin Multiplatform (KMP)

Проект собран как **Kotlin Multiplatform** с модулями `core`, `app/shared`, нативными приложениями (Android, iOS, Desktop) и Ktor-сервером. Сейчас **единственный пользовательский интерфейс MVP — отдельный React-клиент**, но KMP выбран осознанно как задел на будущее: по мере созревания продукта Compose Multiplatform и мобильные приложения смогут делить с сервером бизнес-правила, валидацию и форматирование без переписывания с нуля. KMP здесь — инвестиция в переиспользование кода между модулями и приложениями.

### React для веб-MVP

Веб-интерфейс MVP реализован как **отдельное React + Vite + Tailwind приложение** (`app/webReact`), а не через Compose for Web:

- быстрый старт и знакомый стек для итераций UI;
- независимый деплой фронтенда (nginx + прокси `/api`);
- меньше связанности с Gradle и KMP при активной доработке экранов опросов и профиля.

## Структура репозитория

| Путь | Назначение                                     |
|------|------------------------------------------------|
| [`core/`](./core/src) | Общий код для всех таргетов (модели, утилиты)  |
| [`app/shared/`](./app/shared/src) | UI и логика Compose Multiplatform              |
| [`app/webReact/`](./app/webReact) | **Единственный веб-клиент MVP** (React)        |
| [`app/webApp/`](./app/webApp) | Compose Web (JS), auth MVP                     |
| [`app/androidApp/`](./app/androidApp), [`app/iosApp/`](./app/iosApp), [`app/desktopApp/`](./app/desktopApp) | Нативные оболочки KMP                          |
| [`server/`](./server/src/main/kotlin) | Ktor API, домен, миграции Flyway, расчёты, LLM |
| [`deploy/`](./deploy) | Dockerfile для Railway                         |

## Локальная разработка

### Требования

- JDK 17+, Docker (PostgreSQL)
- Для React: **Node.js 20+** (см. [app/webReact/README.md](./app/webReact/README.md) при ошибке `Unexpected token '||='`)

### Аутентификация

Используются **httpOnly session cookies**. Локально — dev-вход (`AUTH_DEV_MODE=true`).

1. Скопируйте [`env.example`](./env.example) в `.env` (`AUTH_DEV_MODE=true`; в `WEB_ORIGIN` укажите оба origin, если работаете и с React, и с Compose).
2. PostgreSQL: `docker compose up -d`
3. API: `./gradlew :server:run` (миграции Flyway применяются автоматически)
4. Веб-клиент:
   - **React:** `cd app/webReact && npm install && npm run dev` → http://localhost:8081
   - **Compose:** `./gradlew :app:webApp:jsBrowserDevelopmentRun` → http://localhost:8082

Схема БД и справочные данные — в Flyway-миграциях (`server/src/main/kotlin/db/migration/`) и seed (`server/src/main/resources/db/seed/init_inserts.sql`). При конфликте со старыми миграциями:

```bash
docker compose down -v && docker compose up -d
```

| Endpoint | Описание |
|----------|----------|
| `POST /api/auth/dev/login` | Dev-вход (требует `AUTH_DEV_MODE=true`) |
| `GET /api/auth/me` | Текущий пользователь |
| `POST /api/auth/logout` | Выход |
| `POST /api/auth/complete-registration` | Выбор роли (неизменяемо) |

### Запуск приложений

- **React (веб-MVP):** `cd app/webReact && npm run dev` → http://localhost:8081
- **Server**: `./gradlew :server:run`
- Android: `./gradlew :app:androidApp:assembleDebug`
- Desktop: `./gradlew :app:desktopApp:run` (hot reload: `:app:desktopApp:hotRun --auto`)
- iOS: открыть [`app/iosApp`](./app/iosApp) в Xcode

### Тесты

- Android: `./gradlew :app:shared:testAndroidHostTest`
- Desktop: `./gradlew :app:shared:jvmTest`
- Server: `./gradlew :server:test`
- Web (Wasm): `./gradlew :app:shared:wasmJsTest`
- Web (JS): `./gradlew :app:shared:jsTest`
- iOS: `./gradlew :app:shared:iosSimulatorArm64Test`

---

## Деплой на Railway (GitHub)

В одном проекте Railway три сервиса: **Postgres**, **Backend** (Ktor API), **Frontend** (React + nginx). Пользователи открывают только URL фронтенда; nginx проксирует `/api/*` на backend по приватной сети Railway. Для автоматических деплоев нужно подключить GitHub репозиторий к сервисам Backend и Frontend в настройках этих сервисов Railway. Альтернативный вариант, деплоить с локального репозитория через Railway CLI.

### Архитектура

| Сервис | Root Directory | Config file (от корня репо) |
|--------|----------------|----------------------------|
| Backend | **пусто** (корень репо) | `/railway.toml` |
| Frontend | **пусто** | `/deploy/railway.frontend.toml` |
| Postgres | — | — |

Образы собираются **из корня репозитория** (backend нуждается в `:core` + `:server`; frontend — `deploy/Dockerfile.webReact`).

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

В проекте уже должен быть **Postgres**. Добавьте два application-сервиса и подключите к каждому **тот же** GitHub-репозиторий и ветку.

#### Backend

1. **+ New** → **Empty Service** → имя `Backend`.
2. **Settings → Source**: GitHub-репозиторий и ветка.
3. **Settings → Root Directory**: **пусто**.
4. **Settings → Config file**: `/railway.toml`.
5. **Variables**:

   | Переменная | Значение |
   |------------|----------|
   | `DATABASE_URL` | `${{Postgres.DATABASE_URL}}` |
   | `WEB_ORIGIN` | `https://${{Frontend.RAILWAY_PUBLIC_DOMAIN}}` (после появления домена у frontend) |
   | `FRONTEND_URL` | то же, что `WEB_ORIGIN` |
   | `AUTH_DEV_MODE` | `false` (prod) или `true` (staging) |

6. Деплой (автоматически при push или **Deploy** в dashboard).
7. Публичный домен опционален (health: `GET /health`).

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

   API слушает порт `8080` (`deploy/Dockerfile.server`, Ktor по умолчанию без `PORT`).

6. **Networking → Public Networking**: **Generate Domain** (обязательно для пользователей).
7. Деплой.

#### После появления URL у frontend

Если `WEB_ORIGIN` / `FRONTEND_URL` не были заданы через `${{Frontend.RAILWAY_PUBLIC_DOMAIN}}` до создания домена — установите их и передеплойте **Backend**.

### Порядок деплоя

1. Postgres (уже создан)
2. Backend (`/health`, Flyway в логах)
3. Frontend (публичный домен + `BACKEND_UPSTREAM`)
4. Повторный деплой Backend, если нужно обновить `WEB_ORIGIN` / `FRONTEND_URL`

### Проверка

| Проверка | Как |
|----------|-----|
| Health API | `GET https://<backend-domain>/health` → `{"status":"ok"}` |
| Frontend | `https://<frontend-domain>/` |
| API через прокси | Вход при `AUTH_DEV_MODE=true` на backend |
| Сборка | В логах деплоя — **Dockerfile**, не **Railpack** |

### Railway vs локально

- В контейнерах нет `.env` — переменные задаются в Railway dashboard.
- Railway выставляет `PORT` для обоих сервисов.
- `DATABASE_URL` от Postgres — `postgresql://...`; сервер добавляет JDBC `sslmode=require`.

Переменные LLM (`LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL` и др.) задайте на Backend в dashboard — см. комментарии в [`env.example`](./env.example).
