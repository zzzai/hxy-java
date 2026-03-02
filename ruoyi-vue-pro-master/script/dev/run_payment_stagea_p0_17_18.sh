#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'USAGE'
Usage:
  script/dev/run_payment_stagea_p0_17_18.sh

Env:
  RUN_ID                         执行 ID（默认时间戳）
  ARTIFACT_BASE_DIR              产物根目录（默认 .tmp/payment_stagea_p0_17_18）
  BIZ_DATE                       对账日期（默认昨天）
  STALE_MINUTES                  卡滞阈值分钟（默认 10）
  REQUIRE_NAMING_GUARD=0|1       新增资产命名门禁是否阻断（默认 1）
  REQUIRE_MEMORY_GUARD=0|1       长记忆门禁是否阻断（默认 1）
  RUN_STORE_SKU_STOCK_GATE=0|1   是否执行门店 SKU 库存流水门禁（默认 0）
  REQUIRE_STORE_SKU_STOCK_GATE=0|1 门店 SKU 库存门禁是否阻断（默认 0）
  EMIT_ON_WARN=0|1               WARN 是否生成工单（默认 0）
  ENABLE_NOTIFY=0|1              是否发送工单通知（默认 0）
  NOTIFY_DRY_RUN=0|1             通知是否仅演练（默认 1）
  NOTIFY_STRICT=0|1              通知失败是否阻断（默认 0）
  MYSQL_DEFAULTS_FILE             mysql defaults-file (e.g. /root/.my.cnf)
  GIT_DIFF_RANGE                  记忆门禁差异范围（如 base...head）
  DB_HOST/DB_PORT/DB_USER/DB_PASSWORD/DB_NAME
  FEISHU_WEBHOOK_URL/WEBHOOK_URL
USAGE
  exit 0
fi

RUN_ID="${RUN_ID:-$(date +%Y%m%d_%H%M%S)_$RANDOM}"
ARTIFACT_BASE_DIR="${ARTIFACT_BASE_DIR:-${ROOT_DIR}/.tmp/payment_stagea_p0_17_18}"
ARTIFACT_DIR="${ARTIFACT_BASE_DIR}/${RUN_ID}"
ARTIFACT_LOG_DIR="${ARTIFACT_DIR}/logs"
ARTIFACT_RECONCILE_DIR="${ARTIFACT_DIR}/reconcile"
ARTIFACT_TICKET_DIR="${ARTIFACT_DIR}/tickets"

BIZ_DATE="${BIZ_DATE:-$(date -d 'yesterday' +%F)}"
STALE_MINUTES="${STALE_MINUTES:-10}"
EMIT_ON_WARN="${EMIT_ON_WARN:-0}"
ENABLE_NOTIFY="${ENABLE_NOTIFY:-0}"
NOTIFY_STRICT="${NOTIFY_STRICT:-0}"
NOTIFY_DRY_RUN="${NOTIFY_DRY_RUN:-1}"
RUN_STORE_SKU_STOCK_GATE="${RUN_STORE_SKU_STOCK_GATE:-0}"
REQUIRE_STORE_SKU_STOCK_GATE="${REQUIRE_STORE_SKU_STOCK_GATE:-0}"
REQUIRE_NAMING_GUARD="${REQUIRE_NAMING_GUARD:-1}"
REQUIRE_MEMORY_GUARD="${REQUIRE_MEMORY_GUARD:-1}"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_NAME="${DB_NAME:-hxy_dev}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-}"
FEISHU_WEBHOOK_URL="${FEISHU_WEBHOOK_URL:-}"
WEBHOOK_URL="${WEBHOOK_URL:-}"

for flag in "${RUN_STORE_SKU_STOCK_GATE}" "${REQUIRE_STORE_SKU_STOCK_GATE}" "${REQUIRE_NAMING_GUARD}" "${REQUIRE_MEMORY_GUARD}"; do
  if ! [[ "${flag}" =~ ^[01]$ ]]; then
    echo "Invalid gate flag value: ${flag} (expect 0/1)" >&2
    exit 1
  fi
done

