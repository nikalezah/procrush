# ProCrush OpenAPI

Публичный REST API фронт ↔ бэк описан здесь. Это единственный источник истины для путей, тел запросов и ответов.

## Структура каталога

```
openapi/
  specs/              # YAML-файлы (модели + paths), сканируются Spektor
  bundle.yaml         # entry point для Redocly
  dist/openapi.yaml   # bundled spec — коммитится, используется фронтом
```

## Генерация кода

| Слой | Инструмент | Куда попадает |
|------|------------|---------------|
| **Бэкенд** | Spektor (Gradle) | `backend/api/build/spektor-generated/` — routes, `*ServerApi`, DTO |
| **Фронтенд** | `openapi-typescript` | `frontend/src/api/generated/schema.d.ts` |

## Что не входит в OpenAPI

Ручные Ktor routes (не генерируются из spec):

- `GET /`, `GET /health`
- SSE-эндпоинты: `/api/seeker/match-interests/events`, `/api/employer/match-interests/events`, `/api/seeker/personality-preview/events`

Internal API matching (`/internal/*`) описан отдельно и не используется фронтом.

## Workflow: новый или изменённый эндпоинт

1. Правка YAML в `specs/` (модели и paths).
2. Бэкенд: `./gradlew :backend:api:compileKotlin` — Spektor перегенерирует код в `build/`.
3. Реализовать или обновить handler в `backend/api/.../api/handler/` (маппинг generated DTO ↔ домен через `api/mapper/ApiMappers.kt`).
4. Фронтенд:
   ```bash
   cd frontend
   npm run bundle:openapi && npm run generate:api
   ```
5. Закоммитить `dist/openapi.yaml` и `frontend/src/api/generated/schema.d.ts`.

## Связанная документация

- [backend/README.md](../backend/README.md) — структура бэкенда и handlers
- [frontend/README.md](../frontend/README.md) — веб-клиент и dev-сервер
