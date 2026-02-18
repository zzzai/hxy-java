#!/usr/bin/env bash
set -euo pipefail

# D13: 支付幂等回归检查脚本（数据库一致性版）
# 目标：快速发现“重复回调/重复退款/乱序处理”导致的状态异常。

ORDER_NO="${ORDER_NO:-}"
WINDOW_HOURS="${WINDOW_HOURS:-24}"
PENDING_TIMEOUT_MINUTES="${PENDING_TIMEOUT_MINUTES:-30}"
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
  ./shell/payment_idempotency_regression.sh [--order-no ORDER_NO] [--window-hours N] [--pending-timeout-minutes N] [--output-dir PATH] [--no-alert]

参数：
  --order-no ORDER_NO            指定订单回归（order_id 或 out_trade_no）
  --window-hours N               统计窗口小时（默认 24）
  --pending-timeout-minutes N    refund_status=3 超时阈值（默认 30）
  --output-dir PATH              输出目录（默认 runtime/payment_idempotency_regression）
  --no-alert                     不推送告警

环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE
  ALERT_WEBHOOK_URL ALERT_WEBHOOK_TYPE

退出码：
  0  通过（未发现异常）
  2  发现异常（建议人工处理）
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
    --pending-timeout-minutes)
      PENDING_TIMEOUT_MINUTES="$2"
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
if ! [[ "${PENDING_TIMEOUT_MINUTES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --pending-timeout-minutes 必须是正整数"
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
  OUTPUT_DIR="${ROOT_DIR}/runtime/payment_idempotency_regression"
fi
RUN_ID="$(date '+%Y%m%d%H%M%S')"
RUN_DIR="${OUTPUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"

START_TIME="$(date -d "-${WINDOW_HOURS} hours" '+%F %T')"

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

ORDER_FILTER_SO=""
ORDER_FILTER_ORDER=""
ORDER_FILTER_DUP_OUT_TRADE=""
if [[ -n "${ORDER_NO}" ]]; then
  ORDER_ESC="$(sql_escape "${ORDER_NO}")"
  ORDER_FILTER_SO=" AND (so.order_id='${ORDER_ESC}' OR so.out_trade_no='${ORDER_ESC}')"
  ORDER_FILTER_ORDER=" AND (order_id='${ORDER_ESC}' OR out_trade_no='${ORDER_ESC}')"
  ORDER_FILTER_DUP_OUT_TRADE=" AND out_trade_no IN (SELECT DISTINCT out_trade_no FROM eb_store_order WHERE (order_id='${ORDER_ESC}' OR out_trade_no='${ORDER_ESC}') AND out_trade_no IS NOT NULL AND out_trade_no <> '')"
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

FILE_DUP_PAY="${RUN_DIR}/dup_pay_success.tsv"
FILE_DUP_REFUND="${RUN_DIR}/dup_refund_success.tsv"
FILE_PAID_NO_PAYINFO="${RUN_DIR}/paid_without_payinfo.tsv"
FILE_PAID_TRADE_ABNORMAL="${RUN_DIR}/paid_trade_state_abnormal.tsv"
FILE_REFUND_NO_LOG="${RUN_DIR}/refund_done_without_success_log.tsv"
FILE_REFUND_TIMEOUT="${RUN_DIR}/refund_pending_timeout.tsv"
FILE_DUP_OUT_TRADE="${RUN_DIR}/duplicate_out_trade_no.tsv"
FILE_ILLEGAL_REFUND="${RUN_DIR}/illegal_refund_status.tsv"
SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"

run_sql_to_file "${FILE_DUP_PAY}" "
SELECT
  so.order_id,
  so.out_trade_no,
  COUNT(*) AS pay_success_count,
  MIN(s.create_time) AS first_time,
  MAX(s.create_time) AS last_time
FROM eb_store_order_status s
JOIN eb_store_order so ON so.id = s.oid
WHERE s.change_type = 'pay_success'
  AND s.create_time >= '${START_TIME}'
  ${ORDER_FILTER_SO}
GROUP BY s.oid, so.order_id, so.out_trade_no
HAVING COUNT(*) > 1
ORDER BY pay_success_count DESC, so.order_id;
"

run_sql_to_file "${FILE_DUP_REFUND}" "
SELECT
  so.order_id,
  so.out_trade_no,
  COUNT(*) AS refund_success_count,
  MIN(s.create_time) AS first_time,
  MAX(s.create_time) AS last_time
FROM eb_store_order_status s
JOIN eb_store_order so ON so.id = s.oid
WHERE s.change_type = 'refund_price'
  AND s.change_message LIKE '%成功%'
  AND s.create_time >= '${START_TIME}'
  ${ORDER_FILTER_SO}
GROUP BY s.oid, so.order_id, so.out_trade_no
HAVING COUNT(*) > 1
ORDER BY refund_success_count DESC, so.order_id;
"

run_sql_to_file "${FILE_PAID_NO_PAYINFO}" "
SELECT
  so.order_id,
  so.out_trade_no,
  so.pay_time
FROM eb_store_order so
LEFT JOIN (
  SELECT out_trade_no, MAX(id) AS max_id
  FROM eb_wechat_pay_info
  GROUP BY out_trade_no
) latest ON latest.out_trade_no = so.out_trade_no
WHERE so.pay_type = 'weixin'
  AND so.paid = 1
  AND so.pay_time >= '${START_TIME}'
  AND latest.max_id IS NULL
  ${ORDER_FILTER_SO}
ORDER BY so.pay_time DESC;
"

run_sql_to_file "${FILE_PAID_TRADE_ABNORMAL}" "
SELECT
  so.order_id,
  so.out_trade_no,
  so.pay_time,
  IFNULL(pi.trade_state, '') AS trade_state,
  pi.transaction_id
FROM eb_store_order so
JOIN (
  SELECT out_trade_no, MAX(id) AS max_id
  FROM eb_wechat_pay_info
  GROUP BY out_trade_no
) latest ON latest.out_trade_no = so.out_trade_no
JOIN eb_wechat_pay_info pi ON pi.id = latest.max_id
WHERE so.pay_type = 'weixin'
  AND so.paid = 1
  AND so.pay_time >= '${START_TIME}'
  AND IFNULL(pi.trade_state, '') NOT IN ('SUCCESS', 'REFUND')
  ${ORDER_FILTER_SO}
ORDER BY so.pay_time DESC;
"

run_sql_to_file "${FILE_REFUND_NO_LOG}" "
SELECT
  so.order_id,
  so.out_trade_no,
  so.refund_status,
  so.update_time
FROM eb_store_order so
WHERE so.refund_status = 2
  AND so.update_time >= '${START_TIME}'
  AND NOT EXISTS (
    SELECT 1
    FROM eb_store_order_status s
    WHERE s.oid = so.id
      AND s.change_type = 'refund_price'
      AND s.change_message LIKE '%成功%'
  )
  ${ORDER_FILTER_SO}
ORDER BY so.update_time DESC;
"

run_sql_to_file "${FILE_REFUND_TIMEOUT}" "
SELECT
  so.order_id,
  so.out_trade_no,
  so.refund_status,
  so.update_time,
  TIMESTAMPDIFF(MINUTE, so.update_time, NOW()) AS pending_minutes
FROM eb_store_order so
WHERE so.refund_status = 3
  AND TIMESTAMPDIFF(MINUTE, so.update_time, NOW()) > ${PENDING_TIMEOUT_MINUTES}
  ${ORDER_FILTER_SO}
ORDER BY pending_minutes DESC;
"

run_sql_to_file "${FILE_DUP_OUT_TRADE}" "
SELECT
  out_trade_no,
  COUNT(*) AS order_count,
  MIN(order_id) AS min_order_id,
  MAX(order_id) AS max_order_id
FROM eb_store_order
WHERE out_trade_no IS NOT NULL
  AND out_trade_no <> ''
  ${ORDER_FILTER_DUP_OUT_TRADE}
GROUP BY out_trade_no
HAVING COUNT(*) > 1
ORDER BY order_count DESC, out_trade_no;
"

run_sql_to_file "${FILE_ILLEGAL_REFUND}" "
SELECT
  order_id,
  out_trade_no,
  refund_status,
  update_time
FROM eb_store_order
WHERE refund_status NOT IN (0, 1, 2, 3)
  AND update_time >= '${START_TIME}'
  ${ORDER_FILTER_ORDER}
ORDER BY update_time DESC;
"

dup_pay_count="$(count_tsv_rows "${FILE_DUP_PAY}")"
dup_refund_count="$(count_tsv_rows "${FILE_DUP_REFUND}")"
paid_no_payinfo_count="$(count_tsv_rows "${FILE_PAID_NO_PAYINFO}")"
paid_trade_abnormal_count="$(count_tsv_rows "${FILE_PAID_TRADE_ABNORMAL}")"
refund_no_log_count="$(count_tsv_rows "${FILE_REFUND_NO_LOG}")"
refund_timeout_count="$(count_tsv_rows "${FILE_REFUND_TIMEOUT}")"
dup_out_trade_count="$(count_tsv_rows "${FILE_DUP_OUT_TRADE}")"
illegal_refund_count="$(count_tsv_rows "${FILE_ILLEGAL_REFUND}")"

critical_count=$((dup_pay_count + dup_refund_count + paid_no_payinfo_count + dup_out_trade_count + illegal_refund_count))
warn_count=$((paid_trade_abnormal_count + refund_no_log_count + refund_timeout_count))
total_findings=$((critical_count + warn_count))

cat > "${SUMMARY_FILE}" <<TXT
run_id=${RUN_ID}
run_time=$(date '+%Y-%m-%d %H:%M:%S')
window_hours=${WINDOW_HOURS}
start_time=${START_TIME}
order_no=${ORDER_NO}
pending_timeout_minutes=${PENDING_TIMEOUT_MINUTES}
dup_pay_success_count=${dup_pay_count}
dup_refund_success_count=${dup_refund_count}
paid_without_payinfo_count=${paid_no_payinfo_count}
paid_trade_state_abnormal_count=${paid_trade_abnormal_count}
refund_done_without_success_log_count=${refund_no_log_count}
refund_pending_timeout_count=${refund_timeout_count}
duplicate_out_trade_no_count=${dup_out_trade_count}
illegal_refund_status_count=${illegal_refund_count}
critical_count=${critical_count}
warn_count=${warn_count}
total_findings=${total_findings}
run_dir=${RUN_DIR}
TXT

cat > "${REPORT_FILE}" <<MD
# 支付幂等回归报告

- run_id: \`${RUN_ID}\`
- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`
- window_hours: \`${WINDOW_HOURS}\`
- start_time: \`${START_TIME}\`
- order_no: \`${ORDER_NO:-<all>}\`
- pending_timeout_minutes: \`${PENDING_TIMEOUT_MINUTES}\`

| 检查项 | 数量 | 文件 |
|---|---:|---|
| 重复支付成功日志（pay_success） | ${dup_pay_count} | \`${FILE_DUP_PAY}\` |
| 重复退款成功日志（refund_price 成功） | ${dup_refund_count} | \`${FILE_DUP_REFUND}\` |
| 已支付但无支付流水 | ${paid_no_payinfo_count} | \`${FILE_PAID_NO_PAYINFO}\` |
| 已支付但渠道状态异常 | ${paid_trade_abnormal_count} | \`${FILE_PAID_TRADE_ABNORMAL}\` |
| 已退款但无成功日志 | ${refund_no_log_count} | \`${FILE_REFUND_NO_LOG}\` |
| 退款处理中超时 | ${refund_timeout_count} | \`${FILE_REFUND_TIMEOUT}\` |
| 重复 out_trade_no | ${dup_out_trade_count} | \`${FILE_DUP_OUT_TRADE}\` |
| 非法 refund_status | ${illegal_refund_count} | \`${FILE_ILLEGAL_REFUND}\` |

## 结论

- critical_count: **${critical_count}**
- warn_count: **${warn_count}**
- total_findings: **${total_findings}**
- summary: \`${SUMMARY_FILE}\`
MD

echo "[idempotency] report=${REPORT_FILE}"
echo "[idempotency] critical=${critical_count}, warn=${warn_count}, total=${total_findings}"

if (( total_findings > 0 )); then
  if (( NO_ALERT == 0 )) && [[ -x "${ALERT_SCRIPT}" ]]; then
    "${ALERT_SCRIPT}" \
      --title "支付幂等回归告警" \
      --content "critical=${critical_count}; warn=${warn_count}; total=${total_findings}; order_no=${ORDER_NO:-<all>}; report=${REPORT_FILE}" || true
  fi
  exit 2
fi

exit 0
