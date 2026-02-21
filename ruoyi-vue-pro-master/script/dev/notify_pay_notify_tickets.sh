#!/usr/bin/env bash
set -euo pipefail

TICKET_TSV=""
SUMMARY_FILE=""
REPORT_FILE=""
ARTIFACT_INDEX=""
FEISHU_WEBHOOK_URL="${FEISHU_WEBHOOK_URL:-}"
WEBHOOK_URL="${WEBHOOK_URL:-}"
MAX_TICKET_LINES=5
DRY_RUN=0
STRICT=0
NOTIFY_SUMMARY_FILE=""

usage() {
  cat <<'USAGE'
Usage:
  script/dev/notify_pay_notify_tickets.sh [options]

Options:
  --ticket-tsv <file>          工单索引 TSV（默认 /tmp/pay_notify_ci_artifacts/tickets/tickets.tsv）
  --summary-file <file>        summary.txt（默认 /tmp/pay_notify_ci_artifacts/summary.txt）
  --report-file <file>         报告文件（默认 /tmp/pay_notify_ci_artifacts/pay_notify_replay_report.md）
  --artifact-index <file>      artifact_index.md（默认 /tmp/pay_notify_ci_artifacts/artifact_index.md）
  --feishu-webhook-url <url>   飞书机器人 webhook（默认读取 FEISHU_WEBHOOK_URL）
  --webhook-url <url>          通用 webhook URL（默认读取 WEBHOOK_URL）
  --max-ticket-lines <n>       消息中最多展示工单条数（默认 5）
  --dry-run                    仅打印，不发送
  --strict                     发送失败返回非 0（默认关闭）
  --notify-summary-file <file> 通知摘要输出（默认 <ticket-dir>/notify_summary.txt）
  -h, --help                   Show help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --ticket-tsv)
      TICKET_TSV="$2"
      shift 2
      ;;
    --summary-file)
      SUMMARY_FILE="$2"
      shift 2
      ;;
    --report-file)
      REPORT_FILE="$2"
      shift 2
      ;;
    --artifact-index)
      ARTIFACT_INDEX="$2"
      shift 2
      ;;
    --feishu-webhook-url)
      FEISHU_WEBHOOK_URL="$2"
      shift 2
      ;;
    --webhook-url)
      WEBHOOK_URL="$2"
      shift 2
      ;;
    --max-ticket-lines)
      MAX_TICKET_LINES="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    --strict)
      STRICT=1
      shift
      ;;
    --notify-summary-file)
      NOTIFY_SUMMARY_FILE="$2"
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

if [[ -z "${TICKET_TSV}" ]]; then
  TICKET_TSV="/tmp/pay_notify_ci_artifacts/tickets/tickets.tsv"
fi
if [[ -z "${SUMMARY_FILE}" ]]; then
  SUMMARY_FILE="/tmp/pay_notify_ci_artifacts/summary.txt"
fi
if [[ -z "${REPORT_FILE}" ]]; then
  REPORT_FILE="/tmp/pay_notify_ci_artifacts/pay_notify_replay_report.md"
fi
if [[ -z "${ARTIFACT_INDEX}" ]]; then
  ARTIFACT_INDEX="/tmp/pay_notify_ci_artifacts/artifact_index.md"
fi
if [[ -z "${NOTIFY_SUMMARY_FILE}" ]]; then
  ticket_dir="$(dirname "${TICKET_TSV}")"
  NOTIFY_SUMMARY_FILE="${ticket_dir}/notify_summary.txt"
fi

