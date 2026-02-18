#!/usr/bin/env bash
set -euo pipefail

# D10/D14: 一页式 go/no-go 判定
# - 无订单号：输出是否可进入真实订单演练（GO_FOR_ORDER_DRILL / NO_GO）
# - 有订单号：补跑全链路演练后输出是否可上线（GO_LAUNCH / NO_GO）

ORDER_NO="${ORDER_NO:-}"
REPORT_DATE="${REPORT_DATE:-}"
NO_ALERT=0
REQUIRE_APPLY_READY=0
REQUIRE_BOOKING_REPAIR_PASS=0
OUT_DIR="${OUT_DIR:-}"
TICKET_SUMMARY_FILE="${TICKET_SUMMARY_FILE:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_go_nogo_decision.sh [--date YYYY-MM-DD] [--order-no ORDER_NO] [--ticket-summary PATH] [--require-apply-ready 0|1] [--require-booking-repair-pass 0|1] [--no-alert] [--out-dir PATH]

参数：
  --date YYYY-MM-DD          业务日期（默认昨天，用于定位 ticketize summary）
  --order-no ORDER_NO          指定真实订单号，脚本会执行 fullchain drill 并做最终上线判定
  --ticket-summary PATH        指定 ticketize summary 文件（默认 runtime/payment_reconcile/<date>/tickets/summary.txt）
  --require-apply-ready 0|1    是否要求 latest cutover apply 为 mode=apply 且 ready=1（默认 0）
  --require-booking-repair-pass 0|1  是否要求 latest booking_verify_repair severity=PASS（默认 0）
  --no-alert                   不推送机器人告警
  --out-dir PATH               输出目录（默认 runtime/payment_go_nogo）

退出码：
  0  GO（可进入下一步）
  2  NO_GO（存在阻断项）
  1  脚本执行失败
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
    --ticket-summary)
      TICKET_SUMMARY_FILE="$2"
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

if [[ "${REQUIRE_APPLY_READY}" != "0" && "${REQUIRE_APPLY_READY}" != "1" ]]; then
  echo "参数错误: --require-apply-ready 仅支持 0 或 1"
  exit 1
fi
if [[ "${REQUIRE_BOOKING_REPAIR_PASS}" != "0" && "${REQUIRE_BOOKING_REPAIR_PASS}" != "1" ]]; then
  echo "参数错误: --require-booking-repair-pass 仅支持 0 或 1"
  exit 1
fi
if [[ -z "${REPORT_DATE}" ]]; then
  REPORT_DATE="$(date -d 'yesterday' +%F)"
