#!/usr/bin/env bash
set -euo pipefail

# D45: cutover_gate 离线自测
# 目标：在无真实订单/无真实数据库情况下，验证 cutover_gate 的 GO/NO_GO 判定链路。

REPORT_DATE="${REPORT_DATE:-}"
KEEP_TEMP=0
OUT_DIR="${OUT_DIR:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<'USAGE'
用法：
  ./shell/payment_cutover_gate_smoke.sh [--date YYYY-MM-DD] [--keep-temp] [--out-dir PATH]

参数：
  --date YYYY-MM-DD    业务日期（默认昨天）
  --keep-temp          保留临时目录（默认执行后删除）
  --out-dir PATH       输出目录（默认 runtime/payment_cutover_gate_smoke）

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
  OUT_DIR="${ROOT_DIR}/runtime/payment_cutover_gate_smoke"
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
mkdir -p "${TMP_SHELL}"

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

copy_script() {
  local name="$1"
  cp "${ROOT_DIR}/shell/${name}" "${TMP_SHELL}/${name}"
  chmod +x "${TMP_SHELL}/${name}"
}

build_stubs() {
  cat > "${TMP_SHELL}/payment_alert_notify.sh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
exit 0
SH
  chmod +x "${TMP_SHELL}/payment_alert_notify.sh"

  cat > "${TMP_SHELL}/payment_incident_bundle.sh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/runtime/incident_stub"
REPORT_DATE="$(date -d 'yesterday' +%F)"
ORDER_NO=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --out-dir) OUT_DIR="$2"; shift 2 ;;
    --date) REPORT_DATE="$2"; shift 2 ;;
    --order-no) ORDER_NO="$2"; shift 2 ;;
    *) shift ;;
  esac
done
RUN_DIR="${OUT_DIR}/run-stub-$(date '+%Y%m%d%H%M%S')"
mkdir -p "${RUN_DIR}"
cat > "${RUN_DIR}/summary.txt" <<TXT
report_date=${REPORT_DATE}
order_no=${ORDER_NO}
incident_level=P1
run_dir=${RUN_DIR}
TXT
cat > "${RUN_DIR}/report.md" <<MD
# incident stub
- report_date: ${REPORT_DATE}
- order_no: ${ORDER_NO}
MD
echo "[payment-incident-bundle] run_dir=${RUN_DIR}"
echo "[payment-incident-bundle] summary=${RUN_DIR}/summary.txt"
echo "[payment-incident-bundle] report=${RUN_DIR}/report.md"
exit 0
SH
  chmod +x "${TMP_SHELL}/payment_incident_bundle.sh"

  cat > "${TMP_SHELL}/payment_preflight_check.sh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/runtime/preflight_stub"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --out-dir) OUT_DIR="$2"; shift 2 ;;
    *) shift ;;
  esac
done
mkdir -p "${OUT_DIR}"
REPORT="${OUT_DIR}/preflight-$(date '+%Y%m%d%H%M%S').md"
cat > "${REPORT}" <<'MD'
# preflight stub
- [PASS] shell 存在
MD
echo "[preflight] report=${REPORT}"
exit "${PREFLIGHT_STUB_RC:-0}"
SH
  chmod +x "${TMP_SHELL}/payment_preflight_check.sh"

  cat > "${TMP_SHELL}/payment_store_mapping_audit.sh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/runtime/mapping_stub"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --out-dir) OUT_DIR="$2"; shift 2 ;;
    *) shift ;;
  esac
done
RUN_DIR="${OUT_DIR}/run-stub-$(date '+%Y%m%d%H%M%S')"
mkdir -p "${RUN_DIR}"
SUMMARY="${RUN_DIR}/summary.txt"
cat > "${SUMMARY}" <<TXT
run_id=stub
overall=${MAPPING_STUB_OVERALL:-GREEN}
critical_count=0
warn_count=0
run_dir=${RUN_DIR}
TXT
echo "[mapping-audit] summary=${SUMMARY}"
exit "${MAPPING_STUB_RC:-0}"
SH
  chmod +x "${TMP_SHELL}/payment_store_mapping_audit.sh"

  cat > "${TMP_SHELL}/payment_mock_replay.sh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/runtime/mock_stub"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --out-dir) OUT_DIR="$2"; shift 2 ;;
    *) shift ;;
  esac
