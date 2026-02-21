#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:48080}"
EXPECT_DISABLED=()
EXPECT_ENABLED=()
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-8}"
REQUIRE_ENABLED_EXISTING="${REQUIRE_ENABLED_EXISTING:-0}"

usage() {
  cat <<'EOF'
Usage:
  script/dev/verify_crmeb_cutover_paths.sh \
    --base-url http://127.0.0.1:48080 \
    --expect-disabled /api/front/order/list,/api/admin/store/order/list \
    --expect-enabled /api/front/pay/get/config,/api/front/order/data

Options:
  --base-url URL             Server base URL (default: http://127.0.0.1:48080)
  --expect-disabled CSV      Paths expected to return HTTP 410
  --expect-enabled CSV       Paths expected NOT to return HTTP 410
  --timeout-seconds N        curl timeout in seconds (default: 8)
  --require-enabled-existing
                             When set to 1, enabled path must be existing route
                             (not 404/000/5xx), default: 0
  -h, --help                 Show this help
EOF
}

split_csv() {
  local raw="$1"
  local -n out_arr_ref="$2"
  if [[ -z "${raw}" ]]; then
    return
  fi
  IFS=',' read -r -a out_arr_ref <<< "${raw}"
}

trim() {
  local s="$1"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  printf '%s' "${s}"
}

check_http_code() {
  local url="$1"
  curl -sS -o /dev/null -m "${TIMEOUT_SECONDS}" -w '%{http_code}' "${url}"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)
      BASE_URL="${2:-}"
      shift 2
      ;;
    --expect-disabled)
      raw="${2:-}"
      split_csv "${raw}" EXPECT_DISABLED
      shift 2
      ;;
    --expect-enabled)
      raw="${2:-}"
      split_csv "${raw}" EXPECT_ENABLED
      shift 2
      ;;
    --timeout-seconds)
      TIMEOUT_SECONDS="${2:-8}"
      shift 2
      ;;
    --require-enabled-existing)
      REQUIRE_ENABLED_EXISTING="${2:-0}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[cutover-check] unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ ${#EXPECT_DISABLED[@]} -eq 0 && ${#EXPECT_ENABLED[@]} -eq 0 ]]; then
  echo "[cutover-check] no paths provided, nothing to verify" >&2
  usage
  exit 1
fi

echo "[cutover-check] base_url=${BASE_URL}"
echo "[cutover-check] timeout_seconds=${TIMEOUT_SECONDS}"
echo "[cutover-check] require_enabled_existing=${REQUIRE_ENABLED_EXISTING}"

failed=0

for path in "${EXPECT_DISABLED[@]}"; do
  path="$(trim "${path}")"
  [[ -z "${path}" ]] && continue
  code="$(check_http_code "${BASE_URL}${path}")"
  if [[ "${code}" != "410" ]]; then
    echo "[cutover-check][FAIL] expected 410 but got ${code}: ${path}" >&2
    failed=1
  else
    echo "[cutover-check][PASS] 410 as expected: ${path}"
  fi
done

for path in "${EXPECT_ENABLED[@]}"; do
  path="$(trim "${path}")"
  [[ -z "${path}" ]] && continue
  code="$(check_http_code "${BASE_URL}${path}")"
  if [[ "${code}" == "410" ]]; then
    echo "[cutover-check][FAIL] expected non-410 but got 410: ${path}" >&2
    failed=1
  elif [[ "${REQUIRE_ENABLED_EXISTING}" == "1" && ( "${code}" == "404" || "${code}" == "000" || "${code}" =~ ^5 ) ]]; then
    echo "[cutover-check][FAIL] expected existing route but got ${code}: ${path}" >&2
    failed=1
  else
    echo "[cutover-check][PASS] non-410(${code}) as expected: ${path}"
  fi
done

if [[ "${failed}" -ne 0 ]]; then
  echo "[cutover-check] result=FAIL" >&2
  exit 2
fi

echo "[cutover-check] result=PASS"
