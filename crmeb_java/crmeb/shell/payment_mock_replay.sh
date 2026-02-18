#!/usr/bin/env bash
set -euo pipefail

# D43: 无真实订单阶段 mock 回放编排
# 目标：离线持续回归值守链路，不依赖真实支付订单号。

REPORT_DATE="${REPORT_DATE:-}"
OUT_DIR="${OUT_DIR:-}"
NO_ALERT=0
OWNER_MAP_FILE="${OWNER_MAP_FILE:-}"
OWNER_DEFAULT="${OWNER_DEFAULT:-payment-ops}"
OWNER_P1="${OWNER_P1:-payment-oncall}"
SKIP_STORE_MAPPING_AUDIT=0

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_mock_replay.sh [--date YYYY-MM-DD] [--out-dir PATH] [--owner-map-file PATH] [--owner-default NAME] [--owner-p1 NAME] [--skip-store-mapping-audit] [--no-alert]

参数：
  --date YYYY-MM-DD      业务日期（默认昨天）
  --out-dir PATH         输出目录（默认 runtime/payment_mock_replay）
  --owner-map-file PATH  工单 owner 规则文件（可选）
  --owner-default NAME   默认 owner（默认 payment-ops）
  --owner-p1 NAME        P1 默认 owner（默认 payment-oncall）
  --skip-store-mapping-audit  跳过 store_mapping_audit 步骤
  --no-alert             非GREEN时不推送机器人

退出码：
  0  回放通过（GREEN）
  2  回放存在风险（YELLOW/RED）
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --date)
      REPORT_DATE="$2"
      shift 2
      ;;
    --out-dir)
      OUT_DIR="$2"
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
    --skip-store-mapping-audit)
      SKIP_STORE_MAPPING_AUDIT=1
      shift
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

if [[ -z "${REPORT_DATE}" ]]; then
  REPORT_DATE="$(date -d 'yesterday' +%F)"
