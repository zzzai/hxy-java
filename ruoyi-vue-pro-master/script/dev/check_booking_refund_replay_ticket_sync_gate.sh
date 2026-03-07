#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_TSV="${OUTPUT_TSV:-}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_booking_refund_replay_ticket_sync_gate.sh [options]

Options:
  --repo-root <dir>      Repository root (default: auto-detect from script path)
  --summary-file <file>  Summary output file (optional)
  --output-tsv <file>    Result TSV output file (optional)
  -h, --help             Show help

Exit Code:
  0: PASS
  2: BLOCK
  1: Script error
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo-root)
      ROOT_DIR="$2"
      shift 2
      ;;
    --summary-file)
      SUMMARY_FILE="$2"
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

if [[ -z "${SUMMARY_FILE}" ]]; then
  SUMMARY_FILE="/tmp/booking_refund_replay_ticket_sync_gate/summary.txt"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="/tmp/booking_refund_replay_ticket_sync_gate/result.tsv"
fi

mkdir -p "$(dirname "${SUMMARY_FILE}")" "$(dirname "${OUTPUT_TSV}")"
echo -e "severity\tcode\tdetail" > "${OUTPUT_TSV}"

add_issue() {
  local severity="$1"
  local code="$2"
  local detail="$3"
  echo -e "${severity}\t${code}\t${detail}" >> "${OUTPUT_TSV}"
}

mark_block() {
  result="BLOCK"
  exit_code=2
}

check_required_pattern() {
  local file="$1"
  local pattern="$2"
  local code="$3"
  local detail="$4"
  local key="$5"

  if [[ -f "${file}" ]] && grep -Eq "${pattern}" "${file}"; then
    eval "${key}='PASS'"
    return 0
  fi

  eval "${key}='FAIL'"
  mark_block
  add_issue "BLOCK" "${code}" "${detail}"
  return 1
}

check_required_any_pattern() {
  local pattern="$1"
  local code="$2"
  local detail="$3"
  local key="$4"
  shift 4

  local file=""
  for file in "$@"; do
    if [[ -f "${file}" ]] && grep -Eq "${pattern}" "${file}"; then
      eval "${key}='PASS'"
      return 0
    fi
  done

  eval "${key}='FAIL'"
  mark_block
  add_issue "BLOCK" "${code}" "${detail}"
  return 1
}

result="PASS"
exit_code=0

replay_run_detail_page_endpoint_check="UNKNOWN"
replay_run_sync_tickets_endpoint_check="UNKNOWN"
sync_tickets_dry_run_check="UNKNOWN"
sync_tickets_force_resync_check="UNKNOWN"
summary_ticket_sync_success_count_check="UNKNOWN"
summary_ticket_sync_skip_count_check="UNKNOWN"
summary_ticket_sync_fail_count_check="UNKNOWN"
error_code_run_id_not_exists_check="UNKNOWN"

CONTROLLER_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingRefundNotifyLogController.java"
SYNC_TICKETS_REQ_VO_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingRefundReplayRunLogSyncTicketReqVO.java"
SUMMARY_RESP_VO_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingRefundReplayRunLogSummaryRespVO.java"
ERROR_CODE_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/ErrorCodeConstants.java"
OVERLAY_API_FILE="${ROOT_DIR}/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/refundNotifyLog.ts"
ROLLOUT_DOC_FILE="${ROOT_DIR}/../docs/plans/2026-03-06-ops-stageb-required-checks-rollout.md"
HANDOFF_FILE="${ROOT_DIR}/../hxy/07_memory_archive/handoffs/2026-03-07/ops-stageb-refund-replay-v4-ticket-sync-gate-window-c.md"

check_required_any_pattern '/booking/refund-notify-log/replay-run-log/detail/page|/replay-run-log/detail/page' \
  "BRT401_REPLAY_RUN_DETAIL_PAGE_ENDPOINT_MISSING" \
  "missing /booking/refund-notify-log/replay-run-log/detail/page endpoint anchor in controller/overlay" \
  "replay_run_detail_page_endpoint_check" \
  "${CONTROLLER_FILE}" "${OVERLAY_API_FILE}" || true

check_required_any_pattern '/booking/refund-notify-log/replay-run-log/sync-tickets|/replay-run-log/sync-tickets' \
  "BRT402_REPLAY_RUN_SYNC_TICKETS_ENDPOINT_MISSING" \
  "missing /booking/refund-notify-log/replay-run-log/sync-tickets endpoint anchor in controller/overlay" \
  "replay_run_sync_tickets_endpoint_check" \
  "${CONTROLLER_FILE}" "${OVERLAY_API_FILE}" || true

