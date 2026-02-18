#!/usr/bin/env bash
set -euo pipefail

# D44: 切换上线拦截规则统一收敛
# 目标：把切换前关键检查收敛为一个 GO/NO_GO 结论。

REPORT_DATE="${REPORT_DATE:-}"
ORDER_NO="${ORDER_NO:-}"
OUT_DIR="${OUT_DIR:-}"
NO_ALERT=0

REQUIRE_APPLY_READY="${REQUIRE_APPLY_READY:-0}"
REQUIRE_BOOKING_REPAIR_PASS="${REQUIRE_BOOKING_REPAIR_PASS:-0}"
REQUIRE_MAPPING_SMOKE_GREEN="${REQUIRE_MAPPING_SMOKE_GREEN:-0}"
MAPPING_STRICT_MISSING="${MAPPING_STRICT_MISSING:-0}"
REQUIRE_MAPPING_GREEN="${REQUIRE_MAPPING_GREEN:-0}"
REQUIRE_MOCK_GREEN="${REQUIRE_MOCK_GREEN:-0}"
REQUIRE_REFUND_GREEN="${REQUIRE_REFUND_GREEN:-0}"
PREFLIGHT_STRICT="${PREFLIGHT_STRICT:-0}"
REFUND_WINDOW_HOURS="${REFUND_WINDOW_HOURS:-72}"
REFUND_TIMEOUT_MINUTES="${REFUND_TIMEOUT_MINUTES:-30}"
ALLOW_SHARED_SUBMCHID="${ALLOW_SHARED_SUBMCHID:-0}"

SKIP_PREFLIGHT=0
SKIP_MAPPING_AUDIT=0
SKIP_MOCK_REPLAY=0
SKIP_REFUND_CONVERGENCE=0
SKIP_OPS_STATUS=0

OWNER_MAP_FILE="${OWNER_MAP_FILE:-}"
OWNER_DEFAULT="${OWNER_DEFAULT:-payment-ops}"
OWNER_P1="${OWNER_P1:-payment-oncall}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_cutover_gate.sh [--date YYYY-MM-DD] [--order-no ORDER_NO]
    [--require-apply-ready 0|1] [--require-booking-repair-pass 0|1] [--require-mapping-smoke-green 0|1]
    [--mapping-strict-missing 0|1] [--require-mapping-green 0|1] [--require-mock-green 0|1] [--require-refund-green 0|1]
    [--refund-window-hours N] [--refund-timeout-minutes N]
    [--preflight-strict 0|1] [--allow-shared-submchid 0|1]
    [--owner-map-file FILE] [--owner-default NAME] [--owner-p1 NAME]
    [--skip-preflight] [--skip-mapping-audit] [--skip-mock-replay] [--skip-refund-convergence] [--skip-ops-status]
    [--no-alert] [--out-dir PATH]

参数：
  --date YYYY-MM-DD                业务日期（默认昨天）
  --order-no ORDER_NO              真实订单号（有订单时进入 GO_LAUNCH 判定）
  --require-apply-ready 0|1        go_nogo/ops_status 是否要求 apply-ready（默认 0）
  --require-booking-repair-pass 0|1 是否要求 booking_verify_repair=PASS（默认 0）
  --require-mapping-smoke-green 0|1 是否要求 go_nogo 的 mapping_smoke=GREEN（默认 0）
  --mapping-strict-missing 0|1     映射审计是否把 missing 视为阻断（默认 0）
  --require-mapping-green 0|1      是否要求映射审计必须 GREEN（默认 0，YELLOW 记告警不阻断）
  --require-mock-green 0|1         是否要求 mock 回放必须 GREEN（默认 0，YELLOW 记告警不阻断）
  --require-refund-green 0|1       是否要求退款收敛巡检必须 GREEN（默认 0，GREEN_WITH_WARN 记告警不阻断）
  --refund-window-hours N          退款收敛巡检窗口（默认 72）
  --refund-timeout-minutes N       退款收敛超时阈值（默认 30）
  --allow-shared-submchid 0|1      是否允许多个门店共享同一 sub_mchid（默认 0）
  --preflight-strict 0|1           preflight 是否开启 strict（默认 0）
  --owner-map-file FILE            mock 回放里 decision_ticketize 的 owner 规则文件
  --owner-default NAME             默认 owner（默认 payment-ops）
  --owner-p1 NAME                  P1 默认 owner（默认 payment-oncall）
  --skip-preflight                 跳过 preflight
  --skip-mapping-audit             跳过门店映射审计
  --skip-mock-replay               跳过 mock 回放
  --skip-refund-convergence        跳过退款收敛巡检
  --skip-ops-status                跳过 ops_status
  --no-alert                       NO_GO 时不推送机器人
  --out-dir PATH                   输出目录（默认 runtime/payment_cutover_gate）

