#!/usr/bin/env bash
set -euo pipefail

SCENARIO_SUMMARY_FILE=""
RECONCILE_SUMMARY_FILE=""
ROLLBACK_PLAN_FILE=""
ACCEPTANCE_PLAN_FILE=""
OPS_STATUS_FILE=""
REVIEW_TICKET_SUMMARY_FILE=""
SERVICE_ORDER_SUMMARY_FILE=""
COMMISSION_SETTLEMENT_SUMMARY_FILE=""
STORE_SKU_STOCK_SUMMARY_FILE=""
PRODUCT_TEMPLATE_SUMMARY_FILE=""
REQUIRE_SCENARIO="${REQUIRE_SCENARIO:-1}"
REQUIRE_RECONCILE="${REQUIRE_RECONCILE:-1}"
REQUIRE_OPS_STATUS="${REQUIRE_OPS_STATUS:-0}"
REQUIRE_REVIEW_TICKET="${REQUIRE_REVIEW_TICKET:-0}"
REQUIRE_SERVICE_ORDER="${REQUIRE_SERVICE_ORDER:-0}"
REQUIRE_COMMISSION_SETTLEMENT="${REQUIRE_COMMISSION_SETTLEMENT:-0}"
REQUIRE_STORE_SKU_STOCK="${REQUIRE_STORE_SKU_STOCK:-0}"
REQUIRE_PRODUCT_TEMPLATE="${REQUIRE_PRODUCT_TEMPLATE:-0}"
MAX_ARTIFACT_AGE_MINUTES="${MAX_ARTIFACT_AGE_MINUTES:-1440}"
OUTPUT_TSV=""
OUTPUT_REPORT=""

usage() {
  cat <<'USAGE'
Usage:
  script/dev/check_payment_release_blockers.sh [options]

Options:
  --scenario-summary-file <file>    #19 场景回放 summary.txt
  --reconcile-summary-file <file>   #17 日对账 summary.txt
  --rollback-plan-file <file>       回滚方案文档（必填）
  --acceptance-plan-file <file>     验收标准文档（必填）
  --ops-status-file <file>          运维状态文件（可选）
  --review-ticket-summary-file <file> 售后人工复核工单门禁 summary（可选）
  --service-order-summary-file <file> 服务单待预约门禁 summary（可选）
  --commission-settlement-summary-file <file> 佣金结算健康门禁 summary（可选）
  --store-sku-stock-summary-file <file> 门店 SKU 库存流水门禁 summary（可选）
  --product-template-summary-file <file> 商品模板生成链路门禁 summary（可选）
  --require-scenario <0|1>          缺少 #19 产物是否阻断（默认 1）
  --require-reconcile <0|1>         缺少 #17 产物是否阻断（默认 1）
  --require-ops-status <0|1>        缺少 ops 状态是否阻断（默认 0）
  --require-review-ticket <0|1>     缺少/异常工单门禁是否阻断（默认 0）
  --require-service-order <0|1>     缺少/异常服务单门禁是否阻断（默认 0）
  --require-commission-settlement <0|1> 缺少/异常佣金结算门禁是否阻断（默认 0）
  --require-store-sku-stock <0|1>   缺少/异常门店 SKU 库存门禁是否阻断（默认 0）
  --require-product-template <0|1>  缺少/异常商品模板门禁是否阻断（默认 0）
  --max-artifact-age-minutes <n>    产物过期阈值分钟（默认 1440）
  --output-tsv <file>               结果 TSV（可选）
  --output-report <file>            报告 Markdown（可选）
  -h, --help                        Show help

Exit Code:
  0  : PASS/PASS_WITH_WARN
  2  : BLOCK
  1+ : 执行错误
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --scenario-summary-file)
      SCENARIO_SUMMARY_FILE="$2"
      shift 2
      ;;
    --reconcile-summary-file)
      RECONCILE_SUMMARY_FILE="$2"
      shift 2
      ;;
    --rollback-plan-file)
      ROLLBACK_PLAN_FILE="$2"
      shift 2
      ;;
    --acceptance-plan-file)
      ACCEPTANCE_PLAN_FILE="$2"
      shift 2
      ;;
    --ops-status-file)
      OPS_STATUS_FILE="$2"
      shift 2
      ;;
    --review-ticket-summary-file)
      REVIEW_TICKET_SUMMARY_FILE="$2"
      shift 2
      ;;
    --service-order-summary-file)
      SERVICE_ORDER_SUMMARY_FILE="$2"
      shift 2
      ;;
    --commission-settlement-summary-file)
      COMMISSION_SETTLEMENT_SUMMARY_FILE="$2"
      shift 2
      ;;
    --store-sku-stock-summary-file)
      STORE_SKU_STOCK_SUMMARY_FILE="$2"
      shift 2
      ;;
    --product-template-summary-file)
      PRODUCT_TEMPLATE_SUMMARY_FILE="$2"
      shift 2
      ;;
    --require-scenario)
      REQUIRE_SCENARIO="$2"
      shift 2
      ;;
    --require-reconcile)
      REQUIRE_RECONCILE="$2"
      shift 2
      ;;
    --require-ops-status)
      REQUIRE_OPS_STATUS="$2"
      shift 2
      ;;
    --require-review-ticket)
      REQUIRE_REVIEW_TICKET="$2"
      shift 2
      ;;
    --require-service-order)
      REQUIRE_SERVICE_ORDER="$2"
      shift 2
      ;;
    --require-commission-settlement)
      REQUIRE_COMMISSION_SETTLEMENT="$2"
      shift 2
      ;;
    --require-store-sku-stock)
      REQUIRE_STORE_SKU_STOCK="$2"
      shift 2
      ;;
    --require-product-template)
      REQUIRE_PRODUCT_TEMPLATE="$2"
      shift 2
      ;;
    --max-artifact-age-minutes)
      MAX_ARTIFACT_AGE_MINUTES="$2"
      shift 2
      ;;
    --output-tsv)
      OUTPUT_TSV="$2"
      shift 2
      ;;
    --output-report)
      OUTPUT_REPORT="$2"
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

