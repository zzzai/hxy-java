#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-13306}"
DB_NAME="${DB_NAME:-hxy_dev}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-123456}"
TENANT_ID="${TENANT_ID:-1}"
SOCIAL_TYPE="${SOCIAL_TYPE:-34}"
TARGET_APP_ID="${TARGET_APP_ID:-}"
TARGET_APP_SECRET="${TARGET_APP_SECRET:-}"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'USAGE'
Usage:
  TARGET_APP_ID=wx*** TARGET_APP_SECRET=*** script/dev/update_wx_mini_social_client.sh

Env:
  DB_HOST=127.0.0.1
  DB_PORT=13306
  DB_NAME=hxy_dev
  DB_USER=root
  DB_PASSWORD=123456
  TENANT_ID=1
  SOCIAL_TYPE=34
  TARGET_APP_ID=<required>
  TARGET_APP_SECRET=<required>
USAGE
  exit 0
fi

if ! command -v mysql >/dev/null 2>&1; then
  echo "[FAIL] missing command: mysql" >&2
  exit 1
fi

if [[ -z "${TARGET_APP_ID}" ]]; then
  echo "[FAIL] TARGET_APP_ID is required" >&2
  exit 1
fi

if [[ -z "${TARGET_APP_SECRET}" ]]; then
  echo "[FAIL] TARGET_APP_SECRET is required" >&2
  exit 1
fi

mysql \
  -h "${DB_HOST}" \
  -P "${DB_PORT}" \
  -u "${DB_USER}" \
  --password="${DB_PASSWORD}" \
  --default-character-set=utf8mb4 \
  "${DB_NAME}" <<SQL
SET NAMES utf8mb4;

UPDATE system_social_client
SET client_id = '${TARGET_APP_ID}',
    client_secret = '${TARGET_APP_SECRET}',
    status = 0,
    deleted = b'0',
    update_time = NOW()
WHERE social_type = ${SOCIAL_TYPE}
  AND tenant_id = ${TENANT_ID};

INSERT INTO system_social_client (
  name, social_type, user_type, client_id, client_secret, status,
  creator, updater, tenant_id, deleted
)
SELECT
  '微信小程序', ${SOCIAL_TYPE}, 1, '${TARGET_APP_ID}', '${TARGET_APP_SECRET}', 0,
  'script', 'script', ${TENANT_ID}, b'0'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM system_social_client
  WHERE social_type = ${SOCIAL_TYPE}
    AND tenant_id = ${TENANT_ID}
);

SELECT id, tenant_id, social_type, client_id, LEFT(client_secret, 8) AS client_secret_prefix, status,
       IF(deleted = b'1', 1, 0) AS deleted, update_time
FROM system_social_client
WHERE social_type = ${SOCIAL_TYPE}
  AND tenant_id = ${TENANT_ID}
ORDER BY (deleted = b'0') DESC, id DESC
LIMIT 1;
SQL

echo "[PASS] updated system_social_client for social_type=${SOCIAL_TYPE}, tenant_id=${TENANT_ID}"