ARTIFACT_RUN_LOG="${ARTIFACT_LOG_DIR}/run.log"
ARTIFACT_CHECK_LOG="${ARTIFACT_LOG_DIR}/reconcile_check.log"
ARTIFACT_FINAL_GATE_LOG="${ARTIFACT_LOG_DIR}/final_gate.log"
ARTIFACT_SUMMARY_FILE="${ARTIFACT_DIR}/summary.txt"
ARTIFACT_RECON_SUMMARY_FILE="${ARTIFACT_RECONCILE_DIR}/summary.txt"
ARTIFACT_ISSUES_TSV="${ARTIFACT_RECONCILE_DIR}/issues.tsv"
ARTIFACT_REPORT_FILE="${ARTIFACT_DIR}/payment_reconcile_report.md"
ARTIFACT_TICKET_TSV="${ARTIFACT_TICKET_DIR}/tickets.tsv"
ARTIFACT_TICKET_SUMMARY_FILE="${ARTIFACT_TICKET_DIR}/ticket_summary.txt"
ARTIFACT_NOTIFY_SUMMARY_FILE="${ARTIFACT_TICKET_DIR}/notify_summary.txt"
ARTIFACT_NAMING_GUARD_LOG="${ARTIFACT_LOG_DIR}/naming_guard.log"
ARTIFACT_MEMORY_GUARD_LOG="${ARTIFACT_LOG_DIR}/memory_guard.log"
ARTIFACT_STORE_SKU_STOCK_DIR="${ARTIFACT_DIR}/store_sku_stock"
ARTIFACT_STORE_SKU_STOCK_SUMMARY_FILE="${ARTIFACT_STORE_SKU_STOCK_DIR}/summary.txt"
ARTIFACT_STORE_SKU_STOCK_TSV="${ARTIFACT_STORE_SKU_STOCK_DIR}/result.tsv"

mkdir -p "${ARTIFACT_LOG_DIR}" "${ARTIFACT_RECONCILE_DIR}" "${ARTIFACT_TICKET_DIR}" "${ARTIFACT_STORE_SKU_STOCK_DIR}"
exec > >(tee -a "${ARTIFACT_RUN_LOG}") 2>&1

check_rc="unknown"
notify_rc="unknown"
store_sku_stock_gate_rc="SKIP"
naming_guard_rc="SKIP"
memory_guard_rc="SKIP"

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

