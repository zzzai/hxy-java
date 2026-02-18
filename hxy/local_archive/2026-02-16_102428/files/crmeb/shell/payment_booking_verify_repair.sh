#!/usr/bin/env bash
set -euo pipefail

# D26: 预约核销异常自动修复（仅修复安全字段）
# 修复范围：
# 1) 已核销订单 check_in_time 为空 -> 回填为 updated_at 或当前时间
# 2) 已核销订单 locked_expire 未清理 -> 置 0

ORDER_NO="${ORDER_NO:-}"
WINDOW_HOURS="${WINDOW_HOURS:-72}"
OUTPUT_DIR="${OUTPUT_DIR:-}"
APPLY=0
NO_ALERT=0

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_booking_verify_repair.sh [--order-no ORDER_NO] [--window-hours N] [--output-dir PATH] [--apply] [--no-alert]

参数：
  --order-no ORDER_NO   指定预约订单号或预约订单ID
  --window-hours N      检查窗口小时数（默认 72，指定 order-no 时忽略窗口过滤）
  --output-dir PATH     输出目录（默认 runtime/payment_booking_verify_repair）
  --apply               真正执行修复（默认 dry-run）
  --no-alert            异常时不推送机器人

环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE
  ALERT_WEBHOOK_URL ALERT_WEBHOOK_TYPE

退出码：
  0  无需修复或修复完成
  2  存在待修复项（dry-run）或 apply 后仍有残留
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --order-no)
      ORDER_NO="$2"
      shift 2
      ;;
    --window-hours)
      WINDOW_HOURS="$2"
      shift 2
      ;;
    --output-dir)
      OUTPUT_DIR="$2"
      shift 2
      ;;
    --apply)
      APPLY=1
      shift
      ;;
    --no-alert)
      NO_ALERT=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "未知参数: $1"
      usage
      exit 1
      ;;
  esac
done