退出码：
  0  上线拦截规则通过（GO / GO_WITH_WARN）
  2  上线拦截规则不通过（NO_GO）
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
    --require-apply-ready)
      REQUIRE_APPLY_READY="$2"
      shift 2
      ;;
    --require-booking-repair-pass)
      REQUIRE_BOOKING_REPAIR_PASS="$2"
      shift 2
      ;;
    --require-mapping-smoke-green)
      REQUIRE_MAPPING_SMOKE_GREEN="$2"
      shift 2
      ;;
    --mapping-strict-missing)
      MAPPING_STRICT_MISSING="$2"
      shift 2
      ;;
    --require-mapping-green)
      REQUIRE_MAPPING_GREEN="$2"
      shift 2
      ;;
    --require-mock-green)
      REQUIRE_MOCK_GREEN="$2"
      shift 2
      ;;
    --require-refund-green)
      REQUIRE_REFUND_GREEN="$2"
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
    --preflight-strict)
      PREFLIGHT_STRICT="$2"
      shift 2
      ;;
    --allow-shared-submchid)
      ALLOW_SHARED_SUBMCHID="$2"
      shift 2
      ;;
    --owner-map-file)
      OWNER_MAP_FILE="$2"
      shift 2
      ;;
    --owner-default)
      OWNER_DEFAULT="$2"
      shift 2
      ;;
    --owner-p1)
      OWNER_P1="$2"
      shift 2
      ;;
    --skip-preflight)
      SKIP_PREFLIGHT=1
      shift
      ;;
    --skip-mapping-audit)
      SKIP_MAPPING_AUDIT=1
      shift
      ;;
    --skip-mock-replay)
      SKIP_MOCK_REPLAY=1
      shift
      ;;
    --skip-refund-convergence)
      SKIP_REFUND_CONVERGENCE=1
      shift
      ;;
    --skip-ops-status)
      SKIP_OPS_STATUS=1
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

for sw in \
  "${REQUIRE_APPLY_READY}" \
  "${REQUIRE_BOOKING_REPAIR_PASS}" \
  "${REQUIRE_MAPPING_SMOKE_GREEN}" \
  "${MAPPING_STRICT_MISSING}" \
  "${REQUIRE_MAPPING_GREEN}" \
  "${REQUIRE_MOCK_GREEN}" \
  "${REQUIRE_REFUND_GREEN}" \
  "${ALLOW_SHARED_SUBMCHID}" \
  "${PREFLIGHT_STRICT}"; do
  if [[ "${sw}" != "0" && "${sw}" != "1" ]]; then
    echo "参数错误: 开关参数仅支持 0 或 1"
    exit 1
  fi