for val in "${REQUIRE_SCENARIO}" "${REQUIRE_RECONCILE}" "${REQUIRE_OPS_STATUS}" "${REQUIRE_REVIEW_TICKET}" \
  "${REQUIRE_SERVICE_ORDER}" "${REQUIRE_COMMISSION_SETTLEMENT}" "${REQUIRE_STORE_SKU_STOCK}" "${REQUIRE_PRODUCT_TEMPLATE}"; do
  if ! [[ "${val}" =~ ^[01]$ ]]; then
    echo "Invalid require flag: ${val}" >&2
    exit 1
  fi
done
if ! [[ "${MAX_ARTIFACT_AGE_MINUTES}" =~ ^[1-9][0-9]*$ ]]; then
  echo "Invalid --max-artifact-age-minutes: ${MAX_ARTIFACT_AGE_MINUTES}" >&2
  exit 1
fi

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

calc_age_minutes() {
  local ts="$1"
  if [[ -z "${ts}" ]]; then
    echo "-1"
    return
  fi
  local ts_epoch now_epoch
  ts_epoch="$(date -d "${ts}" +%s 2>/dev/null || echo -1)"
  if [[ "${ts_epoch}" == "-1" ]]; then
    echo "-1"
    return
  fi
  now_epoch="$(date +%s)"
  echo $(( (now_epoch - ts_epoch) / 60 ))
}

mkdir -p "$(dirname "${OUTPUT_TSV:-/tmp/payment_release_gate/result.tsv}")" "$(dirname "${OUTPUT_REPORT:-/tmp/payment_release_gate/report.md}")"
[[ -z "${OUTPUT_TSV}" ]] && OUTPUT_TSV="/tmp/payment_release_gate/result.tsv"
[[ -z "${OUTPUT_REPORT}" ]] && OUTPUT_REPORT="/tmp/payment_release_gate/report.md"

mkdir -p "$(dirname "${OUTPUT_TSV}")" "$(dirname "${OUTPUT_REPORT}")"
echo -e "severity\tcode\tdetail" > "${OUTPUT_TSV}"

add_issue() {
  local sev="$1"
  local code="$2"
  local detail="$3"
  echo -e "${sev}\t${code}\t${detail}" >> "${OUTPUT_TSV}"
}

# ---------- DoR / 文档 ----------
if [[ -z "${ROLLBACK_PLAN_FILE}" ]]; then
  add_issue "BLOCK" "G01_ROLLBACK_FILE_PARAM_MISSING" "rollback-plan-file parameter missing"
elif [[ ! -f "${ROLLBACK_PLAN_FILE}" ]]; then
  add_issue "BLOCK" "G02_ROLLBACK_FILE_MISSING" "rollback plan not found: ${ROLLBACK_PLAN_FILE}"
