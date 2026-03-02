#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_NAME="${DB_NAME:-hxy_dev}"
REQUIRE_OVERDUE_ZERO="${REQUIRE_OVERDUE_ZERO:-1}"
REQUIRE_NOTIFY_FAILED_ZERO="${REQUIRE_NOTIFY_FAILED_ZERO:-1}"
OVERDUE_BLOCK_THRESHOLD="${OVERDUE_BLOCK_THRESHOLD:-0}"
NOTIFY_FAILED_BLOCK_THRESHOLD="${NOTIFY_FAILED_BLOCK_THRESHOLD:-0}"
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_TSV="${OUTPUT_TSV:-}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_commission_settlement_gate.sh [options]

Options:
  --db-host <host>                         数据库地址（默认 localhost）
  --db-port <port>                         数据库端口（默认 3306）
  --db-user <user>                         数据库用户名（默认 root）
  --db-password <password>                 数据库密码（默认空）
  --db-name <name>                         数据库名（默认 hxy_dev）
  --require-overdue-zero <0|1>             是否要求待审核逾期为 0（默认 1）
  --require-notify-failed-zero <0|1>       是否要求通知失败为 0（默认 1）
  --overdue-block-threshold <n>            待审核逾期阻断阈值（默认 0）
  --notify-failed-block-threshold <n>      通知失败阻断阈值（默认 0）
  --summary-file <file>                    输出 summary（可选）
  --output-tsv <file>                      输出详情 TSV（可选）
  -h, --help                               显示帮助

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
    --require-notify-failed-zero)
      REQUIRE_NOTIFY_FAILED_ZERO="$2"
      shift 2
      ;;
    --overdue-block-threshold)
      OVERDUE_BLOCK_THRESHOLD="$2"
      shift 2
      ;;
    --notify-failed-block-threshold)
      NOTIFY_FAILED_BLOCK_THRESHOLD="$2"
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

for flag in "${REQUIRE_OVERDUE_ZERO}" "${REQUIRE_NOTIFY_FAILED_ZERO}"; do
  if ! [[ "${flag}" =~ ^[01]$ ]]; then
    echo "Invalid require flag: ${flag}" >&2
    exit 1
  fi
done
for threshold in "${OVERDUE_BLOCK_THRESHOLD}" "${NOTIFY_FAILED_BLOCK_THRESHOLD}"; do
  if ! [[ "${threshold}" =~ ^[0-9]+$ ]]; then
    echo "Invalid threshold: ${threshold}" >&2
    exit 1
  fi
done

if ! command -v mysql >/dev/null 2>&1; then
  echo "mysql command not found" >&2
  exit 1
fi

if [[ -z "${SUMMARY_FILE}" ]]; then
  SUMMARY_FILE="/tmp/commission_settlement_gate/summary.txt"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="/tmp/commission_settlement_gate/result.tsv"
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

SETTLEMENT_TABLE_EXISTS_SQL="SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'technician_commission_settlement';"
NOTIFY_OUTBOX_TABLE_EXISTS_SQL="SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'technician_commission_settlement_notify_outbox';"

settlement_table_exists="$(mysql "${MYSQL_ARGS[@]}" -e "${SETTLEMENT_TABLE_EXISTS_SQL}" 2>/dev/null || echo "")"
notify_outbox_table_exists="$(mysql "${MYSQL_ARGS[@]}" -e "${NOTIFY_OUTBOX_TABLE_EXISTS_SQL}" 2>/dev/null || echo "")"

result="PASS"
exit_code=0
pending_review_overdue_total="0"
pending_review_overdue_p0="0"
notify_failed_total="0"
notify_pending_overdue_total="0"
notify_retry_exhausted_total="0"

