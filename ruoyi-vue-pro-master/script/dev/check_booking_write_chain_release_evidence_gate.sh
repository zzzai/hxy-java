#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SAMPLE_PACK_DIR=""
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_TSV="${OUTPUT_TSV:-}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_booking_write_chain_release_evidence_gate.sh [options]

Options:
  --repo-root <dir>         Repository root (default: auto-detect from script path)
  --sample-pack-dir <dir>   Simulated evidence pack directory
  --summary-file <file>     Summary output file (optional)
  --output-tsv <file>       Result TSV output file (optional)
  -h, --help                Show help

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
    --sample-pack-dir)
      SAMPLE_PACK_DIR="$2"
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

if [[ -z "${SAMPLE_PACK_DIR}" ]]; then
  SAMPLE_PACK_DIR="${ROOT_DIR}/tests/fixtures/booking-write-chain-release-evidence-simulated"
fi
if [[ -z "${SUMMARY_FILE}" ]]; then
  SUMMARY_FILE="/tmp/booking_write_chain_release_evidence_gate/summary.txt"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="/tmp/booking_write_chain_release_evidence_gate/result.tsv"
fi

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

require_file() {
  local file="$1"
  local code="$2"
  if [[ ! -f "${file}" ]]; then
    mark_block
    add_issue "BLOCK" "${code}" "missing file: ${file}"
    return 1
  fi
  return 0
}

require_text_contains() {
  local file="$1"
  local needle="$2"
  local code="$3"
  if ! grep -Fq "${needle}" "${file}"; then
    mark_block
    add_issue "BLOCK" "${code}" "missing text '${needle}' in ${file}"
    return 1
  fi
  return 0
}

json_get() {
  local file="$1"
  local field="$2"
  node -e '
const fs = require("fs");
const file = process.argv[1];
const field = process.argv[2];
const obj = JSON.parse(fs.readFileSync(file, "utf8"));
const value = field.split(".").reduce((acc, key) => {
  if (acc === undefined || acc === null) return undefined;
  if (Array.isArray(acc) && /^\d+$/.test(key)) return acc[Number(key)];
  return Object.prototype.hasOwnProperty.call(acc, key) ? acc[key] : undefined;
}, obj);
if (value === undefined) process.exit(1);
if (typeof value === "object") {
  process.stdout.write(JSON.stringify(value));
} else {
  process.stdout.write(String(value));
}
' "$file" "$field"
}

require_json_equals() {
  local file="$1"
  local field="$2"
  local expected="$3"
  local code="$4"
  local actual
  if ! actual="$(json_get "$file" "$field" 2>/dev/null)"; then
    mark_block
    add_issue "BLOCK" "${code}" "missing json field ${field} in ${file}"
    return 1
  fi
  if [[ "${actual}" != "${expected}" ]]; then
    mark_block
    add_issue "BLOCK" "${code}" "json field ${field} expected ${expected} but got ${actual} in ${file}"
    return 1
  fi
}

require_json_truthy() {
  local file="$1"
  local field="$2"
  local code="$3"
  local actual
  if ! actual="$(json_get "$file" "$field" 2>/dev/null)"; then
    mark_block
    add_issue "BLOCK" "${code}" "missing json field ${field} in ${file}"
    return 1
  fi
  if [[ -z "${actual}" || "${actual}" == "0" || "${actual}" == "null" || "${actual}" == "[]" || "${actual}" == "{}" ]]; then
    mark_block
    add_issue "BLOCK" "${code}" "json field ${field} is empty in ${file}"
    return 1
  fi
}

tech_file="${SAMPLE_PACK_DIR}/technician-list-success.json"
slot_file="${SAMPLE_PACK_DIR}/slot-list-success.json"
create_success_file="${SAMPLE_PACK_DIR}/create-success.json"
create_conflict_file="${SAMPLE_PACK_DIR}/create-conflict.json"
cancel_success_file="${SAMPLE_PACK_DIR}/cancel-success.json"
addon_success_file="${SAMPLE_PACK_DIR}/addon-success.json"
addon_conflict_file="${SAMPLE_PACK_DIR}/addon-conflict.json"
gray_file="${SAMPLE_PACK_DIR}/gray-stage.json"
rollback_file="${SAMPLE_PACK_DIR}/rollback-drill.json"
signoff_file="${SAMPLE_PACK_DIR}/signoff.json"
ledger_doc="${ROOT_DIR}/docs/plans/2026-03-24-miniapp-booking-write-chain-release-evidence-ledger-v1.md"
package_review_doc="${ROOT_DIR}/docs/products/miniapp/2026-03-24-miniapp-booking-write-chain-release-package-review-v1.md"