if ! [[ "${WINDOW_HOURS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --window-hours 必须是正整数"
  exit 1
fi
if [[ -n "${MYSQL_DEFAULTS_FILE}" && ! -f "${MYSQL_DEFAULTS_FILE}" ]]; then
  echo "参数错误: MYSQL_DEFAULTS_FILE 文件不存在 -> ${MYSQL_DEFAULTS_FILE}"
  exit 1
fi
if ! command -v mysql >/dev/null 2>&1; then
  echo "未找到 mysql 客户端"
  exit 1
fi

if [[ -z "${OUTPUT_DIR}" ]]; then
  OUTPUT_DIR="${ROOT_DIR}/runtime/payment_booking_verify_repair"
fi
RUN_ID="$(date '+%Y%m%d%H%M%S')"
RUN_DIR="${OUTPUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"

START_TS="$(date -d "-${WINDOW_HOURS} hours" +%s)"

MYSQL_CMD=(mysql)
if [[ -n "${MYSQL_DEFAULTS_FILE}" ]]; then
  MYSQL_CMD+=(--defaults-extra-file="${MYSQL_DEFAULTS_FILE}")
  if [[ "${DB_USER}" == "root" && -z "${DB_PASS}" ]]; then
    DB_USER=""
  fi
fi
MYSQL_CMD+=(-h "${DB_HOST}" -P "${DB_PORT}")
if [[ -n "${DB_USER}" ]]; then
  MYSQL_CMD+=(-u "${DB_USER}")
fi
MYSQL_CMD+=("${DB_NAME}" --default-character-set=utf8mb4 --batch --raw)
if [[ -n "${DB_PASS}" && -z "${MYSQL_DEFAULTS_FILE}" ]]; then
  MYSQL_CMD+=(--password="${DB_PASS}")
fi

sql_escape() {
  printf "%s" "$1" | sed "s/'/''/g"
}

ORDER_FILTER=""
if [[ -n "${ORDER_NO}" ]]; then
  ORDER_ESC="$(sql_escape "${ORDER_NO}")"
  ORDER_FILTER=" AND (bo.order_no='${ORDER_ESC}' OR bo.id='${ORDER_ESC}')"
fi

TIME_FILTER=""
if [[ -z "${ORDER_NO}" ]]; then
  TIME_FILTER=" AND bo.updated_at >= ${START_TS}"
fi

run_sql_to_file() {
  local file="$1"
  local sql="$2"
  "${MYSQL_CMD[@]}" -e "${sql}" > "${file}"
}

run_sql_scalar() {
  local sql="$1"
  "${MYSQL_CMD[@]}" --skip-column-names -e "${sql}" | head -n 1
}

count_tsv_rows() {
  local file="$1"
  local lines
  lines="$(wc -l < "${file}" | tr -d ' ')"
  if [[ -z "${lines}" ]]; then
    echo 0
    return
  fi
  if (( lines > 0 )); then
    echo $((lines - 1))
  else
    echo 0
  fi
}

FILE_BEFORE_CHECKIN="${RUN_DIR}/before_missing_check_in_time.tsv"
FILE_BEFORE_LOCK="${RUN_DIR}/before_locked_expire_left.tsv"
FILE_AFTER_CHECKIN="${RUN_DIR}/after_missing_check_in_time.tsv"
FILE_AFTER_LOCK="${RUN_DIR}/after_locked_expire_left.tsv"
SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"

query_missing_checkin() {
  cat <<SQL
SELECT bo.id, bo.order_no, bo.uid, bo.status, bo.check_in_time, bo.updated_at
FROM eb_booking_order bo
WHERE bo.status = 3
  AND IFNULL(bo.check_in_time, 0) <= 0
  ${TIME_FILTER}
  ${ORDER_FILTER}
ORDER BY bo.updated_at DESC;
SQL
}

query_locked_expire_left() {
  cat <<SQL
SELECT bo.id, bo.order_no, bo.uid, bo.status, bo.locked_expire, bo.updated_at
FROM eb_booking_order bo
WHERE bo.status = 3
  AND IFNULL(bo.locked_expire, 0) > 0
  ${TIME_FILTER}
  ${ORDER_FILTER}
ORDER BY bo.updated_at DESC;
SQL
}

run_sql_to_file "${FILE_BEFORE_CHECKIN}" "$(query_missing_checkin)"
run_sql_to_file "${FILE_BEFORE_LOCK}" "$(query_locked_expire_left)"

before_missing_checkin_count="$(count_tsv_rows "${FILE_BEFORE_CHECKIN}")"
before_locked_expire_count="$(count_tsv_rows "${FILE_BEFORE_LOCK}")"
before_total=$((before_missing_checkin_count + before_locked_expire_count))

updated_checkin=0
updated_lock=0
if (( APPLY == 1 )); then
  updated_checkin="$(run_sql_scalar "
UPDATE eb_booking_order bo
SET bo.check_in_time = CASE WHEN IFNULL(bo.updated_at, 0) > 0 THEN bo.updated_at ELSE UNIX_TIMESTAMP() END,
    bo.updated_at = UNIX_TIMESTAMP()
WHERE bo.status = 3
  AND IFNULL(bo.check_in_time, 0) <= 0
  ${TIME_FILTER}
  ${ORDER_FILTER};
SELECT ROW_COUNT();
")"
  updated_lock="$(run_sql_scalar "
UPDATE eb_booking_order bo
SET bo.locked_expire = 0,
    bo.updated_at = UNIX_TIMESTAMP()
WHERE bo.status = 3
  AND IFNULL(bo.locked_expire, 0) > 0
  ${TIME_FILTER}
  ${ORDER_FILTER};
SELECT ROW_COUNT();
")"
fi

run_sql_to_file "${FILE_AFTER_CHECKIN}" "$(query_missing_checkin)"
run_sql_to_file "${FILE_AFTER_LOCK}" "$(query_locked_expire_left)"

after_missing_checkin_count="$(count_tsv_rows "${FILE_AFTER_CHECKIN}")"
after_locked_expire_count="$(count_tsv_rows "${FILE_AFTER_LOCK}")"
after_total=$((after_missing_checkin_count + after_locked_expire_count))

severity="PASS"
exit_code=0
if (( APPLY == 0 )); then
  if (( before_total > 0 )); then
    severity="PENDING"
    exit_code=2
  fi
else
  if (( after_total > 0 )); then
    severity="ALERT"
    exit_code=2
  fi
fi

cat > "${SUMMARY_FILE}" <<TXT
run_id=${RUN_ID}
run_time=$(date '+%Y-%m-%d %H:%M:%S')
window_hours=${WINDOW_HOURS}
start_ts=${START_TS}
order_no=${ORDER_NO}
mode=$([[ ${APPLY} -eq 1 ]] && echo apply || echo dry-run)
severity=${severity}
before_missing_check_in_time_count=${before_missing_checkin_count}
before_locked_expire_left_count=${before_locked_expire_count}
before_total=${before_total}
updated_check_in_time_count=${updated_checkin}
updated_locked_expire_count=${updated_lock}
after_missing_check_in_time_count=${after_missing_checkin_count}
after_locked_expire_left_count=${after_locked_expire_count}
after_total=${after_total}
run_dir=${RUN_DIR}
TXT

{
  echo "# 预约核销异常修复报告"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`"
  echo "- mode: \`$([[ ${APPLY} -eq 1 ]] && echo apply || echo dry-run)\`"
  echo "- window_hours: \`${WINDOW_HOURS}\`"
  echo "- order_no: \`${ORDER_NO:-<none>}\`"
  echo "- severity: **${severity}**"
  echo
  echo "## 一、修复项统计"
  echo
  echo "| 修复项 | 修复前 | 修复后 | apply 更新量 |"
  echo "|---|---:|---:|---:|"
  echo "| 已核销但 check_in_time 为空 | ${before_missing_checkin_count} | ${after_missing_checkin_count} | ${updated_checkin} |"
  echo "| 已核销但 locked_expire 未清理 | ${before_locked_expire_count} | ${after_locked_expire_count} | ${updated_lock} |"
  echo
  echo "## 二、说明"
  echo
  echo "- 本脚本仅修复非资金字段：\`check_in_time\` 和 \`locked_expire\`。"
  echo "- 会员卡核销流水缺失、余额冲突等资金相关异常，不在自动修复范围。"
  echo
  echo "## 三、追溯文件"
  echo
  echo "- summary: \`${SUMMARY_FILE}\`"
  echo "- before_missing_check_in_time: \`${FILE_BEFORE_CHECKIN}\`"
  echo "- before_locked_expire_left: \`${FILE_BEFORE_LOCK}\`"
  echo "- after_missing_check_in_time: \`${FILE_AFTER_CHECKIN}\`"
  echo "- after_locked_expire_left: \`${FILE_AFTER_LOCK}\`"
} > "${REPORT_FILE}"

echo "[booking-verify-repair] run_dir=${RUN_DIR}"
echo "[booking-verify-repair] summary=${SUMMARY_FILE}"
echo "[booking-verify-repair] severity=${severity}, before_total=${before_total}, after_total=${after_total}"
echo "[booking-verify-repair] report=${REPORT_FILE}"

if (( exit_code != 0 && NO_ALERT == 0 )) && [[ -x "${ALERT_SCRIPT}" ]]; then
  "${ALERT_SCRIPT}" \
    --title "预约核销异常待修复" \
    --content "severity=${severity}; mode=$([[ ${APPLY} -eq 1 ]] && echo apply || echo dry-run); before_total=${before_total}; after_total=${after_total}; report=${REPORT_FILE}" || true
fi

exit "${exit_code}"
