#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${BASE_DIR}/docker-compose.backend.yml"
ENV_FILE="${BASE_DIR}/.env"
ENV_EXAMPLE="${BASE_DIR}/.env.example"

if [[ ! -f "${ENV_FILE}" ]]; then
  cp "${ENV_EXAMPLE}" "${ENV_FILE}"
  echo "未发现 ${ENV_FILE}，已按模板创建。请先修改密码后重试。"
  exit 1
fi

mkdir -p "${BASE_DIR}/../../runtime/docker/mysql-data"
mkdir -p "${BASE_DIR}/../../runtime/docker/redis-data"
mkdir -p "${BASE_DIR}/../../runtime/docker/admin-log"
mkdir -p "${BASE_DIR}/../../runtime/docker/front-log"
mkdir -p "${BASE_DIR}/../../runtime/docker/images"

docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --build "$@"