fi

if [[ -z "${ACCEPTANCE_PLAN_FILE}" ]]; then
  add_issue "BLOCK" "G03_ACCEPTANCE_FILE_PARAM_MISSING" "acceptance-plan-file parameter missing"
elif [[ ! -f "${ACCEPTANCE_PLAN_FILE}" ]]; then
  add_issue "BLOCK" "G04_ACCEPTANCE_FILE_MISSING" "acceptance plan not found: ${ACCEPTANCE_PLAN_FILE}"
fi

# ---------- #19 场景回放 ----------
if [[ "${REQUIRE_SCENARIO}" == "1" ]]; then
  if [[ -z "${SCENARIO_SUMMARY_FILE}" || ! -f "${SCENARIO_SUMMARY_FILE}" ]]; then
    add_issue "BLOCK" "G10_SCENARIO_SUMMARY_MISSING" "scenario summary missing"
  else
    scenario_result="$(kv "${SCENARIO_SUMMARY_FILE}" suite_result)"
    scenario_block_count="$(kv "${SCENARIO_SUMMARY_FILE}" scenario_block_count)"
    scenario_warn_count="$(kv "${SCENARIO_SUMMARY_FILE}" scenario_warn_count)"
    scenario_generated_at="$(kv "${SCENARIO_SUMMARY_FILE}" generated_at)"
    scenario_age_min="$(calc_age_minutes "${scenario_generated_at}")"

    [[ -z "${scenario_block_count}" ]] && scenario_block_count="0"
    [[ -z "${scenario_warn_count}" ]] && scenario_warn_count="0"

    if [[ "${scenario_result}" == "BLOCK" || "${scenario_block_count}" != "0" ]]; then
      add_issue "BLOCK" "G11_SCENARIO_BLOCK" "scenario_result=${scenario_result}, scenario_block_count=${scenario_block_count}"
    fi
    if [[ "${scenario_warn_count}" != "0" ]]; then
      add_issue "WARN" "G12_SCENARIO_WARN" "scenario_warn_count=${scenario_warn_count}"
    fi
    if [[ "${scenario_age_min}" == "-1" ]]; then
      add_issue "WARN" "G13_SCENARIO_TIMESTAMP_UNKNOWN" "invalid generated_at in scenario summary"
    elif [[ "${scenario_age_min}" -gt "${MAX_ARTIFACT_AGE_MINUTES}" ]]; then
      add_issue "BLOCK" "G14_SCENARIO_ARTIFACT_STALE" "scenario artifact age ${scenario_age_min}min > ${MAX_ARTIFACT_AGE_MINUTES}min"
    fi
  fi
fi

# ---------- #17 对账 ----------
if [[ "${REQUIRE_RECONCILE}" == "1" ]]; then
  if [[ -z "${RECONCILE_SUMMARY_FILE}" || ! -f "${RECONCILE_SUMMARY_FILE}" ]]; then
    add_issue "BLOCK" "G20_RECONCILE_SUMMARY_MISSING" "reconcile summary missing"
  else
    reconcile_result="$(kv "${RECONCILE_SUMMARY_FILE}" reconcile_result)"
    reconcile_block_count="$(kv "${RECONCILE_SUMMARY_FILE}" issue_block_count)"
    reconcile_warn_count="$(kv "${RECONCILE_SUMMARY_FILE}" issue_warn_count)"
    reconcile_generated_at="$(kv "${RECONCILE_SUMMARY_FILE}" generated_at)"
    reconcile_age_min="$(calc_age_minutes "${reconcile_generated_at}")"

    [[ -z "${reconcile_block_count}" ]] && reconcile_block_count="0"
    [[ -z "${reconcile_warn_count}" ]] && reconcile_warn_count="0"

    if [[ "${reconcile_result}" == "BLOCK" || "${reconcile_block_count}" != "0" ]]; then
      add_issue "BLOCK" "G21_RECONCILE_BLOCK" "reconcile_result=${reconcile_result}, issue_block_count=${reconcile_block_count}"
    fi
    if [[ "${reconcile_warn_count}" != "0" ]]; then
      add_issue "WARN" "G22_RECONCILE_WARN" "issue_warn_count=${reconcile_warn_count}"
    fi
    if [[ "${reconcile_age_min}" == "-1" ]]; then
      add_issue "WARN" "G23_RECONCILE_TIMESTAMP_UNKNOWN" "invalid generated_at in reconcile summary"
    elif [[ "${reconcile_age_min}" -gt "${MAX_ARTIFACT_AGE_MINUTES}" ]]; then
      add_issue "BLOCK" "G24_RECONCILE_ARTIFACT_STALE" "reconcile artifact age ${reconcile_age_min}min > ${MAX_ARTIFACT_AGE_MINUTES}min"
    fi
  fi
