#!/usr/bin/env bash
set -euo pipefail

ARTIFACT_DIR=""
SUMMARY_FILE=""
ACCEPTANCE_LOG=""
REPLAY_LOG=""
OUTPUT_FILE=""

usage() {
  cat <<'USAGE'
Usage:
  script/dev/generate_pay_notify_replay_report.sh [options]

Options:
  --artifact-dir <dir>     Artifact 根目录（默认 /tmp/pay_notify_ci_artifacts）
  --summary-file <file>    summary.txt 路径（默认 <artifact-dir>/summary.txt）
  --acceptance-log <file>  验收日志路径（默认 <artifact-dir>/logs/acceptance.log）
  --replay-log <file>      回放日志路径（默认 <artifact-dir>/logs/replay.log）
  --output-file <file>     报告输出路径（默认 <artifact-dir>/pay_notify_replay_report.md）
  -h, --help               Show help
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
    --replay-log)
      REPLAY_LOG="$2"
      shift 2
      ;;
    --output-file)
      OUTPUT_FILE="$2"
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
if [[ -z "${REPLAY_LOG}" ]]; then
  REPLAY_LOG="${ARTIFACT_DIR}/logs/replay.log"
fi
if [[ -z "${OUTPUT_FILE}" ]]; then
  OUTPUT_FILE="${ARTIFACT_DIR}/pay_notify_replay_report.md"
fi

mkdir -p "$(dirname "${OUTPUT_FILE}")"

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

status_from_summary() {
  local block="$1"
  local warn="$2"
  local acceptance="$3"
  local replay_rc="$4"
  local acceptance_rc="$5"
  if [[ "${replay_rc}" != "0" || "${acceptance_rc}" != "0" || "${block}" != "0" ]]; then
    echo "FAIL"
    return
  fi
  if [[ "${warn}" != "0" || "${acceptance}" == "PASS_WITH_WARN" ]]; then
    echo "PASS_WITH_WARN"
    return
  fi
  echo "PASS"
}

extract_issues_table() {
  local file="$1"
  if [[ ! -f "${file}" ]]; then
    echo "| severity | code | detail |"
    echo "|---|---|---|"
    echo "| N/A | N/A | acceptance log missing |"
    return
  fi
  local rows
  rows="$(awk '
    BEGIN {FS="\t"; found=0}
    /^severity[[:space:]]+code[[:space:]]+detail$/ {found=1; next}
    found==1 && ($1=="BLOCK" || $1=="WARN") {
      gsub(/\|/, "\\|", $3);
      print "| " $1 " | " $2 " | " $3 " |"
    }' "${file}")"
  echo "| severity | code | detail |"
  echo "|---|---|---|"
  if [[ -n "${rows}" ]]; then
    printf "%s\n" "${rows}"
  else
    echo "| N/A | N/A | no issue rows in acceptance log |"
  fi
}

extract_result_line() {
  local file="$1"
  if [[ ! -f "${file}" ]]; then
    echo "unknown"
    return
  fi
  local line
  line="$(grep -E '^Result:' "${file}" | tail -n 1 || true)"
  if [[ -z "${line}" ]]; then
    echo "unknown"
  else
    echo "${line#Result: }"
  fi
}

GENERATED_AT="$(date '+%Y-%m-%d %H:%M:%S')"
SUMMARY_GENERATED_AT="$(kv "${SUMMARY_FILE}" "generated_at")"
MERCHANT_ORDER_ID="$(kv "${SUMMARY_FILE}" "merchant_order_id")"
SCENARIO="$(kv "${SUMMARY_FILE}" "scenario")"
BIZ_TYPE="$(kv "${SUMMARY_FILE}" "biz_type")"
CHANNEL_ID="$(kv "${SUMMARY_FILE}" "channel_id")"
RUN_REPLAY="$(kv "${SUMMARY_FILE}" "run_replay")"
REPLAY_RC="$(kv "${SUMMARY_FILE}" "replay_exit_code")"
ACCEPTANCE_RC="$(kv "${SUMMARY_FILE}" "acceptance_exit_code")"
ACCEPTANCE_RESULT="$(kv "${SUMMARY_FILE}" "acceptance_result")"
BLOCK_COUNT="$(kv "${SUMMARY_FILE}" "acceptance_block_count")"
WARN_COUNT="$(kv "${SUMMARY_FILE}" "acceptance_warn_count")"
REPLAY_HTTP_COUNT="$(kv "${SUMMARY_FILE}" "replay_http_count")"
WORKFLOW_RUN_URL="$(kv "${SUMMARY_FILE}" "workflow_run_url")"

