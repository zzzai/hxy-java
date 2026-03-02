#!/usr/bin/env bash
set -euo pipefail

# Quick smoke check for admin login chain after frontend/backend rebuild.
# It validates:
# 1) tenant lookup
# 2) captcha get
# 3) username/password login
# 4) permission info + required menu routes

BASE_URL="${BASE_URL:-http://127.0.0.1:18080/prod-api}"
TENANT_HEADER_ID="${TENANT_HEADER_ID:-1}"
TENANT_NAME="${TENANT_NAME:-芋道源码}"
USERNAME="${USERNAME:-admin}"
PASSWORD="${PASSWORD:-admin123}"
CHECK_STORE_MENUS="${CHECK_STORE_MENUS:-1}"

urlencode() {
  local s="$1"
  local out=""
  local i c
  for ((i=0; i<${#s}; i++)); do
    c="${s:$i:1}"
    case "$c" in
      [a-zA-Z0-9.~_-]) out+="$c" ;;
      *) printf -v c_hex '%%%02X' "'$c"; out+="$c_hex" ;;
    esac
  done
  printf '%s' "$out"
}

fail() {
  echo "[FAIL] $*" >&2
  exit 1
}

echo "[INFO] BASE_URL=${BASE_URL}"
echo "[INFO] TENANT_HEADER_ID=${TENANT_HEADER_ID}, TENANT_NAME=${TENANT_NAME}"

tenant_name_encoded="$(urlencode "${TENANT_NAME}")"
tenant_resp="$(curl -s "${BASE_URL}/admin-api/system/tenant/get-id-by-name?name=${tenant_name_encoded}" -H "tenant-id: ${TENANT_HEADER_ID}")"
echo "${tenant_resp}" | grep -q '"code":0' || fail "tenant get-id-by-name failed: ${tenant_resp}"

captcha_resp="$(curl -s -X POST "${BASE_URL}/admin-api/system/captcha/get" \
  -H 'Content-Type: application/json' \
  -H "tenant-id: ${TENANT_HEADER_ID}" \
  -d '{"captchaType":"blockPuzzle"}')"
echo "${captcha_resp}" | grep -q '"repCode":"0000"' || fail "captcha get failed: ${captcha_resp}"

login_resp="$(curl -s -X POST "${BASE_URL}/admin-api/system/auth/login" \
  -H 'Content-Type: application/json' \
  -H "tenant-id: ${TENANT_HEADER_ID}" \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")"
echo "${login_resp}" | grep -q '"code":0' || fail "auth login failed: ${login_resp}"

token="$(echo "${login_resp}" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')"
[ -n "${token}" ] || fail "access token parse failed: ${login_resp}"

perm_resp="$(curl -s "${BASE_URL}/admin-api/system/auth/get-permission-info" \
  -H "Authorization: Bearer ${token}" \
  -H "tenant-id: ${TENANT_HEADER_ID}")"
echo "${perm_resp}" | grep -q '"code":0' || fail "permission info failed: ${perm_resp}"

if [ "${CHECK_STORE_MENUS}" = "1" ]; then
  echo "${perm_resp}" | grep -q 'store-spu-mapping' || fail "missing menu route: store-spu-mapping"
  echo "${perm_resp}" | grep -q 'store-sku-mapping' || fail "missing menu route: store-sku-mapping"
fi

echo "[PASS] admin login chain is healthy."
