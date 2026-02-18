#!/usr/bin/env bash
set -euo pipefail

# 支付日常运营简报生成器
# 统计来源：runtime/payment_ops_daily/run-*/summary.txt

REPORT_DATE="${REPORT_DATE:-}"
OPS_DIR="${OPS_DIR:-}"
OUT_DIR="${OUT_DIR:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_OPS_DIR="${ROOT_DIR}/runtime/payment_ops_daily"
DEFAULT_OUT_DIR="${ROOT_DIR}/runtime/payment_daily_report"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_daily_report.sh [--date YYYY-MM-DD] [--ops-dir PATH] [--out-dir PATH]

参数：
  --date YYYY-MM-DD   统计日期（默认昨天，按 recon_date 过滤）
  --ops-dir PATH      巡检运行目录（默认 runtime/payment_ops_daily）
  --out-dir PATH      报告输出目录（默认 runtime/payment_daily_report）

退出码：
  0  报告生成成功
  2  指定日期无可用巡检数据
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --date)
      REPORT_DATE="$2"
      shift 2
      ;;
    --ops-dir)
      OPS_DIR="$2"
      shift 2
      ;;
    --out-dir)
      OUT_DIR="$2"
      shift 2
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

if [[ -z "${OPS_DIR}" ]]; then
  OPS_DIR="${DEFAULT_OPS_DIR}"
fi
if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${DEFAULT_OUT_DIR}"
fi

kv() {
  local file="$1"
  local key="$2"
  local line
  line="$(grep -E "^${key}=" "${file}" | head -n 1 || true)"
  if [[ -z "${line}" ]]; then
    printf ''
  else
    printf '%s' "${line#*=}"
  fi
}

latest_summary() {
  local base="$1"
  if [[ ! -d "${base}" ]]; then
    printf ''
    return
  fi
  find "${base}" -maxdepth 2 -type f -name 'summary.txt' -printf '%T@ %p\n' 2>/dev/null \
    | sort -n \
    | tail -n 1 \
    | cut -d' ' -f2- || true
}

latest_summary_by_key() {
  local base="$1"
  local key="$2"
  local expected="$3"
  if [[ ! -d "${base}" ]]; then
    printf ''
    return
  fi
  local file=""
  while IFS= read -r file; do
    [[ -f "${file}" ]] || continue
    if [[ "$(kv "${file}" "${key}")" == "${expected}" ]]; then
      printf '%s' "${file}"
      return
    fi
  done < <(find "${base}" -maxdepth 2 -type f -name 'summary.txt' -printf '%T@ %p\n' 2>/dev/null | sort -nr | cut -d' ' -f2-)
  printf ''
}

if [[ ! -d "${OPS_DIR}" ]]; then
  echo "未找到巡检目录: ${OPS_DIR}"
  exit 2
fi

run_count=0
ok_count=0
alert_count=0
error_count=0
preflight_alert_count=0
monitor_alert_count=0
reconcile_alert_count=0

last_run_time=""
last_summary_file=""

while IFS= read -r summary; do
  [[ -f "${summary}" ]] || continue
  # 仅处理 run 根 summary（避免误读 reconcile 子目录 summary）
  if ! grep -q '^run_id=' "${summary}"; then
    continue
  fi

  recon_date="$(kv "${summary}" "recon_date")"
  [[ "${recon_date}" == "${REPORT_DATE}" ]] || continue

  run_count=$((run_count + 1))
  severity="$(kv "${summary}" "severity")"
  run_time="$(kv "${summary}" "run_time")"
  preflight_rc="$(kv "${summary}" "preflight_rc")"
  monitor_rc="$(kv "${summary}" "monitor_rc")"
  reconcile_rc="$(kv "${summary}" "reconcile_rc")"

  case "${severity}" in
    OK) ok_count=$((ok_count + 1)) ;;
    ALERT) alert_count=$((alert_count + 1)) ;;
    ERROR) error_count=$((error_count + 1)) ;;
  esac

  if [[ "${preflight_rc}" != "0" ]]; then
    preflight_alert_count=$((preflight_alert_count + 1))
  fi
  if [[ "${monitor_rc}" != "0" ]]; then
    monitor_alert_count=$((monitor_alert_count + 1))
  fi
  if [[ "${reconcile_rc}" != "0" ]]; then
    reconcile_alert_count=$((reconcile_alert_count + 1))
  fi

  if [[ -z "${last_run_time}" || "${run_time}" > "${last_run_time}" ]]; then
    last_run_time="${run_time}"
    last_summary_file="${summary}"
  fi
