#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'USAGE'
Usage:
  script/dev/run_payment_stagea_p0_19_20.sh

Env:
  RUN_ID                                执行 ID（默认时间戳）
  ARTIFACT_BASE_DIR                     产物目录（默认 .tmp/payment_stagea_p0_19_20）
  RUN_TESTS=0|1                         #19 是否跑回归测试（默认 0）
  RUN_NOTIFY_SMOKE=0|1                  #19 是否跑通知回放烟测（默认 1）
  RUN_RETRY_POLICY_CHECK=0|1            #19 是否跑重试策略检查（默认 1）
  RUN_PARTNER_READINESS_CHECK=0|1       #19 是否跑服务商参数检查（默认 1）
  PARTNER_READINESS_CONFIG_FILE=<file>  #19 服务商参数配置文件（默认 sample）
  PARTNER_READINESS_STRICT=0|1          #19 服务商参数检查严格模式（默认 0）
  RUN_RECONCILE_CHECK=0|1               #19 是否直接跑对账检查（默认 0）
  RECONCILE_SUMMARY_FILE=<file>         #17 产物 summary（默认自动发现最新）
  ROLLBACK_PLAN_FILE=<file>             回滚方案文档（默认 ../hxy/03_payment/支付系统-技术架构说明-RuoYi版-2026-02-21.md）
  ACCEPTANCE_PLAN_FILE=<file>           验收标准文档（默认 ../hxy/06_roadmap/HXY-全新工作计划-0到50店-RuoYi版-v1-2026-02-22.md）
  REQUIRE_SCENARIO=0|1                  #20 缺少 #19 产物是否阻断（默认 1）
  REQUIRE_RECONCILE=0|1                 #20 缺少 #17 产物是否阻断（默认 1）
  REQUIRE_NAMING_GUARD=0|1              #20 新增资产命名门禁是否阻断（默认 1）
  REQUIRE_MEMORY_GUARD=0|1              #20 长记忆门禁是否阻断（默认 1）
  REQUIRE_OPS_STATUS=0|1                #20 缺少 ops 状态是否阻断（默认 0）
  RUN_REVIEW_TICKET_GATE=0|1            #20 是否执行人工复核工单门禁检查（默认 0）
  REQUIRE_REVIEW_TICKET=0|1             #20 工单门禁结果是否阻断发布（默认 0）
  RUN_SERVICE_ORDER_GATE=0|1            #20 是否执行服务单待预约门禁检查（默认 0）
  REQUIRE_SERVICE_ORDER_GATE=0|1        #20 服务单门禁结果是否阻断发布（默认 0）
  SERVICE_ORDER_WAIT_TIMEOUT_MINUTES=<n> 服务单待预约超时阈值分钟（默认 30）
  RUN_STORE_SKU_STOCK_GATE=0|1          #20 是否执行门店 SKU 库存流水门禁（默认 0）
  REQUIRE_STORE_SKU_STOCK_GATE=0|1      #20 门店 SKU 库存门禁是否阻断发布（默认 0）
  RUN_PRODUCT_TEMPLATE_GATE=0|1         #20 是否执行商品模板生成链路门禁（默认 0）
  REQUIRE_PRODUCT_TEMPLATE_GATE=0|1     #20 模板链路门禁结果是否阻断发布（默认 0）
  TEMPLATE_GATE_ACCESS_TOKEN=<token>    模板链路门禁所需 admin token（RUN_PRODUCT_TEMPLATE_GATE=1 时必填）
  TEMPLATE_GATE_BASE_URL=<url>          模板链路门禁 admin-api 地址（默认 http://127.0.0.1:48080/admin-api）
  TEMPLATE_GATE_CATEGORY_ID=<id>        模板链路门禁类目ID（默认 101）
  TEMPLATE_GATE_SPU_ID=<id>             模板链路门禁 SPU ID（默认 30001）
  TEMPLATE_GATE_TEMPLATE_VERSION_ID=<id> 模板链路门禁模板版本ID（可选）
  TEMPLATE_GATE_IDEMPOTENCY_KEY=<key>   模板链路门禁提交幂等键（默认 SPU30001-V1-CHAIN）
  OPS_STATUS_FILE=<file>                运维状态文件（可选）
  MAX_ARTIFACT_AGE_MINUTES=<n>          产物过期阈值（默认 1440）
USAGE
  exit 0
fi

RUN_ID="${RUN_ID:-$(date +%Y%m%d_%H%M%S)_$RANDOM}"
ARTIFACT_BASE_DIR="${ARTIFACT_BASE_DIR:-${ROOT_DIR}/.tmp/payment_stagea_p0_19_20}"
ARTIFACT_DIR="${ARTIFACT_BASE_DIR}/${RUN_ID}"
LOG_DIR="${ARTIFACT_DIR}/logs"

