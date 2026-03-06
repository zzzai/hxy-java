#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_TSV="${OUTPUT_TSV:-}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_booking_refund_notify_gate.sh [options]

Options:
  --repo-root <dir>        Repository root (default: auto detect from script path)
  --summary-file <file>    Summary output file (optional)
  --output-tsv <file>      Result TSV output file (optional)
  -h, --help               Show help

Exit Code:
  0: PASS / PASS_WITH_WARN
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
  SUMMARY_FILE="/tmp/booking_refund_notify_gate/summary.txt"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="/tmp/booking_refund_notify_gate/result.tsv"
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
    result="PASS_WITH_WARN"
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

result="PASS"
exit_code=0

migration_table_check="UNKNOWN"
migration_pay_refund_id_check="UNKNOWN"
migration_refund_time_check="UNKNOWN"
error_code_invalid_id_check="UNKNOWN"
error_code_idempotent_check="UNKNOWN"
controller_mapping_check="UNKNOWN"
controller_endpoint_check="UNKNOWN"
service_refunded_branch_check="UNKNOWN"
service_idempotent_throw_check="UNKNOWN"
service_idempotent_match_check="UNKNOWN"

MIGRATION_FILE="${ROOT_DIR}/sql/mysql/hxy/2026-03-06-hxy-booking-order-refund-notify-audit.sql"
ERROR_CODE_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/ErrorCodeConstants.java"
CONTROLLER_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppBookingOrderController.java"
SERVICE_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingOrderServiceImpl.java"

if [[ ! -f "${MIGRATION_FILE}" ]]; then
  migration_table_check="FAIL"
  migration_pay_refund_id_check="FAIL"
  migration_refund_time_check="FAIL"
  mark_block
  add_issue "BLOCK" "BR01_MIGRATION_FILE_MISSING" "missing migration file: ${MIGRATION_FILE}"
else
  check_required_pattern "${MIGRATION_FILE}" 'ALTER TABLE[[:space:]]+[`]?booking_order[`]?' "BR02_MIGRATION_BOOKING_ORDER_MISSING" "missing booking_order alter statement in ${MIGRATION_FILE}" "migration_table_check" || true
  check_required_pattern "${MIGRATION_FILE}" 'ADD COLUMN IF NOT EXISTS[[:space:]]+[`]pay_refund_id[`]' "BR03_MIGRATION_PAY_REFUND_ID_MISSING" "missing pay_refund_id column migration in ${MIGRATION_FILE}" "migration_pay_refund_id_check" || true
  check_required_pattern "${MIGRATION_FILE}" 'ADD COLUMN IF NOT EXISTS[[:space:]]+[`]refund_time[`]' "BR04_MIGRATION_REFUND_TIME_MISSING" "missing refund_time column migration in ${MIGRATION_FILE}" "migration_refund_time_check" || true
fi

check_required_pattern "${ERROR_CODE_FILE}" "BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID" "BR05_ERROR_CODE_INVALID_ID_MISSING" "missing BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID in ${ERROR_CODE_FILE}" "error_code_invalid_id_check" || true
check_required_pattern "${ERROR_CODE_FILE}" "BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT" "BR06_ERROR_CODE_IDEMPOTENT_CONFLICT_MISSING" "missing BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT in ${ERROR_CODE_FILE}" "error_code_idempotent_check" || true

check_required_pattern "${CONTROLLER_FILE}" "@RequestMapping\\(\"/booking/order\"\\)" "BR07_CONTROLLER_ROOT_MAPPING_MISSING" "missing /booking/order root mapping in ${CONTROLLER_FILE}" "controller_mapping_check" || true
check_required_pattern "${CONTROLLER_FILE}" "@PostMapping\\(\"/update-refunded\"\\)" "BR08_CONTROLLER_UPDATE_REFUNDED_MAPPING_MISSING" "missing /booking/order/update-refunded endpoint in ${CONTROLLER_FILE}" "controller_endpoint_check" || true

check_required_pattern "${SERVICE_FILE}" "BookingOrderStatusEnum\\.REFUNDED\\.getStatus\\(\\)\\.equals\\(order\\.getStatus\\(\\)\\)" "BR09_SERVICE_REFUNDED_BRANCH_MISSING" "missing refunded status branch in ${SERVICE_FILE}" "service_refunded_branch_check" || true
check_required_pattern "${SERVICE_FILE}" "throw[[:space:]]+exception\\(BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT\\)" "BR10_SERVICE_IDEMPOTENT_THROW_MISSING" "missing BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT throw in ${SERVICE_FILE}" "service_idempotent_throw_check" || true
check_required_pattern "${SERVICE_FILE}" "order\\.getPayRefundId\\(\\)[[:space:]]*!=[[:space:]]*null[[:space:]]*&&[[:space:]]*order\\.getPayRefundId\\(\\)\\.equals\\(payRefundId\\)" "BR11_SERVICE_IDEMPOTENT_MATCH_BRANCH_MISSING" "missing same refund id idempotent branch in ${SERVICE_FILE}" "service_idempotent_match_check" || true

if [[ "${result}" == "PASS" ]]; then
  if [[ ! -f "${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingOrderServiceImplTest.java" ]]; then
    mark_warn
    add_issue "WARN" "BR12_BOOKING_SERVICE_TEST_MISSING" "BookingOrderServiceImplTest not found"
  fi
  if [[ ! -f "${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/app/AppBookingOrderControllerTest.java" ]]; then
    mark_warn
    add_issue "WARN" "BR13_APP_BOOKING_CONTROLLER_TEST_MISSING" "AppBookingOrderControllerTest not found"
  fi
  if [[ ! -f "${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/FourAccountReconcileServiceImplTest.java" ]]; then
    mark_warn
    add_issue "WARN" "BR14_FOUR_ACCOUNT_SERVICE_TEST_MISSING" "FourAccountReconcileServiceImplTest not found"
  fi
fi

{
  echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "result=${result}"
  echo "repo_root=${ROOT_DIR}"
  echo "migration_file=${MIGRATION_FILE}"
  echo "error_code_file=${ERROR_CODE_FILE}"
  echo "controller_file=${CONTROLLER_FILE}"
  echo "service_file=${SERVICE_FILE}"
  echo "migration_table_check=${migration_table_check}"
  echo "migration_pay_refund_id_check=${migration_pay_refund_id_check}"
  echo "migration_refund_time_check=${migration_refund_time_check}"
  echo "error_code_invalid_id_check=${error_code_invalid_id_check}"
  echo "error_code_idempotent_check=${error_code_idempotent_check}"
  echo "controller_mapping_check=${controller_mapping_check}"
  echo "controller_endpoint_check=${controller_endpoint_check}"
  echo "service_refunded_branch_check=${service_refunded_branch_check}"
  echo "service_idempotent_throw_check=${service_idempotent_throw_check}"
  echo "service_idempotent_match_check=${service_idempotent_match_check}"
  echo "output_tsv=${OUTPUT_TSV}"
} > "${SUMMARY_FILE}"

echo "== Booking Refund Notify Gate =="
echo "result=${result}"
echo "summary_file=${SUMMARY_FILE}"
echo "output_tsv=${OUTPUT_TSV}"

exit "${exit_code}"
