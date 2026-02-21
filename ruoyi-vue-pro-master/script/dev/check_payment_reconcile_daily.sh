#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MYSQL_BIN="${MYSQL_BIN:-mysql}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

DB_HOST="${DB_HOST:-}"
DB_PORT="${DB_PORT:-}"
DB_USER="${DB_USER:-}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_NAME="${DB_NAME:-ruoyi-vue-pro}"
BIZ_DATE="${BIZ_DATE:-$(date -d 'yesterday' +%F)}"
STALE_MINUTES="${STALE_MINUTES:-10}"
ISSUES_TSV=""
SUMMARY_FILE=""

SQL_FILE="${ROOT_DIR}/script/sql/payment_reconcile_daily.sql"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_payment_reconcile_daily.sh [options]

Options:
  --biz-date <yyyy-mm-dd>      对账日期（默认昨天）
  --stale-minutes <minutes>    任务卡滞判定阈值（默认 10）
  --db-host <host>             MySQL host (default: 127.0.0.1)
  --db-port <port>             MySQL port (default: 3306)
  --db-user <user>             MySQL user (default: root)
  --db-password <password>     MySQL password (default: env DB_PASSWORD)
  --mysql-defaults-file <path> MySQL defaults file (optional, e.g. /root/.my.cnf)
  --db-name <name>             Database name (default: ruoyi-vue-pro)
  --issues-tsv <file>          输出问题明细 TSV（可选）
  --summary-file <file>        输出 summary.txt（可选）
  -h, --help                   Show help

Exit Code:
  0  : 对账通过（无 BLOCK，允许 WARN）
  2  : 对账阻断（存在 BLOCK）
  1+ : 参数或执行错误
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --biz-date)
      BIZ_DATE="$2"
      shift 2
      ;;
    --stale-minutes)
      STALE_MINUTES="$2"
      shift 2
      ;;
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
    --mysql-defaults-file)
      MYSQL_DEFAULTS_FILE="$2"
      shift 2
      ;;
    --db-name)
      DB_NAME="$2"
      shift 2
      ;;
    --issues-tsv)
      ISSUES_TSV="$2"
      shift 2
      ;;
    --summary-file)
      SUMMARY_FILE="$2"
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

