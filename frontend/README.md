# ProCrush React web client

ProCrush web UI: React + Vite + Tailwind. Standalone application with independent deployment (nginx + `/api` proxy).

## Access

| Mode | URL |
|------|-----|
| **Full stack (kind)** | http://127.10.0.10 — see [deploy/k8s/README.md](../deploy/k8s/README.md) |
| **Hot-reload dev server** | http://localhost:8081 (proxies `/api` to Ktor :8080; requires port-forward of infrastructure from kind) |

## Requirements

- **Node.js 20+** (LTS 22 recommended)
- Running API: `./gradlew :backend:api:run` from the repository root
- API environment variables: `AUTH_DEV_MODE=true`, `WEB_ORIGIN` including `http://localhost:8081` (full list — [`deploy/k8s/base/configmap.yaml`](../deploy/k8s/base/configmap.yaml))

## Commands

```bash
npm install
npm run dev
npm run build
```

Before `dev`/`build`, `validate:i18n` runs automatically (`prebuild`). API types are generated from OpenAPI — see [openapi/README.md](../openapi/README.md).

## Internationalization

UI translations and error codes — in [i18n/](../i18n/README.md). Locale: auto from browser → fallback `ru`; **ru / en** switcher in Account (`localStorage`: `procrush.locale`).

## Troubleshooting

### `Unexpected token '||='` when running `npm run dev`

Your terminal is using an old Node.js (often v14). Vite 6 and Tailwind 4 need Node 20+.

1. Check version: `node --version`
2. Install LTS (Windows): `winget install OpenJS.NodeJS.LTS`
3. **Close and reopen** the terminal (PATH must refresh)
4. Confirm `node --version` shows v20+ or v22+, then run `npm run dev` again

If multiple Node versions are installed, ensure the LTS install comes first in `PATH` (`where node` on Windows).

## Related documentation

- [openapi/README.md](../openapi/README.md) — REST API contract and codegen
- [i18n/README.md](../i18n/README.md) — translations
- [deploy/README.md](../deploy/README.md) — deployment (Railway, Docker)
- [deploy/k8s/README.md](../deploy/k8s/README.md) — local kind stack
