#!/usr/bin/env bash
set -euo pipefail

require_env() {
  local key="$1"
  if [[ -z "${!key:-}" ]]; then
    echo "missing env: ${key}" >&2
    exit 1
  fi
}

require_file() {
  local path="$1"
  if [[ ! -f "${path}" ]]; then
    echo "file not found: ${path}" >&2
    exit 1
  fi
}

require_cmd() {
  local cmd="$1"
  if ! command -v "${cmd}" >/dev/null 2>&1; then
    echo "command not found: ${cmd}" >&2
    exit 1
  fi
}

require_cmd jq
require_cmd mysql

require_env DB_USER
require_env DB_PASSWORD
require_env DB_NAME
require_env PAY_APP_ID
require_env SP_APP_ID
require_env SP_MCH_ID
require_env SUB_MCH_ID
require_env API_V3_KEY
require_env CERT_SERIAL_NO
require_env PUBLIC_KEY_ID
require_env PRIVATE_KEY_FILE

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
CHANNEL_CODE="${CHANNEL_CODE:-wx_lite}"
SUB_APP_ID="${SUB_APP_ID:-}"
PUBLIC_KEY_FILE="${PUBLIC_KEY_FILE:-}"
PARTNER_MODE="${PARTNER_MODE:-true}"
DRY_RUN="${DRY_RUN:-0}"

require_file "${PRIVATE_KEY_FILE}"
if [[ -n "${PUBLIC_KEY_FILE}" ]]; then
  require_file "${PUBLIC_KEY_FILE}"
fi

PRIVATE_KEY_CONTENT="$(cat "${PRIVATE_KEY_FILE}")"
PUBLIC_KEY_CONTENT=""
if [[ -n "${PUBLIC_KEY_FILE}" ]]; then
  PUBLIC_KEY_CONTENT="$(cat "${PUBLIC_KEY_FILE}")"
fi

CONFIG_JSON="$(
  jq -cn \
    --arg appId "${SP_APP_ID}" \
    --arg mchId "${SP_MCH_ID}" \
    --arg apiVersion "v3" \
    --arg subMchId "${SUB_MCH_ID}" \
    --arg privateKeyContent "${PRIVATE_KEY_CONTENT}" \
    --arg apiV3Key "${API_V3_KEY}" \
    --arg certSerialNo "${CERT_SERIAL_NO}" \
    --arg publicKeyId "${PUBLIC_KEY_ID}" \
    --arg subAppId "${SUB_APP_ID}" \
    --arg publicKeyContent "${PUBLIC_KEY_CONTENT}" \
    --arg partnerMode "${PARTNER_MODE}" \
    '
    {
      appId: $appId,
      mchId: $mchId,
      apiVersion: $apiVersion,
      partnerMode: ($partnerMode == "true"),
      subMchId: $subMchId,
      privateKeyContent: $privateKeyContent,
      apiV3Key: $apiV3Key,
      certSerialNo: $certSerialNo,
      publicKeyId: $publicKeyId
    }
    + (if $subAppId != "" then {subAppId: $subAppId} else {} end)
    + (if $publicKeyContent != "" then {publicKeyContent: $publicKeyContent} else {} end)
    '
)"

CONFIG_B64="$(printf '%s' "${CONFIG_JSON}" | base64 -w 0)"

echo "target channel: app_id=${PAY_APP_ID}, code=${CHANNEL_CODE}, db=${DB_NAME}@${DB_HOST}:${DB_PORT}"
if [[ "${DRY_RUN}" == "1" ]]; then
  echo "dry run mode, generated config:"
  echo "${CONFIG_JSON}" | jq .
  exit 0
fi

MYSQL_PWD="${DB_PASSWORD}" mysql \
  -h "${DB_HOST}" \
  -P "${DB_PORT}" \
  -u "${DB_USER}" \
  --default-character-set=utf8mb4 \
  "${DB_NAME}" <<SQL
SET NAMES utf8mb4;

UPDATE pay_channel
SET config = CONVERT(FROM_BASE64('${CONFIG_B64}') USING utf8mb4),
    update_time = NOW()
WHERE app_id = ${PAY_APP_ID}
  AND code = '${CHANNEL_CODE}'
  AND deleted = b'0';

SELECT id, app_id, code, status, JSON_EXTRACT(config, '$.apiVersion') AS api_version,
       JSON_EXTRACT(config, '$.partnerMode') AS partner_mode,
       JSON_EXTRACT(config, '$.mchId') AS mch_id,
       JSON_EXTRACT(config, '$.subMchId') AS sub_mch_id,
       update_time
FROM pay_channel
WHERE app_id = ${PAY_APP_ID}
  AND code = '${CHANNEL_CODE}'
  AND deleted = b'0';
SQL

echo "done."
