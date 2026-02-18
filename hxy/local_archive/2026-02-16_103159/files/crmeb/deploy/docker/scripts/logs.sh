#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${BASE_DIR}/docker-compose.backend.yml"
ENV_FILE="${BASE_DIR}/.env"

docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" logs -f --tail=200 "$@"