if [[ "${settlement_table_exists}" != "1" ]]; then
  if [[ "${REQUIRE_OVERDUE_ZERO}" == "1" ]]; then
    result="BLOCK"
    exit_code=2
    add_issue "BLOCK" "CS01_SETTLEMENT_TABLE_MISSING" "technician_commission_settlement table missing"
  else
    result="PASS_WITH_WARN"
    add_issue "WARN" "CS01_SETTLEMENT_TABLE_MISSING" "technician_commission_settlement table missing"
  fi
else
  SETTLEMENT_SQL="
SELECT
  SUM(CASE WHEN status = 10
            AND review_deadline_time IS NOT NULL
            AND review_deadline_time < NOW()
      THEN 1 ELSE 0 END) AS pending_review_overdue_total,
  SUM(CASE WHEN status = 10
            AND review_deadline_time IS NOT NULL
            AND review_deadline_time < NOW()
            AND review_escalated = b'1'
      THEN 1 ELSE 0 END) AS pending_review_overdue_p0
FROM technician_commission_settlement
WHERE deleted = b'0';
"
  settlement_line="$(mysql "${MYSQL_ARGS[@]}" -e "${SETTLEMENT_SQL}" 2>/dev/null || true)"
  if [[ -z "${settlement_line}" ]]; then
    result="BLOCK"
    exit_code=2
    add_issue "BLOCK" "CS02_SETTLEMENT_QUERY_FAILED" "failed to query technician_commission_settlement"
  else
    pending_review_overdue_total="$(awk '{print $1}' <<<"${settlement_line}")"
    pending_review_overdue_p0="$(awk '{print $2}' <<<"${settlement_line}")"
    [[ -z "${pending_review_overdue_total}" || "${pending_review_overdue_total}" == "NULL" ]] && pending_review_overdue_total="0"
    [[ -z "${pending_review_overdue_p0}" || "${pending_review_overdue_p0}" == "NULL" ]] && pending_review_overdue_p0="0"

    if (( pending_review_overdue_total > OVERDUE_BLOCK_THRESHOLD )); then
      if [[ "${REQUIRE_OVERDUE_ZERO}" == "1" ]]; then
        result="BLOCK"
        exit_code=2
        add_issue "BLOCK" "CS03_REVIEW_OVERDUE_BLOCK" "pending_review_overdue_total=${pending_review_overdue_total}, threshold=${OVERDUE_BLOCK_THRESHOLD}"
      else
        result="PASS_WITH_WARN"
        add_issue "WARN" "CS04_REVIEW_OVERDUE_WARN" "pending_review_overdue_total=${pending_review_overdue_total}, threshold=${OVERDUE_BLOCK_THRESHOLD}"
      fi
    fi
  fi
fi

if [[ "${notify_outbox_table_exists}" != "1" ]]; then
  if [[ "${REQUIRE_NOTIFY_FAILED_ZERO}" == "1" ]]; then
    result="BLOCK"
    exit_code=2
    add_issue "BLOCK" "CS05_NOTIFY_OUTBOX_TABLE_MISSING" "technician_commission_settlement_notify_outbox table missing"
  else
    result="PASS_WITH_WARN"
    add_issue "WARN" "CS05_NOTIFY_OUTBOX_TABLE_MISSING" "technician_commission_settlement_notify_outbox table missing"
  fi
else
  NOTIFY_SQL="
SELECT
  SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS notify_failed_total,
  SUM(CASE WHEN status = 0
            AND next_retry_time IS NOT NULL
            AND next_retry_time < NOW()
      THEN 1 ELSE 0 END) AS notify_pending_overdue_total,
  SUM(CASE WHEN status = 2
            AND retry_count >= 5
      THEN 1 ELSE 0 END) AS notify_retry_exhausted_total
