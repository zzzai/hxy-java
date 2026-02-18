#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PROJECT_DIR="$(cd "${BASE_DIR}/../.." && pwd)"
ENV_FILE="${BASE_DIR}/.env"
ENV_EXAMPLE="${BASE_DIR}/.env.example"
COMPOSE_FILE="${BASE_DIR}/docker-compose.backend.yml"

SKIP_BUILD=0
SKIP_SMOKE=0
SKIP_PREFLIGHT=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build)
      SKIP_BUILD=1
      shift
      ;;
    --skip-smoke)
      SKIP_SMOKE=1
      shift
      ;;
    --skip-preflight)
      SKIP_PREFLIGHT=1
      shift
      ;;
    *)
      echo "未知参数: $1"
      echo "用法: ./scripts/release_smoke.sh [--skip-build] [--skip-smoke] [--skip-preflight]"
      exit 1
      ;;
  esac
done

if [[ ! -f "${ENV_FILE}" ]]; then
  cp "${ENV_EXAMPLE}" "${ENV_FILE}"
  echo "未发现 ${ENV_FILE}，已按模板创建。请先修改配置后重试。"
  exit 1
fi

ADMIN_BASE_URL="${ADMIN_BASE_URL:-http://127.0.0.1:20500}"
FRONT_BASE_URL="${FRONT_BASE_URL:-http://127.0.0.1:20510}"
TARGET_USER_ID="${TARGET_USER_ID:-41}"
MAX_RETRY="${MAX_RETRY:-30}"
SLEEP_SECONDS="${SLEEP_SECONDS:-2}"

ADMIN_ACCOUNT="${ADMIN_ACCOUNT:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
FRONT_ACCOUNT="${FRONT_ACCOUNT:-18292417675}"
FRONT_PASSWORD="${FRONT_PASSWORD:-Crmeb_123456}"

if [[ "${SKIP_PREFLIGHT}" != "1" ]]; then
  echo "[0/4] 执行环境预检..."
  bash "${SCRIPT_DIR}/preflight.sh"
else
  echo "[0/4] 跳过环境预检（--skip-preflight）"
fi

wait_http() {
  local name="$1"
  local url="$2"
  local attempt=1
  while (( attempt <= MAX_RETRY )); do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      echo "[OK] ${name} 就绪: ${url}"
      return 0
    fi
    sleep "${SLEEP_SECONDS}"
    attempt=$((attempt + 1))
  done
  echo "[FAIL] ${name} 未就绪: ${url}"
  return 1
}

extract_token() {
  local resp="$1"
  printf "%s" "${resp}" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p'
}

extract_code() {
  local resp="$1"
  printf "%s" "${resp}" | sed -n 's/.*"code":\([0-9]\+\).*/\1/p'
}

if [[ "${SKIP_BUILD}" != "1" ]]; then
  echo "[1/4] 构建镜像..."
  bash "${SCRIPT_DIR}/build.sh"
else
  echo "[1/4] 跳过构建镜像（--skip-build）"
fi

echo "[2/4] 启动容器..."
mkdir -p "${BASE_DIR}/../../runtime/docker/mysql-data"
mkdir -p "${BASE_DIR}/../../runtime/docker/redis-data"
mkdir -p "${BASE_DIR}/../../runtime/docker/admin-log"
mkdir -p "${BASE_DIR}/../../runtime/docker/front-log"
mkdir -p "${BASE_DIR}/../../runtime/docker/images"
if [[ "${SKIP_BUILD}" == "1" ]]; then
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --no-build
else
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --build
fi

echo "[3/4] 健康检查..."
wait_http "admin" "${ADMIN_BASE_URL}/api/admin/getLoginPic"
wait_http "front" "${FRONT_BASE_URL}/api/front/login/config"

if [[ "${SKIP_SMOKE}" == "1" ]]; then
  echo "[4/4] 跳过联调冒烟（--skip-smoke）"
  exit 0
fi

echo "[4/4] 登录并执行治理冒烟..."
admin_login_resp="$(curl -fsS -X POST "${ADMIN_BASE_URL}/api/admin/login" \
  -H "Content-Type: application/json" \
  -d "{\"account\":\"${ADMIN_ACCOUNT}\",\"pwd\":\"${ADMIN_PASSWORD}\"}")"
admin_code="$(extract_code "${admin_login_resp}")"
admin_token="$(extract_token "${admin_login_resp}")"
if [[ "${admin_code}" != "200" || -z "${admin_token}" ]]; then
  echo "[FAIL] admin 登录失败: ${admin_login_resp}"
  exit 2
fi

front_login_resp="$(curl -fsS -X POST "${FRONT_BASE_URL}/api/front/login" \
  -H "Content-Type: application/json" \
  -d "{\"account\":\"${FRONT_ACCOUNT}\",\"password\":\"${FRONT_PASSWORD}\"}")"
front_code="$(extract_code "${front_login_resp}")"
front_token="$(extract_token "${front_login_resp}")"
if [[ "${front_code}" != "200" || -z "${front_token}" ]]; then
  echo "[FAIL] front 登录失败: ${front_login_resp}"
  exit 2
fi

FRONT_BASE_URL="${FRONT_BASE_URL}" \
ADMIN_BASE_URL="${ADMIN_BASE_URL}" \
TARGET_USER_ID="${TARGET_USER_ID}" \
FRONT_TOKEN="${front_token}" \
ADMIN_TOKEN="${admin_token}" \
bash "${PROJECT_DIR}/shell/data_governance_smoke.sh"

echo "发布联调流程完成。"
