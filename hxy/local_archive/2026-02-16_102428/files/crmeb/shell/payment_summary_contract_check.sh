#!/usr/bin/env bash
set -euo pipefail

# D16: summary 协议回归检查
# 目标：防止脚本 summary 字段被改坏，影响 warroom/go_nogo/日报解析。

REPORT_DATE="${REPORT_DATE:-}"
REQUIRE_ALL=0
NO_ALERT=0
OUT_DIR="${OUT_DIR:-}"
RUNTIME_ROOT="${RUNTIME_ROOT:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_summary_contract_check.sh [--date YYYY-MM-DD] [--require-all] [--runtime-root PATH] [--no-alert] [--out-dir PATH]

参数：
  --date YYYY-MM-DD    对账日期（默认昨天，仅用于检查 reconcile summary）
  --require-all        缺失 summary 也按 FAIL 处理
  --runtime-root PATH  指定 runtime 根目录（默认 <repo>/runtime）
  --no-alert           有 FAIL 时不推送告警
  --out-dir PATH       输出目录（默认 runtime/payment_contract_check）

退出码：
  0  合同检查通过（允许缺失但不缺字段）
  2  存在 FAIL（缺字段或 require-all 下缺 summary）
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --date)
      REPORT_DATE="$2"
      shift 2
      ;;
    --require-all)
      REQUIRE_ALL=1
      shift
      ;;
    --runtime-root)
      RUNTIME_ROOT="$2"
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

if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${ROOT_DIR}/runtime/payment_contract_check"
fi
if [[ -z "${RUNTIME_ROOT}" ]]; then
  RUNTIME_ROOT="${ROOT_DIR}/runtime"
fi
RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"
SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"

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

latest_summary_by_prefix() {
  local base="$1"
  local prefix="$2"
  if [[ ! -d "${base}" ]]; then
    printf ''
    return
  fi
  find "${base}" -maxdepth 2 -type f -name 'summary.txt' -path "*/${prefix}-*/summary.txt" -printf '%T@ %p\n' 2>/dev/null \
    | sort -n \
    | tail -n 1 \
    | cut -d' ' -f2- || true
}

has_key() {
  local file="$1"
  local key="$2"
  if [[ ! -f "${file}" ]]; then
    return 1
  fi
  grep -q -E "^${key}=" "${file}"
}

warns=()
fails=()
result_rows=()

add_warn() {
  warns+=("$1")
}

add_fail() {
  fails+=("$1")
}

