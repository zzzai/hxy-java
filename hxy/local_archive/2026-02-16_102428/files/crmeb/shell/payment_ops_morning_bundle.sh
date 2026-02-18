#!/usr/bin/env bash
set -euo pipefail

# 支付早班一键包：
# ops_daily -> go_nogo -> warroom -> booking_verify_regression -> booking_verify_repair
# 统一输出一份值守结论，适合“无订单号阶段”快速判断当天是否可继续联调。

REPORT_DATE="${REPORT_DATE:-}"
WINDOW_MINUTES="${WINDOW_MINUTES:-15}"
TAIL_LINES="${TAIL_LINES:-3000}"
WINDOW_HOURS="${WINDOW_HOURS:-72}"
REQUIRE_APPLY_READY="${REQUIRE_APPLY_READY:-0}"
REQUIRE_BOOKING_REPAIR_PASS="${REQUIRE_BOOKING_REPAIR_PASS:-0}"
SLA_LOOKBACK_DAYS="${SLA_LOOKBACK_DAYS:-14}"
SLA_DAYS="${SLA_DAYS:-1}"
STRICT_PREFLIGHT=0
NO_ALERT=0
OUT_DIR="${OUT_DIR:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_ops_morning_bundle.sh [--date YYYY-MM-DD] [--window N] [--tail N] [--window-hours N] [--require-apply-ready 0|1] [--require-booking-repair-pass 0|1] [--sla-lookback-days N] [--sla-days N] [--strict-preflight] [--no-alert] [--out-dir PATH]

参数：
  --date YYYY-MM-DD            对账/看板日期（默认昨天）
  --window N                   ops_daily 监控窗口分钟（默认 15）
  --tail N                     ops_daily 日志尾行数（默认 3000）
  --window-hours N             warroom 幂等回归窗口小时（默认 72）
  --require-apply-ready 0|1    go_nogo 是否要求 apply-ready（默认 0）
  --require-booking-repair-pass 0|1  go_nogo 是否要求 booking_verify_repair=PASS（默认 0）
  --sla-lookback-days N        对账SLA回看天数（默认 14）
  --sla-days N                 对账SLA允许天数（默认 1）
  --strict-preflight           ops_daily 开启 strict preflight
  --no-alert                   不推送汇总告警
  --out-dir PATH               输出目录（默认 runtime/payment_ops_morning_bundle）

