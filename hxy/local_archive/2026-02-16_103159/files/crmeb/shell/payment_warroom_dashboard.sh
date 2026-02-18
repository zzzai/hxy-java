#!/usr/bin/env bash
set -euo pipefail

# 支付值守看板汇总脚本
# 汇总来源：
# 1) payment_ops_daily
# 2) payment_idempotency_regression
# 3) payment_daily_report
# 4) payment_reconcile summary
# 5) payment_reconcile ticketize summary
# 6) latest cutover_rehearsal / cutover_apply
# 7) latest go/no-go summary
# 8) latest booking_verify_repair summary
# 9) cron 托管任务状态
# 10) latest decision_chain_smoke summary
# 11) latest store_mapping_audit summary
# 12) latest cutover_gate summary

REPORT_DATE="${REPORT_DATE:-}"
WINDOW_HOURS="${WINDOW_HOURS:-72}"
NO_ALERT=0
OUT_DIR="${OUT_DIR:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_warroom_dashboard.sh [--date YYYY-MM-DD] [--window-hours N] [--no-alert] [--out-dir PATH]

参数：
  --date YYYY-MM-DD    看板日期（默认昨天）
  --window-hours N     幂等回归窗口小时（默认 72）
  --no-alert           不推送机器人告警
  --out-dir PATH       输出目录（默认 runtime/payment_warroom）