RUN_TESTS="${RUN_TESTS:-0}"
RUN_NOTIFY_SMOKE="${RUN_NOTIFY_SMOKE:-1}"
RUN_RETRY_POLICY_CHECK="${RUN_RETRY_POLICY_CHECK:-1}"
RUN_PARTNER_READINESS_CHECK="${RUN_PARTNER_READINESS_CHECK:-1}"
PARTNER_READINESS_CONFIG_FILE="${PARTNER_READINESS_CONFIG_FILE:-${ROOT_DIR}/script/dev/samples/wx_partner_channel.sample.json}"
PARTNER_READINESS_STRICT="${PARTNER_READINESS_STRICT:-0}"
RUN_RECONCILE_CHECK="${RUN_RECONCILE_CHECK:-0}"

RECONCILE_SUMMARY_FILE="${RECONCILE_SUMMARY_FILE:-}"
ROLLBACK_PLAN_FILE="${ROLLBACK_PLAN_FILE:-}"
ACCEPTANCE_PLAN_FILE="${ACCEPTANCE_PLAN_FILE:-}"
REQUIRE_SCENARIO="${REQUIRE_SCENARIO:-1}"
REQUIRE_RECONCILE="${REQUIRE_RECONCILE:-1}"
REQUIRE_NAMING_GUARD="${REQUIRE_NAMING_GUARD:-1}"
REQUIRE_MEMORY_GUARD="${REQUIRE_MEMORY_GUARD:-1}"
REQUIRE_OPS_STATUS="${REQUIRE_OPS_STATUS:-0}"
RUN_REVIEW_TICKET_GATE="${RUN_REVIEW_TICKET_GATE:-0}"
REQUIRE_REVIEW_TICKET="${REQUIRE_REVIEW_TICKET:-0}"
RUN_SERVICE_ORDER_GATE="${RUN_SERVICE_ORDER_GATE:-0}"
REQUIRE_SERVICE_ORDER_GATE="${REQUIRE_SERVICE_ORDER_GATE:-0}"
SERVICE_ORDER_WAIT_TIMEOUT_MINUTES="${SERVICE_ORDER_WAIT_TIMEOUT_MINUTES:-30}"
RUN_STORE_SKU_STOCK_GATE="${RUN_STORE_SKU_STOCK_GATE:-0}"
REQUIRE_STORE_SKU_STOCK_GATE="${REQUIRE_STORE_SKU_STOCK_GATE:-0}"
RUN_PRODUCT_TEMPLATE_GATE="${RUN_PRODUCT_TEMPLATE_GATE:-0}"
REQUIRE_PRODUCT_TEMPLATE_GATE="${REQUIRE_PRODUCT_TEMPLATE_GATE:-0}"
TEMPLATE_GATE_ACCESS_TOKEN="${TEMPLATE_GATE_ACCESS_TOKEN:-}"
TEMPLATE_GATE_BASE_URL="${TEMPLATE_GATE_BASE_URL:-http://127.0.0.1:48080/admin-api}"
TEMPLATE_GATE_CATEGORY_ID="${TEMPLATE_GATE_CATEGORY_ID:-101}"
TEMPLATE_GATE_SPU_ID="${TEMPLATE_GATE_SPU_ID:-30001}"
TEMPLATE_GATE_TEMPLATE_VERSION_ID="${TEMPLATE_GATE_TEMPLATE_VERSION_ID:-}"
TEMPLATE_GATE_IDEMPOTENCY_KEY="${TEMPLATE_GATE_IDEMPOTENCY_KEY:-SPU30001-V1-CHAIN}"
OPS_STATUS_FILE="${OPS_STATUS_FILE:-}"
MAX_ARTIFACT_AGE_MINUTES="${MAX_ARTIFACT_AGE_MINUTES:-1440}"
RECONCILE_OPTIONAL=0

if ! [[ "${PARTNER_READINESS_STRICT}" =~ ^[01]$ ]]; then
  echo "[stageA-p0-19-20] invalid PARTNER_READINESS_STRICT=${PARTNER_READINESS_STRICT}, expect 0|1" >&2
  exit 1
fi

RUN_LOG="${LOG_DIR}/run.log"
FINAL_GATE_LOG="${LOG_DIR}/final_gate.log"
RELEASE_GATE_LOG="${LOG_DIR}/release_gate.log"
NAMING_GUARD_LOG="${LOG_DIR}/naming_guard.log"
MEMORY_GUARD_LOG="${LOG_DIR}/memory_guard.log"
SUMMARY_FILE="${ARTIFACT_DIR}/summary.txt"
INDEX_FILE="${ARTIFACT_DIR}/artifact_index.md"
CI_GATE_FAIL_INDEX="${ARTIFACT_DIR}/ci_gate_fail_logs.md"