require_file "${tech_file}" "BWC01_TECH_SAMPLE_MISSING" || true
require_file "${slot_file}" "BWC02_SLOT_SAMPLE_MISSING" || true
require_file "${create_success_file}" "BWC03_CREATE_SUCCESS_SAMPLE_MISSING" || true
require_file "${create_conflict_file}" "BWC04_CREATE_CONFLICT_SAMPLE_MISSING" || true
require_file "${cancel_success_file}" "BWC05_CANCEL_SUCCESS_SAMPLE_MISSING" || true
require_file "${addon_success_file}" "BWC06_ADDON_SUCCESS_SAMPLE_MISSING" || true
require_file "${addon_conflict_file}" "BWC07_ADDON_CONFLICT_SAMPLE_MISSING" || true
require_file "${gray_file}" "BWC08_GRAY_SAMPLE_MISSING" || true
require_file "${rollback_file}" "BWC09_ROLLBACK_SAMPLE_MISSING" || true
require_file "${signoff_file}" "BWC10_SIGNOFF_SAMPLE_MISSING" || true
require_file "${ledger_doc}" "BWC10A_LEDGER_DOC_MISSING" || true
require_file "${package_review_doc}" "BWC10B_PACKAGE_REVIEW_DOC_MISSING" || true

require_json_equals "${tech_file}" "request.method" "GET" "BWC11_TECH_METHOD_INVALID" || true
require_json_equals "${tech_file}" "request.path" "/booking/technician/list" "BWC12_TECH_PATH_INVALID" || true
require_json_truthy "${tech_file}" "request.query.storeId" "BWC13_TECH_STORE_ID_MISSING" || true
require_json_equals "${tech_file}" "response.commonResult.code" "0" "BWC14_TECH_CODE_INVALID" || true
require_json_truthy "${tech_file}" "audit.runId" "BWC15_TECH_RUN_ID_MISSING" || true

require_json_equals "${slot_file}" "request.method" "GET" "BWC21_SLOT_METHOD_INVALID" || true
require_json_equals "${slot_file}" "request.path" "/booking/slot/list-by-technician" "BWC22_SLOT_PATH_INVALID" || true
require_json_truthy "${slot_file}" "request.query.technicianId" "BWC23_SLOT_TECHNICIAN_ID_MISSING" || true
require_json_truthy "${slot_file}" "request.query.date" "BWC24_SLOT_DATE_MISSING" || true
require_json_equals "${slot_file}" "response.commonResult.code" "0" "BWC25_SLOT_CODE_INVALID" || true

require_json_equals "${create_success_file}" "request.method" "POST" "BWC31_CREATE_SUCCESS_METHOD_INVALID" || true
require_json_equals "${create_success_file}" "request.path" "/booking/order/create" "BWC32_CREATE_SUCCESS_PATH_INVALID" || true
require_json_equals "${create_success_file}" "request.body.dispatchMode" "1" "BWC33_CREATE_DISPATCH_MODE_INVALID" || true
require_json_equals "${create_success_file}" "response.commonResult.code" "0" "BWC34_CREATE_SUCCESS_CODE_INVALID" || true
require_json_equals "${create_success_file}" "helperOutcome.route" "/pages/booking/order-detail" "BWC35_CREATE_SUCCESS_ROUTE_INVALID" || true
require_json_equals "${create_success_file}" "audit.judgement.primary" "CODE_EQ_0" "BWC36_CREATE_SUCCESS_JUDGEMENT_INVALID" || true

require_json_equals "${create_conflict_file}" "request.method" "POST" "BWC41_CREATE_CONFLICT_METHOD_INVALID" || true
require_json_equals "${create_conflict_file}" "request.path" "/booking/order/create" "BWC42_CREATE_CONFLICT_PATH_INVALID" || true
require_json_equals "${create_conflict_file}" "response.commonResult.code" "1030003001" "BWC43_CREATE_CONFLICT_CODE_INVALID" || true
require_json_equals "${create_conflict_file}" "helperOutcome.routerStayedOnPage" "true" "BWC44_CREATE_CONFLICT_STAY_INVALID" || true
require_json_equals "${create_conflict_file}" "audit.judgement.primary" "ERROR_CODE_ONLY" "BWC45_CREATE_CONFLICT_JUDGEMENT_INVALID" || true
require_json_equals "${create_conflict_file}" "audit.judgement.messageIgnored" "true" "BWC46_CREATE_CONFLICT_MESSAGE_BRANCH_INVALID" || true

require_json_equals "${cancel_success_file}" "request.method" "POST" "BWC51_CANCEL_METHOD_INVALID" || true
require_json_equals "${cancel_success_file}" "request.path" "/booking/order/cancel" "BWC52_CANCEL_PATH_INVALID" || true
require_json_truthy "${cancel_success_file}" "request.query.id" "BWC53_CANCEL_ID_MISSING" || true
require_json_equals "${cancel_success_file}" "response.commonResult.code" "0" "BWC54_CANCEL_CODE_INVALID" || true
require_json_equals "${cancel_success_file}" "readback.postStatus" "CANCELLED" "BWC55_CANCEL_STATUS_INVALID" || true
require_json_equals "${cancel_success_file}" "readback.refreshed" "true" "BWC56_CANCEL_REFRESH_INVALID" || true

