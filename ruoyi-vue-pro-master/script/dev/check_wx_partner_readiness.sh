#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEFAULT_CONFIG="${ROOT_DIR}/script/dev/samples/wx_partner_channel.sample.json"

CONFIG_FILE="${CONFIG_FILE:-${DEFAULT_CONFIG}}"
CHANNEL_CODE="${CHANNEL_CODE:-wx_lite}"
EXPECT_APP_ID="${EXPECT_APP_ID:-}"
EXPECT_SUB_APP_ID="${EXPECT_SUB_APP_ID:-}"
EXPECT_SP_MCHID="${EXPECT_SP_MCHID:-}"
EXPECT_SUB_MCHID="${EXPECT_SUB_MCHID:-}"
REQUIRE_SUB_APP_ID="${REQUIRE_SUB_APP_ID:-auto}" # auto|1|0

usage() {
  cat <<'EOF'
Usage:
  script/dev/check_wx_partner_readiness.sh [options]

Options:
  --config FILE             Config JSON file path
  --channel-code CODE       wx_lite|wx_pub|wx_wap (default: wx_lite)
  --expect-appid APPID      Assert appId matches expected value
  --expect-sub-appid APPID  Assert subAppId matches expected value
  --expect-sp-mchid ID      Assert mchId matches expected sp_mchid
  --expect-sub-mchid ID     Assert subMchId matches expected sub_mchid
  --require-sub-appid 1|0   Force subAppId required / optional, default auto
  -h, --help                Show this help

Env alternatives:
  CONFIG_FILE, CHANNEL_CODE, EXPECT_APP_ID, EXPECT_SUB_APP_ID,
  EXPECT_SP_MCHID, EXPECT_SUB_MCHID, REQUIRE_SUB_APP_ID
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --config)
      CONFIG_FILE="${2:-}"
      shift 2
      ;;
    --channel-code)
      CHANNEL_CODE="${2:-}"
      shift 2
      ;;
    --expect-appid)
      EXPECT_APP_ID="${2:-}"
      shift 2
      ;;
    --expect-sub-appid)
      EXPECT_SUB_APP_ID="${2:-}"
      shift 2
      ;;
    --expect-sp-mchid)
      EXPECT_SP_MCHID="${2:-}"
      shift 2
      ;;
    --expect-sub-mchid)
      EXPECT_SUB_MCHID="${2:-}"
      shift 2
      ;;
    --require-sub-appid)
      REQUIRE_SUB_APP_ID="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[wx-partner-check][FAIL] unknown argument: $1" >&2
      usage
      exit 2
      ;;
  esac
done

if ! command -v jq >/dev/null 2>&1; then
  echo "[wx-partner-check][FAIL] jq is required" >&2
  exit 2
fi

if [[ ! -f "${CONFIG_FILE}" ]]; then
  echo "[wx-partner-check][FAIL] config file not found: ${CONFIG_FILE}" >&2
  exit 2
fi

if ! jq -e . "${CONFIG_FILE}" >/dev/null 2>&1; then
  echo "[wx-partner-check][FAIL] invalid JSON: ${CONFIG_FILE}" >&2
  exit 2
fi

if [[ "${REQUIRE_SUB_APP_ID}" != "auto" && "${REQUIRE_SUB_APP_ID}" != "1" && "${REQUIRE_SUB_APP_ID}" != "0" ]]; then
  echo "[wx-partner-check][FAIL] --require-sub-appid only accepts auto|1|0" >&2
  exit 2
fi

get_string() {
  local key="$1"
  jq -r "${key} // empty" "${CONFIG_FILE}"
}

fail_count=0

fail() {
  local message="$1"
  echo "[wx-partner-check][FAIL] ${message}" >&2
  fail_count=$((fail_count + 1))
}

check_not_blank() {
  local name="$1"
  local value="$2"
  if [[ -z "${value}" ]]; then
    fail "${name} is required"
    return 1
  fi
  return 0
}

api_version="$(get_string '.apiVersion')"
partner_mode="$(jq -r '.partnerMode' "${CONFIG_FILE}")"
app_id="$(get_string '.appId')"
sub_app_id="$(get_string '.subAppId')"
mch_id="$(get_string '.mchId')"
sub_mch_id="$(get_string '.subMchId')"
api_v3_key="$(get_string '.apiV3Key')"
cert_serial_no="$(get_string '.certSerialNo')"
private_key_content="$(get_string '.privateKeyContent')"
public_key_id="$(get_string '.publicKeyId')"
public_key_content="$(get_string '.publicKeyContent')"

