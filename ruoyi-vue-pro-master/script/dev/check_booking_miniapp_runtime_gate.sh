#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
SUMMARY_FILE="${SUMMARY_FILE:-/tmp/booking_miniapp_runtime_gate/summary.txt}"
OUTPUT_TSV="${OUTPUT_TSV:-/tmp/booking_miniapp_runtime_gate/result.tsv}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_booking_miniapp_runtime_gate.sh [options]

Options:
  --repo-root <dir>        Repository root (default: auto detect from script path)
  --summary-file <file>    Summary output file (optional)
  --output-tsv <file>      Result TSV output file (optional)
  -h, --help               Show help

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

mkdir -p "$(dirname "${SUMMARY_FILE}")" "$(dirname "${OUTPUT_TSV}")"
echo -e "severity\tcode\tdetail" > "${OUTPUT_TSV}"

result="PASS"
exit_code=0

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

check_file_exists() {
  local file="$1"
  local code="$2"
  if [[ ! -f "${file}" ]]; then
    mark_block
    add_issue "BLOCK" "${code}" "missing file: ${file}"
    return 1
  fi
  return 0
}

check_pattern_present() {
  local file="$1"
  local pattern="$2"
  local code="$3"
  local detail="$4"
  if ! rg -q --fixed-strings "${pattern}" "${file}"; then
    mark_block
    add_issue "BLOCK" "${code}" "${detail}"
    return 1
  fi
  return 0
}

check_pattern_absent() {
  local file="$1"
  local pattern="$2"
  local code="$3"
  local detail="$4"
  if rg -q --fixed-strings "${pattern}" "${file}"; then
    mark_block
    add_issue "BLOCK" "${code}" "${detail}"
    return 1
  fi
  return 0
}

check_block_contains() {
  local file="$1"
  local start_marker="$2"
  local needle="$3"
  local code="$4"
  local detail="$5"
  local block

  block="$(
    awk -v start="${start_marker}" '
      index($0, start) { flag = 1 }
      flag { print }
      flag && $0 ~ /^  },$/ { exit }
    ' "${file}"
  )"

  if [[ -z "${block}" ]] || ! grep -Fq "${needle}" <<<"${block}"; then
    mark_block
    add_issue "BLOCK" "${code}" "${detail}"
    return 1
  fi
  return 0
}

check_block_absent() {
  local file="$1"
  local start_marker="$2"
  local needle="$3"
  local code="$4"
  local detail="$5"
  local block

  block="$(
    awk -v start="${start_marker}" '
      index($0, start) { flag = 1 }
      flag { print }
      flag && $0 ~ /^  },$/ { exit }
    ' "${file}"
  )"

  if [[ -n "${block}" ]] && grep -Fq "${needle}" <<<"${block}"; then
    mark_block
    add_issue "BLOCK" "${code}" "${detail}"
    return 1
  fi
  return 0
}

API_FILE="${ROOT_DIR}/yudao-mall-uniapp/sheep/api/trade/booking.js"
LOGIC_FILE="${ROOT_DIR}/yudao-mall-uniapp/pages/booking/logic.js"
PAGE_TEST_FILE="${ROOT_DIR}/yudao-mall-uniapp/tests/booking-page-smoke.test.mjs"
API_TEST_FILE="${ROOT_DIR}/yudao-mall-uniapp/tests/booking-api-alignment.test.mjs"
PAGES_DIR="${ROOT_DIR}/yudao-mall-uniapp/pages/booking"

check_file_exists "${API_FILE}" "BMR01_API_FILE_MISSING" || true
check_file_exists "${LOGIC_FILE}" "BMR02_LOGIC_FILE_MISSING" || true
check_file_exists "${PAGE_TEST_FILE}" "BMR03_PAGE_TEST_MISSING" || true
check_file_exists "${API_TEST_FILE}" "BMR04_API_TEST_MISSING" || true

