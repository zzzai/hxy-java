#!/usr/bin/env bash
set -euo pipefail

# D32: 判定链路同日优先自测（离线）
# 目标：防止 go_nogo / warroom 误读跨日期 latest summary。

REPORT_DATE="${REPORT_DATE:-}"
KEEP_TEMP=0
OUT_DIR="${OUT_DIR:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_decision_chain_smoke.sh [--date YYYY-MM-DD] [--keep-temp] [--out-dir PATH]

参数：
  --date YYYY-MM-DD    业务日期（默认昨天）
  --keep-temp          保留临时目录（默认执行后删除）
  --out-dir PATH       输出目录（默认 runtime/payment_decision_chain_smoke）

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
  OUT_DIR="${ROOT_DIR}/runtime/payment_decision_chain_smoke"
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

TMP_ROOT="${TMP_BASE}/root"
TMP_SHELL="${TMP_ROOT}/shell"
TMP_RUNTIME="${TMP_ROOT}/runtime"
mkdir -p "${TMP_SHELL}" "${TMP_RUNTIME}"

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

touch_summary() {
  local file="$1"
  mkdir -p "$(dirname "${file}")"
  cat > "${file}"
}

copy_script() {
  local name="$1"
  cp "${ROOT_DIR}/shell/${name}" "${TMP_SHELL}/${name}"
  chmod +x "${TMP_SHELL}/${name}"
}

build_stubs() {
  cat > "${TMP_SHELL}/payment_ops_daily.sh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DATE="$(date -d 'yesterday' +%F)"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --date) REPORT_DATE="$2"; shift 2 ;;
    --no-alert) shift ;;
    *) shift ;;
  esac
done
RUN_DIR="${ROOT_DIR}/runtime/payment_ops_daily/run-stub-${REPORT_DATE}"
mkdir -p "${RUN_DIR}"
cat > "${RUN_DIR}/summary.txt" <<TXT
run_id=stub
severity=OK
preflight_rc=0
monitor_rc=0
reconcile_rc=0
reconcile_summary=${ROOT_DIR}/runtime/payment_reconcile/${REPORT_DATE}/summary.txt
TXT
echo "[ops-daily] summary=${RUN_DIR}/summary.txt"
SH
  chmod +x "${TMP_SHELL}/payment_ops_daily.sh"

  cat > "${TMP_SHELL}/payment_idempotency_regression.sh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="${ROOT_DIR}/runtime/payment_idempotency_regression/run-stub"
mkdir -p "${RUN_DIR}"
cat > "${RUN_DIR}/summary.txt" <<TXT
run_id=stub
total_findings=0
critical_count=0
warn_count=0
run_dir=${RUN_DIR}
TXT
REPORT_FILE="${RUN_DIR}/report.md"
cat > "${REPORT_FILE}" <<TXT
# stub
TXT
echo "[idempotency] report=${REPORT_FILE}"
SH
  chmod +x "${TMP_SHELL}/payment_idempotency_regression.sh"

  cat > "${TMP_SHELL}/payment_daily_report.sh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_DATE="$(date -d 'yesterday' +%F)"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --date) REPORT_DATE="$2"; shift 2 ;;
    *) shift ;;
  esac
done
OUT_DIR="${ROOT_DIR}/runtime/payment_daily_report/${REPORT_DATE}"
mkdir -p "${OUT_DIR}"
FILE="${OUT_DIR}/payment_daily_report_${REPORT_DATE}.md"
cat > "${FILE}" <<TXT
# stub
TXT
echo "[daily-report] 生成成功: ${FILE}"
SH
  chmod +x "${TMP_SHELL}/payment_daily_report.sh"
}

