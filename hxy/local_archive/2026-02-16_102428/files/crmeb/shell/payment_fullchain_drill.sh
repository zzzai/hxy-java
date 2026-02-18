#!/usr/bin/env bash
set -euo pipefail

# D7: 支付全链路演练脚本（数据核验版）
# 说明：在真实交易存在后，用订单/流水/日志交叉核验，生成演练报告。

ORDER_NO="${ORDER_NO:-}"
DRY_RUN=0

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-crmeb_java}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/runtime/payment_drill"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"
MONITOR_SCRIPT="${ROOT_DIR}/shell/payment_monitor_quickcheck.sh"
mkdir -p "${OUT_DIR}"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_fullchain_drill.sh [--order-no ORDER_NO] [--dry-run]

参数：
  --order-no   指定演练订单号（eb_store_order.order_id）
  --dry-run    仅输出计划，不连接数据库

环境变量：
  DB_HOST DB_PORT DB_NAME DB_USER DB_PASS MYSQL_DEFAULTS_FILE
  ALERT_WEBHOOK_URL ALERT_WEBHOOK_TYPE

退出码：
  0  演练通过
  2  演练失败（存在关键项未通过）
  1  执行错误
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --order-no)
      ORDER_NO="$2"
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

if [[ ${DRY_RUN} -eq 1 ]]; then
  cat <<MSG
[dry-run] 支付全链路演练计划：
1) 读取最新已支付微信订单（或指定 --order-no）
2) 校验订单支付状态/商户单号
3) 校验微信流水 trade_state=SUCCESS
4) 校验订单状态日志（pay_success）
5) 若存在退款状态，校验 refund_price 日志
6) 关联最近一次日对账结果，校验是否仍在未消差清单
7) 输出 markdown 报告
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
MYSQL_CMD+=("${DB_NAME}" --default-character-set=utf8mb4 --batch --raw --skip-column-names)
if [[ -n "${DB_PASS}" && -z "${MYSQL_DEFAULTS_FILE}" ]]; then
  MYSQL_CMD+=(--password="${DB_PASS}")
fi

if [[ -z "${ORDER_NO}" ]]; then
  ORDER_NO="$("${MYSQL_CMD[@]}" -e "
    SELECT so.order_id
    FROM eb_store_order so
    WHERE so.pay_type='weixin' AND so.paid=1
    ORDER BY so.pay_time DESC
    LIMIT 1;")"
fi

if [[ -z "${ORDER_NO}" ]]; then
  echo "未找到可用于演练的微信已支付订单，请先完成一笔真实交易。"
  exit 1
fi

order_row="$("${MYSQL_CMD[@]}" -e "
  SELECT so.id, so.order_id, IFNULL(so.out_trade_no,''), so.paid, IFNULL(so.refund_status,0), so.pay_price, DATE_FORMAT(so.pay_time,'%Y-%m-%d %H:%i:%s'), IFNULL(so.store_id,0)
  FROM eb_store_order so
  WHERE so.order_id='${ORDER_NO}'
  LIMIT 1;")"

if [[ -z "${order_row}" ]]; then
  echo "订单不存在: ${ORDER_NO}"
  exit 1
fi

IFS=$'\t' read -r oid order_id out_trade_no paid refund_status pay_price pay_time store_id <<< "${order_row}"

pay_info_count="$("${MYSQL_CMD[@]}" -e "SELECT COUNT(1) FROM eb_wechat_pay_info WHERE out_trade_no='${out_trade_no}';")"
pay_info_success_count="$("${MYSQL_CMD[@]}" -e "SELECT COUNT(1) FROM eb_wechat_pay_info WHERE out_trade_no='${out_trade_no}' AND IFNULL(trade_state,'')='SUCCESS';")"
pay_info_latest_trade_state="$("${MYSQL_CMD[@]}" -e "SELECT IFNULL(trade_state,'') FROM eb_wechat_pay_info WHERE out_trade_no='${out_trade_no}' ORDER BY id DESC LIMIT 1;")"
pay_log_count="$("${MYSQL_CMD[@]}" -e "SELECT COUNT(1) FROM eb_store_order_status WHERE oid=${oid} AND change_type='pay_success';")"
refund_log_count="$("${MYSQL_CMD[@]}" -e "SELECT COUNT(1) FROM eb_store_order_status WHERE oid=${oid} AND change_type='refund_price';")"

monitor_result="N/A"
if [[ -x "${MONITOR_SCRIPT}" ]]; then
  set +e
  "${MONITOR_SCRIPT}" --window 30 --tail 3000 >/tmp/payment_drill_monitor.txt 2>&1
  monitor_rc=$?
  set -e
  if [[ ${monitor_rc} -eq 0 ]]; then
    monitor_result="PASS"
  elif [[ ${monitor_rc} -eq 2 ]]; then
    monitor_result="WARN"
  else
    monitor_result="FAIL"
  fi
else
  monitor_result="SKIP"
fi

check_paid="FAIL"
check_pay_log="FAIL"
check_pay_info="FAIL"
check_refund="PASS"
check_refund_channel_state="PASS"
check_reconcile_consistency="WARN"
reconcile_note="未找到对账摘要文件"

