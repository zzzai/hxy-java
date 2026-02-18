#!/usr/bin/env bash
set -euo pipefail

# D3/D5: 支付 T+1 日对账脚本
# 对齐：eb_store_order（订单） vs eb_wechat_pay_info（微信流水）
# D5联动：退款成功后自动消差（仅消 A3 类）

RECON_DATE="${RECON_DATE:-}"       # 格式 YYYY-MM-DD，默认昨天
OUTPUT_DIR="${OUTPUT_DIR:-}"       # 默认 runtime/payment_reconcile
DRY_RUN=0

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
  ./shell/payment_reconcile_daily.sh [--date YYYY-MM-DD] [--output-dir PATH] [--dry-run]

参数：
  --date         对账日期（按自然日统计）
  --output-dir   报表输出目录（默认 runtime/payment_reconcile）
  --dry-run      仅生成 SQL 与执行计划，不连接数据库

环境变量（可选）：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE
  ALERT_WEBHOOK_URL ALERT_WEBHOOK_TYPE

退出码：
  0  对账正常（无差异）
  2  有差异单（建议人工处理）
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --date)
      RECON_DATE="$2"
      shift 2
      ;;
    --output-dir)
      OUTPUT_DIR="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=1
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

if [[ -z "${RECON_DATE}" ]]; then
  RECON_DATE="$(date -d 'yesterday' +%F)"
fi

