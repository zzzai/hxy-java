#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_TSV="${OUTPUT_TSV:-}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_booking_refund_replay_runlog_gate.sh [options]

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
  SUMMARY_FILE="/tmp/booking_refund_replay_runlog_gate/summary.txt"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="/tmp/booking_refund_replay_runlog_gate/result.tsv"
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

check_required_rg() {
  local search_root="$1"
  local pattern="$2"
  local code="$3"
  local detail="$4"
  local key="$5"

  if rg -q --glob "*.sql" -e "${pattern}" "${search_root}"; then
    eval "${key}='PASS'"
    return 0
  fi

  eval "${key}='FAIL'"
  mark_block
  add_issue "BLOCK" "${code}" "${detail}"
  return 1
}

result="PASS"
exit_code=0

controller_replay_due_endpoint_check="UNKNOWN"
controller_replay_runlog_page_endpoint_check="UNKNOWN"
controller_replay_runlog_get_endpoint_check="UNKNOWN"
sql_runlog_table_check="UNKNOWN"
sql_runlog_run_id_check="UNKNOWN"
sql_runlog_success_count_check="UNKNOWN"
sql_runlog_skip_count_check="UNKNOWN"
sql_runlog_fail_count_check="UNKNOWN"
service_replay_due_method_check="UNKNOWN"
service_replay_stats_anchor_check="UNKNOWN"
job_run_id_anchor_check="UNKNOWN"
job_stats_anchor_check="UNKNOWN"

CONTROLLER_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingRefundNotifyLogController.java"
SERVICE_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingRefundNotifyLogService.java"
SERVICE_IMPL_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingRefundNotifyLogServiceImpl.java"
JOB_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/job/BookingRefundNotifyReplayJob.java"
SQL_DIR="${ROOT_DIR}/sql/mysql/hxy"

check_required_pattern "${CONTROLLER_FILE}" '@(PostMapping|GetMapping)\("/replay-due"\)' "BRR301_REPLAY_DUE_ENDPOINT_MISSING" "missing /booking/refund-notify-log/replay-due endpoint in ${CONTROLLER_FILE}" "controller_replay_due_endpoint_check" || true
check_required_pattern "${CONTROLLER_FILE}" '@(PostMapping|GetMapping)\("/replay-run-log/page"\)' "BRR302_REPLAY_RUNLOG_PAGE_ENDPOINT_MISSING" "missing /booking/refund-notify-log/replay-run-log/page endpoint in ${CONTROLLER_FILE}" "controller_replay_runlog_page_endpoint_check" || true
check_required_pattern "${CONTROLLER_FILE}" '@(PostMapping|GetMapping)\("/replay-run-log/get"\)' "BRR303_REPLAY_RUNLOG_GET_ENDPOINT_MISSING" "missing /booking/refund-notify-log/replay-run-log/get endpoint in ${CONTROLLER_FILE}" "controller_replay_runlog_get_endpoint_check" || true

check_required_rg "${SQL_DIR}" 'hxy_booking_refund_replay_run_log' "BRR304_RUNLOG_SQL_TABLE_MISSING" "missing hxy_booking_refund_replay_run_log SQL anchor under ${SQL_DIR}" "sql_runlog_table_check" || true
check_required_rg "${SQL_DIR}" '`run_id`|[[:space:]]run_id[[:space:]]' "BRR305_RUNLOG_SQL_RUN_ID_FIELD_MISSING" "missing run_id SQL anchor field under ${SQL_DIR}" "sql_runlog_run_id_check" || true
check_required_rg "${SQL_DIR}" '`success_count`|[[:space:]]success_count[[:space:]]' "BRR306_RUNLOG_SQL_SUCCESS_COUNT_FIELD_MISSING" "missing success_count SQL anchor field under ${SQL_DIR}" "sql_runlog_success_count_check" || true
check_required_rg "${SQL_DIR}" '`skip_count`|[[:space:]]skip_count[[:space:]]' "BRR307_RUNLOG_SQL_SKIP_COUNT_FIELD_MISSING" "missing skip_count SQL anchor field under ${SQL_DIR}" "sql_runlog_skip_count_check" || true
check_required_rg "${SQL_DIR}" '`fail_count`|[[:space:]]fail_count[[:space:]]' "BRR308_RUNLOG_SQL_FAIL_COUNT_FIELD_MISSING" "missing fail_count SQL anchor field under ${SQL_DIR}" "sql_runlog_fail_count_check" || true

