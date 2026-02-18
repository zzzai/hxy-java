#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${BASE_DIR}/.env"
ENV_EXAMPLE="${BASE_DIR}/.env.example"
COMPOSE_FILE="${BASE_DIR}/docker-compose.backend.yml"

if [[ ! -f "${ENV_FILE}" ]]; then
  cp "${ENV_EXAMPLE}" "${ENV_FILE}"
  echo "未发现 ${ENV_FILE}，已按模板创建。请先修改配置后重试。"
  exit 1
fi

load_env_file() {
  local file="$1"
  while IFS= read -r line || [[ -n "${line}" ]]; do
    [[ -z "${line}" || "${line}" =~ ^[[:space:]]*# ]] && continue
    if [[ "${line}" =~ ^[A-Za-z_][A-Za-z0-9_]*= ]]; then
      local key="${line%%=*}"
      local value="${line#*=}"
      export "${key}=${value}"
    fi
  done < "${file}"
}

load_env_file "${ENV_FILE}"

require_cmd() {
  local cmd="$1"
  if ! command -v "${cmd}" >/dev/null 2>&1; then
    echo "[FAIL] 缺少命令: ${cmd}"
    exit 2
  fi
}

check_port() {
  local name="$1"
  local port="$2"
  if [[ -z "${port}" ]]; then
    echo "[WARN] ${name} 未配置端口，跳过"
    return 0
  fi
  if ss -ltn "sport = :${port}" 2>/dev/null | tail -n +2 | grep -q .; then
    echo "[WARN] 端口 ${port} 已被占用（${name}）"
  else
    echo "[OK] 端口 ${port} 可用（${name}）"
  fi
}

echo "[1/4] 校验依赖命令..."
require_cmd docker
require_cmd curl
require_cmd ss
echo "[OK] 依赖命令齐全"

echo "[2/4] 校验 Docker Daemon..."
docker info >/dev/null
echo "[OK] Docker Daemon 可用"

echo "[3/4] 校验 compose 配置..."
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" config >/dev/null
echo "[OK] compose 配置有效"

echo "[4/4] 端口预检..."
check_port MYSQL_HOST_PORT "${MYSQL_HOST_PORT:-}"
check_port REDIS_HOST_PORT "${REDIS_HOST_PORT:-}"
check_port ADMIN_HOST_PORT "${ADMIN_HOST_PORT:-}"
check_port FRONT_HOST_PORT "${FRONT_HOST_PORT:-}"

echo "预检通过。"