check_contract() {
  local name="$1"
  local file="$2"
  shift 2
  local keys=("$@")

  if [[ -z "${file}" || ! -f "${file}" ]]; then
    if [[ ${REQUIRE_ALL} -eq 1 ]]; then
      add_fail "${name}: 缺少 summary"
      result_rows+=("${name}|FAIL|缺少 summary")
    else
      add_warn "${name}: 缺少 summary（本次跳过字段校验）"
      result_rows+=("${name}|WARN|缺少 summary")
    fi
    return
  fi

  local missing=()
  local key
  for key in "${keys[@]}"; do
    if ! has_key "${file}" "${key}"; then
      missing+=("${key}")
    fi
  done

  if (( ${#missing[@]} > 0 )); then
    add_fail "${name}: 缺字段 -> $(printf '%s,' "${missing[@]}" | sed 's/,$//')"
    result_rows+=("${name}|FAIL|$(printf '缺字段:%s' "$(printf '%s,' "${missing[@]}" | sed 's/,$//')")")
  else
    result_rows+=("${name}|PASS|字段完整")
  fi
}

echo "[contract-check] run_dir=${RUN_DIR}"
echo "[contract-check] report_date=${REPORT_DATE}, require_all=${REQUIRE_ALL}, runtime_root=${RUNTIME_ROOT}"

ops_summary="$(latest_summary "${RUNTIME_ROOT}/payment_ops_daily")"
recon_summary="${RUNTIME_ROOT}/payment_reconcile/${REPORT_DATE}/summary.txt"
ticketize_summary="${RUNTIME_ROOT}/payment_reconcile/${REPORT_DATE}/tickets/summary.txt"
idem_summary="$(latest_summary "${RUNTIME_ROOT}/payment_idempotency_regression")"
rehearsal_summary="$(latest_summary "${RUNTIME_ROOT}/payment_cutover_rehearsal")"
apply_summary="$(latest_summary "${RUNTIME_ROOT}/payment_cutover_apply")"
gonogo_summary="$(latest_summary "${RUNTIME_ROOT}/payment_go_nogo")"
warroom_summary="$(latest_summary "${RUNTIME_ROOT}/payment_warroom")"
morning_summary="$(latest_summary "${RUNTIME_ROOT}/payment_ops_morning_bundle")"
reconcile_sla_summary="$(latest_summary "${RUNTIME_ROOT}/payment_reconcile_sla")"
booking_verify_summary="$(latest_summary "${RUNTIME_ROOT}/payment_booking_verify_regression")"
booking_repair_summary="$(latest_summary "${RUNTIME_ROOT}/payment_booking_verify_repair")"
decision_chain_summary="$(latest_summary "${RUNTIME_ROOT}/payment_decision_chain_smoke")"
store_map_import_summary="$(latest_summary_by_prefix "${RUNTIME_ROOT}/payment_store_mapping" "import")"
store_map_template_summary="$(latest_summary_by_prefix "${RUNTIME_ROOT}/payment_store_mapping" "template")"
store_map_audit_summary="$(latest_summary_by_prefix "${RUNTIME_ROOT}/payment_store_mapping" "audit")"
cutover_gate_summary="$(latest_summary "${RUNTIME_ROOT}/payment_cutover_gate")"

check_contract "ops_daily" "${ops_summary}" \
  "run_id" "severity" "preflight_rc" "monitor_rc" "reconcile_rc" "reconcile_summary"
check_contract "reconcile(${REPORT_DATE})" "${recon_summary}" \
  "recon_date" "main_raw_diff_count" "main_cleared_by_refund_count" "main_diff_count" "orphan_wx_count"
check_contract "reconcile_ticketize(${REPORT_DATE})" "${ticketize_summary}" \
  "recon_date" "total_tickets" "p1_count" "p2_count" "escalated_count" "sla_status" "ticket_md" "escalation_md"
check_contract "idempotency" "${idem_summary}" \
  "run_id" "total_findings" "critical_count" "warn_count" "run_dir"
check_contract "cutover_rehearsal" "${rehearsal_summary}" \
  "run_id" "ready_for_order_drill" "preflight_rc" "ops_daily_rc" "idempotency_rc"
check_contract "cutover_apply" "${apply_summary}" \
  "run_id" "mode" "ready" "rolled_back" "run_dir"
check_contract "go_nogo" "${gonogo_summary}" \
  "run_id" "report_date" "decision" "blocker_count" "gate_order_drill" "gate_launch" "ticket_summary" "ticket_gate_ok" "ticket_p1" "require_booking_repair_pass" "booking_repair_gate_ok" "booking_repair_severity" "warroom_date_match" "warroom_report_date" "rehearsal_date_match" "rehearsal_recon_date"
check_contract "warroom" "${warroom_summary}" \
  "run_id" "overall" "risk_count" "latest_go_nogo_summary" "latest_sla_summary" "ticketize_summary" "ticket_p1" "latest_booking_repair_summary" "booking_repair_severity" "go_nogo_date_match" "go_nogo_report_date" "rehearsal_date_match" "rehearsal_recon_date" "latest_decision_chain_summary" "decision_chain_severity" "decision_chain_fail_count" "latest_mapping_audit_summary" "mapping_overall" "mapping_critical_count" "mapping_warn_count" "latest_cutover_gate_summary" "cutover_gate_decision" "cutover_gate_overall" "cutover_gate_block_count" "cutover_gate_warn_count" "cutover_gate_report_date" "cron_decision_chain_enabled"
check_contract "morning_bundle" "${morning_summary}" \
  "run_id" "severity" "gonogo_decision" "warroom_overall" "sla_rc" "sla_severity" "booking_verify_rc" "booking_repair_rc" "require_booking_repair_pass"
check_contract "reconcile_sla" "${reconcile_sla_summary}" \
  "run_id" "severity" "issue_days" "pending_days" "breach_days" "detail_file"
check_contract "booking_verify_regression" "${booking_verify_summary}" \
  "run_id" "severity" "critical_count" "warn_count" "run_dir"
check_contract "booking_verify_repair" "${booking_repair_summary}" \
  "run_id" "mode" "severity" "before_total" "after_total" "run_dir"
check_contract "decision_chain_smoke" "${decision_chain_summary}" \
  "run_id" "severity" "fail_count" "go_nogo_rc" "warroom_rc" "go_nogo_warroom_date_match" "go_nogo_rehearsal_date_match" "warroom_go_nogo_date_match" "warroom_rehearsal_date_match" "run_dir"
check_contract "store_mapping_import" "${store_map_import_summary}" \
  "csv" "mode" "apply_status" "total_store_count" "conflict_count" "run_dir"
check_contract "store_mapping_template_export" "${store_map_template_summary}" \
  "total_store_count" "template_file" "reference_file" "run_dir"
check_contract "store_mapping_audit" "${store_map_audit_summary}" \
  "run_id" "overall" "critical_count" "warn_count" "missing_count" "invalid_count" "duplicate_store_count" "orphan_non_empty_count" "run_dir"
check_contract "cutover_gate" "${cutover_gate_summary}" \
  "run_id" "report_date" "overall" "gate_decision" "block_count" "warn_count" "preflight_rc" "mapping_overall" "mock_replay_overall" "go_nogo_decision" "ops_status_overall" "run_dir"

warn_count="${#warns[@]}"
fail_count="${#fails[@]}"

exit_code=0
if (( fail_count > 0 )); then
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

cat > "${SUMMARY_FILE}" <<TXT
run_id=${RUN_ID}
run_time=$(date '+%Y-%m-%d %H:%M:%S')
report_date=${REPORT_DATE}
require_all=${REQUIRE_ALL}
runtime_root=${RUNTIME_ROOT}
warn_count=${warn_count}
fail_count=${fail_count}
warn_reasons=${warn_text}
fail_reasons=${fail_text}
ops_summary=${ops_summary}
reconcile_summary=${recon_summary}
reconcile_ticketize_summary=${ticketize_summary}
idempotency_summary=${idem_summary}
cutover_rehearsal_summary=${rehearsal_summary}
cutover_apply_summary=${apply_summary}
go_nogo_summary=${gonogo_summary}
warroom_summary=${warroom_summary}
morning_bundle_summary=${morning_summary}
reconcile_sla_summary=${reconcile_sla_summary}
booking_verify_summary=${booking_verify_summary}
booking_repair_summary=${booking_repair_summary}
decision_chain_summary=${decision_chain_summary}
store_mapping_import_summary=${store_map_import_summary}
store_mapping_template_summary=${store_map_template_summary}
store_mapping_audit_summary=${store_map_audit_summary}
cutover_gate_summary=${cutover_gate_summary}
run_dir=${RUN_DIR}
TXT

{
  echo "# 支付 summary 协议回归检查"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`"
  echo "- report_date: \`${REPORT_DATE}\`"
  echo "- require_all: \`${REQUIRE_ALL}\`"
  echo "- runtime_root: \`${RUNTIME_ROOT}\`"
  echo "- warn_count: \`${warn_count}\`"
  echo "- fail_count: \`${fail_count}\`"
  echo
  echo "## 一、检查结果"
  echo
  echo "| 模块 | 结果 | 说明 |"
  echo "|---|---|---|"
  local_row=""
  for local_row in "${result_rows[@]}"; do
    IFS='|' read -r mod lv detail <<< "${local_row}"
    echo "| ${mod} | ${lv} | ${detail} |"
  done
  echo
  echo "## 二、风险明细"
  echo
  if (( fail_count == 0 )); then
    echo "- FAIL：无"
  else
    echo "- FAIL："
    for item in "${fails[@]}"; do
      echo "  - ${item}"
    done
  fi
  if (( warn_count == 0 )); then
    echo "- WARN：无"
  else
    echo "- WARN："
    for item in "${warns[@]}"; do
      echo "  - ${item}"
    done
  fi
  echo
  echo "## 三、追溯路径"
  echo
  echo "- summary: \`${SUMMARY_FILE}\`"
  echo "- ops_summary: \`${ops_summary}\`"
  echo "- reconcile_summary: \`${recon_summary}\`"
  echo "- reconcile_ticketize_summary: \`${ticketize_summary}\`"
  echo "- idempotency_summary: \`${idem_summary}\`"
  echo "- cutover_rehearsal_summary: \`${rehearsal_summary}\`"
  echo "- cutover_apply_summary: \`${apply_summary}\`"
  echo "- go_nogo_summary: \`${gonogo_summary}\`"
  echo "- warroom_summary: \`${warroom_summary}\`"
  echo "- morning_bundle_summary: \`${morning_summary}\`"
  echo "- reconcile_sla_summary: \`${reconcile_sla_summary}\`"
  echo "- decision_chain_summary: \`${decision_chain_summary}\`"
  echo "- store_mapping_import_summary: \`${store_map_import_summary}\`"
  echo "- store_mapping_template_summary: \`${store_map_template_summary}\`"
  echo "- store_mapping_audit_summary: \`${store_map_audit_summary}\`"
  echo "- cutover_gate_summary: \`${cutover_gate_summary}\`"
} > "${REPORT_FILE}"

echo "[contract-check] report=${REPORT_FILE}"
echo "[contract-check] summary=${SUMMARY_FILE}"
echo "[contract-check] warn_count=${warn_count}, fail_count=${fail_count}"

if (( exit_code != 0 && NO_ALERT == 0 )) && [[ -x "${ALERT_SCRIPT}" ]]; then
  "${ALERT_SCRIPT}" \
    --title "支付summary协议回归告警" \
    --content "report_date=${REPORT_DATE}; fail_count=${fail_count}; warn_count=${warn_count}; report=${REPORT_FILE}" || true
fi

exit "${exit_code}"