if ! [[ "${RECON_DATE}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "参数错误: --date 需要 YYYY-MM-DD"
  exit 1
fi

START_TIME="${RECON_DATE} 00:00:00"
END_DATE="$(date -d "${RECON_DATE} +1 day" +%F)"
END_TIME="${END_DATE} 00:00:00"

if [[ -z "${OUTPUT_DIR}" ]]; then
  OUTPUT_DIR="${ROOT_DIR}/runtime/payment_reconcile"
fi

RUN_DIR="${OUTPUT_DIR}/${RECON_DATE}"
mkdir -p "${RUN_DIR}"

MAIN_SQL_FILE="${RUN_DIR}/main_diff.sql"
ORPHAN_SQL_FILE="${RUN_DIR}/orphan_wx.sql"
MAIN_RAW_TSV="${RUN_DIR}/main_diff_raw.tsv"
MAIN_CLEARED_TSV="${RUN_DIR}/main_diff_cleared_refund.tsv"
MAIN_TSV="${RUN_DIR}/main_diff.tsv"
ORPHAN_TSV="${RUN_DIR}/orphan_wx.tsv"
SUMMARY_FILE="${RUN_DIR}/summary.txt"

cat > "${MAIN_SQL_FILE}" <<SQL
WITH order_paid AS (
    SELECT
        so.id,
        so.order_id,
        so.out_trade_no,
        so.uid,
        so.store_id,
        so.pay_type,
        so.paid,
        so.pay_time,
        so.pay_price,
        IFNULL(so.refund_status, 0) AS refund_status,
        CAST(ROUND(so.pay_price * 100, 0) AS SIGNED) AS pay_price_cent
    FROM eb_store_order so
    WHERE so.pay_type = 'weixin'
      AND so.pay_time >= '${START_TIME}'
      AND so.pay_time < '${END_TIME}'
),
pay_info AS (
    SELECT
        wpi.id,
        wpi.out_trade_no,
        wpi.app_id,
        wpi.mch_id,
        wpi.total_fee,
        wpi.trade_state,
        wpi.transaction_id,
        wpi.time_end
    FROM eb_wechat_pay_info wpi
    WHERE wpi.out_trade_no IS NOT NULL
),
refund_success AS (
    SELECT
        sos.oid,
        1 AS has_refund_success_log
    FROM eb_store_order_status sos
    WHERE sos.change_type = 'refund_price'
      AND sos.change_message LIKE '%成功%'
    GROUP BY sos.oid
)
SELECT
    op.order_id,
    op.out_trade_no,
    op.store_id,
    op.pay_time,
    op.pay_price_cent AS order_total_fee,
    pi.total_fee      AS wx_total_fee,
    op.paid           AS order_paid_flag,
    op.refund_status  AS order_refund_status,
    IFNULL(rs.has_refund_success_log, 0) AS refund_success_log,
    pi.trade_state    AS wx_trade_state,
    pi.mch_id,
    pi.app_id,
    pi.transaction_id,
    CASE
        WHEN pi.out_trade_no IS NULL THEN 'A1_订单有支付记录_但微信流水缺失'
        WHEN op.pay_price_cent <> IFNULL(pi.total_fee, -1) THEN 'A2_金额不一致'
        WHEN op.paid = 1 AND IFNULL(pi.trade_state, '') <> 'SUCCESS' THEN 'A3_订单已支付_微信非SUCCESS'
        WHEN op.paid = 0 AND IFNULL(pi.trade_state, '') = 'SUCCESS' THEN 'A4_微信SUCCESS_订单未支付'
        ELSE 'OK'
    END AS diff_type,
    CASE
        WHEN op.paid = 1
          AND IFNULL(pi.trade_state, '') <> 'SUCCESS'
          AND op.refund_status = 2
          AND IFNULL(rs.has_refund_success_log, 0) = 1
          AND IFNULL(pi.trade_state, '') IN ('REFUND', 'SUCCESS')
        THEN 1
        ELSE 0
    END AS auto_clear_by_refund,
    CASE
        WHEN op.paid = 1
          AND IFNULL(pi.trade_state, '') <> 'SUCCESS'
          AND op.refund_status = 2
          AND IFNULL(rs.has_refund_success_log, 0) = 1
          AND IFNULL(pi.trade_state, '') IN ('REFUND', 'SUCCESS')
        THEN 'C1_退款成功自动消差'
        ELSE ''
    END AS auto_clear_reason
FROM order_paid op
LEFT JOIN pay_info pi
       ON pi.out_trade_no = op.out_trade_no
LEFT JOIN refund_success rs
       ON rs.oid = op.id
WHERE pi.out_trade_no IS NULL
   OR op.pay_price_cent <> IFNULL(pi.total_fee, -1)
   OR (op.paid = 1 AND IFNULL(pi.trade_state, '') <> 'SUCCESS')
   OR (op.paid = 0 AND IFNULL(pi.trade_state, '') = 'SUCCESS')
ORDER BY op.pay_time, op.order_id;
SQL

cat > "${ORPHAN_SQL_FILE}" <<SQL
WITH pay_info AS (
    SELECT
        wpi.out_trade_no,
        wpi.transaction_id,
        wpi.total_fee,
        wpi.trade_state,
        wpi.mch_id,
        wpi.app_id,
        wpi.time_end
    FROM eb_wechat_pay_info wpi
    WHERE wpi.out_trade_no IS NOT NULL
)
SELECT
    pi.out_trade_no,
    pi.transaction_id,
    pi.total_fee,
    pi.trade_state,
    pi.mch_id,
    pi.app_id,
    pi.time_end
FROM pay_info pi
LEFT JOIN eb_store_order so
       ON so.out_trade_no = pi.out_trade_no
WHERE so.id IS NULL
  AND IFNULL(pi.trade_state, '') = 'SUCCESS'
  AND pi.time_end >= '${START_TIME}'
  AND pi.time_end < '${END_TIME}'
ORDER BY pi.time_end;
SQL

if [[ ${DRY_RUN} -eq 1 ]]; then
  cat <<MSG
[dry-run] 已生成 SQL：
- ${MAIN_SQL_FILE}
- ${ORPHAN_SQL_FILE}
[dry-run] 对账时间窗：${START_TIME} ~ ${END_TIME}
[dry-run] 结果分层：
- raw: ${MAIN_RAW_TSV}
- auto-cleared: ${MAIN_CLEARED_TSV}
- unresolved: ${MAIN_TSV}
MSG
  exit 0
fi

if ! command -v mysql >/dev/null 2>&1; then
  echo "未找到 mysql 客户端"
  exit 1
fi

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

"${MYSQL_CMD[@]}" < "${MAIN_SQL_FILE}" > "${MAIN_RAW_TSV}"
"${MYSQL_CMD[@]}" < "${ORPHAN_SQL_FILE}" > "${ORPHAN_TSV}"

: > "${MAIN_TSV}"
: > "${MAIN_CLEARED_TSV}"

awk -F $'\t' -v unresolved="${MAIN_TSV}" -v cleared="${MAIN_CLEARED_TSV}" '
BEGIN { idx = 0; }
NR == 1 {
  for (i = 1; i <= NF; i++) {
    if ($i == "auto_clear_by_refund") {
      idx = i;
      break;
    }
  }
  if (idx == 0) { idx = NF; }
  print $0 > unresolved;
  print $0 > cleared;
  next;
}
{
  if ($idx == "1") {
    print $0 > cleared;
  } else {
    print $0 > unresolved;
  }
}
' "${MAIN_RAW_TSV}"

# mysql batch 默认带表头，因此行数减1
main_raw_lines="$(wc -l < "${MAIN_RAW_TSV}" | tr -d ' ')"
main_lines="$(wc -l < "${MAIN_TSV}" | tr -d ' ')"
main_cleared_lines="$(wc -l < "${MAIN_CLEARED_TSV}" | tr -d ' ')"
orphan_lines="$(wc -l < "${ORPHAN_TSV}" | tr -d ' ')"
main_raw_count=$(( main_raw_lines > 0 ? main_raw_lines - 1 : 0 ))
main_diff_count=$(( main_lines > 0 ? main_lines - 1 : 0 ))
main_cleared_count=$(( main_cleared_lines > 0 ? main_cleared_lines - 1 : 0 ))
orphan_count=$(( orphan_lines > 0 ? orphan_lines - 1 : 0 ))

cat > "${SUMMARY_FILE}" <<TXT
recon_date=${RECON_DATE}
start_time=${START_TIME}
end_time=${END_TIME}
main_raw_diff_count=${main_raw_count}
main_cleared_by_refund_count=${main_cleared_count}
main_diff_count=${main_diff_count}
orphan_wx_count=${orphan_count}
main_raw_diff_file=${MAIN_RAW_TSV}
main_cleared_file=${MAIN_CLEARED_TSV}
main_diff_file=${MAIN_TSV}
orphan_wx_file=${ORPHAN_TSV}
TXT

echo "[reconcile] 完成: ${SUMMARY_FILE}"
echo "[reconcile] main_raw_diff_count=${main_raw_count}, main_cleared_by_refund_count=${main_cleared_count}, main_diff_count=${main_diff_count}, orphan_wx_count=${orphan_count}"

if (( main_diff_count > 0 || orphan_count > 0 )); then
  if [[ -x "${ALERT_SCRIPT}" ]]; then
    "${ALERT_SCRIPT}" \
      --title "支付日对账告警" \
      --content "日期=${RECON_DATE}; raw_diff=${main_raw_count}; auto_cleared_refund=${main_cleared_count}; unresolved_diff=${main_diff_count}; orphan_wx=${orphan_count}; summary=${SUMMARY_FILE}; unresolved_file=${MAIN_TSV}; cleared_file=${MAIN_CLEARED_TSV}" || true
  fi
  exit 2
fi

exit 0
