#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

MYSQL_IMAGE="${MYSQL_IMAGE:-mysql:8.0}"
MYSQL_CONTAINER_NAME="${MYSQL_CONTAINER_NAME:-hxy-stagea-p0-17-18-${RANDOM}}"
MYSQL_PORT="${MYSQL_PORT:-auto}"
DB_NAME="${DB_NAME:-hxy_dev}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root}"
RUN_ID="${RUN_ID:-local_$(date +%Y%m%d_%H%M%S)_$RANDOM}"
ARTIFACT_BASE_DIR="${ARTIFACT_BASE_DIR:-${ROOT_DIR}/.tmp/payment_stagea_p0_17_18_local_ci}"
BIZ_DATE="${BIZ_DATE:-}"
STALE_MINUTES="${STALE_MINUTES:-10}"
RUN_STORE_SKU_STOCK_GATE="${RUN_STORE_SKU_STOCK_GATE:-1}"
REQUIRE_STORE_SKU_STOCK_GATE="${REQUIRE_STORE_SKU_STOCK_GATE:-0}"
REQUIRE_NAMING_GUARD="${REQUIRE_NAMING_GUARD:-1}"
REQUIRE_MEMORY_GUARD="${REQUIRE_MEMORY_GUARD:-1}"
EMIT_ON_WARN="${EMIT_ON_WARN:-0}"
ENABLE_NOTIFY="${ENABLE_NOTIFY:-0}"
NOTIFY_DRY_RUN="${NOTIFY_DRY_RUN:-1}"
NOTIFY_STRICT="${NOTIFY_STRICT:-0}"
KEEP_MYSQL_CONTAINER="${KEEP_MYSQL_CONTAINER:-0}"

TMP_BIN_DIR=""

usage() {
  cat <<'USAGE'
Usage:
  script/dev/run_payment_stagea_p0_17_18_local_ci.sh [options]

Options:
  --mysql-image <image>                 MySQL 镜像（默认 mysql:8.0）
  --mysql-container-name <name>         MySQL 容器名（默认自动生成）
  --mysql-port <port|auto>              本机映射端口（默认 auto，自动分配空闲端口）
  --db-name <name>                      数据库名（默认 hxy_dev）
  --db-user <user>                      数据库用户（默认 root）
  --db-password <password>              数据库密码（默认 root）
  --run-id <id>                         执行 ID（默认 local_时间戳）
  --artifact-base-dir <dir>             产物目录（默认 .tmp/payment_stagea_p0_17_18_local_ci）
  --biz-date <yyyy-mm-dd>               对账日期（可选）
  --stale-minutes <n>                   卡滞阈值分钟（默认 10）
  --run-store-sku-stock-gate <0|1>      是否跑库存流水门禁（默认 1）
  --require-store-sku-stock-gate <0|1>  库存流水门禁是否阻断（默认 0）
  --require-naming-guard <0|1>          命名门禁是否阻断（默认 1）
  --require-memory-guard <0|1>          记忆门禁是否阻断（默认 1）
  --emit-on-warn <0|1>                  WARN 是否生成工单（默认 0）
  --enable-notify <0|1>                 是否发送通知（默认 0）
  --notify-dry-run <0|1>                通知是否演练（默认 1）
  --notify-strict <0|1>                 通知失败是否阻断（默认 0）
  --keep-mysql-container <0|1>          是否保留 MySQL 容器（默认 0）
  -h, --help                            Show help

Notes:
  1) 脚本会拉起临时 MySQL 容器并导入最小 schema。
  2) 脚本会在当前会话临时注入 mysql wrapper（通过 docker exec 调用容器内 mysql）。
  3) 默认结束后自动清理容器；可用 --keep-mysql-container 1 保留。
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mysql-image)
      MYSQL_IMAGE="$2"
      shift 2
      ;;
    --mysql-container-name)
      MYSQL_CONTAINER_NAME="$2"
      shift 2
      ;;
    --mysql-port)
      MYSQL_PORT="$2"
      shift 2
      ;;
    --db-name)
      DB_NAME="$2"
      shift 2
      ;;
    --db-user)
      DB_USER="$2"
      shift 2
      ;;
    --db-password)
      DB_PASSWORD="$2"
      shift 2
      ;;
    --run-id)
      RUN_ID="$2"
      shift 2
      ;;
    --artifact-base-dir)
      ARTIFACT_BASE_DIR="$2"
      shift 2
      ;;
    --biz-date)
      BIZ_DATE="$2"
      shift 2
      ;;
    --stale-minutes)
      STALE_MINUTES="$2"
      shift 2
      ;;
    --run-store-sku-stock-gate)
      RUN_STORE_SKU_STOCK_GATE="$2"
      shift 2
      ;;
    --require-store-sku-stock-gate)
      REQUIRE_STORE_SKU_STOCK_GATE="$2"
      shift 2
      ;;
    --require-naming-guard)
      REQUIRE_NAMING_GUARD="$2"
      shift 2
      ;;
    --require-memory-guard)
      REQUIRE_MEMORY_GUARD="$2"
      shift 2
      ;;
    --emit-on-warn)
      EMIT_ON_WARN="$2"
      shift 2
      ;;
    --enable-notify)
      ENABLE_NOTIFY="$2"
      shift 2
      ;;
    --notify-dry-run)
      NOTIFY_DRY_RUN="$2"
      shift 2
      ;;
    --notify-strict)
      NOTIFY_STRICT="$2"
      shift 2
      ;;
    --keep-mysql-container)
      KEEP_MYSQL_CONTAINER="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