fi
if ! [[ "${REPORT_DATE}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "参数错误: --date 需要 YYYY-MM-DD"
  exit 1
fi
if [[ -z "${TICKET_SUMMARY_FILE}" ]]; then
  TICKET_SUMMARY_FILE="${ROOT_DIR}/runtime/payment_reconcile/${REPORT_DATE}/tickets/summary.txt"
fi

if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${ROOT_DIR}/runtime/payment_go_nogo"
fi
RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"
SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/decision.md"

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

echo "[go-nogo] run_dir=${RUN_DIR}"
echo "[go-nogo] report_date=${REPORT_DATE}, order_no=${ORDER_NO:-<none>}, require_apply_ready=${REQUIRE_APPLY_READY}, require_booking_repair_pass=${REQUIRE_BOOKING_REPAIR_PASS}"

blockers=()

# 1) warroom
warroom_summary="$(resolve_summary "${ROOT_DIR}/runtime/payment_warroom" "report_date" "${REPORT_DATE}")"
warroom_overall="$(kv "${warroom_summary}" "overall")"
warroom_risk_count="$(kv "${warroom_summary}" "risk_count")"
warroom_report_date="$(kv "${warroom_summary}" "report_date")"
warroom_date_match=1
warroom_ok=0
if [[ -n "${warroom_summary}" ]]; then
  if [[ "${warroom_overall}" == "GREEN" ]] && [[ "${warroom_risk_count:-1}" == "0" ]]; then
    warroom_ok=1
  fi
  if [[ "${warroom_report_date}" != "${REPORT_DATE}" ]]; then
    warroom_date_match=0
    warroom_ok=0
    blockers+=("warroom summary 日期不匹配(report_date=${warroom_report_date:-N/A}, expected=${REPORT_DATE})")
  fi
else
  warroom_date_match=0
fi

# 1.5) ticketize（按业务日期硬门槛）
ticket_total="$(kv "${TICKET_SUMMARY_FILE}" "total_tickets")"
ticket_p1="$(kv "${TICKET_SUMMARY_FILE}" "p1_count")"
ticket_p2="$(kv "${TICKET_SUMMARY_FILE}" "p2_count")"
ticket_sla_status="$(kv "${TICKET_SUMMARY_FILE}" "sla_status")"
ticket_gate_ok=0
if [[ ! -f "${TICKET_SUMMARY_FILE}" ]]; then
  blockers+=("缺少 ticketize summary(${REPORT_DATE})")
else
  if [[ -z "${ticket_p1}" || ! "${ticket_p1}" =~ ^[0-9]+$ ]]; then
    blockers+=("ticketize summary 字段异常(p1_count=${ticket_p1:-N/A})")
  elif (( ticket_p1 > 0 )); then
    blockers+=("存在 P1 工单(${ticket_p1})，需先处置后再进入下一步")
  else
    ticket_gate_ok=1
  fi
fi

# 1.6) booking verify repair（可选门槛）
booking_repair_summary="$(latest_summary "${ROOT_DIR}/runtime/payment_booking_verify_repair")"
booking_repair_severity="$(kv "${booking_repair_summary}" "severity")"
booking_repair_before_total="$(kv "${booking_repair_summary}" "before_total")"
booking_repair_after_total="$(kv "${booking_repair_summary}" "after_total")"
booking_repair_gate_ok=1
if [[ "${REQUIRE_BOOKING_REPAIR_PASS}" == "1" ]]; then
  if [[ -z "${booking_repair_summary}" ]]; then
    booking_repair_gate_ok=0
    blockers+=("缺少 booking_verify_repair summary（已要求 PASS）")
  elif [[ "${booking_repair_severity}" != "PASS" ]]; then
    booking_repair_gate_ok=0
    blockers+=("booking_verify_repair 未通过(severity=${booking_repair_severity:-N/A}, before_total=${booking_repair_before_total:-N/A}, after_total=${booking_repair_after_total:-N/A})")
  fi
fi

# 2) cutover rehearsal
rehearsal_summary="$(resolve_summary "${ROOT_DIR}/runtime/payment_cutover_rehearsal" "recon_date" "${REPORT_DATE}")"
rehearsal_ready="$(kv "${rehearsal_summary}" "ready_for_order_drill")"
rehearsal_recon_date="$(kv "${rehearsal_summary}" "recon_date")"
rehearsal_date_match=1
rehearsal_ok=0
if [[ -n "${rehearsal_summary}" ]]; then
  if [[ "${rehearsal_recon_date}" != "${REPORT_DATE}" ]]; then
    rehearsal_date_match=0
    blockers+=("cutover rehearsal 日期不匹配(recon_date=${rehearsal_recon_date:-N/A}, expected=${REPORT_DATE})")
  fi
  if [[ "${rehearsal_ready}" == "1" && "${rehearsal_date_match}" == "1" ]]; then
    rehearsal_ok=1
  fi
else
  rehearsal_date_match=0
  blockers+=("缺少 cutover rehearsal summary")
fi
if [[ "${rehearsal_ok}" != "1" ]]; then
  blockers+=("cutover rehearsal 未通过(ready_for_order_drill=${rehearsal_ready:-N/A})")
fi

# 3) cutover apply（可选门槛）
apply_summary="$(latest_summary "${ROOT_DIR}/runtime/payment_cutover_apply")"
apply_mode="$(kv "${apply_summary}" "mode")"
apply_ready="$(kv "${apply_summary}" "ready")"
apply_rolled_back="$(kv "${apply_summary}" "rolled_back")"
apply_gate_ok=1
if [[ "${REQUIRE_APPLY_READY}" == "1" ]]; then
  if [[ -z "${apply_summary}" ]]; then
    apply_gate_ok=0
    blockers+=("缺少 cutover apply summary（已要求 apply-ready）")
  else
    if [[ "${apply_mode}" != "apply" || "${apply_ready}" != "1" || "${apply_rolled_back:-0}" != "0" ]]; then
      apply_gate_ok=0
      blockers+=("cutover apply 不满足门槛(mode=${apply_mode:-N/A}, ready=${apply_ready:-N/A}, rolled_back=${apply_rolled_back:-N/A})")
    fi
  fi
fi

# 4) fullchain drill（有订单号时执行）
drill_rc="-"
drill_report=""
if [[ -n "${ORDER_NO}" ]]; then
  drill_rc="$(run_step "01_fullchain_drill" ./shell/payment_fullchain_drill.sh --order-no "${ORDER_NO}")"
  drill_report="$(sed -n 's/^\[drill\] 演练报告已生成: //p' "${RUN_DIR}/01_fullchain_drill.log" | tail -n 1 || true)"
  if [[ "${drill_rc}" != "0" ]]; then
    blockers+=("fullchain drill 失败(rc=${drill_rc})")
  fi
fi

gate_order_drill=0
if [[ "${rehearsal_ok}" == "1" && "${ticket_gate_ok}" == "1" ]]; then
  if [[ "${booking_repair_gate_ok}" == "1" ]]; then
    if [[ "${REQUIRE_APPLY_READY}" == "1" ]]; then
      if [[ "${apply_gate_ok}" == "1" ]]; then
        gate_order_drill=1
      fi
    else
      gate_order_drill=1
    fi
  fi
fi

gate_launch=0
if [[ -n "${ORDER_NO}" ]]; then
  if [[ "${gate_order_drill}" == "1" && "${drill_rc}" == "0" && "${apply_gate_ok}" == "1" ]]; then
    gate_launch=1
  fi
fi

decision="NO_GO"
if [[ -z "${ORDER_NO}" ]]; then
  if [[ "${gate_order_drill}" == "1" ]]; then
    decision="GO_FOR_ORDER_DRILL"
  fi
else
  if [[ "${gate_launch}" == "1" ]]; then
    decision="GO_LAUNCH"
  fi
fi

blocker_count="${#blockers[@]}"

{
  echo "run_id=${RUN_ID}"
  echo "run_time=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "report_date=${REPORT_DATE}"
  echo "order_no=${ORDER_NO}"
  echo "require_apply_ready=${REQUIRE_APPLY_READY}"
  echo "require_booking_repair_pass=${REQUIRE_BOOKING_REPAIR_PASS}"
  echo "decision=${decision}"
  echo "blocker_count=${blocker_count}"
  echo "gate_order_drill=${gate_order_drill}"
  echo "gate_launch=${gate_launch}"
  echo "ticket_summary=${TICKET_SUMMARY_FILE}"
  echo "ticket_gate_ok=${ticket_gate_ok}"
  echo "ticket_total=${ticket_total}"
  echo "ticket_p1=${ticket_p1}"
  echo "ticket_p2=${ticket_p2}"
  echo "ticket_sla_status=${ticket_sla_status}"
  echo "booking_repair_summary=${booking_repair_summary}"
  echo "booking_repair_gate_ok=${booking_repair_gate_ok}"
  echo "booking_repair_severity=${booking_repair_severity}"
  echo "booking_repair_before_total=${booking_repair_before_total}"
  echo "booking_repair_after_total=${booking_repair_after_total}"
  echo "warroom_summary=${warroom_summary}"
  echo "warroom_ok=${warroom_ok}"
  echo "warroom_overall=${warroom_overall}"
  echo "warroom_risk_count=${warroom_risk_count}"
  echo "warroom_report_date=${warroom_report_date}"
  echo "warroom_date_match=${warroom_date_match}"
  echo "rehearsal_summary=${rehearsal_summary}"
  echo "rehearsal_ok=${rehearsal_ok}"
  echo "rehearsal_ready=${rehearsal_ready}"
  echo "rehearsal_recon_date=${rehearsal_recon_date}"
  echo "rehearsal_date_match=${rehearsal_date_match}"
  echo "apply_summary=${apply_summary}"
  echo "apply_mode=${apply_mode}"
  echo "apply_ready=${apply_ready}"
  echo "apply_rolled_back=${apply_rolled_back}"
  echo "apply_gate_ok=${apply_gate_ok}"
  echo "drill_rc=${drill_rc}"
  echo "drill_report=${drill_report}"
  echo "run_dir=${RUN_DIR}"
} > "${SUMMARY_FILE}"

{
  echo "# 支付上线 go/no-go 判定"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`"
  echo "- report_date: \`${REPORT_DATE}\`"
  echo "- order_no: \`${ORDER_NO:-<none>}\`"
  echo "- require_apply_ready: \`${REQUIRE_APPLY_READY}\`"
  echo "- require_booking_repair_pass: \`${REQUIRE_BOOKING_REPAIR_PASS}\`"
  echo "- decision: **${decision}**"
  echo
  echo "## 门槛检查"
  echo
  echo "| 检查项 | 结果 | 依据 |"
  echo "|---|---|---|"
  echo "| ticketize(${REPORT_DATE}) | $([[ ${ticket_gate_ok} -eq 1 ]] && echo PASS || echo FAIL) | total=${ticket_total:-N/A}, p1=${ticket_p1:-N/A}, p2=${ticket_p2:-N/A}, sla=${ticket_sla_status:-N/A} |"
  echo "| booking_verify_repair(latest) | $([[ ${booking_repair_gate_ok} -eq 1 ]] && echo PASS || echo FAIL) | severity=${booking_repair_severity:-N/A}, before_total=${booking_repair_before_total:-N/A}, after_total=${booking_repair_after_total:-N/A} |"
  echo "| warroom | $([[ ${warroom_ok} -eq 1 && ${warroom_date_match} -eq 1 ]] && echo PASS || echo FAIL) | overall=${warroom_overall:-N/A}, risk_count=${warroom_risk_count:-N/A}, report_date=${warroom_report_date:-N/A}, date_match=${warroom_date_match} |"
  echo "| cutover rehearsal | $([[ ${rehearsal_ok} -eq 1 && ${rehearsal_date_match} -eq 1 ]] && echo PASS || echo FAIL) | ready_for_order_drill=${rehearsal_ready:-N/A}, recon_date=${rehearsal_recon_date:-N/A}, date_match=${rehearsal_date_match} |"
  echo "| cutover apply gate | $([[ ${apply_gate_ok} -eq 1 ]] && echo PASS || echo FAIL) | mode=${apply_mode:-N/A}, ready=${apply_ready:-N/A}, rolled_back=${apply_rolled_back:-N/A} |"
  echo "| fullchain drill | $([[ \"${drill_rc}\" == \"-\" ]] && echo SKIP || ([[ \"${drill_rc}\" == \"0\" ]] && echo PASS || echo FAIL)) | rc=${drill_rc} |"
  echo
  echo "## 决策结论"
  echo
  echo "- gate_order_drill: \`${gate_order_drill}\`"
  echo "- gate_launch: \`${gate_launch}\`"
  echo "- blocker_count: \`${blocker_count}\`"
  echo
  if [[ ${blocker_count} -gt 0 ]]; then
    echo "### 阻断项"
    for b in "${blockers[@]}"; do
      echo "- ${b}"
    done
    echo
  fi
  echo "## 追溯文件"
  echo
  echo "- summary: \`${SUMMARY_FILE}\`"
  echo "- ticket_summary: \`${TICKET_SUMMARY_FILE}\`"
  echo "- booking_repair_summary: \`${booking_repair_summary}\`"
  echo "- warroom: \`${warroom_summary}\`"
  echo "- warroom_report_date: \`${warroom_report_date:-N/A}\`"
  echo "- warroom_date_match: \`${warroom_date_match}\`"
  echo "- rehearsal: \`${rehearsal_summary}\`"
  echo "- rehearsal_recon_date: \`${rehearsal_recon_date:-N/A}\`"
  echo "- rehearsal_date_match: \`${rehearsal_date_match}\`"
  echo "- apply: \`${apply_summary}\`"
  echo "- drill: \`${drill_report}\`"
} > "${REPORT_FILE}"

echo "[go-nogo] summary=${SUMMARY_FILE}"
echo "[go-nogo] report=${REPORT_FILE}"
echo "[go-nogo] decision=${decision}, blocker_count=${blocker_count}"

if [[ "${decision}" == "NO_GO" ]]; then
  if [[ ${NO_ALERT} -eq 0 ]] && [[ -x "${ALERT_SCRIPT}" ]]; then
    "${ALERT_SCRIPT}" \
      --title "支付上线判定告警" \
      --content "decision=${decision}; blocker_count=${blocker_count}; order_no=${ORDER_NO:-<none>}; report=${REPORT_FILE}" || true
  fi
  exit 2
fi

exit 0
