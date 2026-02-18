#!/usr/bin/env bash
set -euo pipefail

# D15: 一键值守状态总览（无订单号阶段）
# 仅聚合最新运行产物，不触发实际支付动作。

REPORT_DATE="${REPORT_DATE:-}"
NO_ALERT=0
REFRESH=0
REFRESH_ORDER_NO="${REFRESH_ORDER_NO:-}"
REFRESH_REQUIRE_APPLY_READY="${REFRESH_REQUIRE_APPLY_READY:-0}"
REFRESH_WINDOW_HOURS="${REFRESH_WINDOW_HOURS:-72}"
REQUIRE_BOOKING_REPAIR_PASS="${REQUIRE_BOOKING_REPAIR_PASS:-0}"
REQUIRE_DECISION_CHAIN_PASS="${REQUIRE_DECISION_CHAIN_PASS:-0}"
SLA_LOOKBACK_DAYS="${SLA_LOOKBACK_DAYS:-14}"
SLA_DAYS="${SLA_DAYS:-1}"
REFUND_WINDOW_HOURS="${REFUND_WINDOW_HOURS:-72}"
REFUND_TIMEOUT_MINUTES="${REFUND_TIMEOUT_MINUTES:-30}"
OUT_DIR="${OUT_DIR:-}"
RUNTIME_ROOT="${RUNTIME_ROOT:-}"
MAX_SUMMARY_AGE_MINUTES="${MAX_SUMMARY_AGE_MINUTES:-240}"
MAX_RECON_AGE_DAYS="${MAX_RECON_AGE_DAYS:-2}"
MAX_DAILY_REPORT_AGE_DAYS="${MAX_DAILY_REPORT_AGE_DAYS:-2}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_ops_status.sh [--date YYYY-MM-DD] [--refresh] [--refresh-order-no ORDER_NO] [--refresh-require-apply-ready 0|1] [--refresh-window-hours N] [--refund-window-hours N] [--refund-timeout-minutes N] [--require-booking-repair-pass 0|1] [--require-decision-chain-pass 0|1] [--sla-lookback-days N] [--sla-days N] [--runtime-root PATH] [--max-summary-age-minutes N] [--max-recon-age-days N] [--max-daily-report-age-days N] [--no-alert] [--out-dir PATH]

参数：
  --date YYYY-MM-DD                    状态日期（默认昨天）
  --refresh                            先执行一轮 morning-bundle 再汇总
  --refresh-order-no ORDER_NO          refresh 时透传给 morning-bundle/go_nogo 的订单号（可选）
  --refresh-require-apply-ready 0|1    refresh 时传给 morning-bundle 的 apply-ready 门槛（默认 0）
  --refresh-window-hours N             refresh 时 warroom/rehearsal/booking 统一窗口小时（默认 72）
  --refund-window-hours N              refund_convergence 检查窗口（小时，默认 72）
  --refund-timeout-minutes N           refund_convergence 超时阈值（分钟，默认 30）
  --require-booking-repair-pass 0|1    值守判定是否要求 booking_verify_repair=PASS（默认 0）
  --require-decision-chain-pass 0|1    值守判定是否要求 decision_chain_smoke=PASS（默认 0）
  --sla-lookback-days N                reconcile_sla 回看天数（默认 14）
  --sla-days N                         reconcile_sla 允许天数（默认 1）
  --runtime-root PATH                  runtime 根目录（默认 <repo>/runtime）
  --max-summary-age-minutes N          latest summary 允许最大“新鲜度”（分钟，默认 240）
  --max-recon-age-days N               对账 summary 文件允许最大“新鲜度”（天，默认 2）
  --max-daily-report-age-days N        日报文件允许最大“新鲜度”（天，默认 2）
  --no-alert                           总览非 GREEN 时不推送告警
  --out-dir PATH                       输出目录（默认 runtime/payment_ops_status）

退出码：
  0  overall=GREEN
  2  overall=YELLOW/RED
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --date)
      REPORT_DATE="$2"
      shift 2
      ;;
    --refresh)
      REFRESH=1
      shift
      ;;
    --refresh-order-no)
      REFRESH_ORDER_NO="$2"
      shift 2
      ;;
    --refresh-require-apply-ready)
      REFRESH_REQUIRE_APPLY_READY="$2"
      shift 2
      ;;
    --refresh-window-hours)
      REFRESH_WINDOW_HOURS="$2"
      shift 2
      ;;
    --refund-window-hours)
      REFUND_WINDOW_HOURS="$2"
      shift 2
      ;;
    --refund-timeout-minutes)
      REFUND_TIMEOUT_MINUTES="$2"
      shift 2
      ;;
    --require-booking-repair-pass)
      REQUIRE_BOOKING_REPAIR_PASS="$2"
      shift 2
      ;;
    --require-decision-chain-pass)
      REQUIRE_DECISION_CHAIN_PASS="$2"
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
    --runtime-root)
      RUNTIME_ROOT="$2"
      shift 2
      ;;
    --max-summary-age-minutes)
      MAX_SUMMARY_AGE_MINUTES="$2"
      shift 2
      ;;
    --max-recon-age-days)
      MAX_RECON_AGE_DAYS="$2"
      shift 2
      ;;
    --max-daily-report-age-days)
      MAX_DAILY_REPORT_AGE_DAYS="$2"
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
if [[ "${REFRESH_REQUIRE_APPLY_READY}" != "0" && "${REFRESH_REQUIRE_APPLY_READY}" != "1" ]]; then
  echo "参数错误: --refresh-require-apply-ready 仅支持 0 或 1"
  exit 1