FROM technician_commission_settlement_notify_outbox
WHERE deleted = b'0';
"
  notify_line="$(mysql "${MYSQL_ARGS[@]}" -e "${NOTIFY_SQL}" 2>/dev/null || true)"
  if [[ -z "${notify_line}" ]]; then
    result="BLOCK"
    exit_code=2
    add_issue "BLOCK" "CS06_NOTIFY_OUTBOX_QUERY_FAILED" "failed to query technician_commission_settlement_notify_outbox"
  else
    notify_failed_total="$(awk '{print $1}' <<<"${notify_line}")"
    notify_pending_overdue_total="$(awk '{print $2}' <<<"${notify_line}")"
    notify_retry_exhausted_total="$(awk '{print $3}' <<<"${notify_line}")"
    [[ -z "${notify_failed_total}" || "${notify_failed_total}" == "NULL" ]] && notify_failed_total="0"
    [[ -z "${notify_pending_overdue_total}" || "${notify_pending_overdue_total}" == "NULL" ]] && notify_pending_overdue_total="0"
    [[ -z "${notify_retry_exhausted_total}" || "${notify_retry_exhausted_total}" == "NULL" ]] && notify_retry_exhausted_total="0"

    if (( notify_failed_total > NOTIFY_FAILED_BLOCK_THRESHOLD )); then
      if [[ "${REQUIRE_NOTIFY_FAILED_ZERO}" == "1" ]]; then
        result="BLOCK"
        exit_code=2
        add_issue "BLOCK" "CS07_NOTIFY_FAILED_BLOCK" "notify_failed_total=${notify_failed_total}, threshold=${NOTIFY_FAILED_BLOCK_THRESHOLD}"
      else
        result="PASS_WITH_WARN"
        add_issue "WARN" "CS08_NOTIFY_FAILED_WARN" "notify_failed_total=${notify_failed_total}, threshold=${NOTIFY_FAILED_BLOCK_THRESHOLD}"
      fi
    fi

    if [[ "${notify_pending_overdue_total}" != "0" ]]; then
      result="PASS_WITH_WARN"
      add_issue "WARN" "CS09_NOTIFY_PENDING_OVERDUE_WARN" "notify_pending_overdue_total=${notify_pending_overdue_total}"
    fi
    if [[ "${notify_retry_exhausted_total}" != "0" ]]; then
      result="PASS_WITH_WARN"
      add_issue "WARN" "CS10_NOTIFY_RETRY_EXHAUSTED_WARN" "notify_retry_exhausted_total=${notify_retry_exhausted_total}"
    fi
  fi
fi

{
  echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "result=${result}"
  echo "require_overdue_zero=${REQUIRE_OVERDUE_ZERO}"
  echo "require_notify_failed_zero=${REQUIRE_NOTIFY_FAILED_ZERO}"
  echo "overdue_block_threshold=${OVERDUE_BLOCK_THRESHOLD}"
  echo "notify_failed_block_threshold=${NOTIFY_FAILED_BLOCK_THRESHOLD}"
  echo "pending_review_overdue_total=${pending_review_overdue_total}"
  echo "pending_review_overdue_p0=${pending_review_overdue_p0}"
  echo "notify_failed_total=${notify_failed_total}"
  echo "notify_pending_overdue_total=${notify_pending_overdue_total}"
  echo "notify_retry_exhausted_total=${notify_retry_exhausted_total}"
  echo "db_host=${DB_HOST}"
  echo "db_port=${DB_PORT}"
  echo "db_name=${DB_NAME}"
  echo "output_tsv=${OUTPUT_TSV}"
} > "${SUMMARY_FILE}"

echo "== Commission Settlement Gate =="
echo "result=${result}"
echo "pending_review_overdue_total=${pending_review_overdue_total}"
echo "pending_review_overdue_p0=${pending_review_overdue_p0}"
echo "notify_failed_total=${notify_failed_total}"
echo "notify_pending_overdue_total=${notify_pending_overdue_total}"
echo "notify_retry_exhausted_total=${notify_retry_exhausted_total}"
echo "summary_file=${SUMMARY_FILE}"
echo "output_tsv=${OUTPUT_TSV}"

exit "${exit_code}"