finalize() {
  local rc=$?
  local pipeline_rc="${PIPELINE_EXIT_CODE:-$rc}"
  local reconcile_result
  local issue_total
  local issue_block_count
  local issue_warn_count
  local ticket_total
  local block_ticket_total
  local warn_ticket_total

  reconcile_result="$(kv "${ARTIFACT_RECON_SUMMARY_FILE}" reconcile_result)"
  issue_total="$(kv "${ARTIFACT_RECON_SUMMARY_FILE}" issue_total)"
  issue_block_count="$(kv "${ARTIFACT_RECON_SUMMARY_FILE}" issue_block_count)"
  issue_warn_count="$(kv "${ARTIFACT_RECON_SUMMARY_FILE}" issue_warn_count)"
  ticket_total="$(kv "${ARTIFACT_TICKET_SUMMARY_FILE}" ticket_total)"
  block_ticket_total="$(kv "${ARTIFACT_TICKET_SUMMARY_FILE}" block_ticket_total)"
  warn_ticket_total="$(kv "${ARTIFACT_TICKET_SUMMARY_FILE}" warn_ticket_total)"

  [[ -z "${reconcile_result}" ]] && reconcile_result="unknown"
  [[ -z "${issue_total}" ]] && issue_total="0"
  [[ -z "${issue_block_count}" ]] && issue_block_count="0"
  [[ -z "${issue_warn_count}" ]] && issue_warn_count="0"
  [[ -z "${ticket_total}" ]] && ticket_total="0"
  [[ -z "${block_ticket_total}" ]] && block_ticket_total="0"
  [[ -z "${warn_ticket_total}" ]] && warn_ticket_total="0"

  {
    echo "run_id=${RUN_ID}"
    echo "biz_date=${BIZ_DATE}"
    echo "stale_minutes=${STALE_MINUTES}"
    echo "emit_on_warn=${EMIT_ON_WARN}"
    echo "enable_notify=${ENABLE_NOTIFY}"
    echo "notify_dry_run=${NOTIFY_DRY_RUN}"
    echo "run_store_sku_stock_gate=${RUN_STORE_SKU_STOCK_GATE}"
    echo "require_store_sku_stock_gate=${REQUIRE_STORE_SKU_STOCK_GATE}"
    echo "require_naming_guard=${REQUIRE_NAMING_GUARD}"
    echo "require_memory_guard=${REQUIRE_MEMORY_GUARD}"
    echo "check_exit_code=${check_rc}"
    echo "notify_exit_code=${notify_rc}"
    echo "store_sku_stock_gate_rc=${store_sku_stock_gate_rc}"
    echo "naming_guard_rc=${naming_guard_rc}"
    echo "memory_guard_rc=${memory_guard_rc}"
    echo "reconcile_result=${reconcile_result}"
    echo "issue_total=${issue_total}"
    echo "issue_block_count=${issue_block_count}"
    echo "issue_warn_count=${issue_warn_count}"
    echo "ticket_total=${ticket_total}"
    echo "block_ticket_total=${block_ticket_total}"
    echo "warn_ticket_total=${warn_ticket_total}"
    echo "pipeline_exit_code=${pipeline_rc}"
    echo "run_log=${ARTIFACT_RUN_LOG}"
    echo "check_log=${ARTIFACT_CHECK_LOG}"
    echo "final_gate_log=${ARTIFACT_FINAL_GATE_LOG}"
    echo "reconcile_summary=${ARTIFACT_RECON_SUMMARY_FILE}"
    echo "issues_tsv=${ARTIFACT_ISSUES_TSV}"
    echo "report_file=${ARTIFACT_REPORT_FILE}"
    echo "ticket_tsv=${ARTIFACT_TICKET_TSV}"
    echo "ticket_summary_file=${ARTIFACT_TICKET_SUMMARY_FILE}"
    echo "notify_summary_file=${ARTIFACT_NOTIFY_SUMMARY_FILE}"
    echo "naming_guard_log=${ARTIFACT_NAMING_GUARD_LOG}"
    echo "memory_guard_log=${ARTIFACT_MEMORY_GUARD_LOG}"
    echo "store_sku_stock_gate_summary_file=${ARTIFACT_STORE_SKU_STOCK_SUMMARY_FILE}"
    echo "store_sku_stock_gate_tsv=${ARTIFACT_STORE_SKU_STOCK_TSV}"
  } > "${ARTIFACT_SUMMARY_FILE}"

  {
    echo "payment_reconcile_daily_gate"
    echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "pipeline_exit_code=${pipeline_rc}"
    if [[ "${pipeline_rc}" == "0" ]]; then
      echo "decision=PASS"
    else
      echo "decision=BLOCK"
      echo "---- reconcile_check.log tail (last 200 lines) ----"
      tail -n 200 "${ARTIFACT_CHECK_LOG}" || true
      if [[ -f "${ARTIFACT_STORE_SKU_STOCK_SUMMARY_FILE}" ]]; then
        echo "---- store_sku_stock_gate summary ----"
        cat "${ARTIFACT_STORE_SKU_STOCK_SUMMARY_FILE}" || true
      fi
    fi
    echo "run_log=${ARTIFACT_RUN_LOG}"
  } > "${ARTIFACT_FINAL_GATE_LOG}"

  set +e
  bash script/dev/payment_reconcile_artifact_index.sh \
    --out-dir "${ARTIFACT_DIR}" \
    --summary-file "${ARTIFACT_SUMMARY_FILE}" \
    --check-log "${ARTIFACT_CHECK_LOG}" \
    --report-file "${ARTIFACT_REPORT_FILE}" \
    --issues-tsv "${ARTIFACT_ISSUES_TSV}" \
    --ticket-dir "${ARTIFACT_TICKET_DIR}" \
    --ticket-tsv "${ARTIFACT_TICKET_TSV}" \
    --ticket-summary-file "${ARTIFACT_TICKET_SUMMARY_FILE}" \
    --notify-summary-file "${ARTIFACT_NOTIFY_SUMMARY_FILE}" \
    --final-gate-log "${ARTIFACT_FINAL_GATE_LOG}" >/dev/null
  set -e

  echo "[stageA-p0-17-18] artifact_dir=${ARTIFACT_DIR}"
  echo "[stageA-p0-17-18] summary=${ARTIFACT_SUMMARY_FILE}"
  return "${pipeline_rc}"
  }

