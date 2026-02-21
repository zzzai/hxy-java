#!/usr/bin/env bash
set -euo pipefail

OUT_DIR=""
SUMMARY_FILE=""
CHECK_LOG=""
REPORT_FILE=""
ISSUES_TSV=""
TICKET_DIR=""
TICKET_TSV=""
TICKET_SUMMARY_FILE=""
NOTIFY_SUMMARY_FILE=""
FINAL_GATE_LOG=""
MAX_ISSUES="20"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/payment_reconcile_artifact_index.sh --out-dir <dir> [options]

Options:
  --out-dir <dir>              产物目录（必填）
  --summary-file <file>        总结文件（默认 <out-dir>/summary.txt）
  --check-log <file>           对账检查日志（默认 <out-dir>/logs/reconcile_check.log）
  --report-file <file>         报告文件（默认 <out-dir>/payment_reconcile_report.md）
  --issues-tsv <file>          问题明细（默认 <out-dir>/reconcile/issues.tsv）
  --ticket-dir <dir>           工单目录（默认 <out-dir>/tickets）
  --ticket-tsv <file>          工单索引（默认 <ticket-dir>/tickets.tsv）
  --ticket-summary-file <file> 工单摘要（默认 <ticket-dir>/ticket_summary.txt）
  --notify-summary-file <file> 通知摘要（默认 <ticket-dir>/notify_summary.txt）
  --final-gate-log <file>      最终拦截日志（默认 <out-dir>/logs/final_gate.log）
  --max-issues <n>             索引中最多展示问题条数（默认 20）
  -h, --help                   Show help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --out-dir)
      OUT_DIR="$2"
      shift 2
      ;;
    --summary-file)
      SUMMARY_FILE="$2"
      shift 2
      ;;
    --check-log)
      CHECK_LOG="$2"
      shift 2
      ;;
    --report-file)
      REPORT_FILE="$2"
      shift 2
      ;;
    --issues-tsv)
      ISSUES_TSV="$2"
      shift 2
      ;;
    --ticket-dir)
      TICKET_DIR="$2"
      shift 2
      ;;
    --ticket-tsv)
      TICKET_TSV="$2"
      shift 2
      ;;
    --ticket-summary-file)
      TICKET_SUMMARY_FILE="$2"
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
    --max-issues)
      MAX_ISSUES="$2"
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
  echo "--out-dir is required" >&2
  usage >&2
  exit 1