require_json_equals "${addon_success_file}" "request.method" "POST" "BWC61_ADDON_SUCCESS_METHOD_INVALID" || true
require_json_equals "${addon_success_file}" "request.path" "/app-api/booking/addon/create" "BWC62_ADDON_SUCCESS_PATH_INVALID" || true
require_json_truthy "${addon_success_file}" "request.body.parentOrderId" "BWC63_ADDON_SUCCESS_PARENT_ORDER_MISSING" || true
require_json_equals "${addon_success_file}" "response.commonResult.code" "0" "BWC64_ADDON_SUCCESS_CODE_INVALID" || true
require_json_equals "${addon_success_file}" "helperOutcome.route" "/pages/booking/order-detail" "BWC65_ADDON_SUCCESS_ROUTE_INVALID" || true

require_json_equals "${addon_conflict_file}" "request.method" "POST" "BWC71_ADDON_CONFLICT_METHOD_INVALID" || true
require_json_equals "${addon_conflict_file}" "request.path" "/app-api/booking/addon/create" "BWC72_ADDON_CONFLICT_PATH_INVALID" || true
require_json_equals "${addon_conflict_file}" "response.commonResult.code" "1030004001" "BWC73_ADDON_CONFLICT_CODE_INVALID" || true
require_json_equals "${addon_conflict_file}" "helperOutcome.routerStayedOnPage" "true" "BWC74_ADDON_CONFLICT_STAY_INVALID" || true
require_json_equals "${addon_conflict_file}" "audit.judgement.primary" "ERROR_CODE_ONLY" "BWC75_ADDON_CONFLICT_JUDGEMENT_INVALID" || true
require_json_equals "${addon_conflict_file}" "audit.judgement.messageIgnored" "true" "BWC76_ADDON_CONFLICT_MESSAGE_BRANCH_INVALID" || true

require_json_equals "${gray_file}" "sampleMode" "SIMULATED_SELFTEST" "BWC81_GRAY_MODE_INVALID" || true
require_json_equals "${gray_file}" "result" "PASS" "BWC82_GRAY_RESULT_INVALID" || true
require_json_equals "${gray_file}" "decision" "NO_RELEASE" "BWC83_GRAY_DECISION_INVALID" || true
require_json_truthy "${gray_file}" "runId" "BWC84_GRAY_RUN_ID_MISSING" || true

require_json_equals "${rollback_file}" "sampleMode" "SIMULATED_SELFTEST" "BWC91_ROLLBACK_MODE_INVALID" || true
require_json_equals "${rollback_file}" "rollbackMode" "QUERY_ONLY" "BWC92_ROLLBACK_SCOPE_INVALID" || true
require_json_equals "${rollback_file}" "result" "PASS" "BWC93_ROLLBACK_RESULT_INVALID" || true
require_json_truthy "${rollback_file}" "owner" "BWC94_ROLLBACK_OWNER_MISSING" || true

require_json_equals "${signoff_file}" "sampleMode" "SIMULATED_SELFTEST" "BWC101_SIGNOFF_MODE_INVALID" || true
require_json_equals "${signoff_file}" "decision" "SELFTEST_ONLY_NO_RELEASE" "BWC102_SIGNOFF_DECISION_INVALID" || true
require_json_truthy "${signoff_file}" "owners.product" "BWC103_SIGNOFF_PRODUCT_OWNER_MISSING" || true
require_json_truthy "${signoff_file}" "owners.tech" "BWC104_SIGNOFF_TECH_OWNER_MISSING" || true
require_json_truthy "${signoff_file}" "owners.ops" "BWC105_SIGNOFF_OPS_OWNER_MISSING" || true
require_text_contains "${ledger_doc}" "Cannot Release / No-Go" "BWC106_LEDGER_NO_GO_MISSING" || true
require_text_contains "${ledger_doc}" "selftest pack" "BWC107_LEDGER_SELFTEST_BOUNDARY_MISSING" || true
require_text_contains "${package_review_doc}" "Cannot Release" "BWC108_PACKAGE_REVIEW_CANNOT_RELEASE_MISSING" || true
require_text_contains "${package_review_doc}" "No-Go" "BWC109_PACKAGE_REVIEW_NO_GO_MISSING" || true
require_text_contains "${package_review_doc}" "selftest pack 不能替代真实发布证据" "BWC110_PACKAGE_REVIEW_SELFTEST_BOUNDARY_MISSING" || true

add_issue "INFO" "BWC000_SELFTEST_PACK_PRESENT" "simulated booking write-chain release evidence pack validated"
add_issue "INFO" "BWC000A_RELEASE_DOCS_PRESENT" "booking write-chain release package docs validated"

cat > "${SUMMARY_FILE}" <<EOF_SUMMARY
# Booking Write-Chain Release Evidence Gate
result=${result}
sample_pack_dir=${SAMPLE_PACK_DIR}
summary_scope=simulated_selftest_only
doc_truth=booking_release_package_docs_validated
release_decision=NO_RELEASE
EOF_SUMMARY

echo "[booking-write-chain-evidence-gate] result=${result}"
echo "[booking-write-chain-evidence-gate] sample_pack_dir=${SAMPLE_PACK_DIR}"
echo "[booking-write-chain-evidence-gate] summary=${SUMMARY_FILE}"
echo "[booking-write-chain-evidence-gate] output_tsv=${OUTPUT_TSV}"

if [[ "${result}" == "BLOCK" ]]; then
  exit 2
fi
