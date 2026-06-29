# ProCrush i18n

Единый источник кодов ошибок API и пользовательских переводов. Живёт в корне репозитория — по той же идее, что и OpenAPI: не внутри `frontend/` или `backend/`.

## Структура каталога

```
i18n/
  error-codes.yaml       # коды ошибок + HTTP status + англ. техническое message
  locales/ru|en/         # errors.json (по кодам) и ui.json (весь интерфейс)
  generated/             # ErrorCode.kt (бэкенд) и errorCodes.ts (фронт)
  scripts/generate.mjs   # codegen + валидация
```

## Как это работает

| Слой | Что отдаёт / показывает |
|------|-------------------------|
| **Backend** | `{ "code": "SURVEY_ALREADY_COMPLETED", "message": "Survey already completed", "details": {} }` — `message` только для логов и отладки |
| **Frontend** | `t('seeker.dashboard.title')` для UI; `resolveApiError(err)` переводит `code` по выбранной локали |

Локаль: авто по браузеру → fallback `ru`; переключатель **ru / en** в «Аккаунт» (`localStorage`: `procrush.locale`).

**Не переводится в v1:** тексты опросов из БД, LLM-профиль, названия профессий и навыков.

## Команды

```bash
cd i18n
npm install
npm run generate   # validate + write generated/kotlin and generated/typescript
npm run validate   # check locales match error-codes.yaml
```

Фронтенд перед `dev`/`build` сам вызывает `validate:i18n` (`npm run prebuild` в `frontend/`).

## Workflow: новый код ошибки или строка UI

1. Добавить код в `error-codes.yaml` и переводы в `locales/ru/errors.json` и `locales/en/errors.json`.
2. Для UI — ключи в `locales/ru/ui.json` и `locales/en/ui.json`.
3. Регенерация: `npm run generate`
4. Backend: `./gradlew :backend:api:compileKotlin` — модуль `contracts` подключает `i18n/generated/kotlin`.
5. Закоммитить `generated/` вместе с yaml/json.

## API contract

Backend returns:

```json
{
  "code": "SURVEY_ALREADY_COMPLETED",
  "message": "Survey already completed",
  "details": { "questionId": "3" }
}
```

- `code` — machine-readable, from `error-codes.yaml`
- `message` — English technical text for logs (not shown to users)
- `details` — optional interpolation values for translated messages

Frontend maps `code` + `details` to the locale in `locales/*/errors.json`.

## Связанная документация

- [frontend/README.md](../frontend/README.md) — веб-клиент
- [backend/README.md](../backend/README.md) — бэкенд
