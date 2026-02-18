#!/usr/bin/env bash
set -euo pipefail

# D20: ops_status 离线自测（含新鲜度守卫）
# 目标：验证值守总览在“正常/过期”场景的返回码和判定行为。

REPORT_DATE="${REPORT_DATE:-}"
KEEP_TEMP=0
OUT_DIR="${OUT_DIR:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_ops_status_smoke.sh [--date YYYY-MM-DD] [--keep-temp] [--out-dir PATH]

参数：
  --date YYYY-MM-DD    对账日期（默认昨天）
  --keep-temp          保留临时目录（默认执行后删除）
  --out-dir PATH       输出目录（默认 runtime/payment_ops_status_smoke）

退出码：
  0  自测通过
  2  自测失败
  1  脚本执行错误
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --date)
      REPORT_DATE="$2"
      shift 2
      ;;
    --keep-temp)
      KEEP_TEMP=1
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
  OUT_DIR="${ROOT_DIR}/runtime/payment_ops_status_smoke"
fi
RUN_ID="$(date '+%Y%m%d%H%M%S')-$$"
RUN_DIR="${OUT_DIR}/run-${RUN_ID}"
mkdir -p "${RUN_DIR}"
SUMMARY_FILE="${RUN_DIR}/summary.txt"
REPORT_FILE="${RUN_DIR}/report.md"

TMP_BASE="$(mktemp -d)"
if [[ ${KEEP_TEMP} -eq 0 ]]; then
  trap 'rm -rf "${TMP_BASE}"' EXIT
fi

write_file() {
  local file="$1"
  mkdir -p "$(dirname "${file}")"
  cat > "${file}"
}

build_runtime_fixture() {
  local runtime_root="$1"
  local date="$2"

  write_file "${runtime_root}/payment_ops_daily/run-ok/summary.txt" <<TXT
run_id=ok
recon_date=${date}
severity=OK
preflight_rc=0
monitor_rc=0
reconcile_rc=0
reconcile_summary=${runtime_root}/payment_reconcile/${date}/summary.txt
TXT

  write_file "${runtime_root}/payment_ops_morning_bundle/run-ok/summary.txt" <<TXT
run_id=ok
report_date=${date}
severity=OK
gonogo_decision=GO
warroom_overall=GREEN
TXT

  write_file "${runtime_root}/payment_warroom/run-ok/summary.txt" <<TXT
run_id=ok
report_date=${date}
overall=GREEN
risk_count=0
go_nogo_report_date=${date}
go_nogo_date_match=1
rehearsal_recon_date=${date}
rehearsal_date_match=1
TXT

  write_file "${runtime_root}/payment_go_nogo/run-ok/summary.txt" <<TXT
run_id=ok
report_date=${date}
decision=GO
blocker_count=0
gate_order_drill=PASS
gate_launch=PASS
warroom_report_date=${date}
warroom_date_match=1
rehearsal_recon_date=${date}
rehearsal_date_match=1
TXT

  write_file "${runtime_root}/payment_cutover_rehearsal/run-ok/summary.txt" <<TXT
run_id=ok
recon_date=${date}
ready_for_order_drill=1
TXT

  write_file "${runtime_root}/payment_cutover_apply/run-ok/summary.txt" <<'TXT'
run_id=ok
mode=dry-run
ready=1
rolled_back=0
TXT

  write_file "${runtime_root}/payment_idempotency_regression/run-ok/summary.txt" <<'TXT'
run_id=ok
total_findings=0
critical_count=0
warn_count=0
TXT

  write_file "${runtime_root}/payment_reconcile_sla/run-ok/summary.txt" <<'TXT'
run_id=ok
severity=OK
issue_days=0
pending_days=0
breach_days=0
sla_days=1
lookback_days=14
TXT

  write_file "${runtime_root}/payment_booking_verify_regression/run-ok/summary.txt" <<'TXT'
run_id=ok
severity=PASS
critical_count=0
warn_count=0
run_dir=/tmp/fake
TXT

  write_file "${runtime_root}/payment_booking_verify_repair/run-ok/summary.txt" <<'TXT'
run_id=ok
mode=dry-run
severity=PASS
before_total=0
after_total=0
run_dir=/tmp/fake
TXT

  write_file "${runtime_root}/payment_decision_chain_smoke/run-ok/summary.txt" <<TXT
run_id=ok
report_date=${date}
severity=PASS
fail_count=0
go_nogo_rc=0
warroom_rc=2
go_nogo_warroom_date_match=1
go_nogo_rehearsal_date_match=1
warroom_go_nogo_date_match=1
warroom_rehearsal_date_match=1
run_dir=/tmp/fake
TXT

  write_file "${runtime_root}/payment_store_mapping_smoke/run-ok/summary.txt" <<'TXT'
run_id=ok
overall=GREEN
critical_count=0
warn_count=0
run_dir=/tmp/fake
TXT

  write_file "${runtime_root}/payment_exception_acceptance/run-ok/summary.txt" <<TXT
run_id=ok
report_date=${date}
overall=GREEN
block_check_count=0
warn_check_count=0
run_dir=/tmp/fake
TXT

  write_file "${runtime_root}/payment_refund_convergence/run-ok/summary.txt" <<'TXT'
run_id=ok
gate_result=GREEN
block_check_count=0
warn_check_count=0
run_dir=/tmp/fake
TXT

  write_file "${runtime_root}/payment_reconcile/${date}/summary.txt" <<TXT
recon_date=${date}
main_raw_diff_count=0
main_cleared_by_refund_count=0
main_diff_count=0
orphan_wx_count=0
TXT

  write_file "${runtime_root}/payment_reconcile/${date}/tickets/summary.txt" <<TXT
recon_date=${date}
total_tickets=0
p1_count=0
p2_count=0
escalated_count=0
sla_status=OK
ticket_md=${runtime_root}/payment_reconcile/${date}/tickets/tickets.md
TXT

  mkdir -p "${runtime_root}/payment_daily_report/${date}"
  cat > "${runtime_root}/payment_daily_report/${date}/payment_daily_report_${date}.md" <<TXT
# fake report ${date}
TXT
}

