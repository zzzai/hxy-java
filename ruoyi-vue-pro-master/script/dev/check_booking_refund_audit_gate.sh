#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_TSV="${OUTPUT_TSV:-}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_booking_refund_audit_gate.sh [options]

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
  SUMMARY_FILE="/tmp/booking_refund_audit_gate/summary.txt"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="/tmp/booking_refund_audit_gate/result.tsv"
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

refund_notify_root_check="UNKNOWN"
refund_notify_page_check="UNKNOWN"
refund_notify_replay_check="UNKNOWN"
refund_audit_root_check="UNKNOWN"
refund_audit_summary_check="UNKNOWN"
error_code_1030004011_check="UNKNOWN"
error_code_1030004013_check="UNKNOWN"
error_code_1030004014_check="UNKNOWN"

REFUND_NOTIFY_CONTROLLER_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingRefundNotifyLogController.java"
FOUR_ACCOUNT_CONTROLLER_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/FourAccountReconcileController.java"
ERROR_CODE_FILE="${ROOT_DIR}/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/ErrorCodeConstants.java"

check_required_pattern "${REFUND_NOTIFY_CONTROLLER_FILE}" '@RequestMapping\("/booking/refund-notify-log"\)' "BRA01_REFUND_NOTIFY_ROOT_MAPPING_MISSING" "missing /booking/refund-notify-log root mapping in ${REFUND_NOTIFY_CONTROLLER_FILE}" "refund_notify_root_check" || true
check_required_pattern "${REFUND_NOTIFY_CONTROLLER_FILE}" '@GetMapping\("/page"\)' "BRA02_REFUND_NOTIFY_PAGE_ENDPOINT_MISSING" "missing /booking/refund-notify-log/page endpoint in ${REFUND_NOTIFY_CONTROLLER_FILE}" "refund_notify_page_check" || true
check_required_pattern "${REFUND_NOTIFY_CONTROLLER_FILE}" '@PostMapping\("/replay"\)' "BRA03_REFUND_NOTIFY_REPLAY_ENDPOINT_MISSING" "missing /booking/refund-notify-log/replay endpoint in ${REFUND_NOTIFY_CONTROLLER_FILE}" "refund_notify_replay_check" || true

check_required_pattern "${FOUR_ACCOUNT_CONTROLLER_FILE}" '@RequestMapping\("/booking/four-account-reconcile"\)' "BRA04_REFUND_AUDIT_ROOT_MAPPING_MISSING" "missing /booking/four-account-reconcile root mapping in ${FOUR_ACCOUNT_CONTROLLER_FILE}" "refund_audit_root_check" || true
check_required_pattern "${FOUR_ACCOUNT_CONTROLLER_FILE}" '@GetMapping\("/refund-audit-summary"\)' "BRA05_REFUND_AUDIT_SUMMARY_ENDPOINT_MISSING" "missing /booking/four-account-reconcile/refund-audit-summary endpoint in ${FOUR_ACCOUNT_CONTROLLER_FILE}" "refund_audit_summary_check" || true

check_required_pattern "${ERROR_CODE_FILE}" 'BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID[[:space:]]*=.*(1_030_004_011|1030004011)' "BRA06_ERROR_CODE_1030004011_MISSING" "missing 1030004011 refund notify invalid order id error code in ${ERROR_CODE_FILE}" "error_code_1030004011_check" || true
check_required_pattern "${ERROR_CODE_FILE}" 'BOOKING_ORDER_REFUND_NOTIFY_LOG_NOT_EXISTS[[:space:]]*=.*(1_030_004_013|1030004013)' "BRA07_ERROR_CODE_1030004013_MISSING" "missing 1030004013 refund notify log not exists error code in ${ERROR_CODE_FILE}" "error_code_1030004013_check" || true
check_required_pattern "${ERROR_CODE_FILE}" 'BOOKING_ORDER_REFUND_NOTIFY_LOG_STATUS_INVALID[[:space:]]*=.*(1_030_004_014|1030004014)' "BRA08_ERROR_CODE_1030004014_MISSING" "missing 1030004014 refund notify log status invalid error code in ${ERROR_CODE_FILE}" "error_code_1030004014_check" || true

{
  echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "result=${result}"
  echo "repo_root=${ROOT_DIR}"
  echo "refund_notify_controller_file=${REFUND_NOTIFY_CONTROLLER_FILE}"
  echo "four_account_controller_file=${FOUR_ACCOUNT_CONTROLLER_FILE}"
  echo "error_code_file=${ERROR_CODE_FILE}"
  echo "refund_notify_root_check=${refund_notify_root_check}"
  echo "refund_notify_page_check=${refund_notify_page_check}"
  echo "refund_notify_replay_check=${refund_notify_replay_check}"
  echo "refund_audit_root_check=${refund_audit_root_check}"
  echo "refund_audit_summary_check=${refund_audit_summary_check}"
  echo "error_code_1030004011_check=${error_code_1030004011_check}"
  echo "error_code_1030004013_check=${error_code_1030004013_check}"
  echo "error_code_1030004014_check=${error_code_1030004014_check}"
  echo "output_tsv=${OUTPUT_TSV}"
} > "${SUMMARY_FILE}"

echo "== Booking Refund Audit Gate =="
echo "result=${result}"
echo "summary_file=${SUMMARY_FILE}"
echo "output_tsv=${OUTPUT_TSV}"

exit "${exit_code}"