SCENARIO_OUT_DIR="${ARTIFACT_DIR}/scenario-replay"
SCENARIO_SUMMARY_FILE="${SCENARIO_OUT_DIR}/summary.txt"
SCENARIO_REPORT_FILE="${SCENARIO_OUT_DIR}/report.md"
SCENARIO_TSV="${SCENARIO_OUT_DIR}/scenario_result.tsv"
REVIEW_TICKET_GATE_OUT_DIR="${ARTIFACT_DIR}/review-ticket-gate"
REVIEW_TICKET_GATE_SUMMARY_FILE="${REVIEW_TICKET_GATE_OUT_DIR}/summary.txt"
REVIEW_TICKET_GATE_TSV="${REVIEW_TICKET_GATE_OUT_DIR}/result.tsv"
SERVICE_ORDER_GATE_OUT_DIR="${ARTIFACT_DIR}/service-order-gate"
SERVICE_ORDER_GATE_SUMMARY_FILE="${SERVICE_ORDER_GATE_OUT_DIR}/summary.txt"
SERVICE_ORDER_GATE_TSV="${SERVICE_ORDER_GATE_OUT_DIR}/result.tsv"
SERVICE_ORDER_GATE_SUMMARY_FOR_RELEASE=""
STORE_SKU_STOCK_GATE_OUT_DIR="${ARTIFACT_DIR}/store-sku-stock-gate"
STORE_SKU_STOCK_GATE_SUMMARY_FILE="${STORE_SKU_STOCK_GATE_OUT_DIR}/summary.txt"
STORE_SKU_STOCK_GATE_TSV="${STORE_SKU_STOCK_GATE_OUT_DIR}/result.tsv"
STORE_SKU_STOCK_GATE_SUMMARY_FOR_RELEASE=""
PRODUCT_TEMPLATE_GATE_OUT_DIR="${ARTIFACT_DIR}/product-template-gate"
PRODUCT_TEMPLATE_GATE_SUMMARY_FILE="${PRODUCT_TEMPLATE_GATE_OUT_DIR}/summary.txt"
PRODUCT_TEMPLATE_GATE_SUMMARY_FOR_RELEASE=""

GATE_TSV="${ARTIFACT_DIR}/release_gate.tsv"
GATE_REPORT="${ARTIFACT_DIR}/release_gate_report.md"

mkdir -p "${LOG_DIR}" "${SCENARIO_OUT_DIR}" "${REVIEW_TICKET_GATE_OUT_DIR}" "${SERVICE_ORDER_GATE_OUT_DIR}" "${STORE_SKU_STOCK_GATE_OUT_DIR}" "${PRODUCT_TEMPLATE_GATE_OUT_DIR}"
exec > >(tee -a "${RUN_LOG}") 2>&1

pick_first_existing_file() {
  local fallback="$1"
  local candidate
  for candidate in "$@"; do
    if [[ -n "${candidate}" && -f "${candidate}" ]]; then
      echo "${candidate}"
      return
    fi
  done
  echo "${fallback}"
}

resolve_doc_defaults() {
  if [[ -z "${ROLLBACK_PLAN_FILE}" ]]; then
    ROLLBACK_PLAN_FILE="$(pick_first_existing_file \
      "${ROOT_DIR}/../hxy/03_payment/支付系统-技术架构说明-RuoYi版-2026-02-21.md" \
      "${ROOT_DIR}/../hxy/02_architecture/HXY-技术架构规划-v2-2026-02-22.md" \
      "${ROOT_DIR}/../hxy/支付系统-技术架构说明-RuoYi版-2026-02-21.md")"
  fi

  if [[ -z "${ACCEPTANCE_PLAN_FILE}" ]]; then
    ACCEPTANCE_PLAN_FILE="$(pick_first_existing_file \
      "${ROOT_DIR}/../hxy/06_roadmap/HXY-全新工作计划-0到50店-RuoYi版-v1-2026-02-22.md" \
      "${ROOT_DIR}/../hxy/06_roadmap/支付系统-30项开发计划-执行版-2026-02-18.md" \
      "${ROOT_DIR}/../hxy/支付系统-30项开发计划-执行版-2026-02-18.md")"
  fi
}

resolve_reconcile_summary() {
  if [[ -n "${RECONCILE_SUMMARY_FILE}" && -f "${RECONCILE_SUMMARY_FILE}" ]]; then
    return
  fi
  local latest
  latest="$(ls -1dt .tmp/payment_stagea_p0_17_18/*/reconcile/summary.txt 2>/dev/null | head -n 1 || true)"
  if [[ -n "${latest}" ]]; then
    RECONCILE_SUMMARY_FILE="${latest}"
  fi
}