[[ -z "${MERCHANT_ORDER_ID}" ]] && MERCHANT_ORDER_ID="N/A"
[[ -z "${SCENARIO}" ]] && SCENARIO="N/A"
[[ -z "${BIZ_TYPE}" ]] && BIZ_TYPE="N/A"
[[ -z "${CHANNEL_ID}" ]] && CHANNEL_ID="N/A"
[[ -z "${RUN_REPLAY}" ]] && RUN_REPLAY="N/A"
[[ -z "${REPLAY_RC}" ]] && REPLAY_RC="unknown"
[[ -z "${ACCEPTANCE_RC}" ]] && ACCEPTANCE_RC="unknown"
[[ -z "${ACCEPTANCE_RESULT}" ]] && ACCEPTANCE_RESULT="$(extract_result_line "${ACCEPTANCE_LOG}")"
[[ -z "${BLOCK_COUNT}" ]] && BLOCK_COUNT="0"
[[ -z "${WARN_COUNT}" ]] && WARN_COUNT="0"
[[ -z "${REPLAY_HTTP_COUNT}" ]] && REPLAY_HTTP_COUNT="0"

FINAL_STATUS="$(status_from_summary "${BLOCK_COUNT}" "${WARN_COUNT}" "${ACCEPTANCE_RESULT}" "${REPLAY_RC}" "${ACCEPTANCE_RC}")"

{
  echo "# Pay Notify Replay Acceptance Report"
  echo
  echo "- generated_at: ${GENERATED_AT}"
  echo "- summary_generated_at: ${SUMMARY_GENERATED_AT:-N/A}"
  echo "- workflow_run_url: ${WORKFLOW_RUN_URL:-N/A}"
  echo
  echo "## 1) Summary"
  echo
  echo "| field | value |"
  echo "|---|---|"
  echo "| final_status | **${FINAL_STATUS}** |"
  echo "| merchant_order_id | \`${MERCHANT_ORDER_ID}\` |"
  echo "| scenario | \`${SCENARIO}\` |"
  echo "| biz_type | \`${BIZ_TYPE}\` |"
  echo "| channel_id | \`${CHANNEL_ID}\` |"
  echo "| run_replay | ${RUN_REPLAY} |"
  echo "| replay_exit_code | ${REPLAY_RC} |"
  echo "| acceptance_exit_code | ${ACCEPTANCE_RC} |"
  echo "| acceptance_result | ${ACCEPTANCE_RESULT} |"
  echo "| acceptance_block_count | ${BLOCK_COUNT} |"
  echo "| acceptance_warn_count | ${WARN_COUNT} |"
  echo "| replay_http_count | ${REPLAY_HTTP_COUNT} |"
  echo
  echo "## 2) Acceptance Issues"
  echo
  extract_issues_table "${ACCEPTANCE_LOG}"
  echo
  echo "## 3) Artifact Files"
  echo
  echo "- summary: \`${SUMMARY_FILE}\`"
  echo "- acceptance_log: \`${ACCEPTANCE_LOG}\`"
  echo "- replay_log: \`${REPLAY_LOG}\`"
  echo "- index: \`${ARTIFACT_DIR}/artifact_index.md\`"
} > "${OUTPUT_FILE}"

echo "[pay-notify-report] report=${OUTPUT_FILE}"
