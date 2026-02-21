#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

OUT_ROOT="${OUT_ROOT:-${ROOT_DIR}/.tmp/pay_notify_pipeline_smoke}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d_%H%M%S)}"
MERCHANT_ORDER_ID="${MERCHANT_ORDER_ID:-SMOKE_ORDER_001}"
SCENARIO="${SCENARIO:-duplicate}"
BIZ_TYPE="${BIZ_TYPE:-order}"
CHANNEL_ID="${CHANNEL_ID:-18}"
WORKFLOW_RUN_URL="${WORKFLOW_RUN_URL:-smoke://local}"
NOTIFY_DRY_RUN="${NOTIFY_DRY_RUN:-1}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/pay_notify_pipeline_smoke.sh [options]

Options:
  --out-root <dir>          运行输出根目录（默认 <repo>/.tmp/pay_notify_pipeline_smoke）
  --run-id <id>             运行 ID（默认 时间戳）
  --merchant-order-id <id>  商户单号（默认 SMOKE_ORDER_001）
  --scenario <name>         回放场景（默认 duplicate）
  --biz-type <name>         业务类型（默认 order）
  --channel-id <id>         渠道 ID（默认 18）
  --workflow-run-url <url>  运行链接标识（默认 smoke://local）
  --notify-dry-run <0|1>    通知是否 dry-run（默认 1）
  -h, --help                Show help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --out-root)
      OUT_ROOT="$2"
      shift 2
      ;;
    --run-id)
      RUN_ID="$2"
      shift 2
      ;;
    --merchant-order-id)
      MERCHANT_ORDER_ID="$2"
      shift 2
      ;;
    --scenario)
      SCENARIO="$2"
      shift 2
      ;;
    --biz-type)
      BIZ_TYPE="$2"
      shift 2
      ;;
    --channel-id)
      CHANNEL_ID="$2"
      shift 2
      ;;
    --workflow-run-url)
      WORKFLOW_RUN_URL="$2"
      shift 2
      ;;
    --notify-dry-run)
      NOTIFY_DRY_RUN="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if ! [[ "${NOTIFY_DRY_RUN}" =~ ^[01]$ ]]; then
  echo "Invalid --notify-dry-run: ${NOTIFY_DRY_RUN}" >&2
  exit 1
fi

RUN_DIR="${OUT_ROOT}/${RUN_ID}"
ARTIFACT_DIR="${RUN_DIR}/artifact"
mkdir -p "${ARTIFACT_DIR}/logs" "${ARTIFACT_DIR}/replay" "${ARTIFACT_DIR}/tickets"

ACCEPTANCE_LOG="${ARTIFACT_DIR}/logs/acceptance.log"
REPLAY_LOG="${ARTIFACT_DIR}/logs/replay.log"
FINAL_GATE_LOG="${ARTIFACT_DIR}/logs/final_gate.log"
REPLAY_SUMMARY="${ARTIFACT_DIR}/replay/summary.tsv"
SUMMARY_FILE="${ARTIFACT_DIR}/summary.txt"
REPORT_FILE="${ARTIFACT_DIR}/pay_notify_replay_report.md"
TICKET_TSV="${ARTIFACT_DIR}/tickets/tickets.tsv"
NOTIFY_SUMMARY="${ARTIFACT_DIR}/tickets/notify_summary.txt"
INDEX_FILE="${ARTIFACT_DIR}/artifact_index.md"

cat > "${ACCEPTANCE_LOG}" <<'EOF'
== Pay Notify Replay Acceptance ==
severity	code	detail
BLOCK	B06_NOTIFY_TASK_STUCK	task_id=901
WARN	W02_STALE_WAITING_TASK	task_id=902
Summary: BLOCK=1, WARN=1
Result: FAIL (has BLOCK issues)
EOF

