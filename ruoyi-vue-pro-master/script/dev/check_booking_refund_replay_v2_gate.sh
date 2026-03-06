#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_TSV="${OUTPUT_TSV:-}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_booking_refund_replay_v2_gate.sh [options]

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
  SUMMARY_FILE="/tmp/booking_refund_replay_v2_gate/summary.txt"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="/tmp/booking_refund_replay_v2_gate/result.tsv"
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

result="PASS"
exit_code=0

controller_replay_endpoint_check="UNKNOWN"
request_ids_check="UNKNOWN"
request_dry_run_check="UNKNOWN"
response_success_count_check="UNKNOWN"
response_skip_count_check="UNKNOWN"
response_fail_count_check="UNKNOWN"
response_details_check="UNKNOWN"
replay_v2_service_anchor_check="UNKNOWN"
sql_audit_fields_check="UNKNOWN"
do_audit_fields_check="UNKNOWN"
mapper_audit_fields_check="UNKNOWN"
audit_fields_anchor_check="UNKNOWN"
error_code_invalid_param_check="UNKNOWN"
error_code_status_conflict_check="UNKNOWN"

CONTROLLER_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingRefundNotifyLogController.java"
REQUEST_VO_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingRefundNotifyLogReplayReqVO.java"
RESPONSE_VO_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingRefundNotifyLogReplayRespVO.java"
SERVICE_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingRefundNotifyLogService.java"
SERVICE_IMPL_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingRefundNotifyLogServiceImpl.java"
ERROR_CODE_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/ErrorCodeConstants.java"
SQL_FILE="${ROOT_DIR}/sql/mysql/hxy/2026-03-06-hxy-booking-refund-notify-log.sql"
DO_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/BookingRefundNotifyLogDO.java"
MAPPER_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/BookingRefundNotifyLogMapper.java"

check_required_pattern "${CONTROLLER_FILE}" '@PostMapping\("/replay"\)' "BRV201_REPLAY_ENDPOINT_MISSING" "missing replay endpoint mapping in ${CONTROLLER_FILE}" "controller_replay_endpoint_check" || true
check_required_pattern "${REQUEST_VO_FILE}" 'private[[:space:]]+List<[[:space:]]*Long[[:space:]]*>[[:space:]]+ids[[:space:]]*;' "BRV202_REPLAY_IDS_MISSING" "missing ids list field for replay v2 request in ${REQUEST_VO_FILE}" "request_ids_check" || true
check_required_pattern "${REQUEST_VO_FILE}" 'private[[:space:]]+(Boolean|boolean)[[:space:]]+dryRun[[:space:]]*;' "BRV203_REPLAY_DRY_RUN_MISSING" "missing dryRun field for replay v2 request in ${REQUEST_VO_FILE}" "request_dry_run_check" || true

check_required_pattern "${RESPONSE_VO_FILE}" 'private[[:space:]]+Integer[[:space:]]+successCount[[:space:]]*;' "BRV204_REPLAY_SUCCESS_COUNT_MISSING" "missing successCount in replay v2 response VO ${RESPONSE_VO_FILE}" "response_success_count_check" || true
check_required_pattern "${RESPONSE_VO_FILE}" 'private[[:space:]]+Integer[[:space:]]+skipCount[[:space:]]*;' "BRV205_REPLAY_SKIP_COUNT_MISSING" "missing skipCount in replay v2 response VO ${RESPONSE_VO_FILE}" "response_skip_count_check" || true
check_required_pattern "${RESPONSE_VO_FILE}" 'private[[:space:]]+Integer[[:space:]]+failCount[[:space:]]*;' "BRV206_REPLAY_FAIL_COUNT_MISSING" "missing failCount in replay v2 response VO ${RESPONSE_VO_FILE}" "response_fail_count_check" || true
check_required_pattern "${RESPONSE_VO_FILE}" 'private[[:space:]]+List<[^>]+>[[:space:]]+details[[:space:]]*;' "BRV207_REPLAY_DETAILS_MISSING" "missing details list in replay v2 response VO ${RESPONSE_VO_FILE}" "response_details_check" || true

if [[ -f "${SERVICE_FILE}" ]] && [[ -f "${SERVICE_IMPL_FILE}" ]] && \
   grep -Eq 'replay.*ids|batch.*replay|replay.*dryRun' "${SERVICE_FILE}" && \
   grep -Eq 'replay.*ids|batch.*replay|replay.*dryRun' "${SERVICE_IMPL_FILE}"; then
  replay_v2_service_anchor_check="PASS"
