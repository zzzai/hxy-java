#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_NAME="${DB_NAME:-hxy_dev}"
REQUIRE_OVERDUE_ZERO="${REQUIRE_OVERDUE_ZERO:-1}"
REQUIRE_EXPIRE_ABNORMAL_ZERO="${REQUIRE_EXPIRE_ABNORMAL_ZERO:-${REQUIRE_OVERDUE_ZERO}}"
EXPIRE_ACTION_CODE="${EXPIRE_ACTION_CODE:-EXPIRE}"
EXPIRE_REMARK="${EXPIRE_REMARK:-SYSTEM_SLA_EXPIRED}"
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_TSV="${OUTPUT_TSV:-}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_store_lifecycle_change_order_gate.sh [options]

Options:
  --db-host <host>                      数据库地址（默认 localhost）
  --db-port <port>                      数据库端口（默认 3306）
  --db-user <user>                      数据库用户名（默认 root）
  --db-password <password>              数据库密码（默认空）
  --db-name <name>                      数据库名（默认 hxy_dev）
  --require-overdue-zero <0|1>          是否要求待审批超时单为 0（默认 1）
  --require-expire-abnormal-zero <0|1>  是否要求“未被 EXPIRE 任务收口”的超时单为 0（默认同 --require-overdue-zero）
  --expire-action-code <code>           过期任务动作码（默认 EXPIRE）
  --expire-remark <remark>              过期任务备注（默认 SYSTEM_SLA_EXPIRED）
  --summary-file <file>                 输出 summary（可选）
  --output-tsv <file>                   输出详情 TSV（可选）
  -h, --help                            显示帮助

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
    --require-expire-abnormal-zero)
      REQUIRE_EXPIRE_ABNORMAL_ZERO="$2"
      shift 2
      ;;
    --expire-action-code)
      EXPIRE_ACTION_CODE="$2"
      shift 2
      ;;
    --expire-remark)
      EXPIRE_REMARK="$2"
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
if ! [[ "${REQUIRE_EXPIRE_ABNORMAL_ZERO}" =~ ^[01]$ ]]; then
  echo "Invalid --require-expire-abnormal-zero: ${REQUIRE_EXPIRE_ABNORMAL_ZERO}" >&2
  exit 1
fi
if [[ -z "${EXPIRE_ACTION_CODE}" ]]; then
  echo "Invalid --expire-action-code: empty" >&2
  exit 1
fi
if [[ -z "${EXPIRE_REMARK}" ]]; then
  echo "Invalid --expire-remark: empty" >&2
  exit 1
fi

if ! command -v mysql >/dev/null 2>&1; then
  echo "mysql command not found" >&2
  exit 1
fi

if [[ -z "${SUMMARY_FILE}" ]]; then
  SUMMARY_FILE="/tmp/store_lifecycle_change_order_gate/summary.txt"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="/tmp/store_lifecycle_change_order_gate/result.tsv"
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
pending_overdue_total="0"
overdue_closed_total="0"
overdue_closed_by_expire_total="0"
overdue_closed_abnormal_total="0"

table_exists_sql="SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'hxy_store_lifecycle_change_order';"
table_exists="$(mysql "${MYSQL_ARGS[@]}" -e "${table_exists_sql}" 2>/dev/null || true)"
if [[ "${table_exists}" != "1" ]]; then
  if [[ "${REQUIRE_OVERDUE_ZERO}" == "1" || "${REQUIRE_EXPIRE_ABNORMAL_ZERO}" == "1" ]]; then
    mark_block
    add_issue "BLOCK" "LC01_TABLE_MISSING" "hxy_store_lifecycle_change_order table missing"
  else
    mark_warn
    add_issue "WARN" "LC01_TABLE_MISSING" "hxy_store_lifecycle_change_order table missing"
  fi
else
  COUNT_SQL="
SELECT
  SUM(CASE WHEN status = 10 THEN 1 ELSE 0 END) AS pending_total,
  SUM(CASE WHEN status = 10
            AND sla_deadline_time IS NOT NULL
            AND sla_deadline_time < NOW()
      THEN 1 ELSE 0 END) AS pending_overdue_total,
  SUM(CASE WHEN status = 40
            AND sla_deadline_time IS NOT NULL
            AND sla_deadline_time < NOW()
      THEN 1 ELSE 0 END) AS overdue_closed_total,
  SUM(CASE WHEN status = 40
            AND sla_deadline_time IS NOT NULL
            AND sla_deadline_time < NOW()
            AND UPPER(COALESCE(last_action_code, '')) = UPPER('${EXPIRE_ACTION_CODE}')
            AND COALESCE(approve_remark, '') = '${EXPIRE_REMARK}'
      THEN 1 ELSE 0 END) AS overdue_closed_by_expire_total,
  SUM(CASE WHEN status = 40
            AND sla_deadline_time IS NOT NULL
            AND sla_deadline_time < NOW()
            AND NOT (
              UPPER(COALESCE(last_action_code, '')) = UPPER('${EXPIRE_ACTION_CODE}')
              AND COALESCE(approve_remark, '') = '${EXPIRE_REMARK}'
            )
      THEN 1 ELSE 0 END) AS overdue_closed_abnormal_total
