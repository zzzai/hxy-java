#!/usr/bin/env bash
set -euo pipefail

ARTIFACT_DIR=""
SUMMARY_FILE=""
ACCEPTANCE_LOG=""
TICKET_DIR=""
EMIT_ON_WARN="0"
OUTPUT_TSV=""

usage() {
  cat <<'USAGE'
Usage:
  script/dev/create_pay_notify_block_ticket.sh [options]

Options:
  --artifact-dir <dir>      Artifact 根目录（默认 /tmp/pay_notify_ci_artifacts）
  --summary-file <file>     summary.txt 路径（默认 <artifact-dir>/summary.txt）
  --acceptance-log <file>   验收日志路径（默认 <artifact-dir>/logs/acceptance.log）
  --ticket-dir <dir>        工单目录（默认 <artifact-dir>/tickets）
  --emit-on-warn <0|1>      WARN 场景是否也产出工单（默认 0）
  --output-tsv <file>       工单索引 TSV（默认 <ticket-dir>/tickets.tsv）
  -h, --help                Show help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --artifact-dir)
      ARTIFACT_DIR="$2"
      shift 2
      ;;
    --summary-file)
      SUMMARY_FILE="$2"
      shift 2
      ;;
    --acceptance-log)
      ACCEPTANCE_LOG="$2"
      shift 2
      ;;
    --ticket-dir)
      TICKET_DIR="$2"
      shift 2
      ;;
    --emit-on-warn)
      EMIT_ON_WARN="$2"
      shift 2
      ;;
    --output-tsv)
      OUTPUT_TSV="$2"
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

if [[ -z "${ARTIFACT_DIR}" ]]; then
  ARTIFACT_DIR="/tmp/pay_notify_ci_artifacts"
fi
if [[ -z "${SUMMARY_FILE}" ]]; then
  SUMMARY_FILE="${ARTIFACT_DIR}/summary.txt"
fi
if [[ -z "${ACCEPTANCE_LOG}" ]]; then
  ACCEPTANCE_LOG="${ARTIFACT_DIR}/logs/acceptance.log"
fi
if [[ -z "${TICKET_DIR}" ]]; then
  TICKET_DIR="${ARTIFACT_DIR}/tickets"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="${TICKET_DIR}/tickets.tsv"
fi

mkdir -p "${TICKET_DIR}"

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

extract_issue_preview() {
  local file="$1"
  if [[ ! -f "${file}" ]]; then
    echo "acceptance log missing"
    return
  fi
  local row
  row="$(awk '
    BEGIN {FS="\t"; found=0}
    /^severity[[:space:]]+code[[:space:]]+detail$/ {found=1; next}
    found==1 && ($1=="BLOCK" || $1=="WARN") {
      print $1 "|" $2 "|" $3;
      exit;
    }' "${file}")"
  if [[ -z "${row}" ]]; then
    echo "no issue row"
  else
    echo "${row}"
  fi
}

if ! [[ "${EMIT_ON_WARN}" =~ ^[01]$ ]]; then
  echo "Invalid --emit-on-warn: ${EMIT_ON_WARN}" >&2
  exit 1
fi

MERCHANT_ORDER_ID="$(kv "${SUMMARY_FILE}" "merchant_order_id")"
SCENARIO="$(kv "${SUMMARY_FILE}" "scenario")"
BIZ_TYPE="$(kv "${SUMMARY_FILE}" "biz_type")"
CHANNEL_ID="$(kv "${SUMMARY_FILE}" "channel_id")"
BLOCK_COUNT="$(kv "${SUMMARY_FILE}" "acceptance_block_count")"
WARN_COUNT="$(kv "${SUMMARY_FILE}" "acceptance_warn_count")"
ACCEPTANCE_RESULT="$(kv "${SUMMARY_FILE}" "acceptance_result")"
WORKFLOW_RUN_URL="$(kv "${SUMMARY_FILE}" "workflow_run_url")"
ACCEPTANCE_RC="$(kv "${SUMMARY_FILE}" "acceptance_exit_code")"

[[ -z "${MERCHANT_ORDER_ID}" ]] && MERCHANT_ORDER_ID="N/A"
[[ -z "${SCENARIO}" ]] && SCENARIO="N/A"
[[ -z "${BIZ_TYPE}" ]] && BIZ_TYPE="N/A"
[[ -z "${CHANNEL_ID}" ]] && CHANNEL_ID="N/A"
[[ -z "${BLOCK_COUNT}" ]] && BLOCK_COUNT="0"
[[ -z "${WARN_COUNT}" ]] && WARN_COUNT="0"
[[ -z "${ACCEPTANCE_RESULT}" ]] && ACCEPTANCE_RESULT="unknown"
[[ -z "${ACCEPTANCE_RC}" ]] && ACCEPTANCE_RC="unknown"