fi
if ! [[ "${MAX_ISSUES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "Invalid --max-issues: ${MAX_ISSUES}" >&2
  exit 1
fi

mkdir -p "${OUT_DIR}"
if [[ -z "${SUMMARY_FILE}" ]]; then
  SUMMARY_FILE="${OUT_DIR}/summary.txt"
fi
if [[ -z "${CHECK_LOG}" ]]; then
  CHECK_LOG="${OUT_DIR}/logs/reconcile_check.log"
fi
if [[ -z "${REPORT_FILE}" ]]; then
  REPORT_FILE="${OUT_DIR}/payment_reconcile_report.md"
fi
if [[ -z "${ISSUES_TSV}" ]]; then
  ISSUES_TSV="${OUT_DIR}/reconcile/issues.tsv"
fi
if [[ -z "${TICKET_DIR}" ]]; then
  TICKET_DIR="${OUT_DIR}/tickets"
fi
if [[ -z "${TICKET_TSV}" ]]; then
  TICKET_TSV="${TICKET_DIR}/tickets.tsv"
fi
if [[ -z "${TICKET_SUMMARY_FILE}" ]]; then
  TICKET_SUMMARY_FILE="${TICKET_DIR}/ticket_summary.txt"
fi
if [[ -z "${NOTIFY_SUMMARY_FILE}" ]]; then
  NOTIFY_SUMMARY_FILE="${TICKET_DIR}/notify_summary.txt"
fi
if [[ -z "${FINAL_GATE_LOG}" ]]; then
  FINAL_GATE_LOG="${OUT_DIR}/logs/final_gate.log"
fi

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

to_rel() {
  local path="$1"
  if [[ "${path}" == "${OUT_DIR}" ]]; then
    printf '.'
  elif [[ "${path}" == "${OUT_DIR}/"* ]]; then
    printf '%s' "${path#${OUT_DIR}/}"
  else
    printf '%s' "${path}"
  fi
}

file_state() {
  local path="$1"
  if [[ -f "${path}" ]]; then
    printf 'present'
  else
    printf 'missing'
  fi
}

issue_preview=""
if [[ -f "${ISSUES_TSV}" ]]; then
  issue_preview="$(awk -F'\t' -v max_issues="${MAX_ISSUES}" '
    NR==1 {next}
    NR>1 && NF>0 {
      c++;
      if (c>max_issues) {next}
      gsub(/\|/, "\\|", $6)
      printf "| %s | %s | %s | %s | %s |\n", $1,$2,$4,$5,$6;
    }' "${ISSUES_TSV}")"
fi

ticket_preview=""
if [[ -f "${TICKET_TSV}" ]]; then
  ticket_preview="$(awk -F'\t' -v max_issues="${MAX_ISSUES}" '
    NR==1 {next}
    NR>1 && NF>0 {
      c++;
      if (c>max_issues) {next}
      printf "| %s | %s | %s | %s |\n", $2,$3,$7,$6;
    }' "${TICKET_TSV}")"
fi

INDEX_FILE="${OUT_DIR}/artifact_index.md"

{
  echo "# Payment Reconcile Artifact Index"
  echo
  echo "- generated_at: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "- pipeline_exit_code: \`$(kv "${SUMMARY_FILE}" pipeline_exit_code)\`"
  echo "- biz_date: \`$(kv "${SUMMARY_FILE}" biz_date)\`"
  echo "- reconcile_result: \`$(kv "${SUMMARY_FILE}" reconcile_result)\`"
  echo "- issue_total: \`$(kv "${SUMMARY_FILE}" issue_total)\`"
  echo "- issue_block_count: \`$(kv "${SUMMARY_FILE}" issue_block_count)\`"
  echo "- issue_warn_count: \`$(kv "${SUMMARY_FILE}" issue_warn_count)\`"
  echo "- ticket_total: \`$(kv "${SUMMARY_FILE}" ticket_total)\`"
  echo "- block_ticket_total: \`$(kv "${SUMMARY_FILE}" block_ticket_total)\`"
  echo "- warn_ticket_total: \`$(kv "${SUMMARY_FILE}" warn_ticket_total)\`"
  echo
  echo "## 1) Files"
  echo
  echo "| file | status |"
  echo "|---|---|"
  echo "| \`$(to_rel "${SUMMARY_FILE}")\` | $(file_state "${SUMMARY_FILE}") |"
  echo "| \`$(to_rel "${CHECK_LOG}")\` | $(file_state "${CHECK_LOG}") |"
  echo "| \`$(to_rel "${REPORT_FILE}")\` | $(file_state "${REPORT_FILE}") |"
  echo "| \`$(to_rel "${ISSUES_TSV}")\` | $(file_state "${ISSUES_TSV}") |"
  echo "| \`$(to_rel "${TICKET_TSV}")\` | $(file_state "${TICKET_TSV}") |"
  echo "| \`$(to_rel "${TICKET_SUMMARY_FILE}")\` | $(file_state "${TICKET_SUMMARY_FILE}") |"
  echo "| \`$(to_rel "${NOTIFY_SUMMARY_FILE}")\` | $(file_state "${NOTIFY_SUMMARY_FILE}") |"
  echo "| \`$(to_rel "${FINAL_GATE_LOG}")\` | $(file_state "${FINAL_GATE_LOG}") |"
  echo
  echo "## 2) Issue Preview"
  echo
  echo "| severity | issue_type | biz_key | code | detail |"
  echo "|---|---|---|---|---|"
  if [[ -n "${issue_preview}" ]]; then
    printf '%s\n' "${issue_preview}"
  else
    echo "| N/A | N/A | N/A | N/A | no issues or file missing |"
  fi
  echo
  echo "## 3) Ticket Preview"
  echo
  echo "| ticket_no | severity | code | biz_key |"
  echo "|---|---|---|---|"
  if [[ -n "${ticket_preview}" ]]; then
    printf '%s\n' "${ticket_preview}"
  else
    echo "| N/A | N/A | N/A | N/A |"
  fi
} > "${INDEX_FILE}"

echo "artifact_index=${INDEX_FILE}"