cat > "${REPLAY_LOG}" <<'EOF'
[2026-02-19 13:00:00] label=duplicate_1 http=204 resp=/tmp/r1
[2026-02-19 13:00:01] label=duplicate_2 http=204 resp=/tmp/r2
EOF

cat > "${REPLAY_SUMMARY}" <<'EOF'
2026-02-19 13:00:00	duplicate_1	204	/tmp/r1
2026-02-19 13:00:01	duplicate_2	204	/tmp/r2
EOF

cat > "${FINAL_GATE_LOG}" <<'EOF'
generated_at=2026-02-19 13:00:02
replay_rc=0
acceptance_rc=2
final_gate_exit_code=2
final_gate_reason=Acceptance failed: rc=2
EOF

bash "${ROOT_DIR}/script/dev/pay_notify_replay_artifact_index.sh" \
  --out-dir "${ARTIFACT_DIR}" \
  --replay-out-dir "${ARTIFACT_DIR}/replay" \
  --replay-log "${REPLAY_LOG}" \
  --acceptance-log "${ACCEPTANCE_LOG}" \
  --final-gate-log "${FINAL_GATE_LOG}" \
  --run-replay 1 \
  --replay-exit-code 0 \
  --acceptance-exit-code 2 \
  --merchant-order-id "${MERCHANT_ORDER_ID}" \
  --scenario "${SCENARIO}" \
  --biz-type "${BIZ_TYPE}" \
  --channel-id "${CHANNEL_ID}" \
  --workflow-run-url "${WORKFLOW_RUN_URL}"

bash "${ROOT_DIR}/script/dev/generate_pay_notify_replay_report.sh" \
  --artifact-dir "${ARTIFACT_DIR}" \
  --summary-file "${SUMMARY_FILE}" \
  --acceptance-log "${ACCEPTANCE_LOG}" \
  --replay-log "${REPLAY_LOG}" \
  --output-file "${REPORT_FILE}"

bash "${ROOT_DIR}/script/dev/create_pay_notify_block_ticket.sh" \
  --artifact-dir "${ARTIFACT_DIR}" \
  --summary-file "${SUMMARY_FILE}" \
  --acceptance-log "${ACCEPTANCE_LOG}" \
  --ticket-dir "${ARTIFACT_DIR}/tickets" \
  --emit-on-warn 0 \
  --output-tsv "${TICKET_TSV}"

notify_cmd=(
  bash "${ROOT_DIR}/script/dev/notify_pay_notify_tickets.sh"
  --ticket-tsv "${TICKET_TSV}"
  --summary-file "${SUMMARY_FILE}"
  --report-file "${REPORT_FILE}"
  --artifact-index "${INDEX_FILE}"
  --notify-summary-file "${NOTIFY_SUMMARY}"
)
if [[ "${NOTIFY_DRY_RUN}" == "1" ]]; then
  notify_cmd+=(--dry-run)
fi
"${notify_cmd[@]}" > "${ARTIFACT_DIR}/logs/ticket_notify.log" 2>&1

bash "${ROOT_DIR}/script/dev/pay_notify_replay_artifact_index.sh" \
  --out-dir "${ARTIFACT_DIR}" \
  --replay-out-dir "${ARTIFACT_DIR}/replay" \
  --replay-log "${REPLAY_LOG}" \
  --acceptance-log "${ACCEPTANCE_LOG}" \
  --final-gate-log "${FINAL_GATE_LOG}" \
  --run-replay 1 \
  --replay-exit-code 0 \
  --acceptance-exit-code 2 \
  --merchant-order-id "${MERCHANT_ORDER_ID}" \
  --scenario "${SCENARIO}" \
  --biz-type "${BIZ_TYPE}" \
  --channel-id "${CHANNEL_ID}" \
  --report-file "${REPORT_FILE}" \
  --ticket-dir "${ARTIFACT_DIR}/tickets" \
  --notify-summary-file "${NOTIFY_SUMMARY}" \
  --workflow-run-url "${WORKFLOW_RUN_URL}"

