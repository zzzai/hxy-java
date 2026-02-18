#!/usr/bin/env bash
set -euo pipefail

# D39: cron 自监控
# 检查托管 cron 是否存在、关键任务日志是否新鲜。

OUT_DIR="${OUT_DIR:-}"
RUNTIME_DIR="${RUNTIME_DIR:-}"
FAST_MAX_AGE_MINUTES="${FAST_MAX_AGE_MINUTES:-20}"
DAILY_MAX_AGE_HOURS="${DAILY_MAX_AGE_HOURS:-36}"
NO_ALERT=0

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_cron_healthcheck.sh [--out-dir PATH] [--runtime-dir PATH] [--fast-max-age-minutes N] [--daily-max-age-hours N] [--no-alert]

参数：
  --out-dir PATH               输出目录（默认 runtime/payment_cron_healthcheck）
  --runtime-dir PATH           runtime 根目录（默认 <repo>/runtime）
  --fast-max-age-minutes N     高频任务日志新鲜度阈值（默认 20）
  --daily-max-age-hours N      日常任务日志新鲜度阈值（默认 36）
  --no-alert                   失败时不推送告警

退出码：
  0  检查通过
  2  存在风险（缺托管块/关键日志过旧或缺失）
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --out-dir)
      OUT_DIR="$2"
      shift 2
      ;;
    --runtime-dir)
      RUNTIME_DIR="$2"
      shift 2
      ;;
    --fast-max-age-minutes)
      FAST_MAX_AGE_MINUTES="$2"
      shift 2
      ;;
    --daily-max-age-hours)
      DAILY_MAX_AGE_HOURS="$2"
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

if [[ -z "${RUNTIME_DIR}" ]]; then
  RUNTIME_DIR="${ROOT_DIR}/runtime"