done
if ! [[ "${REFUND_WINDOW_HOURS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --refund-window-hours 必须是正整数"
  exit 1
fi
if ! [[ "${REFUND_TIMEOUT_MINUTES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --refund-timeout-minutes 必须是正整数"
  exit 1
fi
if [[ -z "${OWNER_DEFAULT}" || -z "${OWNER_P1}" ]]; then
  echo "参数错误: --owner-default/--owner-p1 不能为空"
  exit 1
fi
if [[ -n "${OWNER_MAP_FILE}" && ! -f "${OWNER_MAP_FILE}" ]]; then
  echo "参数错误: --owner-map-file 文件不存在 -> ${OWNER_MAP_FILE}"
  exit 1
fi
if [[ -z "${REPORT_DATE}" ]]; then
  REPORT_DATE="$(date -d 'yesterday' +%F)"
fi
if ! [[ "${REPORT_DATE}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "参数错误: --date 需要 YYYY-MM-DD"
  exit 1
fi

if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${ROOT_DIR}/runtime/payment_cutover_gate"
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
  find "${base}" -maxdepth 3 -type f -name 'summary.txt' -printf '%T@ %p\n' 2>/dev/null \
    | sort -n \
    | tail -n 1 \
    | cut -d' ' -f2- || true
}

join_by_semicolon() {
  local arr=("$@")
  if (( ${#arr[@]} == 0 )); then
    printf ''
    return
  fi
  local out=""
  local item=""
  for item in "${arr[@]}"; do
    if [[ -n "${out}" ]]; then
      out="${out}; ${item}"
    else
      out="${item}"
    fi
  done
  printf '%s' "${out}"
}

hint_for_reason() {
  local reason="$1"
  case "${reason}" in
    *"preflight"*)
      printf '%s' "执行 ./shell/payment_preflight_check.sh --strict，按 preflight_fail_items 逐项修复后重试"
      ;;
    *"store_mapping_audit"*|*"mapping_audit"*)
      printf '%s' "先跑 ./shell/payment_store_mapping_audit.sh --strict-missing 1，清理 missing/placeholder/invalid 后再重试"
      ;;
    *"mock_replay"*)
      printf '%s' "执行 ./shell/payment_mock_replay.sh --date ${REPORT_DATE}，根据工单输出修复映射与配置"
      ;;
    *"refund_convergence"*)
      printf '%s' "执行 ./shell/payment_refund_convergence_check.sh --window-hours ${REFUND_WINDOW_HOURS} --refund-timeout-minutes ${REFUND_TIMEOUT_MINUTES}，先清零 R01/R02 阻断项"
      ;;
    *"go_nogo"*)
      printf '%s' "执行 ./shell/payment_go_nogo_decision.sh --date ${REPORT_DATE}，先清空 blocker 再切换"
      ;;
    *"ops_status"*)
      printf '%s' "执行 ./shell/payment_ops_status.sh --date ${REPORT_DATE} --refresh，先把 RED 项清零"
      ;;
    *"booking_verify_repair"*)
      printf '%s' "执行 ./shell/payment_booking_verify_repair.sh --window-hours 72 --apply，再复查"
      ;;
    *"日期不匹配"*|*"date"*|*"report_date"*|*"recon_date"*)
      printf '%s' "补齐同日产物：按 --date ${REPORT_DATE} 重新执行 warroom/go_nogo/rehearsal 链路"
      ;;
    *"cutover rehearsal"*|*"ready_for_order_drill"*)
      printf '%s' "重跑 ./shell/payment_cutover_rehearsal.sh --date ${REPORT_DATE}，确保 ready_for_order_drill=1"
      ;;
    *)
      printf '%s' "按报告中的 step 日志定位根因并重跑 payment_cutover_gate.sh 复核"
      ;;
  esac
}

declare -a BLOCKERS=()
declare -a WARNS=()
declare -a ROWS=()

add_row() {
  ROWS+=("$1|$2|$3|$4|$5")
}
add_block() {
  BLOCKERS+=("$1")
}
add_warn() {
  WARNS+=("$1")
}

run_step() {
  local step="$1"
  shift
  local log="${RUN_DIR}/${step}.log"
  set +e
  "$@" > "${log}" 2>&1
  local rc=$?
  set -e
  printf '%s' "${rc}"
}