if [[ -f "${API_FILE}" ]]; then
  check_block_contains "${API_FILE}" "  getTechnicianList:" "/booking/technician/list" "BMR05_TECHNICIAN_LIST_PATH_MISSING" "missing canonical technician list path in ${API_FILE}" || true
  check_block_contains "${API_FILE}" "  getTechnicianList:" "method: 'GET'" "BMR06_TECHNICIAN_LIST_METHOD_MISSING" "missing GET technician list method in ${API_FILE}" || true
  check_block_contains "${API_FILE}" "  getTimeSlots:" "/booking/slot/list-by-technician" "BMR07_SLOT_LIST_PATH_MISSING" "missing canonical slot list path in ${API_FILE}" || true
  check_block_contains "${API_FILE}" "  getTimeSlots:" "method: 'GET'" "BMR08_SLOT_LIST_METHOD_MISSING" "missing GET slot list method in ${API_FILE}" || true
  check_block_contains "${API_FILE}" "  cancelOrder:" "/booking/order/cancel" "BMR09_CANCEL_PATH_MISSING" "missing canonical cancel path in ${API_FILE}" || true
  check_block_contains "${API_FILE}" "  cancelOrder:" "method: 'POST'" "BMR10_CANCEL_METHOD_MISSING" "missing POST cancel method in ${API_FILE}" || true
  check_block_contains "${API_FILE}" "  cancelOrder:" "reason: cancelReason" "BMR11_CANCEL_REASON_MAPPING_MISSING" "missing reason param mapping in ${API_FILE}" || true
  check_block_contains "${API_FILE}" "  createAddonOrder:" "/app-api/booking/addon/create" "BMR12_ADDON_PATH_MISSING" "missing canonical addon path in ${API_FILE}" || true
  check_block_contains "${API_FILE}" "  createAddonOrder:" "method: 'POST'" "BMR13_ADDON_METHOD_MISSING" "missing POST addon method in ${API_FILE}" || true

  check_pattern_absent "${API_FILE}" "/booking/technician/list-by-store" "BMR14_OLD_TECHNICIAN_PATH_PRESENT" "old technician list path still present in ${API_FILE}" || true
  check_pattern_absent "${API_FILE}" "/booking/time-slot/list" "BMR15_OLD_SLOT_PATH_PRESENT" "old slot list path still present in ${API_FILE}" || true
  check_block_absent "${API_FILE}" "  cancelOrder:" "method: 'PUT'" "BMR16_OLD_CANCEL_METHOD_PRESENT" "old cancel PUT method still present in ${API_FILE}" || true
  check_block_absent "${API_FILE}" "  createAddonOrder:" "url: '/booking/addon/create'" "BMR17_OLD_ADDON_PATH_PRESENT" "old addon path still present in ${API_FILE}" || true
fi

if [[ -d "${PAGES_DIR}" ]]; then
  check_pattern_absent "${PAGES_DIR}" "BookingApi.getTechnicianList(" "BMR18_DIRECT_TECHNICIAN_LIST_CALL_PRESENT" "booking pages still call BookingApi.getTechnicianList directly" || true
  check_pattern_absent "${PAGES_DIR}" "BookingApi.getTechnician(" "BMR19_DIRECT_TECHNICIAN_DETAIL_CALL_PRESENT" "booking pages still call BookingApi.getTechnician directly" || true
  check_pattern_absent "${PAGES_DIR}" "BookingApi.getTimeSlots(" "BMR20_DIRECT_SLOT_CALL_PRESENT" "booking pages still call BookingApi.getTimeSlots directly" || true
  check_pattern_absent "${PAGES_DIR}" "BookingApi.createOrder(" "BMR21_DIRECT_CREATE_CALL_PRESENT" "booking pages still call BookingApi.createOrder directly" || true
  check_pattern_absent "${PAGES_DIR}" "BookingApi.cancelOrder(" "BMR22_DIRECT_CANCEL_CALL_PRESENT" "booking pages still call BookingApi.cancelOrder directly" || true
  check_pattern_absent "${PAGES_DIR}" "BookingApi.createAddonOrder(" "BMR23_DIRECT_ADDON_CALL_PRESENT" "booking pages still call BookingApi.createAddonOrder directly" || true

  check_pattern_present "${ROOT_DIR}/yudao-mall-uniapp/pages/booking/technician-list.vue" "loadTechnicianList" "BMR24_TECHNICIAN_LIST_HELPER_MISSING" "technician-list.vue is not wired to loadTechnicianList" || true
  check_pattern_present "${ROOT_DIR}/yudao-mall-uniapp/pages/booking/technician-list.vue" "goToTechnicianDetail" "BMR25_TECHNICIAN_DETAIL_NAV_HELPER_MISSING" "technician-list.vue is not wired to goToTechnicianDetail" || true
  check_pattern_present "${ROOT_DIR}/yudao-mall-uniapp/pages/booking/technician-detail.vue" "loadTechnicianDetail" "BMR26_TECHNICIAN_DETAIL_HELPER_MISSING" "technician-detail.vue is not wired to loadTechnicianDetail" || true
  check_pattern_present "${ROOT_DIR}/yudao-mall-uniapp/pages/booking/technician-detail.vue" "loadTimeSlots" "BMR27_SLOT_HELPER_MISSING" "technician-detail.vue is not wired to loadTimeSlots" || true
  check_pattern_present "${ROOT_DIR}/yudao-mall-uniapp/pages/booking/technician-detail.vue" "goToOrderConfirm" "BMR28_ORDER_CONFIRM_NAV_HELPER_MISSING" "technician-detail.vue is not wired to goToOrderConfirm" || true
  check_pattern_present "${ROOT_DIR}/yudao-mall-uniapp/pages/booking/order-confirm.vue" "submitBookingOrderAndGo" "BMR29_CREATE_HELPER_MISSING" "order-confirm.vue is not wired to submitBookingOrderAndGo" || true
  check_pattern_present "${ROOT_DIR}/yudao-mall-uniapp/pages/booking/order-list.vue" "cancelBookingOrderAndRefresh" "BMR30_LIST_CANCEL_HELPER_MISSING" "order-list.vue is not wired to cancelBookingOrderAndRefresh" || true
  check_pattern_present "${ROOT_DIR}/yudao-mall-uniapp/pages/booking/order-list.vue" "goToOrderDetail" "BMR31_LIST_DETAIL_NAV_HELPER_MISSING" "order-list.vue is not wired to goToOrderDetail" || true
  check_pattern_present "${ROOT_DIR}/yudao-mall-uniapp/pages/booking/order-detail.vue" "cancelBookingOrderAndRefresh" "BMR32_DETAIL_CANCEL_HELPER_MISSING" "order-detail.vue is not wired to cancelBookingOrderAndRefresh" || true
  check_pattern_present "${ROOT_DIR}/yudao-mall-uniapp/pages/booking/addon.vue" "submitAddonOrderAndGo" "BMR33_ADDON_HELPER_MISSING" "addon.vue is not wired to submitAddonOrderAndGo" || true