fi
if ! [[ "${REFRESH_WINDOW_HOURS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --refresh-window-hours 必须是正整数"
  exit 1
fi
if [[ "${REQUIRE_BOOKING_REPAIR_PASS}" != "0" && "${REQUIRE_BOOKING_REPAIR_PASS}" != "1" ]]; then
  echo "参数错误: --require-booking-repair-pass 仅支持 0 或 1"
  exit 1
fi
if [[ "${REQUIRE_DECISION_CHAIN_PASS}" != "0" && "${REQUIRE_DECISION_CHAIN_PASS}" != "1" ]]; then
  echo "参数错误: --require-decision-chain-pass 仅支持 0 或 1"
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
if ! [[ "${REFUND_WINDOW_HOURS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: REFUND_WINDOW_HOURS 必须是正整数"
  exit 1
fi
if ! [[ "${REFUND_TIMEOUT_MINUTES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: REFUND_TIMEOUT_MINUTES 必须是正整数"
  exit 1
fi
if ! [[ "${MAX_SUMMARY_AGE_MINUTES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --max-summary-age-minutes 必须是正整数"
  exit 1
fi
if ! [[ "${MAX_RECON_AGE_DAYS}" =~ ^[0-9]+$ ]]; then
  echo "参数错误: --max-recon-age-days 必须是非负整数"
  exit 1
fi
if ! [[ "${MAX_DAILY_REPORT_AGE_DAYS}" =~ ^[0-9]+$ ]]; then
  echo "参数错误: --max-daily-report-age-days 必须是非负整数"
  exit 1
fi
if [[ -n "${RUNTIME_ROOT}" && ! -d "${RUNTIME_ROOT}" ]]; then
  echo "参数错误: --runtime-root 目录不存在 -> ${RUNTIME_ROOT}"
  exit 1
fi

if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${ROOT_DIR}/runtime/payment_ops_status"
fi
if [[ -z "${RUNTIME_ROOT}" ]]; then
  RUNTIME_ROOT="${ROOT_DIR}/runtime"
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

file_age_minutes() {
  local file="$1"
  if [[ -z "${file}" || ! -f "${file}" ]]; then
    printf -- "-1"
    return
  fi
  local now_ts mtime age
  now_ts="$(date +%s)"
  mtime="$(stat -c %Y "${file}" 2>/dev/null || echo "")"
  if [[ -z "${mtime}" || ! "${mtime}" =~ ^[0-9]+$ ]]; then
    printf -- "-1"
    return
  fi
  age=$(( (now_ts - mtime) / 60 ))
  if (( age < 0 )); then
    age=0
  fi
  printf '%s' "${age}"
}

is_positive_int() {
  [[ "$1" =~ ^[0-9]+$ ]]
}

warn_reasons=()
block_reasons=()

add_warn() {
  warn_reasons+=("$1")
}

add_block() {
  block_reasons+=("$1")
}

echo "[ops-status] run_dir=${RUN_DIR}"
echo "[ops-status] report_date=${REPORT_DATE}, refresh=${REFRESH}, refresh_window_hours=${REFRESH_WINDOW_HOURS}, refund_window_hours=${REFUND_WINDOW_HOURS}, refund_timeout_minutes=${REFUND_TIMEOUT_MINUTES}, require_booking_repair_pass=${REQUIRE_BOOKING_REPAIR_PASS}, require_decision_chain_pass=${REQUIRE_DECISION_CHAIN_PASS}, runtime_root=${RUNTIME_ROOT}"

refresh_rc="-"
refresh_summary=""
refresh_sla_rc="-"
refresh_sla_summary=""
refresh_booking_verify_rc="-"
refresh_booking_verify_summary=""
refresh_booking_repair_rc="-"
refresh_booking_repair_summary=""
refresh_decision_chain_rc="-"
refresh_decision_chain_summary=""
refresh_exception_rc="-"
refresh_exception_summary=""
refresh_refund_convergence_rc="-"
refresh_refund_convergence_summary=""
refresh_warroom_rc="-"
refresh_warroom_summary=""
refresh_rehearsal_rc="-"
refresh_rehearsal_summary=""
refresh_mapping_smoke_rc="-"
refresh_mapping_smoke_summary=""
if [[ ${REFRESH} -eq 1 ]]; then
  refresh_args=(
    ./shell/payment_ops_morning_bundle.sh
    --date "${REPORT_DATE}"
    --require-apply-ready "${REFRESH_REQUIRE_APPLY_READY}"
    --require-booking-repair-pass "${REQUIRE_BOOKING_REPAIR_PASS}"
    --refund-window-hours "${REFUND_WINDOW_HOURS}"
    --refund-timeout-minutes "${REFUND_TIMEOUT_MINUTES}"
    --no-alert
  )
  if [[ -n "${REFRESH_ORDER_NO}" ]]; then
    refresh_args+=(--order-no "${REFRESH_ORDER_NO}")
  fi
  set +e
  "${refresh_args[@]}" > "${RUN_DIR}/01_refresh.log" 2>&1
  refresh_rc=$?
  set -e
  refresh_summary="$(sed -n 's/^\[morning-bundle\] summary=//p' "${RUN_DIR}/01_refresh.log" | tail -n 1 || true)"
  if [[ "${refresh_rc}" == "1" ]]; then
    add_block "refresh 执行失败(rc=1)"
  elif [[ "${refresh_rc}" == "2" ]]; then
    add_warn "refresh 返回告警(rc=2)"
  fi

  set +e
  ./shell/payment_reconcile_sla_guard.sh \
    --lookback-days "${SLA_LOOKBACK_DAYS}" \
    --sla-days "${SLA_DAYS}" \
    --no-alert > "${RUN_DIR}/01b_refresh_sla.log" 2>&1
  refresh_sla_rc=$?
  set -e
  refresh_sla_summary="$(sed -n 's/^\[reconcile-sla\] summary=//p' "${RUN_DIR}/01b_refresh_sla.log" | tail -n 1 || true)"
  if [[ "${refresh_sla_rc}" == "1" ]]; then
    add_block "refresh_sla 执行失败(rc=1)"
  elif [[ "${refresh_sla_rc}" == "2" ]]; then
    add_warn "refresh_sla 返回告警(rc=2)"
  fi

  set +e
  ./shell/payment_booking_verify_regression.sh \
    --window-hours 72 \
    --no-alert > "${RUN_DIR}/01c_refresh_booking_verify.log" 2>&1
  refresh_booking_verify_rc=$?
  set -e
  refresh_booking_verify_summary="$(sed -n 's/^\[booking-verify-regression\] summary=//p' "${RUN_DIR}/01c_refresh_booking_verify.log" | tail -n 1 || true)"
  if [[ "${refresh_booking_verify_rc}" == "1" ]]; then
    add_block "refresh_booking_verify 执行失败(rc=1)"
  elif [[ "${refresh_booking_verify_rc}" == "2" ]]; then
    add_warn "refresh_booking_verify 返回告警(rc=2)"
  fi

  set +e
  ./shell/payment_booking_verify_repair.sh \
    --window-hours "${REFRESH_WINDOW_HOURS}" \
    --output-dir "${RUN_DIR}/refresh_booking_repair" \
    --no-alert > "${RUN_DIR}/01c2_refresh_booking_repair.log" 2>&1
  refresh_booking_repair_rc=$?
  set -e
  refresh_booking_repair_summary="$(find "${RUN_DIR}/refresh_booking_repair" -maxdepth 2 -type f -name summary.txt 2>/dev/null | sort | tail -n 1 || true)"
  if [[ "${refresh_booking_repair_rc}" == "1" ]]; then
    add_block "refresh_booking_repair 执行失败(rc=1)"
  elif [[ "${refresh_booking_repair_rc}" == "2" ]]; then
    add_warn "refresh_booking_repair 返回告警(rc=2)"
  fi

  if [[ "${REQUIRE_DECISION_CHAIN_PASS}" == "1" ]]; then
    set +e
    ./shell/payment_decision_chain_smoke.sh \
      --date "${REPORT_DATE}" > "${RUN_DIR}/01d_refresh_decision_chain.log" 2>&1
    refresh_decision_chain_rc=$?
    set -e
    refresh_decision_chain_summary="$(sed -n 's/^\[decision-chain-smoke\] summary=//p' "${RUN_DIR}/01d_refresh_decision_chain.log" | tail -n 1 || true)"
    if [[ "${refresh_decision_chain_rc}" != "0" ]]; then
      add_block "refresh_decision_chain 执行失败(rc=${refresh_decision_chain_rc})"
    fi
  fi

  set +e
  ./shell/payment_exception_acceptance.sh \
    --date "${REPORT_DATE}" \
    --window-hours 72 \
    --no-alert > "${RUN_DIR}/01e_refresh_exception.log" 2>&1
  refresh_exception_rc=$?
  set -e
  refresh_exception_summary="$(sed -n 's/^\[payment-exception-acceptance\] summary=//p' "${RUN_DIR}/01e_refresh_exception.log" | tail -n 1 || true)"
  if [[ "${refresh_exception_rc}" == "1" ]]; then
    add_block "refresh_exception 执行失败(rc=1)"
  elif [[ "${refresh_exception_rc}" == "2" ]]; then
    add_warn "refresh_exception 返回风险(rc=2)"
  fi

  set +e
  ./shell/payment_refund_convergence_check.sh \
    --window-hours "${REFUND_WINDOW_HOURS}" \
    --refund-timeout-minutes "${REFUND_TIMEOUT_MINUTES}" \
    --out-dir "${RUN_DIR}/refresh_refund_convergence" \
    --no-alert > "${RUN_DIR}/01f_refresh_refund_convergence.log" 2>&1
  refresh_refund_convergence_rc=$?
  set -e
  refresh_refund_convergence_summary="$(latest_summary "${RUN_DIR}/refresh_refund_convergence")"
  if [[ "${refresh_refund_convergence_rc}" == "1" ]]; then
    add_block "refresh_refund_convergence 执行失败(rc=1)"
  elif [[ "${refresh_refund_convergence_rc}" == "2" ]]; then
    add_block "refresh_refund_convergence 存在阻断(rc=2)"
  fi

  set +e
  ./shell/payment_warroom_dashboard.sh \
    --date "${REPORT_DATE}" \
    --window-hours "${REFRESH_WINDOW_HOURS}" \
    --out-dir "${RUN_DIR}/refresh_warroom" \
    --no-alert > "${RUN_DIR}/01g_refresh_warroom.log" 2>&1
  refresh_warroom_rc=$?
  set -e
  refresh_warroom_summary="$(latest_summary "${RUN_DIR}/refresh_warroom")"
  if [[ "${refresh_warroom_rc}" == "1" ]]; then
    add_block "refresh_warroom 执行失败(rc=1)"
  elif [[ "${refresh_warroom_rc}" == "2" ]]; then
    add_warn "refresh_warroom 返回告警(rc=2)"
  fi

  set +e
  refresh_rehearsal_cmd=(
    ./shell/payment_cutover_rehearsal.sh
    --date "${REPORT_DATE}"
    --window-hours "${REFRESH_WINDOW_HOURS}"
    --out-dir "${RUN_DIR}/refresh_rehearsal"
    --no-alert
  )
  # 非 apply-ready 阶段，预演采用 non-strict 以避免 WARN 造成值守黄灯噪音。
  if [[ "${REFRESH_REQUIRE_APPLY_READY}" == "0" ]]; then
    refresh_rehearsal_cmd+=(--non-strict-preflight)
  fi
  "${refresh_rehearsal_cmd[@]}" > "${RUN_DIR}/01h_refresh_rehearsal.log" 2>&1
  refresh_rehearsal_rc=$?
  set -e
  refresh_rehearsal_summary="$(latest_summary "${RUN_DIR}/refresh_rehearsal")"
  if [[ "${refresh_rehearsal_rc}" == "1" ]]; then
    add_block "refresh_rehearsal 执行失败(rc=1)"
  elif [[ "${refresh_rehearsal_rc}" == "2" ]]; then
    add_warn "refresh_rehearsal 返回告警(rc=2)"
  fi

  set +e
  ./shell/payment_store_mapping_pipeline_smoke.sh \
    --out-dir "${RUN_DIR}/refresh_mapping_smoke" > "${RUN_DIR}/01i_refresh_mapping_smoke.log" 2>&1
  refresh_mapping_smoke_rc=$?
  set -e
  refresh_mapping_smoke_summary="$(latest_summary "${RUN_DIR}/refresh_mapping_smoke")"
  if [[ "${refresh_mapping_smoke_rc}" == "1" ]]; then
    add_block "refresh_mapping_smoke 执行失败(rc=1)"
  elif [[ "${refresh_mapping_smoke_rc}" == "2" ]]; then
    add_warn "refresh_mapping_smoke 返回告警(rc=2)"
  fi
fi

ops_summary="$(resolve_summary "${RUNTIME_ROOT}/payment_ops_daily" "recon_date" "${REPORT_DATE}")"
morning_summary="$(resolve_summary "${RUNTIME_ROOT}/payment_ops_morning_bundle" "report_date" "${REPORT_DATE}")"
warroom_summary="$(resolve_summary "${RUNTIME_ROOT}/payment_warroom" "report_date" "${REPORT_DATE}")"
gonogo_summary="$(resolve_summary "${RUNTIME_ROOT}/payment_go_nogo" "report_date" "${REPORT_DATE}")"
rehearsal_summary="$(resolve_summary "${RUNTIME_ROOT}/payment_cutover_rehearsal" "recon_date" "${REPORT_DATE}")"
apply_summary="$(latest_summary "${RUNTIME_ROOT}/payment_cutover_apply")"
idem_summary="$(latest_summary "${RUNTIME_ROOT}/payment_idempotency_regression")"
sla_summary="$(latest_summary "${RUNTIME_ROOT}/payment_reconcile_sla")"
booking_verify_summary="$(latest_summary "${RUNTIME_ROOT}/payment_booking_verify_regression")"
booking_repair_summary="$(latest_summary "${RUNTIME_ROOT}/payment_booking_verify_repair")"
decision_chain_summary="$(latest_summary "${RUNTIME_ROOT}/payment_decision_chain_smoke")"
mapping_smoke_summary="$(latest_summary "${RUNTIME_ROOT}/payment_store_mapping_smoke")"
exception_summary="$(latest_summary "${RUNTIME_ROOT}/payment_exception_acceptance")"
refund_convergence_summary="$(latest_summary "${RUNTIME_ROOT}/payment_refund_convergence")"
if [[ -n "${refresh_booking_verify_summary}" ]]; then
  booking_verify_summary="${refresh_booking_verify_summary}"
fi
if [[ -n "${refresh_booking_repair_summary}" ]]; then
  booking_repair_summary="${refresh_booking_repair_summary}"
fi
if [[ -n "${refresh_decision_chain_summary}" ]]; then
  decision_chain_summary="${refresh_decision_chain_summary}"
fi
if [[ -n "${refresh_exception_summary}" ]]; then
  exception_summary="${refresh_exception_summary}"
fi
if [[ -n "${refresh_refund_convergence_summary}" ]]; then
  refund_convergence_summary="${refresh_refund_convergence_summary}"
fi
if [[ -n "${refresh_warroom_summary}" ]]; then
  warroom_summary="${refresh_warroom_summary}"
fi
if [[ -n "${refresh_rehearsal_summary}" ]]; then
  rehearsal_summary="${refresh_rehearsal_summary}"
fi
if [[ -n "${refresh_mapping_smoke_summary}" ]]; then
  mapping_smoke_summary="${refresh_mapping_smoke_summary}"
fi

recon_summary="${RUNTIME_ROOT}/payment_reconcile/${REPORT_DATE}/summary.txt"
ticketize_summary="${RUNTIME_ROOT}/payment_reconcile/${REPORT_DATE}/tickets/summary.txt"
daily_report_file="${RUNTIME_ROOT}/payment_daily_report/${REPORT_DATE}/payment_daily_report_${REPORT_DATE}.md"

ops_severity="$(kv "${ops_summary}" "severity")"
ops_preflight_rc="$(kv "${ops_summary}" "preflight_rc")"
ops_monitor_rc="$(kv "${ops_summary}" "monitor_rc")"
ops_reconcile_rc="$(kv "${ops_summary}" "reconcile_rc")"

morning_severity="$(kv "${morning_summary}" "severity")"
morning_gonogo_decision="$(kv "${morning_summary}" "gonogo_decision")"
morning_warroom_overall="$(kv "${morning_summary}" "warroom_overall")"

warroom_overall="$(kv "${warroom_summary}" "overall")"
warroom_risk_count="$(kv "${warroom_summary}" "risk_count")"
warroom_go_nogo_date_match="$(kv "${warroom_summary}" "go_nogo_date_match")"
warroom_go_nogo_report_date="$(kv "${warroom_summary}" "go_nogo_report_date")"
warroom_rehearsal_date_match="$(kv "${warroom_summary}" "rehearsal_date_match")"
warroom_rehearsal_recon_date="$(kv "${warroom_summary}" "rehearsal_recon_date")"

gonogo_decision="$(kv "${gonogo_summary}" "decision")"
gonogo_blocker_count="$(kv "${gonogo_summary}" "blocker_count")"
gonogo_gate_order_drill="$(kv "${gonogo_summary}" "gate_order_drill")"
gonogo_gate_launch="$(kv "${gonogo_summary}" "gate_launch")"
gonogo_warroom_date_match="$(kv "${gonogo_summary}" "warroom_date_match")"
gonogo_warroom_report_date="$(kv "${gonogo_summary}" "warroom_report_date")"
gonogo_rehearsal_date_match="$(kv "${gonogo_summary}" "rehearsal_date_match")"
gonogo_rehearsal_recon_date="$(kv "${gonogo_summary}" "rehearsal_recon_date")"

rehearsal_ready="$(kv "${rehearsal_summary}" "ready_for_order_drill")"
apply_mode="$(kv "${apply_summary}" "mode")"
apply_ready="$(kv "${apply_summary}" "ready")"
apply_rolled_back="$(kv "${apply_summary}" "rolled_back")"

idem_total="$(kv "${idem_summary}" "total_findings")"
idem_critical="$(kv "${idem_summary}" "critical_count")"
idem_warn="$(kv "${idem_summary}" "warn_count")"

booking_verify_severity="$(kv "${booking_verify_summary}" "severity")"
booking_verify_critical="$(kv "${booking_verify_summary}" "critical_count")"
booking_verify_warn="$(kv "${booking_verify_summary}" "warn_count")"

booking_repair_severity="$(kv "${booking_repair_summary}" "severity")"
booking_repair_before_total="$(kv "${booking_repair_summary}" "before_total")"
booking_repair_after_total="$(kv "${booking_repair_summary}" "after_total")"
decision_chain_severity="$(kv "${decision_chain_summary}" "severity")"
decision_chain_fail_count="$(kv "${decision_chain_summary}" "fail_count")"
mapping_smoke_overall="$(kv "${mapping_smoke_summary}" "overall")"
exception_overall="$(kv "${exception_summary}" "overall")"
exception_block_check_count="$(kv "${exception_summary}" "block_check_count")"
exception_warn_check_count="$(kv "${exception_summary}" "warn_check_count")"
refund_convergence_result="$(kv "${refund_convergence_summary}" "gate_result")"
refund_convergence_block_count="$(kv "${refund_convergence_summary}" "block_check_count")"
refund_convergence_warn_count="$(kv "${refund_convergence_summary}" "warn_check_count")"
refund_r05_actions_file="$(kv "${refund_convergence_summary}" "r05_actions_file")"
refund_r05_action_count=0
if [[ -n "${refund_r05_actions_file}" && -f "${refund_r05_actions_file}" ]]; then
  refund_r05_action_count="$(grep -c 'payment_refund_manual_converge.sh' "${refund_r05_actions_file}" || true)"
fi

sla_severity="$(kv "${sla_summary}" "severity")"
sla_issue_days="$(kv "${sla_summary}" "issue_days")"
sla_pending_days="$(kv "${sla_summary}" "pending_days")"
sla_breach_days="$(kv "${sla_summary}" "breach_days")"
sla_sla_days="$(kv "${sla_summary}" "sla_days")"
sla_lookback_days="$(kv "${sla_summary}" "lookback_days")"

recon_raw="$(kv "${recon_summary}" "main_raw_diff_count")"
recon_cleared="$(kv "${recon_summary}" "main_cleared_by_refund_count")"
recon_unresolved="$(kv "${recon_summary}" "main_diff_count")"
recon_orphan="$(kv "${recon_summary}" "orphan_wx_count")"
ticket_total="$(kv "${ticketize_summary}" "total_tickets")"
ticket_p1="$(kv "${ticketize_summary}" "p1_count")"
ticket_p2="$(kv "${ticketize_summary}" "p2_count")"
ticket_escalated="$(kv "${ticketize_summary}" "escalated_count")"
ticket_sla_status="$(kv "${ticketize_summary}" "sla_status")"

ops_age_minutes="$(file_age_minutes "${ops_summary}")"
morning_age_minutes="$(file_age_minutes "${morning_summary}")"
warroom_age_minutes="$(file_age_minutes "${warroom_summary}")"
gonogo_age_minutes="$(file_age_minutes "${gonogo_summary}")"
rehearsal_age_minutes="$(file_age_minutes "${rehearsal_summary}")"
apply_age_minutes="$(file_age_minutes "${apply_summary}")"
idem_age_minutes="$(file_age_minutes "${idem_summary}")"
sla_age_minutes="$(file_age_minutes "${sla_summary}")"
booking_verify_age_minutes="$(file_age_minutes "${booking_verify_summary}")"
booking_repair_age_minutes="$(file_age_minutes "${booking_repair_summary}")"
decision_chain_age_minutes="$(file_age_minutes "${decision_chain_summary}")"
mapping_smoke_age_minutes="$(file_age_minutes "${mapping_smoke_summary}")"
exception_age_minutes="$(file_age_minutes "${exception_summary}")"
refund_convergence_age_minutes="$(file_age_minutes "${refund_convergence_summary}")"
recon_age_minutes="$(file_age_minutes "${recon_summary}")"
ticketize_age_minutes="$(file_age_minutes "${ticketize_summary}")"
daily_report_age_minutes="$(file_age_minutes "${daily_report_file}")"

if [[ -z "${ops_summary}" ]]; then
  add_warn "缺少 ops_daily summary"
fi
if [[ -z "${morning_summary}" ]]; then
  add_warn "缺少 morning_bundle summary"
fi
if [[ -z "${warroom_summary}" ]]; then
  add_warn "缺少 warroom summary"
fi
if [[ -z "${gonogo_summary}" ]]; then
  add_warn "缺少 go_nogo summary"
fi
if [[ -z "${rehearsal_summary}" ]]; then
  add_warn "缺少 cutover_rehearsal summary"
fi
if [[ -z "${sla_summary}" ]]; then
  add_warn "缺少 reconcile_sla summary"
fi
if [[ -z "${booking_verify_summary}" ]]; then
  add_warn "缺少 booking_verify_regression summary"
fi
if [[ -z "${booking_repair_summary}" ]]; then
  add_warn "缺少 booking_verify_repair summary"
fi
if [[ "${REQUIRE_DECISION_CHAIN_PASS}" == "1" && -z "${decision_chain_summary}" ]]; then
  add_warn "缺少 decision_chain_smoke summary"
fi
if [[ -z "${mapping_smoke_summary}" ]]; then
  add_warn "缺少 store_mapping_smoke summary"
fi
if [[ -z "${exception_summary}" ]]; then
  add_warn "缺少 payment_exception_acceptance summary"
fi
if [[ -z "${refund_convergence_summary}" ]]; then
  add_warn "缺少 payment_refund_convergence summary"
fi
if [[ ! -f "${recon_summary}" ]]; then
  add_warn "缺少当日 reconcile summary(${REPORT_DATE})"
fi
if [[ ! -f "${ticketize_summary}" ]]; then
  add_warn "缺少当日 ticketize summary(${REPORT_DATE})"
fi
if [[ ! -f "${daily_report_file}" ]]; then
  add_warn "缺少当日 daily report(${REPORT_DATE})"
fi

check_stale_minutes() {
  local label="$1"
  local age_minutes="$2"
  local max_minutes="$3"
  if is_positive_int "${age_minutes}" && (( age_minutes > max_minutes )); then
    add_warn "${label} 过旧(age=${age_minutes}m > ${max_minutes}m)"
  fi
}

check_stale_minutes "ops_summary" "${ops_age_minutes}" "${MAX_SUMMARY_AGE_MINUTES}"
check_stale_minutes "morning_summary" "${morning_age_minutes}" "${MAX_SUMMARY_AGE_MINUTES}"
check_stale_minutes "warroom_summary" "${warroom_age_minutes}" "${MAX_SUMMARY_AGE_MINUTES}"
check_stale_minutes "go_nogo_summary" "${gonogo_age_minutes}" "${MAX_SUMMARY_AGE_MINUTES}"
check_stale_minutes "rehearsal_summary" "${rehearsal_age_minutes}" "${MAX_SUMMARY_AGE_MINUTES}"
# 仅在要求 apply-ready 的场景校验 apply_summary 新鲜度，避免无正式切换阶段长期黄灯。
if [[ "${REFRESH_REQUIRE_APPLY_READY}" == "1" ]]; then
  check_stale_minutes "apply_summary" "${apply_age_minutes}" "${MAX_SUMMARY_AGE_MINUTES}"
fi
check_stale_minutes "idempotency_summary" "${idem_age_minutes}" "${MAX_SUMMARY_AGE_MINUTES}"
check_stale_minutes "reconcile_sla_summary" "${sla_age_minutes}" "${MAX_SUMMARY_AGE_MINUTES}"
check_stale_minutes "booking_verify_summary" "${booking_verify_age_minutes}" "${MAX_SUMMARY_AGE_MINUTES}"
check_stale_minutes "booking_repair_summary" "${booking_repair_age_minutes}" "${MAX_SUMMARY_AGE_MINUTES}"
if [[ "${REQUIRE_DECISION_CHAIN_PASS}" == "1" ]]; then
  check_stale_minutes "decision_chain_summary" "${decision_chain_age_minutes}" "${MAX_SUMMARY_AGE_MINUTES}"
fi
check_stale_minutes "mapping_smoke_summary" "${mapping_smoke_age_minutes}" "${MAX_SUMMARY_AGE_MINUTES}"
check_stale_minutes "payment_exception_acceptance_summary" "${exception_age_minutes}" "${MAX_SUMMARY_AGE_MINUTES}"
check_stale_minutes "payment_refund_convergence_summary" "${refund_convergence_age_minutes}" "${MAX_SUMMARY_AGE_MINUTES}"
check_stale_minutes "reconcile_summary" "${recon_age_minutes}" "$(( MAX_RECON_AGE_DAYS * 1440 ))"
check_stale_minutes "ticketize_summary" "${ticketize_age_minutes}" "$(( MAX_RECON_AGE_DAYS * 1440 ))"
check_stale_minutes "daily_report_file" "${daily_report_age_minutes}" "$(( MAX_DAILY_REPORT_AGE_DAYS * 1440 ))"

case "${ops_severity:-UNKNOWN}" in
  ERROR) add_block "ops_daily severity=ERROR" ;;
  ALERT) add_warn "ops_daily severity=ALERT" ;;
esac

case "${morning_severity:-UNKNOWN}" in
  ERROR) add_block "morning_bundle severity=ERROR" ;;
  ALERT) add_warn "morning_bundle severity=ALERT" ;;
esac

if [[ -n "${warroom_summary}" ]]; then
  if [[ "${warroom_overall}" != "GREEN" ]]; then
    # cutover_gate 内带真实订单演练时，go_nogo 已 GO_LAUNCH 的情况下，warroom 非 GREEN 先降级为 WARN，避免互锁。
    if [[ -n "${REFRESH_ORDER_NO}" && "${gonogo_decision:-}" == "GO_LAUNCH" ]]; then
      add_warn "warroom overall=${warroom_overall:-N/A}（order-drill 模式降级为 WARN）"
    else
      add_block "warroom overall=${warroom_overall:-N/A}"
    fi
  fi
  if [[ "${warroom_go_nogo_date_match}" == "0" ]]; then
    add_block "warroom.go_nogo 日期不匹配(report_date=${warroom_go_nogo_report_date:-N/A}, expected=${REPORT_DATE})"
  elif [[ -z "${warroom_go_nogo_date_match}" ]]; then
    add_warn "warroom 缺少 go_nogo_date_match 字段"
  fi
  if [[ "${warroom_rehearsal_date_match}" == "0" ]]; then
    add_block "warroom.rehearsal 日期不匹配(recon_date=${warroom_rehearsal_recon_date:-N/A}, expected=${REPORT_DATE})"
  elif [[ -z "${warroom_rehearsal_date_match}" ]]; then
    add_warn "warroom 缺少 rehearsal_date_match 字段"
  fi
  if is_positive_int "${warroom_risk_count:-}"; then
    if (( warroom_risk_count > 0 )); then
      add_warn "warroom risk_count=${warroom_risk_count}"
    fi
  fi
fi

if [[ -n "${gonogo_summary}" ]]; then
  if [[ "${gonogo_decision}" == "NO_GO" || "${gonogo_decision}" == "UNKNOWN" || -z "${gonogo_decision}" ]]; then
    add_block "go_nogo decision=${gonogo_decision:-N/A}"
  fi
  if [[ "${gonogo_warroom_date_match}" == "0" ]]; then
    add_block "go_nogo.warroom 日期不匹配(report_date=${gonogo_warroom_report_date:-N/A}, expected=${REPORT_DATE})"
  elif [[ -z "${gonogo_warroom_date_match}" ]]; then
    add_warn "go_nogo 缺少 warroom_date_match 字段"
  fi
  if [[ "${gonogo_rehearsal_date_match}" == "0" ]]; then
    add_block "go_nogo.rehearsal 日期不匹配(recon_date=${gonogo_rehearsal_recon_date:-N/A}, expected=${REPORT_DATE})"
  elif [[ -z "${gonogo_rehearsal_date_match}" ]]; then
    add_warn "go_nogo 缺少 rehearsal_date_match 字段"
  fi
fi

if [[ -n "${rehearsal_summary}" && "${rehearsal_ready}" != "1" && "${refresh_rehearsal_rc}" != "2" ]]; then
  add_warn "rehearsal ready_for_order_drill=${rehearsal_ready:-N/A}"
fi

if [[ -n "${apply_summary}" ]]; then
  if [[ "${apply_mode}" == "apply" && "${apply_rolled_back:-0}" == "1" ]]; then
    add_block "cutover_apply 发生回滚(rolled_back=1)"
  fi
  if [[ "${apply_mode}" == "apply" && "${apply_ready:-0}" != "1" ]]; then
    add_warn "cutover_apply ready=${apply_ready:-N/A}"
  fi
fi

if is_positive_int "${recon_unresolved:-}" && is_positive_int "${recon_orphan:-}"; then
  if (( recon_unresolved > 0 || recon_orphan > 0 )); then
    add_block "reconcile 未解决差异 unresolved=${recon_unresolved}, orphan=${recon_orphan}"
  fi
fi

if is_positive_int "${idem_critical:-}" && (( idem_critical > 0 )); then
  add_block "idempotency critical_count=${idem_critical}"
fi
if is_positive_int "${idem_warn:-}" && (( idem_warn > 0 )); then
  add_warn "idempotency warn_count=${idem_warn}"
fi

case "${booking_verify_severity:-UNKNOWN}" in
  ALERT) add_block "booking_verify severity=ALERT (critical=${booking_verify_critical:-N/A})" ;;
  WARN) add_warn "booking_verify severity=WARN (warn=${booking_verify_warn:-N/A})" ;;
esac

if [[ "${REQUIRE_BOOKING_REPAIR_PASS}" == "1" ]]; then
  if [[ -z "${booking_repair_summary}" ]]; then
    add_block "booking_repair summary 缺失（已要求 PASS）"
  elif [[ "${booking_repair_severity}" != "PASS" ]]; then
    add_block "booking_repair 未通过(severity=${booking_repair_severity:-N/A}, before_total=${booking_repair_before_total:-N/A}, after_total=${booking_repair_after_total:-N/A})"
  fi
else
  case "${booking_repair_severity:-UNKNOWN}" in
    ALERT) add_block "booking_repair severity=ALERT (after_total=${booking_repair_after_total:-N/A})" ;;
    PENDING) add_warn "booking_repair severity=PENDING (before_total=${booking_repair_before_total:-N/A})" ;;
  esac