done
RUN_DIR="${OUT_DIR}/run-stub-$(date '+%Y%m%d%H%M%S')"
mkdir -p "${RUN_DIR}"
SUMMARY="${RUN_DIR}/summary.txt"
cat > "${SUMMARY}" <<TXT
run_id=stub
overall=${MOCK_STUB_OVERALL:-GREEN}
fail_count=0
warn_count=0
run_dir=${RUN_DIR}
TXT
echo "[mock-replay] summary=${SUMMARY}"
exit "${MOCK_STUB_RC:-0}"
SH
  chmod +x "${TMP_SHELL}/payment_mock_replay.sh"

  cat > "${TMP_SHELL}/payment_refund_convergence_check.sh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/runtime/refund_stub"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --out-dir) OUT_DIR="$2"; shift 2 ;;
    *) shift ;;
  esac
done
RUN_DIR="${OUT_DIR}/run-stub-$(date '+%Y%m%d%H%M%S')"
mkdir -p "${RUN_DIR}"
SUMMARY="${RUN_DIR}/summary.txt"
cat > "${SUMMARY}" <<TXT
run_id=stub
gate_result=${REFUND_STUB_RESULT:-GREEN}
block_check_count=${REFUND_STUB_BLOCK_COUNT:-0}
warn_check_count=${REFUND_STUB_WARN_COUNT:-0}
run_dir=${RUN_DIR}
TXT
echo "[refund-convergence] summary=${SUMMARY}"
exit "${REFUND_STUB_RC:-0}"
SH
  chmod +x "${TMP_SHELL}/payment_refund_convergence_check.sh"

  cat > "${TMP_SHELL}/payment_go_nogo_decision.sh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/runtime/go_nogo_stub"
ORDER_NO=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --out-dir) OUT_DIR="$2"; shift 2 ;;
    --order-no) ORDER_NO="$2"; shift 2 ;;
    *) shift ;;
  esac
done
RUN_DIR="${OUT_DIR}/run-stub-$(date '+%Y%m%d%H%M%S')"
mkdir -p "${RUN_DIR}"
SUMMARY="${RUN_DIR}/summary.txt"
decision="${GONOGO_STUB_DECISION:-}"
if [[ -z "${decision}" ]]; then
  if [[ -n "${ORDER_NO}" ]]; then
    decision="GO_LAUNCH"
  else
    decision="GO_FOR_ORDER_DRILL"
  fi
fi
cat > "${SUMMARY}" <<TXT
run_id=stub
decision=${decision}
blocker_count=${GONOGO_STUB_BLOCKER_COUNT:-0}
report_date=$(date -d 'yesterday' +%F)
run_dir=${RUN_DIR}
TXT
echo "[go-nogo] summary=${SUMMARY}"
exit "${GONOGO_STUB_RC:-0}"
SH
  chmod +x "${TMP_SHELL}/payment_go_nogo_decision.sh"

  cat > "${TMP_SHELL}/payment_ops_status.sh" <<'SH'
#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/runtime/ops_status_stub"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --out-dir) OUT_DIR="$2"; shift 2 ;;
    *) shift ;;
  esac
done
RUN_DIR="${OUT_DIR}/run-stub-$(date '+%Y%m%d%H%M%S')"
mkdir -p "${RUN_DIR}"
SUMMARY="${RUN_DIR}/summary.txt"
cat > "${SUMMARY}" <<TXT
run_id=stub
overall=${OPS_STATUS_STUB_OVERALL:-GREEN}
warn_count=0
block_count=0
run_dir=${RUN_DIR}
TXT
echo "[ops-status] summary=${SUMMARY}"
exit "${OPS_STATUS_STUB_RC:-0}"
SH
  chmod +x "${TMP_SHELL}/payment_ops_status.sh"
}