fi

# ---------- 人工复核工单门禁 ----------
if [[ "${REQUIRE_REVIEW_TICKET}" == "1" ]]; then
  if [[ -z "${REVIEW_TICKET_SUMMARY_FILE}" || ! -f "${REVIEW_TICKET_SUMMARY_FILE}" ]]; then
    add_issue "BLOCK" "G25_REVIEW_TICKET_SUMMARY_MISSING" "review ticket summary missing"
  else
    review_ticket_result="$(kv "${REVIEW_TICKET_SUMMARY_FILE}" result)"
    pending_overdue_total="$(kv "${REVIEW_TICKET_SUMMARY_FILE}" pending_overdue_total)"
    pending_overdue_p0="$(kv "${REVIEW_TICKET_SUMMARY_FILE}" pending_overdue_p0)"

    [[ -z "${pending_overdue_total}" ]] && pending_overdue_total="0"
    [[ -z "${pending_overdue_p0}" ]] && pending_overdue_p0="0"

    if [[ "${review_ticket_result}" == "BLOCK" || "${pending_overdue_p0}" != "0" ]]; then
      add_issue "BLOCK" "G26_REVIEW_TICKET_P0_BLOCK" "review_ticket_result=${review_ticket_result}, pending_overdue_p0=${pending_overdue_p0}"
    fi
    if [[ "${pending_overdue_total}" != "0" && "${pending_overdue_p0}" == "0" ]]; then
      add_issue "WARN" "G27_REVIEW_TICKET_WARN" "pending_overdue_total=${pending_overdue_total}"
    fi
  fi
fi

# ---------- 服务单待预约门禁 ----------
if [[ "${REQUIRE_SERVICE_ORDER}" == "1" ]]; then
  if [[ -z "${SERVICE_ORDER_SUMMARY_FILE}" || ! -f "${SERVICE_ORDER_SUMMARY_FILE}" ]]; then
    add_issue "BLOCK" "G40_SERVICE_ORDER_SUMMARY_MISSING" "service order summary missing"
  fi
elif [[ -n "${SERVICE_ORDER_SUMMARY_FILE}" && ! -f "${SERVICE_ORDER_SUMMARY_FILE}" ]]; then
  add_issue "WARN" "G41_SERVICE_ORDER_SUMMARY_NOT_FOUND" "service order summary not found: ${SERVICE_ORDER_SUMMARY_FILE}"
fi

if [[ -n "${SERVICE_ORDER_SUMMARY_FILE}" && -f "${SERVICE_ORDER_SUMMARY_FILE}" ]]; then
  service_order_result="$(kv "${SERVICE_ORDER_SUMMARY_FILE}" result)"
  waiting_total="$(kv "${SERVICE_ORDER_SUMMARY_FILE}" waiting_total)"
  waiting_overdue_total="$(kv "${SERVICE_ORDER_SUMMARY_FILE}" waiting_overdue_total)"
  service_order_generated_at="$(kv "${SERVICE_ORDER_SUMMARY_FILE}" generated_at)"
  service_order_age_min="$(calc_age_minutes "${service_order_generated_at}")"

  [[ -z "${waiting_total}" ]] && waiting_total="0"
  [[ -z "${waiting_overdue_total}" ]] && waiting_overdue_total="0"

  if [[ "${service_order_result}" == "BLOCK" || "${waiting_overdue_total}" != "0" ]]; then
    add_issue "BLOCK" "G42_SERVICE_ORDER_OVERDUE_BLOCK" "service_order_result=${service_order_result}, waiting_overdue_total=${waiting_overdue_total}"
  fi
  if [[ "${waiting_total}" != "0" && "${waiting_overdue_total}" == "0" ]]; then
    add_issue "WARN" "G43_SERVICE_ORDER_WAITING_WARN" "waiting_total=${waiting_total}, waiting_overdue_total=${waiting_overdue_total}"
  fi
  if [[ "${service_order_age_min}" == "-1" ]]; then
    add_issue "WARN" "G44_SERVICE_ORDER_TIMESTAMP_UNKNOWN" "invalid generated_at in service order summary"
  elif [[ "${service_order_age_min}" -gt "${MAX_ARTIFACT_AGE_MINUTES}" ]]; then
    add_issue "BLOCK" "G45_SERVICE_ORDER_ARTIFACT_STALE" "service order artifact age ${service_order_age_min}min > ${MAX_ARTIFACT_AGE_MINUTES}min"
  fi
