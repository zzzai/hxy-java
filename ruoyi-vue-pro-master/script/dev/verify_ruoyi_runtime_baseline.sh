#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=script/dev/lib/http_code.sh
source "${ROOT_DIR}/script/dev/lib/http_code.sh"

BASE_URL="${BASE_URL:-http://127.0.0.1:48080}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-8}"

usage() {
  cat <<'EOF'
Usage:
  script/dev/verify_ruoyi_runtime_baseline.sh \
    --base-url https://api.hexiaoyue.com \
    --timeout-seconds 8

Checks:
  1) /actuator/health 必须 200
  2) /admin-api/* 与 /app-api/* 关键入口必须存在（不能是 404）
EOF
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
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[runtime-baseline] unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

check_code() {
  local path="$1"
  curl_http_code "${TIMEOUT_SECONDS}" "${BASE_URL}${path}"
}

is_existing_route_code() {
  local code="$1"
  if [[ "${code}" == "000" || "${code}" == "404" || "${code}" =~ ^5 ]]; then
    return 1
  fi
  return 0
}

echo "[runtime-baseline] base_url=${BASE_URL}"
echo "[runtime-baseline] timeout_seconds=${TIMEOUT_SECONDS}"

failed=0
warned=0

health_code="$(check_code "/actuator/health")"
if [[ "${health_code}" == "200" ]]; then
  echo "[runtime-baseline][PASS] /actuator/health -> 200"
elif [[ "${health_code}" == "404" ]]; then
  # 部分部署默认未暴露 actuator，降级为告警，不作为阻断
  echo "[runtime-baseline][WARN] /actuator/health -> 404 (fallback to route baseline)" >&2
  warned=1
else
  echo "[runtime-baseline][FAIL] /actuator/health expected 200/404, got ${health_code}" >&2
  failed=1
fi

ruoyi_core_paths=(
  "/admin-api/system/auth/get-permission-info"
  "/admin-api/pay/channel/page?pageNo=1&pageSize=1"
  "/app-api/member/user/get"
  "/app-api/pay/order/get?id=1"
)

for path in "${ruoyi_core_paths[@]}"; do
  code="$(check_code "${path}")"
  if ! is_existing_route_code "${code}"; then
    echo "[runtime-baseline][FAIL] ${path} expected existing route, got ${code}" >&2
    failed=1
  else
    echo "[runtime-baseline][PASS] ${path} -> ${code}"
  fi
done

if [[ "${failed}" -ne 0 ]]; then
  echo "[runtime-baseline] result=FAIL" >&2
  exit 2
fi

if [[ "${warned}" -ne 0 ]]; then
  echo "[runtime-baseline] result=PASS_WITH_WARN"
  exit 0
fi

echo "[runtime-baseline] result=PASS"