echo "[cutover-gate] run_dir=${RUN_DIR}"
echo "[cutover-gate] report_date=${REPORT_DATE}, order_no=${ORDER_NO:-<none>}, require_apply_ready=${REQUIRE_APPLY_READY}, require_booking_repair_pass=${REQUIRE_BOOKING_REPAIR_PASS}, require_mapping_smoke_green=${REQUIRE_MAPPING_SMOKE_GREEN}, preflight_strict=${PREFLIGHT_STRICT}, require_refund_green=${REQUIRE_REFUND_GREEN}, allow_shared_submchid=${ALLOW_SHARED_SUBMCHID}"

preflight_rc="-"
preflight_fail_count="-"
preflight_fail_items=""
if [[ ${SKIP_PREFLIGHT} -eq 0 ]]; then
  preflight_cmd=(./shell/payment_preflight_check.sh --out-dir "${RUN_DIR}/preflight")
  if [[ "${PREFLIGHT_STRICT}" == "1" ]]; then
    preflight_cmd+=(--strict)
  fi
  preflight_rc="$(run_step "01_preflight" "${preflight_cmd[@]}")"
  preflight_report="$(find "${RUN_DIR}/preflight" -maxdepth 1 -type f -name 'preflight-*.md' -printf '%T@ %p\n' 2>/dev/null | sort -n | tail -n 1 | cut -d' ' -f2- || true)"
  if [[ -n "${preflight_report}" && -f "${preflight_report}" ]]; then
    preflight_fail_count="$(grep -c '\[FAIL\]' "${preflight_report}" || true)"
    preflight_fail_items="$(grep '\[FAIL\]' "${preflight_report}" | sed 's/^- \[FAIL\] //g' | head -n 5 | tr '\n' '; ' | sed 's/[; ]*$//' || true)"
  fi

  if [[ "${preflight_rc}" == "1" ]]; then
    add_block "preflight 执行失败(rc=1)"
    add_row "preflight" "FAIL" "${preflight_rc}" "BLOCK" "strict=${PREFLIGHT_STRICT}, fail_items=${preflight_fail_count}"
  elif [[ "${preflight_rc}" == "2" ]]; then
    if [[ "${preflight_fail_count}" =~ ^[0-9]+$ ]] && (( preflight_fail_count == 0 )); then
      add_warn "preflight 存在 WARN(rc=2, strict=${PREFLIGHT_STRICT})"
      add_row "preflight" "WARN" "${preflight_rc}" "WARN" "strict=${PREFLIGHT_STRICT}, fail_items=0"
    else
      add_block "preflight 未通过(rc=2, fail_items=${preflight_fail_count:-N/A})"
      add_row "preflight" "FAIL" "${preflight_rc}" "BLOCK" "strict=${PREFLIGHT_STRICT}, fail_items=${preflight_fail_count:-N/A}"
    fi
  else
    add_row "preflight" "PASS" "${preflight_rc}" "-" "strict=${PREFLIGHT_STRICT}, fail_items=${preflight_fail_count:-0}"
  fi
else
  add_row "preflight" "SKIP" "${preflight_rc}" "-" "skip_preflight=1"
fi

mapping_rc="-"
mapping_overall="-"
if [[ ${SKIP_MAPPING_AUDIT} -eq 0 ]]; then
  mapping_rc="$(run_step "02_mapping_audit" ./shell/payment_store_mapping_audit.sh --strict-missing "${MAPPING_STRICT_MISSING}" --allow-shared-submchid "${ALLOW_SHARED_SUBMCHID}" --out-dir "${RUN_DIR}/mapping_audit" --no-alert)"
  mapping_summary="$(latest_summary "${RUN_DIR}/mapping_audit")"
  mapping_overall="$(kv "${mapping_summary}" "overall")"
  if [[ "${mapping_rc}" == "1" ]]; then
    add_block "store_mapping_audit 执行失败(rc=1)"
    add_row "mapping_audit" "FAIL" "${mapping_rc}" "BLOCK" "overall=${mapping_overall:-N/A}"
  elif [[ "${mapping_rc}" == "2" ]]; then
    if [[ "${REQUIRE_MAPPING_GREEN}" == "1" ]]; then
      add_block "store_mapping_audit 非 GREEN(overall=${mapping_overall:-YELLOW/RED})"
      add_row "mapping_audit" "FAIL" "${mapping_rc}" "BLOCK" "require_mapping_green=1, overall=${mapping_overall:-N/A}"
    else
      add_warn "store_mapping_audit 非 GREEN(overall=${mapping_overall:-YELLOW/RED})"
      add_row "mapping_audit" "WARN" "${mapping_rc}" "WARN" "overall=${mapping_overall:-N/A}"
    fi
  else
    add_row "mapping_audit" "PASS" "${mapping_rc}" "-" "overall=${mapping_overall:-GREEN}"
  fi