if [[ ! "${BIZ_DATE}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "Invalid --biz-date: ${BIZ_DATE}" >&2
  exit 1
fi
if ! date -d "${BIZ_DATE}" '+%F' >/dev/null 2>&1; then
  echo "Invalid date value: ${BIZ_DATE}" >&2
  exit 1
fi
if ! [[ "${STALE_MINUTES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "Invalid --stale-minutes: ${STALE_MINUTES}" >&2
  exit 1
fi
if [[ -n "${MYSQL_DEFAULTS_FILE}" && ! -f "${MYSQL_DEFAULTS_FILE}" ]]; then
  echo "Invalid --mysql-defaults-file (not found): ${MYSQL_DEFAULTS_FILE}" >&2
  exit 1
fi
if [[ ! -f "${SQL_FILE}" ]]; then
  echo "Missing SQL file: ${SQL_FILE}" >&2
  exit 1
fi

if [[ -n "${MYSQL_DEFAULTS_FILE}" ]]; then
  : "${DB_HOST:=}"
  : "${DB_PORT:=}"
  : "${DB_USER:=}"
else
  : "${DB_HOST:=127.0.0.1}"
  : "${DB_PORT:=3306}"
  : "${DB_USER:=root}"
fi

MYSQL_ARGS=(--batch --raw --skip-column-names)
if [[ -n "${MYSQL_DEFAULTS_FILE}" ]]; then
  MYSQL_ARGS=(--defaults-file="${MYSQL_DEFAULTS_FILE}" "${MYSQL_ARGS[@]}")
fi
if [[ -n "${DB_HOST}" ]]; then
  MYSQL_ARGS+=(--protocol=TCP -h"${DB_HOST}")
fi
if [[ -n "${DB_PORT}" ]]; then
  MYSQL_ARGS+=(-P"${DB_PORT}")
fi
if [[ -n "${DB_USER}" ]]; then
  MYSQL_ARGS+=(-u"${DB_USER}")
fi
if [[ -n "${DB_PASSWORD}" ]]; then
  MYSQL_ARGS+=(-p"${DB_PASSWORD}")
fi

if [[ -n "${ISSUES_TSV}" ]]; then
  mkdir -p "$(dirname "${ISSUES_TSV}")"
fi
if [[ -n "${SUMMARY_FILE}" ]]; then
  mkdir -p "$(dirname "${SUMMARY_FILE}")"
fi

required_tables=(pay_order pay_order_extension pay_refund pay_notify_task)
missing_tables=()
for table in "${required_tables[@]}"; do
  exists="$("${MYSQL_BIN}" "${MYSQL_ARGS[@]}" information_schema -e "SELECT COUNT(*) FROM tables WHERE table_schema='${DB_NAME}' AND table_name='${table}';" 2>/dev/null | tail -n 1 || echo "0")"
  if [[ "${exists}" != "1" ]]; then
    missing_tables+=("${table}")
  fi
done
if [[ "${#missing_tables[@]}" -gt 0 ]]; then
  echo "Missing required tables in ${DB_NAME}: ${missing_tables[*]}" >&2
  echo "Hint: initialize payment schema first (e.g. sql/mysql/ruoyi-modules-member-pay-mall.sql)." >&2
  exit 1
fi

ISSUES="$("${MYSQL_BIN}" "${MYSQL_ARGS[@]}" "${DB_NAME}" <<SQL
SET @window_start = '${BIZ_DATE} 00:00:00';
SET @window_end = DATE_ADD(@window_start, INTERVAL 1 DAY);
SET @stale_minutes = ${STALE_MINUTES};
SOURCE ${SQL_FILE};
SQL
)"

HEADER=$'severity\tissue_type\tentity_type\tbiz_key\tcode\tdetail\texpected_amount\tactual_amount\toccurred_at'

if [[ -n "${ISSUES_TSV}" ]]; then
  {
    printf '%s\n' "${HEADER}"
    if [[ -n "${ISSUES}" ]]; then
      printf '%s\n' "${ISSUES}"
    fi
  } > "${ISSUES_TSV}"
fi

BLOCK_COUNT="$(printf '%s\n' "${ISSUES}" | awk -F'\t' '$1=="BLOCK"{c++} END{print c+0}')"
WARN_COUNT="$(printf '%s\n' "${ISSUES}" | awk -F'\t' '$1=="WARN"{c++} END{print c+0}')"
TOTAL_COUNT="$(printf '%s\n' "${ISSUES}" | awk 'NF>0{c++} END{print c+0}')"

RESULT="PASS"
EXIT_CODE=0
if [[ "${BLOCK_COUNT}" -gt 0 ]]; then
  RESULT="BLOCK"
  EXIT_CODE=2
elif [[ "${WARN_COUNT}" -gt 0 ]]; then
  RESULT="PASS_WITH_WARN"
fi

echo "== Payment Reconcile Daily =="
if [[ -n "${DB_HOST}" && -n "${DB_PORT}" ]]; then
  echo "db=${DB_NAME}@${DB_HOST}:${DB_PORT}"
else
  echo "db=${DB_NAME} (connection from mysql defaults file)"
fi
echo "biz_date=${BIZ_DATE}"
echo "stale_minutes=${STALE_MINUTES}"
echo
printf '%s\n' "${HEADER}"
if [[ -n "${ISSUES}" ]]; then
  printf '%s\n' "${ISSUES}"
fi
echo
echo "Summary: TOTAL=${TOTAL_COUNT}, BLOCK=${BLOCK_COUNT}, WARN=${WARN_COUNT}"
echo "Result: ${RESULT}"

if [[ -n "${SUMMARY_FILE}" ]]; then
  {
    echo "generated_at=$(date '+%Y-%m-%d %H:%M:%S')"
    echo "biz_date=${BIZ_DATE}"
    echo "window_start=${BIZ_DATE} 00:00:00"
    echo "window_end=$(date -d "${BIZ_DATE} +1 day" '+%F') 00:00:00"
    echo "stale_minutes=${STALE_MINUTES}"
    echo "db_host=${DB_HOST}"
    echo "db_port=${DB_PORT}"
    echo "db_name=${DB_NAME}"
    echo "mysql_defaults_file=${MYSQL_DEFAULTS_FILE}"
    echo "issue_total=${TOTAL_COUNT}"
    echo "issue_block_count=${BLOCK_COUNT}"
    echo "issue_warn_count=${WARN_COUNT}"
    echo "reconcile_result=${RESULT}"
    echo "exit_code=${EXIT_CODE}"
    if [[ -n "${ISSUES_TSV}" ]]; then
      echo "issues_tsv=${ISSUES_TSV}"
    fi
  } > "${SUMMARY_FILE}"
fi

exit "${EXIT_CODE}"
