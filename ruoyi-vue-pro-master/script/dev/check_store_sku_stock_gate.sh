#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_NAME="${DB_NAME:-hxy_dev}"
REQUIRE_OVERDUE_ZERO="${REQUIRE_OVERDUE_ZERO:-1}"
REQUIRE_APPROVAL_OVERDUE_ZERO="${REQUIRE_APPROVAL_OVERDUE_ZERO:-${REQUIRE_OVERDUE_ZERO}}"
PENDING_APPROVAL_TIMEOUT_MINUTES="${PENDING_APPROVAL_TIMEOUT_MINUTES:-120}"
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
  --require-approval-overdue-zero <0|1> 是否要求库存审批单待审批超时为 0（默认同 --require-overdue-zero）
  --pending-approval-timeout-minutes <n> 库存审批单待审批超时阈值分钟（默认 120）
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
    --require-approval-overdue-zero)
      REQUIRE_APPROVAL_OVERDUE_ZERO="$2"
      shift 2
      ;;
    --pending-approval-timeout-minutes)
      PENDING_APPROVAL_TIMEOUT_MINUTES="$2"
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
if ! [[ "${REQUIRE_APPROVAL_OVERDUE_ZERO}" =~ ^[01]$ ]]; then
  echo "Invalid --require-approval-overdue-zero: ${REQUIRE_APPROVAL_OVERDUE_ZERO}" >&2
  exit 1