fi

if [[ "${REQUIRE_DECISION_CHAIN_PASS}" == "1" ]]; then
  if [[ -z "${decision_chain_summary}" ]]; then
    add_block "decision_chain_smoke summary 缺失（已要求 PASS）"
  elif [[ "${decision_chain_severity}" != "PASS" ]]; then
    add_block "decision_chain_smoke 未通过(severity=${decision_chain_severity:-N/A}, fail_count=${decision_chain_fail_count:-N/A})"
  fi
else
  case "${decision_chain_severity:-UNKNOWN}" in
    FAIL|ALERT) add_block "decision_chain_smoke severity=${decision_chain_severity} (fail_count=${decision_chain_fail_count:-N/A})" ;;
    WARN) add_warn "decision_chain_smoke severity=WARN (fail_count=${decision_chain_fail_count:-N/A})" ;;
  esac
fi

if [[ -n "${mapping_smoke_summary}" && "${mapping_smoke_overall}" != "GREEN" ]]; then
  add_warn "store_mapping_smoke overall=${mapping_smoke_overall:-N/A}"
fi

case "${exception_overall:-UNKNOWN}" in
  RED) add_block "payment_exception_acceptance overall=RED (block_checks=${exception_block_check_count:-N/A})" ;;
  YELLOW) add_warn "payment_exception_acceptance overall=YELLOW (warn_checks=${exception_warn_check_count:-N/A})" ;;
