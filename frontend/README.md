# ProCrush React web client

Веб-интерфейс ProCrush: React + Vite + Tailwind. Отдельное приложение с независимым деплоем (nginx + прокси `/api`).

## Доступ

| Режим | URL |
|-------|-----|
| **Полный стек (kind)** | http://127.10.0.10 — см. [deploy/k8s/README.md](../deploy/k8s/README.md) |
| **Hot-reload dev server** | http://localhost:8081 (проксирует `/api` на Ktor :8080; требует port-forward инфраструктуры из kind) |

## Требования

- **Node.js 20+** (LTS 22 recommended)
- Запущенный API: `./gradlew :backend:api:run` из корня репозитория
- Переменные окружения API: `AUTH_DEV_MODE=true`, `WEB_ORIGIN` с `http://localhost:8081` (полный список — [`deploy/k8s/base/configmap.yaml`](../deploy/k8s/base/configmap.yaml))

## Команды

```bash
npm install
npm run dev
npm run build
```

Перед `dev`/`build` автоматически запускается `validate:i18n` (`prebuild`). Типы API генерируются из OpenAPI — см. [openapi/README.md](../openapi/README.md).

## Интернационализация

Переводы UI и коды ошибок — в [i18n/](../i18n/README.md). Локаль: авто по браузеру → fallback `ru`; переключатель **ru / en** в «Аккаунт» (`localStorage`: `procrush.locale`).

## Troubleshooting

### `Unexpected token '||='` when running `npm run dev`

Your terminal is using an old Node.js (often v14). Vite 6 and Tailwind 4 need Node 20+.

1. Check version: `node --version`
2. Install LTS (Windows): `winget install OpenJS.NodeJS.LTS`
3. **Close and reopen** the terminal (PATH must refresh)
4. Confirm `node --version` shows v20+ or v22+, then run `npm run dev` again

If multiple Node versions are installed, ensure the LTS install comes first in `PATH` (`where node` on Windows).

## Связанная документация

- [openapi/README.md](../openapi/README.md) — контракт REST API и codegen
- [i18n/README.md](../i18n/README.md) — переводы
- [deploy/README.md](../deploy/README.md) — деплой (Railway, Docker)
- [deploy/k8s/README.md](../deploy/k8s/README.md) — локальный стек в kind
