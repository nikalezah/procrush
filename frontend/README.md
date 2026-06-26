# ProCrush React web client

**Полный стек:** http://127.10.0.10 (kind + Ingress) — см. [deploy/k8s/README.md](../deploy/k8s/README.md).

**Hot-reload dev server:** http://localhost:8081 (proxies `/api` to Ktor on :8080; требует port-forward инфраструктуры из kind).

## Requirements

- **Node.js 20+** (LTS 22 recommended)
- Running API: `./gradlew :backend:api:run` from repo root
- `.env` with `AUTH_DEV_MODE=true` and `WEB_ORIGIN` including `http://localhost:8081`

## Commands

```bash
npm install
npm run dev
npm run build
```

## Troubleshooting

### `Unexpected token '||='` when running `npm run dev`

Your terminal is using an old Node.js (often v14). Vite 6 and Tailwind 4 need Node 20+.

1. Check version: `node --version`
2. Install LTS (Windows): `winget install OpenJS.NodeJS.LTS`
3. **Close and reopen** the terminal (PATH must refresh)
4. Confirm `node --version` shows v20+ or v22+, then run `npm run dev` again

If multiple Node versions are installed, ensure the LTS install comes first in `PATH` (`where node` on Windows).
