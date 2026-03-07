#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_TSV="${OUTPUT_TSV:-}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_booking_refund_replay_run_summary_gate.sh [options]

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
  SUMMARY_FILE="/tmp/booking_refund_replay_run_summary_gate/summary.txt"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="/tmp/booking_refund_replay_run_summary_gate/result.tsv"
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

check_required_pattern_ci() {
  local file="$1"
  local pattern="$2"
  local code="$3"
  local detail="$4"
  local key="$5"

  if [[ -f "${file}" ]] && grep -Eiq "${pattern}" "${file}"; then
    eval "${key}='PASS'"
    return 0
  fi

  eval "${key}='FAIL'"
  mark_block
  add_issue "BLOCK" "${code}" "${detail}"
  return 1
}

check_required_any_pattern_ci() {
  local pattern="$1"
  local code="$2"
  local detail="$3"
  local key="$4"
  shift 4

  local file=""
  for file in "$@"; do
    if [[ -f "${file}" ]] && grep -Eiq "${pattern}" "${file}"; then
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

controller_replay_run_summary_endpoint_check="UNKNOWN"
controller_replay_run_sync_tickets_endpoint_check="UNKNOWN"
service_summary_method_check="UNKNOWN"
service_sync_tickets_method_check="UNKNOWN"
service_impl_summary_method_check="UNKNOWN"
service_impl_sync_tickets_method_check="UNKNOWN"
service_impl_fail_open_semantics_check="UNKNOWN"
controller_test_summary_case_check="UNKNOWN"
controller_test_sync_tickets_case_check="UNKNOWN"
service_test_summary_case_check="UNKNOWN"
service_test_sync_tickets_case_check="UNKNOWN"
service_test_fail_open_case_check="UNKNOWN"
error_code_run_id_not_exists_check="UNKNOWN"
constant_ticket_sync_degraded_check="UNKNOWN"

CONTROLLER_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingRefundNotifyLogController.java"
SERVICE_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingRefundNotifyLogService.java"
SERVICE_IMPL_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingRefundNotifyLogServiceImpl.java"
CONTROLLER_TEST_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingRefundNotifyLogControllerTest.java"
SERVICE_TEST_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingRefundNotifyLogServiceTest.java"
ERROR_CODE_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/ErrorCodeConstants.java"

check_required_pattern "${CONTROLLER_FILE}" '@(PostMapping|GetMapping)\("/replay-run-log/summary"\)' "BRS501_REPLAY_RUN_SUMMARY_ENDPOINT_MISSING" "missing /booking/refund-notify-log/replay-run-log/summary endpoint in ${CONTROLLER_FILE}" "controller_replay_run_summary_endpoint_check" || true
check_required_pattern "${CONTROLLER_FILE}" '@(PostMapping|GetMapping)\("/replay-run-log/sync-tickets"\)' "BRS502_REPLAY_RUN_SYNC_TICKETS_ENDPOINT_MISSING" "missing /booking/refund-notify-log/replay-run-log/sync-tickets endpoint in ${CONTROLLER_FILE}" "controller_replay_run_sync_tickets_endpoint_check" || true

check_required_pattern "${SERVICE_FILE}" '(replayRunLogSummary|getReplayRunLogSummary|summaryReplayRunLog)[[:space:]]*\(' "BRS503_SERVICE_SUMMARY_METHOD_MISSING" "missing replay-run summary method anchor in ${SERVICE_FILE}" "service_summary_method_check" || true
check_required_pattern "${SERVICE_FILE}" '(syncReplayRunLogTickets|syncReplayRunTickets|replayRunLogSyncTickets|syncTicketsByRunId)[[:space:]]*\(' "BRS504_SERVICE_SYNC_TICKETS_METHOD_MISSING" "missing replay-run sync tickets method anchor in ${SERVICE_FILE}" "service_sync_tickets_method_check" || true
check_required_pattern "${SERVICE_IMPL_FILE}" '(replayRunLogSummary|getReplayRunLogSummary|summaryReplayRunLog)[[:space:]]*\(' "BRS505_SERVICE_IMPL_SUMMARY_METHOD_MISSING" "missing replay-run summary implementation anchor in ${SERVICE_IMPL_FILE}" "service_impl_summary_method_check" || true
check_required_pattern "${SERVICE_IMPL_FILE}" '(syncReplayRunLogTickets|syncReplayRunTickets|replayRunLogSyncTickets|syncTicketsByRunId)[[:space:]]*\(' "BRS506_SERVICE_IMPL_SYNC_TICKETS_METHOD_MISSING" "missing replay-run sync tickets implementation anchor in ${SERVICE_IMPL_FILE}" "service_impl_sync_tickets_method_check" || true
check_required_pattern_ci "${SERVICE_IMPL_FILE}" '(fail[-_ ]?open|degrad(e|ed)|TICKET_SYNC_WARN|SYNC_TICKETS?_DEGRADE(D)?|TICKET_SYNC_DEGRADED)' "BRS507_SERVICE_IMPL_FAIL_OPEN_ANCHOR_MISSING" "missing fail-open/degraded ticket-sync anchor in ${SERVICE_IMPL_FILE}" "service_impl_fail_open_semantics_check" || true

check_required_pattern_ci "${CONTROLLER_TEST_FILE}" 'replayRunLogSummary|replayRunSummary|/replay-run-log/summary' "BRS508_CONTROLLER_TEST_SUMMARY_CASE_MISSING" "missing replay-run summary test anchor in ${CONTROLLER_TEST_FILE}" "controller_test_summary_case_check" || true
check_required_pattern_ci "${CONTROLLER_TEST_FILE}" 'syncTickets|sync[-_ ]tickets|/replay-run-log/sync-tickets' "BRS509_CONTROLLER_TEST_SYNC_TICKETS_CASE_MISSING" "missing replay-run sync-tickets test anchor in ${CONTROLLER_TEST_FILE}" "controller_test_sync_tickets_case_check" || true
check_required_pattern_ci "${SERVICE_TEST_FILE}" 'replayRunLogSummary|replayRunSummary|run summary' "BRS510_SERVICE_TEST_SUMMARY_CASE_MISSING" "missing replay-run summary service test anchor in ${SERVICE_TEST_FILE}" "service_test_summary_case_check" || true
check_required_pattern_ci "${SERVICE_TEST_FILE}" 'syncTickets|sync[-_ ]tickets|ticket sync|syncReplayRunLogTickets|replayRunLogSyncTickets' "BRS511_SERVICE_TEST_SYNC_TICKETS_CASE_MISSING" "missing replay-run sync-tickets service test anchor in ${SERVICE_TEST_FILE}" "service_test_sync_tickets_case_check" || true
check_required_pattern_ci "${SERVICE_TEST_FILE}" '(fail[-_ ]?open|degrad(e|ed)|warn)' "BRS512_SERVICE_TEST_FAIL_OPEN_CASE_MISSING" "missing fail-open/degraded test anchor in ${SERVICE_TEST_FILE}" "service_test_fail_open_case_check" || true

check_required_pattern "${ERROR_CODE_FILE}" 'BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS[[:space:]]*=.*(1_030_004_016|1030004016)' "BRS513_ERROR_CODE_RUN_ID_NOT_EXISTS_MISSING" "missing runId-not-exists error code anchor in ${ERROR_CODE_FILE}" "error_code_run_id_not_exists_check" || true
check_required_any_pattern_ci '(TICKET_SYNC_(DEGRADE|DEGRADED|WARN)|SYNC_TICKETS?_(DEGRADE|DEGRADED|WARN)|TICKET_SYNC_DEGRADED|ticket[[:space:]_\-]*sync[[:space:]_\-]*degrad|degrad(e|ed)[[:space:]_\-]*ticket[[:space:]_\-]*sync|fail[-_ ]?open[[:space:]_\-]*ticket[[:space:]_\-]*sync)' "BRS514_CONSTANT_TICKET_SYNC_DEGRADED_MISSING" "missing ticket-sync degraded constant/anchor (search scope: service impl/test/error codes)" "constant_ticket_sync_degraded_check" "${SERVICE_IMPL_FILE}" "${SERVICE_TEST_FILE}" "${ERROR_CODE_FILE}" || true

{
  echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "result=${result}"
  echo "repo_root=${ROOT_DIR}"
  echo "controller_file=${CONTROLLER_FILE}"
  echo "service_file=${SERVICE_FILE}"
  echo "service_impl_file=${SERVICE_IMPL_FILE}"
  echo "controller_test_file=${CONTROLLER_TEST_FILE}"
  echo "service_test_file=${SERVICE_TEST_FILE}"
  echo "error_code_file=${ERROR_CODE_FILE}"
  echo "controller_replay_run_summary_endpoint_check=${controller_replay_run_summary_endpoint_check}"
  echo "controller_replay_run_sync_tickets_endpoint_check=${controller_replay_run_sync_tickets_endpoint_check}"
  echo "service_summary_method_check=${service_summary_method_check}"
  echo "service_sync_tickets_method_check=${service_sync_tickets_method_check}"
  echo "service_impl_summary_method_check=${service_impl_summary_method_check}"
  echo "service_impl_sync_tickets_method_check=${service_impl_sync_tickets_method_check}"
  echo "service_impl_fail_open_semantics_check=${service_impl_fail_open_semantics_check}"
  echo "controller_test_summary_case_check=${controller_test_summary_case_check}"
  echo "controller_test_sync_tickets_case_check=${controller_test_sync_tickets_case_check}"
  echo "service_test_summary_case_check=${service_test_summary_case_check}"
  echo "service_test_sync_tickets_case_check=${service_test_sync_tickets_case_check}"
  echo "service_test_fail_open_case_check=${service_test_fail_open_case_check}"
  echo "error_code_run_id_not_exists_check=${error_code_run_id_not_exists_check}"
  echo "constant_ticket_sync_degraded_check=${constant_ticket_sync_degraded_check}"
  echo "output_tsv=${OUTPUT_TSV}"
} > "${SUMMARY_FILE}"

echo "== Booking Refund Replay Run Summary Gate =="
echo "result=${result}"
echo "summary_file=${SUMMARY_FILE}"
echo "output_tsv=${OUTPUT_TSV}"

exit "${exit_code}"