copy_script "payment_cutover_gate.sh"
build_stubs

PASS_LOG="${RUN_DIR}/case_pass.log"
set +e
env \
  PREFLIGHT_STUB_RC=0 \
  MAPPING_STUB_RC=0 \
  MOCK_STUB_RC=0 \
  REFUND_STUB_RC=0 \
  GONOGO_STUB_RC=0 \
  OPS_STATUS_STUB_RC=0 \
  bash -c "cd '${TMP_ROOT}' && ./shell/payment_cutover_gate.sh --date '${REPORT_DATE}' --order-no 'wxNo_smoke_order_001' --out-dir '${RUN_DIR}/case_pass' --no-alert" > "${PASS_LOG}" 2>&1
PASS_RC=$?
set -e
PASS_SUMMARY="$(sed -n 's/^\[cutover-gate\] summary=//p' "${PASS_LOG}" | tail -n 1 || true)"
PASS_OVERALL="$(kv "${PASS_SUMMARY}" "overall")"
PASS_DECISION="$(kv "${PASS_SUMMARY}" "gate_decision")"
PASS_BLOCKS="$(kv "${PASS_SUMMARY}" "block_count")"
PASS_INCIDENT_TRIGGERED="$(kv "${PASS_SUMMARY}" "incident_bundle_triggered")"
PASS_INCIDENT_RC="$(kv "${PASS_SUMMARY}" "incident_bundle_rc")"

FAIL_LOG="${RUN_DIR}/case_fail.log"
set +e
env \
  PREFLIGHT_STUB_RC=0 \
  MAPPING_STUB_RC=0 \
  MOCK_STUB_RC=0 \
  REFUND_STUB_RC=0 \
  GONOGO_STUB_RC=2 \
  GONOGO_STUB_DECISION=NO_GO \
  GONOGO_STUB_BLOCKER_COUNT=2 \
  OPS_STATUS_STUB_RC=0 \
  bash -c "cd '${TMP_ROOT}' && ./shell/payment_cutover_gate.sh --date '${REPORT_DATE}' --order-no 'wxNo_smoke_order_002' --out-dir '${RUN_DIR}/case_fail' --no-alert" > "${FAIL_LOG}" 2>&1
FAIL_RC=$?
set -e
FAIL_SUMMARY="$(sed -n 's/^\[cutover-gate\] summary=//p' "${FAIL_LOG}" | tail -n 1 || true)"
FAIL_OVERALL="$(kv "${FAIL_SUMMARY}" "overall")"
FAIL_DECISION="$(kv "${FAIL_SUMMARY}" "gate_decision")"
FAIL_BLOCKS="$(kv "${FAIL_SUMMARY}" "block_count")"
FAIL_INCIDENT_TRIGGERED="$(kv "${FAIL_SUMMARY}" "incident_bundle_triggered")"
FAIL_INCIDENT_RC="$(kv "${FAIL_SUMMARY}" "incident_bundle_rc")"

severity="PASS"
exit_code=0
fails=()

if [[ "${PASS_RC}" != "0" ]]; then
  fails+=("pass_case rc=${PASS_RC}（预期 0）")
fi
if [[ "${PASS_OVERALL}" != "GREEN" ]]; then
  fails+=("pass_case overall=${PASS_OVERALL:-N/A}（预期 GREEN）")
fi
if [[ "${PASS_DECISION}" != "GO" ]]; then
  fails+=("pass_case gate_decision=${PASS_DECISION:-N/A}（预期 GO）")
fi
if [[ "${PASS_BLOCKS}" != "0" ]]; then
  fails+=("pass_case block_count=${PASS_BLOCKS:-N/A}（预期 0）")
fi
if [[ "${PASS_INCIDENT_TRIGGERED}" != "0" ]]; then
  fails+=("pass_case incident_bundle_triggered=${PASS_INCIDENT_TRIGGERED:-N/A}（预期 0）")
fi

if [[ "${FAIL_RC}" != "2" ]]; then
  fails+=("fail_case rc=${FAIL_RC}（预期 2）")