fi

# ---------- 佣金结算健康门禁 ----------
if [[ "${REQUIRE_COMMISSION_SETTLEMENT}" == "1" ]]; then
  if [[ -z "${COMMISSION_SETTLEMENT_SUMMARY_FILE}" || ! -f "${COMMISSION_SETTLEMENT_SUMMARY_FILE}" ]]; then
    add_issue "BLOCK" "G58_COMMISSION_SETTLEMENT_SUMMARY_MISSING" "commission settlement summary missing"
  fi
elif [[ -n "${COMMISSION_SETTLEMENT_SUMMARY_FILE}" && ! -f "${COMMISSION_SETTLEMENT_SUMMARY_FILE}" ]]; then
  add_issue "WARN" "G59_COMMISSION_SETTLEMENT_SUMMARY_NOT_FOUND" "commission settlement summary not found: ${COMMISSION_SETTLEMENT_SUMMARY_FILE}"
fi

if [[ -n "${COMMISSION_SETTLEMENT_SUMMARY_FILE}" && -f "${COMMISSION_SETTLEMENT_SUMMARY_FILE}" ]]; then
  commission_settlement_result="$(kv "${COMMISSION_SETTLEMENT_SUMMARY_FILE}" result)"
  pending_review_overdue_total="$(kv "${COMMISSION_SETTLEMENT_SUMMARY_FILE}" pending_review_overdue_total)"
  notify_failed_total="$(kv "${COMMISSION_SETTLEMENT_SUMMARY_FILE}" notify_failed_total)"
  notify_pending_overdue_total="$(kv "${COMMISSION_SETTLEMENT_SUMMARY_FILE}" notify_pending_overdue_total)"
  commission_settlement_generated_at="$(kv "${COMMISSION_SETTLEMENT_SUMMARY_FILE}" generated_at)"
  commission_settlement_age_min="$(calc_age_minutes "${commission_settlement_generated_at}")"

  [[ -z "${pending_review_overdue_total}" ]] && pending_review_overdue_total="0"
  [[ -z "${notify_failed_total}" ]] && notify_failed_total="0"
  [[ -z "${notify_pending_overdue_total}" ]] && notify_pending_overdue_total="0"

  if [[ "${commission_settlement_result}" == "BLOCK" ]]; then
    add_issue "BLOCK" "G60_COMMISSION_SETTLEMENT_BLOCK" "commission_settlement_result=${commission_settlement_result}, pending_review_overdue_total=${pending_review_overdue_total}, notify_failed_total=${notify_failed_total}"
  fi
  if [[ "${pending_review_overdue_total}" != "0" && "${commission_settlement_result}" != "BLOCK" ]]; then
    add_issue "WARN" "G61_COMMISSION_REVIEW_OVERDUE_WARN" "pending_review_overdue_total=${pending_review_overdue_total}"
  fi
  if [[ "${notify_failed_total}" != "0" && "${commission_settlement_result}" != "BLOCK" ]]; then
    add_issue "WARN" "G62_COMMISSION_NOTIFY_FAILED_WARN" "notify_failed_total=${notify_failed_total}"
  fi
  if [[ "${notify_pending_overdue_total}" != "0" ]]; then
    add_issue "WARN" "G63_COMMISSION_NOTIFY_PENDING_WARN" "notify_pending_overdue_total=${notify_pending_overdue_total}"
  fi
  if [[ "${commission_settlement_age_min}" == "-1" ]]; then
    add_issue "WARN" "G64_COMMISSION_SETTLEMENT_TIMESTAMP_UNKNOWN" "invalid generated_at in commission settlement summary"
  elif [[ "${commission_settlement_age_min}" -gt "${MAX_ARTIFACT_AGE_MINUTES}" ]]; then
    add_issue "BLOCK" "G65_COMMISSION_SETTLEMENT_ARTIFACT_STALE" "commission settlement artifact age ${commission_settlement_age_min}min > ${MAX_ARTIFACT_AGE_MINUTES}min"
  fi