trap finalize EXIT

echo "[stageA-p0-17-18] step=naming-guard"
set +e
bash script/dev/check_hxy_naming_guard.sh > "${ARTIFACT_NAMING_GUARD_LOG}" 2>&1
naming_guard_rc=$?
set -e
if [[ "${naming_guard_rc}" != "0" ]]; then
  if [[ "${REQUIRE_NAMING_GUARD}" == "1" ]]; then
    echo "[stageA-p0-17-18] result=BLOCK (naming guard rc=${naming_guard_rc})" >&2
    PIPELINE_EXIT_CODE=2
    exit 2
  fi
  echo "[stageA-p0-17-18] warn: naming guard failed rc=${naming_guard_rc}, continue (require=0)"
fi

echo "[stageA-p0-17-18] step=memory-guard"
set +e
if [[ -n "${GIT_DIFF_RANGE:-}" ]]; then
  CHECK_STAGED=0 \
  CHECK_UNSTAGED=0 \
  CHECK_UNTRACKED=0 \
  GIT_DIFF_RANGE="${GIT_DIFF_RANGE}" \
  bash script/dev/check_hxy_memory_guard.sh > "${ARTIFACT_MEMORY_GUARD_LOG}" 2>&1
else
  CHECK_STAGED=1 \
  CHECK_UNSTAGED=0 \
  CHECK_UNTRACKED=0 \
  bash script/dev/check_hxy_memory_guard.sh > "${ARTIFACT_MEMORY_GUARD_LOG}" 2>&1
fi
memory_guard_rc=$?
set -e
if [[ "${memory_guard_rc}" != "0" ]]; then
  if [[ "${REQUIRE_MEMORY_GUARD}" == "1" ]]; then
    echo "[stageA-p0-17-18] result=BLOCK (memory guard rc=${memory_guard_rc})" >&2
    PIPELINE_EXIT_CODE=2
    exit 2
  fi
  echo "[stageA-p0-17-18] warn: memory guard failed rc=${memory_guard_rc}, continue (require=0)"
fi

echo "[stageA-p0-17-18] step=payment-reconcile-check"
set +e
check_cmd=(
  bash script/dev/check_payment_reconcile_daily.sh
  --biz-date "${BIZ_DATE}"
  --stale-minutes "${STALE_MINUTES}"
  --db-host "${DB_HOST}"
  --db-port "${DB_PORT}"
  --db-user "${DB_USER}"
  --db-password "${DB_PASSWORD}"
  --db-name "${DB_NAME}"
  --issues-tsv "${ARTIFACT_ISSUES_TSV}"
  --summary-file "${ARTIFACT_RECON_SUMMARY_FILE}"
)
if [[ -n "${MYSQL_DEFAULTS_FILE}" ]]; then
  check_cmd+=(--mysql-defaults-file "${MYSQL_DEFAULTS_FILE}")
fi
"${check_cmd[@]}" 2>&1 | tee "${ARTIFACT_CHECK_LOG}"
check_rc=${PIPESTATUS[0]}
set -e

if [[ "${check_rc}" != "0" && "${check_rc}" != "2" ]]; then
  echo "[stageA-p0-17-18] reconcile check execution failed: rc=${check_rc}" >&2
  PIPELINE_EXIT_CODE="${check_rc}"
  exit "${check_rc}"
fi

echo "[stageA-p0-17-18] step=reconcile-report"
bash script/dev/generate_payment_reconcile_report.sh \
  --summary-file "${ARTIFACT_RECON_SUMMARY_FILE}" \
  --issues-tsv "${ARTIFACT_ISSUES_TSV}" \
  --output-file "${ARTIFACT_REPORT_FILE}"

