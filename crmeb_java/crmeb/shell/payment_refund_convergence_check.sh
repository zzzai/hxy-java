#!/usr/bin/env bash
set -euo pipefail

# 退款闭环巡检：
# 目标：识别“申请中/退款中长时间不收敛”和“退款终态账务不一致”。

WINDOW_HOURS="${WINDOW_HOURS:-72}"
REFUND_TIMEOUT_MINUTES="${REFUND_TIMEOUT_MINUTES:-30}"
ORDER_NO="${ORDER_NO:-}"
OUT_DIR="${OUT_DIR:-}"
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
  ./shell/payment_refund_convergence_check.sh [--window-hours N] [--refund-timeout-minutes N]
    [--order-no ORDER_NO] [--out-dir PATH] [--no-alert]

参数：
  --window-hours N                 巡检窗口（默认 72 小时）
  --refund-timeout-minutes N       退款超时阈值（默认 30 分钟）
  --order-no ORDER_NO              指定单号（order_id / out_trade_no）
  --out-dir PATH                   输出目录（默认 runtime/payment_refund_convergence）
  --no-alert                       不推送告警

环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE

退出码：
  0  通过（GREEN / GREEN_WITH_WARN）
  2  不通过（RED）
  1  执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --window-hours)
      WINDOW_HOURS="$2"
      shift 2
      ;;
    --refund-timeout-minutes)
      REFUND_TIMEOUT_MINUTES="$2"
      shift 2
      ;;
    --order-no)
      ORDER_NO="$2"
      shift 2
      ;;
    --out-dir)
      OUT_DIR="$2"
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
if ! [[ "${REFUND_TIMEOUT_MINUTES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --refund-timeout-minutes 必须是正整数"
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

if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${ROOT_DIR}/runtime/payment_refund_convergence"
fi
RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"
SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"

START_TIME="$(date -d "-${WINDOW_HOURS} hours" '+%F %T')"
REFUND_TIMEOUT_BEFORE="$(date -d "-${REFUND_TIMEOUT_MINUTES} minutes" '+%F %T')"

MYSQL_CMD=(mysql)
if [[ -n "${MYSQL_DEFAULTS_FILE}" ]]; then
  MYSQL_CMD+=(--defaults-extra-file="${MYSQL_DEFAULTS_FILE}")
else
  MYSQL_CMD+=(-h "${DB_HOST}" -P "${DB_PORT}")
  if [[ -n "${DB_USER}" ]]; then
    MYSQL_CMD+=(-u "${DB_USER}")
  fi
  if [[ -n "${DB_PASS}" ]]; then
    MYSQL_CMD+=(--password="${DB_PASS}")
  fi
fi
MYSQL_CMD+=("${DB_NAME}" --default-character-set=utf8mb4 --batch --raw)

sql_escape() {
  printf '%s' "$1" | sed "s/'/''/g"
}

ORDER_FILTER=""
if [[ -n "${ORDER_NO}" ]]; then
  ORDER_ESC="$(sql_escape "${ORDER_NO}")"
  ORDER_FILTER=" AND (so.order_id='${ORDER_ESC}' OR so.out_trade_no='${ORDER_ESC}')"
fi

run_sql_to_file() {
  local file="$1"
  local sql="$2"
  "${MYSQL_CMD[@]}" -e "${sql}" > "${file}"
}

count_tsv_rows() {
  local file="$1"
  if [[ ! -f "${file}" ]]; then
    echo 0
    return
  fi
  tail -n +2 "${file}" | awk 'NF > 0 {c++} END {print c+0}'
}

declare -a CHECK_RESULTS=()
BLOCK_CHECK_COUNT=0
WARN_CHECK_COUNT=0
BLOCK_ROW_COUNT=0
WARN_ROW_COUNT=0

record_check() {
  local check_id="$1"
  local severity="$2"
  local title="$3"
  local file="$4"
  local rows="$5"
  local status="PASS"
  if (( rows > 0 )); then
    status="HIT"
    if [[ "${severity}" == "BLOCK" ]]; then
      BLOCK_CHECK_COUNT=$((BLOCK_CHECK_COUNT + 1))
      BLOCK_ROW_COUNT=$((BLOCK_ROW_COUNT + rows))
    else
      WARN_CHECK_COUNT=$((WARN_CHECK_COUNT + 1))
      WARN_ROW_COUNT=$((WARN_ROW_COUNT + rows))
    fi
  fi
  CHECK_RESULTS+=("${check_id}|${severity}|${status}|${rows}|${title}|${file}")
}

# R01: 退款申请中/退款中超时未收敛（阻断）
FILE_01="${RUN_DIR}/01_refund_stuck_timeout.tsv"
run_sql_to_file "${FILE_01}" "
SELECT so.order_id, so.out_trade_no, so.refund_status, so.refund_price, so.update_time
FROM eb_store_order so
WHERE so.pay_type='weixin'
  AND so.paid=1
  AND so.refund_status IN (1, 3)
  AND so.update_time <= '${REFUND_TIMEOUT_BEFORE}'
  ${ORDER_FILTER}
ORDER BY so.update_time ASC;
"
record_check "R01" "BLOCK" "退款超时未收敛（状态=申请中/退款中）" "${FILE_01}" "$(count_tsv_rows "${FILE_01}")"

# R02: 退款成功但订单不是已支付（阻断）
FILE_02="${RUN_DIR}/02_refund_success_but_unpaid.tsv"
run_sql_to_file "${FILE_02}" "
SELECT so.order_id, so.out_trade_no, so.paid, so.refund_status, so.update_time
FROM eb_store_order so
WHERE so.refund_status=2
  AND so.paid=0
  AND so.update_time >= '${START_TIME}'
  ${ORDER_FILTER}
ORDER BY so.update_time DESC;
"
record_check "R02" "BLOCK" "退款成功但订单非已支付" "${FILE_02}" "$(count_tsv_rows "${FILE_02}")"

# R03: 退款成功但缺少退款日志（告警）
FILE_03="${RUN_DIR}/03_refund_success_without_status_log.tsv"
run_sql_to_file "${FILE_03}" "
SELECT so.order_id, so.out_trade_no, so.refund_status, so.update_time
FROM eb_store_order so
WHERE so.refund_status=2
  AND so.update_time >= '${START_TIME}'
  ${ORDER_FILTER}
  AND NOT EXISTS (
    SELECT 1 FROM eb_store_order_status s
    WHERE s.oid = so.id
      AND s.change_type='refund_price'
  )
ORDER BY so.update_time DESC;
"
record_check "R03" "WARN" "退款成功但缺少 refund_price 日志" "${FILE_03}" "$(count_tsv_rows "${FILE_03}")"

# R04: 退款成功但退款金额异常（告警）
FILE_04="${RUN_DIR}/04_refund_success_invalid_amount.tsv"
run_sql_to_file "${FILE_04}" "
SELECT so.order_id, so.out_trade_no, so.refund_price, so.pay_price, so.update_time
FROM eb_store_order so
WHERE so.refund_status=2
  AND so.update_time >= '${START_TIME}'
  ${ORDER_FILTER}
  AND (so.refund_price IS NULL OR so.refund_price <= 0 OR so.refund_price > so.pay_price)
ORDER BY so.update_time DESC;
"
record_check "R04" "WARN" "退款成功但退款金额异常（<=0 或 > pay_price）" "${FILE_04}" "$(count_tsv_rows "${FILE_04}")"

GATE_RESULT="GREEN"
EXIT_CODE=0
if (( BLOCK_CHECK_COUNT > 0 )); then
  GATE_RESULT="RED"
  EXIT_CODE=2
elif (( WARN_CHECK_COUNT > 0 )); then
  GATE_RESULT="GREEN_WITH_WARN"
fi

{
  echo "gate_result=${GATE_RESULT}"
  echo "window_hours=${WINDOW_HOURS}"
  echo "refund_timeout_minutes=${REFUND_TIMEOUT_MINUTES}"
  echo "order_no=${ORDER_NO}"
  echo "block_check_count=${BLOCK_CHECK_COUNT}"
  echo "warn_check_count=${WARN_CHECK_COUNT}"
  echo "block_rows=${BLOCK_ROW_COUNT}"
  echo "warn_rows=${WARN_ROW_COUNT}"
  echo "run_dir=${RUN_DIR}"
} > "${SUMMARY_FILE}"

{
  echo "# 退款闭环巡检报告"
  echo
  echo "- 生成时间: $(date '+%F %T')"
  echo "- 窗口: ${WINDOW_HOURS} 小时"
  echo "- 退款超时阈值: ${REFUND_TIMEOUT_MINUTES} 分钟"
  if [[ -n "${ORDER_NO}" ]]; then
    echo "- 指定单号: ${ORDER_NO}"
  fi
  echo "- 结论: **${GATE_RESULT}**"
  echo
  echo "| 检查项 | 严重级别 | 结果 | 命中行数 | 明细 |"
  echo "|---|---|---|---:|---|"
  for item in "${CHECK_RESULTS[@]}"; do
    IFS='|' read -r check_id severity status rows title file <<< "${item}"
    echo "| ${check_id} ${title} | ${severity} | ${status} | ${rows} | \`${file}\` |"
  done
  echo
  echo "## 明细文件"
  for item in "${CHECK_RESULTS[@]}"; do
    IFS='|' read -r _ _ _ _ _ file <<< "${item}"
    echo "- \`${file}\`"
  done
} > "${REPORT_FILE}"

if [[ "${NO_ALERT}" -eq 0 && "${GATE_RESULT}" != "GREEN" && -x "${ALERT_SCRIPT}" ]]; then
  MSG_TITLE="退款闭环巡检 ${GATE_RESULT}"
  MSG_BODY="window=${WINDOW_HOURS}h timeout=${REFUND_TIMEOUT_MINUTES}m block_rows=${BLOCK_ROW_COUNT} warn_rows=${WARN_ROW_COUNT} report=${REPORT_FILE}"
  "${ALERT_SCRIPT}" --title "${MSG_TITLE}" --message "${MSG_BODY}" || true
fi

cat "${SUMMARY_FILE}"
echo "report_file=${REPORT_FILE}"
exit "${EXIT_CODE}"