esac

case "${refund_convergence_result:-UNKNOWN}" in
  RED) add_block "payment_refund_convergence gate_result=RED (block_checks=${refund_convergence_block_count:-N/A})" ;;
  GREEN_WITH_WARN) add_warn "payment_refund_convergence gate_result=GREEN_WITH_WARN (warn_checks=${refund_convergence_warn_count:-N/A})" ;;
  UNKNOWN) ;;
  "") ;;
  GREEN) ;;
  *) add_warn "payment_refund_convergence gate_result=${refund_convergence_result}" ;;
esac
if is_positive_int "${refund_r05_action_count}" && (( refund_r05_action_count > 0 )); then
  add_warn "payment_refund_convergence 存在可人工收敛异常(action_count=${refund_r05_action_count}, actions=${refund_r05_actions_file})"
fi

if [[ -n "${sla_summary}" ]]; then
  case "${sla_severity:-UNKNOWN}" in
    BREACH) add_block "reconcile_sla severity=BREACH (breach_days=${sla_breach_days:-N/A})" ;;
    WARN) add_warn "reconcile_sla severity=WARN (pending_days=${sla_pending_days:-N/A})" ;;
  esac
fi

warn_count="${#warn_reasons[@]}"
block_count="${#block_reasons[@]}"

overall="GREEN"
exit_code=0
if (( block_count > 0 )); then
  overall="RED"
  exit_code=2
