#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SAMPLE_PACK_DIR=""
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_TSV="${OUTPUT_TSV:-}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_reserved_runtime_release_evidence_gate.sh [options]

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
  SAMPLE_PACK_DIR="${ROOT_DIR}/tests/fixtures/reserved-runtime-release-evidence-simulated"
fi
if [[ -z "${SUMMARY_FILE}" ]]; then
  SUMMARY_FILE="/tmp/reserved_runtime_release_evidence_gate/summary.txt"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="/tmp/reserved_runtime_release_evidence_gate/result.tsv"
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

gift_page_file="${SAMPLE_PACK_DIR}/gift-card-template-page.json"
gift_create_file="${SAMPLE_PACK_DIR}/gift-card-order-create.json"
referral_overview_file="${SAMPLE_PACK_DIR}/referral-overview.json"
referral_bind_file="${SAMPLE_PACK_DIR}/referral-bind-inviter.json"
feed_page_file="${SAMPLE_PACK_DIR}/technician-feed-page.json"
feed_comment_file="${SAMPLE_PACK_DIR}/technician-feed-comment-create.json"
switch_file="${SAMPLE_PACK_DIR}/switch-snapshot.json"
gray_file="${SAMPLE_PACK_DIR}/gray-stage.json"
rollback_file="${SAMPLE_PACK_DIR}/rollback-drill.json"
signoff_file="${SAMPLE_PACK_DIR}/signoff.json"

require_file "${gift_page_file}" "RRE01_GIFT_PAGE_SAMPLE_MISSING" || true
require_file "${gift_create_file}" "RRE02_GIFT_CREATE_SAMPLE_MISSING" || true
require_file "${referral_overview_file}" "RRE03_REFERRAL_OVERVIEW_SAMPLE_MISSING" || true
require_file "${referral_bind_file}" "RRE04_REFERRAL_BIND_SAMPLE_MISSING" || true
require_file "${feed_page_file}" "RRE05_FEED_PAGE_SAMPLE_MISSING" || true
require_file "${feed_comment_file}" "RRE06_FEED_COMMENT_SAMPLE_MISSING" || true
require_file "${switch_file}" "RRE07_SWITCH_SNAPSHOT_MISSING" || true
require_file "${gray_file}" "RRE08_GRAY_SAMPLE_MISSING" || true
require_file "${rollback_file}" "RRE09_ROLLBACK_SAMPLE_MISSING" || true
require_file "${signoff_file}" "RRE10_SIGNOFF_SAMPLE_MISSING" || true

require_json_equals "${gift_page_file}" "request.method" "GET" "RRE11_GIFT_PAGE_METHOD_INVALID" || true
require_json_equals "${gift_page_file}" "request.path" "/promotion/gift-card/template/page" "RRE12_GIFT_PAGE_PATH_INVALID" || true
require_json_truthy "${gift_page_file}" "request.query.pageNo" "RRE13_GIFT_PAGE_PAGE_NO_MISSING" || true
require_json_equals "${gift_page_file}" "response.commonResult.code" "0" "RRE14_GIFT_PAGE_CODE_INVALID" || true

require_json_equals "${gift_create_file}" "request.method" "POST" "RRE21_GIFT_CREATE_METHOD_INVALID" || true
require_json_equals "${gift_create_file}" "request.path" "/promotion/gift-card/order/create" "RRE22_GIFT_CREATE_PATH_INVALID" || true
require_json_truthy "${gift_create_file}" "request.body.templateId" "RRE23_GIFT_CREATE_TEMPLATE_ID_MISSING" || true
require_json_truthy "${gift_create_file}" "request.body.clientToken" "RRE24_GIFT_CREATE_CLIENT_TOKEN_MISSING" || true
require_json_equals "${gift_create_file}" "response.commonResult.code" "0" "RRE25_GIFT_CREATE_CODE_INVALID" || true

require_json_equals "${referral_overview_file}" "request.method" "GET" "RRE31_REFERRAL_OVERVIEW_METHOD_INVALID" || true
require_json_equals "${referral_overview_file}" "request.path" "/promotion/referral/overview" "RRE32_REFERRAL_OVERVIEW_PATH_INVALID" || true
require_json_equals "${referral_overview_file}" "response.commonResult.code" "0" "RRE33_REFERRAL_OVERVIEW_CODE_INVALID" || true
require_json_truthy "${referral_overview_file}" "response.commonResult.data.referralCode" "RRE34_REFERRAL_CODE_MISSING" || true

require_json_equals "${referral_bind_file}" "request.method" "POST" "RRE41_REFERRAL_BIND_METHOD_INVALID" || true
require_json_equals "${referral_bind_file}" "request.path" "/promotion/referral/bind-inviter" "RRE42_REFERRAL_BIND_PATH_INVALID" || true
require_json_truthy "${referral_bind_file}" "request.body.clientToken" "RRE43_REFERRAL_BIND_CLIENT_TOKEN_MISSING" || true
require_json_equals "${referral_bind_file}" "response.commonResult.code" "0" "RRE44_REFERRAL_BIND_CODE_INVALID" || true
require_json_equals "${referral_bind_file}" "audit.judgement.primary" "CODE_EQ_0" "RRE45_REFERRAL_BIND_JUDGEMENT_INVALID" || true

