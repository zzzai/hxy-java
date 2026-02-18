#!/usr/bin/env bash
set -euo pipefail

# 支付故障应急证据打包脚本
# 用途：在发生支付异常时，一键收集值守/上线拦截规则/对账/日志证据，便于总部快速处置。

REPORT_DATE="${REPORT_DATE:-}"
ORDER_NO="${ORDER_NO:-}"
LOG_LINES="${LOG_LINES:-300}"
OUT_DIR="${OUT_DIR:-}"
RUNTIME_ROOT="${RUNTIME_ROOT:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_incident_bundle.sh [--date YYYY-MM-DD] [--order-no ORDER_NO] [--log-lines N] [--runtime-root PATH] [--out-dir PATH]

参数：
  --date YYYY-MM-DD      事件日期（默认昨天）
  --order-no ORDER_NO    可选，支付商户单号/out_trade_no/transaction_id 任一
  --log-lines N          每个日志文件截取行数（默认 300）
  --runtime-root PATH    runtime 根目录（默认 <repo>/runtime）
  --out-dir PATH         输出目录（默认 runtime/payment_incident_bundle）

退出码：
  0  打包成功
  1  脚本执行错误
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --date)
      REPORT_DATE="$2"
      shift 2
      ;;
    --order-no)
      ORDER_NO="$2"
      shift 2
      ;;
    --log-lines)
      LOG_LINES="$2"
      shift 2
      ;;
    --runtime-root)
      RUNTIME_ROOT="$2"
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
if ! [[ "${LOG_LINES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --log-lines 必须是正整数"
  exit 1
fi
if [[ -z "${RUNTIME_ROOT}" ]]; then
  RUNTIME_ROOT="${ROOT_DIR}/runtime"
fi
if [[ ! -d "${RUNTIME_ROOT}" ]]; then
  echo "runtime 目录不存在: ${RUNTIME_ROOT}"
  exit 1
fi
if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${RUNTIME_ROOT}/payment_incident_bundle"
fi

RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
EVID_DIR="${RUN_DIR}/evidence"
mkdir -p "${EVID_DIR}"

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

copy_summary() {
  local name="$1"
  local src="$2"
  local dst="${EVID_DIR}/${name}.summary.txt"
  if [[ -f "${src}" ]]; then
    cp "${src}" "${dst}"
    printf '%s' "${dst}"
  else
    printf ''
  fi
}

latest_log_file() {
  local dir="$1"
  if [[ ! -d "${dir}" ]]; then
    printf ''
    return
  fi
  find "${dir}" -type f \( -name '*.log' -o -name '*.out' \) -printf '%T@ %p\n' 2>/dev/null \
    | sort -n \
    | tail -n 1 \
    | cut -d' ' -f2- || true
}

echo "[payment-incident-bundle] run_dir=${RUN_DIR}"
echo "[payment-incident-bundle] report_date=${REPORT_DATE}"
if [[ -n "${ORDER_NO}" ]]; then
  echo "[payment-incident-bundle] order_no=${ORDER_NO}"
fi

ops_summary="$(resolve_summary "${RUNTIME_ROOT}/payment_ops_status" "report_date" "${REPORT_DATE}")"
warroom_summary="$(resolve_summary "${RUNTIME_ROOT}/payment_warroom" "report_date" "${REPORT_DATE}")"
gate_summary="$(resolve_summary "${RUNTIME_ROOT}/payment_cutover_gate" "report_date" "${REPORT_DATE}")"
go_nogo_summary="$(resolve_summary "${RUNTIME_ROOT}/payment_go_nogo" "report_date" "${REPORT_DATE}")"
reconcile_summary="${RUNTIME_ROOT}/payment_reconcile/${REPORT_DATE}/summary.txt"
refund_summary="$(latest_summary "${RUNTIME_ROOT}/payment_refund_convergence")"
exception_summary="$(latest_summary "${RUNTIME_ROOT}/payment_exception_acceptance")"

ops_evidence="$(copy_summary "ops_status" "${ops_summary}")"
warroom_evidence="$(copy_summary "warroom" "${warroom_summary}")"
gate_evidence="$(copy_summary "cutover_gate" "${gate_summary}")"
go_nogo_evidence="$(copy_summary "go_nogo" "${go_nogo_summary}")"
reconcile_evidence="$(copy_summary "reconcile" "${reconcile_summary}")"
refund_evidence="$(copy_summary "refund_convergence" "${refund_summary}")"
exception_evidence="$(copy_summary "exception_acceptance" "${exception_summary}")"

order_locator_rc=""
order_locator_log=""
if [[ -n "${ORDER_NO}" ]]; then
  order_locator_log="${RUN_DIR}/order_locator.log"
  set +e
  (
    cd "${ROOT_DIR}"
    ./shell/payment_order_locator.sh --order-no "${ORDER_NO}"
  ) >"${order_locator_log}" 2>&1
  order_locator_rc=$?
  set -e
fi

admin_log_src="$(latest_log_file "${ROOT_DIR}/crmeb-admin/crmeb_admin_log")"
front_log_src="$(latest_log_file "${ROOT_DIR}/crmeb-front/crmeb_front_log")"

admin_log_tail="${EVID_DIR}/admin_latest.log"
front_log_tail="${EVID_DIR}/front_latest.log"
monitor_log_tail="${EVID_DIR}/payment_monitor_cron.log"
ops_cron_log_tail="${EVID_DIR}/payment_ops_cron.log"

if [[ -f "${admin_log_src}" ]]; then
  tail -n "${LOG_LINES}" "${admin_log_src}" > "${admin_log_tail}" || true
fi
if [[ -f "${front_log_src}" ]]; then
  tail -n "${LOG_LINES}" "${front_log_src}" > "${front_log_tail}" || true
fi
if [[ -f "${RUNTIME_ROOT}/payment_monitor_cron.log" ]]; then
  tail -n "${LOG_LINES}" "${RUNTIME_ROOT}/payment_monitor_cron.log" > "${monitor_log_tail}" || true
fi
if [[ -f "${RUNTIME_ROOT}/payment_ops_cron.log" ]]; then
  tail -n "${LOG_LINES}" "${RUNTIME_ROOT}/payment_ops_cron.log" > "${ops_cron_log_tail}" || true
fi

ops_overall="$(kv "${ops_summary}" "overall")"
gate_decision="$(kv "${gate_summary}" "gate_decision")"
gate_overall="$(kv "${gate_summary}" "overall")"
gate_block_count="$(kv "${gate_summary}" "block_count")"
gate_warn_count="$(kv "${gate_summary}" "warn_count")"
reconcile_main_diff="$(kv "${reconcile_summary}" "main_diff_count")"
reconcile_orphan_wx="$(kv "${reconcile_summary}" "orphan_wx_count")"

incident_level="P2"
incident_reason="observed"
if [[ "${ops_overall}" == "RED" || "${gate_decision}" == "NO_GO" ]]; then
  incident_level="P0"
  incident_reason="core-gate-blocked"
elif [[ "${ops_overall}" == "YELLOW" || "${gate_decision}" == "GO_WITH_WARN" || "${gate_overall}" == "YELLOW" ]]; then
  incident_level="P1"
  incident_reason="risk-warning"
fi

{
  echo "report_date=${REPORT_DATE}"
  echo "order_no=${ORDER_NO}"
  echo "incident_level=${incident_level}"
  echo "incident_reason=${incident_reason}"
  echo "ops_status_overall=${ops_overall:-N/A}"
  echo "cutover_gate_overall=${gate_overall:-N/A}"
  echo "cutover_gate_decision=${gate_decision:-N/A}"
  echo "cutover_gate_block_count=${gate_block_count:-N/A}"
  echo "cutover_gate_warn_count=${gate_warn_count:-N/A}"
  echo "reconcile_main_diff_count=${reconcile_main_diff:-N/A}"
  echo "reconcile_orphan_wx_count=${reconcile_orphan_wx:-N/A}"
  echo "order_locator_rc=${order_locator_rc:-N/A}"
  echo "ops_status_summary=${ops_summary:-N/A}"
  echo "warroom_summary=${warroom_summary:-N/A}"
  echo "cutover_gate_summary=${gate_summary:-N/A}"
  echo "go_nogo_summary=${go_nogo_summary:-N/A}"
  echo "reconcile_summary=${reconcile_summary:-N/A}"
  echo "refund_convergence_summary=${refund_summary:-N/A}"
  echo "exception_acceptance_summary=${exception_summary:-N/A}"
  echo "run_dir=${RUN_DIR}"
} > "${SUMMARY_FILE}"

{
  echo "# 支付故障应急证据包"
  echo
  echo "- 生成时间：$(date '+%F %T')"
  echo "- 事件日期：${REPORT_DATE}"
  echo "- 订单号：${ORDER_NO:-<未指定>}"
  echo "- 事件级别：${incident_level}"
  echo "- 判断原因：${incident_reason}"
  echo
  echo "## 快速结论"
  echo
  echo "- ops_status：\`${ops_overall:-N/A}\`"
  echo "- cutover_gate：\`${gate_overall:-N/A}\` / \`${gate_decision:-N/A}\`"
  echo "- 对账差异：main_diff=\`${reconcile_main_diff:-N/A}\`, orphan_wx=\`${reconcile_orphan_wx:-N/A}\`"
  echo "- 订单定位：rc=\`${order_locator_rc:-N/A}\`"
  echo
  echo "## 证据清单"
  echo
  [[ -n "${ops_evidence}" ]] && echo "- ops_status：\`${ops_evidence}\`"
  [[ -n "${warroom_evidence}" ]] && echo "- warroom：\`${warroom_evidence}\`"
  [[ -n "${gate_evidence}" ]] && echo "- cutover_gate：\`${gate_evidence}\`"
  [[ -n "${go_nogo_evidence}" ]] && echo "- go_nogo：\`${go_nogo_evidence}\`"
  [[ -n "${reconcile_evidence}" ]] && echo "- reconcile：\`${reconcile_evidence}\`"
  [[ -n "${refund_evidence}" ]] && echo "- refund_convergence：\`${refund_evidence}\`"
  [[ -n "${exception_evidence}" ]] && echo "- exception_acceptance：\`${exception_evidence}\`"
  [[ -n "${order_locator_log}" ]] && echo "- order_locator：\`${order_locator_log}\`"
  [[ -f "${admin_log_tail}" ]] && echo "- admin log tail：\`${admin_log_tail}\`"
  [[ -f "${front_log_tail}" ]] && echo "- front log tail：\`${front_log_tail}\`"
  [[ -f "${monitor_log_tail}" ]] && echo "- monitor cron log tail：\`${monitor_log_tail}\`"
  [[ -f "${ops_cron_log_tail}" ]] && echo "- ops cron log tail：\`${ops_cron_log_tail}\`"
  echo
  echo "## 处置建议（按级别）"
  echo
  echo "- P0：立即冻结上线与退款自动任务，执行 'payment_cutover_gate.sh --require-refund-green 1' 复核，并启动人工值守。"
  echo "- P1：保持上线阻断规则开启，优先消除 'cutover_gate' / 'ops_status' 告警项，再恢复自动化。"
  echo "- P2：持续观察并记录，无需立即干预。"
} > "${REPORT_FILE}"

echo "[payment-incident-bundle] summary=${SUMMARY_FILE}"
echo "[payment-incident-bundle] report=${REPORT_FILE}"
echo "[payment-incident-bundle] incident_level=${incident_level}"