assert_file() {
  local f="$1"
  if [[ ! -f "${f}" ]]; then
    echo "[smoke] missing file: ${f}" >&2
    exit 2
  fi
}

assert_contains() {
  local f="$1"
  local p="$2"
  if ! grep -qE "${p}" "${f}"; then
    echo "[smoke] pattern not found: ${p} in ${f}" >&2
    exit 2
  fi
}

assert_file "${SUMMARY_FILE}"
assert_file "${REPORT_FILE}"
assert_file "${TICKET_TSV}"
assert_file "${NOTIFY_SUMMARY}"
assert_file "${INDEX_FILE}"
assert_file "${FINAL_GATE_LOG}"

assert_contains "${SUMMARY_FILE}" '^acceptance_block_count=1$'
assert_contains "${REPORT_FILE}" 'final_status'
assert_contains "${TICKET_TSV}" '^created_at'
assert_contains "${NOTIFY_SUMMARY}" '^notify_status='
assert_contains "${INDEX_FILE}" 'notify_summary'

cat > "${RUN_DIR}/summary.txt" <<EOF
run_id=${RUN_ID}
run_dir=${RUN_DIR}
artifact_dir=${ARTIFACT_DIR}
smoke_result=PASS
merchant_order_id=${MERCHANT_ORDER_ID}
scenario=${SCENARIO}
biz_type=${BIZ_TYPE}
channel_id=${CHANNEL_ID}
workflow_run_url=${WORKFLOW_RUN_URL}
notify_dry_run=${NOTIFY_DRY_RUN}
summary_file=${SUMMARY_FILE}
report_file=${REPORT_FILE}
ticket_tsv=${TICKET_TSV}
notify_summary=${NOTIFY_SUMMARY}
index_file=${INDEX_FILE}
EOF

cat > "${RUN_DIR}/summary.json" <<EOF
{
  "run_id": "${RUN_ID}",
  "run_dir": "${RUN_DIR}",
  "artifact_dir": "${ARTIFACT_DIR}",
  "smoke_result": "PASS",
  "merchant_order_id": "${MERCHANT_ORDER_ID}",
  "scenario": "${SCENARIO}",
  "biz_type": "${BIZ_TYPE}",
  "channel_id": "${CHANNEL_ID}",
  "workflow_run_url": "${WORKFLOW_RUN_URL}",
  "notify_dry_run": ${NOTIFY_DRY_RUN},
  "summary_file": "${SUMMARY_FILE}",
  "report_file": "${REPORT_FILE}",
  "ticket_tsv": "${TICKET_TSV}",
  "notify_summary": "${NOTIFY_SUMMARY}",
  "index_file": "${INDEX_FILE}"
}
EOF

cat > "${RUN_DIR}/report.md" <<EOF
# Pay Notify Pipeline Smoke Report

- run_id: \`${RUN_ID}\`
- run_dir: \`${RUN_DIR}\`
- result: **PASS**

## Inputs

- merchant_order_id: \`${MERCHANT_ORDER_ID}\`
- scenario: \`${SCENARIO}\`
- biz_type: \`${BIZ_TYPE}\`
- channel_id: \`${CHANNEL_ID}\`
- workflow_run_url: \`${WORKFLOW_RUN_URL}\`
- notify_dry_run: \`${NOTIFY_DRY_RUN}\`

## Artifacts

- summary: \`${SUMMARY_FILE}\`
- report: \`${REPORT_FILE}\`
- ticket_tsv: \`${TICKET_TSV}\`
- notify_summary: \`${NOTIFY_SUMMARY}\`
- index: \`${INDEX_FILE}\`
EOF

echo "[smoke] summary=${RUN_DIR}/summary.txt"
echo "[smoke] report=${RUN_DIR}/report.md"
echo "[smoke] json=${RUN_DIR}/summary.json"