done < <(find "${OPS_DIR}" -type f -name 'summary.txt' | sort)

REPORT_DAY_DIR="${OUT_DIR}/${REPORT_DATE}"
mkdir -p "${REPORT_DAY_DIR}"
REPORT_FILE="${REPORT_DAY_DIR}/payment_daily_report_${REPORT_DATE}.md"

if (( run_count == 0 )); then
  cat > "${REPORT_FILE}" <<MD
# 支付日常运营简报（${REPORT_DATE}）

- 生成时间：$(date '+%Y-%m-%d %H:%M:%S')
- 统计目录：\`${OPS_DIR}\`
- 结论：当日未发现可用巡检数据（按 \`recon_date=${REPORT_DATE}\` 过滤）

建议：先执行一轮 \`payment_ops_daily.sh\`，再重新生成简报。
MD
  echo "[daily-report] 无数据: ${REPORT_FILE}"
  exit 2
fi

latest_run_id="$(kv "${last_summary_file}" "run_id")"
latest_severity="$(kv "${last_summary_file}" "severity")"
latest_preflight_rc="$(kv "${last_summary_file}" "preflight_rc")"
latest_monitor_rc="$(kv "${last_summary_file}" "monitor_rc")"
latest_reconcile_rc="$(kv "${last_summary_file}" "reconcile_rc")"
latest_preflight_log="$(kv "${last_summary_file}" "preflight_log")"
latest_monitor_log="$(kv "${last_summary_file}" "monitor_log")"
latest_reconcile_log="$(kv "${last_summary_file}" "reconcile_log")"
latest_reconcile_summary="$(kv "${last_summary_file}" "reconcile_summary")"

raw_diff="-"
auto_cleared="-"
unresolved_diff="-"
orphan_wx="-"
if [[ -n "${latest_reconcile_summary}" && -f "${latest_reconcile_summary}" ]]; then
  raw_diff="$(kv "${latest_reconcile_summary}" "main_raw_diff_count")"
  auto_cleared="$(kv "${latest_reconcile_summary}" "main_cleared_by_refund_count")"
  unresolved_diff="$(kv "${latest_reconcile_summary}" "main_diff_count")"
  orphan_wx="$(kv "${latest_reconcile_summary}" "orphan_wx_count")"
fi

latest_sla_summary="$(latest_summary "${ROOT_DIR}/runtime/payment_reconcile_sla")"
sla_severity="-"
sla_issue_days="-"
sla_pending_days="-"
sla_breach_days="-"
if [[ -n "${latest_sla_summary}" && -f "${latest_sla_summary}" ]]; then
  sla_severity="$(kv "${latest_sla_summary}" "severity")"
  sla_issue_days="$(kv "${latest_sla_summary}" "issue_days")"
  sla_pending_days="$(kv "${latest_sla_summary}" "pending_days")"
  sla_breach_days="$(kv "${latest_sla_summary}" "breach_days")"
fi

ticketize_summary="${ROOT_DIR}/runtime/payment_reconcile/${REPORT_DATE}/tickets/summary.txt"
ticket_total="-"
ticket_p1="-"
ticket_p2="-"
ticket_escalated="-"
ticket_sla_status="-"
if [[ -f "${ticketize_summary}" ]]; then
  ticket_total="$(kv "${ticketize_summary}" "total_tickets")"
  ticket_p1="$(kv "${ticketize_summary}" "p1_count")"
  ticket_p2="$(kv "${ticketize_summary}" "p2_count")"
  ticket_escalated="$(kv "${ticketize_summary}" "escalated_count")"
  ticket_sla_status="$(kv "${ticketize_summary}" "sla_status")"
fi

latest_booking_repair_summary="$(latest_summary "${ROOT_DIR}/runtime/payment_booking_verify_repair")"
booking_repair_severity="-"
booking_repair_before_total="-"
booking_repair_after_total="-"
if [[ -n "${latest_booking_repair_summary}" && -f "${latest_booking_repair_summary}" ]]; then
  booking_repair_severity="$(kv "${latest_booking_repair_summary}" "severity")"
  booking_repair_before_total="$(kv "${latest_booking_repair_summary}" "before_total")"
  booking_repair_after_total="$(kv "${latest_booking_repair_summary}" "after_total")"
fi

latest_mapping_audit_summary="$(latest_summary "${ROOT_DIR}/runtime/payment_store_mapping")"
mapping_overall="-"
mapping_critical_count="-"
mapping_warn_count="-"
mapping_missing_count="-"
mapping_invalid_count="-"
mapping_duplicate_store_count="-"
mapping_orphan_non_empty_count="-"
if [[ -n "${latest_mapping_audit_summary}" && -f "${latest_mapping_audit_summary}" ]]; then
  mapping_overall="$(kv "${latest_mapping_audit_summary}" "overall")"
  mapping_critical_count="$(kv "${latest_mapping_audit_summary}" "critical_count")"
  mapping_warn_count="$(kv "${latest_mapping_audit_summary}" "warn_count")"
  mapping_missing_count="$(kv "${latest_mapping_audit_summary}" "missing_count")"
  mapping_invalid_count="$(kv "${latest_mapping_audit_summary}" "invalid_count")"
  mapping_duplicate_store_count="$(kv "${latest_mapping_audit_summary}" "duplicate_store_count")"
  mapping_orphan_non_empty_count="$(kv "${latest_mapping_audit_summary}" "orphan_non_empty_count")"
fi

latest_cutover_gate_summary="$(latest_summary_by_key "${ROOT_DIR}/runtime/payment_cutover_gate" "report_date" "${REPORT_DATE}")"
if [[ -z "${latest_cutover_gate_summary}" ]]; then
  latest_cutover_gate_summary="$(latest_summary "${ROOT_DIR}/runtime/payment_cutover_gate")"
fi
cutover_gate_overall="-"
cutover_gate_decision="-"
cutover_gate_block_count="-"
cutover_gate_warn_count="-"
cutover_gate_preflight_rc="-"
cutover_gate_ops_status_overall="-"
if [[ -n "${latest_cutover_gate_summary}" && -f "${latest_cutover_gate_summary}" ]]; then
  cutover_gate_overall="$(kv "${latest_cutover_gate_summary}" "overall")"
  cutover_gate_decision="$(kv "${latest_cutover_gate_summary}" "gate_decision")"
  cutover_gate_block_count="$(kv "${latest_cutover_gate_summary}" "block_count")"
  cutover_gate_warn_count="$(kv "${latest_cutover_gate_summary}" "warn_count")"
  cutover_gate_preflight_rc="$(kv "${latest_cutover_gate_summary}" "preflight_rc")"
  cutover_gate_ops_status_overall="$(kv "${latest_cutover_gate_summary}" "ops_status_overall")"
fi

latest_decision_chain_summary="$(latest_summary "${ROOT_DIR}/runtime/payment_decision_chain_smoke")"
decision_chain_severity="-"
decision_chain_fail_count="-"
if [[ -n "${latest_decision_chain_summary}" && -f "${latest_decision_chain_summary}" ]]; then
  decision_chain_severity="$(kv "${latest_decision_chain_summary}" "severity")"
  decision_chain_fail_count="$(kv "${latest_decision_chain_summary}" "fail_count")"
fi

decision_ticketize_summary="${ROOT_DIR}/runtime/payment_reconcile/${REPORT_DATE}/tickets/summary_decision.txt"
decision_ticket_total="-"
decision_ticket_p1="-"
decision_ticket_p2="-"
if [[ -f "${decision_ticketize_summary}" ]]; then
  decision_ticket_total="$(kv "${decision_ticketize_summary}" "total_tickets")"
  decision_ticket_p1="$(kv "${decision_ticketize_summary}" "p1_count")"
  decision_ticket_p2="$(kv "${decision_ticketize_summary}" "p2_count")"
fi

trend_rows="$(
  for offset in 6 5 4 3 2 1 0; do
    day="$(date -d "${REPORT_DATE} -${offset} day" +%F)"

    trend_unresolved="-"
    recon_summary_day="${ROOT_DIR}/runtime/payment_reconcile/${day}/summary.txt"
    if [[ -f "${recon_summary_day}" ]]; then
      trend_unresolved="$(kv "${recon_summary_day}" "main_diff_count")"
      [[ -n "${trend_unresolved}" ]] || trend_unresolved="-"
    fi

    trend_ticket_p1="-"
    ticket_summary_day="${ROOT_DIR}/runtime/payment_reconcile/${day}/tickets/summary.txt"
    if [[ -f "${ticket_summary_day}" ]]; then
      trend_ticket_p1="$(kv "${ticket_summary_day}" "p1_count")"
      [[ -n "${trend_ticket_p1}" ]] || trend_ticket_p1="-"
    fi

    trend_decision_fail="-"
    decision_summary_day="$(latest_summary_by_key "${ROOT_DIR}/runtime/payment_decision_chain_smoke" "report_date" "${day}")"
    if [[ -n "${decision_summary_day}" && -f "${decision_summary_day}" ]]; then
      trend_decision_fail="$(kv "${decision_summary_day}" "fail_count")"
      [[ -n "${trend_decision_fail}" ]] || trend_decision_fail="-"
    fi

    echo "| ${day} | ${trend_unresolved} | ${trend_ticket_p1} | ${trend_decision_fail} |"
  done
)"

cat > "${REPORT_FILE}" <<MD
# 支付日常运营简报（${REPORT_DATE}）

- 生成时间：$(date '+%Y-%m-%d %H:%M:%S')
- 统计目录：\`${OPS_DIR}\`
- 巡检总次数：${run_count}
- 结论：最新一轮为 **${latest_severity}**

## 一、巡检概览

| 指标 | 数值 |
|---|---:|
| OK 次数 | ${ok_count} |
| ALERT 次数 | ${alert_count} |
| ERROR 次数 | ${error_count} |
| preflight 非0次数 | ${preflight_alert_count} |
| monitor 非0次数 | ${monitor_alert_count} |
| reconcile 非0次数 | ${reconcile_alert_count} |

## 二、最新一轮详情

- run_id：\`${latest_run_id}\`
- run_time：\`${last_run_time}\`
- severity：\`${latest_severity}\`
- preflight_rc：\`${latest_preflight_rc}\`
- monitor_rc：\`${latest_monitor_rc}\`
- reconcile_rc：\`${latest_reconcile_rc}\`

## 三、对账摘要（最新一轮）

| 项 | 数值 |
|---|---:|
| 原始差异（raw） | ${raw_diff} |
| 退款自动消差 | ${auto_cleared} |
| 最终待处理差异 | ${unresolved_diff} |
| 孤儿微信流水 | ${orphan_wx} |

## 四、对账SLA

| 项 | 数值 |
|---|---:|
| SLA状态 | ${sla_severity} |
| issue_days | ${sla_issue_days} |
| pending_days | ${sla_pending_days} |
| breach_days | ${sla_breach_days} |

## 五、对账工单

| 项 | 数值 |
|---|---:|
| tickets_total | ${ticket_total} |
| tickets_p1 | ${ticket_p1} |
| tickets_p2 | ${ticket_p2} |
| tickets_escalated | ${ticket_escalated} |
| ticket_sla_status | ${ticket_sla_status} |

## 六、预约核销修复

| 项 | 数值 |
|---|---:|
| booking_repair_severity | ${booking_repair_severity} |
| booking_repair_before_total | ${booking_repair_before_total} |
| booking_repair_after_total | ${booking_repair_after_total} |

## 七、门店映射与门禁

| 项 | 数值 |
|---|---:|
| mapping_overall | ${mapping_overall} |
| mapping_critical_count | ${mapping_critical_count} |
| mapping_warn_count | ${mapping_warn_count} |
| mapping_missing_count | ${mapping_missing_count} |
| mapping_invalid_count | ${mapping_invalid_count} |
| mapping_duplicate_store_count | ${mapping_duplicate_store_count} |
| mapping_orphan_non_empty_count | ${mapping_orphan_non_empty_count} |
| cutover_gate_overall | ${cutover_gate_overall} |
| cutover_gate_decision | ${cutover_gate_decision} |
| cutover_gate_block_count | ${cutover_gate_block_count} |
| cutover_gate_warn_count | ${cutover_gate_warn_count} |
| cutover_gate_preflight_rc | ${cutover_gate_preflight_rc} |
| cutover_gate_ops_status_overall | ${cutover_gate_ops_status_overall} |

## 八、可追溯日志

- preflight：\`${latest_preflight_log}\`
- monitor：\`${latest_monitor_log}\`
- reconcile：\`${latest_reconcile_log}\`
- reconcile summary：\`${latest_reconcile_summary}\`
- reconcile sla summary：\`${latest_sla_summary}\`
- ticketize summary：\`${ticketize_summary}\`
- booking repair summary：\`${latest_booking_repair_summary}\`
- decision_chain summary：\`${latest_decision_chain_summary}\`
- decision ticketize summary：\`${decision_ticketize_summary}\`
- mapping audit summary：\`${latest_mapping_audit_summary}\`
- cutover gate summary：\`${latest_cutover_gate_summary}\`

## 九、判定链路自测

| 项 | 数值 |
|---|---:|
| decision_chain_severity | ${decision_chain_severity} |
| decision_chain_fail_count | ${decision_chain_fail_count} |
| decision_ticket_total | ${decision_ticket_total} |
| decision_ticket_p1 | ${decision_ticket_p1} |
| decision_ticket_p2 | ${decision_ticket_p2} |

## 十、近7天趋势

| 日期 | unresolved_diff | ticket_p1 | decision_chain_fail_count |
|---|---:|---:|---:|
${trend_rows}

## 十一、群内可发摘要

\`\`\`
支付日报 ${REPORT_DATE}
巡检=${run_count}次，最新=${latest_severity}（${last_run_time}）
对账：raw=${raw_diff}，autoCleared=${auto_cleared}，unresolved=${unresolved_diff}，orphan=${orphan_wx}
SLA：severity=${sla_severity}，pending=${sla_pending_days}，breach=${sla_breach_days}
工单：total=${ticket_total}，p1=${ticket_p1}，p2=${ticket_p2}，escalated=${ticket_escalated}
核销修复：severity=${booking_repair_severity}，before=${booking_repair_before_total}，after=${booking_repair_after_total}
映射审计：overall=${mapping_overall}，critical=${mapping_critical_count}，warn=${mapping_warn_count}
切换门禁：decision=${cutover_gate_decision}，overall=${cutover_gate_overall}，block=${cutover_gate_block_count}，warn=${cutover_gate_warn_count}
判定链路：severity=${decision_chain_severity}，fail_count=${decision_chain_fail_count}
判定工单：total=${decision_ticket_total}，p1=${decision_ticket_p1}，p2=${decision_ticket_p2}
\`\`\`
MD

echo "[daily-report] 生成成功: ${REPORT_FILE}"
exit 0
