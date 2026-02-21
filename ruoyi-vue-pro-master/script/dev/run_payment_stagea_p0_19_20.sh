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
  RUN_RECONCILE_CHECK=0|1               #19 是否直接跑对账检查（默认 0）
  RECONCILE_SUMMARY_FILE=<file>         #17 产物 summary（默认自动发现最新）
  ROLLBACK_PLAN_FILE=<file>             回滚方案文档（默认 ../hxy/支付系统-技术架构说明-RuoYi版-2026-02-21.md）
  ACCEPTANCE_PLAN_FILE=<file>           验收标准文档（默认 ../hxy/支付系统-30项开发计划-执行版-2026-02-18.md）
  REQUIRE_SCENARIO=0|1                  #20 缺少 #19 产物是否阻断（默认 1）
  REQUIRE_RECONCILE=0|1                 #20 缺少 #17 产物是否阻断（默认 1）
  REQUIRE_OPS_STATUS=0|1                #20 缺少 ops 状态是否阻断（默认 0）
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
RUN_RECONCILE_CHECK="${RUN_RECONCILE_CHECK:-0}"

RECONCILE_SUMMARY_FILE="${RECONCILE_SUMMARY_FILE:-}"
ROLLBACK_PLAN_FILE="${ROLLBACK_PLAN_FILE:-${ROOT_DIR}/../hxy/支付系统-技术架构说明-RuoYi版-2026-02-21.md}"
ACCEPTANCE_PLAN_FILE="${ACCEPTANCE_PLAN_FILE:-${ROOT_DIR}/../hxy/支付系统-30项开发计划-执行版-2026-02-18.md}"
REQUIRE_SCENARIO="${REQUIRE_SCENARIO:-1}"
REQUIRE_RECONCILE="${REQUIRE_RECONCILE:-1}"
REQUIRE_OPS_STATUS="${REQUIRE_OPS_STATUS:-0}"
OPS_STATUS_FILE="${OPS_STATUS_FILE:-}"
MAX_ARTIFACT_AGE_MINUTES="${MAX_ARTIFACT_AGE_MINUTES:-1440}"

RUN_LOG="${LOG_DIR}/run.log"
FINAL_GATE_LOG="${LOG_DIR}/final_gate.log"
SUMMARY_FILE="${ARTIFACT_DIR}/summary.txt"
INDEX_FILE="${ARTIFACT_DIR}/artifact_index.md"

SCENARIO_OUT_DIR="${ARTIFACT_DIR}/scenario-replay"
SCENARIO_SUMMARY_FILE="${SCENARIO_OUT_DIR}/summary.txt"
SCENARIO_REPORT_FILE="${SCENARIO_OUT_DIR}/report.md"
SCENARIO_TSV="${SCENARIO_OUT_DIR}/scenario_result.tsv"

GATE_TSV="${ARTIFACT_DIR}/release_gate.tsv"
GATE_REPORT="${ARTIFACT_DIR}/release_gate_report.md"

mkdir -p "${LOG_DIR}" "${SCENARIO_OUT_DIR}"
exec > >(tee -a "${RUN_LOG}") 2>&1

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
    echo "gate_rc=${gate_rc}"
    echo "gate_result=${gate_result}"
    echo "gate_block_count=${block_count}"
    echo "gate_warn_count=${warn_count}"
    echo "reconcile_summary_file=${RECONCILE_SUMMARY_FILE}"
    echo "rollback_plan_file=${ROLLBACK_PLAN_FILE}"
    echo "acceptance_plan_file=${ACCEPTANCE_PLAN_FILE}"
    echo "run_log=${RUN_LOG}"
    echo "final_gate_log=${FINAL_GATE_LOG}"
    echo "scenario_summary_file=${SCENARIO_SUMMARY_FILE}"
    echo "scenario_report_file=${SCENARIO_REPORT_FILE}"
    echo "scenario_tsv=${SCENARIO_TSV}"
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
    echo "- scenario summary: \`scenario-replay/summary.txt\`"
    echo "- scenario report: \`scenario-replay/report.md\`"
    echo "- scenario tsv: \`scenario-replay/scenario_result.tsv\`"
    echo "- release gate tsv: \`release_gate.tsv\`"
    echo "- release gate report: \`release_gate_report.md\`"
  } > "${INDEX_FILE}"

  echo "[stageA-p0-19-20] artifact_dir=${ARTIFACT_DIR}"
  echo "[stageA-p0-19-20] summary=${SUMMARY_FILE}"

  return "${pipeline_rc}"
}

trap finalize EXIT

resolve_reconcile_summary

echo "[stageA-p0-19-20] step=#19 abnormal scenario replay"
set +e
bash script/dev/run_payment_abnormal_scenario_replay.sh \
  --run-id "${RUN_ID}" \
  --out-base-dir "${ARTIFACT_DIR}" \
  --run-tests "${RUN_TESTS}" \
  --run-notify-smoke "${RUN_NOTIFY_SMOKE}" \
  --run-retry-policy-check "${RUN_RETRY_POLICY_CHECK}" \
  --run-partner-readiness-check "${RUN_PARTNER_READINESS_CHECK}" \
  --run-reconcile-check "${RUN_RECONCILE_CHECK}" \
  --reconcile-summary-file "${RECONCILE_SUMMARY_FILE}" \
  --db-host "${DB_HOST:-127.0.0.1}" \
  --db-port "${DB_PORT:-3306}" \
  --db-user "${DB_USER:-root}" \
  --db-password "${DB_PASSWORD:-}" \
  --db-name "${DB_NAME:-ruoyi-vue-pro}"
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

echo "[stageA-p0-19-20] step=#20 release blocker gate"
set +e
bash script/dev/check_payment_release_blockers.sh \
  --scenario-summary-file "${SCENARIO_SUMMARY_FILE}" \
  --reconcile-summary-file "${RECONCILE_SUMMARY_FILE}" \
  --rollback-plan-file "${ROLLBACK_PLAN_FILE}" \
  --acceptance-plan-file "${ACCEPTANCE_PLAN_FILE}" \
  --ops-status-file "${OPS_STATUS_FILE}" \
  --require-scenario "${REQUIRE_SCENARIO}" \
  --require-reconcile "${REQUIRE_RECONCILE}" \
  --require-ops-status "${REQUIRE_OPS_STATUS}" \
  --max-artifact-age-minutes "${MAX_ARTIFACT_AGE_MINUTES}" \
  --output-tsv "${GATE_TSV}" \
  --output-report "${GATE_REPORT}" > "${LOG_DIR}/release_gate.log" 2>&1

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