else
  add_row "mapping_audit" "SKIP" "${mapping_rc}" "-" "skip_mapping_audit=1"
fi

mock_rc="-"
mock_overall="-"
if [[ ${SKIP_MOCK_REPLAY} -eq 0 ]]; then
  mock_cmd=(env ALLOW_SHARED_SUBMCHID="${ALLOW_SHARED_SUBMCHID}" ./shell/payment_mock_replay.sh --date "${REPORT_DATE}" --out-dir "${RUN_DIR}/mock_replay" --owner-default "${OWNER_DEFAULT}" --owner-p1 "${OWNER_P1}" --no-alert)
  if [[ ${SKIP_MAPPING_AUDIT} -eq 0 ]]; then
    mock_cmd+=(--skip-store-mapping-audit)
  fi
  if [[ -n "${OWNER_MAP_FILE}" ]]; then
    mock_cmd+=(--owner-map-file "${OWNER_MAP_FILE}")
  fi
  mock_rc="$(run_step "03_mock_replay" "${mock_cmd[@]}")"
  mock_summary="$(latest_summary "${RUN_DIR}/mock_replay")"
  mock_overall="$(kv "${mock_summary}" "overall")"
  if [[ "${mock_rc}" == "1" ]]; then
    add_block "mock_replay 执行失败(rc=1)"
    add_row "mock_replay" "FAIL" "${mock_rc}" "BLOCK" "overall=${mock_overall:-N/A}"
  elif [[ "${mock_rc}" == "2" ]]; then
    if [[ "${REQUIRE_MOCK_GREEN}" == "1" ]]; then
      add_block "mock_replay 非 GREEN(overall=${mock_overall:-YELLOW/RED})"
      add_row "mock_replay" "FAIL" "${mock_rc}" "BLOCK" "require_mock_green=1, overall=${mock_overall:-N/A}"
    else
      add_warn "mock_replay 非 GREEN(overall=${mock_overall:-YELLOW/RED})"
      add_row "mock_replay" "WARN" "${mock_rc}" "WARN" "overall=${mock_overall:-N/A}"
    fi
  else
    add_row "mock_replay" "PASS" "${mock_rc}" "-" "overall=${mock_overall:-GREEN}"
  fi
else
  add_row "mock_replay" "SKIP" "${mock_rc}" "-" "skip_mock_replay=1"
fi

