#!/usr/bin/env bash
set -euo pipefail

OUT_DIR=""
REPLAY_OUT_DIR=""
REPLAY_LOG=""
ACCEPTANCE_LOG=""
RUN_REPLAY="0"
REPLAY_EXIT_CODE="unknown"
ACCEPTANCE_EXIT_CODE="unknown"
MERCHANT_ORDER_ID=""
SCENARIO=""
BIZ_TYPE=""
CHANNEL_ID=""
WORKFLOW_RUN_URL=""
MAX_HIGHLIGHT_LINES=40
REPORT_FILE=""
TICKET_DIR=""
NOTIFY_SUMMARY_FILE=""
FINAL_GATE_LOG=""

usage() {
  cat <<'USAGE'
Usage:
  script/dev/pay_notify_replay_artifact_index.sh [options]

Options:
  --out-dir <dir>                  输出目录（默认 /tmp/pay_notify_ci_artifacts_时间戳）
  --replay-out-dir <dir>           回放脚本输出目录（默认 <out-dir>/replay）
  --replay-log <file>              回放日志文件（默认 <out-dir>/logs/replay.log）
  --acceptance-log <file>          验收日志文件（默认 <out-dir>/logs/acceptance.log）
  --run-replay <0|1>               本次是否执行了回放（默认 0）
  --replay-exit-code <code>        回放脚本退出码
  --acceptance-exit-code <code>    验收脚本退出码
  --merchant-order-id <id>         商户单号
  --scenario <name>                duplicate|out_of_order|delayed
  --biz-type <name>                order|refund|transfer
  --channel-id <id>                渠道 ID
  --workflow-run-url <url>         Workflow 运行地址
  --max-highlight-lines <n>        高亮行数上限（默认 40）
  --report-file <file>             回放验收报告文件（默认 <out-dir>/pay_notify_replay_report.md）
  --ticket-dir <dir>               工单目录（默认 <out-dir>/tickets）
  --notify-summary-file <file>     通知摘要文件（默认 <ticket-dir>/notify_summary.txt）
  --final-gate-log <file>          最终门禁日志文件（默认 <out-dir>/logs/final_gate.log）
  -h, --help                       Show help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --out-dir)
      OUT_DIR="$2"
      shift 2
      ;;
    --replay-out-dir)
      REPLAY_OUT_DIR="$2"
      shift 2
      ;;
    --replay-log)
      REPLAY_LOG="$2"
      shift 2
      ;;
    --acceptance-log)
      ACCEPTANCE_LOG="$2"
      shift 2
      ;;
    --run-replay)
      RUN_REPLAY="$2"
      shift 2
      ;;
    --replay-exit-code)
      REPLAY_EXIT_CODE="$2"
      shift 2
      ;;
    --acceptance-exit-code)
      ACCEPTANCE_EXIT_CODE="$2"
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
    --max-highlight-lines)
      MAX_HIGHLIGHT_LINES="$2"
      shift 2
      ;;
    --report-file)
      REPORT_FILE="$2"
      shift 2
      ;;
    --ticket-dir)
      TICKET_DIR="$2"
      shift 2
      ;;
    --notify-summary-file)
      NOTIFY_SUMMARY_FILE="$2"
      shift 2
      ;;
    --final-gate-log)
      FINAL_GATE_LOG="$2"
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

if [[ -z "${OUT_DIR}" ]]; then
  OUT_DIR="/tmp/pay_notify_ci_artifacts_$(date +%Y%m%d_%H%M%S)"
fi
if [[ -z "${REPLAY_OUT_DIR}" ]]; then
  REPLAY_OUT_DIR="${OUT_DIR}/replay"
fi
if [[ -z "${REPLAY_LOG}" ]]; then
  REPLAY_LOG="${OUT_DIR}/logs/replay.log"
fi
if [[ -z "${ACCEPTANCE_LOG}" ]]; then
  ACCEPTANCE_LOG="${OUT_DIR}/logs/acceptance.log"
fi
if [[ -z "${REPORT_FILE}" ]]; then
  REPORT_FILE="${OUT_DIR}/pay_notify_replay_report.md"
fi
if [[ -z "${TICKET_DIR}" ]]; then
  TICKET_DIR="${OUT_DIR}/tickets"
fi
if [[ -z "${NOTIFY_SUMMARY_FILE}" ]]; then
  NOTIFY_SUMMARY_FILE="${TICKET_DIR}/notify_summary.txt"
fi
if [[ -z "${FINAL_GATE_LOG}" ]]; then
  FINAL_GATE_LOG="${OUT_DIR}/logs/final_gate.log"
