#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_NAME="${DB_NAME:-hxy_dev}"
REQUIRE_P0_ZERO="${REQUIRE_P0_ZERO:-1}"
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_TSV="${OUTPUT_TSV:-}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_after_sale_review_ticket_gate.sh [options]

Options:
  --db-host <host>                 数据库地址（默认 localhost）
  --db-port <port>                 数据库端口（默认 3306）
  --db-user <user>                 数据库用户名（默认 root）
  --db-password <password>         数据库密码（默认空）
  --db-name <name>                 数据库名（默认 ruoyi-vue-pro）
  --require-p0-zero <0|1>          是否要求逾期 P0 工单为 0（默认 1）
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
    --require-p0-zero)
      REQUIRE_P0_ZERO="$2"
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

if ! [[ "${REQUIRE_P0_ZERO}" =~ ^[01]$ ]]; then
  echo "Invalid --require-p0-zero: ${REQUIRE_P0_ZERO}" >&2
  exit 1
fi

if ! command -v mysql >/dev/null 2>&1; then
  echo "mysql command not found" >&2
  exit 1
fi

if [[ -z "${SUMMARY_FILE}" ]]; then
  SUMMARY_FILE="/tmp/after_sale_review_ticket_gate/summary.txt"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="/tmp/after_sale_review_ticket_gate/result.tsv"
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

TABLE_EXISTS_SQL="SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'trade_after_sale_review_ticket';"
table_exists="$(mysql "${MYSQL_ARGS[@]}" -e "${TABLE_EXISTS_SQL}" 2>/dev/null || echo "")"

result="PASS"
exit_code=0
pending_overdue_total="0"
pending_overdue_p0="0"

if [[ "${table_exists}" != "1" ]]; then
  if [[ "${REQUIRE_P0_ZERO}" == "1" ]]; then
    result="BLOCK"
    exit_code=2
    add_issue "BLOCK" "RT01_TABLE_MISSING" "trade_after_sale_review_ticket table missing"
  else
    result="PASS_WITH_WARN"
    add_issue "WARN" "RT01_TABLE_MISSING" "trade_after_sale_review_ticket table missing"
  fi
else
  COUNT_SQL="
SELECT
  SUM(CASE WHEN status = 0 AND sla_deadline_time IS NOT NULL AND sla_deadline_time < NOW() THEN 1 ELSE 0 END) AS pending_overdue_total,
  SUM(CASE WHEN status = 0 AND severity = 'P0' AND sla_deadline_time IS NOT NULL AND sla_deadline_time < NOW() THEN 1 ELSE 0 END) AS pending_overdue_p0
FROM trade_after_sale_review_ticket
WHERE deleted = b'0';
"
  query_line="$(mysql "${MYSQL_ARGS[@]}" -e "${COUNT_SQL}" 2>/dev/null || true)"
  if [[ -z "${query_line}" ]]; then
    result="BLOCK"
    exit_code=2
    add_issue "BLOCK" "RT02_QUERY_FAILED" "failed to query trade_after_sale_review_ticket"
  else
    pending_overdue_total="$(awk '{print $1}' <<<"${query_line}")"
    pending_overdue_p0="$(awk '{print $2}' <<<"${query_line}")"
    [[ -z "${pending_overdue_total}" || "${pending_overdue_total}" == "NULL" ]] && pending_overdue_total="0"
    [[ -z "${pending_overdue_p0}" || "${pending_overdue_p0}" == "NULL" ]] && pending_overdue_p0="0"

    if [[ "${REQUIRE_P0_ZERO}" == "1" && "${pending_overdue_p0}" != "0" ]]; then
      result="BLOCK"
      exit_code=2
      add_issue "BLOCK" "RT03_OVERDUE_P0_EXISTS" "pending_overdue_p0=${pending_overdue_p0}"
    elif [[ "${pending_overdue_total}" != "0" ]]; then
      result="PASS_WITH_WARN"
      add_issue "WARN" "RT04_OVERDUE_EXISTS" "pending_overdue_total=${pending_overdue_total}, pending_overdue_p0=${pending_overdue_p0}"
    fi
  fi
fi

{
  echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "result=${result}"
  echo "require_p0_zero=${REQUIRE_P0_ZERO}"
  echo "pending_overdue_total=${pending_overdue_total}"
  echo "pending_overdue_p0=${pending_overdue_p0}"
  echo "db_host=${DB_HOST}"
  echo "db_port=${DB_PORT}"
  echo "db_name=${DB_NAME}"
  echo "output_tsv=${OUTPUT_TSV}"
} > "${SUMMARY_FILE}"

echo "== After Sale Review Ticket Gate =="
echo "result=${result}"
echo "pending_overdue_total=${pending_overdue_total}"
echo "pending_overdue_p0=${pending_overdue_p0}"
echo "summary_file=${SUMMARY_FILE}"
echo "output_tsv=${OUTPUT_TSV}"

exit "${exit_code}"
