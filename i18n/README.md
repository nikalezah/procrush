# ProCrush i18n

Single source for API error codes and user-facing translations. Lives at the repository root — same idea as OpenAPI: not inside `frontend/` or `backend/`.

## Directory structure

```
i18n/
  error-codes.yaml       # error codes + HTTP status + English technical message
  locales/ru|en/         # errors.json (by code) and ui.json (full UI)
```

Generated artifacts (not committed):

- Kotlin: `backend/contracts/build/generated/i18n/kotlin/.../ErrorCode.kt`
- TypeScript: `frontend/src/generated/i18n/errorCodes.ts`

## How it works

| Layer | Returns / displays |
|-------|-------------------|
| **Backend** | `{ "code": "SURVEY_ALREADY_COMPLETED", "message": "Survey already completed", "details": {} }` — `message` is for logs and debugging only |
| **Frontend** | `t('seeker.dashboard.title')` for UI; `resolveApiError(err)` translates `code` for the selected locale |

Locale: auto from browser → fallback `ru`; **ru / en** switcher in Account (`localStorage`: `procrush.locale`).

**Not translated in v1:** survey texts from DB, LLM profile, occupation and skill names.

## Commands

```bash
# From repo root — validates locales/*/errors.json against error-codes.yaml, then generates Kotlin + TypeScript
./gradlew generateI18n
```

Frontend `predev` / `prebuild` call the same Gradle task. Backend `compileKotlin*` depends on it via `:backend:contracts`.

## Workflow: new error code or UI string

1. Add code to `error-codes.yaml` and translations in `locales/ru/errors.json` and `locales/en/errors.json` (manual — missing keys fail the build).
2. For UI — keys in `locales/ru/ui.json` and `locales/en/ui.json`.
3. Regenerate: `./gradlew generateI18n` (or `npm run dev` / compile — runs generate automatically).
4. Commit yaml/json only — do **not** commit generated sources.

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
