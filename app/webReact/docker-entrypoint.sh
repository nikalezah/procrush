#!/bin/sh
set -eu

export PORT="${PORT:-8080}"
if [ -z "${BACKEND_UPSTREAM:-}" ]; then
  echo "BACKEND_UPSTREAM is required (e.g. backend.railway.internal:8080)" >&2
  exit 1
fi

envsubst '${PORT} ${BACKEND_UPSTREAM}' \
  < /etc/nginx/templates/default.conf.template \
  > /etc/nginx/conf.d/default.conf

exec nginx -g 'daemon off;'
