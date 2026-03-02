#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
UNI_MANIFEST="${UNI_MANIFEST:-${ROOT_DIR}/../yudao-mall-uniapp/manifest.json}"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-13306}"
DB_NAME="${DB_NAME:-hxy_dev}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-123456}"
TENANT_ID="${TENANT_ID:-1}"
PAY_APP_ID="${PAY_APP_ID:-1}"
CHANNEL_CODE="${CHANNEL_CODE:-wx_lite}"
SOCIAL_TYPE="${SOCIAL_TYPE:-34}"
EXPECTED_APPID="${EXPECTED_APPID:-}"

DEFAULT_SOCIAL_SECRET="6f270509224a7ae1296bbf1c8cb97aed"
DEFAULT_SOCIAL_APPID="wx63c280fe3248a3e7"

for cmd in jq mysql; do
  if ! command -v "${cmd}" >/dev/null 2>&1; then
    echo "[FAIL] missing command: ${cmd}" >&2
    exit 1
  fi
done

if [[ ! -f "${UNI_MANIFEST}" ]]; then
  echo "[FAIL] manifest not found: ${UNI_MANIFEST}" >&2
  exit 1
fi

MYSQL_BASE=(
  mysql
  "-h${DB_HOST}"
  "-P${DB_PORT}"
  "-u${DB_USER}"
  "-p${DB_PASSWORD}"
  "-N"
  "-s"
  "${DB_NAME}"
)

query_one_line() {
  local sql="$1"
  "${MYSQL_BASE[@]}" -e "${sql}" 2>/dev/null | head -n 1 || true
}

UNI_APPID="$(jq -r '.["mp-weixin"].appid // empty' "${UNI_MANIFEST}")"
SOCIAL_ROW="$(query_one_line "SELECT client_id, client_secret, status, IF(deleted = b'1', 1, 0) FROM system_social_client WHERE social_type=${SOCIAL_TYPE} AND tenant_id=${TENANT_ID} ORDER BY (deleted = b'0') DESC, id DESC LIMIT 1;")"
PAY_ROW="$(query_one_line "SELECT JSON_UNQUOTE(JSON_EXTRACT(config,'$.appId')), JSON_UNQUOTE(JSON_EXTRACT(config,'$.subAppId')) FROM pay_channel WHERE app_id=${PAY_APP_ID} AND code='${CHANNEL_CODE}' AND deleted=b'0' ORDER BY id DESC LIMIT 1;")"
TENANT_WEBSITES="$(query_one_line "SELECT websites FROM system_tenant WHERE id=${TENANT_ID} LIMIT 1;")"

SOCIAL_APPID=""
SOCIAL_SECRET=""
SOCIAL_STATUS=""
SOCIAL_DELETED_FLAG=""
if [[ -n "${SOCIAL_ROW}" ]]; then
  SOCIAL_APPID="$(echo "${SOCIAL_ROW}" | awk -F'\t' '{print $1}')"
  SOCIAL_SECRET="$(echo "${SOCIAL_ROW}" | awk -F'\t' '{print $2}')"
  SOCIAL_STATUS="$(echo "${SOCIAL_ROW}" | awk -F'\t' '{print $3}')"
  SOCIAL_DELETED_FLAG="$(echo "${SOCIAL_ROW}" | awk -F'\t' '{print $4}')"
fi

PAY_APPID_CFG=""
PAY_SUB_APPID_CFG=""
if [[ -n "${PAY_ROW}" ]]; then
  PAY_APPID_CFG="${PAY_ROW%%$'\t'*}"
  PAY_SUB_APPID_CFG="${PAY_ROW#*$'\t'}"
fi

if [[ -z "${EXPECTED_APPID}" ]]; then
  if [[ -n "${PAY_SUB_APPID_CFG}" && "${PAY_SUB_APPID_CFG}" != "null" ]]; then
    EXPECTED_APPID="${PAY_SUB_APPID_CFG}"
  elif [[ -n "${PAY_APPID_CFG}" && "${PAY_APPID_CFG}" != "null" ]]; then
    EXPECTED_APPID="${PAY_APPID_CFG}"
  else
    EXPECTED_APPID="${UNI_APPID}"
  fi