fi
if [[ "${FAIL_OVERALL}" != "RED" ]]; then
  fails+=("fail_case overall=${FAIL_OVERALL:-N/A}（预期 RED）")
fi
if [[ "${FAIL_DECISION}" != "NO_GO" ]]; then
  fails+=("fail_case gate_decision=${FAIL_DECISION:-N/A}（预期 NO_GO）")
fi
if ! [[ "${FAIL_BLOCKS}" =~ ^[1-9][0-9]*$ ]]; then
  fails+=("fail_case block_count=${FAIL_BLOCKS:-N/A}（预期 >=1）")
fi
if [[ "${FAIL_INCIDENT_TRIGGERED}" != "1" ]]; then
  fails+=("fail_case incident_bundle_triggered=${FAIL_INCIDENT_TRIGGERED:-N/A}（预期 1）")
fi
if [[ "${FAIL_INCIDENT_RC}" != "0" ]]; then
  fails+=("fail_case incident_bundle_rc=${FAIL_INCIDENT_RC:-N/A}（预期 0）")
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
pass_case_rc=${PASS_RC}
pass_case_overall=${PASS_OVERALL}
pass_case_gate_decision=${PASS_DECISION}
pass_case_block_count=${PASS_BLOCKS}
pass_case_incident_bundle_triggered=${PASS_INCIDENT_TRIGGERED}
pass_case_incident_bundle_rc=${PASS_INCIDENT_RC}
fail_case_rc=${FAIL_RC}
fail_case_overall=${FAIL_OVERALL}
fail_case_gate_decision=${FAIL_DECISION}
fail_case_block_count=${FAIL_BLOCKS}
fail_case_incident_bundle_triggered=${FAIL_INCIDENT_TRIGGERED}
fail_case_incident_bundle_rc=${FAIL_INCIDENT_RC}
fail_reasons=${fail_text}
temp_root=${TMP_ROOT}
run_dir=${RUN_DIR}
TXT

{
  echo "# cutover_gate 离线自测"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- run_time: \`$(date '+%Y-%m-%d %H:%M:%S')\`"
  echo "- report_date: \`${REPORT_DATE}\`"
  echo "- severity: **${severity}**"
  echo
  echo "## 用例结果"
  echo
  echo "| 用例 | 预期 rc | 实际 rc | overall | gate_decision | block_count | incident_bundle_triggered | incident_bundle_rc |"
  echo "|---|---:|---:|---|---|---:|"
  echo "| pass_case | 0 | ${PASS_RC} | ${PASS_OVERALL:-N/A} | ${PASS_DECISION:-N/A} | ${PASS_BLOCKS:-N/A} | ${PASS_INCIDENT_TRIGGERED:-N/A} | ${PASS_INCIDENT_RC:-N/A} |"
  echo "| fail_case | 2 | ${FAIL_RC} | ${FAIL_OVERALL:-N/A} | ${FAIL_DECISION:-N/A} | ${FAIL_BLOCKS:-N/A} | ${FAIL_INCIDENT_TRIGGERED:-N/A} | ${FAIL_INCIDENT_RC:-N/A} |"
  echo
  echo "## 失败明细"
  echo
  if (( ${#fails[@]} == 0 )); then
    echo "- 无"
  else
    for item in "${fails[@]}"; do
      echo "- ${item}"
    done
  fi
  echo
  echo "## 追溯文件"
  echo
  echo "- summary: \`${SUMMARY_FILE}\`"
  echo "- pass_log: \`${PASS_LOG}\`"
  echo "- fail_log: \`${FAIL_LOG}\`"
  echo "- temp_root: \`${TMP_ROOT}\`"
} > "${REPORT_FILE}"

echo "[cutover-gate-smoke] summary=${SUMMARY_FILE}"
echo "[cutover-gate-smoke] report=${REPORT_FILE}"
echo "[cutover-gate-smoke] severity=${severity}, pass_case_rc=${PASS_RC}, fail_case_rc=${FAIL_RC}"

exit "${exit_code}"