退出码：
  0  看板整体健康
  2  存在风险项（需人工介入）
  1  脚本执行失败
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
    --no-alert)
      NO_ALERT=1
      shift
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
if ! [[ "${WINDOW_HOURS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --window-hours 必须是正整数"
  exit 1
fi

if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${ROOT_DIR}/runtime/payment_warroom"
fi
RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"

SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/dashboard.md"

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

resolve_summary() {
  local base="$1"
  local key="$2"
  local expected="$3"
  local matched=""
  matched="$(latest_summary_by_key "${base}" "${key}" "${expected}")"
  if [[ -n "${matched}" ]]; then
    printf '%s' "${matched}"
  else
    latest_summary "${base}"
  fi
}

run_step() {
  local step="$1"
  shift
  local log="${RUN_DIR}/${step}.log"
  set +e
  "$@" >"${log}" 2>&1
  local rc=$?
  set -e
  printf '%s' "${rc}"
}

echo "[warroom] run_dir=${RUN_DIR}"
echo "[warroom] report_date=${REPORT_DATE}, window_hours=${WINDOW_HOURS}"

ops_rc="$(run_step "01_ops_daily" ./shell/payment_ops_daily.sh --date "${REPORT_DATE}" --no-alert)"
ops_summary="$(sed -n 's/.*summary=//p' "${RUN_DIR}/01_ops_daily.log" | tail -n 1 || true)"
ops_severity="$(kv "${ops_summary}" "severity")"
ops_preflight_rc="$(kv "${ops_summary}" "preflight_rc")"
ops_monitor_rc="$(kv "${ops_summary}" "monitor_rc")"
ops_reconcile_rc="$(kv "${ops_summary}" "reconcile_rc")"

idem_rc="$(run_step "02_idempotency" ./shell/payment_idempotency_regression.sh --window-hours "${WINDOW_HOURS}" --no-alert)"
idem_report="$(sed -n 's/^\[idempotency\] report=//p' "${RUN_DIR}/02_idempotency.log" | tail -n 1 || true)"
idem_summary=""
if [[ -n "${idem_report}" ]]; then
  idem_summary="$(dirname "${idem_report}")/summary.txt"
fi
idem_critical="$(kv "${idem_summary}" "critical_count")"
idem_warn="$(kv "${idem_summary}" "warn_count")"
idem_total="$(kv "${idem_summary}" "total_findings")"

daily_rc="$(run_step "03_daily_report" ./shell/payment_daily_report.sh --date "${REPORT_DATE}")"
daily_report_file="$(sed -n 's/^\[daily-report\] 生成成功: //p' "${RUN_DIR}/03_daily_report.log" | tail -n 1 || true)"

recon_summary="${ROOT_DIR}/runtime/payment_reconcile/${REPORT_DATE}/summary.txt"
recon_raw="$(kv "${recon_summary}" "main_raw_diff_count")"
recon_cleared="$(kv "${recon_summary}" "main_cleared_by_refund_count")"
recon_unresolved="$(kv "${recon_summary}" "main_diff_count")"
recon_orphan="$(kv "${recon_summary}" "orphan_wx_count")"
ticketize_summary="${ROOT_DIR}/runtime/payment_reconcile/${REPORT_DATE}/tickets/summary.txt"
ticket_total="$(kv "${ticketize_summary}" "total_tickets")"
ticket_p1="$(kv "${ticketize_summary}" "p1_count")"
ticket_p2="$(kv "${ticketize_summary}" "p2_count")"
ticket_escalated="$(kv "${ticketize_summary}" "escalated_count")"
ticket_sla_status="$(kv "${ticketize_summary}" "sla_status")"

latest_rehearsal_summary="$(resolve_summary "${ROOT_DIR}/runtime/payment_cutover_rehearsal" "recon_date" "${REPORT_DATE}")"
latest_apply_summary="$(latest_summary "${ROOT_DIR}/runtime/payment_cutover_apply")"
latest_sla_summary="$(latest_summary "${ROOT_DIR}/runtime/payment_reconcile_sla")"
latest_booking_repair_summary="$(latest_summary "${ROOT_DIR}/runtime/payment_booking_verify_repair")"
latest_decision_chain_summary="$(latest_summary "${ROOT_DIR}/runtime/payment_decision_chain_smoke")"
latest_mapping_audit_summary="$(latest_summary "${ROOT_DIR}/runtime/payment_store_mapping")"
latest_cutover_gate_summary="$(resolve_summary "${ROOT_DIR}/runtime/payment_cutover_gate" "report_date" "${REPORT_DATE}")"
if [[ -z "${latest_cutover_gate_summary}" ]]; then
  latest_cutover_gate_summary="$(latest_summary "${ROOT_DIR}/runtime/payment_cutover_gate")"
fi
rehearsal_ready="$(kv "${latest_rehearsal_summary}" "ready_for_order_drill")"
rehearsal_recon_date="$(kv "${latest_rehearsal_summary}" "recon_date")"
rehearsal_date_match=1
if [[ -z "${latest_rehearsal_summary}" || "${rehearsal_recon_date}" != "${REPORT_DATE}" ]]; then
  rehearsal_date_match=0
fi
apply_ready="$(kv "${latest_apply_summary}" "ready")"
apply_rolled_back="$(kv "${latest_apply_summary}" "rolled_back")"
sla_severity="$(kv "${latest_sla_summary}" "severity")"
sla_pending_days="$(kv "${latest_sla_summary}" "pending_days")"
sla_breach_days="$(kv "${latest_sla_summary}" "breach_days")"
booking_repair_severity="$(kv "${latest_booking_repair_summary}" "severity")"
booking_repair_before_total="$(kv "${latest_booking_repair_summary}" "before_total")"
booking_repair_after_total="$(kv "${latest_booking_repair_summary}" "after_total")"
decision_chain_severity="$(kv "${latest_decision_chain_summary}" "severity")"
decision_chain_fail_count="$(kv "${latest_decision_chain_summary}" "fail_count")"
mapping_overall="$(kv "${latest_mapping_audit_summary}" "overall")"
mapping_critical_count="$(kv "${latest_mapping_audit_summary}" "critical_count")"
mapping_warn_count="$(kv "${latest_mapping_audit_summary}" "warn_count")"
cutover_gate_overall="$(kv "${latest_cutover_gate_summary}" "overall")"
cutover_gate_decision="$(kv "${latest_cutover_gate_summary}" "gate_decision")"
cutover_gate_block_count="$(kv "${latest_cutover_gate_summary}" "block_count")"
cutover_gate_warn_count="$(kv "${latest_cutover_gate_summary}" "warn_count")"
cutover_gate_report_date="$(kv "${latest_cutover_gate_summary}" "report_date")"

latest_go_nogo_summary="$(resolve_summary "${ROOT_DIR}/runtime/payment_go_nogo" "report_date" "${REPORT_DATE}")"
go_nogo_decision="$(kv "${latest_go_nogo_summary}" "decision")"
go_nogo_blocker_count="$(kv "${latest_go_nogo_summary}" "blocker_count")"
go_nogo_gate_order_drill="$(kv "${latest_go_nogo_summary}" "gate_order_drill")"
go_nogo_gate_launch="$(kv "${latest_go_nogo_summary}" "gate_launch")"
go_nogo_report_date="$(kv "${latest_go_nogo_summary}" "report_date")"
go_nogo_date_match=1
if [[ -z "${latest_go_nogo_summary}" || "${go_nogo_report_date}" != "${REPORT_DATE}" ]]; then
  go_nogo_date_match=0
fi

cron_all="$(crontab -l 2>/dev/null || true)"
cron_managed=0
cron_task_count=0
cron_decision_chain_enabled=0
if [[ "${cron_all}" == *"# >>> payment ops managed >>>"* && "${cron_all}" == *"# <<< payment ops managed <<<"* ]]; then
  cron_managed=1
  cron_block="$(printf '%s\n' "${cron_all}" | awk '
    $0=="# >>> payment ops managed >>>" {show=1; next}
    $0=="# <<< payment ops managed <<<" {show=0; next}
    show==1 {print}
  ')"
  cron_task_count="$(printf '%s\n' "${cron_block}" | sed '/^[[:space:]]*$/d' | wc -l | tr -d ' ')"
  if printf '%s\n' "${cron_block}" | grep -q "payment_decision_chain_smoke.sh"; then
    cron_decision_chain_enabled=1
  fi
fi

recon_unresolved="${recon_unresolved:-0}"
recon_orphan="${recon_orphan:-0}"
ticket_total="${ticket_total:-0}"
ticket_p1="${ticket_p1:-0}"
ticket_p2="${ticket_p2:-0}"
ticket_escalated="${ticket_escalated:-0}"
ticket_sla_status="${ticket_sla_status:-UNKNOWN}"
idem_total="${idem_total:-0}"
rehearsal_ready="${rehearsal_ready:-0}"
rehearsal_recon_date="${rehearsal_recon_date:-}"
rehearsal_date_match="${rehearsal_date_match:-0}"
apply_ready="${apply_ready:-0}"
apply_rolled_back="${apply_rolled_back:-0}"
go_nogo_decision="${go_nogo_decision:-UNKNOWN}"
go_nogo_blocker_count="${go_nogo_blocker_count:-0}"
go_nogo_gate_order_drill="${go_nogo_gate_order_drill:-0}"
go_nogo_gate_launch="${go_nogo_gate_launch:-0}"
go_nogo_report_date="${go_nogo_report_date:-}"
go_nogo_date_match="${go_nogo_date_match:-0}"
sla_severity="${sla_severity:-UNKNOWN}"
sla_pending_days="${sla_pending_days:-0}"
sla_breach_days="${sla_breach_days:-0}"
booking_repair_severity="${booking_repair_severity:-UNKNOWN}"
booking_repair_before_total="${booking_repair_before_total:-0}"
booking_repair_after_total="${booking_repair_after_total:-0}"
decision_chain_severity="${decision_chain_severity:-UNKNOWN}"
decision_chain_fail_count="${decision_chain_fail_count:-0}"
mapping_overall="${mapping_overall:-UNKNOWN}"
mapping_critical_count="${mapping_critical_count:-0}"
mapping_warn_count="${mapping_warn_count:-0}"
cutover_gate_overall="${cutover_gate_overall:-UNKNOWN}"
cutover_gate_decision="${cutover_gate_decision:-UNKNOWN}"
cutover_gate_block_count="${cutover_gate_block_count:-0}"
cutover_gate_warn_count="${cutover_gate_warn_count:-0}"
cutover_gate_report_date="${cutover_gate_report_date:-}"

overall="GREEN"
risk_count=0

if [[ "${ops_rc}" != "0" ]]; then risk_count=$((risk_count + 1)); fi
if [[ "${idem_rc}" != "0" ]]; then risk_count=$((risk_count + 1)); fi
if [[ "${daily_rc}" != "0" ]]; then risk_count=$((risk_count + 1)); fi
if ! [[ "${recon_unresolved}" =~ ^[0-9]+$ ]]; then recon_unresolved=0; fi
if ! [[ "${recon_orphan}" =~ ^[0-9]+$ ]]; then recon_orphan=0; fi
if ! [[ "${ticket_total}" =~ ^[0-9]+$ ]]; then ticket_total=0; fi
if ! [[ "${ticket_p1}" =~ ^[0-9]+$ ]]; then ticket_p1=0; fi
if ! [[ "${ticket_p2}" =~ ^[0-9]+$ ]]; then ticket_p2=0; fi
if ! [[ "${ticket_escalated}" =~ ^[0-9]+$ ]]; then ticket_escalated=0; fi
if (( recon_unresolved > 0 || recon_orphan > 0 )); then risk_count=$((risk_count + 1)); fi
if [[ ! -f "${ticketize_summary}" ]]; then risk_count=$((risk_count + 1)); fi
if (( ticket_p1 > 0 )); then risk_count=$((risk_count + 1)); fi
if [[ "${cron_managed}" != "1" ]]; then risk_count=$((risk_count + 1)); fi
if [[ -n "${latest_go_nogo_summary}" && "${go_nogo_decision}" == "NO_GO" ]]; then risk_count=$((risk_count + 1)); fi
if [[ -n "${latest_sla_summary}" && "${sla_severity}" == "BREACH" ]]; then risk_count=$((risk_count + 1)); fi
if [[ -n "${latest_booking_repair_summary}" && ( "${booking_repair_severity}" == "PENDING" || "${booking_repair_severity}" == "ALERT" ) ]]; then risk_count=$((risk_count + 1)); fi
if [[ "${go_nogo_date_match}" != "1" ]]; then risk_count=$((risk_count + 1)); fi
if [[ "${rehearsal_date_match}" != "1" ]]; then risk_count=$((risk_count + 1)); fi
if [[ "${cron_decision_chain_enabled}" == "1" && -z "${latest_decision_chain_summary}" ]]; then risk_count=$((risk_count + 1)); fi
if [[ -n "${latest_decision_chain_summary}" && "${decision_chain_severity}" != "PASS" ]]; then risk_count=$((risk_count + 1)); fi
if ! [[ "${mapping_critical_count}" =~ ^[0-9]+$ ]]; then mapping_critical_count=0; fi
if ! [[ "${mapping_warn_count}" =~ ^[0-9]+$ ]]; then mapping_warn_count=0; fi
if ! [[ "${cutover_gate_block_count}" =~ ^[0-9]+$ ]]; then cutover_gate_block_count=0; fi
if ! [[ "${cutover_gate_warn_count}" =~ ^[0-9]+$ ]]; then cutover_gate_warn_count=0; fi
if [[ -n "${latest_mapping_audit_summary}" && "${mapping_critical_count}" -gt 0 ]]; then risk_count=$((risk_count + 1)); fi
if [[ -n "${latest_cutover_gate_summary}" && ( "${cutover_gate_decision}" == "NO_GO" || "${cutover_gate_overall}" == "RED" ) ]]; then risk_count=$((risk_count + 1)); fi
if (( risk_count > 0 )); then overall="RED"; fi

cat > "${SUMMARY_FILE}" <<TXT
run_id=${RUN_ID}
run_time=$(date '+%Y-%m-%d %H:%M:%S')
report_date=${REPORT_DATE}
window_hours=${WINDOW_HOURS}
overall=${overall}
risk_count=${risk_count}
ops_rc=${ops_rc}
ops_summary=${ops_summary}
idem_rc=${idem_rc}
idem_summary=${idem_summary}
daily_rc=${daily_rc}
daily_report_file=${daily_report_file}
recon_summary=${recon_summary}
ticketize_summary=${ticketize_summary}
ticket_total=${ticket_total}
ticket_p1=${ticket_p1}
ticket_p2=${ticket_p2}
ticket_escalated=${ticket_escalated}
ticket_sla_status=${ticket_sla_status}
latest_rehearsal_summary=${latest_rehearsal_summary}
latest_apply_summary=${latest_apply_summary}
latest_sla_summary=${latest_sla_summary}
latest_booking_repair_summary=${latest_booking_repair_summary}
latest_decision_chain_summary=${latest_decision_chain_summary}
latest_mapping_audit_summary=${latest_mapping_audit_summary}
latest_cutover_gate_summary=${latest_cutover_gate_summary}
latest_go_nogo_summary=${latest_go_nogo_summary}
go_nogo_report_date=${go_nogo_report_date}
go_nogo_date_match=${go_nogo_date_match}
go_nogo_decision=${go_nogo_decision}
go_nogo_blocker_count=${go_nogo_blocker_count}
go_nogo_gate_order_drill=${go_nogo_gate_order_drill}
go_nogo_gate_launch=${go_nogo_gate_launch}
rehearsal_recon_date=${rehearsal_recon_date}
rehearsal_date_match=${rehearsal_date_match}
sla_severity=${sla_severity}
sla_pending_days=${sla_pending_days}
sla_breach_days=${sla_breach_days}
booking_repair_severity=${booking_repair_severity}
booking_repair_before_total=${booking_repair_before_total}
booking_repair_after_total=${booking_repair_after_total}
decision_chain_severity=${decision_chain_severity}
decision_chain_fail_count=${decision_chain_fail_count}
mapping_overall=${mapping_overall}
mapping_critical_count=${mapping_critical_count}
mapping_warn_count=${mapping_warn_count}
cutover_gate_overall=${cutover_gate_overall}
cutover_gate_decision=${cutover_gate_decision}
cutover_gate_block_count=${cutover_gate_block_count}
cutover_gate_warn_count=${cutover_gate_warn_count}
cutover_gate_report_date=${cutover_gate_report_date}
cron_managed=${cron_managed}
cron_task_count=${cron_task_count}
cron_decision_chain_enabled=${cron_decision_chain_enabled}
run_dir=${RUN_DIR}
TXT

cat > "${REPORT_FILE}" <<MD
# 支付值守看板（${REPORT_DATE}）

- run_id: \`${RUN_ID}\`
- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`
- overall: **${overall}**
- risk_count: **${risk_count}**

## 一、自动检查结果

| 检查项 | rc | 关键信息 |
|---|---:|---|
| ops_daily | ${ops_rc} | severity=${ops_severity:-N/A}, preflight=${ops_preflight_rc:-N/A}, monitor=${ops_monitor_rc:-N/A}, reconcile=${ops_reconcile_rc:-N/A} |
| idempotency | ${idem_rc} | critical=${idem_critical:-0}, warn=${idem_warn:-0}, total=${idem_total:-0} |
| daily_report | ${daily_rc} | file=\`${daily_report_file:-N/A}\` |
| ticketize(${REPORT_DATE}) | N/A | total=${ticket_total}, p1=${ticket_p1}, p2=${ticket_p2}, escalated=${ticket_escalated}, sla=${ticket_sla_status} |
| go_nogo(latest) | N/A | decision=${go_nogo_decision}, blockers=${go_nogo_blocker_count}, gate_order_drill=${go_nogo_gate_order_drill}, gate_launch=${go_nogo_gate_launch}, report_date=${go_nogo_report_date}, date_match=${go_nogo_date_match} |
| reconcile_sla(latest) | N/A | severity=${sla_severity}, pending_days=${sla_pending_days}, breach_days=${sla_breach_days} |
| booking_verify_repair(latest) | N/A | severity=${booking_repair_severity}, before_total=${booking_repair_before_total}, after_total=${booking_repair_after_total} |
| decision_chain_smoke(latest) | N/A | severity=${decision_chain_severity}, fail_count=${decision_chain_fail_count} |
| mapping_audit(latest) | N/A | overall=${mapping_overall}, critical=${mapping_critical_count}, warn=${mapping_warn_count} |
| cutover_gate(latest) | N/A | decision=${cutover_gate_decision}, overall=${cutover_gate_overall}, block=${cutover_gate_block_count}, warn=${cutover_gate_warn_count}, report_date=${cutover_gate_report_date} |
| rehearsal(latest) | N/A | ready=${rehearsal_ready}, recon_date=${rehearsal_recon_date}, date_match=${rehearsal_date_match} |

## 二、对账指标（${REPORT_DATE}）

| 指标 | 数值 |
|---|---:|
| raw_diff | ${recon_raw:-N/A} |
| auto_cleared_refund | ${recon_cleared:-N/A} |
| unresolved_diff | ${recon_unresolved} |
| orphan_wx | ${recon_orphan} |
| tickets_total | ${ticket_total} |
| tickets_p1 | ${ticket_p1} |
| tickets_p2 | ${ticket_p2} |
| tickets_escalated | ${ticket_escalated} |

## 三、切换准备状态

| 项目 | 值 |
|---|---|
| latest rehearsal ready_for_order_drill | ${rehearsal_ready} |
| latest rehearsal recon_date | ${rehearsal_recon_date} |
| latest rehearsal date_match | ${rehearsal_date_match} |
| latest cutover apply ready | ${apply_ready} |
| latest cutover apply rolled_back | ${apply_rolled_back} |
| latest reconcile_sla severity | ${sla_severity} |
| latest go_nogo report_date | ${go_nogo_report_date} |
| latest go_nogo date_match | ${go_nogo_date_match} |
| latest decision_chain severity | ${decision_chain_severity} |
| latest decision_chain fail_count | ${decision_chain_fail_count} |
| latest mapping_audit overall | ${mapping_overall} |
| latest mapping_audit critical/warn | ${mapping_critical_count}/${mapping_warn_count} |
| latest cutover_gate decision | ${cutover_gate_decision} |
| latest cutover_gate overall | ${cutover_gate_overall} |
| latest cutover_gate block/warn | ${cutover_gate_block_count}/${cutover_gate_warn_count} |
| latest cutover_gate report_date | ${cutover_gate_report_date} |
| cron decision_chain enabled | ${cron_decision_chain_enabled} |
| cron managed | ${cron_managed} |
| cron task count | ${cron_task_count} |

## 四、追溯文件

- ops summary: \`${ops_summary}\`
- idempotency summary: \`${idem_summary}\`
- reconcile summary: \`${recon_summary}\`
- ticketize summary: \`${ticketize_summary}\`
- latest rehearsal summary: \`${latest_rehearsal_summary}\`
- latest apply summary: \`${latest_apply_summary}\`
- latest reconcile_sla summary: \`${latest_sla_summary}\`
- latest booking_repair summary: \`${latest_booking_repair_summary}\`
- latest decision_chain summary: \`${latest_decision_chain_summary}\`
- latest mapping_audit summary: \`${latest_mapping_audit_summary}\`
- latest cutover_gate summary: \`${latest_cutover_gate_summary}\`
- latest go_nogo summary: \`${latest_go_nogo_summary}\`
- dashboard summary: \`${SUMMARY_FILE}\`
MD

echo "[warroom] dashboard=${REPORT_FILE}"
echo "[warroom] overall=${overall}, risk_count=${risk_count}"

if [[ "${overall}" != "GREEN" ]]; then
  if [[ ${NO_ALERT} -eq 0 ]] && [[ -x "${ALERT_SCRIPT}" ]]; then
    "${ALERT_SCRIPT}" \
      --title "支付值守看板告警" \
      --content "date=${REPORT_DATE}; overall=${overall}; risk_count=${risk_count}; ops_rc=${ops_rc}; idempotency_rc=${idem_rc}; daily_rc=${daily_rc}; unresolved=${recon_unresolved}; orphan=${recon_orphan}; ticket_p1=${ticket_p1}; ticket_total=${ticket_total}; booking_repair=${booking_repair_severity}; decision_chain=${decision_chain_severity}; mapping_overall=${mapping_overall}; cutover_gate=${cutover_gate_decision}/${cutover_gate_overall}; go_nogo_date_match=${go_nogo_date_match}; rehearsal_date_match=${rehearsal_date_match}; dashboard=${REPORT_FILE}" || true
  fi
  exit 2
fi

exit 0