fi

if [[ -f "${PAGE_TEST_FILE}" ]]; then
  check_pattern_present "${PAGE_TEST_FILE}" "does not jump to order detail on failure" "BMR34_FAILURE_CREATE_TEST_MISSING" "booking page smoke test missing create failure assertion" || true
  check_pattern_present "${PAGE_TEST_FILE}" "does not refresh on failure" "BMR35_FAILURE_CANCEL_TEST_MISSING" "booking page smoke test missing cancel failure assertion" || true
  check_pattern_present "${PAGE_TEST_FILE}" "addon helper does not jump to order detail on failure" "BMR36_FAILURE_ADDON_TEST_MISSING" "booking page smoke test missing addon failure assertion" || true
fi

if [[ -f "${API_TEST_FILE}" ]]; then
  check_pattern_present "${API_TEST_FILE}" "/booking/slot/list-by-technician" "BMR37_API_SLOT_ALIGNMENT_TEST_MISSING" "booking api alignment test missing slot canonical assertion" || true
  check_pattern_present "${API_TEST_FILE}" "/app-api/booking/addon/create" "BMR38_API_ADDON_ALIGNMENT_TEST_MISSING" "booking api alignment test missing addon canonical assertion" || true
fi

{
  echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "domain=booking"
  echo "doc_closed=YES"
  echo "can_develop=YES"
  echo "can_release=NO"
  echo "result=${result}"
  echo "repo_root=${ROOT_DIR}"
  echo "api_file=${API_FILE}"
  echo "logic_file=${LOGIC_FILE}"
  echo "page_test_file=${PAGE_TEST_FILE}"
  echo "api_test_file=${API_TEST_FILE}"
  echo "output_tsv=${OUTPUT_TSV}"
} > "${SUMMARY_FILE}"

echo "== Booking Miniapp Runtime Gate =="
echo "domain=booking doc_closed=YES can_develop=YES can_release=NO result=${result}"
echo "summary_file=${SUMMARY_FILE}"
echo "output_tsv=${OUTPUT_TSV}"

if [[ "${result}" == "BLOCK" ]]; then
  echo "block_reasons:"
  tail -n +2 "${OUTPUT_TSV}" | while IFS=$'\t' read -r severity code detail; do
    echo "  - ${code}: ${detail}"
  done
fi

exit "${exit_code}"
