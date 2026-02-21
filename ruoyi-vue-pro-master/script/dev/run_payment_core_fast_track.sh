#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

RUN_NOTIFY_SMOKE="${RUN_NOTIFY_SMOKE:-1}"
VERIFY_FAST_TRACK="${VERIFY_FAST_TRACK:-0}"
VERIFY_BASE_URL="${VERIFY_BASE_URL:-http://127.0.0.1:48080}"
VERIFY_TIMEOUT_SECONDS="${VERIFY_TIMEOUT_SECONDS:-8}"
RUN_RESILIENCE_REGRESSION="${RUN_RESILIENCE_REGRESSION:-1}"
RUN_CLEAN="${RUN_CLEAN:-0}"
BYTE_BUDDY_EXPERIMENTAL="${BYTE_BUDDY_EXPERIMENTAL:-1}"
EXTRA_MVN_ARGS_RAW="${EXTRA_MVN_ARGS_RAW:-}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d_%H%M%S)_$$}"
ARTIFACT_BASE_DIR="${ARTIFACT_BASE_DIR:-${ROOT_DIR}/.tmp/payment_core_fast_track}"
ARTIFACT_DIR="${ARTIFACT_DIR:-${ARTIFACT_BASE_DIR}/${RUN_ID}}"
ARTIFACT_LOG_DIR="${ARTIFACT_DIR}/logs"
ARTIFACT_SUREFIRE_DIR="${ARTIFACT_DIR}/surefire-reports"
ARTIFACT_SUMMARY_FILE="${ARTIFACT_DIR}/summary.txt"
ARTIFACT_RUN_LOG="${ARTIFACT_LOG_DIR}/run.log"
ARTIFACT_FINAL_GATE_LOG="${ARTIFACT_LOG_DIR}/final_gate.log"

mkdir -p "${ARTIFACT_LOG_DIR}" "${ARTIFACT_SUREFIRE_DIR}"
exec > >(tee -a "${ARTIFACT_RUN_LOG}") 2>&1

finalize_artifacts() {
  local rc=$?
  local latest_smoke_dir=""
  local copied_smoke_dir=""
  local surefire_report_count
  local index_cmd

  if [[ "${RUN_NOTIFY_SMOKE}" == "1" ]]; then
    latest_smoke_dir="$(ls -1dt .tmp/pay_notify_pipeline_smoke/* 2>/dev/null | head -n 1 || true)"
    if [[ -n "${latest_smoke_dir}" && -d "${latest_smoke_dir}" ]]; then
      mkdir -p "${ARTIFACT_DIR}/pay-notify-smoke"
      cp -R "${latest_smoke_dir}" "${ARTIFACT_DIR}/pay-notify-smoke/"
      copied_smoke_dir="${ARTIFACT_DIR}/pay-notify-smoke/$(basename "${latest_smoke_dir}")"
    fi
  fi

  while IFS= read -r report_file; do
    cp --parents "${report_file}" "${ARTIFACT_SUREFIRE_DIR}/" 2>/dev/null || true
  done < <(find . -type f -path "*/target/surefire-reports/*")
  surefire_report_count="$(find "${ARTIFACT_SUREFIRE_DIR}" -type f | wc -l | tr -d '[:space:]')"

  {
    echo "run_id=${RUN_ID}"
    echo "run_notify_smoke=${RUN_NOTIFY_SMOKE}"
    echo "run_resilience_regression=${RUN_RESILIENCE_REGRESSION}"
    echo "run_clean=${RUN_CLEAN}"
    echo "extra_mvn_args=${EXTRA_MVN_ARGS_RAW}"
    echo "verify_fast_track=${VERIFY_FAST_TRACK}"
    echo "verify_base_url=${VERIFY_BASE_URL}"
    echo "verify_timeout_seconds=${VERIFY_TIMEOUT_SECONDS}"
    echo "pipeline_exit_code=${rc}"
    echo "log_file=${ARTIFACT_RUN_LOG}"
    echo "final_gate_log=${ARTIFACT_FINAL_GATE_LOG}"
    echo "surefire_report_count=${surefire_report_count}"
    echo "pay_notify_smoke_dir=${latest_smoke_dir}"
    echo "pay_notify_smoke_artifact_dir=${copied_smoke_dir}"
  } > "${ARTIFACT_SUMMARY_FILE}"

  {
    echo "payment_core_fast_track_local_gate"
    echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "pipeline_exit_code=${rc}"
    if [[ "${rc}" == "0" ]]; then
      echo "decision=PASS"
    else
      echo "decision=BLOCK"
      echo "---- run.log tail (last 200 lines) ----"
      tail -n 200 "${ARTIFACT_RUN_LOG}" || true
    fi
    echo "run_log=${ARTIFACT_RUN_LOG}"
  } > "${ARTIFACT_FINAL_GATE_LOG}"

  index_cmd=(
    bash script/dev/payment_core_fast_track_artifact_index.sh
    --out-dir "${ARTIFACT_DIR}"
    --summary-file "${ARTIFACT_SUMMARY_FILE}"
    --run-log "${ARTIFACT_RUN_LOG}"
    --final-gate-log "${ARTIFACT_FINAL_GATE_LOG}"
    --surefire-dir "${ARTIFACT_SUREFIRE_DIR}"
  )
  if [[ -n "${copied_smoke_dir}" ]]; then
    index_cmd+=(--notify-smoke-dir "${copied_smoke_dir}")
  fi
  "${index_cmd[@]}" >/dev/null

  echo "[fast-track] artifact_dir=${ARTIFACT_DIR}"
  echo "[fast-track] artifact_summary=${ARTIFACT_SUMMARY_FILE}"
  return "${rc}"
}