if [[ "${paid}" == "1" && -n "${out_trade_no}" ]]; then
  check_paid="PASS"
fi
if (( pay_log_count > 0 )); then
  check_pay_log="PASS"
fi
if (( pay_info_count > 0 && pay_info_success_count > 0 )); then
  check_pay_info="PASS"
fi

if [[ "${refund_status}" != "0" ]]; then
  if (( refund_log_count > 0 )); then
    check_refund="PASS"
  else
    check_refund="FAIL"
  fi
  if [[ "${refund_status}" == "2" ]]; then
    if [[ "${pay_info_latest_trade_state}" == "SUCCESS" || "${pay_info_latest_trade_state}" == "REFUND" || -z "${pay_info_latest_trade_state}" ]]; then
      check_refund_channel_state="PASS"
    else
      check_refund_channel_state="FAIL"
    fi
  fi
fi

# 关联最近一次对账结果，检查是否仍在未消差清单
latest_summary=""
shopt -s nullglob
summary_candidates=("${ROOT_DIR}"/runtime/payment_reconcile/*/summary.txt)
shopt -u nullglob
if [[ ${#summary_candidates[@]} -gt 0 ]]; then
  latest_summary="$(ls -1t "${summary_candidates[@]}" | head -n 1)"
fi

if [[ -n "${latest_summary}" && -f "${latest_summary}" ]]; then
  unresolved_file="$(grep '^main_diff_file=' "${latest_summary}" | head -n1 | cut -d'=' -f2-)"
  cleared_file="$(grep '^main_cleared_file=' "${latest_summary}" | head -n1 | cut -d'=' -f2-)"
  raw_count="$(grep '^main_raw_diff_count=' "${latest_summary}" | head -n1 | cut -d'=' -f2-)"
  unresolved_count="$(grep '^main_diff_count=' "${latest_summary}" | head -n1 | cut -d'=' -f2-)"
  if [[ -n "${unresolved_file}" && -f "${unresolved_file}" ]]; then
    if grep -Fq "${out_trade_no}" "${unresolved_file}"; then
      check_reconcile_consistency="FAIL"
      reconcile_note="订单仍在未消差清单(main_diff.tsv)"
    else
      check_reconcile_consistency="PASS"
      reconcile_note="未命中未消差清单；summary raw=${raw_count:-N/A}, unresolved=${unresolved_count:-N/A}"
      if [[ "${refund_status}" == "2" && -n "${cleared_file}" && -f "${cleared_file}" ]]; then
        if grep -Fq "${out_trade_no}" "${cleared_file}"; then
          reconcile_note="${reconcile_note}; 已命中退款自动消差清单"
        fi
      fi
    fi
  else
    check_reconcile_consistency="WARN"
    reconcile_note="summary存在但 main_diff_file 无效: ${unresolved_file:-<empty>}"
  fi
fi

report_time="$(date '+%Y-%m-%d %H:%M:%S')"
report_file="${OUT_DIR}/drill-${ORDER_NO}-$(date '+%Y%m%d%H%M%S').md"

cat > "${report_file}" <<REPORT
# 支付全链路演练报告

- 生成时间: ${report_time}
- 订单号: ${order_id}
- storeId: ${store_id}
- out_trade_no: ${out_trade_no}
- paid: ${paid}
- refund_status: ${refund_status}
- wx_trade_state(latest): ${pay_info_latest_trade_state:-<empty>}
- pay_price: ${pay_price}
- pay_time: ${pay_time}
- reconcile_summary: ${latest_summary:-<none>}

## 检查项
- 订单支付状态与商户单号: ${check_paid}
- 订单支付成功日志(pay_success): ${check_pay_log} (count=${pay_log_count})
- 微信流水SUCCESS: ${check_pay_info} (all=${pay_info_count}, success=${pay_info_success_count})
- 退款日志一致性(refund_price): ${check_refund} (count=${refund_log_count})
- 退款状态与渠道状态一致性: ${check_refund_channel_state}
- 对账一致性（未消差清单）: ${check_reconcile_consistency} (${reconcile_note})
- 近30分钟监控巡检: ${monitor_result}

## 建议
- 若任一检查项为 FAIL，先停止灰度扩容并排查回调/查单/日志链路。
- 如监控巡检为 WARN，先查看 `runtime/payment_monitor_cron.log` 和应用日志。
REPORT

echo "[drill] 演练报告已生成: ${report_file}"

overall_fail=0
for s in "${check_paid}" "${check_pay_log}" "${check_pay_info}" "${check_refund}" "${check_refund_channel_state}" "${check_reconcile_consistency}"; do
  if [[ "${s}" == "FAIL" ]]; then
    overall_fail=1
    break
  fi
done

if [[ ${overall_fail} -eq 1 ]]; then
  if [[ -x "${ALERT_SCRIPT}" ]]; then
    "${ALERT_SCRIPT}" \
      --title "支付全链路演练失败" \
      --content "order=${order_id}; report=${report_file}" || true
  fi
  exit 2
fi

exit 0