run_status() {
  local runtime_root="$1"
  local out_dir="$2"
  local log_file="$3"
  local max_minutes="$4"
  local max_recon_days="$5"
  local max_report_days="$6"
  local require_decision_chain_pass="$7"

  set +e
  ./shell/payment_ops_status.sh \
    --date "${REPORT_DATE}" \
    --runtime-root "${runtime_root}" \
    --require-decision-chain-pass "${require_decision_chain_pass}" \
    --max-summary-age-minutes "${max_minutes}" \
    --max-recon-age-days "${max_recon_days}" \
    --max-daily-report-age-days "${max_report_days}" \
    --no-alert \
    --out-dir "${out_dir}" > "${log_file}" 2>&1
  local rc=$?
  set -e
  printf '%s' "${rc}"
}

RUNTIME_OK="${TMP_BASE}/runtime_ok"
build_runtime_fixture "${RUNTIME_OK}" "${REPORT_DATE}"
PASS_LOG="${RUN_DIR}/case_pass.log"
PASS_OUT="${RUN_DIR}/case_pass"
PASS_RC="$(run_status "${RUNTIME_OK}" "${PASS_OUT}" "${PASS_LOG}" 240 2 2 0)"

RUNTIME_STALE="${TMP_BASE}/runtime_stale"
cp -a "${RUNTIME_OK}" "${RUNTIME_STALE}"
touch -d '5 days ago' "${RUNTIME_STALE}/payment_ops_daily/run-ok/summary.txt"
touch -d '5 days ago' "${RUNTIME_STALE}/payment_reconcile/${REPORT_DATE}/summary.txt"
touch -d '5 days ago' "${RUNTIME_STALE}/payment_daily_report/${REPORT_DATE}/payment_daily_report_${REPORT_DATE}.md"
STALE_LOG="${RUN_DIR}/case_stale.log"
STALE_OUT="${RUN_DIR}/case_stale"
STALE_RC="$(run_status "${RUNTIME_STALE}" "${STALE_OUT}" "${STALE_LOG}" 60 1 1 0)"

RUNTIME_DECISION_MISSING="${TMP_BASE}/runtime_decision_missing"
cp -a "${RUNTIME_OK}" "${RUNTIME_DECISION_MISSING}"
rm -rf "${RUNTIME_DECISION_MISSING}/payment_decision_chain_smoke"
DECISION_GATE_LOG="${RUN_DIR}/case_decision_gate.log"
DECISION_GATE_OUT="${RUN_DIR}/case_decision_gate"
DECISION_GATE_RC="$(run_status "${RUNTIME_DECISION_MISSING}" "${DECISION_GATE_OUT}" "${DECISION_GATE_LOG}" 240 2 2 1)"

severity="PASS"
exit_code=0
if [[ "${PASS_RC}" != "0" || "${STALE_RC}" != "2" || "${DECISION_GATE_RC}" != "2" ]]; then
  severity="FAIL"
  exit_code=2
fi

cat > "${SUMMARY_FILE}" <<TXT
run_id=${RUN_ID}
run_time=$(date '+%Y-%m-%d %H:%M:%S')
report_date=${REPORT_DATE}
severity=${severity}
pass_case_rc=${PASS_RC}
stale_case_rc=${STALE_RC}
decision_gate_case_rc=${DECISION_GATE_RC}
temp_root=${TMP_BASE}
run_dir=${RUN_DIR}
TXT

cat > "${REPORT_FILE}" <<MD
# 支付值守总览自测（新鲜度守卫）

- run_id: \`${RUN_ID}\`
- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`
- report_date: \`${REPORT_DATE}\`
- severity: **${severity}**

## 用例结果

| 用例 | 预期rc | 实际rc | 说明 |
|---|---:|---:|---|
| pass_case | 0 | ${PASS_RC} | 完整且新鲜数据应返回 GREEN |
| stale_case | 2 | ${STALE_RC} | 过期 summary/report 应返回 YELLOW |
| decision_gate_case | 2 | ${DECISION_GATE_RC} | 开启 \`require-decision-chain-pass=1\` 且缺失 decision-chain 应返回 RED |

## 追溯文件

- summary: \`${SUMMARY_FILE}\`
- pass_log: \`${PASS_LOG}\`
- stale_log: \`${STALE_LOG}\`
- decision_gate_log: \`${DECISION_GATE_LOG}\`
- pass_out: \`${PASS_OUT}\`
- stale_out: \`${STALE_OUT}\`
- decision_gate_out: \`${DECISION_GATE_OUT}\`
- temp_root: \`${TMP_BASE}\`
MD

echo "[ops-status-smoke] summary=${SUMMARY_FILE}"
echo "[ops-status-smoke] report=${REPORT_FILE}"
echo "[ops-status-smoke] severity=${severity}, pass_case_rc=${PASS_RC}, stale_case_rc=${STALE_RC}, decision_gate_case_rc=${DECISION_GATE_RC}"
if [[ ${KEEP_TEMP} -eq 1 ]]; then
  echo "[ops-status-smoke] keep_temp=1, temp_root=${TMP_BASE}"
fi

exit "${exit_code}"