NEED_TICKET="0"
SEVERITY="INFO"
if [[ "${BLOCK_COUNT}" != "0" || "${ACCEPTANCE_RC}" == "2" ]]; then
  NEED_TICKET="1"
  SEVERITY="P1-BLOCK"
elif [[ "${EMIT_ON_WARN}" == "1" && "${WARN_COUNT}" != "0" ]]; then
  NEED_TICKET="1"
  SEVERITY="P2-WARN"
fi

if [[ "${NEED_TICKET}" != "1" ]]; then
  echo "[pay-notify-ticket] skip (no BLOCK and emit_on_warn=${EMIT_ON_WARN})"
  exit 0
fi

TS="$(date '+%Y%m%d%H%M%S')"
SAFE_ORDER_ID="$(echo "${MERCHANT_ORDER_ID}" | tr -c 'A-Za-z0-9._-' '_')"
TICKET_NO="PAY-NOTIFY-${TS}-${SAFE_ORDER_ID}"
TICKET_MD="${TICKET_DIR}/${TICKET_NO}.md"
TICKET_JSON="${TICKET_DIR}/${TICKET_NO}.json"
ISSUE_PREVIEW="$(extract_issue_preview "${ACCEPTANCE_LOG}")"

{
  echo "# ${TICKET_NO}"
  echo
  echo "- created_at: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "- severity: ${SEVERITY}"
  echo "- source: pay-notify-replay-acceptance"
  echo "- merchant_order_id: \`${MERCHANT_ORDER_ID}\`"
  echo "- scenario: \`${SCENARIO}\`"
  echo "- biz_type: \`${BIZ_TYPE}\`"
  echo "- channel_id: \`${CHANNEL_ID}\`"
  echo "- acceptance_result: ${ACCEPTANCE_RESULT}"
  echo "- acceptance_block_count: ${BLOCK_COUNT}"
  echo "- acceptance_warn_count: ${WARN_COUNT}"
  echo "- workflow_run_url: ${WORKFLOW_RUN_URL:-N/A}"
  echo
  echo "## issue_preview"
  echo
  echo "- ${ISSUE_PREVIEW}"
  echo
  echo "## artifact_refs"
  echo
  echo "- \`${ARTIFACT_DIR}/artifact_index.md\`"
  echo "- \`${ARTIFACT_DIR}/pay_notify_replay_report.md\`"
  echo "- \`${ACCEPTANCE_LOG}\`"
} > "${TICKET_MD}"

{
  echo "{"
  echo "  \"ticket_no\": \"${TICKET_NO}\","
  echo "  \"created_at\": \"$(date '+%Y-%m-%d %H:%M:%S')\","
  echo "  \"severity\": \"${SEVERITY}\","
  echo "  \"source\": \"pay-notify-replay-acceptance\","
  echo "  \"merchant_order_id\": \"${MERCHANT_ORDER_ID}\","
  echo "  \"scenario\": \"${SCENARIO}\","
  echo "  \"biz_type\": \"${BIZ_TYPE}\","
  echo "  \"channel_id\": \"${CHANNEL_ID}\","
  echo "  \"acceptance_result\": \"${ACCEPTANCE_RESULT}\","
  echo "  \"acceptance_block_count\": ${BLOCK_COUNT},"
  echo "  \"acceptance_warn_count\": ${WARN_COUNT},"
  echo "  \"workflow_run_url\": \"${WORKFLOW_RUN_URL}\""
  echo "}"
} > "${TICKET_JSON}"

if [[ ! -f "${OUTPUT_TSV}" ]]; then
  echo -e "created_at\tticket_no\tseverity\tmerchant_order_id\tscenario\tbiz_type\tchannel_id\tblock_count\twarn_count\tacceptance_result\tticket_md\tticket_json" > "${OUTPUT_TSV}"
fi
echo -e "$(date '+%Y-%m-%d %H:%M:%S')\t${TICKET_NO}\t${SEVERITY}\t${MERCHANT_ORDER_ID}\t${SCENARIO}\t${BIZ_TYPE}\t${CHANNEL_ID}\t${BLOCK_COUNT}\t${WARN_COUNT}\t${ACCEPTANCE_RESULT}\t${TICKET_MD}\t${TICKET_JSON}" >> "${OUTPUT_TSV}"

echo "[pay-notify-ticket] created ${TICKET_NO}"
echo "[pay-notify-ticket] md=${TICKET_MD}"
echo "[pay-notify-ticket] json=${TICKET_JSON}"