elif (( warn_count > 0 )); then
  overall="YELLOW"
  exit_code=2
fi

warn_text=""
if (( warn_count > 0 )); then
  warn_text="$(printf '%s; ' "${warn_reasons[@]}")"
  warn_text="${warn_text%; }"
fi

block_text=""
if (( block_count > 0 )); then
  block_text="$(printf '%s; ' "${block_reasons[@]}")"
  block_text="${block_text%; }"
fi

cat > "${SUMMARY_FILE}" <<TXT
run_id=${RUN_ID}
run_time=$(date '+%Y-%m-%d %H:%M:%S')
report_date=${REPORT_DATE}
overall=${overall}
warn_count=${warn_count}
block_count=${block_count}
runtime_root=${RUNTIME_ROOT}
warn_reasons=${warn_text}
block_reasons=${block_text}
refresh=${REFRESH}
refresh_order_no=${REFRESH_ORDER_NO}
refresh_rc=${refresh_rc}
refresh_summary=${refresh_summary}
refresh_sla_rc=${refresh_sla_rc}
refresh_sla_summary=${refresh_sla_summary}
refresh_booking_verify_rc=${refresh_booking_verify_rc}
refresh_booking_verify_summary=${refresh_booking_verify_summary}
refresh_booking_repair_rc=${refresh_booking_repair_rc}
refresh_booking_repair_summary=${refresh_booking_repair_summary}
refresh_decision_chain_rc=${refresh_decision_chain_rc}
refresh_decision_chain_summary=${refresh_decision_chain_summary}
refresh_exception_rc=${refresh_exception_rc}
refresh_exception_summary=${refresh_exception_summary}
refresh_refund_convergence_rc=${refresh_refund_convergence_rc}
refresh_refund_convergence_summary=${refresh_refund_convergence_summary}
refresh_warroom_rc=${refresh_warroom_rc}
refresh_warroom_summary=${refresh_warroom_summary}
refresh_rehearsal_rc=${refresh_rehearsal_rc}
refresh_rehearsal_summary=${refresh_rehearsal_summary}
refresh_mapping_smoke_rc=${refresh_mapping_smoke_rc}
refresh_mapping_smoke_summary=${refresh_mapping_smoke_summary}
refresh_sla_lookback_days=${SLA_LOOKBACK_DAYS}
refresh_sla_days=${SLA_DAYS}
refresh_window_hours=${REFRESH_WINDOW_HOURS}
refund_window_hours=${REFUND_WINDOW_HOURS}
refund_timeout_minutes=${REFUND_TIMEOUT_MINUTES}
require_booking_repair_pass=${REQUIRE_BOOKING_REPAIR_PASS}
require_decision_chain_pass=${REQUIRE_DECISION_CHAIN_PASS}
max_summary_age_minutes=${MAX_SUMMARY_AGE_MINUTES}
max_recon_age_days=${MAX_RECON_AGE_DAYS}
max_daily_report_age_days=${MAX_DAILY_REPORT_AGE_DAYS}
ops_summary=${ops_summary}
ops_summary_age_minutes=${ops_age_minutes}
ops_severity=${ops_severity}
ops_preflight_rc=${ops_preflight_rc}
ops_monitor_rc=${ops_monitor_rc}
ops_reconcile_rc=${ops_reconcile_rc}
morning_summary=${morning_summary}
morning_summary_age_minutes=${morning_age_minutes}
morning_severity=${morning_severity}
morning_gonogo_decision=${morning_gonogo_decision}
morning_warroom_overall=${morning_warroom_overall}
warroom_summary=${warroom_summary}
warroom_summary_age_minutes=${warroom_age_minutes}
warroom_overall=${warroom_overall}
warroom_risk_count=${warroom_risk_count}
warroom_go_nogo_date_match=${warroom_go_nogo_date_match}
warroom_go_nogo_report_date=${warroom_go_nogo_report_date}
warroom_rehearsal_date_match=${warroom_rehearsal_date_match}
warroom_rehearsal_recon_date=${warroom_rehearsal_recon_date}
gonogo_summary=${gonogo_summary}
gonogo_summary_age_minutes=${gonogo_age_minutes}
gonogo_decision=${gonogo_decision}
gonogo_blocker_count=${gonogo_blocker_count}
gonogo_gate_order_drill=${gonogo_gate_order_drill}
gonogo_gate_launch=${gonogo_gate_launch}
gonogo_warroom_date_match=${gonogo_warroom_date_match}
gonogo_warroom_report_date=${gonogo_warroom_report_date}
gonogo_rehearsal_date_match=${gonogo_rehearsal_date_match}
gonogo_rehearsal_recon_date=${gonogo_rehearsal_recon_date}
rehearsal_summary=${rehearsal_summary}
rehearsal_summary_age_minutes=${rehearsal_age_minutes}
rehearsal_ready_for_order_drill=${rehearsal_ready}
apply_summary=${apply_summary}
apply_summary_age_minutes=${apply_age_minutes}
apply_mode=${apply_mode}
apply_ready=${apply_ready}
apply_rolled_back=${apply_rolled_back}
idem_summary=${idem_summary}
idem_summary_age_minutes=${idem_age_minutes}
idem_total_findings=${idem_total}
idem_critical_count=${idem_critical}
idem_warn_count=${idem_warn}
booking_verify_summary=${booking_verify_summary}
booking_verify_summary_age_minutes=${booking_verify_age_minutes}
booking_verify_severity=${booking_verify_severity}
booking_verify_critical_count=${booking_verify_critical}
booking_verify_warn_count=${booking_verify_warn}
booking_repair_summary=${booking_repair_summary}
booking_repair_summary_age_minutes=${booking_repair_age_minutes}
booking_repair_severity=${booking_repair_severity}
booking_repair_before_total=${booking_repair_before_total}
booking_repair_after_total=${booking_repair_after_total}
decision_chain_summary=${decision_chain_summary}
decision_chain_summary_age_minutes=${decision_chain_age_minutes}
decision_chain_severity=${decision_chain_severity}
decision_chain_fail_count=${decision_chain_fail_count}
mapping_smoke_summary=${mapping_smoke_summary}
mapping_smoke_summary_age_minutes=${mapping_smoke_age_minutes}
mapping_smoke_overall=${mapping_smoke_overall}
exception_summary=${exception_summary}
exception_summary_age_minutes=${exception_age_minutes}
exception_overall=${exception_overall}
exception_block_check_count=${exception_block_check_count}
exception_warn_check_count=${exception_warn_check_count}
refund_convergence_summary=${refund_convergence_summary}
refund_convergence_summary_age_minutes=${refund_convergence_age_minutes}
refund_convergence_result=${refund_convergence_result}
refund_convergence_block_check_count=${refund_convergence_block_count}
refund_convergence_warn_check_count=${refund_convergence_warn_count}
refund_r05_actions_file=${refund_r05_actions_file}
refund_r05_action_count=${refund_r05_action_count}
reconcile_sla_summary=${sla_summary}
reconcile_sla_summary_age_minutes=${sla_age_minutes}
reconcile_sla_severity=${sla_severity}
reconcile_sla_issue_days=${sla_issue_days}
reconcile_sla_pending_days=${sla_pending_days}
reconcile_sla_breach_days=${sla_breach_days}
reconcile_sla_days=${sla_sla_days}
reconcile_sla_lookback_days=${sla_lookback_days}
reconcile_summary=${recon_summary}
reconcile_summary_age_minutes=${recon_age_minutes}
reconcile_main_raw_diff_count=${recon_raw}
reconcile_main_cleared_by_refund_count=${recon_cleared}
reconcile_main_diff_count=${recon_unresolved}
reconcile_orphan_wx_count=${recon_orphan}
ticketize_summary=${ticketize_summary}
ticketize_summary_age_minutes=${ticketize_age_minutes}
ticketize_total_tickets=${ticket_total}
ticketize_p1_count=${ticket_p1}
ticketize_p2_count=${ticket_p2}
ticketize_escalated_count=${ticket_escalated}
ticketize_sla_status=${ticket_sla_status}
daily_report_file=${daily_report_file}
daily_report_file_age_minutes=${daily_report_age_minutes}
run_dir=${RUN_DIR}
TXT

