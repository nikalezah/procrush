# ProCrush OpenAPI

The public frontend ↔ backend REST API is defined here. This is the single source of truth for paths, request bodies, and responses.

## Directory structure

```
openapi/
  specs/              # YAML files (models + paths), scanned by Spektor
  bundle.yaml         # entry point for Redocly
  dist/openapi.yaml   # bundled spec — committed, used by the frontend
```

## Code generation

| Layer | Tool | Output |
|-------|------|--------|
| **Backend** | Spektor (Gradle) | `backend/api/build/spektor-generated/` — routes, `*ServerApi`, DTOs |
| **Frontend** | `openapi-typescript` | `frontend/src/api/generated/schema.d.ts` |

## Not in OpenAPI

Manual Ktor routes (not generated from spec):

- `GET /`, `GET /health`
- SSE endpoints: `/api/seeker/match-interests/events`, `/api/employer/match-interests/events`, `/api/seeker/personality-preview/events`

Matching internal API (`/internal/*`) is described separately and not used by the frontend.

## Workflow: new or changed endpoint

1. Edit YAML in `specs/` (models and paths).
2. Backend: `./gradlew :backend:api:compileKotlin` — Spektor regenerates code in `build/`.
3. Implement or update handler in `backend/api/.../api/handler/` (map generated DTO ↔ domain via `api/mapper/ApiMappers.kt`).
4. Frontend:
   ```bash
   cd frontend
   npm run bundle:openapi && npm run generate:api
   ```
5. Commit `dist/openapi.yaml` and `frontend/src/api/generated/schema.d.ts`.

## Related documentation

- [backend/README.md](../backend/README.md) — backend structure and handlers
- [frontend/README.md](../frontend/README.md) — web client and dev server