fi
if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${RUNTIME_DIR}/payment_cron_healthcheck"
fi
if ! [[ "${FAST_MAX_AGE_MINUTES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --fast-max-age-minutes 必须是正整数"
  exit 1
fi
if ! [[ "${DAILY_MAX_AGE_HOURS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --daily-max-age-hours 必须是正整数"
  exit 1
fi

RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"
SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"

file_age_minutes() {
  local f="$1"
  if [[ ! -f "${f}" ]]; then
    printf -- "-1"
    return
  fi
  local now_ts mtime age
  now_ts="$(date +%s)"
  mtime="$(stat -c %Y "${f}" 2>/dev/null || echo "")"
  if [[ -z "${mtime}" || ! "${mtime}" =~ ^[0-9]+$ ]]; then
    printf -- "-1"
    return
  fi
  age=$(( (now_ts - mtime) / 60 ))
  if (( age < 0 )); then age=0; fi
  printf '%s' "${age}"
}

cron_all="$(crontab -l 2>/dev/null || true)"
cron_managed=0
cron_block=""
if [[ "${cron_all}" == *"# >>> payment ops managed >>>"* && "${cron_all}" == *"# <<< payment ops managed <<<"* ]]; then
  cron_managed=1
  cron_block="$(printf '%s\n' "${cron_all}" | awk '
    $0=="# >>> payment ops managed >>>" {show=1; next}
    $0=="# <<< payment ops managed <<<" {show=0; next}
    show==1 {print}
  ')"
fi

rows=()
warns=()
fails=()

add_row() {
  rows+=("$1|$2|$3|$4|$5")
}
add_warn() {
  warns+=("$1")
}
add_fail() {
  fails+=("$1")
}

if [[ "${cron_managed}" != "1" ]]; then
  add_fail "未检测到 payment ops 托管 cron 块"
fi

# key|cron_pattern|log_file|max_age_minutes|required
daily_max_age_minutes=$((DAILY_MAX_AGE_HOURS * 60))
checks=(
  "monitor|payment_monitor_alert.sh|${RUNTIME_DIR}/payment_monitor_cron.log|${FAST_MAX_AGE_MINUTES}|1"
  "reconcile|payment_reconcile_daily.sh|${RUNTIME_DIR}/payment_reconcile_cron.log|${daily_max_age_minutes}|1"
  "ticketize|payment_reconcile_ticketize.sh|${RUNTIME_DIR}/payment_ticketize_cron.log|${daily_max_age_minutes}|1"
  "decision_ticketize|payment_decision_ticketize.sh|${RUNTIME_DIR}/payment_decision_ticketize_cron.log|${daily_max_age_minutes}|1"
  "daily_report|payment_daily_report|${RUNTIME_DIR}/payment_daily_report_cron.log|${daily_max_age_minutes}|1"
  "warroom|payment_warroom_dashboard.sh|${RUNTIME_DIR}/payment_warroom_cron.log|${daily_max_age_minutes}|1"
  "go_nogo|payment_go_nogo_decision.sh|${RUNTIME_DIR}/payment_go_nogo_cron.log|${daily_max_age_minutes}|1"
  "ops_status|payment_ops_status.sh|${RUNTIME_DIR}/payment_ops_status_cron.log|${daily_max_age_minutes}|1"
  "contract_check|payment_summary_contract_check.sh|${RUNTIME_DIR}/payment_contract_check_cron.log|${daily_max_age_minutes}|1"
  "sla_guard|payment_reconcile_sla_guard.sh|${RUNTIME_DIR}/payment_reconcile_sla_cron.log|${daily_max_age_minutes}|1"
  "retention|payment_runtime_retention.sh|${RUNTIME_DIR}/payment_retention_cron.log|${daily_max_age_minutes}|1"
  "booking_repair|payment_booking_verify_repair.sh|${RUNTIME_DIR}/payment_booking_repair_cron.log|${daily_max_age_minutes}|1"
  "decision_chain|payment_decision_chain_smoke.sh|${RUNTIME_DIR}/payment_decision_chain_smoke_cron.log|${daily_max_age_minutes}|1"
  "mapping_audit|payment_store_mapping_audit.sh|${RUNTIME_DIR}/payment_store_mapping_audit_cron.log|${daily_max_age_minutes}|0"
  "mapping_smoke|payment_store_mapping_pipeline_smoke.sh|${RUNTIME_DIR}/payment_store_mapping_smoke_cron.log|${daily_max_age_minutes}|0"
  "cutover_gate|payment_cutover_gate.sh|${RUNTIME_DIR}/payment_cutover_gate_cron.log|${daily_max_age_minutes}|0"
)

for item in "${checks[@]}"; do
  IFS='|' read -r key pattern log_file max_age required <<< "${item}"
  enabled=0
  if [[ "${cron_managed}" == "1" ]] && printf '%s\n' "${cron_block}" | grep -Fq "${pattern}"; then
    enabled=1
  fi
  age_minutes="$(file_age_minutes "${log_file}")"
  status="PASS"
  detail="enabled=${enabled}, age_minutes=${age_minutes}, max_age=${max_age}"
  if [[ "${enabled}" != "1" ]]; then
    if [[ "${required}" == "1" ]]; then
      status="FAIL"
      add_fail "${key} 未在托管 cron 中启用(pattern=${pattern})"
    elif [[ "${required}" == "0" ]]; then
      status="PASS"
      detail="enabled=${enabled}, optional=1, age_minutes=${age_minutes}, max_age=${max_age}"
    else
      status="WARN"
      add_warn "${key} 未启用"
    fi
  else
    if [[ "${age_minutes}" == "-1" ]]; then
      status="FAIL"
      add_fail "${key} 日志缺失(${log_file})"
    elif (( age_minutes > max_age )); then
      status="WARN"
      add_warn "${key} 日志过旧(age=${age_minutes}m > ${max_age}m)"
    fi
  fi
  add_row "${key}" "${status}" "${enabled}" "${age_minutes}" "${log_file}"
done

warn_count="${#warns[@]}"
fail_count="${#fails[@]}"

overall="GREEN"
exit_code=0
if (( fail_count > 0 )); then
  overall="RED"
  exit_code=2
elif (( warn_count > 0 )); then
  overall="YELLOW"
  exit_code=2
fi

warn_text=""
if (( warn_count > 0 )); then
  warn_text="$(printf '%s; ' "${warns[@]}")"
  warn_text="${warn_text%; }"
fi
fail_text=""
if (( fail_count > 0 )); then
  fail_text="$(printf '%s; ' "${fails[@]}")"
  fail_text="${fail_text%; }"
fi

{
  echo "run_id=${RUN_ID}"
  echo "run_time=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "overall=${overall}"
  echo "warn_count=${warn_count}"
  echo "fail_count=${fail_count}"
  echo "cron_managed=${cron_managed}"
  echo "fast_max_age_minutes=${FAST_MAX_AGE_MINUTES}"
  echo "daily_max_age_hours=${DAILY_MAX_AGE_HOURS}"
  echo "warn_reasons=${warn_text}"
  echo "fail_reasons=${fail_text}"
  echo "run_dir=${RUN_DIR}"
} > "${SUMMARY_FILE}"

{
  echo "# 支付 cron 健康检查"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`"
  echo "- overall: **${overall}**"
  echo "- cron_managed: \`${cron_managed}\`"
  echo "- fast_max_age_minutes: \`${FAST_MAX_AGE_MINUTES}\`"
  echo "- daily_max_age_hours: \`${DAILY_MAX_AGE_HOURS}\`"
  echo
  echo "## 明细"
  echo
  echo "| task | status | enabled | age_minutes | log_file |"
  echo "|---|---|---:|---:|---|"
  for r in "${rows[@]}"; do
    IFS='|' read -r key lv enabled age log_file <<< "${r}"
    echo "| ${key} | ${lv} | ${enabled} | ${age} | \`${log_file}\` |"
  done
  echo
  echo "## 风险"
  if (( fail_count == 0 )); then
    echo "- FAIL: 无"
  else
    echo "- FAIL:"
    for i in "${fails[@]}"; do
      echo "  - ${i}"
    done
  fi
  if (( warn_count == 0 )); then
    echo "- WARN: 无"
  else
    echo "- WARN:"
    for i in "${warns[@]}"; do
      echo "  - ${i}"
    done
  fi
  echo
  echo "## 追溯"
  echo "- summary: \`${SUMMARY_FILE}\`"
} > "${REPORT_FILE}"

echo "[cron-healthcheck] summary=${SUMMARY_FILE}"
echo "[cron-healthcheck] report=${REPORT_FILE}"
echo "[cron-healthcheck] overall=${overall}, warn_count=${warn_count}, fail_count=${fail_count}"

if (( exit_code != 0 )) && [[ ${NO_ALERT} -eq 0 ]] && [[ -x "${ALERT_SCRIPT}" ]]; then
  "${ALERT_SCRIPT}" \
    --title "支付 cron 健康检查告警" \
    --content "overall=${overall}; warn=${warn_count}; fail=${fail_count}; report=${REPORT_FILE}" || true
fi

exit "${exit_code}"
