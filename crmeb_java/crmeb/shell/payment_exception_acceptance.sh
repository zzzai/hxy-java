#!/usr/bin/env bash
set -euo pipefail

# P0: 支付异常场景验收（覆盖：回调/查单一致性、幂等、超时关单、退款补偿、对账工单化）

REPORT_DATE="${REPORT_DATE:-}"
WINDOW_HOURS="${WINDOW_HOURS:-72}"
ORDER_TIMEOUT_HOURS="${ORDER_TIMEOUT_HOURS:-2}"
REFUND_TIMEOUT_MINUTES="${REFUND_TIMEOUT_MINUTES:-30}"
ORDER_NO="${ORDER_NO:-}"
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
  ./shell/payment_exception_acceptance.sh [--date YYYY-MM-DD] [--window-hours N] [--order-timeout-hours N]
    [--refund-timeout-minutes N] [--order-no ORDER_NO] [--output-dir PATH] [--no-alert]

参数：
  --date YYYY-MM-DD                业务日期（默认昨天）
  --window-hours N                 SQL检查窗口（默认 72 小时）
  --order-timeout-hours N          超时未支付阈值（默认 2 小时）
  --refund-timeout-minutes N       退款中超时阈值（默认 30 分钟）
  --order-no ORDER_NO              指定单号过滤（order_id / out_trade_no / transaction_id）
  --output-dir PATH                输出目录（默认 runtime/payment_exception_acceptance）
  --no-alert                       不推送告警

环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE

退出码：
  0  GREEN（通过）
  2  YELLOW/RED（有风险）
  1  执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --date)
      REPORT_DATE="$2"
      shift 2
      ;;
    --window-hours)
      WINDOW_HOURS="$2"
      shift 2
      ;;
    --order-timeout-hours)
      ORDER_TIMEOUT_HOURS="$2"
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

if [[ -z "${REPORT_DATE}" ]]; then
  REPORT_DATE="$(date -d 'yesterday' +%F)"