scenario_rc="unknown"
gate_rc="unknown"
review_ticket_gate_rc="SKIP"
service_order_gate_rc="SKIP"
store_sku_stock_gate_rc="SKIP"
product_template_gate_rc="SKIP"
naming_guard_rc="SKIP"
memory_guard_rc="SKIP"

to_artifact_rel() {
  local file="$1"
  if [[ "${file}" == "${ARTIFACT_DIR}/"* ]]; then
    echo "${file#${ARTIFACT_DIR}/}"
    return
  fi
  echo "${file}"
}

generate_ci_gate_fail_index() {
  local row_count=0

  append_log_row() {
    local file="$1"
    if [[ ! -f "${file}" ]]; then
      return
    fi
    local rel lines hint escaped_hint
    rel="$(to_artifact_rel "${file}")"
    lines="$(wc -l < "${file}" 2>/dev/null || echo 0)"
    hint="$(grep -E -m1 'BLOCK|FAIL|ERROR|Exception' "${file}" 2>/dev/null || true)"
    if [[ -z "${hint}" ]]; then
      hint="-"
    fi
    escaped_hint="${hint//|/\\|}"
    echo "| \`${rel}\` | present | ${lines} | ${escaped_hint} |"
    row_count=$((row_count + 1))
  }

  {
    echo "# CI Gate Failure Log Index"
    echo
    echo "- generated_at: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "- run_id: \`${RUN_ID}\`"
    echo
    echo "| file | status | lines | first_failure_hint |"
    echo "|---|---|---:|---|"

    append_log_row "${RUN_LOG}"
    append_log_row "${RELEASE_GATE_LOG}"
    append_log_row "${FINAL_GATE_LOG}"
    append_log_row "${SERVICE_ORDER_GATE_OUT_DIR}/run.log"
    append_log_row "${STORE_SKU_STOCK_GATE_OUT_DIR}/run.log"
    append_log_row "${PRODUCT_TEMPLATE_GATE_OUT_DIR}/run.log"

    if [[ -d "${SCENARIO_OUT_DIR}/logs" ]]; then
      while IFS= read -r file; do
        append_log_row "${file}"
      done < <(find "${SCENARIO_OUT_DIR}/logs" -type f -name '*.log' | sort)
    fi

    if [[ -n "${RECONCILE_SUMMARY_FILE}" && -f "${RECONCILE_SUMMARY_FILE}" ]]; then
      local reconcile_root
      reconcile_root="$(cd "$(dirname "${RECONCILE_SUMMARY_FILE}")/.." >/dev/null 2>&1 && pwd || true)"
      if [[ -n "${reconcile_root}" ]]; then
        append_log_row "${reconcile_root}/logs/reconcile_check.log"
        append_log_row "${reconcile_root}/logs/final_gate.log"
        append_log_row "${reconcile_root}/logs/run.log"
      fi
    fi

    if [[ "${row_count}" -eq 0 ]]; then
      echo "| - | missing | 0 | no gate log discovered |"
    fi
  } > "${CI_GATE_FAIL_INDEX}"
}

finalize() {
  local rc=$?
  local pipeline_rc="${PIPELINE_EXIT_CODE:-$rc}"
  local scenario_result gate_result
  local block_count warn_count

  scenario_result="$(grep -E '^suite_result=' "${SCENARIO_SUMMARY_FILE}" 2>/dev/null | head -n 1 | cut -d= -f2- || true)"
  block_count="$(awk -F'\t' 'NR>1 && $1=="BLOCK"{c++} END{print c+0}' "${GATE_TSV}" 2>/dev/null || echo 0)"
  warn_count="$(awk -F'\t' 'NR>1 && $1=="WARN"{c++} END{print c+0}' "${GATE_TSV}" 2>/dev/null || echo 0)"
  gate_result="PASS"
  if [[ "${block_count}" -gt 0 ]]; then
    gate_result="BLOCK"
  elif [[ "${warn_count}" -gt 0 ]]; then
    gate_result="PASS_WITH_WARN"
  fi

  {
    echo "run_id=${RUN_ID}"
    echo "pipeline_exit_code=${pipeline_rc}"
    echo "scenario_rc=${scenario_rc}"
    echo "scenario_result=${scenario_result}"
    echo "review_ticket_gate_rc=${review_ticket_gate_rc}"
    echo "service_order_gate_rc=${service_order_gate_rc}"
    echo "store_sku_stock_gate_rc=${store_sku_stock_gate_rc}"
    echo "product_template_gate_rc=${product_template_gate_rc}"
    echo "naming_guard_rc=${naming_guard_rc}"
    echo "memory_guard_rc=${memory_guard_rc}"
    echo "gate_rc=${gate_rc}"
    echo "gate_result=${gate_result}"
    echo "gate_block_count=${block_count}"
    echo "gate_warn_count=${warn_count}"
    echo "reconcile_summary_file=${RECONCILE_SUMMARY_FILE}"
    echo "rollback_plan_file=${ROLLBACK_PLAN_FILE}"
    echo "acceptance_plan_file=${ACCEPTANCE_PLAN_FILE}"
    echo "run_log=${RUN_LOG}"
    echo "final_gate_log=${FINAL_GATE_LOG}"
    echo "release_gate_log=${RELEASE_GATE_LOG}"
    echo "naming_guard_log=${NAMING_GUARD_LOG}"
    echo "memory_guard_log=${MEMORY_GUARD_LOG}"
    echo "ci_gate_fail_index_file=${CI_GATE_FAIL_INDEX}"
    echo "scenario_summary_file=${SCENARIO_SUMMARY_FILE}"
    echo "scenario_report_file=${SCENARIO_REPORT_FILE}"
    echo "scenario_tsv=${SCENARIO_TSV}"
    echo "review_ticket_gate_summary_file=${REVIEW_TICKET_GATE_SUMMARY_FILE}"
    echo "review_ticket_gate_tsv=${REVIEW_TICKET_GATE_TSV}"
    echo "service_order_gate_summary_file=${SERVICE_ORDER_GATE_SUMMARY_FILE}"
    echo "service_order_gate_tsv=${SERVICE_ORDER_GATE_TSV}"
    echo "store_sku_stock_gate_summary_file=${STORE_SKU_STOCK_GATE_SUMMARY_FILE}"
    echo "store_sku_stock_gate_tsv=${STORE_SKU_STOCK_GATE_TSV}"
    echo "product_template_gate_summary_file=${PRODUCT_TEMPLATE_GATE_SUMMARY_FILE}"
    echo "gate_tsv=${GATE_TSV}"
    echo "gate_report=${GATE_REPORT}"
  } > "${SUMMARY_FILE}"

  {
    echo "payment_stagea_p0_19_20_gate"
    echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "pipeline_exit_code=${pipeline_rc}"
    echo "scenario_rc=${scenario_rc}"
    echo "gate_rc=${gate_rc}"
    if [[ "${pipeline_rc}" == "0" ]]; then
      echo "decision=PASS"
    else
      echo "decision=BLOCK"
      echo "---- run.log tail (last 200 lines) ----"
      tail -n 200 "${RUN_LOG}" || true
    fi
  } > "${FINAL_GATE_LOG}"

  generate_ci_gate_fail_index

  {
    echo "# StageA #19 #20 Artifact Index"
    echo
    echo "- run_id: \`${RUN_ID}\`"
    echo "- pipeline_exit_code: \`${pipeline_rc}\`"
    echo "- scenario_result: \`${scenario_result}\`"
    echo "- release_gate_result: \`${gate_result}\`"
    echo "- gate_block_count: \`${block_count}\`"
    echo "- gate_warn_count: \`${warn_count}\`"
    echo
    echo "## Files"
    echo
    echo "- summary: \`summary.txt\`"
    echo "- final_gate_log: \`logs/final_gate.log\`"
    echo "- release_gate_log: \`logs/release_gate.log\`"
    echo "- naming_guard_log: \`logs/naming_guard.log\`"
    echo "- memory_guard_log: \`logs/memory_guard.log\`"
    echo "- ci gate fail log index: \`ci_gate_fail_logs.md\`"
    echo "- scenario summary: \`scenario-replay/summary.txt\`"
    echo "- scenario report: \`scenario-replay/report.md\`"
    echo "- scenario tsv: \`scenario-replay/scenario_result.tsv\`"
    echo "- review ticket gate summary: \`review-ticket-gate/summary.txt\`"
    echo "- review ticket gate tsv: \`review-ticket-gate/result.tsv\`"
    echo "- service order gate summary: \`service-order-gate/summary.txt\`"
    echo "- service order gate tsv: \`service-order-gate/result.tsv\`"
    echo "- product template gate summary: \`product-template-gate/summary.txt\`"
    echo "- release gate tsv: \`release_gate.tsv\`"
    echo "- release gate report: \`release_gate_report.md\`"
  } > "${INDEX_FILE}"

  echo "[stageA-p0-19-20] artifact_dir=${ARTIFACT_DIR}"
  echo "[stageA-p0-19-20] summary=${SUMMARY_FILE}"

  return "${pipeline_rc}"
}

trap finalize EXIT

resolve_doc_defaults
resolve_reconcile_summary
if [[ "${REQUIRE_RECONCILE}" == "0" ]]; then
  RECONCILE_OPTIONAL=1
fi

if [[ "${REQUIRE_NAMING_GUARD}" == "1" ]]; then
  echo "[stageA-p0-19-20] step=#20 naming guard"
  set +e
  bash script/dev/check_hxy_naming_guard.sh > "${NAMING_GUARD_LOG}" 2>&1
  naming_guard_rc=$?
  set -e
  if [[ "${naming_guard_rc}" == "2" ]]; then
    echo "[stageA-p0-19-20] result=BLOCK (naming guard)"
    PIPELINE_EXIT_CODE=2
    exit 2
  elif [[ "${naming_guard_rc}" != "0" ]]; then
    PIPELINE_EXIT_CODE="${naming_guard_rc}"
    exit "${naming_guard_rc}"
  fi
fi

if [[ "${REQUIRE_MEMORY_GUARD}" == "1" ]]; then
  echo "[stageA-p0-19-20] step=#20 memory guard"
  set +e
  if [[ -n "${GIT_DIFF_RANGE:-}" ]]; then
    CHECK_STAGED=0 \
    CHECK_UNSTAGED=0 \
    CHECK_UNTRACKED=0 \
    GIT_DIFF_RANGE="${GIT_DIFF_RANGE}" \
    bash script/dev/check_hxy_memory_guard.sh > "${MEMORY_GUARD_LOG}" 2>&1
  else
    CHECK_STAGED=1 \
    CHECK_UNSTAGED=0 \
    CHECK_UNTRACKED=0 \
    bash script/dev/check_hxy_memory_guard.sh > "${MEMORY_GUARD_LOG}" 2>&1
  fi
  memory_guard_rc=$?
  set -e
  if [[ "${memory_guard_rc}" == "2" ]]; then
    echo "[stageA-p0-19-20] result=BLOCK (memory guard)"
    PIPELINE_EXIT_CODE=2
    exit 2
  elif [[ "${memory_guard_rc}" != "0" ]]; then
    PIPELINE_EXIT_CODE="${memory_guard_rc}"
    exit "${memory_guard_rc}"
  fi
fi

echo "[stageA-p0-19-20] step=#19 abnormal scenario replay"
set +e
RECONCILE_OPTIONAL="${RECONCILE_OPTIONAL}" bash script/dev/run_payment_abnormal_scenario_replay.sh \
  --run-id "${RUN_ID}" \
  --out-base-dir "${ARTIFACT_DIR}" \
  --run-tests "${RUN_TESTS}" \
  --run-notify-smoke "${RUN_NOTIFY_SMOKE}" \
  --run-retry-policy-check "${RUN_RETRY_POLICY_CHECK}" \
  --run-partner-readiness-check "${RUN_PARTNER_READINESS_CHECK}" \
  --partner-readiness-config-file "${PARTNER_READINESS_CONFIG_FILE}" \
  --partner-readiness-strict "${PARTNER_READINESS_STRICT}" \
  --run-reconcile-check "${RUN_RECONCILE_CHECK}" \
  --reconcile-summary-file "${RECONCILE_SUMMARY_FILE}" \
  --db-host "${DB_HOST:-localhost}" \
  --db-port "${DB_PORT:-3306}" \
  --db-user "${DB_USER:-root}" \
  --db-password "${DB_PASSWORD:-}" \
  --db-name "${DB_NAME:-hxy_dev}"
scenario_rc=$?
set -e

if [[ "${scenario_rc}" != "0" && "${scenario_rc}" != "2" ]]; then
  PIPELINE_EXIT_CODE="${scenario_rc}"
  exit "${scenario_rc}"
fi

# run_payment_abnormal_scenario_replay.sh 输出目录结构：<out-base-dir>/<run-id>/
if [[ -f "${ARTIFACT_DIR}/${RUN_ID}/summary.txt" ]]; then
  mkdir -p "${SCENARIO_OUT_DIR}"
  cp -f "${ARTIFACT_DIR}/${RUN_ID}/summary.txt" "${SCENARIO_SUMMARY_FILE}"
  cp -f "${ARTIFACT_DIR}/${RUN_ID}/report.md" "${SCENARIO_REPORT_FILE}" || true
  cp -f "${ARTIFACT_DIR}/${RUN_ID}/scenario_result.tsv" "${SCENARIO_TSV}" || true
  cp -R "${ARTIFACT_DIR}/${RUN_ID}/logs" "${SCENARIO_OUT_DIR}/" 2>/dev/null || true
fi

# #19 若已执行对账检查，回填其 summary 路径给 #20 门禁使用，避免误判 missing
if [[ -z "${RECONCILE_SUMMARY_FILE}" && -f "${SCENARIO_SUMMARY_FILE}" ]]; then
  scenario_reconcile_summary="$(grep -E '^reconcile_summary_file=' "${SCENARIO_SUMMARY_FILE}" | head -n 1 | cut -d= -f2- || true)"
  if [[ -n "${scenario_reconcile_summary}" && -f "${scenario_reconcile_summary}" ]]; then
    RECONCILE_SUMMARY_FILE="${scenario_reconcile_summary}"
  fi
fi

if [[ "${RUN_REVIEW_TICKET_GATE}" == "1" ]]; then
  echo "[stageA-p0-19-20] step=#20 review ticket gate"
  set +e
  bash script/dev/check_after_sale_review_ticket_gate.sh \
    --db-host "${DB_HOST:-localhost}" \
    --db-port "${DB_PORT:-3306}" \
    --db-user "${DB_USER:-root}" \
    --db-password "${DB_PASSWORD:-}" \
    --db-name "${DB_NAME:-hxy_dev}" \
    --summary-file "${REVIEW_TICKET_GATE_SUMMARY_FILE}" \
    --output-tsv "${REVIEW_TICKET_GATE_TSV}" > "${REVIEW_TICKET_GATE_OUT_DIR}/run.log" 2>&1
  review_ticket_gate_rc=$?
  set -e
  if [[ "${review_ticket_gate_rc}" != "0" && "${review_ticket_gate_rc}" != "2" ]]; then
    PIPELINE_EXIT_CODE="${review_ticket_gate_rc}"
    exit "${review_ticket_gate_rc}"
  fi
fi

if [[ "${RUN_SERVICE_ORDER_GATE}" == "1" ]]; then
  echo "[stageA-p0-19-20] step=#20 service order booking gate"
  set +e
  bash script/dev/check_service_order_booking_gate.sh \
    --db-host "${DB_HOST:-localhost}" \
    --db-port "${DB_PORT:-3306}" \
    --db-user "${DB_USER:-root}" \
    --db-password "${DB_PASSWORD:-}" \
    --db-name "${DB_NAME:-hxy_dev}" \
    --wait-booking-timeout-minutes "${SERVICE_ORDER_WAIT_TIMEOUT_MINUTES}" \
    --require-overdue-zero "${REQUIRE_SERVICE_ORDER_GATE}" \
    --summary-file "${SERVICE_ORDER_GATE_SUMMARY_FILE}" \
    --output-tsv "${SERVICE_ORDER_GATE_TSV}" > "${SERVICE_ORDER_GATE_OUT_DIR}/run.log" 2>&1
  service_order_gate_rc=$?
  set -e
  if [[ "${service_order_gate_rc}" != "0" && "${service_order_gate_rc}" != "2" ]]; then
    PIPELINE_EXIT_CODE="${service_order_gate_rc}"
    exit "${service_order_gate_rc}"
  fi
fi

if [[ -f "${SERVICE_ORDER_GATE_SUMMARY_FILE}" ]]; then
  SERVICE_ORDER_GATE_SUMMARY_FOR_RELEASE="${SERVICE_ORDER_GATE_SUMMARY_FILE}"
fi

if [[ "${RUN_STORE_SKU_STOCK_GATE}" == "1" ]]; then
  echo "[stageA-p0-19-20] step=#20 store sku stock gate"
  set +e
  bash script/dev/check_store_sku_stock_gate.sh \
    --db-host "${DB_HOST:-localhost}" \
    --db-port "${DB_PORT:-3306}" \
    --db-user "${DB_USER:-root}" \
    --db-password "${DB_PASSWORD:-}" \
    --db-name "${DB_NAME:-hxy_dev}" \
    --require-overdue-zero "${REQUIRE_STORE_SKU_STOCK_GATE}" \
    --summary-file "${STORE_SKU_STOCK_GATE_SUMMARY_FILE}" \
    --output-tsv "${STORE_SKU_STOCK_GATE_TSV}" > "${STORE_SKU_STOCK_GATE_OUT_DIR}/run.log" 2>&1
  store_sku_stock_gate_rc=$?
  set -e
  if [[ "${store_sku_stock_gate_rc}" != "0" && "${store_sku_stock_gate_rc}" != "2" ]]; then
    PIPELINE_EXIT_CODE="${store_sku_stock_gate_rc}"
    exit "${store_sku_stock_gate_rc}"
  fi
fi

if [[ -f "${STORE_SKU_STOCK_GATE_SUMMARY_FILE}" ]]; then
  STORE_SKU_STOCK_GATE_SUMMARY_FOR_RELEASE="${STORE_SKU_STOCK_GATE_SUMMARY_FILE}"
fi

if [[ "${RUN_PRODUCT_TEMPLATE_GATE}" == "1" ]]; then
  echo "[stageA-p0-19-20] step=#20 product template generate gate"
  if [[ -z "${TEMPLATE_GATE_ACCESS_TOKEN}" ]]; then
    echo "[stageA-p0-19-20] TEMPLATE_GATE_ACCESS_TOKEN is required when RUN_PRODUCT_TEMPLATE_GATE=1" >&2
    PIPELINE_EXIT_CODE=1
    exit 1
  fi
  set +e
  template_version_args=()
  if [[ -n "${TEMPLATE_GATE_TEMPLATE_VERSION_ID}" ]]; then
    template_version_args=(--template-version-id "${TEMPLATE_GATE_TEMPLATE_VERSION_ID}")
  fi
  bash script/dev/check_product_template_generate_chain.sh \
    --base-url "${TEMPLATE_GATE_BASE_URL}" \
    --access-token "${TEMPLATE_GATE_ACCESS_TOKEN}" \
    --category-id "${TEMPLATE_GATE_CATEGORY_ID}" \
    --spu-id "${TEMPLATE_GATE_SPU_ID}" \
    "${template_version_args[@]}" \
    --idempotency-key "${TEMPLATE_GATE_IDEMPOTENCY_KEY}" \
    --summary-file "${PRODUCT_TEMPLATE_GATE_SUMMARY_FILE}" > "${PRODUCT_TEMPLATE_GATE_OUT_DIR}/run.log" 2>&1
  product_template_gate_rc=$?
  set -e
  if [[ "${product_template_gate_rc}" != "0" && "${product_template_gate_rc}" != "2" ]]; then
    PIPELINE_EXIT_CODE="${product_template_gate_rc}"
    exit "${product_template_gate_rc}"
  fi
fi

if [[ -f "${PRODUCT_TEMPLATE_GATE_SUMMARY_FILE}" ]]; then
  PRODUCT_TEMPLATE_GATE_SUMMARY_FOR_RELEASE="${PRODUCT_TEMPLATE_GATE_SUMMARY_FILE}"
fi

echo "[stageA-p0-19-20] step=#20 release blocker gate"
set +e
bash script/dev/check_payment_release_blockers.sh \
  --scenario-summary-file "${SCENARIO_SUMMARY_FILE}" \
  --reconcile-summary-file "${RECONCILE_SUMMARY_FILE}" \
  --review-ticket-summary-file "${REVIEW_TICKET_GATE_SUMMARY_FILE}" \
  --service-order-summary-file "${SERVICE_ORDER_GATE_SUMMARY_FOR_RELEASE}" \
  --store-sku-stock-summary-file "${STORE_SKU_STOCK_GATE_SUMMARY_FOR_RELEASE}" \
  --product-template-summary-file "${PRODUCT_TEMPLATE_GATE_SUMMARY_FOR_RELEASE}" \
  --rollback-plan-file "${ROLLBACK_PLAN_FILE}" \
  --acceptance-plan-file "${ACCEPTANCE_PLAN_FILE}" \
  --ops-status-file "${OPS_STATUS_FILE}" \
  --require-scenario "${REQUIRE_SCENARIO}" \
  --require-reconcile "${REQUIRE_RECONCILE}" \
  --require-review-ticket "${REQUIRE_REVIEW_TICKET}" \
  --require-service-order "${REQUIRE_SERVICE_ORDER_GATE}" \
  --require-store-sku-stock "${REQUIRE_STORE_SKU_STOCK_GATE}" \
  --require-product-template "${REQUIRE_PRODUCT_TEMPLATE_GATE}" \
  --require-ops-status "${REQUIRE_OPS_STATUS}" \
  --max-artifact-age-minutes "${MAX_ARTIFACT_AGE_MINUTES}" \
  --output-tsv "${GATE_TSV}" \
  --output-report "${GATE_REPORT}" > "${RELEASE_GATE_LOG}" 2>&1

gate_rc=$?
set -e

if [[ "${gate_rc}" == "2" ]]; then
  echo "[stageA-p0-19-20] result=BLOCK"
  PIPELINE_EXIT_CODE=2
  exit 2
elif [[ "${gate_rc}" != "0" ]]; then
  PIPELINE_EXIT_CODE="${gate_rc}"
  exit "${gate_rc}"
fi

echo "[stageA-p0-19-20] result=PASS"
PIPELINE_EXIT_CODE=0
exit 0
