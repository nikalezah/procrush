# ProCrush i18n

Source of truth for API error codes and user-facing translations. Lives outside `frontend/` and `backend/`.

## Layout

```
i18n/
  error-codes.yaml       # codes + English technical messages + HTTP status
  locales/
    ru/errors.json       # user-facing error messages (Russian)
    ru/ui.json           # UI strings (Russian)
    en/errors.json
    en/ui.json
  generated/             # codegen output (committed)
  scripts/
    generate.mjs
    validate.mjs
```

## Workflow

```bash
cd i18n
npm install
npm run generate   # validate + write generated/kotlin and generated/typescript
npm run validate   # check locales match error-codes.yaml
```

After editing `error-codes.yaml`:

1. Add matching keys to `locales/ru/errors.json` and `locales/en/errors.json`
2. Run `npm run generate`
3. Backend uses `jobs.procrush.i18n.ErrorCode`; frontend imports `generated/typescript/errorCodes.ts`

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
