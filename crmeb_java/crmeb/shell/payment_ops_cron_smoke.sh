#!/usr/bin/env bash
set -euo pipefail

# D28: ops_cron 参数编排离线自测
# 目标：验证新增参数是否被正确拼装到托管 cron 命令
# - --status-require-booking-repair-pass
# - --status-require-decision-chain-pass
# - --booking-repair-apply
# - --decision-chain-notify
# - --decision-ticket-notify
# - --cron-health-notify
# - --mapping-audit-notify
# - --mapping-smoke-notify
# - --cutover-gate-notify

OUT_DIR="${OUT_DIR:-}"
BOOKING_REPAIR_WINDOW_HOURS="${BOOKING_REPAIR_WINDOW_HOURS:-72}"
NO_ALERT=0

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_ops_cron_smoke.sh [--out-dir PATH] [--booking-repair-window-hours N] [--no-alert]

参数：
  --out-dir PATH                  输出目录（默认 runtime/payment_ops_cron_smoke）
  --booking-repair-window-hours N 自测中 booking-repair 窗口（默认 72）
  --no-alert                      失败时不推送告警

退出码：
  0  自测通过
  2  自测失败
  1  脚本执行失败
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --out-dir)
      OUT_DIR="$2"
      shift 2
      ;;
    --booking-repair-window-hours)
      BOOKING_REPAIR_WINDOW_HOURS="$2"
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

