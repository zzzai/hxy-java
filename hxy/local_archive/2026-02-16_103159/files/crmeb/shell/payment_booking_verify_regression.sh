#!/usr/bin/env bash
set -euo pipefail

# D25: 预约核销一致性回归检查
# 目标：在无真实订单阶段，持续发现“核销状态/会员卡扣减/核销流水”异常。

ORDER_NO="${ORDER_NO:-}"
WINDOW_HOURS="${WINDOW_HOURS:-72}"
OUTPUT_DIR="${OUTPUT_DIR:-}"
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
  ./shell/payment_booking_verify_regression.sh [--order-no ORDER_NO] [--window-hours N] [--output-dir PATH] [--no-alert]

参数：
  --order-no ORDER_NO   指定预约订单号或预约订单ID
  --window-hours N      检查窗口小时数（默认 72）
  --output-dir PATH     输出目录（默认 runtime/payment_booking_verify_regression）
  --no-alert            异常时不推送机器人

环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE
  ALERT_WEBHOOK_URL ALERT_WEBHOOK_TYPE

退出码：
  0  通过（无异常）
  2  存在异常（需处理）
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
  OUTPUT_DIR="${ROOT_DIR}/runtime/payment_booking_verify_regression"
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

run_sql_to_file() {
  local file="$1"
  local sql="$2"
  "${MYSQL_CMD[@]}" -e "${sql}" > "${file}"
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

FILE_VERIFIED_NO_CHECKIN="${RUN_DIR}/verified_without_checkin_time.tsv"
FILE_VERIFIED_LOCK_LEFT="${RUN_DIR}/verified_with_lock_left.tsv"
FILE_VERIFIED_NO_USAGE="${RUN_DIR}/verified_member_card_without_usage.tsv"
FILE_USAGE_NEGATIVE="${RUN_DIR}/usage_negative_values.tsv"
FILE_CARD_STATUS_CONFLICT="${RUN_DIR}/card_status_value_conflict.tsv"
FILE_DUP_USAGE="${RUN_DIR}/duplicate_usage_per_order.tsv"
SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"

run_sql_to_file "${FILE_VERIFIED_NO_CHECKIN}" "
SELECT bo.id, bo.order_no, bo.uid, bo.member_card_id, bo.status, bo.check_in_time, bo.updated_at
FROM eb_booking_order bo
WHERE bo.status = 3
  AND bo.updated_at >= ${START_TS}
  AND IFNULL(bo.check_in_time, 0) <= 0
  ${ORDER_FILTER}
ORDER BY bo.updated_at DESC;
"

run_sql_to_file "${FILE_VERIFIED_LOCK_LEFT}" "
SELECT bo.id, bo.order_no, bo.uid, bo.status, bo.locked_expire, bo.updated_at
FROM eb_booking_order bo
WHERE bo.status = 3
  AND bo.updated_at >= ${START_TS}
  AND IFNULL(bo.locked_expire, 0) > 0
  ${ORDER_FILTER}
ORDER BY bo.updated_at DESC;
"

run_sql_to_file "${FILE_VERIFIED_NO_USAGE}" "
SELECT bo.id, bo.order_no, bo.uid, bo.member_card_id, bo.status, bo.check_in_time
FROM eb_booking_order bo
WHERE bo.status = 3
  AND bo.updated_at >= ${START_TS}
  AND IFNULL(bo.member_card_id, 0) > 0
  AND NOT EXISTS (
    SELECT 1 FROM eb_member_card_usage u WHERE u.order_id = bo.id
  )
  ${ORDER_FILTER}
ORDER BY bo.updated_at DESC;
"

run_sql_to_file "${FILE_USAGE_NEGATIVE}" "
SELECT id, user_card_id, user_id, order_id, usage_type, used_times, used_amount,
       before_times, after_times, before_amount, after_amount, created_at
FROM eb_member_card_usage
WHERE created_at >= ${START_TS}
  AND (after_times < 0 OR after_amount < 0 OR used_times < 0 OR used_amount < 0)
ORDER BY created_at DESC;
"

run_sql_to_file "${FILE_CARD_STATUS_CONFLICT}" "
SELECT id, uid, card_type, status, remaining_value, expire_time, updated_at
FROM eb_member_card
WHERE
  (status = 3 AND remaining_value > 0)
  OR (status = 1 AND remaining_value <= 0)
ORDER BY updated_at DESC
LIMIT 200;
"

run_sql_to_file "${FILE_DUP_USAGE}" "
SELECT bo.order_no, u.order_id, COUNT(*) AS usage_count, MIN(u.created_at) AS first_created_at, MAX(u.created_at) AS last_created_at
FROM eb_member_card_usage u
JOIN eb_booking_order bo ON bo.id = u.order_id
WHERE u.created_at >= ${START_TS}
GROUP BY bo.order_no, u.order_id
HAVING COUNT(*) > 1
ORDER BY usage_count DESC, last_created_at DESC;
"

count_verified_no_checkin="$(count_tsv_rows "${FILE_VERIFIED_NO_CHECKIN}")"
count_verified_lock_left="$(count_tsv_rows "${FILE_VERIFIED_LOCK_LEFT}")"
count_verified_no_usage="$(count_tsv_rows "${FILE_VERIFIED_NO_USAGE}")"
count_usage_negative="$(count_tsv_rows "${FILE_USAGE_NEGATIVE}")"
count_card_conflict="$(count_tsv_rows "${FILE_CARD_STATUS_CONFLICT}")"
count_dup_usage="$(count_tsv_rows "${FILE_DUP_USAGE}")"

critical_count=$((count_verified_no_checkin + count_verified_lock_left + count_verified_no_usage + count_usage_negative))
warn_count=$((count_card_conflict + count_dup_usage))

exit_code=0
severity="PASS"
if (( critical_count > 0 )); then
  severity="ALERT"
  exit_code=2
elif (( warn_count > 0 )); then
  severity="WARN"
  exit_code=2
fi

cat > "${SUMMARY_FILE}" <<TXT
run_id=${RUN_ID}
run_time=$(date '+%Y-%m-%d %H:%M:%S')
window_hours=${WINDOW_HOURS}
start_ts=${START_TS}
order_no=${ORDER_NO}
severity=${severity}
critical_count=${critical_count}
warn_count=${warn_count}
verified_without_checkin_count=${count_verified_no_checkin}
verified_with_lock_left_count=${count_verified_lock_left}
verified_member_card_without_usage_count=${count_verified_no_usage}
usage_negative_values_count=${count_usage_negative}
card_status_value_conflict_count=${count_card_conflict}
duplicate_usage_per_order_count=${count_dup_usage}
run_dir=${RUN_DIR}
TXT

{
  echo "# 预约核销一致性回归报告"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`"
  echo "- window_hours: \`${WINDOW_HOURS}\`"
  echo "- start_ts: \`${START_TS}\`"
  echo "- order_no: \`${ORDER_NO:-<none>}\`"
  echo "- severity: **${severity}**"
  echo "- critical_count: \`${critical_count}\`"
  echo "- warn_count: \`${warn_count}\`"
  echo
  echo "## 一、关键异常（Critical）"
  echo
  echo "| 检查项 | 数量 | 文件 |"
  echo "|---|---:|---|"
  echo "| 已核销但 check_in_time 为空 | ${count_verified_no_checkin} | \`${FILE_VERIFIED_NO_CHECKIN}\` |"
  echo "| 已核销但 locked_expire 未清理 | ${count_verified_lock_left} | \`${FILE_VERIFIED_LOCK_LEFT}\` |"
  echo "| 绑卡订单已核销但无核销流水 | ${count_verified_no_usage} | \`${FILE_VERIFIED_NO_USAGE}\` |"
  echo "| 核销流水出现负值 | ${count_usage_negative} | \`${FILE_USAGE_NEGATIVE}\` |"
  echo
  echo "## 二、关注项（Warn）"
  echo
  echo "| 检查项 | 数量 | 文件 |"
  echo "|---|---:|---|"
  echo "| 会员卡状态与余额不一致 | ${count_card_conflict} | \`${FILE_CARD_STATUS_CONFLICT}\` |"
  echo "| 单预约订单多条核销流水 | ${count_dup_usage} | \`${FILE_DUP_USAGE}\` |"
  echo
  echo "## 三、追溯文件"
  echo
  echo "- summary: \`${SUMMARY_FILE}\`"
  echo "- report: \`${REPORT_FILE}\`"
} > "${REPORT_FILE}"

echo "[booking-verify-regression] run_dir=${RUN_DIR}"
echo "[booking-verify-regression] summary=${SUMMARY_FILE}"
echo "[booking-verify-regression] severity=${severity}, critical=${critical_count}, warn=${warn_count}"
echo "[booking-verify-regression] report=${REPORT_FILE}"

if (( exit_code != 0 && NO_ALERT == 0 )) && [[ -x "${ALERT_SCRIPT}" ]]; then
  "${ALERT_SCRIPT}" \
    --title "预约核销一致性告警" \
    --content "severity=${severity}; critical=${critical_count}; warn=${warn_count}; window_hours=${WINDOW_HOURS}; report=${REPORT_FILE}" || true
fi

exit "${exit_code}"