fi

# ---------- 门店 SKU 库存流水门禁 ----------
if [[ "${REQUIRE_STORE_SKU_STOCK}" == "1" ]]; then
  if [[ -z "${STORE_SKU_STOCK_SUMMARY_FILE}" || ! -f "${STORE_SKU_STOCK_SUMMARY_FILE}" ]]; then
    add_issue "BLOCK" "G52_STORE_SKU_STOCK_SUMMARY_MISSING" "store sku stock summary missing"
  fi
elif [[ -n "${STORE_SKU_STOCK_SUMMARY_FILE}" && ! -f "${STORE_SKU_STOCK_SUMMARY_FILE}" ]]; then
  add_issue "WARN" "G53_STORE_SKU_STOCK_SUMMARY_NOT_FOUND" "store sku stock summary not found: ${STORE_SKU_STOCK_SUMMARY_FILE}"
fi

if [[ -n "${STORE_SKU_STOCK_SUMMARY_FILE}" && -f "${STORE_SKU_STOCK_SUMMARY_FILE}" ]]; then
  store_sku_stock_result="$(kv "${STORE_SKU_STOCK_SUMMARY_FILE}" result)"
  retry_overdue_total="$(kv "${STORE_SKU_STOCK_SUMMARY_FILE}" retry_overdue_total)"
  retry_now_total="$(kv "${STORE_SKU_STOCK_SUMMARY_FILE}" retry_now_total)"
  pending_total="$(kv "${STORE_SKU_STOCK_SUMMARY_FILE}" pending_total)"
  failed_total="$(kv "${STORE_SKU_STOCK_SUMMARY_FILE}" failed_total)"
  store_sku_stock_generated_at="$(kv "${STORE_SKU_STOCK_SUMMARY_FILE}" generated_at)"
  store_sku_stock_age_min="$(calc_age_minutes "${store_sku_stock_generated_at}")"

  [[ -z "${retry_overdue_total}" ]] && retry_overdue_total="0"
  [[ -z "${retry_now_total}" ]] && retry_now_total="0"
  [[ -z "${pending_total}" ]] && pending_total="0"
  [[ -z "${failed_total}" ]] && failed_total="0"

  if [[ "${store_sku_stock_result}" == "BLOCK" || "${retry_overdue_total}" != "0" ]]; then
    add_issue "BLOCK" "G54_STORE_SKU_STOCK_OVERDUE_BLOCK" "store_sku_stock_result=${store_sku_stock_result}, retry_overdue_total=${retry_overdue_total}"
  fi
  if [[ "${retry_now_total}" != "0" && "${retry_overdue_total}" == "0" ]]; then
    add_issue "WARN" "G55_STORE_SKU_STOCK_RETRY_WARN" "retry_now_total=${retry_now_total}, pending_total=${pending_total}, failed_total=${failed_total}"
  fi
  if [[ "${store_sku_stock_age_min}" == "-1" ]]; then
    add_issue "WARN" "G56_STORE_SKU_STOCK_TIMESTAMP_UNKNOWN" "invalid generated_at in store sku stock summary"
  elif [[ "${store_sku_stock_age_min}" -gt "${MAX_ARTIFACT_AGE_MINUTES}" ]]; then
    add_issue "BLOCK" "G57_STORE_SKU_STOCK_ARTIFACT_STALE" "store sku stock artifact age ${store_sku_stock_age_min}min > ${MAX_ARTIFACT_AGE_MINUTES}min"
  fi
fi

# ---------- 商品模板生成链路门禁 ----------
if [[ "${REQUIRE_PRODUCT_TEMPLATE}" == "1" ]]; then
  if [[ -z "${PRODUCT_TEMPLATE_SUMMARY_FILE}" || ! -f "${PRODUCT_TEMPLATE_SUMMARY_FILE}" ]]; then
    add_issue "BLOCK" "G46_PRODUCT_TEMPLATE_SUMMARY_MISSING" "product template summary missing"
  fi
elif [[ -n "${PRODUCT_TEMPLATE_SUMMARY_FILE}" && ! -f "${PRODUCT_TEMPLATE_SUMMARY_FILE}" ]]; then
  add_issue "WARN" "G47_PRODUCT_TEMPLATE_SUMMARY_NOT_FOUND" "product template summary not found: ${PRODUCT_TEMPLATE_SUMMARY_FILE}"