fi
if ! [[ "${REPORT_DATE}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "参数错误: --date 需要 YYYY-MM-DD"
  exit 1
fi
if ! [[ "${WINDOW_HOURS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --window-hours 必须是正整数"
  exit 1
fi
if ! [[ "${ORDER_TIMEOUT_HOURS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --order-timeout-hours 必须是正整数"
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

if [[ -z "${OUTPUT_DIR}" ]]; then
  OUTPUT_DIR="${ROOT_DIR}/runtime/payment_exception_acceptance"
fi
RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUTPUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"
SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"

START_TIME="$(date -d "-${WINDOW_HOURS} hours" '+%F %T')"
OVERDUE_UNPAID_BEFORE="$(date -d "-${ORDER_TIMEOUT_HOURS} hours" '+%F %T')"
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

ORDER_FILTER_SO=""
ORDER_FILTER_ORDER=""
ORDER_FILTER_PAYINFO=""
if [[ -n "${ORDER_NO}" ]]; then
  ORDER_ESC="$(sql_escape "${ORDER_NO}")"
  ORDER_FILTER_SO=" AND (so.order_id='${ORDER_ESC}' OR so.out_trade_no='${ORDER_ESC}')"
  ORDER_FILTER_ORDER=" AND (order_id='${ORDER_ESC}' OR out_trade_no='${ORDER_ESC}')"
  ORDER_FILTER_PAYINFO=" AND (
    p.out_trade_no='${ORDER_ESC}'
    OR p.transaction_id='${ORDER_ESC}'
    OR EXISTS (
      SELECT 1 FROM eb_store_order sox
      WHERE sox.out_trade_no = p.out_trade_no
        AND sox.order_id='${ORDER_ESC}'
    )
  )"
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
  # 仅统计非空数据行（跳过表头 + 空行），避免“空工单文件”被误判为命中
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

create_issue_file() {
  local file="$1"
  local header="$2"
  local row="$3"
  if [[ -n "${row}" ]]; then
    cat > "${file}" <<EOF
${header}
${row}
EOF
  else
    printf '%s\n' "${header}" > "${file}"
  fi
}

# 1) 回调 + 查单双通道一致性
FILE_01="${RUN_DIR}/01_paid_without_pay_success_log.tsv"
run_sql_to_file "${FILE_01}" "
SELECT so.order_id, so.out_trade_no, so.pay_time
FROM eb_store_order so
WHERE so.pay_type='weixin'
  AND so.paid=1
  AND so.pay_time >= '${START_TIME}'
  ${ORDER_FILTER_SO}
  AND NOT EXISTS (
    SELECT 1 FROM eb_store_order_status s
    WHERE s.oid = so.id
      AND s.change_type='pay_success'
  )
ORDER BY so.pay_time DESC;
"
record_check "P01" "BLOCK" "已支付但缺少pay_success日志" "${FILE_01}" "$(count_tsv_rows "${FILE_01}")"

FILE_02="${RUN_DIR}/02_paid_without_payinfo.tsv"
run_sql_to_file "${FILE_02}" "
SELECT so.order_id, so.out_trade_no, so.pay_time
FROM eb_store_order so
LEFT JOIN (
  SELECT out_trade_no, MAX(id) AS max_id
  FROM eb_wechat_pay_info
  GROUP BY out_trade_no
) lp ON lp.out_trade_no = so.out_trade_no
WHERE so.pay_type='weixin'
  AND so.paid=1
  AND so.pay_time >= '${START_TIME}'
  ${ORDER_FILTER_SO}
  AND lp.max_id IS NULL
ORDER BY so.pay_time DESC;
"
record_check "P02" "BLOCK" "已支付但缺少微信流水" "${FILE_02}" "$(count_tsv_rows "${FILE_02}")"

FILE_03="${RUN_DIR}/03_payinfo_success_but_order_unpaid.tsv"
run_sql_to_file "${FILE_03}" "
SELECT p.out_trade_no, p.transaction_id, p.trade_state, p.time_end, IFNULL(so.order_id,'') AS order_id, IFNULL(so.paid,0) AS paid
FROM eb_wechat_pay_info p
LEFT JOIN eb_store_order so ON so.out_trade_no = p.out_trade_no
WHERE IFNULL(p.trade_state,'')='SUCCESS'
  ${ORDER_FILTER_PAYINFO}
  AND (so.id IS NULL OR so.paid <> 1)
ORDER BY p.id DESC;
"
record_check "P03" "BLOCK" "微信SUCCESS但本地订单未收敛为已支付" "${FILE_03}" "$(count_tsv_rows "${FILE_03}")"

FILE_04="${RUN_DIR}/04_paid_trade_state_abnormal.tsv"
run_sql_to_file "${FILE_04}" "
SELECT so.order_id, so.out_trade_no, IFNULL(pi.trade_state,'') AS trade_state, pi.transaction_id, so.pay_time
FROM eb_store_order so
JOIN (
  SELECT out_trade_no, MAX(id) AS max_id
  FROM eb_wechat_pay_info
  GROUP BY out_trade_no
) lp ON lp.out_trade_no = so.out_trade_no
JOIN eb_wechat_pay_info pi ON pi.id = lp.max_id
WHERE so.pay_type='weixin'
  AND so.paid=1
  AND so.pay_time >= '${START_TIME}'
  ${ORDER_FILTER_SO}
  AND IFNULL(pi.trade_state,'') NOT IN ('SUCCESS', 'REFUND')
ORDER BY so.pay_time DESC;
"
record_check "P04" "WARN" "已支付订单微信侧trade_state异常" "${FILE_04}" "$(count_tsv_rows "${FILE_04}")"

# 2) 幂等键体系（下单/回调/退款）
FILE_05="${RUN_DIR}/05_duplicate_pay_success_log.tsv"
run_sql_to_file "${FILE_05}" "
SELECT so.order_id, so.out_trade_no, COUNT(*) AS cnt, MIN(s.create_time) AS first_time, MAX(s.create_time) AS last_time
FROM eb_store_order_status s
JOIN eb_store_order so ON so.id = s.oid
WHERE s.change_type='pay_success'
  AND s.create_time >= '${START_TIME}'
  ${ORDER_FILTER_SO}
GROUP BY s.oid, so.order_id, so.out_trade_no
HAVING COUNT(*) > 1
ORDER BY cnt DESC, so.order_id;
"
record_check "P05" "BLOCK" "pay_success重复日志（回调幂等异常）" "${FILE_05}" "$(count_tsv_rows "${FILE_05}")"

FILE_06="${RUN_DIR}/06_duplicate_refund_success_log.tsv"
run_sql_to_file "${FILE_06}" "
SELECT so.order_id, so.out_trade_no, COUNT(*) AS cnt, MIN(s.create_time) AS first_time, MAX(s.create_time) AS last_time
FROM eb_store_order_status s
JOIN eb_store_order so ON so.id = s.oid
WHERE s.change_type='refund_price'
  AND s.change_message LIKE '%成功%'
  AND s.create_time >= '${START_TIME}'
  ${ORDER_FILTER_SO}
GROUP BY s.oid, so.order_id, so.out_trade_no
HAVING COUNT(*) > 1
ORDER BY cnt DESC, so.order_id;
"
record_check "P06" "BLOCK" "refund_success重复日志（退款幂等异常）" "${FILE_06}" "$(count_tsv_rows "${FILE_06}")"

FILE_07="${RUN_DIR}/07_duplicate_out_trade_no.tsv"
run_sql_to_file "${FILE_07}" "
SELECT out_trade_no, COUNT(*) AS order_count, GROUP_CONCAT(order_id ORDER BY id ASC SEPARATOR ',') AS order_ids
FROM eb_store_order
WHERE pay_type='weixin'
  AND paid=1
  AND out_trade_no IS NOT NULL
  AND out_trade_no <> ''
  AND pay_time >= '${START_TIME}'
  ${ORDER_FILTER_ORDER}
GROUP BY out_trade_no
HAVING COUNT(*) > 1
ORDER BY order_count DESC, out_trade_no;
"
record_check "P07" "BLOCK" "同一out_trade_no映射多笔已支付订单" "${FILE_07}" "$(count_tsv_rows "${FILE_07}")"

FILE_08="${RUN_DIR}/08_duplicate_transaction_id.tsv"
run_sql_to_file "${FILE_08}" "
SELECT transaction_id, COUNT(*) AS flow_count, GROUP_CONCAT(out_trade_no ORDER BY id ASC SEPARATOR ',') AS out_trade_nos
FROM eb_wechat_pay_info
WHERE transaction_id IS NOT NULL
  AND transaction_id <> ''
GROUP BY transaction_id
HAVING COUNT(*) > 1
ORDER BY flow_count DESC, transaction_id;
"
record_check "P08" "BLOCK" "同一transaction_id出现多次" "${FILE_08}" "$(count_tsv_rows "${FILE_08}")"

# 3) 超时关单 + 资源释放
FILE_09="${RUN_DIR}/09_overdue_unpaid_not_cancelled.tsv"
run_sql_to_file "${FILE_09}" "
SELECT so.order_id, so.out_trade_no, so.create_time, so.is_del, so.is_system_del
FROM eb_store_order so
WHERE so.paid=0
  AND so.is_del=0
  AND so.is_system_del=0
  AND so.create_time <= '${OVERDUE_UNPAID_BEFORE}'
  ${ORDER_FILTER_SO}
ORDER BY so.create_time ASC
LIMIT 500;
"
record_check "P09" "BLOCK" "超时未支付订单未自动取消" "${FILE_09}" "$(count_tsv_rows "${FILE_09}")"

FILE_10="${RUN_DIR}/10_auto_cancel_missing_log.tsv"
run_sql_to_file "${FILE_10}" "
SELECT so.order_id, so.out_trade_no, so.update_time
FROM eb_store_order so
WHERE so.paid=0
  AND so.is_del=1
  AND so.is_system_del=1
  AND so.update_time >= '${START_TIME}'
  ${ORDER_FILTER_SO}
  AND NOT EXISTS (
    SELECT 1 FROM eb_store_order_status s
    WHERE s.oid = so.id
      AND (
        (s.change_type = 'cancel' AND s.change_message LIKE '%到期未支付系统自动取消%')
        OR (s.change_type IN ('cancel', 'cancel_order') AND s.change_message LIKE '%取消订单%')
      )
  )
ORDER BY so.update_time DESC;
"
record_check "P10" "BLOCK" "自动取消缺少cancel日志" "${FILE_10}" "$(count_tsv_rows "${FILE_10}")"

FILE_11="${RUN_DIR}/11_auto_cancel_coupon_not_released.tsv"
run_sql_to_file "${FILE_11}" "
SELECT so.order_id, so.out_trade_no, so.coupon_id, scu.status AS coupon_status, so.update_time
FROM eb_store_order so
JOIN eb_store_coupon_user scu ON scu.id = so.coupon_id
WHERE so.paid=0
  AND so.is_del=1
  AND so.is_system_del=1
  AND so.coupon_id > 0
  AND so.update_time >= '${START_TIME}'
  ${ORDER_FILTER_SO}
  AND scu.status <> 0
ORDER BY so.update_time DESC;
"
record_check "P11" "BLOCK" "自动取消后优惠券未释放为可用" "${FILE_11}" "$(count_tsv_rows "${FILE_11}")"

FILE_12="${RUN_DIR}/12_auto_cancel_has_success_trade.tsv"
run_sql_to_file "${FILE_12}" "
SELECT so.order_id, so.out_trade_no, so.update_time, IFNULL(pi.trade_state,'') AS trade_state, pi.transaction_id
FROM eb_store_order so
JOIN (
  SELECT out_trade_no, MAX(id) AS max_id
  FROM eb_wechat_pay_info
  WHERE out_trade_no IS NOT NULL AND out_trade_no <> ''
  GROUP BY out_trade_no
) lp ON lp.out_trade_no = so.out_trade_no
JOIN eb_wechat_pay_info pi ON pi.id = lp.max_id
WHERE so.paid=0
  AND so.is_del=1
  AND so.is_system_del=1
  AND so.update_time >= '${START_TIME}'
  ${ORDER_FILTER_SO}
  AND IFNULL(pi.trade_state,'')='SUCCESS'
ORDER BY so.update_time DESC;
"
record_check "P12" "BLOCK" "本地已取消但微信侧为SUCCESS（关单/状态收敛异常）" "${FILE_12}" "$(count_tsv_rows "${FILE_12}")"

# 4) 退款补偿 + 逆向回滚
FILE_13="${RUN_DIR}/13_refund_success_without_log.tsv"
run_sql_to_file "${FILE_13}" "
SELECT so.order_id, so.out_trade_no, so.refund_status, so.update_time
FROM eb_store_order so
WHERE so.refund_status = 2
  AND so.pay_type='weixin'
  AND so.update_time >= '${START_TIME}'
  ${ORDER_FILTER_SO}
  AND NOT EXISTS (
    SELECT 1 FROM eb_store_order_status s
    WHERE s.oid = so.id
      AND s.change_type='refund_price'
      AND s.change_message LIKE '%成功%'
  )
ORDER BY so.update_time DESC;
"
record_check "P13" "BLOCK" "refund_status=成功但缺少成功退款日志" "${FILE_13}" "$(count_tsv_rows "${FILE_13}")"

FILE_14="${RUN_DIR}/14_refund_log_without_success_status.tsv"
run_sql_to_file "${FILE_14}" "
SELECT so.order_id, so.out_trade_no, so.refund_status, MAX(s.create_time) AS refund_log_time
FROM eb_store_order so
JOIN eb_store_order_status s ON s.oid = so.id
WHERE s.change_type='refund_price'
  AND s.change_message LIKE '%成功%'
  AND so.pay_type='weixin'
  AND s.create_time >= '${START_TIME}'
  ${ORDER_FILTER_SO}
  AND so.refund_status <> 2
GROUP BY so.id, so.order_id, so.out_trade_no, so.refund_status
ORDER BY refund_log_time DESC;
"
record_check "P14" "BLOCK" "已有退款成功日志但订单退款状态未收敛" "${FILE_14}" "$(count_tsv_rows "${FILE_14}")"

FILE_15="${RUN_DIR}/15_refund_refunding_timeout.tsv"
run_sql_to_file "${FILE_15}" "
SELECT so.order_id, so.out_trade_no, so.refund_status, so.update_time
FROM eb_store_order so
WHERE so.refund_status = 3
  AND so.pay_type='weixin'
  AND so.update_time <= '${REFUND_TIMEOUT_BEFORE}'
  ${ORDER_FILTER_SO}
ORDER BY so.update_time ASC
LIMIT 500;
"
record_check "P15" "BLOCK" "退款中超时未补偿收敛" "${FILE_15}" "$(count_tsv_rows "${FILE_15}")"

FILE_16="${RUN_DIR}/16_refund_applying_stale.tsv"
run_sql_to_file "${FILE_16}" "
SELECT so.order_id, so.out_trade_no, so.refund_status, so.update_time
FROM eb_store_order so
WHERE so.refund_status = 1
  AND so.pay_type='weixin'
  AND so.update_time <= '${OVERDUE_UNPAID_BEFORE}'
  ${ORDER_FILTER_SO}
ORDER BY so.update_time ASC
LIMIT 500;
"
record_check "P16" "WARN" "退款申请长时间未推进" "${FILE_16}" "$(count_tsv_rows "${FILE_16}")"

# 5) 日对账差异自动工单化（文件侧校验）
RECON_SUMMARY="${ROOT_DIR}/runtime/payment_reconcile/${REPORT_DATE}/summary.txt"
TICKET_SUMMARY="${ROOT_DIR}/runtime/payment_reconcile/${REPORT_DATE}/tickets/summary.txt"

kv() {
  local file="$1"
  local key="$2"
  if [[ ! -f "${file}" ]]; then
    printf ''
    return
  fi
  local line
  line="$(grep -E "^${key}=" "${file}" | head -n 1 || true)"
  if [[ -z "${line}" ]]; then
    printf ''
  else
    printf '%s' "${line#*=}"
  fi
}

recon_unresolved="$(kv "${RECON_SUMMARY}" "main_diff_count")"
ticket_total="$(kv "${TICKET_SUMMARY}" "total_tickets")"
ticket_p1="$(kv "${TICKET_SUMMARY}" "p1_count")"
ticket_sla_status="$(kv "${TICKET_SUMMARY}" "sla_status")"

FILE_17="${RUN_DIR}/17_reconcile_unresolved_without_ticket.tsv"
if [[ -f "${RECON_SUMMARY}" ]] && [[ "${recon_unresolved}" =~ ^[0-9]+$ ]] && (( recon_unresolved > 0 )); then
  if [[ ! -f "${TICKET_SUMMARY}" ]] || ! [[ "${ticket_total}" =~ ^[0-9]+$ ]] || (( ticket_total == 0 )); then
    create_issue_file "${FILE_17}" "issue\trecon_unresolved\tticket_total\trecon_summary\tticket_summary" \
      "reconcile_unresolved_without_ticket\t${recon_unresolved}\t${ticket_total:-N/A}\t${RECON_SUMMARY}\t${TICKET_SUMMARY}"
  else
    create_issue_file "${FILE_17}" "issue\trecon_unresolved\tticket_total" ""
  fi
else
  create_issue_file "${FILE_17}" "issue\trecon_unresolved\tticket_total" ""
fi
record_check "P17" "BLOCK" "对账未消差但未自动生成工单" "${FILE_17}" "$(count_tsv_rows "${FILE_17}")"

FILE_18="${RUN_DIR}/18_ticket_escalation_or_sla_breach.tsv"
issue_row=""
if [[ -f "${TICKET_SUMMARY}" ]]; then
  if [[ "${ticket_p1}" =~ ^[0-9]+$ ]] && (( ticket_p1 > 0 )); then
    issue_row="ticket_p1_not_zero\t${ticket_p1}\t${ticket_sla_status:-N/A}\t${TICKET_SUMMARY}"
  fi
  if [[ "${ticket_sla_status^^}" == "BREACH" ]]; then
    if [[ -n "${issue_row}" ]]; then
      issue_row+=$'\n'
    fi
    issue_row+="ticket_sla_breach\t${ticket_p1:-N/A}\t${ticket_sla_status}\t${TICKET_SUMMARY}"
  fi
fi
if [[ -n "${issue_row}" ]]; then
  cat > "${FILE_18}" <<EOF
issue\tp1_count\tsla_status\tticket_summary
${issue_row}
EOF
else
  create_issue_file "${FILE_18}" "issue\tp1_count\tsla_status\tticket_summary" ""
fi
record_check "P18" "BLOCK" "对账工单存在P1或SLA违约" "${FILE_18}" "$(count_tsv_rows "${FILE_18}")"

CHECK_TOTAL="${#CHECK_RESULTS[@]}"
WARN_TOTAL_ROWS=$((WARN_ROW_COUNT))
BLOCK_TOTAL_ROWS=$((BLOCK_ROW_COUNT))

OVERALL="GREEN"
EXIT_CODE=0
if (( BLOCK_CHECK_COUNT > 0 )); then
  OVERALL="RED"
  EXIT_CODE=2
elif (( WARN_CHECK_COUNT > 0 )); then
  OVERALL="YELLOW"
  EXIT_CODE=2
fi

warn_reason=""
block_reason=""
if (( WARN_CHECK_COUNT > 0 )); then
  warn_reason="WARN checks hit=${WARN_CHECK_COUNT}, rows=${WARN_TOTAL_ROWS}"
fi
if (( BLOCK_CHECK_COUNT > 0 )); then
  block_reason="BLOCK checks hit=${BLOCK_CHECK_COUNT}, rows=${BLOCK_TOTAL_ROWS}"
fi

{
  echo "run_id=${RUN_ID}"
  echo "run_time=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "report_date=${REPORT_DATE}"
  echo "overall=${OVERALL}"
  echo "window_hours=${WINDOW_HOURS}"
  echo "order_timeout_hours=${ORDER_TIMEOUT_HOURS}"
  echo "refund_timeout_minutes=${REFUND_TIMEOUT_MINUTES}"
  echo "order_no=${ORDER_NO}"
  echo "db_name=${DB_NAME}"
  echo "check_total=${CHECK_TOTAL}"
  echo "warn_check_count=${WARN_CHECK_COUNT}"
  echo "warn_row_count=${WARN_TOTAL_ROWS}"
  echo "block_check_count=${BLOCK_CHECK_COUNT}"
  echo "block_row_count=${BLOCK_TOTAL_ROWS}"
  echo "warn_reason=${warn_reason}"
  echo "block_reason=${block_reason}"
  echo "reconcile_summary=${RECON_SUMMARY}"
  echo "ticket_summary=${TICKET_SUMMARY}"
  echo "run_dir=${RUN_DIR}"
} > "${SUMMARY_FILE}"

{
  echo "# 支付异常场景验收报告"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`"
  echo "- report_date: \`${REPORT_DATE}\`"
  echo "- overall: **${OVERALL}**"
  echo "- order_no_filter: \`${ORDER_NO:-<none>}\`"
  echo "- db: \`${DB_NAME}\`"
  echo
  echo "## 判定汇总"
  echo
  echo "- check_total: \`${CHECK_TOTAL}\`"
  echo "- block_check_count: \`${BLOCK_CHECK_COUNT}\`"
  echo "- block_row_count: \`${BLOCK_TOTAL_ROWS}\`"
  echo "- warn_check_count: \`${WARN_CHECK_COUNT}\`"
  echo "- warn_row_count: \`${WARN_TOTAL_ROWS}\`"
  echo
  echo "## 明细"
  echo
  echo "| Check | Severity | Status | Rows | Title | Output |"
  echo "|---|---|---|---:|---|---|"
  for line in "${CHECK_RESULTS[@]}"; do
    IFS='|' read -r cid sev st rows title fpath <<< "${line}"
    echo "| ${cid} | ${sev} | ${st} | ${rows} | ${title} | \`${fpath}\` |"
  done
  echo
  echo "## 对账联动"
  echo
  echo "- reconcile_summary: \`${RECON_SUMMARY}\`"
  echo "- ticket_summary: \`${TICKET_SUMMARY}\`"
  echo
  echo "## 输出路径"
  echo
  echo "- summary: \`${SUMMARY_FILE}\`"
  echo "- run_dir: \`${RUN_DIR}\`"
} > "${REPORT_FILE}"

if [[ "${OVERALL}" != "GREEN" && ${NO_ALERT} -eq 0 && -x "${ALERT_SCRIPT}" ]]; then
  "${ALERT_SCRIPT}" \
    --title "支付异常验收: ${OVERALL}" \
    --summary "date=${REPORT_DATE}, block_checks=${BLOCK_CHECK_COUNT}, warn_checks=${WARN_CHECK_COUNT}" \
    --detail "report=${REPORT_FILE}" \
    --severity "$([[ "${OVERALL}" == "RED" ]] && echo ERROR || echo WARN)" || true
fi

echo "[payment-exception-acceptance] summary=${SUMMARY_FILE}"
echo "[payment-exception-acceptance] report=${REPORT_FILE}"
echo "[payment-exception-acceptance] overall=${OVERALL}, block_checks=${BLOCK_CHECK_COUNT}, warn_checks=${WARN_CHECK_COUNT}"

exit "${EXIT_CODE}"