for flag in \
  "${RUN_STORE_SKU_STOCK_GATE}" \
  "${REQUIRE_STORE_SKU_STOCK_GATE}" \
  "${REQUIRE_NAMING_GUARD}" \
  "${REQUIRE_MEMORY_GUARD}" \
  "${EMIT_ON_WARN}" \
  "${ENABLE_NOTIFY}" \
  "${NOTIFY_DRY_RUN}" \
  "${NOTIFY_STRICT}" \
  "${KEEP_MYSQL_CONTAINER}"; do
  if ! [[ "${flag}" =~ ^[01]$ ]]; then
    echo "Invalid 0/1 flag value: ${flag}" >&2
    exit 1
  fi
done

if [[ -n "${BIZ_DATE}" ]] && ! [[ "${BIZ_DATE}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "Invalid --biz-date: ${BIZ_DATE}" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "docker command not found" >&2
  exit 1
fi

cleanup() {
  local rc=$?
  if [[ -n "${TMP_BIN_DIR}" && -d "${TMP_BIN_DIR}" ]]; then
    rm -rf "${TMP_BIN_DIR}" || true
  fi
  if [[ "${KEEP_MYSQL_CONTAINER}" == "0" ]]; then
    docker rm -f "${MYSQL_CONTAINER_NAME}" >/dev/null 2>&1 || true
  fi
  exit "${rc}"
}
trap cleanup EXIT

if docker ps -a --format '{{.Names}}' | grep -qx "${MYSQL_CONTAINER_NAME}"; then
  echo "Container already exists: ${MYSQL_CONTAINER_NAME}" >&2
  exit 1
fi

echo "[local-ci] start mysql container: ${MYSQL_CONTAINER_NAME} (${MYSQL_IMAGE})"
if [[ "${MYSQL_PORT}" == "auto" || -z "${MYSQL_PORT}" ]]; then
  docker run -d \
    --name "${MYSQL_CONTAINER_NAME}" \
    -e MYSQL_ROOT_PASSWORD="${DB_PASSWORD}" \
    -e MYSQL_DATABASE="${DB_NAME}" \
    -v "${ROOT_DIR}:${ROOT_DIR}:ro" \
    -p 127.0.0.1::3306 \
    "${MYSQL_IMAGE}" >/dev/null
  port_line="$(docker port "${MYSQL_CONTAINER_NAME}" 3306/tcp | head -n 1 || true)"
  MYSQL_PORT="${port_line##*:}"
else
  if ! [[ "${MYSQL_PORT}" =~ ^[0-9]+$ ]]; then
    echo "Invalid --mysql-port: ${MYSQL_PORT}" >&2
    exit 1
  fi
  if ! docker run -d \
    --name "${MYSQL_CONTAINER_NAME}" \
    -e MYSQL_ROOT_PASSWORD="${DB_PASSWORD}" \
    -e MYSQL_DATABASE="${DB_NAME}" \
    -v "${ROOT_DIR}:${ROOT_DIR}:ro" \
    -p "${MYSQL_PORT}:3306" \
    "${MYSQL_IMAGE}" >/dev/null; then
    echo "Failed to bind --mysql-port ${MYSQL_PORT}, retry with auto port..." >&2
    docker rm -f "${MYSQL_CONTAINER_NAME}" >/dev/null 2>&1 || true
    docker run -d \
      --name "${MYSQL_CONTAINER_NAME}" \
      -e MYSQL_ROOT_PASSWORD="${DB_PASSWORD}" \
      -e MYSQL_DATABASE="${DB_NAME}" \
      -v "${ROOT_DIR}:${ROOT_DIR}:ro" \
      -p 127.0.0.1::3306 \
      "${MYSQL_IMAGE}" >/dev/null
    port_line="$(docker port "${MYSQL_CONTAINER_NAME}" 3306/tcp | head -n 1 || true)"
    MYSQL_PORT="${port_line##*:}"
  fi
fi
echo "[local-ci] mysql host port: ${MYSQL_PORT}"

echo "[local-ci] waiting mysql ready..."
for i in {1..60}; do
  if docker exec "${MYSQL_CONTAINER_NAME}" mysqladmin ping -uroot "-p${DB_PASSWORD}" --silent >/dev/null 2>&1; then
    echo "[local-ci] mysql ready"
    break
  fi
  if [[ "${i}" == "60" ]]; then
    echo "mysql not ready in time" >&2
    exit 1
  fi
  sleep 2
done

echo "[local-ci] bootstrap schema"
docker exec -i "${MYSQL_CONTAINER_NAME}" mysql -uroot "-p${DB_PASSWORD}" "${DB_NAME}" < "${ROOT_DIR}/sql/mysql/ruoyi-modules-member-pay-mall.sql"
docker exec -i "${MYSQL_CONTAINER_NAME}" mysql -uroot "-p${DB_PASSWORD}" "${DB_NAME}" < "${ROOT_DIR}/sql/mysql/hxy/2026-02-28-hxy-store-product-mapping.sql"
docker exec -i "${MYSQL_CONTAINER_NAME}" mysql -uroot "-p${DB_PASSWORD}" "${DB_NAME}" < "${ROOT_DIR}/sql/mysql/hxy/2026-03-01-hxy-store-sku-stock-flow.sql"

TMP_BIN_DIR="$(mktemp -d)"
cat > "${TMP_BIN_DIR}/mysql" <<'WRAP'
#!/usr/bin/env bash
set -euo pipefail
CONTAINER_NAME="${LOCAL_CI_MYSQL_CONTAINER_NAME:?LOCAL_CI_MYSQL_CONTAINER_NAME is required}"
args=()
skip_next=0
for arg in "$@"; do
  if [[ "${skip_next}" == "1" ]]; then
    skip_next=0
    continue
  fi
  case "${arg}" in
    --host|--port|-h|-P)
      skip_next=1
      ;;
    --host=*|--port=*|-h*|-P*)
      ;;
    *)
      args+=("${arg}")
      ;;
  esac