refund_rc="-"
refund_result="-"
if [[ ${SKIP_REFUND_CONVERGENCE} -eq 0 ]]; then
  refund_cmd=(
    ./shell/payment_refund_convergence_check.sh
    --window-hours "${REFUND_WINDOW_HOURS}"
    --refund-timeout-minutes "${REFUND_TIMEOUT_MINUTES}"
    --out-dir "${RUN_DIR}/refund_convergence"
    --no-alert
  )
  if [[ -n "${ORDER_NO}" ]]; then
    refund_cmd+=(--order-no "${ORDER_NO}")
  fi
  refund_rc="$(run_step "04_refund_convergence" "${refund_cmd[@]}")"
  refund_summary="$(latest_summary "${RUN_DIR}/refund_convergence")"
  refund_result="$(kv "${refund_summary}" "gate_result")"
  if [[ "${refund_rc}" == "1" ]]; then
    add_block "refund_convergence 执行失败(rc=1)"
    add_row "refund_convergence" "FAIL" "${refund_rc}" "BLOCK" "gate_result=${refund_result:-N/A}"
  elif [[ "${refund_rc}" == "2" ]]; then
    add_block "refund_convergence 阻断(gate_result=${refund_result:-RED})"
    add_row "refund_convergence" "FAIL" "${refund_rc}" "BLOCK" "gate_result=${refund_result:-RED}"
  elif [[ "${refund_result}" != "GREEN" ]]; then
    if [[ "${REQUIRE_REFUND_GREEN}" == "1" ]]; then
      add_block "refund_convergence 非 GREEN(gate_result=${refund_result:-N/A})"
      add_row "refund_convergence" "FAIL" "${refund_rc}" "BLOCK" "require_refund_green=1, gate_result=${refund_result:-N/A}"
    else
      add_warn "refund_convergence 非 GREEN(gate_result=${refund_result:-N/A})"
      add_row "refund_convergence" "WARN" "${refund_rc}" "WARN" "gate_result=${refund_result:-N/A}"
    fi
  else
    add_row "refund_convergence" "PASS" "${refund_rc}" "-" "gate_result=${refund_result:-GREEN}"
  fi
else
  add_row "refund_convergence" "SKIP" "${refund_rc}" "-" "skip_refund_convergence=1"
fi

expected_decision="GO_FOR_ORDER_DRILL"
if [[ -n "${ORDER_NO}" ]]; then
  expected_decision="GO_LAUNCH"
fi

gonogo_cmd=(./shell/payment_go_nogo_decision.sh --date "${REPORT_DATE}" --require-apply-ready "${REQUIRE_APPLY_READY}" --require-booking-repair-pass "${REQUIRE_BOOKING_REPAIR_PASS}" --require-mapping-smoke-green "${REQUIRE_MAPPING_SMOKE_GREEN}" --out-dir "${RUN_DIR}/go_nogo" --no-alert)
if [[ -n "${ORDER_NO}" ]]; then
  gonogo_cmd+=(--order-no "${ORDER_NO}")
fi
gonogo_rc="$(run_step "05_go_nogo" "${gonogo_cmd[@]}")"
gonogo_summary="$(latest_summary "${RUN_DIR}/go_nogo")"
gonogo_decision="$(kv "${gonogo_summary}" "decision")"
gonogo_blocker_count="$(kv "${gonogo_summary}" "blocker_count")"
if [[ "${gonogo_rc}" != "0" ]]; then
  add_block "go_nogo 未通过(rc=${gonogo_rc}, decision=${gonogo_decision:-N/A}, blocker_count=${gonogo_blocker_count:-N/A})"
  add_row "go_nogo" "FAIL" "${gonogo_rc}" "BLOCK" "decision=${gonogo_decision:-N/A}, blocker_count=${gonogo_blocker_count:-N/A}"
else
  if [[ "${gonogo_decision}" != "${expected_decision}" ]]; then
    add_block "go_nogo 决策不符合预期(expected=${expected_decision}, actual=${gonogo_decision:-N/A})"
    add_row "go_nogo" "FAIL" "${gonogo_rc}" "BLOCK" "expected=${expected_decision}, actual=${gonogo_decision:-N/A}"
  else
    add_row "go_nogo" "PASS" "${gonogo_rc}" "-" "decision=${gonogo_decision}"
  fi
fi