FROM hxy_store_lifecycle_change_order
WHERE deleted = b'0';
"
  query_line="$(mysql "${MYSQL_ARGS[@]}" -e "${COUNT_SQL}" 2>/dev/null || true)"
  if [[ -z "${query_line}" ]]; then
    mark_block
    add_issue "BLOCK" "LC02_QUERY_FAILED" "failed to query hxy_store_lifecycle_change_order"
  else
    pending_total="$(awk '{print $1}' <<<"${query_line}")"
    pending_overdue_total="$(awk '{print $2}' <<<"${query_line}")"
    overdue_closed_total="$(awk '{print $3}' <<<"${query_line}")"
    overdue_closed_by_expire_total="$(awk '{print $4}' <<<"${query_line}")"
    overdue_closed_abnormal_total="$(awk '{print $5}' <<<"${query_line}")"
    [[ -z "${pending_total}" || "${pending_total}" == "NULL" ]] && pending_total="0"
    [[ -z "${pending_overdue_total}" || "${pending_overdue_total}" == "NULL" ]] && pending_overdue_total="0"
    [[ -z "${overdue_closed_total}" || "${overdue_closed_total}" == "NULL" ]] && overdue_closed_total="0"
    [[ -z "${overdue_closed_by_expire_total}" || "${overdue_closed_by_expire_total}" == "NULL" ]] && overdue_closed_by_expire_total="0"
    [[ -z "${overdue_closed_abnormal_total}" || "${overdue_closed_abnormal_total}" == "NULL" ]] && overdue_closed_abnormal_total="0"

    if [[ "${REQUIRE_OVERDUE_ZERO}" == "1" && "${pending_overdue_total}" != "0" ]]; then
      mark_block
      add_issue "BLOCK" "LC03_PENDING_OVERDUE_EXISTS" "pending_overdue_total=${pending_overdue_total}"
    elif [[ "${pending_overdue_total}" != "0" ]]; then
      mark_warn
      add_issue "WARN" "LC04_PENDING_OVERDUE_WARN" "pending_overdue_total=${pending_overdue_total}"
    fi

    if [[ "${REQUIRE_EXPIRE_ABNORMAL_ZERO}" == "1" && "${overdue_closed_abnormal_total}" != "0" ]]; then
      mark_block
      add_issue "BLOCK" "LC05_EXPIRE_CLOSE_ABNORMAL" "overdue_closed_abnormal_total=${overdue_closed_abnormal_total}, expire_action_code=${EXPIRE_ACTION_CODE}, expire_remark=${EXPIRE_REMARK}"
    elif [[ "${overdue_closed_abnormal_total}" != "0" ]]; then
      mark_warn
      add_issue "WARN" "LC06_EXPIRE_CLOSE_ABNORMAL_WARN" "overdue_closed_abnormal_total=${overdue_closed_abnormal_total}, expire_action_code=${EXPIRE_ACTION_CODE}, expire_remark=${EXPIRE_REMARK}"
    fi

    if [[ "${pending_total}" != "0" && "${pending_overdue_total}" == "0" ]]; then
      mark_warn
      add_issue "WARN" "LC07_PENDING_WARN" "pending_total=${pending_total}, pending_overdue_total=${pending_overdue_total}"
    fi
  fi
fi

{
  echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "result=${result}"
  echo "require_overdue_zero=${REQUIRE_OVERDUE_ZERO}"
  echo "require_expire_abnormal_zero=${REQUIRE_EXPIRE_ABNORMAL_ZERO}"
  echo "expire_action_code=${EXPIRE_ACTION_CODE}"
  echo "expire_remark=${EXPIRE_REMARK}"
  echo "pending_total=${pending_total}"
  echo "pending_overdue_total=${pending_overdue_total}"
  echo "overdue_closed_total=${overdue_closed_total}"
  echo "overdue_closed_by_expire_total=${overdue_closed_by_expire_total}"
  echo "overdue_closed_abnormal_total=${overdue_closed_abnormal_total}"
  echo "db_host=${DB_HOST}"
  echo "db_port=${DB_PORT}"
  echo "db_name=${DB_NAME}"
  echo "output_tsv=${OUTPUT_TSV}"
} > "${SUMMARY_FILE}"

echo "== Store Lifecycle Change Order Gate =="
echo "result=${result}"
echo "pending_total=${pending_total}"
echo "pending_overdue_total=${pending_overdue_total}"
echo "overdue_closed_total=${overdue_closed_total}"
echo "overdue_closed_by_expire_total=${overdue_closed_by_expire_total}"
echo "overdue_closed_abnormal_total=${overdue_closed_abnormal_total}"
echo "summary_file=${SUMMARY_FILE}"
echo "output_tsv=${OUTPUT_TSV}"

exit "${exit_code}"