echo "[stageA-p0-17-18] step=reconcile-ticketize"
bash script/dev/create_payment_reconcile_diff_ticket.sh \
  --issues-tsv "${ARTIFACT_ISSUES_TSV}" \
  --summary-file "${ARTIFACT_RECON_SUMMARY_FILE}" \
  --ticket-dir "${ARTIFACT_TICKET_DIR}" \
  --output-tsv "${ARTIFACT_TICKET_TSV}" \
  --ticket-summary-file "${ARTIFACT_TICKET_SUMMARY_FILE}" \
  --emit-on-warn "${EMIT_ON_WARN}"

if [[ "${ENABLE_NOTIFY}" == "1" ]]; then
  echo "[stageA-p0-17-18] step=reconcile-notify"
  set +e
  bash script/dev/notify_payment_reconcile_tickets.sh \
    --ticket-tsv "${ARTIFACT_TICKET_TSV}" \
    --summary-file "${ARTIFACT_RECON_SUMMARY_FILE}" \
    --report-file "${ARTIFACT_REPORT_FILE}" \
    --artifact-index "${ARTIFACT_DIR}/artifact_index.md" \
    --feishu-webhook-url "${FEISHU_WEBHOOK_URL}" \
    --webhook-url "${WEBHOOK_URL}" \
    --notify-summary-file "${ARTIFACT_NOTIFY_SUMMARY_FILE}" \
    $( [[ "${NOTIFY_DRY_RUN}" == "1" ]] && echo "--dry-run" ) \
    $( [[ "${NOTIFY_STRICT}" == "1" ]] && echo "--strict" )
  notify_rc=$?
  set -e
  if [[ "${notify_rc}" != "0" && "${NOTIFY_STRICT}" == "1" ]]; then
    PIPELINE_EXIT_CODE="${notify_rc}"
    exit "${notify_rc}"
  fi
else
  echo "[stageA-p0-17-18] skip step=reconcile-notify"
  {
    echo "notify_at=$(date '+%Y-%m-%d %H:%M:%S')"
    echo "notify_status=SKIP"
    echo "reason=notify_disabled"
  } > "${ARTIFACT_NOTIFY_SUMMARY_FILE}"
fi

if [[ "${RUN_STORE_SKU_STOCK_GATE}" == "1" ]]; then
  echo "[stageA-p0-17-18] step=store-sku-stock-gate"
  set +e
  bash script/dev/check_store_sku_stock_gate.sh \
    --db-host "${DB_HOST}" \
    --db-port "${DB_PORT}" \
    --db-user "${DB_USER}" \
    --db-password "${DB_PASSWORD}" \
    --db-name "${DB_NAME}" \
    --require-overdue-zero "${REQUIRE_STORE_SKU_STOCK_GATE}" \
    --summary-file "${ARTIFACT_STORE_SKU_STOCK_SUMMARY_FILE}" \
    --output-tsv "${ARTIFACT_STORE_SKU_STOCK_TSV}"
  store_sku_stock_gate_rc=$?
  set -e
  if [[ "${store_sku_stock_gate_rc}" != "0" && "${store_sku_stock_gate_rc}" != "2" ]]; then
    PIPELINE_EXIT_CODE="${store_sku_stock_gate_rc}"
    exit "${store_sku_stock_gate_rc}"
  fi
  if [[ "${store_sku_stock_gate_rc}" == "2" ]]; then
    echo "[stageA-p0-17-18] result=BLOCK (store sku stock gate blocked)"
    PIPELINE_EXIT_CODE=2
    exit 2
  fi
else
  echo "[stageA-p0-17-18] skip step=store-sku-stock-gate"
fi

if [[ "${check_rc}" == "2" ]]; then
  echo "[stageA-p0-17-18] result=BLOCK (has reconcile BLOCK issues)"
  PIPELINE_EXIT_CODE=2
  exit 2
fi

echo "[stageA-p0-17-18] result=PASS"
PIPELINE_EXIT_CODE=0
exit 0