ops_status_rc="-"
ops_status_overall="-"
if [[ ${SKIP_OPS_STATUS} -eq 0 ]]; then
  ops_status_cmd=(
    ./shell/payment_ops_status.sh
    --date "${REPORT_DATE}"
    --refresh
    --refresh-require-apply-ready "${REQUIRE_APPLY_READY}"
    --refund-window-hours "${REFUND_WINDOW_HOURS}"
    --refund-timeout-minutes "${REFUND_TIMEOUT_MINUTES}"
    --require-booking-repair-pass "${REQUIRE_BOOKING_REPAIR_PASS}"
    --require-decision-chain-pass 1
    --out-dir "${RUN_DIR}/ops_status"
    --no-alert
  )
  if [[ -n "${ORDER_NO}" ]]; then
    ops_status_cmd+=(--refresh-order-no "${ORDER_NO}")
  fi
  ops_status_rc="$(run_step "06_ops_status" "${ops_status_cmd[@]}")"
  ops_status_summary="$(latest_summary "${RUN_DIR}/ops_status")"
  ops_status_overall="$(kv "${ops_status_summary}" "overall")"
  if [[ "${ops_status_rc}" == "1" ]]; then
    add_block "ops_status 执行失败(rc=1)"
    add_row "ops_status" "FAIL" "${ops_status_rc}" "BLOCK" "overall=${ops_status_overall:-N/A}"
  elif [[ "${ops_status_rc}" == "2" ]]; then
    if [[ "${ops_status_overall}" == "RED" || -z "${ops_status_overall}" ]]; then
      add_block "ops_status 阻断(rc=2, overall=${ops_status_overall:-N/A})"
      add_row "ops_status" "FAIL" "${ops_status_rc}" "BLOCK" "overall=${ops_status_overall:-N/A}"
    else
      add_warn "ops_status 非 GREEN(overall=${ops_status_overall:-YELLOW})"
      add_row "ops_status" "WARN" "${ops_status_rc}" "WARN" "overall=${ops_status_overall:-YELLOW}"
    fi
  else
    add_row "ops_status" "PASS" "${ops_status_rc}" "-" "overall=${ops_status_overall:-GREEN}"
  fi
else
  add_row "ops_status" "SKIP" "${ops_status_rc}" "-" "skip_ops_status=1"
fi

block_count="${#BLOCKERS[@]}"
warn_count="${#WARNS[@]}"
overall="GREEN"
gate_decision="GO"
exit_code=0
if (( block_count > 0 )); then
  overall="RED"
  gate_decision="NO_GO"
  exit_code=2
elif (( warn_count > 0 )); then
  overall="YELLOW"
  gate_decision="GO_WITH_WARN"
fi

block_reason_text="$(join_by_semicolon "${BLOCKERS[@]}")"
warn_reason_text="$(join_by_semicolon "${WARNS[@]}")"
block_hints=()
warn_hints=()
for reason in "${BLOCKERS[@]}"; do
  block_hints+=("$(hint_for_reason "${reason}")")
done
for reason in "${WARNS[@]}"; do
  warn_hints+=("$(hint_for_reason "${reason}")")
done
block_hint_text="$(join_by_semicolon "${block_hints[@]}")"
warn_hint_text="$(join_by_semicolon "${warn_hints[@]}")"

