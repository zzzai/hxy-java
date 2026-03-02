#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
JAR_PATH="$ROOT_DIR/yudao-server/target/yudao-server.jar"

if [[ ! -f "$JAR_PATH" ]]; then
  echo "Missing jar: $JAR_PATH" >&2
  echo "Run: mvn -DskipTests -pl yudao-server -am package" >&2
  exit 1
fi

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-hxy_dev}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"

REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

JAVA_OPTS="${JAVA_OPTS:--Xms512m -Xmx1024m}"
PROFILE="${PROFILE:-local}"
CRMEB_COMPAT_STAGE="${CRMEB_COMPAT_STAGE:-}"

if [[ -n "${CRMEB_COMPAT_STAGE}" ]]; then
  STAGE_PROFILE=""
  case "${CRMEB_COMPAT_STAGE}" in
    full-compat)
      STAGE_PROFILE="crmeb-gray"
      ;;
    payment-core-only)
      STAGE_PROFILE="crmeb-fast-track"
      ;;
    disabled)
      STAGE_PROFILE="crmeb-disabled"
      ;;
    *)
      echo "Unsupported CRMEB_COMPAT_STAGE: ${CRMEB_COMPAT_STAGE}" >&2
      echo "Allowed: full-compat | payment-core-only | disabled" >&2
      exit 1
      ;;
  esac
  if [[ ",${PROFILE}," != *",${STAGE_PROFILE},"* ]]; then
    PROFILE="${PROFILE},${STAGE_PROFILE}"
  fi
fi

DB_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true"

ARGS=(
  "--spring.profiles.active=${PROFILE}"
  "--spring.datasource.dynamic.datasource.master.url=${DB_URL}"
  "--spring.datasource.dynamic.datasource.master.username=${DB_USER}"
  "--spring.datasource.dynamic.datasource.master.password=${DB_PASSWORD}"
  "--spring.datasource.dynamic.datasource.slave.url=${DB_URL}"
  "--spring.datasource.dynamic.datasource.slave.username=${DB_USER}"
  "--spring.datasource.dynamic.datasource.slave.password=${DB_PASSWORD}"
  "--spring.redis.host=${REDIS_HOST}"
  "--spring.redis.port=${REDIS_PORT}"
)

if [[ -n "$REDIS_PASSWORD" ]]; then
  ARGS+=("--spring.redis.password=${REDIS_PASSWORD}")
fi

echo "Starting yudao-server with profile=${PROFILE} compat_stage=${CRMEB_COMPAT_STAGE:-none} db=${DB_HOST}:${DB_PORT}/${DB_NAME} redis=${REDIS_HOST}:${REDIS_PORT}"
exec java ${JAVA_OPTS} -jar "$JAR_PATH" "${ARGS[@]}" "$@"