退出码：
  0  全部通过
  2  存在告警项（可继续人工研判）
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --date)
      REPORT_DATE="$2"
      shift 2
      ;;
    --window)
      WINDOW_MINUTES="$2"
      shift 2
      ;;
    --tail)
      TAIL_LINES="$2"
      shift 2
      ;;
    --window-hours)
      WINDOW_HOURS="$2"
      shift 2
      ;;
    --require-apply-ready)
      REQUIRE_APPLY_READY="$2"
      shift 2
      ;;
    --require-booking-repair-pass)
      REQUIRE_BOOKING_REPAIR_PASS="$2"
      shift 2
      ;;
    --sla-lookback-days)
      SLA_LOOKBACK_DAYS="$2"
      shift 2
      ;;
    --sla-days)
      SLA_DAYS="$2"
      shift 2
      ;;
    --strict-preflight)
      STRICT_PREFLIGHT=1
      shift
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
if ! [[ "${WINDOW_MINUTES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --window 必须是正整数"
  exit 1
fi
if ! [[ "${TAIL_LINES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --tail 必须是正整数"
  exit 1
fi
if ! [[ "${WINDOW_HOURS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --window-hours 必须是正整数"
  exit 1
fi
if [[ "${REQUIRE_APPLY_READY}" != "0" && "${REQUIRE_APPLY_READY}" != "1" ]]; then
  echo "参数错误: --require-apply-ready 仅支持 0 或 1"
  exit 1
fi
if [[ "${REQUIRE_BOOKING_REPAIR_PASS}" != "0" && "${REQUIRE_BOOKING_REPAIR_PASS}" != "1" ]]; then
  echo "参数错误: --require-booking-repair-pass 仅支持 0 或 1"
  exit 1
fi
if ! [[ "${SLA_LOOKBACK_DAYS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --sla-lookback-days 必须是正整数"
  exit 1
fi
if ! [[ "${SLA_DAYS}" =~ ^[0-9]+$ ]]; then
  echo "参数错误: --sla-days 必须是非负整数"
  exit 1
fi

if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${ROOT_DIR}/runtime/payment_ops_morning_bundle"
fi
RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"
SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"

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

echo "[morning-bundle] run_dir=${RUN_DIR}"
echo "[morning-bundle] date=${REPORT_DATE}, window=${WINDOW_MINUTES}, tail=${TAIL_LINES}, window_hours=${WINDOW_HOURS}, require_booking_repair_pass=${REQUIRE_BOOKING_REPAIR_PASS}, sla_lookback_days=${SLA_LOOKBACK_DAYS}, sla_days=${SLA_DAYS}"

ops_args=(
  ./shell/payment_ops_daily.sh
  --date "${REPORT_DATE}"
  --window "${WINDOW_MINUTES}"
  --tail "${TAIL_LINES}"
  --output-dir "${RUN_DIR}/ops_daily"
  --no-alert
)
if [[ ${STRICT_PREFLIGHT} -eq 1 ]]; then
  ops_args+=(--strict-preflight)
fi
ops_rc="$(run_step "01_ops_daily" "${ops_args[@]}")"
ops_summary="$(sed -n 's/.*summary=//p' "${RUN_DIR}/01_ops_daily.log" | tail -n 1 || true)"
ops_severity="$(kv "${ops_summary}" "severity")"
ops_preflight_rc="$(kv "${ops_summary}" "preflight_rc")"
ops_monitor_rc="$(kv "${ops_summary}" "monitor_rc")"
ops_reconcile_rc="$(kv "${ops_summary}" "reconcile_rc")"

gonogo_rc="$(run_step "02_go_nogo" ./shell/payment_go_nogo_decision.sh --date "${REPORT_DATE}" --require-apply-ready "${REQUIRE_APPLY_READY}" --require-booking-repair-pass "${REQUIRE_BOOKING_REPAIR_PASS}" --no-alert)"
gonogo_summary="$(sed -n 's/^\[go-nogo\] summary=//p' "${RUN_DIR}/02_go_nogo.log" | tail -n 1 || true)"
gonogo_decision="$(kv "${gonogo_summary}" "decision")"
gonogo_blocker_count="$(kv "${gonogo_summary}" "blocker_count")"
gonogo_gate_order_drill="$(kv "${gonogo_summary}" "gate_order_drill")"
gonogo_gate_launch="$(kv "${gonogo_summary}" "gate_launch")"

warroom_rc="$(run_step "03_warroom" ./shell/payment_warroom_dashboard.sh --date "${REPORT_DATE}" --window-hours "${WINDOW_HOURS}" --out-dir "${RUN_DIR}/warroom" --no-alert)"
warroom_summary="$(sed -n 's/^\[warroom\] summary=//p' "${RUN_DIR}/03_warroom.log" | tail -n 1 || true)"
if [[ -z "${warroom_summary}" ]]; then
  warroom_dashboard="$(sed -n 's/^\[warroom\] dashboard=//p' "${RUN_DIR}/03_warroom.log" | tail -n 1 || true)"
  if [[ -n "${warroom_dashboard}" ]]; then
    warroom_summary="$(dirname "${warroom_dashboard}")/summary.txt"
  fi
fi
if [[ -z "${warroom_summary}" ]]; then
  warroom_summary="${RUN_DIR}/warroom/run-${RUN_ID}/summary.txt"
fi
warroom_overall="$(kv "${warroom_summary}" "overall")"
warroom_risk_count="$(kv "${warroom_summary}" "risk_count")"

sla_rc="$(run_step "04_reconcile_sla" ./shell/payment_reconcile_sla_guard.sh --lookback-days "${SLA_LOOKBACK_DAYS}" --sla-days "${SLA_DAYS}" --no-alert)"
sla_summary="$(sed -n 's/^\[reconcile-sla\] summary=//p' "${RUN_DIR}/04_reconcile_sla.log" | tail -n 1 || true)"
sla_severity="$(kv "${sla_summary}" "severity")"
sla_issue_days="$(kv "${sla_summary}" "issue_days")"
sla_pending_days="$(kv "${sla_summary}" "pending_days")"
sla_breach_days="$(kv "${sla_summary}" "breach_days")"

booking_verify_rc="$(run_step "05_booking_verify_regression" ./shell/payment_booking_verify_regression.sh --window-hours "${WINDOW_HOURS}" --output-dir "${RUN_DIR}/booking_verify" --no-alert)"
booking_verify_summary="$(find "${RUN_DIR}/booking_verify" -maxdepth 2 -type f -name summary.txt 2>/dev/null | sort | tail -n 1 || true)"
booking_verify_severity="$(kv "${booking_verify_summary}" "severity")"
booking_verify_critical_count="$(kv "${booking_verify_summary}" "critical_count")"
booking_verify_warn_count="$(kv "${booking_verify_summary}" "warn_count")"

booking_repair_rc="$(run_step "06_booking_verify_repair" ./shell/payment_booking_verify_repair.sh --window-hours "${WINDOW_HOURS}" --output-dir "${RUN_DIR}/booking_repair" --no-alert)"
booking_repair_summary="$(find "${RUN_DIR}/booking_repair" -maxdepth 2 -type f -name summary.txt 2>/dev/null | sort | tail -n 1 || true)"
booking_repair_severity="$(kv "${booking_repair_summary}" "severity")"
booking_repair_before_total="$(kv "${booking_repair_summary}" "before_total")"
booking_repair_after_total="$(kv "${booking_repair_summary}" "after_total")"

severity="OK"
exit_code=0
if [[ "${ops_rc}" == "1" || "${gonogo_rc}" == "1" || "${warroom_rc}" == "1" || "${sla_rc}" == "1" || "${booking_verify_rc}" == "1" || "${booking_repair_rc}" == "1" ]]; then
  severity="ERROR"
  exit_code=1
elif [[ "${ops_rc}" == "2" || "${gonogo_rc}" == "2" || "${warroom_rc}" == "2" || "${sla_rc}" == "2" || "${booking_verify_rc}" == "2" || "${booking_repair_rc}" == "2" ]]; then
  severity="ALERT"
  exit_code=2
fi

cat > "${SUMMARY_FILE}" <<TXT
run_id=${RUN_ID}
run_time=$(date '+%Y-%m-%d %H:%M:%S')
report_date=${REPORT_DATE}
window_minutes=${WINDOW_MINUTES}
tail_lines=${TAIL_LINES}
window_hours=${WINDOW_HOURS}
require_apply_ready=${REQUIRE_APPLY_READY}
require_booking_repair_pass=${REQUIRE_BOOKING_REPAIR_PASS}
sla_lookback_days=${SLA_LOOKBACK_DAYS}
sla_days=${SLA_DAYS}
strict_preflight=${STRICT_PREFLIGHT}
severity=${severity}
ops_rc=${ops_rc}
ops_summary=${ops_summary}
gonogo_rc=${gonogo_rc}
gonogo_summary=${gonogo_summary}
gonogo_decision=${gonogo_decision}
gonogo_blocker_count=${gonogo_blocker_count}
gonogo_gate_order_drill=${gonogo_gate_order_drill}
gonogo_gate_launch=${gonogo_gate_launch}
warroom_rc=${warroom_rc}
warroom_summary=${warroom_summary}
warroom_overall=${warroom_overall}
warroom_risk_count=${warroom_risk_count}
sla_rc=${sla_rc}
sla_summary=${sla_summary}
sla_severity=${sla_severity}
sla_issue_days=${sla_issue_days}
sla_pending_days=${sla_pending_days}
sla_breach_days=${sla_breach_days}
booking_verify_rc=${booking_verify_rc}
booking_verify_summary=${booking_verify_summary}
booking_verify_severity=${booking_verify_severity}
booking_verify_critical_count=${booking_verify_critical_count}
booking_verify_warn_count=${booking_verify_warn_count}
booking_repair_rc=${booking_repair_rc}
booking_repair_summary=${booking_repair_summary}
booking_repair_severity=${booking_repair_severity}
booking_repair_before_total=${booking_repair_before_total}
booking_repair_after_total=${booking_repair_after_total}
run_dir=${RUN_DIR}
TXT

cat > "${REPORT_FILE}" <<MD
# 支付早班一键包报告（${REPORT_DATE}）

- run_id: \`${RUN_ID}\`
- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`
- severity: **${severity}**
- require_apply_ready: \`${REQUIRE_APPLY_READY}\`

## 检查项

| 检查项 | rc | 关键信息 |
|---|---:|---|
| ops_daily | ${ops_rc} | severity=${ops_severity:-N/A}, preflight=${ops_preflight_rc:-N/A}, monitor=${ops_monitor_rc:-N/A}, reconcile=${ops_reconcile_rc:-N/A} |
| go_nogo | ${gonogo_rc} | decision=${gonogo_decision:-N/A}, blockers=${gonogo_blocker_count:-N/A}, gate_order_drill=${gonogo_gate_order_drill:-N/A}, gate_launch=${gonogo_gate_launch:-N/A} |
| warroom | ${warroom_rc} | overall=${warroom_overall:-N/A}, risk_count=${warroom_risk_count:-N/A} |
| reconcile_sla | ${sla_rc} | severity=${sla_severity:-N/A}, issue_days=${sla_issue_days:-N/A}, pending_days=${sla_pending_days:-N/A}, breach_days=${sla_breach_days:-N/A}, sla_days=${SLA_DAYS} |
| booking_verify_regression | ${booking_verify_rc} | severity=${booking_verify_severity:-N/A}, critical=${booking_verify_critical_count:-N/A}, warn=${booking_verify_warn_count:-N/A} |
| booking_verify_repair | ${booking_repair_rc} | severity=${booking_repair_severity:-N/A}, before_total=${booking_repair_before_total:-N/A}, after_total=${booking_repair_after_total:-N/A} |

## 追溯文件

- summary: \`${SUMMARY_FILE}\`
- ops_summary: \`${ops_summary}\`
- go_nogo_summary: \`${gonogo_summary}\`
- warroom_summary: \`${warroom_summary}\`
- reconcile_sla_summary: \`${sla_summary}\`
- booking_verify_summary: \`${booking_verify_summary}\`
- booking_repair_summary: \`${booking_repair_summary}\`
MD

echo "[morning-bundle] summary=${SUMMARY_FILE}"
echo "[morning-bundle] report=${REPORT_FILE}"
echo "[morning-bundle] severity=${severity}, ops_rc=${ops_rc}, gonogo_rc=${gonogo_rc}, warroom_rc=${warroom_rc}, sla_rc=${sla_rc}, booking_verify_rc=${booking_verify_rc}, booking_repair_rc=${booking_repair_rc}"

if (( exit_code != 0 )) && [[ ${NO_ALERT} -eq 0 ]] && [[ -x "${ALERT_SCRIPT}" ]]; then
  "${ALERT_SCRIPT}" \
    --title "支付早班一键包告警" \
    --content "severity=${severity}; date=${REPORT_DATE}; ops_rc=${ops_rc}; go_nogo_rc=${gonogo_rc}; warroom_rc=${warroom_rc}; sla_rc=${sla_rc}; booking_verify_rc=${booking_verify_rc}; booking_repair_rc=${booking_repair_rc}; report=${REPORT_FILE}" || true
fi

exit "${exit_code}"
