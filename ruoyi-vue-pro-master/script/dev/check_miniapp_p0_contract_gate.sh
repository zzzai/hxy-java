#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SUMMARY_FILE="${SUMMARY_FILE:-}"
OUTPUT_TSV="${OUTPUT_TSV:-}"
CONTRACT_FILE="${CONTRACT_FILE:-${ROOT_DIR}/../docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_miniapp_p0_contract_gate.sh [options]

Options:
  --repo-root <dir>      Repository root (default: auto-detect from script path)
  --contract-file <file> Contract freeze file (default: ../docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md)
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
    --contract-file)
      CONTRACT_FILE="$2"
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
  SUMMARY_FILE="/tmp/miniapp_p0_contract_gate/summary.txt"
fi
if [[ -z "${OUTPUT_TSV}" ]]; then
  OUTPUT_TSV="/tmp/miniapp_p0_contract_gate/result.tsv"
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

check_required_file() {
  local file="$1"
  local code="$2"
  local detail="$3"
  local key="$4"

  if [[ -f "${file}" ]]; then
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

result="PASS"
exit_code=0

contract_file_exists_check="UNKNOWN"
p0_page_section_anchor_check="UNKNOWN"
error_code_section_anchor_check="UNKNOWN"
p0_payment_result_page_anchor_check="UNKNOWN"
p0_after_sale_apply_page_anchor_check="UNKNOWN"
p0_after_sale_list_page_anchor_check="UNKNOWN"
p0_after_sale_detail_page_anchor_check="UNKNOWN"
p0_refund_progress_page_anchor_check="UNKNOWN"
p0_exception_fallback_page_anchor_check="UNKNOWN"
error_code_idempotent_conflict_anchor_check="UNKNOWN"
error_code_runid_not_exists_anchor_check="UNKNOWN"
error_code_degrade_semantics_anchor_check="UNKNOWN"

check_required_file "${CONTRACT_FILE}" \
  "MAP001_CONTRACT_FILE_MISSING" \
  "missing contract file ${CONTRACT_FILE}" \
  "contract_file_exists_check" || true

check_required_pattern_ci "${CONTRACT_FILE}" 'P0[[:space:]]*页面清单' \
  "MAP002_P0_PAGE_SECTION_ANCHOR_MISSING" \
  "missing P0 页面清单 anchor in ${CONTRACT_FILE}" \
  "p0_page_section_anchor_check" || true

check_required_pattern_ci "${CONTRACT_FILE}" '(关键错误码|错误码锚点)' \
  "MAP003_ERROR_CODE_SECTION_ANCHOR_MISSING" \
  "missing 关键错误码锚点 anchor in ${CONTRACT_FILE}" \
  "error_code_section_anchor_check" || true

check_required_pattern_ci "${CONTRACT_FILE}" '支付结果' \
  "MAP004_P0_PAYMENT_RESULT_PAGE_ANCHOR_MISSING" \
  "missing 支付结果 page anchor in ${CONTRACT_FILE}" \
  "p0_payment_result_page_anchor_check" || true

check_required_pattern_ci "${CONTRACT_FILE}" '售后申请' \
  "MAP005_P0_AFTER_SALE_APPLY_PAGE_ANCHOR_MISSING" \
  "missing 售后申请 page anchor in ${CONTRACT_FILE}" \
  "p0_after_sale_apply_page_anchor_check" || true

check_required_pattern_ci "${CONTRACT_FILE}" '售后列表' \
  "MAP006_P0_AFTER_SALE_LIST_PAGE_ANCHOR_MISSING" \
  "missing 售后列表 page anchor in ${CONTRACT_FILE}" \
  "p0_after_sale_list_page_anchor_check" || true

check_required_pattern_ci "${CONTRACT_FILE}" '售后详情' \
  "MAP007_P0_AFTER_SALE_DETAIL_PAGE_ANCHOR_MISSING" \
  "missing 售后详情 page anchor in ${CONTRACT_FILE}" \
  "p0_after_sale_detail_page_anchor_check" || true

check_required_pattern_ci "${CONTRACT_FILE}" '退款进度' \
  "MAP008_P0_REFUND_PROGRESS_PAGE_ANCHOR_MISSING" \
  "missing 退款进度 page anchor in ${CONTRACT_FILE}" \
  "p0_refund_progress_page_anchor_check" || true

check_required_pattern_ci "${CONTRACT_FILE}" '异常兜底' \
  "MAP009_P0_EXCEPTION_FALLBACK_PAGE_ANCHOR_MISSING" \
  "missing 异常兜底 page anchor in ${CONTRACT_FILE}" \
  "p0_exception_fallback_page_anchor_check" || true

check_required_pattern_ci "${CONTRACT_FILE}" '(BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT|1030004012|幂等冲突)' \
  "MAP010_ERROR_CODE_IDEMPOTENT_CONFLICT_ANCHOR_MISSING" \
  "missing 幂等冲突 error code anchor in ${CONTRACT_FILE}" \
  "error_code_idempotent_conflict_anchor_check" || true

check_required_pattern_ci "${CONTRACT_FILE}" '(BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS|1030004016|runId[^[:alnum:]]*不存在|runId[[:space:]_-]*not[[:space:]_-]*exists)' \
  "MAP011_ERROR_CODE_RUNID_NOT_EXISTS_ANCHOR_MISSING" \
  "missing runId不存在 error code anchor in ${CONTRACT_FILE}" \
  "error_code_runid_not_exists_anchor_check" || true

check_required_pattern_ci "${CONTRACT_FILE}" '(TICKET_SYNC_DEGRADED|降级语义|fail[-_ ]?open|degrad(e|ed))' \
  "MAP012_ERROR_CODE_DEGRADE_SEMANTICS_ANCHOR_MISSING" \
  "missing 降级语义 anchor in ${CONTRACT_FILE}" \
  "error_code_degrade_semantics_anchor_check" || true

{
  echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "result=${result}"
  echo "repo_root=${ROOT_DIR}"
  echo "contract_file=${CONTRACT_FILE}"
  echo "contract_file_exists_check=${contract_file_exists_check}"
  echo "p0_page_section_anchor_check=${p0_page_section_anchor_check}"
  echo "error_code_section_anchor_check=${error_code_section_anchor_check}"
  echo "p0_payment_result_page_anchor_check=${p0_payment_result_page_anchor_check}"
  echo "p0_after_sale_apply_page_anchor_check=${p0_after_sale_apply_page_anchor_check}"
  echo "p0_after_sale_list_page_anchor_check=${p0_after_sale_list_page_anchor_check}"
  echo "p0_after_sale_detail_page_anchor_check=${p0_after_sale_detail_page_anchor_check}"
  echo "p0_refund_progress_page_anchor_check=${p0_refund_progress_page_anchor_check}"
  echo "p0_exception_fallback_page_anchor_check=${p0_exception_fallback_page_anchor_check}"
  echo "error_code_idempotent_conflict_anchor_check=${error_code_idempotent_conflict_anchor_check}"
  echo "error_code_runid_not_exists_anchor_check=${error_code_runid_not_exists_anchor_check}"
  echo "error_code_degrade_semantics_anchor_check=${error_code_degrade_semantics_anchor_check}"
  echo "output_tsv=${OUTPUT_TSV}"
} > "${SUMMARY_FILE}"

echo "== MiniApp P0 Contract Gate =="
echo "result=${result}"
echo "summary_file=${SUMMARY_FILE}"
echo "output_tsv=${OUTPUT_TSV}"

exit "${exit_code}"
