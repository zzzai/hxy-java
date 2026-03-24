#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SAMPLE_PACK_DIR=""
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_TSV="${OUTPUT_TSV:-}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_bo004_admin_release_evidence_gate.sh [options]

Options:
  --repo-root <dir>         Repository root (default: auto-detect from script path)
  --sample-pack-dir <dir>   Simulated evidence pack directory
  --summary-file <file>     Summary output file (optional)
  --output-tsv <file>       Result TSV output file (optional)
  -h, --help                Show help

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
  SAMPLE_PACK_DIR="${ROOT_DIR}/tests/fixtures/bo004-admin-release-evidence-simulated"
fi
if [[ -z "${SUMMARY_FILE}" ]]; then
  SUMMARY_FILE="/tmp/bo004_admin_release_evidence_gate/summary.txt"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="/tmp/bo004_admin_release_evidence_gate/result.tsv"
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

mark_warn() {
  if [[ "${result}" == "PASS" ]]; then
    result="WARN"
  fi
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

menu_file="${SAMPLE_PACK_DIR}/menu-navigation.json"
query_file="${SAMPLE_PACK_DIR}/query-list-by-technician.json"
settle_file="${SAMPLE_PACK_DIR}/write-settle-readback.json"
config_file="${SAMPLE_PACK_DIR}/config-save-readback.json"
gray_file="${SAMPLE_PACK_DIR}/gray-stage.json"
rollback_file="${SAMPLE_PACK_DIR}/rollback-drill.json"
signoff_file="${SAMPLE_PACK_DIR}/signoff.json"

require_file "${menu_file}" "BO004_E01_MENU_SAMPLE_MISSING" || true
require_file "${query_file}" "BO004_E02_QUERY_SAMPLE_MISSING" || true
require_file "${settle_file}" "BO004_E03_SETTLE_SAMPLE_MISSING" || true
require_file "${config_file}" "BO004_E04_CONFIG_SAMPLE_MISSING" || true
require_file "${gray_file}" "BO004_E05_GRAY_SAMPLE_MISSING" || true
require_file "${rollback_file}" "BO004_E06_ROLLBACK_SAMPLE_MISSING" || true
require_file "${signoff_file}" "BO004_E07_SIGNOFF_SAMPLE_MISSING" || true

require_json_equals "${menu_file}" "capabilityId" "BO-004" "BO004_E11_MENU_CAPABILITY_ID_INVALID" || true
require_json_equals "${menu_file}" "sampleMode" "SIMULATED_SELFTEST" "BO004_E12_MENU_MODE_INVALID" || true
require_json_equals "${menu_file}" "componentPath" "mall/booking/commission/index" "BO004_E13_MENU_COMPONENT_INVALID" || true
require_json_truthy "${menu_file}" "menuSqlFile" "BO004_E14_MENU_SQL_MISSING" || true

require_json_equals "${query_file}" "request.method" "GET" "BO004_E21_QUERY_METHOD_INVALID" || true
require_json_equals "${query_file}" "request.path" "/booking/commission/list-by-technician" "BO004_E22_QUERY_PATH_INVALID" || true
require_json_truthy "${query_file}" "request.query.technicianId" "BO004_E23_QUERY_TECHNICIAN_ID_MISSING" || true
require_json_equals "${query_file}" "response.commonResult.code" "0" "BO004_E24_QUERY_RESPONSE_CODE_INVALID" || true
require_json_truthy "${query_file}" "audit.runId" "BO004_E25_QUERY_RUN_ID_MISSING" || true

require_json_equals "${settle_file}" "request.method" "POST" "BO004_E31_SETTLE_METHOD_INVALID" || true
require_json_equals "${settle_file}" "request.path" "/booking/commission/settle" "BO004_E32_SETTLE_PATH_INVALID" || true
require_json_truthy "${settle_file}" "request.query.commissionId" "BO004_E33_SETTLE_COMMISSION_ID_MISSING" || true
require_json_equals "${settle_file}" "readback.postStatus" "SETTLED" "BO004_E34_SETTLE_POST_STATUS_INVALID" || true
require_json_equals "${settle_file}" "readback.pseudoSuccessFlag" "false" "BO004_E35_SETTLE_PSEUDO_SUCCESS_FLAG_INVALID" || true
require_json_truthy "${settle_file}" "readback.postSettlementTime" "BO004_E36_SETTLE_POST_SETTLEMENT_TIME_MISSING" || true

require_json_equals "${config_file}" "request.method" "POST" "BO004_E41_CONFIG_METHOD_INVALID" || true
require_json_equals "${config_file}" "request.path" "/booking/commission/config/save" "BO004_E42_CONFIG_PATH_INVALID" || true
require_json_truthy "${config_file}" "request.body.storeId" "BO004_E43_CONFIG_STORE_ID_MISSING" || true
require_json_equals "${config_file}" "readback.postConfigCount" "1" "BO004_E44_CONFIG_POST_COUNT_INVALID" || true
require_json_truthy "${config_file}" "audit.runId" "BO004_E45_CONFIG_RUN_ID_MISSING" || true

require_json_equals "${gray_file}" "result" "PASS" "BO004_E51_GRAY_RESULT_INVALID" || true
require_json_equals "${gray_file}" "sampleMode" "SIMULATED_SELFTEST" "BO004_E52_GRAY_MODE_INVALID" || true
require_json_truthy "${gray_file}" "runId" "BO004_E53_GRAY_RUN_ID_MISSING" || true

require_json_equals "${rollback_file}" "rollbackMode" "QUERY_ONLY" "BO004_E61_ROLLBACK_MODE_INVALID" || true
require_json_equals "${rollback_file}" "result" "PASS" "BO004_E62_ROLLBACK_RESULT_INVALID" || true
require_json_truthy "${rollback_file}" "owner" "BO004_E63_ROLLBACK_OWNER_MISSING" || true

require_json_equals "${signoff_file}" "decision" "SELFTEST_ONLY_NO_RELEASE" "BO004_E71_SIGNOFF_DECISION_INVALID" || true
require_json_equals "${signoff_file}" "sampleMode" "SIMULATED_SELFTEST" "BO004_E72_SIGNOFF_MODE_INVALID" || true
require_json_truthy "${signoff_file}" "owners.product" "BO004_E73_SIGNOFF_PRODUCT_OWNER_MISSING" || true
require_json_truthy "${signoff_file}" "owners.tech" "BO004_E74_SIGNOFF_TECH_OWNER_MISSING" || true
require_json_truthy "${signoff_file}" "owners.ops" "BO004_E75_SIGNOFF_OPS_OWNER_MISSING" || true

if [[ "${result}" == "PASS" ]]; then
  add_issue "INFO" "BO004_E00_SELFTEST_PACK_PRESENT" "simulated bo004 admin release evidence pack validated"
fi

cat > "${SUMMARY_FILE}" <<EOF_SUMMARY
# BO-004 Admin Release Evidence Gate
result=${result}
sample_pack_dir=${SAMPLE_PACK_DIR}
summary_file=${SUMMARY_FILE}
output_tsv=${OUTPUT_TSV}
EOF_SUMMARY

echo "[bo004-admin-evidence-gate] result=${result}"
echo "[bo004-admin-evidence-gate] sample_pack_dir=${SAMPLE_PACK_DIR}"
echo "[bo004-admin-evidence-gate] summary=${SUMMARY_FILE}"
echo "[bo004-admin-evidence-gate] output_tsv=${OUTPUT_TSV}"
exit "${exit_code}"
