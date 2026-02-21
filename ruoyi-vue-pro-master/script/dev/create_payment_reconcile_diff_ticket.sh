#!/usr/bin/env bash
set -euo pipefail

ISSUES_TSV=""
SUMMARY_FILE=""
TICKET_DIR=""
OUTPUT_TSV=""
EMIT_ON_WARN="0"
TICKET_SUMMARY_FILE=""

usage() {
  cat <<'USAGE'
Usage:
  script/dev/create_payment_reconcile_diff_ticket.sh [options]

Options:
  --issues-tsv <file>          对账问题明细 TSV（默认 /tmp/payment_reconcile/issues.tsv）
  --summary-file <file>        对账 summary.txt（默认 /tmp/payment_reconcile/summary.txt）
  --ticket-dir <dir>           工单输出目录（默认 /tmp/payment_reconcile/tickets）
  --output-tsv <file>          工单索引 TSV（默认 <ticket-dir>/tickets.tsv）
  --ticket-summary-file <file> 工单摘要（默认 <ticket-dir>/ticket_summary.txt）
  --emit-on-warn <0|1>         WARN 是否产出工单（默认 0）
  -h, --help                   Show help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --issues-tsv)
      ISSUES_TSV="$2"
      shift 2
      ;;
    --summary-file)
      SUMMARY_FILE="$2"
      shift 2
      ;;
    --ticket-dir)
      TICKET_DIR="$2"
      shift 2
      ;;
    --output-tsv)
      OUTPUT_TSV="$2"
      shift 2
      ;;
    --ticket-summary-file)
      TICKET_SUMMARY_FILE="$2"
      shift 2
      ;;
    --emit-on-warn)
      EMIT_ON_WARN="$2"
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

if [[ -z "${ISSUES_TSV}" ]]; then
  ISSUES_TSV="/tmp/payment_reconcile/issues.tsv"
fi
if [[ -z "${SUMMARY_FILE}" ]]; then
  SUMMARY_FILE="/tmp/payment_reconcile/summary.txt"
fi
if [[ -z "${TICKET_DIR}" ]]; then
  TICKET_DIR="/tmp/payment_reconcile/tickets"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="${TICKET_DIR}/tickets.tsv"
fi
if [[ -z "${TICKET_SUMMARY_FILE}" ]]; then
  TICKET_SUMMARY_FILE="${TICKET_DIR}/ticket_summary.txt"
fi
if ! [[ "${EMIT_ON_WARN}" =~ ^[01]$ ]]; then
  echo "Invalid --emit-on-warn: ${EMIT_ON_WARN}" >&2
  exit 1
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

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

BIZ_DATE="$(kv "${SUMMARY_FILE}" biz_date)"
[[ -z "${BIZ_DATE}" ]] && BIZ_DATE="$(date +%F)"

if [[ ! -f "${ISSUES_TSV}" ]]; then
  echo "[payment-reconcile-ticket] skip: issues file missing: ${ISSUES_TSV}"
  {
    echo "generated_at=$(date '+%Y-%m-%d %H:%M:%S')"
    echo "biz_date=${BIZ_DATE}"
    echo "ticket_total=0"
    echo "block_ticket_total=0"
    echo "warn_ticket_total=0"
    echo "reason=issues_file_missing"
    echo "issues_tsv=${ISSUES_TSV}"
  } > "${TICKET_SUMMARY_FILE}"
  exit 0
fi

if [[ ! -f "${OUTPUT_TSV}" ]]; then
  echo -e "created_at\tticket_no\tseverity\tissue_type\tentity_type\tbiz_key\tcode\texpected_amount\tactual_amount\toccurred_at\tdetail\tticket_md\tticket_json" > "${OUTPUT_TSV}"
fi

ticket_total=0
block_ticket_total=0
warn_ticket_total=0
counter=0