trap finalize_artifacts EXIT

echo "[fast-track] start payment-core migration gate"
echo "[fast-track] run_notify_smoke=${RUN_NOTIFY_SMOKE}"
echo "[fast-track] verify_fast_track=${VERIFY_FAST_TRACK}"
echo "[fast-track] run_resilience_regression=${RUN_RESILIENCE_REGRESSION}"
echo "[fast-track] step=crmeb-compat-regression (payment-only)"
FAST_TRACK_PAYMENT_ONLY=1 \
RUN_UNSTABLE_SUITES=0 \
RUN_CLEAN="${RUN_CLEAN}" \
BYTE_BUDDY_EXPERIMENTAL="${BYTE_BUDDY_EXPERIMENTAL}" \
EXTRA_MVN_ARGS_RAW="${EXTRA_MVN_ARGS_RAW}" \
bash script/dev/run_crmeb_compat_regression.sh

if [[ "${RUN_RESILIENCE_REGRESSION}" == "1" ]]; then
  echo "[fast-track] step=pay-resilience-regression"
  RUN_CLEAN="${RUN_CLEAN}" \
  BYTE_BUDDY_EXPERIMENTAL="${BYTE_BUDDY_EXPERIMENTAL}" \
  EXTRA_MVN_ARGS_RAW="${EXTRA_MVN_ARGS_RAW}" \
  bash script/dev/run_payment_resilience_regression.sh
else
  echo "[fast-track] skip step=pay-resilience-regression"
fi

if [[ "${RUN_NOTIFY_SMOKE}" == "1" ]]; then
  echo "[fast-track] step=pay-notify-pipeline-smoke"
  bash script/dev/pay_notify_pipeline_smoke.sh
else
  echo "[fast-track] skip step=pay-notify-pipeline-smoke"
fi

if [[ "${VERIFY_FAST_TRACK}" == "1" ]]; then
  echo "[fast-track] step=verify-payment-core-fast-track"
  bash script/dev/verify_payment_core_fast_track.sh \
    --base-url "${VERIFY_BASE_URL}" \
    --timeout-seconds "${VERIFY_TIMEOUT_SECONDS}"
else
  echo "[fast-track] skip step=verify-payment-core-fast-track"
fi

echo "[fast-track] result=PASS"