if ! [[ "${MAX_TICKET_LINES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "Invalid --max-ticket-lines: ${MAX_TICKET_LINES}" >&2
  exit 1
fi

mkdir -p "$(dirname "${NOTIFY_SUMMARY_FILE}")"

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

send_with_curl() {
  local url="$1"
  local payload="$2"
  local label="$3"
  if [[ "${DRY_RUN}" -eq 1 ]]; then
    echo "[notify-ticket] dry-run ${label} payload:"
    printf '%s\n' "${payload}"
    return 0
  fi
  local http_code
  local resp_file="/tmp/notify_ticket_resp.$$"
  http_code="$(curl -sS -o "${resp_file}" -w "%{http_code}" \
    -H "Content-Type: application/json" \
    -X POST "${url}" \
    -d "${payload}" || true)"
  if [[ "${http_code}" =~ ^2[0-9][0-9]$ ]]; then
    echo "[notify-ticket] ${label} sent, http=${http_code}"
    rm -f "${resp_file}" || true
    return 0
  fi
  echo "[notify-ticket] ${label} failed, http=${http_code}" >&2
  if [[ -f "${resp_file}" ]]; then
    echo "[notify-ticket] ${label} response: $(cat "${resp_file}")" >&2
  fi
  rm -f "${resp_file}" || true
  return 1
}

if [[ ! -f "${TICKET_TSV}" ]]; then
  echo "[notify-ticket] skip: ticket file missing: ${TICKET_TSV}"
  {
    echo "notify_at=$(date '+%Y-%m-%d %H:%M:%S')"
    echo "notify_status=SKIP"
    echo "reason=ticket_file_missing"
    echo "ticket_tsv=${TICKET_TSV}"
  } > "${NOTIFY_SUMMARY_FILE}"
  exit 0
fi

ticket_total="$(tail -n +2 "${TICKET_TSV}" | sed '/^[[:space:]]*$/d' | wc -l | tr -d ' ')"
if [[ "${ticket_total}" == "0" ]]; then
  echo "[notify-ticket] skip: no tickets"
  {
    echo "notify_at=$(date '+%Y-%m-%d %H:%M:%S')"
    echo "notify_status=SKIP"
    echo "reason=no_tickets"
    echo "ticket_tsv=${TICKET_TSV}"
  } > "${NOTIFY_SUMMARY_FILE}"
  exit 0
fi

block_total="$(awk -F'\t' 'NR>1 && $3=="P1-BLOCK"{c++} END{print c+0}' "${TICKET_TSV}")"
warn_total="$(awk -F'\t' 'NR>1 && $3=="P2-WARN"{c++} END{print c+0}' "${TICKET_TSV}")"

merchant_order_id="$(kv "${SUMMARY_FILE}" "merchant_order_id")"
scenario="$(kv "${SUMMARY_FILE}" "scenario")"
biz_type="$(kv "${SUMMARY_FILE}" "biz_type")"
channel_id="$(kv "${SUMMARY_FILE}" "channel_id")"
workflow_run_url="$(kv "${SUMMARY_FILE}" "workflow_run_url")"
[[ -z "${merchant_order_id}" ]] && merchant_order_id="N/A"
[[ -z "${scenario}" ]] && scenario="N/A"
[[ -z "${biz_type}" ]] && biz_type="N/A"
[[ -z "${channel_id}" ]] && channel_id="N/A"
[[ -z "${workflow_run_url}" ]] && workflow_run_url="N/A"

ticket_preview="$(awk -F'\t' -v n="${MAX_TICKET_LINES}" '
  NR==1 {next}
  NR>1 && $0 !~ /^[[:space:]]*$/ {
    c++;
    if (c<=n) {
      print "- " $2 " [" $3 "] order=" $4 " scenario=" $5;
    }
  }' "${TICKET_TSV}")"

text_body="【PayNotify回放验收工单告警】
ticket_total=${ticket_total}, block=${block_total}, warn=${warn_total}
merchant_order_id=${merchant_order_id}, scenario=${scenario}, biz_type=${biz_type}, channel_id=${channel_id}
workflow=${workflow_run_url}
artifact_index=${ARTIFACT_INDEX}
report=${REPORT_FILE}
ticket_tsv=${TICKET_TSV}
preview:
${ticket_preview}"

feishu_payload='{"msg_type":"text","content":{"text":"'"$(json_escape "${text_body}")"'"}}'

generic_tickets_json="$(awk -F'\t' -v n="${MAX_TICKET_LINES}" '
  BEGIN {first=1}
  NR==1 {next}
  NR>1 && $0 !~ /^[[:space:]]*$/ {
    c++;
    if (c>n) {next}
    if (!first) {printf ","}
    first=0
    gsub(/\\/,"\\\\",$2); gsub(/"/,"\\\"",$2)
    gsub(/\\/,"\\\\",$3); gsub(/"/,"\\\"",$3)
    gsub(/\\/,"\\\\",$4); gsub(/"/,"\\\"",$4)
    gsub(/\\/,"\\\\",$5); gsub(/"/,"\\\"",$5)
    gsub(/\\/,"\\\\",$6); gsub(/"/,"\\\"",$6)
    gsub(/\\/,"\\\\",$7); gsub(/"/,"\\\"",$7)
    printf "{\"ticket_no\":\"%s\",\"severity\":\"%s\",\"merchant_order_id\":\"%s\",\"scenario\":\"%s\",\"biz_type\":\"%s\",\"channel_id\":\"%s\"}", $2,$3,$4,$5,$6,$7
  }' "${TICKET_TSV}")"
generic_payload='{
  "event":"pay_notify_ticket_alert",
  "notify_at":"'"$(date '+%Y-%m-%d %H:%M:%S')"'" ,
  "ticket_total":'"${ticket_total}"',
  "block_total":'"${block_total}"',
  "warn_total":'"${warn_total}"',
  "merchant_order_id":"'"$(json_escape "${merchant_order_id}")"'",
  "scenario":"'"$(json_escape "${scenario}")"'",
  "biz_type":"'"$(json_escape "${biz_type}")"'",
  "channel_id":"'"$(json_escape "${channel_id}")"'",
  "workflow_run_url":"'"$(json_escape "${workflow_run_url}")"'",
  "artifact_index":"'"$(json_escape "${ARTIFACT_INDEX}")"'",
  "report_file":"'"$(json_escape "${REPORT_FILE}")"'",
  "ticket_tsv":"'"$(json_escape "${TICKET_TSV}")"'",
  "tickets":['"${generic_tickets_json}"']
}'

send_ok=0
send_fail=0
sent_targets=""

if [[ -n "${FEISHU_WEBHOOK_URL}" ]]; then
  if send_with_curl "${FEISHU_WEBHOOK_URL}" "${feishu_payload}" "feishu"; then
    send_ok=$((send_ok + 1))
    sent_targets="${sent_targets} feishu"
  else
    send_fail=$((send_fail + 1))
  fi
else
  echo "[notify-ticket] feishu webhook not configured, skip"
fi

if [[ -n "${WEBHOOK_URL}" ]]; then
  if send_with_curl "${WEBHOOK_URL}" "${generic_payload}" "generic_webhook"; then
    send_ok=$((send_ok + 1))
    sent_targets="${sent_targets} generic_webhook"
  else
    send_fail=$((send_fail + 1))
  fi
else
  echo "[notify-ticket] generic webhook not configured, skip"
fi

notify_status="OK"
if [[ "${send_ok}" -eq 0 && "${send_fail}" -eq 0 ]]; then
  notify_status="SKIP"
elif [[ "${send_fail}" -gt 0 ]]; then
  notify_status="PARTIAL_FAIL"
fi

{
  echo "notify_at=$(date '+%Y-%m-%d %H:%M:%S')"
  echo "notify_status=${notify_status}"
  echo "ticket_total=${ticket_total}"
  echo "block_total=${block_total}"
  echo "warn_total=${warn_total}"
  echo "send_ok=${send_ok}"
  echo "send_fail=${send_fail}"
  echo "sent_targets=${sent_targets# }"
  echo "ticket_tsv=${TICKET_TSV}"
  echo "report_file=${REPORT_FILE}"
  echo "artifact_index=${ARTIFACT_INDEX}"
} > "${NOTIFY_SUMMARY_FILE}"

echo "[notify-ticket] summary=${NOTIFY_SUMMARY_FILE}"
if [[ "${notify_status}" == "PARTIAL_FAIL" && "${STRICT}" -eq 1 ]]; then
  exit 2
fi
exit 0