build_fixture() {
  local date="$1"
  local future_date="2099-01-01"

  touch_summary "${TMP_RUNTIME}/payment_reconcile/${date}/summary.txt" <<TXT
recon_date=${date}
main_raw_diff_count=0
main_cleared_by_refund_count=0
main_diff_count=0
orphan_wx_count=0
TXT
  touch_summary "${TMP_RUNTIME}/payment_reconcile/${date}/tickets/summary.txt" <<TXT
recon_date=${date}
total_tickets=0
p1_count=0
p2_count=0
escalated_count=0
sla_status=OK
ticket_md=${TMP_RUNTIME}/payment_reconcile/${date}/tickets/tickets.md
escalation_md=${TMP_RUNTIME}/payment_reconcile/${date}/tickets/escalation.md
TXT

  touch_summary "${TMP_RUNTIME}/payment_booking_verify_repair/run-pass/summary.txt" <<TXT
run_id=pass
mode=apply
severity=PASS
before_total=0
after_total=0
run_dir=${TMP_RUNTIME}/payment_booking_verify_repair/run-pass
TXT
  touch_summary "${TMP_RUNTIME}/payment_reconcile_sla/run-ok/summary.txt" <<TXT
run_id=ok
severity=OK
pending_days=0
breach_days=0
TXT
  touch_summary "${TMP_RUNTIME}/payment_cutover_apply/run-ok/summary.txt" <<TXT
run_id=ok
mode=dry-run
ready=1
rolled_back=0
run_dir=${TMP_RUNTIME}/payment_cutover_apply/run-ok
TXT

  touch_summary "${TMP_RUNTIME}/payment_warroom/run-match/summary.txt" <<TXT
run_id=match
report_date=${date}
overall=GREEN
risk_count=0
TXT
  sleep 1
  touch_summary "${TMP_RUNTIME}/payment_warroom/run-newer-mismatch/summary.txt" <<TXT
run_id=mismatch
report_date=${future_date}
overall=RED
risk_count=9
TXT

  touch_summary "${TMP_RUNTIME}/payment_cutover_rehearsal/run-match/summary.txt" <<TXT
run_id=match
recon_date=${date}
ready_for_order_drill=1
preflight_rc=0
ops_daily_rc=0
idempotency_rc=0
TXT
  sleep 1
  touch_summary "${TMP_RUNTIME}/payment_cutover_rehearsal/run-newer-mismatch/summary.txt" <<TXT
run_id=mismatch
recon_date=${future_date}
ready_for_order_drill=0
preflight_rc=0
ops_daily_rc=0
idempotency_rc=0
TXT

  touch_summary "${TMP_RUNTIME}/payment_go_nogo/run-match/summary.txt" <<TXT
run_id=match
report_date=${date}
decision=GO_FOR_ORDER_DRILL
blocker_count=0
gate_order_drill=1
gate_launch=0
TXT
  sleep 1
  touch_summary "${TMP_RUNTIME}/payment_go_nogo/run-newer-mismatch/summary.txt" <<TXT
run_id=mismatch
report_date=${future_date}
decision=NO_GO
blocker_count=2
gate_order_drill=0
gate_launch=0
TXT
}

copy_script "payment_go_nogo_decision.sh"
copy_script "payment_warroom_dashboard.sh"
build_stubs
build_fixture "${REPORT_DATE}"

GO_LOG="${RUN_DIR}/go_nogo.log"
set +e
bash "${TMP_SHELL}/payment_go_nogo_decision.sh" \
  --date "${REPORT_DATE}" \
  --require-booking-repair-pass 1 \
  --no-alert \
  --out-dir "${TMP_RUNTIME}/payment_go_nogo_exec" > "${GO_LOG}" 2>&1
GO_RC=$?
set -e

GO_SUMMARY="$(sed -n 's/^\[go-nogo\] summary=//p' "${GO_LOG}" | tail -n 1 || true)"
GO_DECISION="$(kv "${GO_SUMMARY}" "decision")"
GO_WARROOM="$(kv "${GO_SUMMARY}" "warroom_summary")"
GO_REHEARSAL="$(kv "${GO_SUMMARY}" "rehearsal_summary")"
GO_WARROOM_DATE_MATCH="$(kv "${GO_SUMMARY}" "warroom_date_match")"
GO_REHEARSAL_DATE_MATCH="$(kv "${GO_SUMMARY}" "rehearsal_date_match")"

WAR_LOG="${RUN_DIR}/warroom.log"
set +e
bash "${TMP_SHELL}/payment_warroom_dashboard.sh" \
  --date "${REPORT_DATE}" \
  --window-hours 72 \
  --no-alert \
  --out-dir "${TMP_RUNTIME}/payment_warroom_exec" > "${WAR_LOG}" 2>&1
WAR_RC=$?
set -e

WAR_RUN_DIR="$(sed -n 's/^\[warroom\] run_dir=//p' "${WAR_LOG}" | tail -n 1 || true)"
WAR_SUMMARY="${WAR_RUN_DIR}/summary.txt"
WAR_OVERALL="$(kv "${WAR_SUMMARY}" "overall")"
WAR_GONOGO="$(kv "${WAR_SUMMARY}" "latest_go_nogo_summary")"
WAR_REHEARSAL="$(kv "${WAR_SUMMARY}" "latest_rehearsal_summary")"
WAR_GONOGO_DATE_MATCH="$(kv "${WAR_SUMMARY}" "go_nogo_date_match")"
WAR_REHEARSAL_DATE_MATCH="$(kv "${WAR_SUMMARY}" "rehearsal_date_match")"

severity="PASS"
exit_code=0
fails=()

if [[ "${GO_RC}" != "0" ]]; then
  fails+=("go_nogo rc=${GO_RC}（预期 0）")
fi
if [[ "${GO_DECISION}" != "GO_FOR_ORDER_DRILL" ]]; then
  fails+=("go_nogo decision=${GO_DECISION:-N/A}（预期 GO_FOR_ORDER_DRILL）")
fi
if [[ "${GO_WARROOM}" != *"/run-match/summary.txt" ]]; then
  fails+=("go_nogo 未命中同日 warroom summary: ${GO_WARROOM:-N/A}")
fi
if [[ "${GO_REHEARSAL}" != *"/run-match/summary.txt" ]]; then
  fails+=("go_nogo 未命中同日 rehearsal summary: ${GO_REHEARSAL:-N/A}")