fi

echo "[INFO] expected_appid=${EXPECTED_APPID}"
echo "[INFO] uni_manifest_mp_weixin_appid=${UNI_APPID}"
echo "[INFO] social_client_appid=${SOCIAL_APPID}"
echo "[INFO] social_client_status=${SOCIAL_STATUS:-unknown}"
echo "[INFO] social_client_deleted=${SOCIAL_DELETED_FLAG:-unknown}"
echo "[INFO] pay_channel_appid=${PAY_APPID_CFG}"
echo "[INFO] pay_channel_sub_appid=${PAY_SUB_APPID_CFG}"
echo "[INFO] tenant_websites=${TENANT_WEBSITES}"

fail=0

if [[ -z "${UNI_APPID}" ]]; then
  echo "[FAIL] uni-app manifest missing mp-weixin.appid" >&2
  fail=1
fi

if [[ -z "${SOCIAL_APPID}" ]]; then
  echo "[FAIL] missing social client row (social_type=${SOCIAL_TYPE}, tenant_id=${TENANT_ID})" >&2
  fail=1
fi

if [[ -n "${SOCIAL_DELETED_FLAG}" && "${SOCIAL_DELETED_FLAG}" != "0" ]]; then
  echo "[FAIL] social client row is logically deleted (deleted=1); run update_wx_mini_social_client.sh to reactivate" >&2
  fail=1
fi

if [[ -n "${SOCIAL_STATUS}" && "${SOCIAL_STATUS}" != "0" ]]; then
  echo "[FAIL] social client status is disabled (${SOCIAL_STATUS}); expected 0(enabled)" >&2
  fail=1
fi

if [[ -z "${PAY_APPID_CFG}" || -z "${PAY_SUB_APPID_CFG}" || "${PAY_APPID_CFG}" == "null" || "${PAY_SUB_APPID_CFG}" == "null" ]]; then
  echo "[FAIL] pay_channel(${CHANNEL_CODE}) missing appId/subAppId" >&2
  fail=1
fi

if [[ "${UNI_APPID}" != "${EXPECTED_APPID}" ]]; then
  echo "[FAIL] uni manifest appid mismatch: ${UNI_APPID} != ${EXPECTED_APPID}" >&2
  fail=1
fi

if [[ "${SOCIAL_APPID}" != "${EXPECTED_APPID}" ]]; then
  echo "[FAIL] social client appid mismatch: ${SOCIAL_APPID} != ${EXPECTED_APPID}" >&2
  fail=1
fi

if [[ "${PAY_APPID_CFG}" != "${EXPECTED_APPID}" ]]; then
  echo "[FAIL] pay channel appId mismatch: ${PAY_APPID_CFG} != ${EXPECTED_APPID}" >&2
  fail=1
fi

if [[ "${PAY_SUB_APPID_CFG}" != "${EXPECTED_APPID}" ]]; then
  echo "[FAIL] pay channel subAppId mismatch: ${PAY_SUB_APPID_CFG} != ${EXPECTED_APPID}" >&2
  fail=1
fi

if [[ "${TENANT_WEBSITES}" != *"${EXPECTED_APPID}"* ]]; then
  echo "[FAIL] system_tenant.websites does not include expected appid=${EXPECTED_APPID}" >&2
  fail=1
fi

# Built-in demo social secret should not be used after switching away from the demo appid.
if [[ "${SOCIAL_APPID}" != "${DEFAULT_SOCIAL_APPID}" && "${SOCIAL_SECRET}" == "${DEFAULT_SOCIAL_SECRET}" ]]; then
  echo "[FAIL] social client secret is still the default demo secret for ${DEFAULT_SOCIAL_APPID}; update to real appSecret for ${SOCIAL_APPID}" >&2
  fail=1
fi

if [[ "${fail}" -ne 0 ]]; then
  exit 1
fi

echo "[PASS] wx openid chain consistency check passed."
