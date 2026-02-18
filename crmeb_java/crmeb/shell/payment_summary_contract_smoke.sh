#!/usr/bin/env bash
set -euo pipefail

# D18: summary 协议检查自测脚本（离线）
# 目标：在无真实订单下验证 contract-check 的通过/失败分支都可用。

REPORT_DATE="${REPORT_DATE:-}"
KEEP_TEMP=0
OUT_DIR="${OUT_DIR:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_summary_contract_smoke.sh [--date YYYY-MM-DD] [--keep-temp] [--out-dir PATH]

参数：
  --date YYYY-MM-DD    对账日期（默认昨天）
  --keep-temp          保留临时夹（默认执行后删除）
  --out-dir PATH       输出目录（默认 runtime/payment_contract_smoke）

退出码：
  0  自测通过
  2  自测失败
  1  脚本执行失败
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
  OUT_DIR="${ROOT_DIR}/runtime/payment_contract_smoke"
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

touch_summary() {
  local file="$1"
  mkdir -p "$(dirname "${file}")"
  cat > "${file}"
}

build_fixture() {
  local runtime_root="$1"
  local date="$2"

  touch_summary "${runtime_root}/payment_ops_daily/run-ok/summary.txt" <<TXT
run_id=ok
severity=OK
preflight_rc=0
monitor_rc=0
reconcile_rc=0
reconcile_summary=${runtime_root}/payment_reconcile/${date}/summary.txt
TXT

  touch_summary "${runtime_root}/payment_reconcile/${date}/summary.txt" <<TXT
recon_date=${date}
main_raw_diff_count=0
main_cleared_by_refund_count=0
main_diff_count=0
orphan_wx_count=0
TXT

  touch_summary "${runtime_root}/payment_reconcile/${date}/tickets/summary.txt" <<TXT
recon_date=${date}
total_tickets=0
p1_count=0
p2_count=0
escalated_count=0
sla_status=OK
ticket_md=${runtime_root}/payment_reconcile/${date}/tickets/tickets.md
escalation_md=${runtime_root}/payment_reconcile/${date}/tickets/escalation.md
TXT

  touch_summary "${runtime_root}/payment_idempotency_regression/run-ok/summary.txt" <<'TXT'
run_id=ok
total_findings=0
critical_count=0
warn_count=0
run_dir=/tmp/mock
TXT

  touch_summary "${runtime_root}/payment_cutover_rehearsal/run-ok/summary.txt" <<'TXT'
run_id=ok
ready_for_order_drill=1
preflight_rc=0
ops_daily_rc=0
idempotency_rc=0
TXT

  touch_summary "${runtime_root}/payment_cutover_apply/run-ok/summary.txt" <<'TXT'
run_id=ok
mode=dry-run
ready=1
rolled_back=0
run_dir=/tmp/mock
TXT

  touch_summary "${runtime_root}/payment_go_nogo/run-ok/summary.txt" <<TXT
run_id=ok
report_date=${date}
decision=GO
blocker_count=0
gate_order_drill=PASS
gate_launch=PASS
ticket_summary=/tmp/mock
ticket_gate_ok=1
ticket_p1=0
require_booking_repair_pass=0
booking_repair_gate_ok=1
booking_repair_severity=PASS
warroom_date_match=1
warroom_report_date=${date}
rehearsal_date_match=1
rehearsal_recon_date=${date}
TXT

  touch_summary "${runtime_root}/payment_warroom/run-ok/summary.txt" <<TXT
run_id=ok
overall=GREEN
risk_count=0
latest_go_nogo_summary=/tmp/mock
latest_sla_summary=/tmp/mock
ticketize_summary=/tmp/mock
ticket_p1=0
latest_booking_repair_summary=/tmp/mock
booking_repair_severity=PASS
go_nogo_date_match=1
go_nogo_report_date=${date}
rehearsal_date_match=1
rehearsal_recon_date=${date}
latest_decision_chain_summary=/tmp/mock
decision_chain_severity=PASS
decision_chain_fail_count=0
latest_mapping_audit_summary=/tmp/mock
mapping_overall=GREEN
mapping_critical_count=0
mapping_warn_count=0
latest_cutover_gate_summary=/tmp/mock
cutover_gate_decision=GO
cutover_gate_overall=GREEN
cutover_gate_block_count=0
cutover_gate_warn_count=0
cutover_gate_report_date=${date}
cron_decision_chain_enabled=1
TXT

  touch_summary "${runtime_root}/payment_ops_status/run-ok/summary.txt" <<'TXT'
run_id=ok
overall=GREEN
warn_count=0
block_count=0
mapping_smoke_summary=/tmp/mock
mapping_smoke_summary_age_minutes=3
mapping_smoke_overall=GREEN
TXT

  touch_summary "${runtime_root}/payment_ops_morning_bundle/run-ok/summary.txt" <<'TXT'
run_id=ok
severity=OK
gonogo_decision=GO
warroom_overall=GREEN
sla_rc=0
sla_severity=OK
booking_verify_rc=0
booking_repair_rc=0
require_booking_repair_pass=0
TXT

  touch_summary "${runtime_root}/payment_booking_verify_regression/run-ok/summary.txt" <<'TXT'
run_id=ok
severity=PASS
critical_count=0
warn_count=0
run_dir=/tmp/mock
TXT

  touch_summary "${runtime_root}/payment_booking_verify_repair/run-ok/summary.txt" <<'TXT'
run_id=ok
mode=dry-run
severity=PASS
before_total=0
after_total=0
run_dir=/tmp/mock
TXT

  touch_summary "${runtime_root}/payment_decision_chain_smoke/run-ok/summary.txt" <<'TXT'
run_id=ok
severity=PASS
fail_count=0
go_nogo_rc=0
warroom_rc=2
go_nogo_warroom_date_match=1
go_nogo_rehearsal_date_match=1
warroom_go_nogo_date_match=1
warroom_rehearsal_date_match=1
run_dir=/tmp/mock
TXT

  touch_summary "${runtime_root}/payment_reconcile_sla/run-ok/summary.txt" <<'TXT'
run_id=ok
severity=OK
issue_days=0
pending_days=0
breach_days=0
detail_file=/tmp/mock
TXT

  touch_summary "${runtime_root}/payment_store_mapping/import-ok/summary.txt" <<'TXT'
csv=/tmp/mock.csv
mode=dry-run
apply_status=NOT_APPLIED
total_store_count=3
conflict_count=0
run_dir=/tmp/mock
TXT

  touch_summary "${runtime_root}/payment_store_mapping/template-ok/summary.txt" <<'TXT'
total_store_count=3
template_file=/tmp/mock.csv
reference_file=/tmp/mock.tsv
run_dir=/tmp/mock
TXT

  touch_summary "${runtime_root}/payment_store_mapping/audit-ok/summary.txt" <<'TXT'
run_id=ok
overall=GREEN
critical_count=0
warn_count=0
missing_count=0
invalid_count=0
duplicate_store_count=0
orphan_non_empty_count=0
placeholder_count=0
placeholder_file=/tmp/mock_placeholder.tsv
run_dir=/tmp/mock
TXT

  touch_summary "${runtime_root}/payment_cutover_gate/run-ok/summary.txt" <<TXT
run_id=ok
report_date=${date}
overall=GREEN
gate_decision=GO
block_count=0
warn_count=0
preflight_rc=0
preflight_fail_items=
mapping_overall=GREEN
mock_replay_overall=GREEN
go_nogo_decision=GO_FOR_ORDER_DRILL
ops_status_overall=GREEN
run_dir=/tmp/mock
TXT
}