while IFS=$'\t' read -r severity issue_type entity_type biz_key code detail expected_amount actual_amount occurred_at; do
  [[ "${severity}" == "severity" ]] && continue
  [[ -z "${severity}" ]] && continue

  should_emit=0
  ticket_severity=""
  if [[ "${severity}" == "BLOCK" ]]; then
    should_emit=1
    ticket_severity="P1-BLOCK"
  elif [[ "${severity}" == "WARN" && "${EMIT_ON_WARN}" == "1" ]]; then
    should_emit=1
    ticket_severity="P2-WARN"
  fi

  if [[ "${should_emit}" != "1" ]]; then
    continue
  fi

  counter=$((counter + 1))
  ticket_total=$((ticket_total + 1))
  if [[ "${ticket_severity}" == "P1-BLOCK" ]]; then
    block_ticket_total=$((block_ticket_total + 1))
  else
    warn_ticket_total=$((warn_ticket_total + 1))
  fi

  ts="$(date '+%Y%m%d%H%M%S')"
  safe_code="$(echo "${code}" | tr -c 'A-Za-z0-9._-' '_')"
  ticket_no="PAY-RECON-${BIZ_DATE//-/}-${counter}-${safe_code}"
  ticket_md="${TICKET_DIR}/${ticket_no}.md"
  ticket_json="${TICKET_DIR}/${ticket_no}.json"

  {
    echo "# ${ticket_no}"
    echo
    echo "- created_at: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "- biz_date: ${BIZ_DATE}"
    echo "- severity: ${ticket_severity}"
    echo "- source: payment-reconcile-daily"
    echo "- issue_type: \`${issue_type}\`"
    echo "- entity_type: \`${entity_type}\`"
    echo "- biz_key: \`${biz_key}\`"
    echo "- code: \`${code}\`"
    echo "- expected_amount: \`${expected_amount:-N/A}\`"
    echo "- actual_amount: \`${actual_amount:-N/A}\`"
    echo "- occurred_at: \`${occurred_at:-N/A}\`"
    echo
    echo "## detail"
    echo
    echo "- ${detail}"
    echo
    echo "## artifact_refs"
    echo
    echo "- \`${ISSUES_TSV}\`"
    echo "- \`${SUMMARY_FILE}\`"
  } > "${ticket_md}"

  {
    echo "{"
    echo "  \"ticket_no\": \"${ticket_no}\","
    echo "  \"created_at\": \"$(date '+%Y-%m-%d %H:%M:%S')\","
    echo "  \"biz_date\": \"${BIZ_DATE}\","
    echo "  \"severity\": \"${ticket_severity}\","
    echo "  \"source\": \"payment-reconcile-daily\","
    echo "  \"issue_type\": \"$(json_escape "${issue_type}")\","
    echo "  \"entity_type\": \"$(json_escape "${entity_type}")\","
    echo "  \"biz_key\": \"$(json_escape "${biz_key}")\","
    echo "  \"code\": \"$(json_escape "${code}")\","
    echo "  \"detail\": \"$(json_escape "${detail}")\","
    echo "  \"expected_amount\": \"$(json_escape "${expected_amount}")\","
    echo "  \"actual_amount\": \"$(json_escape "${actual_amount}")\","
    echo "  \"occurred_at\": \"$(json_escape "${occurred_at}")\""
    echo "}"
  } > "${ticket_json}"

  echo -e "$(date '+%Y-%m-%d %H:%M:%S')\t${ticket_no}\t${ticket_severity}\t${issue_type}\t${entity_type}\t${biz_key}\t${code}\t${expected_amount}\t${actual_amount}\t${occurred_at}\t${detail}\t${ticket_md}\t${ticket_json}" >> "${OUTPUT_TSV}"
done < "${ISSUES_TSV}"

{
  echo "generated_at=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "biz_date=${BIZ_DATE}"
  echo "emit_on_warn=${EMIT_ON_WARN}"
  echo "ticket_total=${ticket_total}"
  echo "block_ticket_total=${block_ticket_total}"
  echo "warn_ticket_total=${warn_ticket_total}"
  echo "ticket_tsv=${OUTPUT_TSV}"
  echo "ticket_dir=${TICKET_DIR}"
  echo "issues_tsv=${ISSUES_TSV}"
  echo "summary_file=${SUMMARY_FILE}"
} > "${TICKET_SUMMARY_FILE}"

echo "[payment-reconcile-ticket] ticket_total=${ticket_total}, block=${block_ticket_total}, warn=${warn_ticket_total}"
echo "[payment-reconcile-ticket] ticket_tsv=${OUTPUT_TSV}"