check_required_any_pattern 'dryRun' \
  "BRT403_SYNC_TICKETS_DRY_RUN_ANCHOR_MISSING" \
  "missing dryRun anchor for replay-run sync-tickets in request contract" \
  "sync_tickets_dry_run_check" \
  "${SYNC_TICKETS_REQ_VO_FILE}" "${OVERLAY_API_FILE}" || true

check_required_any_pattern 'forceResync' \
  "BRT404_SYNC_TICKETS_FORCE_RESYNC_ANCHOR_MISSING" \
  "missing forceResync anchor for replay-run sync-tickets in request contract" \
  "sync_tickets_force_resync_check" \
  "${SYNC_TICKETS_REQ_VO_FILE}" "${OVERLAY_API_FILE}" || true

check_required_any_pattern 'ticketSyncSuccessCount' \
  "BRT405_SUMMARY_TICKET_SYNC_SUCCESS_COUNT_MISSING" \
  "missing summary field anchor ticketSyncSuccessCount" \
  "summary_ticket_sync_success_count_check" \
  "${SUMMARY_RESP_VO_FILE}" "${OVERLAY_API_FILE}" "${ROLLOUT_DOC_FILE}" "${HANDOFF_FILE}" || true

check_required_any_pattern 'ticketSyncSkipCount' \
  "BRT406_SUMMARY_TICKET_SYNC_SKIP_COUNT_MISSING" \
  "missing summary field anchor ticketSyncSkipCount" \
  "summary_ticket_sync_skip_count_check" \
  "${SUMMARY_RESP_VO_FILE}" "${OVERLAY_API_FILE}" "${ROLLOUT_DOC_FILE}" "${HANDOFF_FILE}" || true

check_required_any_pattern 'ticketSyncFailCount' \
  "BRT407_SUMMARY_TICKET_SYNC_FAIL_COUNT_MISSING" \
  "missing summary field anchor ticketSyncFailCount" \
  "summary_ticket_sync_fail_count_check" \
  "${SUMMARY_RESP_VO_FILE}" "${OVERLAY_API_FILE}" "${ROLLOUT_DOC_FILE}" "${HANDOFF_FILE}" || true

check_required_pattern "${ERROR_CODE_FILE}" \
  'BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS[[:space:]]*=.*(1_030_004_016|1030004016)' \
  "BRT408_ERROR_CODE_RUN_ID_NOT_EXISTS_MISSING" \
  "missing runId-not-exists error code BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS(1030004016)" \
  "error_code_run_id_not_exists_check" || true

{
  echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "result=${result}"
  echo "repo_root=${ROOT_DIR}"
  echo "controller_file=${CONTROLLER_FILE}"
  echo "sync_tickets_req_vo_file=${SYNC_TICKETS_REQ_VO_FILE}"
  echo "summary_resp_vo_file=${SUMMARY_RESP_VO_FILE}"
  echo "overlay_api_file=${OVERLAY_API_FILE}"
  echo "rollout_doc_file=${ROLLOUT_DOC_FILE}"
  echo "handoff_file=${HANDOFF_FILE}"
  echo "error_code_file=${ERROR_CODE_FILE}"
  echo "replay_run_detail_page_endpoint_check=${replay_run_detail_page_endpoint_check}"
  echo "replay_run_sync_tickets_endpoint_check=${replay_run_sync_tickets_endpoint_check}"
  echo "sync_tickets_dry_run_check=${sync_tickets_dry_run_check}"
  echo "sync_tickets_force_resync_check=${sync_tickets_force_resync_check}"
  echo "summary_ticket_sync_success_count_check=${summary_ticket_sync_success_count_check}"
  echo "summary_ticket_sync_skip_count_check=${summary_ticket_sync_skip_count_check}"
  echo "summary_ticket_sync_fail_count_check=${summary_ticket_sync_fail_count_check}"
  echo "error_code_run_id_not_exists_check=${error_code_run_id_not_exists_check}"
  echo "output_tsv=${OUTPUT_TSV}"
} > "${SUMMARY_FILE}"

echo "== Booking Refund Replay Ticket Sync Gate =="
echo "result=${result}"
echo "summary_file=${SUMMARY_FILE}"
echo "output_tsv=${OUTPUT_TSV}"

exit "${exit_code}"