require_json_equals "${feed_page_file}" "request.method" "GET" "RRE51_FEED_PAGE_METHOD_INVALID" || true
require_json_equals "${feed_page_file}" "request.path" "/booking/technician/feed/page" "RRE52_FEED_PAGE_PATH_INVALID" || true
require_json_truthy "${feed_page_file}" "request.query.storeId" "RRE53_FEED_STORE_ID_MISSING" || true
require_json_equals "${feed_page_file}" "response.commonResult.code" "0" "RRE54_FEED_PAGE_CODE_INVALID" || true

require_json_equals "${feed_comment_file}" "request.method" "POST" "RRE61_FEED_COMMENT_METHOD_INVALID" || true
require_json_equals "${feed_comment_file}" "request.path" "/booking/technician/feed/comment/create" "RRE62_FEED_COMMENT_PATH_INVALID" || true
require_json_truthy "${feed_comment_file}" "request.body.clientToken" "RRE63_FEED_COMMENT_CLIENT_TOKEN_MISSING" || true
require_json_equals "${feed_comment_file}" "response.commonResult.code" "0" "RRE64_FEED_COMMENT_CODE_INVALID" || true
require_json_equals "${feed_comment_file}" "response.commonResult.data.status" "REVIEWING" "RRE65_FEED_COMMENT_STATUS_INVALID" || true

require_json_equals "${switch_file}" "sampleMode" "SIMULATED_SELFTEST" "RRE71_SWITCH_MODE_INVALID" || true
require_json_equals "${switch_file}" "switches.miniapp.gift-card" "off" "RRE72_GIFT_SWITCH_INVALID" || true
require_json_equals "${switch_file}" "switches.miniapp.referral" "off" "RRE73_REFERRAL_SWITCH_INVALID" || true
require_json_equals "${switch_file}" "switches.miniapp.technician-feed.audit" "off" "RRE74_FEED_SWITCH_INVALID" || true
require_json_equals "${switch_file}" "decision" "NO_RELEASE" "RRE75_SWITCH_DECISION_INVALID" || true

require_json_equals "${gray_file}" "sampleMode" "SIMULATED_SELFTEST" "RRE81_GRAY_MODE_INVALID" || true
require_json_equals "${gray_file}" "result" "PASS" "RRE82_GRAY_RESULT_INVALID" || true
require_json_equals "${gray_file}" "decision" "NO_RELEASE" "RRE83_GRAY_DECISION_INVALID" || true
require_json_truthy "${gray_file}" "runId" "RRE84_GRAY_RUN_ID_MISSING" || true

require_json_equals "${rollback_file}" "sampleMode" "SIMULATED_SELFTEST" "RRE91_ROLLBACK_MODE_INVALID" || true
require_json_equals "${rollback_file}" "rollbackMode" "ALL_SWITCH_OFF" "RRE92_ROLLBACK_SCOPE_INVALID" || true
require_json_equals "${rollback_file}" "result" "PASS" "RRE93_ROLLBACK_RESULT_INVALID" || true
require_json_truthy "${rollback_file}" "owner" "RRE94_ROLLBACK_OWNER_MISSING" || true

require_json_equals "${signoff_file}" "sampleMode" "SIMULATED_SELFTEST" "RRE101_SIGNOFF_MODE_INVALID" || true
require_json_equals "${signoff_file}" "decision" "SELFTEST_ONLY_NO_RELEASE" "RRE102_SIGNOFF_DECISION_INVALID" || true
require_json_truthy "${signoff_file}" "owners.product" "RRE103_SIGNOFF_PRODUCT_OWNER_MISSING" || true
require_json_truthy "${signoff_file}" "owners.tech" "RRE104_SIGNOFF_TECH_OWNER_MISSING" || true
require_json_truthy "${signoff_file}" "owners.ops" "RRE105_SIGNOFF_OPS_OWNER_MISSING" || true

add_issue "INFO" "RRE000_SELFTEST_PACK_PRESENT" "simulated reserved runtime release evidence pack validated"

cat > "${SUMMARY_FILE}" <<EOF_SUMMARY
# Reserved Runtime Release Evidence Gate
result=${result}
sample_pack_dir=${SAMPLE_PACK_DIR}
summary_scope=simulated_selftest_only
release_decision=NO_RELEASE
EOF_SUMMARY

echo "[reserved-runtime-evidence-gate] result=${result}"
echo "[reserved-runtime-evidence-gate] sample_pack_dir=${SAMPLE_PACK_DIR}"
echo "[reserved-runtime-evidence-gate] summary=${SUMMARY_FILE}"
echo "[reserved-runtime-evidence-gate] output_tsv=${OUTPUT_TSV}"

if [[ "${result}" == "BLOCK" ]]; then
  exit 2
fi