fi
if [[ "${GO_WARROOM_DATE_MATCH}" != "1" ]]; then
  fails+=("go_nogo warroom_date_match=${GO_WARROOM_DATE_MATCH:-N/A}（预期 1）")
fi
if [[ "${GO_REHEARSAL_DATE_MATCH}" != "1" ]]; then
  fails+=("go_nogo rehearsal_date_match=${GO_REHEARSAL_DATE_MATCH:-N/A}（预期 1）")
fi

if [[ "${WAR_GONOGO}" != *"/run-match/summary.txt" ]]; then
  fails+=("warroom 未命中同日 go_nogo summary: ${WAR_GONOGO:-N/A}")
fi
if [[ "${WAR_REHEARSAL}" != *"/run-match/summary.txt" ]]; then
  fails+=("warroom 未命中同日 rehearsal summary: ${WAR_REHEARSAL:-N/A}")
fi
if [[ "${WAR_GONOGO_DATE_MATCH}" != "1" ]]; then
  fails+=("warroom go_nogo_date_match=${WAR_GONOGO_DATE_MATCH:-N/A}（预期 1）")
fi
if [[ "${WAR_REHEARSAL_DATE_MATCH}" != "1" ]]; then
  fails+=("warroom rehearsal_date_match=${WAR_REHEARSAL_DATE_MATCH:-N/A}（预期 1）")
fi

if (( ${#fails[@]} > 0 )); then
  severity="FAIL"
  exit_code=2
fi
fail_count="${#fails[@]}"

fail_text=""
if (( ${#fails[@]} > 0 )); then
  fail_text="$(printf '%s; ' "${fails[@]}")"
  fail_text="${fail_text%; }"
fi

cat > "${SUMMARY_FILE}" <<TXT
run_id=${RUN_ID}
run_time=$(date '+%Y-%m-%d %H:%M:%S')
report_date=${REPORT_DATE}
severity=${severity}
fail_count=${fail_count}
go_nogo_rc=${GO_RC}
go_nogo_decision=${GO_DECISION}
go_nogo_selected_warroom=${GO_WARROOM}
go_nogo_selected_rehearsal=${GO_REHEARSAL}
go_nogo_warroom_date_match=${GO_WARROOM_DATE_MATCH}
go_nogo_rehearsal_date_match=${GO_REHEARSAL_DATE_MATCH}
warroom_rc=${WAR_RC}
warroom_overall=${WAR_OVERALL}
warroom_selected_go_nogo=${WAR_GONOGO}
warroom_selected_rehearsal=${WAR_REHEARSAL}
warroom_go_nogo_date_match=${WAR_GONOGO_DATE_MATCH}
warroom_rehearsal_date_match=${WAR_REHEARSAL_DATE_MATCH}
fail_reasons=${fail_text}
temp_root=${TMP_ROOT}
run_dir=${RUN_DIR}
TXT

cat > "${REPORT_FILE}" <<MD
# 判定链路同日优先自测

- run_id: \`${RUN_ID}\`
- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`
- report_date: \`${REPORT_DATE}\`
- severity: **${severity}**

## 结果

| 检查项 | 实际值 | 预期 |
|---|---|---|
| go_nogo rc | ${GO_RC} | 0 |
| go_nogo decision | ${GO_DECISION:-N/A} | GO_FOR_ORDER_DRILL |
| go_nogo warroom summary | \`${GO_WARROOM:-N/A}\` | 包含 \`/run-match/summary.txt\` |
| go_nogo rehearsal summary | \`${GO_REHEARSAL:-N/A}\` | 包含 \`/run-match/summary.txt\` |
| go_nogo warroom_date_match | ${GO_WARROOM_DATE_MATCH:-N/A} | 1 |
| go_nogo rehearsal_date_match | ${GO_REHEARSAL_DATE_MATCH:-N/A} | 1 |
| warroom rc(仅记录) | ${WAR_RC} | N/A |
| warroom overall(仅记录) | ${WAR_OVERALL:-N/A} | N/A |
| warroom go_nogo summary | \`${WAR_GONOGO:-N/A}\` | 包含 \`/run-match/summary.txt\` |
| warroom rehearsal summary | \`${WAR_REHEARSAL:-N/A}\` | 包含 \`/run-match/summary.txt\` |
| warroom go_nogo_date_match | ${WAR_GONOGO_DATE_MATCH:-N/A} | 1 |
| warroom rehearsal_date_match | ${WAR_REHEARSAL_DATE_MATCH:-N/A} | 1 |

## 日志

- go_nogo log: \`${GO_LOG}\`
- warroom log: \`${WAR_LOG}\`
- summary: \`${SUMMARY_FILE}\`
MD

echo "[decision-chain-smoke] summary=${SUMMARY_FILE}"
echo "[decision-chain-smoke] report=${REPORT_FILE}"
echo "[decision-chain-smoke] severity=${severity}, fail_count=${fail_count}, go_nogo_rc=${GO_RC}, warroom_rc=${WAR_RC}"

exit "${exit_code}"