if [[ -f "${SERVICE_FILE}" ]] && [[ -f "${SERVICE_IMPL_FILE}" ]] && \
   grep -Eq 'replayDueFailedLogs' "${SERVICE_FILE}" && \
   grep -Eq 'replayDueFailedLogs' "${SERVICE_IMPL_FILE}"; then
  service_replay_due_method_check="PASS"
else
  service_replay_due_method_check="FAIL"
  mark_block
  add_issue "BLOCK" "BRR309_SERVICE_REPLAY_DUE_METHOD_MISSING" "missing replayDueFailedLogs anchor in ${SERVICE_FILE} or ${SERVICE_IMPL_FILE}"
fi

if [[ -f "${SERVICE_IMPL_FILE}" ]] && grep -Eq 'setSuccessCount|setSkipCount|setFailCount|countReplayResult' "${SERVICE_IMPL_FILE}"; then
  service_replay_stats_anchor_check="PASS"
else
  service_replay_stats_anchor_check="FAIL"
  mark_block
  add_issue "BLOCK" "BRR310_SERVICE_REPLAY_STATS_ANCHOR_MISSING" "missing replay statistics anchor in ${SERVICE_IMPL_FILE}"
fi

check_required_pattern "${JOB_FILE}" 'runId[[:space:]]*=|runId=' "BRR311_JOB_RUN_ID_ANCHOR_MISSING" "missing runId anchor in ${JOB_FILE}" "job_run_id_anchor_check" || true
check_required_pattern "${JOB_FILE}" 'getSuccessCount\(\)|getSkipCount\(\)|getFailCount\(\)' "BRR312_JOB_STATS_ANCHOR_MISSING" "missing success/skip/fail stats anchor in ${JOB_FILE}" "job_stats_anchor_check" || true

{
  echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "result=${result}"
  echo "repo_root=${ROOT_DIR}"
  echo "controller_file=${CONTROLLER_FILE}"
  echo "service_file=${SERVICE_FILE}"
  echo "service_impl_file=${SERVICE_IMPL_FILE}"
  echo "job_file=${JOB_FILE}"
  echo "sql_dir=${SQL_DIR}"
  echo "controller_replay_due_endpoint_check=${controller_replay_due_endpoint_check}"
  echo "controller_replay_runlog_page_endpoint_check=${controller_replay_runlog_page_endpoint_check}"
  echo "controller_replay_runlog_get_endpoint_check=${controller_replay_runlog_get_endpoint_check}"
  echo "sql_runlog_table_check=${sql_runlog_table_check}"
  echo "sql_runlog_run_id_check=${sql_runlog_run_id_check}"
  echo "sql_runlog_success_count_check=${sql_runlog_success_count_check}"
  echo "sql_runlog_skip_count_check=${sql_runlog_skip_count_check}"
  echo "sql_runlog_fail_count_check=${sql_runlog_fail_count_check}"
  echo "service_replay_due_method_check=${service_replay_due_method_check}"
  echo "service_replay_stats_anchor_check=${service_replay_stats_anchor_check}"
  echo "job_run_id_anchor_check=${job_run_id_anchor_check}"
  echo "job_stats_anchor_check=${job_stats_anchor_check}"
  echo "output_tsv=${OUTPUT_TSV}"
} > "${SUMMARY_FILE}"

echo "== Booking Refund Replay RunLog Gate =="
echo "result=${result}"
echo "summary_file=${SUMMARY_FILE}"
echo "output_tsv=${OUTPUT_TSV}"

exit "${exit_code}"