fi
if ! [[ "${MAX_HIGHLIGHT_LINES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "Invalid --max-highlight-lines: ${MAX_HIGHLIGHT_LINES}" >&2
  exit 1
fi

mkdir -p "${OUT_DIR}" "${OUT_DIR}/logs" "${REPLAY_OUT_DIR}"

INDEX_MD="${OUT_DIR}/artifact_index.md"
SUMMARY_TXT="${OUT_DIR}/summary.txt"
REPLAY_SUMMARY_TSV="${REPLAY_OUT_DIR}/summary.tsv"

file_exists() {
  [[ -f "$1" ]] && echo "yes" || echo "no"
}

file_lines() {
  if [[ -f "$1" ]]; then
    wc -l < "$1" | tr -d ' '
  else
    echo "0"
  fi
}

file_size() {
  if [[ -f "$1" ]]; then
    stat -c %s "$1"
  else
    echo "0"
  fi
}

normalize_rc_status() {
  local rc="$1"
  if [[ "${rc}" == "0" ]]; then
    echo "PASS"
  elif [[ "${rc}" == "2" ]]; then
    echo "BLOCK"
  elif [[ "${rc}" == "unknown" ]]; then
    echo "UNKNOWN"
  else
    echo "FAIL(rc=${rc})"
  fi
}

extract_acceptance_result() {
  local file="$1"
  local result=""
  if [[ -f "${file}" ]]; then
    result="$(grep -E '^Result:' "${file}" | tail -n 1 | sed 's/^Result:[[:space:]]*//')"
  fi
  if [[ -z "${result}" ]]; then
    echo "unknown"
  else
    echo "${result}"
  fi
}

extract_acceptance_count() {
  local file="$1"
  local key="$2"
  local line value
  if [[ ! -f "${file}" ]]; then
    echo "0"
    return
  fi
  line="$(grep -E '^Summary:[[:space:]]+BLOCK=[0-9]+,[[:space:]]+WARN=[0-9]+' "${file}" | tail -n 1 || true)"
  if [[ -z "${line}" ]]; then
    echo "0"
    return
  fi
  value="$(echo "${line}" | sed -n "s/.*${key}=\\([0-9]\\+\\).*/\\1/p")"
  if [[ -z "${value}" ]]; then
    echo "0"
  else
    echo "${value}"
  fi
}

ACCEPTANCE_RESULT="$(extract_acceptance_result "${ACCEPTANCE_LOG}")"
BLOCK_COUNT="$(extract_acceptance_count "${ACCEPTANCE_LOG}" "BLOCK")"
WARN_COUNT="$(extract_acceptance_count "${ACCEPTANCE_LOG}" "WARN")"
REPLAY_RC_STATUS="$(normalize_rc_status "${REPLAY_EXIT_CODE}")"
ACCEPTANCE_RC_STATUS="$(normalize_rc_status "${ACCEPTANCE_EXIT_CODE}")"
REPLAY_HTTP_COUNT="$(file_lines "${REPLAY_SUMMARY_TSV}")"
TICKET_TSV="${TICKET_DIR}/tickets.tsv"

{
  echo "generated_at=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "out_dir=${OUT_DIR}"
  echo "replay_out_dir=${REPLAY_OUT_DIR}"
  echo "replay_summary_tsv=${REPLAY_SUMMARY_TSV}"
  echo "replay_summary_exists=$(file_exists "${REPLAY_SUMMARY_TSV}")"
  echo "replay_http_count=${REPLAY_HTTP_COUNT}"
  echo "run_replay=${RUN_REPLAY}"
  echo "replay_exit_code=${REPLAY_EXIT_CODE}"
  echo "replay_status=${REPLAY_RC_STATUS}"
  echo "acceptance_exit_code=${ACCEPTANCE_EXIT_CODE}"
  echo "acceptance_status=${ACCEPTANCE_RC_STATUS}"
  echo "acceptance_result=${ACCEPTANCE_RESULT}"
  echo "acceptance_block_count=${BLOCK_COUNT}"
  echo "acceptance_warn_count=${WARN_COUNT}"
  echo "merchant_order_id=${MERCHANT_ORDER_ID}"
  echo "scenario=${SCENARIO}"
  echo "biz_type=${BIZ_TYPE}"
  echo "channel_id=${CHANNEL_ID}"
  echo "workflow_run_url=${WORKFLOW_RUN_URL}"
  echo "replay_log=${REPLAY_LOG}"
  echo "acceptance_log=${ACCEPTANCE_LOG}"
  echo "final_gate_log=${FINAL_GATE_LOG}"
  echo "report_file=${REPORT_FILE}"
  echo "ticket_dir=${TICKET_DIR}"
  echo "ticket_tsv=${TICKET_TSV}"
  echo "notify_summary_file=${NOTIFY_SUMMARY_FILE}"
  echo "report_exists=$(file_exists "${REPORT_FILE}")"
  echo "ticket_tsv_exists=$(file_exists "${TICKET_TSV}")"
  echo "notify_summary_exists=$(file_exists "${NOTIFY_SUMMARY_FILE}")"
  echo "index_md=${INDEX_MD}"
} > "${SUMMARY_TXT}"

{
  echo "# Pay Notify Replay Artifact Index"
  echo
  echo "- generated_at: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "- workflow_run_url: ${WORKFLOW_RUN_URL:-N/A}"
  echo "- merchant_order_id: \`${MERCHANT_ORDER_ID:-N/A}\`"
  echo "- scenario: \`${SCENARIO:-N/A}\`"
  echo "- biz_type: \`${BIZ_TYPE:-N/A}\`"
  echo "- channel_id: \`${CHANNEL_ID:-N/A}\`"
  echo
  echo "## 1) Overall Result"
  echo
  echo "| item | value |"
  echo "|---|---|"
  echo "| run_replay | ${RUN_REPLAY} |"
  echo "| replay_exit_code | ${REPLAY_EXIT_CODE} (${REPLAY_RC_STATUS}) |"
  echo "| acceptance_exit_code | ${ACCEPTANCE_EXIT_CODE} (${ACCEPTANCE_RC_STATUS}) |"
  echo "| acceptance_result | ${ACCEPTANCE_RESULT} |"
  echo "| acceptance BLOCK | ${BLOCK_COUNT} |"
  echo "| acceptance WARN | ${WARN_COUNT} |"
  echo
  echo "## 2) Logs"
  echo
  echo "| file | exists | size_bytes | lines |"
  echo "|---|---|---:|---:|"
  echo "| \`${REPLAY_LOG}\` | $(file_exists "${REPLAY_LOG}") | $(file_size "${REPLAY_LOG}") | $(file_lines "${REPLAY_LOG}") |"
  echo "| \`${ACCEPTANCE_LOG}\` | $(file_exists "${ACCEPTANCE_LOG}") | $(file_size "${ACCEPTANCE_LOG}") | $(file_lines "${ACCEPTANCE_LOG}") |"
  echo "| \`${FINAL_GATE_LOG}\` | $(file_exists "${FINAL_GATE_LOG}") | $(file_size "${FINAL_GATE_LOG}") | $(file_lines "${FINAL_GATE_LOG}") |"
  echo
  echo "## 3) Replay HTTP Summary"
  echo
  if [[ -f "${REPLAY_SUMMARY_TSV}" ]]; then
    echo "| ts | label | http_code | response_file |"
    echo "|---|---|---:|---|"
    while IFS=$'\t' read -r ts label code resp; do
      [[ -n "${ts}" ]] || continue
      echo "| ${ts} | ${label} | ${code} | \`${resp}\` |"
    done < "${REPLAY_SUMMARY_TSV}"
  else
    echo "- missing \`${REPLAY_SUMMARY_TSV}\`"
  fi
  echo
  echo "## 4) Failure Highlights"
  echo
  has_highlight=0
  for file in "${REPLAY_LOG}" "${ACCEPTANCE_LOG}" "${FINAL_GATE_LOG}"; do
    [[ -f "${file}" ]] || continue
    highlights="$(grep -Eain 'BLOCK|WARN|FAIL|ERROR|EXCEPTION|Traceback|timed out|denied|rejected|invalid' "${file}" 2>/dev/null | tail -n "${MAX_HIGHLIGHT_LINES}" || true)"
    [[ -n "${highlights}" ]] || continue
    has_highlight=1
    echo "### $(basename "${file}")"
    echo
    echo "- file: \`${file}\`"
    echo
    echo '```text'
    echo "${highlights}"
    echo '```'
    echo
  done
  if [[ "${has_highlight}" -eq 0 ]]; then
    echo "- no highlights"
    echo
  fi
  echo "## 5) Report & Tickets"
  echo
  echo "| item | path | exists |"
  echo "|---|---|---|"
  echo "| report | \`${REPORT_FILE}\` | $(file_exists "${REPORT_FILE}") |"
  echo "| ticket_tsv | \`${TICKET_TSV}\` | $(file_exists "${TICKET_TSV}") |"
  echo "| notify_summary | \`${NOTIFY_SUMMARY_FILE}\` | $(file_exists "${NOTIFY_SUMMARY_FILE}") |"
  if [[ -d "${TICKET_DIR}" ]]; then
    while IFS= read -r ticket_file; do
      [[ -n "${ticket_file}" ]] || continue
      base="$(basename "${ticket_file}")"
      if [[ "${base}" == "tickets.tsv" ]]; then
        continue
      fi
      if [[ "${base}" == "notify_summary.txt" ]]; then
        continue
      fi
      echo "| ticket_file | \`${ticket_file}\` | yes |"
    done < <(find "${TICKET_DIR}" -maxdepth 1 -type f 2>/dev/null | sort)
  fi
  echo
  echo "## 6) Summary File"
  echo
  echo "- \`${SUMMARY_TXT}\`"
} > "${INDEX_MD}"

echo "[pay-notify-artifact-index] index=${INDEX_MD}"
echo "[pay-notify-artifact-index] summary=${SUMMARY_TXT}"