run_contract_check() {
  local runtime_root="$1"
  local out_dir="$2"
  local log_file="$3"
  set +e
  ./shell/payment_summary_contract_check.sh \
    --date "${REPORT_DATE}" \
    --runtime-root "${runtime_root}" \
    --require-all \
    --no-alert \
    --out-dir "${out_dir}" > "${log_file}" 2>&1
  local rc=$?
  set -e
  printf '%s' "${rc}"
}

RUNTIME_OK="${TMP_BASE}/runtime_ok"
build_fixture "${RUNTIME_OK}" "${REPORT_DATE}"
PASS_LOG="${RUN_DIR}/case_pass.log"
PASS_OUT="${RUN_DIR}/case_pass"
PASS_RC="$(run_contract_check "${RUNTIME_OK}" "${PASS_OUT}" "${PASS_LOG}")"

RUNTIME_BAD="${TMP_BASE}/runtime_bad"
cp -a "${RUNTIME_OK}" "${RUNTIME_BAD}"
sed -i '/^sla_rc=/d' "${RUNTIME_BAD}/payment_ops_morning_bundle/run-ok/summary.txt"
FAIL_LOG="${RUN_DIR}/case_fail.log"
FAIL_OUT="${RUN_DIR}/case_fail"
FAIL_RC="$(run_contract_check "${RUNTIME_BAD}" "${FAIL_OUT}" "${FAIL_LOG}")"

severity="PASS"
exit_code=0
if [[ "${PASS_RC}" != "0" || "${FAIL_RC}" != "2" ]]; then
  severity="FAIL"
  exit_code=2
fi

cat > "${SUMMARY_FILE}" <<TXT
run_id=${RUN_ID}
run_time=$(date '+%Y-%m-%d %H:%M:%S')
report_date=${REPORT_DATE}
severity=${severity}
pass_case_rc=${PASS_RC}
fail_case_rc=${FAIL_RC}
temp_root=${TMP_BASE}
run_dir=${RUN_DIR}
TXT

cat > "${REPORT_FILE}" <<MD
# 支付 summary 协议自测

- run_id: \`${RUN_ID}\`
- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`
- report_date: \`${REPORT_DATE}\`
- severity: **${severity}**

## 用例结果

| 用例 | 预期rc | 实际rc | 说明 |
|---|---:|---:|---|
| pass_case | 0 | ${PASS_RC} | 完整字段应通过 |
| fail_case | 2 | ${FAIL_RC} | 删除 \`morning_bundle.sla_rc\` 应失败 |

## 追溯文件

- summary: \`${SUMMARY_FILE}\`
- pass_log: \`${PASS_LOG}\`
- fail_log: \`${FAIL_LOG}\`
- pass_out: \`${PASS_OUT}\`
- fail_out: \`${FAIL_OUT}\`
- temp_root: \`${TMP_BASE}\`
MD

echo "[contract-smoke] summary=${SUMMARY_FILE}"
echo "[contract-smoke] report=${REPORT_FILE}"
echo "[contract-smoke] severity=${severity}, pass_case_rc=${PASS_RC}, fail_case_rc=${FAIL_RC}"

if [[ ${KEEP_TEMP} -eq 1 ]]; then
  echo "[contract-smoke] keep_temp=1, temp_root=${TMP_BASE}"
fi

exit "${exit_code}"
