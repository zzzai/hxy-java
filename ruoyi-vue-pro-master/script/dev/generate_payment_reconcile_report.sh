#!/usr/bin/env bash
set -euo pipefail

SUMMARY_FILE=""
ISSUES_TSV=""
OUTPUT_FILE=""
MAX_ROWS="${MAX_ROWS:-200}"

usage() {
  cat <<'USAGE'
Usage:
  script/dev/generate_payment_reconcile_report.sh [options]

Options:
  --summary-file <file>   check 脚本输出的 summary.txt（默认 /tmp/payment_reconcile/summary.txt）
  --issues-tsv <file>     问题明细 TSV（默认 /tmp/payment_reconcile/issues.tsv）
  --output-file <file>    报告输出路径（默认 /tmp/payment_reconcile/payment_reconcile_report.md）
  --max-rows <n>          报告最多展示问题条数（默认 200）
  -h, --help              Show help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --summary-file)
      SUMMARY_FILE="$2"
      shift 2
      ;;
    --issues-tsv)
      ISSUES_TSV="$2"
      shift 2
      ;;
    --output-file)
      OUTPUT_FILE="$2"
      shift 2
      ;;
    --max-rows)
      MAX_ROWS="$2"
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
  SUMMARY_FILE="/tmp/payment_reconcile/summary.txt"
fi
if [[ -z "${ISSUES_TSV}" ]]; then
  ISSUES_TSV="/tmp/payment_reconcile/issues.tsv"
fi
if [[ -z "${OUTPUT_FILE}" ]]; then
  OUTPUT_FILE="/tmp/payment_reconcile/payment_reconcile_report.md"
fi
if ! [[ "${MAX_ROWS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "Invalid --max-rows: ${MAX_ROWS}" >&2
  exit 1
fi

mkdir -p "$(dirname "${OUTPUT_FILE}")"

kv() {
  local file="$1"
  local key="$2"
  if [[ ! -f "${file}" ]]; then
    printf ''
    return
  fi
  local line
  line="$(grep -E "^${key}=" "${file}" | head -n 1 || true)"
  if [[ -z "${line}" ]]; then
    printf ''
  else
    printf '%s' "${line#*=}"
  fi
}

escape_md() {
  printf '%s' "$1" | sed 's/|/\\|/g'
}

BIZ_DATE="$(kv "${SUMMARY_FILE}" biz_date)"
RESULT="$(kv "${SUMMARY_FILE}" reconcile_result)"
TOTAL="$(kv "${SUMMARY_FILE}" issue_total)"
BLOCK_COUNT="$(kv "${SUMMARY_FILE}" issue_block_count)"
WARN_COUNT="$(kv "${SUMMARY_FILE}" issue_warn_count)"
WINDOW_START="$(kv "${SUMMARY_FILE}" window_start)"
WINDOW_END="$(kv "${SUMMARY_FILE}" window_end)"

[[ -z "${BIZ_DATE}" ]] && BIZ_DATE="N/A"
[[ -z "${RESULT}" ]] && RESULT="unknown"
[[ -z "${TOTAL}" ]] && TOTAL="0"
[[ -z "${BLOCK_COUNT}" ]] && BLOCK_COUNT="0"
[[ -z "${WARN_COUNT}" ]] && WARN_COUNT="0"
[[ -z "${WINDOW_START}" ]] && WINDOW_START="N/A"
[[ -z "${WINDOW_END}" ]] && WINDOW_END="N/A"

issue_rows=""
if [[ -f "${ISSUES_TSV}" ]]; then
  issue_rows="$(awk -F'\t' -v max_rows="${MAX_ROWS}" '
    NR==1 {next}
    NR>1 && NF>0 {
      c++;
      if (c > max_rows) {
        next;
      }
      gsub(/\|/, "\\|", $0);
      printf "| %s | %s | %s | %s | %s | %s | %s | %s | %s |\n", $1,$2,$3,$4,$5,$6,$7,$8,$9;
    }' "${ISSUES_TSV}")"
fi

issue_type_stats=""
if [[ -f "${ISSUES_TSV}" ]]; then
  issue_type_stats="$(awk -F'\t' '
    NR==1 {next}
    NR>1 && NF>0 {
      key=$1"|"$2;
      c[key]++;
    }
    END {
      for (k in c) {
        split(k, arr, "|");
        printf "| %s | %s | %d |\n", arr[1], arr[2], c[k];
      }
    }' "${ISSUES_TSV}" | sort)"
fi

{
  echo "# Payment Reconcile Daily Report"
  echo
  echo "- generated_at: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "- biz_date: ${BIZ_DATE}"
  echo "- window: ${WINDOW_START} ~ ${WINDOW_END}"
  echo
  echo "## 1) Summary"
  echo
  echo "| field | value |"
  echo "|---|---|"
  echo "| result | **$(escape_md "${RESULT}")** |"
  echo "| total_issues | ${TOTAL} |"
  echo "| block_issues | ${BLOCK_COUNT} |"
  echo "| warn_issues | ${WARN_COUNT} |"
  echo "| summary_file | \`${SUMMARY_FILE}\` |"
  echo "| issues_tsv | \`${ISSUES_TSV}\` |"
  echo
  echo "## 2) Issue Distribution"
  echo
  echo "| severity | issue_type | count |"
  echo "|---|---|---:|"
  if [[ -n "${issue_type_stats}" ]]; then
    printf '%s\n' "${issue_type_stats}"
  else
    echo "| N/A | N/A | 0 |"
  fi
  echo
  echo "## 3) Issue Details"
  echo
  echo "| severity | issue_type | entity_type | biz_key | code | detail | expected_amount | actual_amount | occurred_at |"
  echo "|---|---|---|---|---|---|---:|---:|---|"
  if [[ -n "${issue_rows}" ]]; then
    printf '%s\n' "${issue_rows}"
  else
    echo "| N/A | N/A | N/A | N/A | N/A | no issues | N/A | N/A | N/A |"
  fi
} > "${OUTPUT_FILE}"

echo "[payment-reconcile-report] report=${OUTPUT_FILE}"
