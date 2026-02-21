#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

BASE_URL="${BASE_URL:-http://127.0.0.1:48080}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-8}"
EXTRA_EXPECT_DISABLED=""
EXTRA_EXPECT_ENABLED=""

EXPECT_DISABLED=(
  "/api/front/order/list"
  "/api/front/order/detail/1"
  "/api/front/order/data"
  "/api/front/recharge/index"
  "/api/admin/store/order/list"
)

EXPECT_ENABLED=(
  "/api/front/pay/get/config"
  "/api/front/pay/queryPayResult?orderNo=smoke_order_no"
  "/api/front/order/get/pay/config"
  "/api/front/order/refund/reason"
  "/api/front/wechat/authorize/program/login"
  "/api/admin/system/config/check?k=pay_routine_appid"
  "/api/admin/store/order/refund/query?orderNo=smoke_order_no"
  "/api/admin/store/order/refund/ticket/list"
  "/api/admin/store/order/refund/confirm"
)

usage() {
  cat <<'EOF'
Usage:
  script/dev/verify_payment_core_fast_track.sh \
    --base-url http://127.0.0.1:48080 \
    --timeout-seconds 8

Options:
  --base-url URL              Server base URL (default: http://127.0.0.1:48080)
  --timeout-seconds N         Curl timeout in seconds (default: 8)
  --extra-expect-disabled CSV Additional disabled paths, comma-separated
  --extra-expect-enabled CSV  Additional enabled paths, comma-separated
  -h, --help                  Show this help
EOF
}

split_csv_append() {
  local raw="$1"
  local -n arr_ref="$2"
  local item=""
  IFS=',' read -r -a extra <<< "${raw}"
  for item in "${extra[@]}"; do
    item="${item#"${item%%[![:space:]]*}"}"
    item="${item%"${item##*[![:space:]]}"}"
    [[ -z "${item}" ]] && continue
    arr_ref+=("${item}")
  done
}

to_csv() {
  local -n arr_ref="$1"
  local output=""
  local item=""
  for item in "${arr_ref[@]}"; do
    if [[ -z "${output}" ]]; then
      output="${item}"
    else
      output="${output},${item}"
    fi
  done
  printf '%s' "${output}"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)
      BASE_URL="${2:-}"
      shift 2
      ;;
    --timeout-seconds)
      TIMEOUT_SECONDS="${2:-8}"
      shift 2
      ;;
    --extra-expect-disabled)
      EXTRA_EXPECT_DISABLED="${2:-}"
      shift 2
      ;;
    --extra-expect-enabled)
      EXTRA_EXPECT_ENABLED="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[payment-core-fast-track] unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -n "${EXTRA_EXPECT_DISABLED}" ]]; then
  split_csv_append "${EXTRA_EXPECT_DISABLED}" EXPECT_DISABLED
fi
if [[ -n "${EXTRA_EXPECT_ENABLED}" ]]; then
  split_csv_append "${EXTRA_EXPECT_ENABLED}" EXPECT_ENABLED
fi

echo "[payment-core-fast-track] base_url=${BASE_URL}"
echo "[payment-core-fast-track] timeout_seconds=${TIMEOUT_SECONDS}"
echo "[payment-core-fast-track] expect_disabled_count=${#EXPECT_DISABLED[@]}"
echo "[payment-core-fast-track] expect_enabled_count=${#EXPECT_ENABLED[@]}"

bash script/dev/verify_crmeb_cutover_paths.sh \
  --base-url "${BASE_URL}" \
  --timeout-seconds "${TIMEOUT_SECONDS}" \
  --require-enabled-existing 1 \
  --expect-disabled "$(to_csv EXPECT_DISABLED)" \
  --expect-enabled "$(to_csv EXPECT_ENABLED)"

echo "[payment-core-fast-track] result=PASS"
