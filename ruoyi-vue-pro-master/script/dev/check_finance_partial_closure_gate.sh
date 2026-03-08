#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_TSV="${OUTPUT_TSV:-}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_finance_partial_closure_gate.sh [options]

Options:
  --repo-root <dir>      Repository root (default: auto-detect from script path)
  --summary-file <file>  Summary output file (optional)
  --output-tsv <file>    Result TSV output file (optional)
  -h, --help             Show help

Exit Code:
  0: PASS / WARN
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
  SUMMARY_FILE="/tmp/finance_partial_closure_gate/summary.txt"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="/tmp/finance_partial_closure_gate/result.tsv"
fi

mkdir -p "$(dirname "${SUMMARY_FILE}")" "$(dirname "${OUTPUT_TSV}")"
echo -e "severity\tcode\tdetail" > "${OUTPUT_TSV}"

add_issue() {
  local severity="$1"
  local code="$2"
  local detail="$3"
  echo -e "${severity}\t${code}\t${detail}" >> "${OUTPUT_TSV}"
}

mark_warn() {
  if [[ "${result}" == "PASS" ]]; then
    result="WARN"
  fi
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

check_warn_pattern_ci() {
  local file="$1"
  local pattern="$2"
  local code="$3"
  local detail="$4"
  local key="$5"

  if [[ -f "${file}" ]] && grep -Eiq "${pattern}" "${file}"; then
    eval "${key}='PASS'"
    return 0
  fi

  eval "${key}='WARN'"
  mark_warn
  add_issue "WARN" "${code}" "${detail}"
  return 1
}

result="PASS"
exit_code=0

refund_idempotent_error_code_check="UNKNOWN"
refund_idempotent_throw_check="UNKNOWN"
refund_idempotent_branch_check="UNKNOWN"
replay_error_code_check="UNKNOWN"
replay_run_id_throw_check="UNKNOWN"
replay_controller_replay_endpoint_check="UNKNOWN"
replay_controller_replay_due_endpoint_check="UNKNOWN"
replay_controller_summary_endpoint_check="UNKNOWN"
replay_controller_sync_tickets_endpoint_check="UNKNOWN"
replay_service_replay_failed_logs_check="UNKNOWN"
replay_service_replay_due_logs_check="UNKNOWN"
replay_service_summary_method_check="UNKNOWN"
replay_service_sync_method_check="UNKNOWN"
replay_service_fail_open_log_check="UNKNOWN"
replay_service_degrade_log_check="UNKNOWN"
replay_service_ticket_sync_warning_tag_check="UNKNOWN"
commission_accrual_error_code_check="UNKNOWN"
commission_accrual_biz_type_check="UNKNOWN"
commission_accrual_source_biz_no_check="UNKNOWN"
commission_accrual_payload_consistency_check="UNKNOWN"
commission_accrual_mapper_biz_key_usage_check="UNKNOWN"
commission_accrual_conflict_throw_check="UNKNOWN"
commission_reversal_error_code_check="UNKNOWN"
commission_reversal_source_biz_no_check="UNKNOWN"
commission_reversal_duplicate_key_semantics_check="UNKNOWN"
commission_reversal_payload_consistency_check="UNKNOWN"
commission_mapper_origin_lookup_check="UNKNOWN"
commission_mapper_biz_key_lookup_check="UNKNOWN"
reversal_unique_sql_column_check="UNKNOWN"
reversal_unique_sql_index_check="UNKNOWN"
four_account_summary_trade_minus_commission_field_check="UNKNOWN"
four_account_summary_commission_amount_field_check="UNKNOWN"
four_account_summary_commission_diff_abs_field_check="UNKNOWN"
four_account_summary_unresolved_field_check="UNKNOWN"
four_account_refund_summary_difference_amount_field_check="UNKNOWN"
four_account_refund_summary_unresolved_field_check="UNKNOWN"
four_account_summary_degraded_field_check="UNKNOWN"
four_account_refund_summary_degraded_field_check="UNKNOWN"
four_account_service_trade_minus_commission_agg_check="UNKNOWN"
four_account_service_trade_minus_commission_setter_check="UNKNOWN"
four_account_service_commission_setter_check="UNKNOWN"
four_account_service_commission_diff_abs_setter_check="UNKNOWN"
four_account_service_difference_amount_agg_check="UNKNOWN"
four_account_service_difference_amount_setter_check="UNKNOWN"
four_account_service_unresolved_ticket_setter_check="UNKNOWN"
four_account_service_degraded_set_check="UNKNOWN"
four_account_service_degrade_log_check="UNKNOWN"
four_account_service_fail_open_entry_check="UNKNOWN"
four_account_service_fail_open_catch_check="UNKNOWN"
four_account_service_fail_open_failed_order_capture_check="UNKNOWN"
four_account_service_fail_open_semantics_check="UNKNOWN"
booking_order_test_idempotent_case_check="UNKNOWN"
booking_refund_replay_test_anchor_check="UNKNOWN"
technician_commission_test_anchor_check="UNKNOWN"
four_account_test_fail_open_case_check="UNKNOWN"

ERROR_CODE_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/ErrorCodeConstants.java"
BOOKING_ORDER_SERVICE_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingOrderServiceImpl.java"
BOOKING_REFUND_NOTIFY_LOG_CONTROLLER_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingRefundNotifyLogController.java"
BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingRefundNotifyLogServiceImpl.java"
TECHNICIAN_COMMISSION_SERVICE_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/TechnicianCommissionServiceImpl.java"
TECHNICIAN_COMMISSION_MAPPER_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/TechnicianCommissionMapper.java"
REVERSAL_SQL_FILE="${ROOT_DIR}/sql/mysql/hxy/2026-03-02-hxy-technician-commission-reversal-idempotency.sql"
FOUR_ACCOUNT_SUMMARY_VO_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/FourAccountReconcileSummaryRespVO.java"
FOUR_ACCOUNT_REFUND_SUMMARY_VO_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/FourAccountRefundAuditSummaryRespVO.java"
FOUR_ACCOUNT_SERVICE_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/FourAccountReconcileServiceImpl.java"
BOOKING_ORDER_TEST_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingOrderServiceImplTest.java"
BOOKING_REFUND_NOTIFY_LOG_SERVICE_TEST_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingRefundNotifyLogServiceTest.java"
TECHNICIAN_COMMISSION_TEST_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/TechnicianCommissionServiceImplTest.java"
FOUR_ACCOUNT_SERVICE_TEST_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/FourAccountReconcileServiceImplTest.java"

# 1) 退款回调幂等冲突错误码 + 关键分支
check_required_pattern "${ERROR_CODE_FILE}" \
  'BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT[[:space:]]*=.*(1_030_004_012|1030004012)' \
  "FPC001_REFUND_IDEMPOTENT_ERROR_CODE_MISSING" \
  "missing BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012) anchor in ${ERROR_CODE_FILE}" \
  "refund_idempotent_error_code_check" || true

check_required_pattern "${BOOKING_ORDER_SERVICE_FILE}" \
  'throw[[:space:]]+exception\(BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT\)' \
  "FPC002_REFUND_IDEMPOTENT_THROW_MISSING" \
  "missing BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT throw anchor in ${BOOKING_ORDER_SERVICE_FILE}" \
  "refund_idempotent_throw_check" || true

check_required_pattern "${BOOKING_ORDER_SERVICE_FILE}" \
  'order\.getPayRefundId\(\)[[:space:]]*!=[[:space:]]*null[[:space:]]*&&[[:space:]]*order\.getPayRefundId\(\)\.equals\(payRefundId\)' \
  "FPC003_REFUND_IDEMPOTENT_BRANCH_MISSING" \
  "missing refund callback idempotent same-refund-id branch in ${BOOKING_ORDER_SERVICE_FILE}" \
  "refund_idempotent_branch_check" || true

# 2) 退款 replay 关键锚点（controller/service/error-code/fail-open）
check_required_pattern "${ERROR_CODE_FILE}" \
  'BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS[[:space:]]*=.*(1_030_004_016|1030004016)' \
  "FPC018_REFUND_REPLAY_RUN_ID_ERROR_CODE_MISSING" \
  "missing BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS(1030004016) anchor in ${ERROR_CODE_FILE}" \
  "replay_error_code_check" || true

check_required_pattern "${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}" \
  'throw[[:space:]]+exception\(BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS\)' \
  "FPC019_REFUND_REPLAY_RUN_ID_THROW_MISSING" \
  "missing BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS throw anchor in ${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}" \
  "replay_run_id_throw_check" || true

check_required_pattern "${BOOKING_REFUND_NOTIFY_LOG_CONTROLLER_FILE}" \
  '@PostMapping\("/replay"\)' \
  "FPC020_REFUND_REPLAY_ENDPOINT_MISSING" \
  "missing /replay endpoint anchor in ${BOOKING_REFUND_NOTIFY_LOG_CONTROLLER_FILE}" \
  "replay_controller_replay_endpoint_check" || true

check_required_pattern "${BOOKING_REFUND_NOTIFY_LOG_CONTROLLER_FILE}" \
  '@PostMapping\("/replay-due"\)' \
  "FPC021_REFUND_REPLAY_DUE_ENDPOINT_MISSING" \
  "missing /replay-due endpoint anchor in ${BOOKING_REFUND_NOTIFY_LOG_CONTROLLER_FILE}" \
  "replay_controller_replay_due_endpoint_check" || true

check_required_pattern "${BOOKING_REFUND_NOTIFY_LOG_CONTROLLER_FILE}" \
  '@GetMapping\("/replay-run-log/summary"\)' \
  "FPC022_REFUND_REPLAY_SUMMARY_ENDPOINT_MISSING" \
  "missing /replay-run-log/summary endpoint anchor in ${BOOKING_REFUND_NOTIFY_LOG_CONTROLLER_FILE}" \
  "replay_controller_summary_endpoint_check" || true

check_required_pattern "${BOOKING_REFUND_NOTIFY_LOG_CONTROLLER_FILE}" \
  '@PostMapping\("/replay-run-log/sync-tickets"\)' \
  "FPC023_REFUND_REPLAY_SYNC_TICKETS_ENDPOINT_MISSING" \
  "missing /replay-run-log/sync-tickets endpoint anchor in ${BOOKING_REFUND_NOTIFY_LOG_CONTROLLER_FILE}" \
  "replay_controller_sync_tickets_endpoint_check" || true

check_required_pattern "${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}" \
  'replayFailedLogs[[:space:]]*\(' \
  "FPC024_REFUND_REPLAY_SERVICE_METHOD_MISSING" \
  "missing replayFailedLogs service anchor in ${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}" \
  "replay_service_replay_failed_logs_check" || true

check_required_pattern "${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}" \
  'replayDueFailedLogs[[:space:]]*\(' \
  "FPC025_REFUND_REPLAY_DUE_SERVICE_METHOD_MISSING" \
  "missing replayDueFailedLogs service anchor in ${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}" \
  "replay_service_replay_due_logs_check" || true

check_required_pattern "${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}" \
  'getReplayRunLogSummary[[:space:]]*\(' \
  "FPC026_REFUND_REPLAY_SUMMARY_SERVICE_METHOD_MISSING" \
  "missing getReplayRunLogSummary service anchor in ${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}" \
  "replay_service_summary_method_check" || true

check_required_pattern "${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}" \
  'syncReplayRunLogTickets[[:space:]]*\(' \
  "FPC027_REFUND_REPLAY_SYNC_TICKETS_SERVICE_METHOD_MISSING" \
  "missing syncReplayRunLogTickets service anchor in ${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}" \
  "replay_service_sync_method_check" || true

check_required_pattern_ci "${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}" \
  'fail-open continue' \
  "FPC028_REFUND_REPLAY_FAIL_OPEN_LOG_MISSING" \
  "missing replay fail-open continue log anchor in ${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}" \
  "replay_service_fail_open_log_check" || true

check_required_pattern_ci "${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}" \
  'refresh failed, degrade continue' \
  "FPC029_REFUND_REPLAY_DEGRADE_LOG_MISSING" \
  "missing replay refresh degrade-continue log anchor in ${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}" \
  "replay_service_degrade_log_check" || true

check_required_pattern "${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}" \
  'WARNING_TAG_TICKET_SYNC_DEGRADED' \
  "FPC030_REFUND_REPLAY_TICKET_SYNC_WARNING_TAG_MISSING" \
  "missing WARNING_TAG_TICKET_SYNC_DEGRADED anchor in ${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}" \
  "replay_service_ticket_sync_warning_tag_check" || true

# 3) 提成计提/冲正幂等关键契约
check_required_pattern "${ERROR_CODE_FILE}" \
  'COMMISSION_ACCRUAL_IDEMPOTENT_CONFLICT[[:space:]]*=.*(1_030_007_012|1030007012)' \
  "FPC031_COMMISSION_ACCRUAL_ERROR_CODE_MISSING" \
  "missing COMMISSION_ACCRUAL_IDEMPOTENT_CONFLICT(1030007012) anchor in ${ERROR_CODE_FILE}" \
  "commission_accrual_error_code_check" || true

check_required_pattern "${TECHNICIAN_COMMISSION_SERVICE_FILE}" \
  'ACCRUAL_BIZ_TYPE[[:space:]]*=[[:space:]]*"FULFILLMENT_COMPLETE"' \
  "FPC032_COMMISSION_ACCRUAL_BIZ_TYPE_MISSING" \
  "missing ACCRUAL_BIZ_TYPE=FULFILLMENT_COMPLETE anchor in ${TECHNICIAN_COMMISSION_SERVICE_FILE}" \
  "commission_accrual_biz_type_check" || true

check_required_pattern "${TECHNICIAN_COMMISSION_SERVICE_FILE}" \
  'buildAccrualSourceBizNo[[:space:]]*\(' \
  "FPC033_COMMISSION_ACCRUAL_SOURCE_BIZ_NO_MISSING" \
  "missing buildAccrualSourceBizNo anchor in ${TECHNICIAN_COMMISSION_SERVICE_FILE}" \
  "commission_accrual_source_biz_no_check" || true

check_required_pattern "${TECHNICIAN_COMMISSION_SERVICE_FILE}" \
  'ensureAccrualPayloadConsistent[[:space:]]*\(' \
  "FPC034_COMMISSION_ACCRUAL_PAYLOAD_CONSISTENCY_MISSING" \
  "missing ensureAccrualPayloadConsistent idempotent contract anchor in ${TECHNICIAN_COMMISSION_SERVICE_FILE}" \
  "commission_accrual_payload_consistency_check" || true

check_required_pattern "${TECHNICIAN_COMMISSION_SERVICE_FILE}" \
  'selectByBizKey\(ACCRUAL_BIZ_TYPE' \
  "FPC035_COMMISSION_ACCRUAL_BIZ_KEY_LOOKUP_USAGE_MISSING" \
  "missing accrual selectByBizKey(ACCRUAL_BIZ_TYPE, ...) usage anchor in ${TECHNICIAN_COMMISSION_SERVICE_FILE}" \
  "commission_accrual_mapper_biz_key_usage_check" || true

check_required_pattern "${TECHNICIAN_COMMISSION_SERVICE_FILE}" \
  'throw[[:space:]]+exception\(COMMISSION_ACCRUAL_IDEMPOTENT_CONFLICT\)' \
  "FPC036_COMMISSION_ACCRUAL_CONFLICT_THROW_MISSING" \
  "missing COMMISSION_ACCRUAL_IDEMPOTENT_CONFLICT throw anchor in ${TECHNICIAN_COMMISSION_SERVICE_FILE}" \
  "commission_accrual_conflict_throw_check" || true

check_required_pattern "${ERROR_CODE_FILE}" \
  'COMMISSION_REVERSAL_IDEMPOTENT_CONFLICT[[:space:]]*=.*(1_030_007_011|1030007011)' \
  "FPC004_COMMISSION_REVERSAL_ERROR_CODE_MISSING" \
  "missing COMMISSION_REVERSAL_IDEMPOTENT_CONFLICT(1030007011) anchor in ${ERROR_CODE_FILE}" \
  "commission_reversal_error_code_check" || true

check_required_pattern "${TECHNICIAN_COMMISSION_SERVICE_FILE}" \
  'buildReversalSourceBizNo[[:space:]]*\(' \
  "FPC044_COMMISSION_REVERSAL_SOURCE_BIZ_NO_MISSING" \
  "missing buildReversalSourceBizNo anchor in ${TECHNICIAN_COMMISSION_SERVICE_FILE}" \
  "commission_reversal_source_biz_no_check" || true

check_required_pattern "${TECHNICIAN_COMMISSION_SERVICE_FILE}" \
  'catch[[:space:]]*\([[:space:]]*DuplicateKeyException' \
  "FPC005_COMMISSION_REVERSAL_DUPLICATE_KEY_SEMANTICS_MISSING" \
  "missing DuplicateKeyException conflict semantic anchor in ${TECHNICIAN_COMMISSION_SERVICE_FILE}" \
  "commission_reversal_duplicate_key_semantics_check" || true

check_required_pattern "${TECHNICIAN_COMMISSION_SERVICE_FILE}" \
  'ensureReversalPayloadConsistent[[:space:]]*\(' \
  "FPC006_COMMISSION_REVERSAL_PAYLOAD_CONSISTENCY_MISSING" \
  "missing ensureReversalPayloadConsistent idempotent contract anchor in ${TECHNICIAN_COMMISSION_SERVICE_FILE}" \
  "commission_reversal_payload_consistency_check" || true

check_required_pattern "${TECHNICIAN_COMMISSION_MAPPER_FILE}" \
  'selectByOriginCommissionId[[:space:]]*\(' \
  "FPC007_COMMISSION_MAPPER_ORIGIN_LOOKUP_MISSING" \
  "missing selectByOriginCommissionId idempotent lookup anchor in ${TECHNICIAN_COMMISSION_MAPPER_FILE}" \
  "commission_mapper_origin_lookup_check" || true

check_required_pattern "${TECHNICIAN_COMMISSION_MAPPER_FILE}" \
  'selectByBizKey[[:space:]]*\(' \
  "FPC008_COMMISSION_MAPPER_BIZ_KEY_LOOKUP_MISSING" \
  "missing selectByBizKey idempotent lookup anchor in ${TECHNICIAN_COMMISSION_MAPPER_FILE}" \
  "commission_mapper_biz_key_lookup_check" || true

# 4) 退款冲正唯一约束 / 冲突语义
check_required_pattern "${REVERSAL_SQL_FILE}" \
  'ADD[[:space:]]+COLUMN[[:space:]]+IF[[:space:]]+NOT[[:space:]]+EXISTS[[:space:]]+`origin_commission_id`' \
  "FPC009_REVERSAL_UNIQUE_COLUMN_MISSING" \
  "missing origin_commission_id idempotent column anchor in ${REVERSAL_SQL_FILE}" \
  "reversal_unique_sql_column_check" || true

check_required_pattern "${REVERSAL_SQL_FILE}" \
  'ADD[[:space:]]+UNIQUE[[:space:]]+INDEX[[:space:]]+`uk_origin_commission_id`[[:space:]]*\(`origin_commission_id`\)' \
  "FPC010_REVERSAL_UNIQUE_INDEX_MISSING" \
  "missing unique index uk_origin_commission_id(origin_commission_id) in ${REVERSAL_SQL_FILE}" \
  "reversal_unique_sql_index_check" || true

# 5) 四账提成聚合摘要字段 + fail-open/degrade 标记
check_required_pattern "${FOUR_ACCOUNT_SUMMARY_VO_FILE}" \
  'tradeMinusCommissionSplitSum' \
  "FPC037_FOUR_ACCOUNT_SUMMARY_TRADE_MINUS_COMMISSION_FIELD_MISSING" \
  "missing tradeMinusCommissionSplitSum field in ${FOUR_ACCOUNT_SUMMARY_VO_FILE}" \
  "four_account_summary_trade_minus_commission_field_check" || true

check_required_pattern "${FOUR_ACCOUNT_SUMMARY_VO_FILE}" \
  'commissionAmountSum' \
  "FPC038_FOUR_ACCOUNT_SUMMARY_COMMISSION_AMOUNT_FIELD_MISSING" \
  "missing commissionAmountSum field in ${FOUR_ACCOUNT_SUMMARY_VO_FILE}" \
  "four_account_summary_commission_amount_field_check" || true

check_required_pattern "${FOUR_ACCOUNT_SUMMARY_VO_FILE}" \
  'commissionDifferenceAbsSum' \
  "FPC039_FOUR_ACCOUNT_SUMMARY_COMMISSION_DIFF_ABS_FIELD_MISSING" \
  "missing commissionDifferenceAbsSum field in ${FOUR_ACCOUNT_SUMMARY_VO_FILE}" \
  "four_account_summary_commission_diff_abs_field_check" || true

check_required_pattern "${FOUR_ACCOUNT_SUMMARY_VO_FILE}" \
  'unresolvedTicketCount' \
  "FPC046_FOUR_ACCOUNT_SUMMARY_UNRESOLVED_FIELD_MISSING" \
  "missing unresolvedTicketCount field in ${FOUR_ACCOUNT_SUMMARY_VO_FILE}" \
  "four_account_summary_unresolved_field_check" || true

check_required_pattern "${FOUR_ACCOUNT_REFUND_SUMMARY_VO_FILE}" \
  'differenceAmountSum' \
  "FPC040_FOUR_ACCOUNT_REFUND_SUMMARY_DIFFERENCE_AMOUNT_FIELD_MISSING" \
  "missing differenceAmountSum field in ${FOUR_ACCOUNT_REFUND_SUMMARY_VO_FILE}" \
  "four_account_refund_summary_difference_amount_field_check" || true

check_required_pattern "${FOUR_ACCOUNT_REFUND_SUMMARY_VO_FILE}" \
  'unresolvedTicketCount' \
  "FPC047_FOUR_ACCOUNT_REFUND_SUMMARY_UNRESOLVED_FIELD_MISSING" \
  "missing unresolvedTicketCount field in ${FOUR_ACCOUNT_REFUND_SUMMARY_VO_FILE}" \
  "four_account_refund_summary_unresolved_field_check" || true

check_required_pattern "${FOUR_ACCOUNT_SUMMARY_VO_FILE}" \
  'ticketSummaryDegraded' \
  "FPC011_FOUR_ACCOUNT_SUMMARY_DEGRADED_FIELD_MISSING" \
  "missing ticketSummaryDegraded field in ${FOUR_ACCOUNT_SUMMARY_VO_FILE}" \
  "four_account_summary_degraded_field_check" || true

check_required_pattern "${FOUR_ACCOUNT_REFUND_SUMMARY_VO_FILE}" \
  'ticketSummaryDegraded' \
  "FPC012_FOUR_ACCOUNT_REFUND_SUMMARY_DEGRADED_FIELD_MISSING" \
  "missing ticketSummaryDegraded field in ${FOUR_ACCOUNT_REFUND_SUMMARY_VO_FILE}" \
  "four_account_refund_summary_degraded_field_check" || true

check_required_pattern "${FOUR_ACCOUNT_SERVICE_FILE}" \
  'long[[:space:]]+tradeMinusCommissionSplitSum[[:space:]]*=' \
  "FPC041_FOUR_ACCOUNT_SERVICE_TRADE_MINUS_COMMISSION_AGG_MISSING" \
  "missing tradeMinusCommissionSplitSum aggregate anchor in ${FOUR_ACCOUNT_SERVICE_FILE}" \
  "four_account_service_trade_minus_commission_agg_check" || true

check_required_pattern "${FOUR_ACCOUNT_SERVICE_FILE}" \
  'respVO\.setTradeMinusCommissionSplitSum\(tradeMinusCommissionSplitSum\)' \
  "FPC042_FOUR_ACCOUNT_SERVICE_TRADE_MINUS_COMMISSION_SETTER_MISSING" \
  "missing tradeMinusCommissionSplitSum setter anchor in ${FOUR_ACCOUNT_SERVICE_FILE}" \
  "four_account_service_trade_minus_commission_setter_check" || true

check_required_pattern "${FOUR_ACCOUNT_SERVICE_FILE}" \
  'respVO\.setCommissionAmountSum\(commissionAmountSum\)' \
  "FPC043_FOUR_ACCOUNT_SERVICE_COMMISSION_SETTER_MISSING" \
  "missing commissionAmountSum setter anchor in ${FOUR_ACCOUNT_SERVICE_FILE}" \
  "four_account_service_commission_setter_check" || true

check_required_pattern "${FOUR_ACCOUNT_SERVICE_FILE}" \
  'respVO\.setCommissionDifferenceAbsSum\(commissionDifferenceAbsSum\)' \
  "FPC045_FOUR_ACCOUNT_SERVICE_COMMISSION_DIFF_ABS_SETTER_MISSING" \
  "missing commissionDifferenceAbsSum setter anchor in ${FOUR_ACCOUNT_SERVICE_FILE}" \
  "four_account_service_commission_diff_abs_setter_check" || true

check_required_pattern "${FOUR_ACCOUNT_SERVICE_FILE}" \
  'long[[:space:]]+differenceAmountSum[[:space:]]*=' \
  "FPC048_FOUR_ACCOUNT_SERVICE_DIFFERENCE_AMOUNT_AGG_MISSING" \
  "missing differenceAmountSum aggregate anchor in ${FOUR_ACCOUNT_SERVICE_FILE}" \
  "four_account_service_difference_amount_agg_check" || true

check_required_pattern "${FOUR_ACCOUNT_SERVICE_FILE}" \
  'respVO\.setDifferenceAmountSum\(differenceAmountSum\)' \
  "FPC049_FOUR_ACCOUNT_SERVICE_DIFFERENCE_AMOUNT_SETTER_MISSING" \
  "missing differenceAmountSum setter anchor in ${FOUR_ACCOUNT_SERVICE_FILE}" \
  "four_account_service_difference_amount_setter_check" || true

check_required_pattern "${FOUR_ACCOUNT_SERVICE_FILE}" \
  'respVO\.setUnresolvedTicketCount\(unresolvedTicketCount\)' \
  "FPC050_FOUR_ACCOUNT_SERVICE_UNRESOLVED_TICKET_SETTER_MISSING" \
  "missing unresolvedTicketCount setter anchor in ${FOUR_ACCOUNT_SERVICE_FILE}" \
  "four_account_service_unresolved_ticket_setter_check" || true

check_required_pattern "${FOUR_ACCOUNT_SERVICE_FILE}" \
  'setTicketSummaryDegraded\(ticketLoadResult\.degraded\)' \
  "FPC013_FOUR_ACCOUNT_SERVICE_DEGRADED_SET_MISSING" \
  "missing ticketSummaryDegraded assignment anchor in ${FOUR_ACCOUNT_SERVICE_FILE}" \
  "four_account_service_degraded_set_check" || true

check_required_pattern_ci "${FOUR_ACCOUNT_SERVICE_FILE}" \
  'load ticket summary degrade' \
  "FPC014_FOUR_ACCOUNT_SERVICE_DEGRADE_LOG_MISSING" \
  "missing ticket-summary degrade log anchor in ${FOUR_ACCOUNT_SERVICE_FILE}" \
  "four_account_service_degrade_log_check" || true

check_required_pattern "${FOUR_ACCOUNT_SERVICE_FILE}" \
  'syncRefundCommissionAuditTickets[[:space:]]*\(' \
  "FPC015_FOUR_ACCOUNT_SERVICE_FAIL_OPEN_ENTRY_MISSING" \
  "missing syncRefundCommissionAuditTickets fail-open entry anchor in ${FOUR_ACCOUNT_SERVICE_FILE}" \
  "four_account_service_fail_open_entry_check" || true
check_required_pattern "${FOUR_ACCOUNT_SERVICE_FILE}" \
  'catch[[:space:]]*\([[:space:]]*Exception[[:space:]]+ex[[:space:]]*\)' \
  "FPC016_FOUR_ACCOUNT_SERVICE_FAIL_OPEN_CATCH_MISSING" \
  "missing fail-open catch(Exception ex) anchor in ${FOUR_ACCOUNT_SERVICE_FILE}" \
  "four_account_service_fail_open_catch_check" || true
check_required_pattern "${FOUR_ACCOUNT_SERVICE_FILE}" \
  'failedOrderIds\.add\(row\.getOrderId\(\)\)' \
  "FPC017_FOUR_ACCOUNT_SERVICE_FAIL_OPEN_FAILED_ORDER_CAPTURE_MISSING" \
  "missing fail-open failedOrderIds capture anchor in ${FOUR_ACCOUNT_SERVICE_FILE}" \
  "four_account_service_fail_open_failed_order_capture_check" || true

if [[ "${four_account_service_fail_open_entry_check}" == "PASS" \
  && "${four_account_service_fail_open_catch_check}" == "PASS" \
  && "${four_account_service_fail_open_failed_order_capture_check}" == "PASS" ]]; then
  four_account_service_fail_open_semantics_check="PASS"
else
  four_account_service_fail_open_semantics_check="FAIL"
fi

# WARN: 测试锚点（用于审计可追踪，不作为硬阻断）
check_warn_pattern_ci "${BOOKING_ORDER_TEST_FILE}" \
  'conflictWhenAlreadyRefundedWithDifferentPayRefundId|BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT' \
  "FPCW01_REFUND_IDEMPOTENT_TEST_ANCHOR_MISSING" \
  "refund idempotent conflict test anchor not found in ${BOOKING_ORDER_TEST_FILE}" \
  "booking_order_test_idempotent_case_check" || true

check_warn_pattern_ci "${BOOKING_REFUND_NOTIFY_LOG_SERVICE_TEST_FILE}" \
  'replayFailedLogs_shouldFailOpenWhenFourAccountRefreshThrows|syncReplayRunLogTickets_shouldFailOpenAndReturnFailedIds|replayDueFailedLogs_shouldScanAndReplay' \
  "FPCW03_REFUND_REPLAY_TEST_ANCHOR_MISSING" \
  "refund replay fail-open/degrade test anchor not found in ${BOOKING_REFUND_NOTIFY_LOG_SERVICE_TEST_FILE}" \
  "booking_refund_replay_test_anchor_check" || true

check_warn_pattern_ci "${TECHNICIAN_COMMISSION_TEST_FILE}" \
  'testCalculateCommission_duplicatePrevention|BOOKING_COMMISSION_ACCRUAL|testCancelCommission_shouldBeIdempotentForSettledReversal' \
  "FPCW04_TECHNICIAN_COMMISSION_TEST_ANCHOR_MISSING" \
  "technician commission accrual/reversal idempotent test anchor not found in ${TECHNICIAN_COMMISSION_TEST_FILE}" \
  "technician_commission_test_anchor_check" || true

check_warn_pattern_ci "${FOUR_ACCOUNT_SERVICE_TEST_FILE}" \
  'syncRefundCommissionAuditTickets_shouldUpsertAndFailOpen|shouldDegradeWhenTradeSummaryFails|shouldDegradeWhenTradeFails' \
  "FPCW02_FOUR_ACCOUNT_DEGRADE_TEST_ANCHOR_MISSING" \
  "four-account degrade/fail-open test anchor not found in ${FOUR_ACCOUNT_SERVICE_TEST_FILE}" \
  "four_account_test_fail_open_case_check" || true

{
  echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "result=${result}"
  echo "repo_root=${ROOT_DIR}"
  echo "error_code_file=${ERROR_CODE_FILE}"
  echo "booking_order_service_file=${BOOKING_ORDER_SERVICE_FILE}"
  echo "booking_refund_notify_log_controller_file=${BOOKING_REFUND_NOTIFY_LOG_CONTROLLER_FILE}"
  echo "booking_refund_notify_log_service_file=${BOOKING_REFUND_NOTIFY_LOG_SERVICE_FILE}"
  echo "technician_commission_service_file=${TECHNICIAN_COMMISSION_SERVICE_FILE}"
  echo "technician_commission_mapper_file=${TECHNICIAN_COMMISSION_MAPPER_FILE}"
  echo "reversal_sql_file=${REVERSAL_SQL_FILE}"
  echo "four_account_summary_vo_file=${FOUR_ACCOUNT_SUMMARY_VO_FILE}"
  echo "four_account_refund_summary_vo_file=${FOUR_ACCOUNT_REFUND_SUMMARY_VO_FILE}"
  echo "four_account_service_file=${FOUR_ACCOUNT_SERVICE_FILE}"
  echo "booking_order_test_file=${BOOKING_ORDER_TEST_FILE}"
  echo "booking_refund_notify_log_service_test_file=${BOOKING_REFUND_NOTIFY_LOG_SERVICE_TEST_FILE}"
  echo "technician_commission_test_file=${TECHNICIAN_COMMISSION_TEST_FILE}"
  echo "four_account_service_test_file=${FOUR_ACCOUNT_SERVICE_TEST_FILE}"
  echo "refund_idempotent_error_code_check=${refund_idempotent_error_code_check}"
  echo "refund_idempotent_throw_check=${refund_idempotent_throw_check}"
  echo "refund_idempotent_branch_check=${refund_idempotent_branch_check}"
  echo "replay_error_code_check=${replay_error_code_check}"
  echo "replay_run_id_throw_check=${replay_run_id_throw_check}"
  echo "replay_controller_replay_endpoint_check=${replay_controller_replay_endpoint_check}"
  echo "replay_controller_replay_due_endpoint_check=${replay_controller_replay_due_endpoint_check}"
  echo "replay_controller_summary_endpoint_check=${replay_controller_summary_endpoint_check}"
  echo "replay_controller_sync_tickets_endpoint_check=${replay_controller_sync_tickets_endpoint_check}"
  echo "replay_service_replay_failed_logs_check=${replay_service_replay_failed_logs_check}"
  echo "replay_service_replay_due_logs_check=${replay_service_replay_due_logs_check}"
  echo "replay_service_summary_method_check=${replay_service_summary_method_check}"
  echo "replay_service_sync_method_check=${replay_service_sync_method_check}"
  echo "replay_service_fail_open_log_check=${replay_service_fail_open_log_check}"
  echo "replay_service_degrade_log_check=${replay_service_degrade_log_check}"
  echo "replay_service_ticket_sync_warning_tag_check=${replay_service_ticket_sync_warning_tag_check}"
  echo "commission_accrual_error_code_check=${commission_accrual_error_code_check}"
  echo "commission_accrual_biz_type_check=${commission_accrual_biz_type_check}"
  echo "commission_accrual_source_biz_no_check=${commission_accrual_source_biz_no_check}"
  echo "commission_accrual_payload_consistency_check=${commission_accrual_payload_consistency_check}"
  echo "commission_accrual_mapper_biz_key_usage_check=${commission_accrual_mapper_biz_key_usage_check}"
  echo "commission_accrual_conflict_throw_check=${commission_accrual_conflict_throw_check}"
  echo "commission_reversal_error_code_check=${commission_reversal_error_code_check}"
  echo "commission_reversal_source_biz_no_check=${commission_reversal_source_biz_no_check}"
  echo "commission_reversal_duplicate_key_semantics_check=${commission_reversal_duplicate_key_semantics_check}"
  echo "commission_reversal_payload_consistency_check=${commission_reversal_payload_consistency_check}"
  echo "commission_mapper_origin_lookup_check=${commission_mapper_origin_lookup_check}"
  echo "commission_mapper_biz_key_lookup_check=${commission_mapper_biz_key_lookup_check}"
  echo "reversal_unique_sql_column_check=${reversal_unique_sql_column_check}"
  echo "reversal_unique_sql_index_check=${reversal_unique_sql_index_check}"
  echo "four_account_summary_trade_minus_commission_field_check=${four_account_summary_trade_minus_commission_field_check}"
  echo "four_account_summary_commission_amount_field_check=${four_account_summary_commission_amount_field_check}"
  echo "four_account_summary_commission_diff_abs_field_check=${four_account_summary_commission_diff_abs_field_check}"
  echo "four_account_summary_unresolved_field_check=${four_account_summary_unresolved_field_check}"
  echo "four_account_refund_summary_difference_amount_field_check=${four_account_refund_summary_difference_amount_field_check}"
  echo "four_account_refund_summary_unresolved_field_check=${four_account_refund_summary_unresolved_field_check}"
  echo "four_account_summary_degraded_field_check=${four_account_summary_degraded_field_check}"
  echo "four_account_refund_summary_degraded_field_check=${four_account_refund_summary_degraded_field_check}"
  echo "four_account_service_trade_minus_commission_agg_check=${four_account_service_trade_minus_commission_agg_check}"
  echo "four_account_service_trade_minus_commission_setter_check=${four_account_service_trade_minus_commission_setter_check}"
  echo "four_account_service_commission_setter_check=${four_account_service_commission_setter_check}"
  echo "four_account_service_commission_diff_abs_setter_check=${four_account_service_commission_diff_abs_setter_check}"
  echo "four_account_service_difference_amount_agg_check=${four_account_service_difference_amount_agg_check}"
  echo "four_account_service_difference_amount_setter_check=${four_account_service_difference_amount_setter_check}"
  echo "four_account_service_unresolved_ticket_setter_check=${four_account_service_unresolved_ticket_setter_check}"
  echo "four_account_service_degraded_set_check=${four_account_service_degraded_set_check}"
  echo "four_account_service_degrade_log_check=${four_account_service_degrade_log_check}"
  echo "four_account_service_fail_open_entry_check=${four_account_service_fail_open_entry_check}"
  echo "four_account_service_fail_open_catch_check=${four_account_service_fail_open_catch_check}"
  echo "four_account_service_fail_open_failed_order_capture_check=${four_account_service_fail_open_failed_order_capture_check}"
  echo "four_account_service_fail_open_semantics_check=${four_account_service_fail_open_semantics_check}"
  echo "booking_order_test_idempotent_case_check=${booking_order_test_idempotent_case_check}"
  echo "booking_refund_replay_test_anchor_check=${booking_refund_replay_test_anchor_check}"
  echo "technician_commission_test_anchor_check=${technician_commission_test_anchor_check}"
  echo "four_account_test_fail_open_case_check=${four_account_test_fail_open_case_check}"
  echo "output_tsv=${OUTPUT_TSV}"
} > "${SUMMARY_FILE}"

echo "== Finance Partial Closure Gate =="
echo "result=${result}"
echo "summary_file=${SUMMARY_FILE}"
echo "output_tsv=${OUTPUT_TSV}"

exit "${exit_code}"