fi
if ! [[ "${REPORT_DATE}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "参数错误: --date 需要 YYYY-MM-DD"
  exit 1
fi
if [[ -z "${OWNER_DEFAULT}" || -z "${OWNER_P1}" ]]; then
  echo "参数错误: owner 参数不能为空"
  exit 1
fi
if [[ -n "${OWNER_MAP_FILE}" && ! -f "${OWNER_MAP_FILE}" ]]; then
  echo "参数错误: --owner-map-file 不存在 -> ${OWNER_MAP_FILE}"
  exit 1
fi
if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${ROOT_DIR}/runtime/payment_mock_replay"
fi

RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"
SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"

declare -a ROWS=()
declare -a FAILS=()
declare -a WARNS=()

run_step() {
  local step="$1"
  local expected="$2"
  shift 2
  local log_file="${RUN_DIR}/${step}.log"
  local expected_disp="${expected//|/_or_}"

  set +e
  "$@" > "${log_file}" 2>&1
  local rc=$?
  set -e

  local level="PASS"
  local reason="-"
  if [[ "${expected}" == "0" ]]; then
    if [[ "${rc}" != "0" ]]; then
      level="FAIL"
      reason="expected=0, actual=${rc}"
      FAILS+=("${step}:${reason}")
    fi
  elif [[ "${expected}" == "0|2" ]]; then
    if [[ "${rc}" == "2" ]]; then
      # 无真实订单阶段，部分步骤返回 2（YELLOW）属于可接受态，按通过处理避免误报。
      if [[ "${step}" == "ops_status_smoke" || "${step}" == "decision_ticketize" ]]; then
        level="PASS"
        reason="expected=0_or_2, actual=2(accepted)"
      else
        level="WARN"
        reason="expected=0_or_2, actual=2"
        WARNS+=("${step}:${reason}")
      fi
    elif [[ "${rc}" != "0" ]]; then
      level="FAIL"
      reason="expected=0_or_2, actual=${rc}"
      FAILS+=("${step}:${reason}")
    fi
  else
    if [[ "${rc}" != "${expected}" ]]; then
      level="FAIL"
      reason="expected=${expected}, actual=${rc}"
      FAILS+=("${step}:${reason}")
    fi
  fi

  ROWS+=("${step}|${level}|${expected_disp}|${rc}|${reason}|${log_file}")
}

run_step "ops_status_smoke" "0|2" \
  ./shell/payment_ops_status_smoke.sh --date "${REPORT_DATE}" --out-dir "${RUN_DIR}/ops_status_smoke"
run_step "summary_contract_smoke" "0" \
  ./shell/payment_summary_contract_smoke.sh --date "${REPORT_DATE}" --out-dir "${RUN_DIR}/contract_smoke"
run_step "decision_chain_smoke" "0" \
  ./shell/payment_decision_chain_smoke.sh --date "${REPORT_DATE}" --out-dir "${RUN_DIR}/decision_chain_smoke"
run_step "ops_cron_smoke" "0" \
  ./shell/payment_ops_cron_smoke.sh --out-dir "${RUN_DIR}/ops_cron_smoke" --no-alert

decision_ticket_cmd=(./shell/payment_decision_ticketize.sh --date "${REPORT_DATE}" --output-dir "${RUN_DIR}/decision_ticketize" --owner-default "${OWNER_DEFAULT}" --owner-p1 "${OWNER_P1}" --no-alert)
if [[ -n "${OWNER_MAP_FILE}" ]]; then
  decision_ticket_cmd+=(--owner-map-file "${OWNER_MAP_FILE}")
fi
run_step "decision_ticketize" "0|2" "${decision_ticket_cmd[@]}"

run_step "cron_healthcheck" "0|2" \
  ./shell/payment_cron_healthcheck.sh --out-dir "${RUN_DIR}/cron_healthcheck" --no-alert

if [[ ${SKIP_STORE_MAPPING_AUDIT} -eq 1 ]]; then
  ROWS+=("store_mapping_audit|SKIP|-|-|skip_store_mapping_audit=1|-")
else
  allow_shared_submchid="${ALLOW_SHARED_SUBMCHID:-0}"
  run_step "store_mapping_audit" "0|2" \
    ./shell/payment_store_mapping_audit.sh --out-dir "${RUN_DIR}/store_mapping_audit" --strict-missing 0 --allow-shared-submchid "${allow_shared_submchid}" --no-alert
fi

fail_count="${#FAILS[@]}"
warn_count="${#WARNS[@]}"
overall="GREEN"
exit_code=0
if (( fail_count > 0 )); then
  overall="RED"
  exit_code=2
elif (( warn_count > 0 )); then
  overall="YELLOW"
  exit_code=2
fi

{
  echo "run_id=${RUN_ID}"
  echo "run_time=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "report_date=${REPORT_DATE}"
  echo "overall=${overall}"
  echo "fail_count=${fail_count}"
  echo "warn_count=${warn_count}"
  echo "owner_default=${OWNER_DEFAULT}"
  echo "owner_p1=${OWNER_P1}"
  echo "owner_map_file=${OWNER_MAP_FILE}"
  echo "skip_store_mapping_audit=${SKIP_STORE_MAPPING_AUDIT}"
  echo "run_dir=${RUN_DIR}"
} > "${SUMMARY_FILE}"

{
  echo "# 支付 mock 回放报告"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`"
  echo "- report_date: \`${REPORT_DATE}\`"
  echo "- overall: **${overall}**"
  echo
  echo "## 步骤结果"
  echo
  echo "| step | level | expected_rc | actual_rc | reason | log |"
  echo "|---|---|---|---|---|---|"
  for row in "${ROWS[@]}"; do
    IFS='|' read -r step level expected actual reason log_file <<< "${row}"
    echo "| ${step} | ${level} | ${expected} | ${actual} | ${reason} | \`${log_file}\` |"
  done
  echo
  echo "## 汇总"
  echo
  echo "- fail_count: ${fail_count}"
  echo "- warn_count: ${warn_count}"
  if (( fail_count > 0 )); then
    echo "- FAIL:"
    for item in "${FAILS[@]}"; do
      echo "  - ${item}"
    done
  fi
  if (( warn_count > 0 )); then
    echo "- WARN:"
    for item in "${WARNS[@]}"; do
      echo "  - ${item}"
    done
  fi
  echo
  echo "## 追溯"
  echo
  echo "- summary: \`${SUMMARY_FILE}\`"
  echo "- run_dir: \`${RUN_DIR}\`"
} > "${REPORT_FILE}"

echo "[mock-replay] summary=${SUMMARY_FILE}"
echo "[mock-replay] report=${REPORT_FILE}"
echo "[mock-replay] overall=${overall}, fail_count=${fail_count}, warn_count=${warn_count}"

if (( exit_code != 0 )) && [[ ${NO_ALERT} -eq 0 ]] && [[ -x "${ALERT_SCRIPT}" ]]; then
  "${ALERT_SCRIPT}" \
    --title "支付 mock 回放告警" \
    --content "overall=${overall}; fail=${fail_count}; warn=${warn_count}; report=${REPORT_FILE}" || true
fi

exit "${exit_code}"