{
  echo "run_id=${RUN_ID}"
  echo "run_time=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "report_date=${REPORT_DATE}"
  echo "order_no=${ORDER_NO}"
  echo "require_apply_ready=${REQUIRE_APPLY_READY}"
  echo "require_booking_repair_pass=${REQUIRE_BOOKING_REPAIR_PASS}"
  echo "require_mapping_smoke_green=${REQUIRE_MAPPING_SMOKE_GREEN}"
  echo "mapping_strict_missing=${MAPPING_STRICT_MISSING}"
  echo "require_mapping_green=${REQUIRE_MAPPING_GREEN}"
  echo "require_mock_green=${REQUIRE_MOCK_GREEN}"
  echo "require_refund_green=${REQUIRE_REFUND_GREEN}"
  echo "allow_shared_submchid=${ALLOW_SHARED_SUBMCHID}"
  echo "refund_window_hours=${REFUND_WINDOW_HOURS}"
  echo "refund_timeout_minutes=${REFUND_TIMEOUT_MINUTES}"
  echo "preflight_strict=${PREFLIGHT_STRICT}"
  echo "skip_preflight=${SKIP_PREFLIGHT}"
  echo "skip_mapping_audit=${SKIP_MAPPING_AUDIT}"
  echo "skip_mock_replay=${SKIP_MOCK_REPLAY}"
  echo "skip_refund_convergence=${SKIP_REFUND_CONVERGENCE}"
  echo "skip_ops_status=${SKIP_OPS_STATUS}"
  echo "preflight_rc=${preflight_rc}"
  echo "preflight_fail_count=${preflight_fail_count}"
  echo "preflight_fail_items=${preflight_fail_items}"
  echo "mapping_rc=${mapping_rc}"
  echo "mapping_overall=${mapping_overall}"
  echo "mock_replay_rc=${mock_rc}"
  echo "mock_replay_overall=${mock_overall}"
  echo "refund_convergence_rc=${refund_rc}"
  echo "refund_convergence_result=${refund_result}"
  echo "go_nogo_rc=${gonogo_rc}"
  echo "go_nogo_decision=${gonogo_decision}"
  echo "go_nogo_expected_decision=${expected_decision}"
  echo "ops_status_rc=${ops_status_rc}"
  echo "ops_status_overall=${ops_status_overall}"
  echo "overall=${overall}"
  echo "gate_decision=${gate_decision}"
  echo "block_count=${block_count}"
  echo "warn_count=${warn_count}"
  echo "block_reasons=${block_reason_text}"
  echo "warn_reasons=${warn_reason_text}"
  echo "block_hints=${block_hint_text}"
  echo "warn_hints=${warn_hint_text}"
  echo "run_dir=${RUN_DIR}"
} > "${SUMMARY_FILE}"

{
  echo "# 支付切换上线拦截规则报告"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`"
  echo "- report_date: \`${REPORT_DATE}\`"
  echo "- order_no: \`${ORDER_NO:-<none>}\`"
  echo "- require_mapping_smoke_green: \`${REQUIRE_MAPPING_SMOKE_GREEN}\`"
  echo "- overall: **${overall}**"
  echo "- gate_decision: **${gate_decision}**"
  echo "- preflight_fail_items: \`${preflight_fail_items:-<none>}\`"
  echo
  echo "## 步骤结果"
  echo
  echo "| step | level | rc | gate | detail |"
  echo "|---|---|---:|---|---|"
  for row in "${ROWS[@]}"; do
    IFS='|' read -r step level rc gate detail <<< "${row}"
    echo "| ${step} | ${level} | ${rc} | ${gate} | ${detail} |"
  done
  echo
  echo "## 风险说明"
  if (( block_count == 0 )); then
    echo "- BLOCK: 无"
  else
    echo "- BLOCK:"
    for item in "${BLOCKERS[@]}"; do
      echo "  - ${item} | 建议: $(hint_for_reason "${item}")"
    done
  fi
  if (( warn_count == 0 )); then
    echo "- WARN: 无"
  else
    echo "- WARN:"
    for item in "${WARNS[@]}"; do
      echo "  - ${item} | 建议: $(hint_for_reason "${item}")"
    done
  fi
  echo
  echo "## 追溯"
  echo "- summary: \`${SUMMARY_FILE}\`"
  echo "- run_dir: \`${RUN_DIR}\`"
} > "${REPORT_FILE}"

echo "[cutover-gate] summary=${SUMMARY_FILE}"
echo "[cutover-gate] report=${REPORT_FILE}"
echo "[cutover-gate] overall=${overall}, gate_decision=${gate_decision}, block_count=${block_count}, warn_count=${warn_count}"

if (( exit_code != 0 )) && [[ ${NO_ALERT} -eq 0 ]] && [[ -x "${ALERT_SCRIPT}" ]]; then
  "${ALERT_SCRIPT}" \
    --title "支付切换上线拦截规则告警" \
    --content "overall=${overall}; gate_decision=${gate_decision}; block_count=${block_count}; warn_count=${warn_count}; report=${REPORT_FILE}" || true
fi

exit "${exit_code}"
