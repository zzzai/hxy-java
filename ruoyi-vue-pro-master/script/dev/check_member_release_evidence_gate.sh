#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SAMPLE_PACK_DIR=""
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_TSV="${OUTPUT_TSV:-}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_member_release_evidence_gate.sh [options]

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
  SAMPLE_PACK_DIR="${ROOT_DIR}/tests/fixtures/member-release-evidence-simulated"
fi
if [[ -z "${SUMMARY_FILE}" ]]; then
  SUMMARY_FILE="/tmp/member_release_evidence_gate/summary.txt"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="/tmp/member_release_evidence_gate/result.tsv"
fi

mkdir -p "$(dirname "${SUMMARY_FILE}")" "$(dirname "${OUTPUT_TSV}")"
echo -e "severity\tcode\tdetail" > "${OUTPUT_TSV}"

result="PASS"
exit_code=0

add_issue() {
  local severity="$1"
  local code="$2"
  local detail="$3"
  echo -e "${severity}\t${code}\tdetail" | sed "s/detail/${detail//\//\\/}/" >> "${OUTPUT_TSV}"
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

level_file="${SAMPLE_PACK_DIR}/level-page-sample.json"
experience_file="${SAMPLE_PACK_DIR}/level-experience-page.json"
asset_file="${SAMPLE_PACK_DIR}/asset-ledger-page.json"
tag_file="${SAMPLE_PACK_DIR}/tag-page-sample.json"
switch_file="${SAMPLE_PACK_DIR}/switch-snapshot.json"
gray_file="${SAMPLE_PACK_DIR}/gray-stage.json"
rollback_file="${SAMPLE_PACK_DIR}/rollback-drill.json"
signoff_file="${SAMPLE_PACK_DIR}/signoff.json"

require_file "${level_file}" "MRE01_LEVEL_SAMPLE_MISSING" || true
require_file "${experience_file}" "MRE02_EXPERIENCE_SAMPLE_MISSING" || true
require_file "${asset_file}" "MRE03_ASSET_SAMPLE_MISSING" || true
require_file "${tag_file}" "MRE04_TAG_SAMPLE_MISSING" || true
require_file "${switch_file}" "MRE05_SWITCH_SNAPSHOT_MISSING" || true
require_file "${gray_file}" "MRE06_GRAY_SAMPLE_MISSING" || true
require_file "${rollback_file}" "MRE07_ROLLBACK_SAMPLE_MISSING" || true
require_file "${signoff_file}" "MRE08_SIGNOFF_SAMPLE_MISSING" || true

require_json_equals "${level_file}" "request.method" "GET" "MRE11_LEVEL_METHOD_INVALID" || true
require_json_equals "${level_file}" "request.path" "/member/level/list" "MRE12_LEVEL_PATH_INVALID" || true
require_json_equals "${level_file}" "response.commonResult.code" "0" "MRE13_LEVEL_CODE_INVALID" || true

require_json_equals "${experience_file}" "request.method" "GET" "MRE21_EXPERIENCE_METHOD_INVALID" || true
require_json_equals "${experience_file}" "request.path" "/member/experience-record/page" "MRE22_EXPERIENCE_PATH_INVALID" || true
require_json_truthy "${experience_file}" "request.query.pageNo" "MRE23_EXPERIENCE_PAGE_NO_MISSING" || true
require_json_equals "${experience_file}" "response.commonResult.code" "0" "MRE24_EXPERIENCE_CODE_INVALID" || true

require_json_equals "${asset_file}" "request.method" "GET" "MRE31_ASSET_METHOD_INVALID" || true
require_json_equals "${asset_file}" "request.path" "/member/asset-ledger/page" "MRE32_ASSET_PATH_INVALID" || true
require_json_truthy "${asset_file}" "request.query.assetType" "MRE33_ASSET_TYPE_MISSING" || true
require_json_equals "${asset_file}" "response.commonResult.code" "0" "MRE34_ASSET_CODE_INVALID" || true
require_json_equals "${asset_file}" "response.commonResult.data.degraded" "false" "MRE35_ASSET_DEGRADED_DEFAULT_INVALID" || true
require_json_equals "${asset_file}" "audit.judgement.degradedFieldIsDefaultOnly" "true" "MRE36_ASSET_DEGRADED_JUDGEMENT_INVALID" || true

require_json_equals "${tag_file}" "request.method" "GET" "MRE41_TAG_METHOD_INVALID" || true
require_json_equals "${tag_file}" "request.path" "/member/tag/my" "MRE42_TAG_PATH_INVALID" || true
require_json_equals "${tag_file}" "response.commonResult.code" "0" "MRE43_TAG_CODE_INVALID" || true

require_json_equals "${switch_file}" "sampleMode" "SIMULATED_SELFTEST" "MRE51_SWITCH_MODE_INVALID" || true
require_json_equals "${switch_file}" "hasDedicatedFeatureFlag" "false" "MRE52_FEATURE_FLAG_INVENTED" || true
require_json_equals "${switch_file}" "gateStrategy" "A_WINDOW_RELEASE_SIGNOFF_REQUIRED" "MRE53_GATE_STRATEGY_INVALID" || true

require_json_equals "${gray_file}" "sampleMode" "SIMULATED_SELFTEST" "MRE61_GRAY_MODE_INVALID" || true
require_json_equals "${gray_file}" "result" "PASS" "MRE62_GRAY_RESULT_INVALID" || true
require_json_equals "${gray_file}" "decision" "NO_RELEASE" "MRE63_GRAY_DECISION_INVALID" || true
require_json_truthy "${gray_file}" "runId" "MRE64_GRAY_RUN_ID_MISSING" || true

require_json_equals "${rollback_file}" "sampleMode" "SIMULATED_SELFTEST" "MRE71_ROLLBACK_MODE_INVALID" || true
require_json_equals "${rollback_file}" "rollbackMode" "NO_RELEASE_PROMOTION" "MRE72_ROLLBACK_SCOPE_INVALID" || true
require_json_equals "${rollback_file}" "result" "PASS" "MRE73_ROLLBACK_RESULT_INVALID" || true
require_json_truthy "${rollback_file}" "owner" "MRE74_ROLLBACK_OWNER_MISSING" || true

require_json_equals "${signoff_file}" "sampleMode" "SIMULATED_SELFTEST" "MRE81_SIGNOFF_MODE_INVALID" || true
require_json_equals "${signoff_file}" "decision" "SELFTEST_ONLY_NO_RELEASE" "MRE82_SIGNOFF_DECISION_INVALID" || true
require_json_truthy "${signoff_file}" "owners.product" "MRE83_SIGNOFF_PRODUCT_OWNER_MISSING" || true
require_json_truthy "${signoff_file}" "owners.tech" "MRE84_SIGNOFF_TECH_OWNER_MISSING" || true
require_json_truthy "${signoff_file}" "owners.ops" "MRE85_SIGNOFF_OPS_OWNER_MISSING" || true

add_issue "INFO" "MRE000_SELFTEST_PACK_PRESENT" "simulated member release evidence pack validated"

cat > "${SUMMARY_FILE}" <<EOF_SUMMARY
# Member Release Evidence Gate
result=${result}
sample_pack_dir=${SAMPLE_PACK_DIR}
summary_scope=simulated_selftest_only
release_decision=NO_RELEASE
EOF_SUMMARY

echo "[member-evidence-gate] result=${result}"
echo "[member-evidence-gate] sample_pack_dir=${SAMPLE_PACK_DIR}"
echo "[member-evidence-gate] summary=${SUMMARY_FILE}"
echo "[member-evidence-gate] output_tsv=${OUTPUT_TSV}"

if [[ "${result}" == "BLOCK" ]]; then
  exit 2
fi