else
  replay_v2_service_anchor_check="FAIL"
  mark_block
  add_issue "BLOCK" "BRV208_REPLAY_V2_SERVICE_ANCHOR_MISSING" "missing replay v2 service anchor (ids/dryRun batch semantics) in ${SERVICE_FILE} or ${SERVICE_IMPL_FILE}"
fi

if [[ -f "${SQL_FILE}" ]] && grep -Eq '`retry_count`|`next_retry_time`|`error_code`|`error_msg`' "${SQL_FILE}"; then
  sql_audit_fields_check="PASS"
else
  sql_audit_fields_check="FAIL"
fi

if [[ -f "${DO_FILE}" ]] && grep -Eq 'retryCount|nextRetryTime|errorCode|errorMsg' "${DO_FILE}"; then
  do_audit_fields_check="PASS"
else
  do_audit_fields_check="FAIL"
fi

if [[ -f "${MAPPER_FILE}" ]] && grep -Eq 'updateReplaySuccess|updateReplayFailure|set\(BookingRefundNotifyLogDO::getRetryCount|set\(BookingRefundNotifyLogDO::getNextRetryTime' "${MAPPER_FILE}"; then
  mapper_audit_fields_check="PASS"
else
  mapper_audit_fields_check="FAIL"
fi

if [[ "${sql_audit_fields_check}" == "PASS" || "${do_audit_fields_check}" == "PASS" || "${mapper_audit_fields_check}" == "PASS" ]]; then
  audit_fields_anchor_check="PASS"
else
  audit_fields_anchor_check="FAIL"
  mark_block
  add_issue "BLOCK" "BRV209_REFUND_NOTIFY_LOG_AUDIT_FIELDS_MISSING" "cannot locate booking_refund_notify_log audit fields from SQL/DO/Mapper anchors"
fi

check_required_pattern "${ERROR_CODE_FILE}" 'BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID[[:space:]]*=.*(1_030_004_011|1030004011)' "BRV210_ERROR_CODE_INVALID_PARAM_MISSING" "missing invalid parameter error code anchor in ${ERROR_CODE_FILE}" "error_code_invalid_param_check" || true
check_required_pattern "${ERROR_CODE_FILE}" 'BOOKING_ORDER_REFUND_NOTIFY_LOG_STATUS_INVALID[[:space:]]*=.*(1_030_004_014|1030004014)' "BRV211_ERROR_CODE_STATUS_CONFLICT_MISSING" "missing status conflict error code anchor in ${ERROR_CODE_FILE}" "error_code_status_conflict_check" || true

{
  echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "result=${result}"
  echo "repo_root=${ROOT_DIR}"
  echo "controller_file=${CONTROLLER_FILE}"
  echo "request_vo_file=${REQUEST_VO_FILE}"
  echo "response_vo_file=${RESPONSE_VO_FILE}"
  echo "service_file=${SERVICE_FILE}"
  echo "service_impl_file=${SERVICE_IMPL_FILE}"
  echo "error_code_file=${ERROR_CODE_FILE}"
  echo "sql_file=${SQL_FILE}"
  echo "do_file=${DO_FILE}"
  echo "mapper_file=${MAPPER_FILE}"
  echo "controller_replay_endpoint_check=${controller_replay_endpoint_check}"
  echo "request_ids_check=${request_ids_check}"
  echo "request_dry_run_check=${request_dry_run_check}"
  echo "response_success_count_check=${response_success_count_check}"
  echo "response_skip_count_check=${response_skip_count_check}"
  echo "response_fail_count_check=${response_fail_count_check}"
  echo "response_details_check=${response_details_check}"
  echo "replay_v2_service_anchor_check=${replay_v2_service_anchor_check}"
  echo "sql_audit_fields_check=${sql_audit_fields_check}"
  echo "do_audit_fields_check=${do_audit_fields_check}"
  echo "mapper_audit_fields_check=${mapper_audit_fields_check}"
  echo "audit_fields_anchor_check=${audit_fields_anchor_check}"
  echo "error_code_invalid_param_check=${error_code_invalid_param_check}"
  echo "error_code_status_conflict_check=${error_code_status_conflict_check}"
  echo "output_tsv=${OUTPUT_TSV}"
} > "${SUMMARY_FILE}"

echo "== Booking Refund Replay V2 Gate =="
echo "result=${result}"
echo "summary_file=${SUMMARY_FILE}"
echo "output_tsv=${OUTPUT_TSV}"

exit "${exit_code}"
