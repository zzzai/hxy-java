#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_NAME="${DB_NAME:-hxy_dev}"
REQUIRE_OVERDUE_ZERO="${REQUIRE_OVERDUE_ZERO:-1}"
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_TSV="${OUTPUT_TSV:-}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_store_sku_stock_gate.sh [options]

Options:
  --db-host <host>                 数据库地址（默认 localhost）
  --db-port <port>                 数据库端口（默认 3306）
  --db-user <user>                 数据库用户名（默认 root）
  --db-password <password>         数据库密码（默认空）
  --db-name <name>                 数据库名（默认 hxy_dev）
  --require-overdue-zero <0|1>     是否要求库存流水逾期待重试为 0（默认 1）
  --summary-file <file>            输出 summary（可选）
  --output-tsv <file>              输出详情 TSV（可选）
  -h, --help                       显示帮助

Exit Code:
  0: PASS / PASS_WITH_WARN
  2: BLOCK
  1: 执行异常
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --db-host)
      DB_HOST="$2"
      shift 2
      ;;
    --db-port)
      DB_PORT="$2"
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
    --db-name)
      DB_NAME="$2"
      shift 2
      ;;
    --require-overdue-zero)
      REQUIRE_OVERDUE_ZERO="$2"
      shift 2
      ;;
    --summary-file)
      SUMMARY_FILE="$2"
      shift 2
      ;;
    --output-tsv)
      OUTPUT_TSV="$2"
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

if ! [[ "${REQUIRE_OVERDUE_ZERO}" =~ ^[01]$ ]]; then
  echo "Invalid --require-overdue-zero: ${REQUIRE_OVERDUE_ZERO}" >&2
  exit 1
fi

if ! command -v mysql >/dev/null 2>&1; then
  echo "mysql command not found" >&2
  exit 1
fi

if [[ -z "${SUMMARY_FILE}" ]]; then
  SUMMARY_FILE="/tmp/store_sku_stock_gate/summary.txt"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="/tmp/store_sku_stock_gate/result.tsv"
fi

mkdir -p "$(dirname "${SUMMARY_FILE}")" "$(dirname "${OUTPUT_TSV}")"
echo -e "severity\tcode\tdetail" > "${OUTPUT_TSV}"

add_issue() {
  local severity="$1"
  local code="$2"
  local detail="$3"
  echo -e "${severity}\t${code}\t${detail}" >> "${OUTPUT_TSV}"
}

MYSQL_ARGS=(--host "${DB_HOST}" --port "${DB_PORT}" --user "${DB_USER}" --database "${DB_NAME}" --batch --skip-column-names --default-character-set=utf8mb4)
if [[ -n "${DB_PASSWORD}" ]]; then
  MYSQL_ARGS+=(--password="${DB_PASSWORD}")
fi

TABLE_EXISTS_SQL="SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'hxy_store_product_sku_stock_flow';"
table_exists="$(mysql "${MYSQL_ARGS[@]}" -e "${TABLE_EXISTS_SQL}" 2>/dev/null || echo "")"

result="PASS"
exit_code=0
pending_total="0"
failed_total="0"
retry_overdue_total="0"
retry_now_total="0"

if [[ "${table_exists}" != "1" ]]; then
  if [[ "${REQUIRE_OVERDUE_ZERO}" == "1" ]]; then
    result="BLOCK"
    exit_code=2
    add_issue "BLOCK" "SS01_TABLE_MISSING" "hxy_store_product_sku_stock_flow table missing"
  else
    result="PASS_WITH_WARN"
    add_issue "WARN" "SS01_TABLE_MISSING" "hxy_store_product_sku_stock_flow table missing"
  fi
else
  COUNT_SQL="
SELECT
  SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS pending_total,
  SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS failed_total,
  SUM(CASE WHEN status IN (0, 2)
            AND next_retry_time IS NOT NULL
            AND next_retry_time < NOW()
      THEN 1 ELSE 0 END) AS retry_overdue_total,
  SUM(CASE WHEN status IN (0, 2)
            AND (next_retry_time IS NULL OR next_retry_time <= NOW())
      THEN 1 ELSE 0 END) AS retry_now_total
FROM hxy_store_product_sku_stock_flow
WHERE deleted = b'0';
"
  query_line="$(mysql "${MYSQL_ARGS[@]}" -e "${COUNT_SQL}" 2>/dev/null || true)"
  if [[ -z "${query_line}" ]]; then
    result="BLOCK"
    exit_code=2
    add_issue "BLOCK" "SS02_QUERY_FAILED" "failed to query hxy_store_product_sku_stock_flow"
  else
    pending_total="$(awk '{print $1}' <<<"${query_line}")"
    failed_total="$(awk '{print $2}' <<<"${query_line}")"
    retry_overdue_total="$(awk '{print $3}' <<<"${query_line}")"
    retry_now_total="$(awk '{print $4}' <<<"${query_line}")"
    [[ -z "${pending_total}" || "${pending_total}" == "NULL" ]] && pending_total="0"
    [[ -z "${failed_total}" || "${failed_total}" == "NULL" ]] && failed_total="0"
    [[ -z "${retry_overdue_total}" || "${retry_overdue_total}" == "NULL" ]] && retry_overdue_total="0"
    [[ -z "${retry_now_total}" || "${retry_now_total}" == "NULL" ]] && retry_now_total="0"

    if [[ "${REQUIRE_OVERDUE_ZERO}" == "1" && "${retry_overdue_total}" != "0" ]]; then
      result="BLOCK"
      exit_code=2
      add_issue "BLOCK" "SS03_RETRY_OVERDUE_EXISTS" "retry_overdue_total=${retry_overdue_total}"
    elif [[ "${retry_overdue_total}" != "0" ]]; then
      result="PASS_WITH_WARN"
      add_issue "WARN" "SS04_RETRY_OVERDUE_WARN" "retry_overdue_total=${retry_overdue_total}"
    fi

    if [[ "${retry_now_total}" != "0" && "${retry_overdue_total}" == "0" ]]; then
      result="PASS_WITH_WARN"
      add_issue "WARN" "SS05_RETRY_PENDING_WARN" "retry_now_total=${retry_now_total}, pending_total=${pending_total}, failed_total=${failed_total}"
    fi
  fi
fi

{
  echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "result=${result}"
  echo "require_overdue_zero=${REQUIRE_OVERDUE_ZERO}"
  echo "pending_total=${pending_total}"
  echo "failed_total=${failed_total}"
  echo "retry_overdue_total=${retry_overdue_total}"
  echo "retry_now_total=${retry_now_total}"
  echo "db_host=${DB_HOST}"
  echo "db_port=${DB_PORT}"
  echo "db_name=${DB_NAME}"
  echo "output_tsv=${OUTPUT_TSV}"
} > "${SUMMARY_FILE}"

echo "== Store SKU Stock Gate =="
echo "result=${result}"
echo "pending_total=${pending_total}"
echo "failed_total=${failed_total}"
echo "retry_overdue_total=${retry_overdue_total}"
echo "retry_now_total=${retry_now_total}"
echo "summary_file=${SUMMARY_FILE}"
echo "output_tsv=${OUTPUT_TSV}"

exit "${exit_code}"