fi

if [[ -n "${PRODUCT_TEMPLATE_SUMMARY_FILE}" && -f "${PRODUCT_TEMPLATE_SUMMARY_FILE}" ]]; then
  product_template_result="$(kv "${PRODUCT_TEMPLATE_SUMMARY_FILE}" result)"
  product_template_generated_at="$(kv "${PRODUCT_TEMPLATE_SUMMARY_FILE}" generated_at)"
  product_template_age_min="$(calc_age_minutes "${product_template_generated_at}")"

  if [[ "${product_template_result}" == "BLOCK" ]]; then
    add_issue "BLOCK" "G48_PRODUCT_TEMPLATE_BLOCK" "product_template_result=${product_template_result}"
  elif [[ "${product_template_result}" != "PASS" ]]; then
    add_issue "WARN" "G49_PRODUCT_TEMPLATE_RESULT_UNKNOWN" "product_template_result=${product_template_result}"
  fi
  if [[ "${product_template_age_min}" == "-1" ]]; then
    add_issue "WARN" "G50_PRODUCT_TEMPLATE_TIMESTAMP_UNKNOWN" "invalid generated_at in product template summary"
  elif [[ "${product_template_age_min}" -gt "${MAX_ARTIFACT_AGE_MINUTES}" ]]; then
    add_issue "BLOCK" "G51_PRODUCT_TEMPLATE_ARTIFACT_STALE" "product template artifact age ${product_template_age_min}min > ${MAX_ARTIFACT_AGE_MINUTES}min"
  fi
fi

# ---------- 运维状态 ----------
if [[ "${REQUIRE_OPS_STATUS}" == "1" ]]; then
  if [[ -z "${OPS_STATUS_FILE}" || ! -f "${OPS_STATUS_FILE}" ]]; then
    add_issue "BLOCK" "G30_OPS_STATUS_MISSING" "ops status file missing"
  else
    ops_status="$(kv "${OPS_STATUS_FILE}" ops_status)"
    ops_generated_at="$(kv "${OPS_STATUS_FILE}" generated_at)"
    ops_age_min="$(calc_age_minutes "${ops_generated_at}")"
    if [[ "${ops_status}" != "PASS" ]]; then
      add_issue "BLOCK" "G31_OPS_STATUS_FAIL" "ops_status=${ops_status}"
    fi
    if [[ "${ops_age_min}" == "-1" ]]; then
      add_issue "WARN" "G32_OPS_TIMESTAMP_UNKNOWN" "invalid generated_at in ops status"
    elif [[ "${ops_age_min}" -gt "${MAX_ARTIFACT_AGE_MINUTES}" ]]; then
      add_issue "BLOCK" "G33_OPS_STATUS_STALE" "ops status age ${ops_age_min}min > ${MAX_ARTIFACT_AGE_MINUTES}min"
    fi
  fi
fi

block_count="$(awk -F'\t' 'NR>1 && $1=="BLOCK"{c++} END{print c+0}' "${OUTPUT_TSV}")"
warn_count="$(awk -F'\t' 'NR>1 && $1=="WARN"{c++} END{print c+0}' "${OUTPUT_TSV}")"
result="PASS"
exit_code=0
if [[ "${block_count}" -gt 0 ]]; then
  result="BLOCK"
  exit_code=2
elif [[ "${warn_count}" -gt 0 ]]; then
  result="PASS_WITH_WARN"
fi

{
  echo "# Payment Release Blocker Gate"
  echo
  echo "- generated_at: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "- result: **${result}**"
  echo "- block_count: ${block_count}"
  echo "- warn_count: ${warn_count}"
  echo
  echo "## Issues"
  echo
  echo "| severity | code | detail |"
  echo "|---|---|---|"
  awk -F'\t' 'NR>1 {gsub(/\|/, "\\|", $3); printf "| %s | %s | %s |\n", $1,$2,$3}' "${OUTPUT_TSV}"
} > "${OUTPUT_REPORT}"

echo "== Payment Release Blocker Gate =="
echo -e "severity\tcode\tdetail"
awk 'NR>1 {print}' "${OUTPUT_TSV}" || true
echo "Summary: BLOCK=${block_count}, WARN=${warn_count}"
echo "Result: ${result}"
echo "report=${OUTPUT_REPORT}"
echo "result=${result}"

exit "${exit_code}"