if ! [[ "${BOOKING_REPAIR_WINDOW_HOURS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "参数错误: --booking-repair-window-hours 必须是正整数"
  exit 1
fi

if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="${ROOT_DIR}/runtime/payment_ops_cron_smoke"
fi
RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"

DRYRUN_FILE="${RUN_DIR}/ops_cron_dryrun.txt"
SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"

set +e
./shell/payment_ops_cron.sh \
  --ticket-notify 1 \
  --status-notify 1 \
  --status-refresh 1 \
  --status-require-booking-repair-pass 1 \
  --status-require-decision-chain-pass 1 \
  --booking-repair-notify 1 \
  --booking-repair-window-hours "${BOOKING_REPAIR_WINDOW_HOURS}" \
  --booking-repair-apply 1 \
  --decision-chain-notify 1 \
  --decision-ticket-notify 1 \
  --cron-health-notify 1 \
  --mapping-audit-notify 1 \
  --mapping-audit-strict-missing 0 \
  --mapping-smoke-notify 1 \
  --cutover-gate-notify 1 \
  --cutover-gate-require-apply-ready 0 \
  --cutover-gate-require-booking-repair-pass 1 \
  --cutover-gate-mapping-strict-missing 0 \
  --cutover-gate-require-mapping-green 0 \
  --cutover-gate-require-mock-green 0 \
  --go-nogo-notify 1 \
  --go-nogo-require-booking-repair-pass 1 \
  --morning-bundle-notify 1 \
  --morning-bundle-require-booking-repair-pass 1 > "${DRYRUN_FILE}" 2>&1
cron_rc=$?
set -e

assertions=()
failures=()

expect_contains() {
  local key="$1"
  local pattern="$2"
  if grep -Fq "${pattern}" "${DRYRUN_FILE}"; then
    assertions+=("${key}|PASS|${pattern}")
  else
    assertions+=("${key}|FAIL|${pattern}")
    failures+=("${key} 缺失: ${pattern}")
  fi
}

if [[ "${cron_rc}" == "0" ]]; then
  assertions+=("ops_cron_rc|PASS|rc=0")
else
  assertions+=("ops_cron_rc|FAIL|rc=${cron_rc}")
  failures+=("payment_ops_cron.sh dry-run 失败(rc=${cron_rc})")
fi

expect_contains "status_gate" "payment_ops_status.sh --require-booking-repair-pass 1"
expect_contains "status_decision_chain_gate" "payment_ops_status.sh --require-booking-repair-pass 1 --require-decision-chain-pass 1"
expect_contains "booking_apply" "payment_booking_verify_repair.sh --window-hours ${BOOKING_REPAIR_WINDOW_HOURS} --apply"
expect_contains "decision_chain_smoke" "payment_decision_chain_smoke.sh"
expect_contains "decision_ticketize" "payment_decision_ticketize.sh"
expect_contains "cron_healthcheck" "payment_cron_healthcheck.sh"
expect_contains "mapping_audit" "payment_store_mapping_audit.sh --strict-missing 0"
expect_contains "mapping_smoke" "payment_store_mapping_pipeline_smoke.sh"
expect_contains "cutover_gate" "payment_cutover_gate.sh --require-apply-ready 0 --require-booking-repair-pass 1 --require-mapping-smoke-green 0 --mapping-strict-missing 0 --require-mapping-green 0 --require-mock-green 0 --owner-default payment-ops --owner-p1 payment-oncall"
expect_contains "ticket_owner_defaults" "payment_reconcile_ticketize.sh --max-rows 200 --amount-p1-threshold-cent 100000 --owner-default payment-ops --owner-p1 payment-oncall"
expect_contains "gonogo_gate" "payment_go_nogo_decision.sh --require-apply-ready 0 --require-booking-repair-pass 1"
expect_contains "gonogo_mapping_smoke_gate" "payment_go_nogo_decision.sh --require-apply-ready 0 --require-booking-repair-pass 1 --require-mapping-smoke-green 0"
expect_contains "morning_gate" "payment_ops_morning_bundle.sh --window 15 --tail 3000 --window-hours 72 --require-apply-ready 0 --require-booking-repair-pass 1"
expect_contains "morning_mapping_smoke_gate" "payment_ops_morning_bundle.sh --window 15 --tail 3000 --window-hours 72 --require-apply-ready 0 --require-booking-repair-pass 1 --require-mapping-smoke-green 0"

fail_count="${#failures[@]}"
exit_code=0
severity="PASS"
if (( fail_count > 0 )); then
  exit_code=2
  severity="ALERT"
fi

fail_text=""
if (( fail_count > 0 )); then
  fail_text="$(printf '%s; ' "${failures[@]}")"
  fail_text="${fail_text%; }"
fi

cat > "${SUMMARY_FILE}" <<TXT
run_id=${RUN_ID}
run_time=$(date '+%Y-%m-%d %H:%M:%S')
severity=${severity}
fail_count=${fail_count}
fail_reasons=${fail_text}
ops_cron_rc=${cron_rc}
dryrun_file=${DRYRUN_FILE}
run_dir=${RUN_DIR}
TXT

{
  echo "# ops_cron 参数编排离线自测"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`"
  echo "- severity: **${severity}**"
  echo "- fail_count: \`${fail_count}\`"
  echo
  echo "## 一、断言结果"
  echo
  echo "| 断言项 | 结果 | 期望片段 |"
  echo "|---|---|---|"
  row=""
  for row in "${assertions[@]}"; do
    IFS='|' read -r key lv detail <<< "${row}"
    echo "| ${key} | ${lv} | \`${detail}\` |"
  done
  echo
  echo "## 二、失败明细"
  echo
  if (( fail_count == 0 )); then
    echo "- 无"
  else
    for item in "${failures[@]}"; do
      echo "- ${item}"
    done
  fi
  echo
  echo "## 三、追溯文件"
  echo
  echo "- dryrun: \`${DRYRUN_FILE}\`"
  echo "- summary: \`${SUMMARY_FILE}\`"
} > "${REPORT_FILE}"

echo "[ops-cron-smoke] dryrun=${DRYRUN_FILE}"
echo "[ops-cron-smoke] summary=${SUMMARY_FILE}"
echo "[ops-cron-smoke] report=${REPORT_FILE}"
echo "[ops-cron-smoke] severity=${severity}, fail_count=${fail_count}"

if (( exit_code != 0 )) && [[ ${NO_ALERT} -eq 0 ]] && [[ -x "${ALERT_SCRIPT}" ]]; then
  "${ALERT_SCRIPT}" \
    --title "ops_cron 参数编排自测告警" \
    --content "severity=${severity}; fail_count=${fail_count}; report=${REPORT_FILE}" || true
fi

exit "${exit_code}"