done
exec docker exec -i "${CONTAINER_NAME}" mysql "${args[@]}"
WRAP
chmod +x "${TMP_BIN_DIR}/mysql"
export PATH="${TMP_BIN_DIR}:${PATH}"
export LOCAL_CI_MYSQL_CONTAINER_NAME="${MYSQL_CONTAINER_NAME}"

echo "[local-ci] run stageA p0 17/18 pipeline"
set +e
RUN_ID="${RUN_ID}" \
ARTIFACT_BASE_DIR="${ARTIFACT_BASE_DIR}" \
BIZ_DATE="${BIZ_DATE}" \
STALE_MINUTES="${STALE_MINUTES}" \
DB_HOST="127.0.0.1" \
DB_PORT="${MYSQL_PORT}" \
DB_USER="${DB_USER}" \
DB_PASSWORD="${DB_PASSWORD}" \
DB_NAME="${DB_NAME}" \
RUN_STORE_SKU_STOCK_GATE="${RUN_STORE_SKU_STOCK_GATE}" \
REQUIRE_STORE_SKU_STOCK_GATE="${REQUIRE_STORE_SKU_STOCK_GATE}" \
REQUIRE_NAMING_GUARD="${REQUIRE_NAMING_GUARD}" \
REQUIRE_MEMORY_GUARD="${REQUIRE_MEMORY_GUARD}" \
EMIT_ON_WARN="${EMIT_ON_WARN}" \
ENABLE_NOTIFY="${ENABLE_NOTIFY}" \
NOTIFY_DRY_RUN="${NOTIFY_DRY_RUN}" \
NOTIFY_STRICT="${NOTIFY_STRICT}" \
bash script/dev/run_payment_stagea_p0_17_18.sh
PIPE_RC=$?
set -e

OUT_DIR="${ARTIFACT_BASE_DIR}/${RUN_ID}"
if [[ ! -d "${OUT_DIR}" ]]; then
  OUT_DIR="$(ls -1dt "${ARTIFACT_BASE_DIR}"/* 2>/dev/null | head -n 1 || true)"
fi
SUMMARY_FILE="${OUT_DIR}/summary.txt"

echo "[local-ci] pipeline_rc=${PIPE_RC}"
echo "[local-ci] out_dir=${OUT_DIR}"
if [[ -f "${SUMMARY_FILE}" ]]; then
  echo "[local-ci] summary=${SUMMARY_FILE}"
  tail -n 30 "${SUMMARY_FILE}" || true
fi

if [[ "${KEEP_MYSQL_CONTAINER}" == "1" ]]; then
  echo "[local-ci] keep mysql container: ${MYSQL_CONTAINER_NAME}"
  echo "[local-ci] connect: docker exec -it ${MYSQL_CONTAINER_NAME} mysql -uroot -p${DB_PASSWORD} ${DB_NAME}"
fi

exit "${PIPE_RC}"
