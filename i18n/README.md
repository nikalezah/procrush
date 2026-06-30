# ProCrush i18n

Single source for API error codes and user-facing translations. Lives at the repository root — same idea as OpenAPI: not inside `frontend/` or `backend/`.

## Directory structure

```
i18n/
  error-codes.yaml       # error codes + HTTP status + English technical message
  locales/ru|en/         # errors.json (by code) and ui.json (full UI)
  generated/             # ErrorCode.kt (backend) and errorCodes.ts (frontend)
  scripts/generate.mjs   # codegen + validation
```

## How it works

| Layer | Returns / displays |
|-------|-------------------|
| **Backend** | `{ "code": "SURVEY_ALREADY_COMPLETED", "message": "Survey already completed", "details": {} }` — `message` is for logs and debugging only |
| **Frontend** | `t('seeker.dashboard.title')` for UI; `resolveApiError(err)` translates `code` for the selected locale |

Locale: auto from browser → fallback `ru`; **ru / en** switcher in Account (`localStorage`: `procrush.locale`).

**Not translated in v1:** survey texts from DB, LLM profile, occupation and skill names.

## Commands

```bash
cd i18n
npm install
npm run generate   # validate + write generated/kotlin and generated/typescript
npm run validate   # check locales match error-codes.yaml
```

The frontend runs `validate:i18n` before `dev`/`build` (`npm run prebuild` in `frontend/`).

## Workflow: new error code or UI string

1. Add code to `error-codes.yaml` and translations in `locales/ru/errors.json` and `locales/en/errors.json`.
2. For UI — keys in `locales/ru/ui.json` and `locales/en/ui.json`.
3. Regenerate: `npm run generate`
4. Backend: `./gradlew :backend:api:compileKotlin` — the `contracts` module includes `i18n/generated/kotlin`.
5. Commit `generated/` together with yaml/json.

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

## Related documentation

- [frontend/README.md](../frontend/README.md) — web client
- [backend/README.md](../backend/README.md) — backend