fi
if ! [[ "${PENDING_APPROVAL_TIMEOUT_MINUTES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "Invalid --pending-approval-timeout-minutes: ${PENDING_APPROVAL_TIMEOUT_MINUTES}" >&2
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

mark_warn() {
  if [[ "${result}" == "PASS" ]]; then
    result="PASS_WITH_WARN"
  fi
}

mark_block() {
  result="BLOCK"
  exit_code=2
}

MYSQL_ARGS=(--host "${DB_HOST}" --port "${DB_PORT}" --user "${DB_USER}" --database "${DB_NAME}" --batch --skip-column-names --default-character-set=utf8mb4)
if [[ -n "${DB_PASSWORD}" ]]; then
  MYSQL_ARGS+=(--password="${DB_PASSWORD}")
fi

result="PASS"
exit_code=0
pending_total="0"
failed_total="0"
retry_overdue_total="0"
retry_now_total="0"
adjust_pending_total="0"
adjust_pending_overdue_total="0"
transfer_pending_total="0"
transfer_pending_overdue_total="0"

table_exists_sql() {
  local table_name="$1"
  mysql "${MYSQL_ARGS[@]}" -e "SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '${table_name}';" 2>/dev/null || true
}

stock_flow_table_exists="$(table_exists_sql "hxy_store_product_sku_stock_flow")"
if [[ "${stock_flow_table_exists}" != "1" ]]; then
  if [[ "${REQUIRE_OVERDUE_ZERO}" == "1" ]]; then
    mark_block
    add_issue "BLOCK" "SS01_TABLE_MISSING" "hxy_store_product_sku_stock_flow table missing"
  else
    mark_warn
    add_issue "WARN" "SS01_TABLE_MISSING" "hxy_store_product_sku_stock_flow table missing"
  fi
else
  stock_flow_count_sql="
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
  stock_flow_query_line="$(mysql "${MYSQL_ARGS[@]}" -e "${stock_flow_count_sql}" 2>/dev/null || true)"
  if [[ -z "${stock_flow_query_line}" ]]; then
    mark_block
    add_issue "BLOCK" "SS02_QUERY_FAILED" "failed to query hxy_store_product_sku_stock_flow"
  else
    pending_total="$(awk '{print $1}' <<<"${stock_flow_query_line}")"
    failed_total="$(awk '{print $2}' <<<"${stock_flow_query_line}")"
    retry_overdue_total="$(awk '{print $3}' <<<"${stock_flow_query_line}")"
    retry_now_total="$(awk '{print $4}' <<<"${stock_flow_query_line}")"
    [[ -z "${pending_total}" || "${pending_total}" == "NULL" ]] && pending_total="0"
    [[ -z "${failed_total}" || "${failed_total}" == "NULL" ]] && failed_total="0"
    [[ -z "${retry_overdue_total}" || "${retry_overdue_total}" == "NULL" ]] && retry_overdue_total="0"
    [[ -z "${retry_now_total}" || "${retry_now_total}" == "NULL" ]] && retry_now_total="0"

    if [[ "${REQUIRE_OVERDUE_ZERO}" == "1" && "${retry_overdue_total}" != "0" ]]; then
      mark_block
      add_issue "BLOCK" "SS03_RETRY_OVERDUE_EXISTS" "retry_overdue_total=${retry_overdue_total}"
    elif [[ "${retry_overdue_total}" != "0" ]]; then
      mark_warn
      add_issue "WARN" "SS04_RETRY_OVERDUE_WARN" "retry_overdue_total=${retry_overdue_total}"
    fi

    if [[ "${retry_now_total}" != "0" && "${retry_overdue_total}" == "0" ]]; then
      mark_warn
      add_issue "WARN" "SS05_RETRY_PENDING_WARN" "retry_now_total=${retry_now_total}, pending_total=${pending_total}, failed_total=${failed_total}"
    fi
  fi
fi

stock_adjust_table_exists="$(table_exists_sql "hxy_store_sku_stock_adjust_order")"
if [[ "${stock_adjust_table_exists}" != "1" ]]; then
  if [[ "${REQUIRE_APPROVAL_OVERDUE_ZERO}" == "1" ]]; then
    mark_block
    add_issue "BLOCK" "SS06_TABLE_MISSING" "hxy_store_sku_stock_adjust_order table missing"
  else
    mark_warn
    add_issue "WARN" "SS06_TABLE_MISSING" "hxy_store_sku_stock_adjust_order table missing"
  fi
else
  stock_adjust_count_sql="
SELECT
  SUM(CASE WHEN status = 10 THEN 1 ELSE 0 END) AS pending_total,
  SUM(CASE WHEN status = 10
            AND COALESCE(last_action_time, update_time, create_time) IS NOT NULL
            AND COALESCE(last_action_time, update_time, create_time) < DATE_SUB(NOW(), INTERVAL ${PENDING_APPROVAL_TIMEOUT_MINUTES} MINUTE)
      THEN 1 ELSE 0 END) AS pending_overdue_total
FROM hxy_store_sku_stock_adjust_order
WHERE deleted = b'0';
"
  stock_adjust_query_line="$(mysql "${MYSQL_ARGS[@]}" -e "${stock_adjust_count_sql}" 2>/dev/null || true)"
  if [[ -z "${stock_adjust_query_line}" ]]; then
    mark_block
    add_issue "BLOCK" "SS07_QUERY_FAILED" "failed to query hxy_store_sku_stock_adjust_order"
  else
    adjust_pending_total="$(awk '{print $1}' <<<"${stock_adjust_query_line}")"
    adjust_pending_overdue_total="$(awk '{print $2}' <<<"${stock_adjust_query_line}")"
    [[ -z "${adjust_pending_total}" || "${adjust_pending_total}" == "NULL" ]] && adjust_pending_total="0"
    [[ -z "${adjust_pending_overdue_total}" || "${adjust_pending_overdue_total}" == "NULL" ]] && adjust_pending_overdue_total="0"

    if [[ "${REQUIRE_APPROVAL_OVERDUE_ZERO}" == "1" && "${adjust_pending_overdue_total}" != "0" ]]; then
      mark_block
      add_issue "BLOCK" "SS08_ADJUST_OVERDUE_EXISTS" "pending_overdue_total=${adjust_pending_overdue_total}, timeout_minutes=${PENDING_APPROVAL_TIMEOUT_MINUTES}"
    elif [[ "${adjust_pending_overdue_total}" != "0" ]]; then
      mark_warn
      add_issue "WARN" "SS09_ADJUST_OVERDUE_WARN" "pending_overdue_total=${adjust_pending_overdue_total}, timeout_minutes=${PENDING_APPROVAL_TIMEOUT_MINUTES}"
    fi

    if [[ "${adjust_pending_total}" != "0" && "${adjust_pending_overdue_total}" == "0" ]]; then
      mark_warn
      add_issue "WARN" "SS10_ADJUST_PENDING_WARN" "pending_total=${adjust_pending_total}, pending_overdue_total=${adjust_pending_overdue_total}"
    fi
  fi
fi

transfer_table_exists="$(table_exists_sql "hxy_store_sku_transfer_order")"
if [[ "${transfer_table_exists}" != "1" ]]; then
  if [[ "${REQUIRE_APPROVAL_OVERDUE_ZERO}" == "1" ]]; then
    mark_block
    add_issue "BLOCK" "SS11_TABLE_MISSING" "hxy_store_sku_transfer_order table missing"
  else
    mark_warn
    add_issue "WARN" "SS11_TABLE_MISSING" "hxy_store_sku_transfer_order table missing"
  fi
else
  transfer_count_sql="
SELECT
  SUM(CASE WHEN status = 10 THEN 1 ELSE 0 END) AS pending_total,
  SUM(CASE WHEN status = 10
            AND COALESCE(last_action_time, update_time, create_time) IS NOT NULL
            AND COALESCE(last_action_time, update_time, create_time) < DATE_SUB(NOW(), INTERVAL ${PENDING_APPROVAL_TIMEOUT_MINUTES} MINUTE)
      THEN 1 ELSE 0 END) AS pending_overdue_total
FROM hxy_store_sku_transfer_order
WHERE deleted = b'0';
"
  transfer_query_line="$(mysql "${MYSQL_ARGS[@]}" -e "${transfer_count_sql}" 2>/dev/null || true)"
  if [[ -z "${transfer_query_line}" ]]; then
    mark_block
    add_issue "BLOCK" "SS12_QUERY_FAILED" "failed to query hxy_store_sku_transfer_order"
  else
    transfer_pending_total="$(awk '{print $1}' <<<"${transfer_query_line}")"
    transfer_pending_overdue_total="$(awk '{print $2}' <<<"${transfer_query_line}")"
    [[ -z "${transfer_pending_total}" || "${transfer_pending_total}" == "NULL" ]] && transfer_pending_total="0"
    [[ -z "${transfer_pending_overdue_total}" || "${transfer_pending_overdue_total}" == "NULL" ]] && transfer_pending_overdue_total="0"

    if [[ "${REQUIRE_APPROVAL_OVERDUE_ZERO}" == "1" && "${transfer_pending_overdue_total}" != "0" ]]; then
      mark_block
      add_issue "BLOCK" "SS13_TRANSFER_OVERDUE_EXISTS" "pending_overdue_total=${transfer_pending_overdue_total}, timeout_minutes=${PENDING_APPROVAL_TIMEOUT_MINUTES}"
    elif [[ "${transfer_pending_overdue_total}" != "0" ]]; then
      mark_warn
      add_issue "WARN" "SS14_TRANSFER_OVERDUE_WARN" "pending_overdue_total=${transfer_pending_overdue_total}, timeout_minutes=${PENDING_APPROVAL_TIMEOUT_MINUTES}"
    fi

    if [[ "${transfer_pending_total}" != "0" && "${transfer_pending_overdue_total}" == "0" ]]; then
      mark_warn
      add_issue "WARN" "SS15_TRANSFER_PENDING_WARN" "pending_total=${transfer_pending_total}, pending_overdue_total=${transfer_pending_overdue_total}"
    fi
  fi
fi

{
  echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "result=${result}"
  echo "require_overdue_zero=${REQUIRE_OVERDUE_ZERO}"
  echo "require_approval_overdue_zero=${REQUIRE_APPROVAL_OVERDUE_ZERO}"
  echo "pending_approval_timeout_minutes=${PENDING_APPROVAL_TIMEOUT_MINUTES}"
  echo "pending_total=${pending_total}"
  echo "failed_total=${failed_total}"
  echo "retry_overdue_total=${retry_overdue_total}"
  echo "retry_now_total=${retry_now_total}"
  echo "adjust_pending_total=${adjust_pending_total}"
  echo "adjust_pending_overdue_total=${adjust_pending_overdue_total}"
  echo "transfer_pending_total=${transfer_pending_total}"
  echo "transfer_pending_overdue_total=${transfer_pending_overdue_total}"
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
echo "adjust_pending_total=${adjust_pending_total}"
echo "adjust_pending_overdue_total=${adjust_pending_overdue_total}"
echo "transfer_pending_total=${transfer_pending_total}"
echo "transfer_pending_overdue_total=${transfer_pending_overdue_total}"
echo "summary_file=${SUMMARY_FILE}"
echo "output_tsv=${OUTPUT_TSV}"

exit "${exit_code}"