check_not_blank "apiVersion" "${api_version}" || true
check_not_blank "appId" "${app_id}" || true
check_not_blank "mchId(sp_mchid)" "${mch_id}" || true
check_not_blank "subMchId(sub_mchid)" "${sub_mch_id}" || true
check_not_blank "apiV3Key" "${api_v3_key}" || true
check_not_blank "certSerialNo" "${cert_serial_no}" || true
check_not_blank "privateKeyContent" "${private_key_content}" || true
check_not_blank "publicKeyId" "${public_key_id}" || true

if [[ "${api_version}" != "v3" ]]; then
  fail "apiVersion must be v3, got: ${api_version:-<empty>}"
fi

if [[ "${partner_mode}" != "true" ]]; then
  fail "partnerMode must be true, got: ${partner_mode}"
fi

if [[ -n "${mch_id}" && ! "${mch_id}" =~ ^[0-9]{10}$ ]]; then
  fail "mchId(sp_mchid) must be 10 digits"
fi

if [[ -n "${sub_mch_id}" && ! "${sub_mch_id}" =~ ^[0-9]{10}$ ]]; then
  fail "subMchId(sub_mchid) must be 10 digits"
fi

if [[ -n "${mch_id}" && -n "${sub_mch_id}" && "${mch_id}" == "${sub_mch_id}" ]]; then
  fail "mchId(sp_mchid) must not equal subMchId(sub_mchid)"
fi

if [[ -n "${api_v3_key}" && ${#api_v3_key} -ne 32 ]]; then
  fail "apiV3Key length must be 32"
fi

if [[ -n "${cert_serial_no}" && ! "${cert_serial_no}" =~ ^[A-Fa-f0-9]{16,64}$ ]]; then
  fail "certSerialNo must be hex string (16~64 chars)"
fi

if [[ -n "${private_key_content}" ]]; then
  if [[ "${private_key_content}" != *"-----BEGIN PRIVATE KEY-----"* || "${private_key_content}" != *"-----END PRIVATE KEY-----"* ]]; then
    fail "privateKeyContent must contain BEGIN/END PRIVATE KEY block"
  fi
fi

if [[ -n "${public_key_content}" ]]; then
  if [[ "${public_key_content}" != *"-----BEGIN PUBLIC KEY-----"* || "${public_key_content}" != *"-----END PUBLIC KEY-----"* ]]; then
    fail "publicKeyContent must contain BEGIN/END PUBLIC KEY block"
  fi
fi

if [[ "${REQUIRE_SUB_APP_ID}" == "auto" ]]; then
  if [[ "${CHANNEL_CODE}" == "wx_lite" || "${CHANNEL_CODE}" == "wx_pub" ]]; then
    REQUIRE_SUB_APP_ID="1"
  else
    REQUIRE_SUB_APP_ID="0"
  fi
fi

if [[ "${REQUIRE_SUB_APP_ID}" == "1" && -z "${sub_app_id}" ]]; then
  fail "subAppId is required for channelCode=${CHANNEL_CODE}"
fi

if [[ -n "${EXPECT_APP_ID}" && "${app_id}" != "${EXPECT_APP_ID}" ]]; then
  fail "appId mismatch, expect=${EXPECT_APP_ID}, actual=${app_id:-<empty>}"
fi

if [[ -n "${EXPECT_SUB_APP_ID}" && "${sub_app_id}" != "${EXPECT_SUB_APP_ID}" ]]; then
  fail "subAppId mismatch, expect=${EXPECT_SUB_APP_ID}, actual=${sub_app_id:-<empty>}"
fi

if [[ -n "${EXPECT_SP_MCHID}" && "${mch_id}" != "${EXPECT_SP_MCHID}" ]]; then
  fail "mchId mismatch, expect=${EXPECT_SP_MCHID}, actual=${mch_id:-<empty>}"
fi

if [[ -n "${EXPECT_SUB_MCHID}" && "${sub_mch_id}" != "${EXPECT_SUB_MCHID}" ]]; then
  fail "subMchId mismatch, expect=${EXPECT_SUB_MCHID}, actual=${sub_mch_id:-<empty>}"
fi

if [[ "${fail_count}" -gt 0 ]]; then
  echo "[wx-partner-check] result=FAIL fail_count=${fail_count}" >&2
  exit 2
fi

echo "[wx-partner-check] config=${CONFIG_FILE}"
echo "[wx-partner-check] channel_code=${CHANNEL_CODE}"
echo "[wx-partner-check] partner_mode=${partner_mode} api_version=${api_version}"
echo "[wx-partner-check] mch_id=${mch_id} sub_mch_id=${sub_mch_id}"
if [[ -n "${sub_app_id}" ]]; then
  echo "[wx-partner-check] sub_app_id=${sub_app_id}"
fi
echo "[wx-partner-check] result=PASS"