{
  echo "# 支付值守总览（${REPORT_DATE}）"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`"
  echo "- overall: **${overall}**"
  echo "- warn_count: \`${warn_count}\`"
  echo "- block_count: \`${block_count}\`"
  echo "- require_booking_repair_pass: \`${REQUIRE_BOOKING_REPAIR_PASS}\`"
  echo "- require_decision_chain_pass: \`${REQUIRE_DECISION_CHAIN_PASS}\`"
  echo "- runtime_root: \`${RUNTIME_ROOT}\`"
  echo
  echo "## 一、核心判定"
  echo
  echo "| 模块 | 状态 | 关键字段 |"
  echo "|---|---|---|"
  echo "| ops_daily(latest) | ${ops_severity:-N/A} | preflight=${ops_preflight_rc:-N/A}, monitor=${ops_monitor_rc:-N/A}, reconcile=${ops_reconcile_rc:-N/A} |"
  echo "| morning_bundle(latest) | ${morning_severity:-N/A} | go_nogo=${morning_gonogo_decision:-N/A}, warroom=${morning_warroom_overall:-N/A} |"
  echo "| warroom(latest) | ${warroom_overall:-N/A} | risk_count=${warroom_risk_count:-N/A}, go_nogo_date_match=${warroom_go_nogo_date_match:-N/A}, rehearsal_date_match=${warroom_rehearsal_date_match:-N/A} |"
  echo "| go_nogo(latest) | ${gonogo_decision:-N/A} | blockers=${gonogo_blocker_count:-N/A}, gate_order_drill=${gonogo_gate_order_drill:-N/A}, gate_launch=${gonogo_gate_launch:-N/A}, warroom_date_match=${gonogo_warroom_date_match:-N/A}, rehearsal_date_match=${gonogo_rehearsal_date_match:-N/A} |"
  echo "| reconcile_sla(latest) | ${sla_severity:-N/A} | issue_days=${sla_issue_days:-N/A}, pending_days=${sla_pending_days:-N/A}, breach_days=${sla_breach_days:-N/A}, sla_days=${sla_sla_days:-N/A} |"
  echo "| rehearsal(latest) | ready=${rehearsal_ready:-N/A} | summary=$(printf '\`%s\`' "${rehearsal_summary:-N/A}") |"
  echo "| cutover_apply(latest) | mode=${apply_mode:-N/A} | ready=${apply_ready:-N/A}, rolled_back=${apply_rolled_back:-N/A} |"
  echo "| reconcile(${REPORT_DATE}) | unresolved=${recon_unresolved:-N/A} | raw=${recon_raw:-N/A}, auto_cleared=${recon_cleared:-N/A}, orphan=${recon_orphan:-N/A} |"
  echo "| ticketize(${REPORT_DATE}) | total=${ticket_total:-N/A} | p1=${ticket_p1:-N/A}, p2=${ticket_p2:-N/A}, escalated=${ticket_escalated:-N/A}, sla=${ticket_sla_status:-N/A} |"
  echo "| idempotency(latest) | critical=${idem_critical:-N/A} | warn=${idem_warn:-N/A}, total=${idem_total:-N/A} |"
  echo "| booking_verify(latest) | ${booking_verify_severity:-N/A} | critical=${booking_verify_critical:-N/A}, warn=${booking_verify_warn:-N/A} |"
  echo "| booking_repair(latest) | ${booking_repair_severity:-N/A} | before_total=${booking_repair_before_total:-N/A}, after_total=${booking_repair_after_total:-N/A} |"
  echo "| decision_chain_smoke(latest) | ${decision_chain_severity:-N/A} | fail_count=${decision_chain_fail_count:-N/A} |"
  echo "| store_mapping_smoke(latest) | ${mapping_smoke_overall:-N/A} | summary=$(printf '\`%s\`' "${mapping_smoke_summary:-N/A}") |"
  echo "| exception_acceptance(latest) | ${exception_overall:-N/A} | block_checks=${exception_block_check_count:-N/A}, warn_checks=${exception_warn_check_count:-N/A} |"
  echo "| refund_convergence(latest) | ${refund_convergence_result:-N/A} | block_checks=${refund_convergence_block_count:-N/A}, warn_checks=${refund_convergence_warn_count:-N/A} |"
  echo "| refund_R05_actions(latest) | count=${refund_r05_action_count:-0} | file=$(printf '\`%s\`' "${refund_r05_actions_file:-N/A}") |"
  echo
  echo "## 二、数据新鲜度"
  echo
  echo "- max_summary_age_minutes: \`${MAX_SUMMARY_AGE_MINUTES}\`"
  echo "- max_recon_age_days: \`${MAX_RECON_AGE_DAYS}\`"
  echo "- max_daily_report_age_days: \`${MAX_DAILY_REPORT_AGE_DAYS}\`"
  echo
  echo "| 文件 | age_minutes |"
  echo "|---|---:|"
  echo "| ops_summary | ${ops_age_minutes:-N/A} |"
  echo "| morning_summary | ${morning_age_minutes:-N/A} |"
  echo "| warroom_summary | ${warroom_age_minutes:-N/A} |"
  echo "| go_nogo_summary | ${gonogo_age_minutes:-N/A} |"
  echo "| rehearsal_summary | ${rehearsal_age_minutes:-N/A} |"
  echo "| apply_summary | ${apply_age_minutes:-N/A} |"
  echo "| idempotency_summary | ${idem_age_minutes:-N/A} |"
  echo "| reconcile_sla_summary | ${sla_age_minutes:-N/A} |"
  echo "| booking_verify_summary | ${booking_verify_age_minutes:-N/A} |"
  echo "| booking_repair_summary | ${booking_repair_age_minutes:-N/A} |"
  echo "| decision_chain_summary | ${decision_chain_age_minutes:-N/A} |"
  echo "| mapping_smoke_summary | ${mapping_smoke_age_minutes:-N/A} |"
  echo "| exception_summary | ${exception_age_minutes:-N/A} |"
  echo "| refund_convergence_summary | ${refund_convergence_age_minutes:-N/A} |"
  echo "| reconcile_summary | ${recon_age_minutes:-N/A} |"
  echo "| ticketize_summary | ${ticketize_age_minutes:-N/A} |"
  echo "| daily_report_file | ${daily_report_age_minutes:-N/A} |"
  echo
  echo "## 三、风险与建议"
  echo
  if (( block_count == 0 )); then
    echo "- 阻断项：无"
  else
    echo "- 阻断项："
    for item in "${block_reasons[@]}"; do
      echo "  - ${item}"
    done
  fi
  if (( warn_count == 0 )); then
    echo "- 关注项：无"
  else
    echo "- 关注项："
    for item in "${warn_reasons[@]}"; do
      echo "  - ${item}"
    done
  fi
  echo
  echo "## 四、追溯路径"
  echo
  echo "- summary: \`${SUMMARY_FILE}\`"
  echo "- ops_summary: \`${ops_summary}\`"
  echo "- morning_summary: \`${morning_summary}\`"
  echo "- warroom_summary: \`${warroom_summary}\`"
  echo "- go_nogo_summary: \`${gonogo_summary}\`"
  echo "- rehearsal_summary: \`${rehearsal_summary}\`"
  echo "- apply_summary: \`${apply_summary}\`"
  echo "- idempotency_summary: \`${idem_summary}\`"
  echo "- reconcile_sla_summary: \`${sla_summary}\`"
  echo "- booking_verify_summary: \`${booking_verify_summary}\`"
  echo "- booking_repair_summary: \`${booking_repair_summary}\`"
  echo "- decision_chain_summary: \`${decision_chain_summary}\`"
  echo "- mapping_smoke_summary: \`${mapping_smoke_summary}\`"
  echo "- exception_summary: \`${exception_summary}\`"
  echo "- refund_convergence_summary: \`${refund_convergence_summary}\`"
  echo "- refund_r05_actions_file: \`${refund_r05_actions_file}\`"
  echo "- reconcile_summary: \`${recon_summary}\`"
  echo "- ticketize_summary: \`${ticketize_summary}\`"
  echo "- daily_report: \`${daily_report_file}\`"
} > "${REPORT_FILE}"

echo "[ops-status] report=${REPORT_FILE}"
echo "[ops-status] summary=${SUMMARY_FILE}"
echo "[ops-status] overall=${overall}, warn_count=${warn_count}, block_count=${block_count}"

if (( exit_code != 0 && NO_ALERT == 0 )) && [[ -x "${ALERT_SCRIPT}" ]]; then
  "${ALERT_SCRIPT}" \
    --title "支付值守总览告警" \
    --content "overall=${overall}; date=${REPORT_DATE}; warn_count=${warn_count}; block_count=${block_count}; report=${REPORT_FILE}" || true
fi

exit "${exit_code}"
