#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

RUN_ID="${RUN_ID:-$(date +%Y%m%d_%H%M%S)_$RANDOM}"
ARTIFACT_BASE_DIR="${ARTIFACT_BASE_DIR:-${ROOT_DIR}/.tmp/non_payment_secondary_baseline}"
ARTIFACT_DIR="${ARTIFACT_BASE_DIR}/${RUN_ID}"
RUN_CLEAN="${RUN_CLEAN:-0}"
BYTE_BUDDY_EXPERIMENTAL="${BYTE_BUDDY_EXPERIMENTAL:-1}"
EXTRA_MVN_ARGS_RAW="${EXTRA_MVN_ARGS_RAW:-}"
VERIFY_RUNTIME="${VERIFY_RUNTIME:-0}"
VERIFY_STAGE="${VERIFY_STAGE:-full-compat}"
VERIFY_BASE_URL="${VERIFY_BASE_URL:-http://127.0.0.1:48080}"
VERIFY_TIMEOUT_SECONDS="${VERIFY_TIMEOUT_SECONDS:-8}"

ARTIFACT_LOG_DIR="${ARTIFACT_DIR}/logs"
ARTIFACT_SUREFIRE_DIR="${ARTIFACT_DIR}/surefire-reports"
ARTIFACT_SUMMARY_FILE="${ARTIFACT_DIR}/summary.txt"
ARTIFACT_RUN_LOG="${ARTIFACT_LOG_DIR}/run.log"
ARTIFACT_RUNTIME_LOG="${ARTIFACT_LOG_DIR}/runtime_verify.log"
ARTIFACT_INDEX_FILE="${ARTIFACT_DIR}/artifact_index.md"

usage() {
  cat <<'EOF'
Usage:
  script/dev/run_non_payment_secondary_baseline.sh

Env:
  RUN_ID                       执行 ID（默认时间戳）
  ARTIFACT_BASE_DIR            产物根目录（默认 .tmp/non_payment_secondary_baseline）
  RUN_CLEAN=0|1                是否 clean test（默认 0）
  BYTE_BUDDY_EXPERIMENTAL=0|1  是否加 ByteBuddy 参数（默认 1）
  EXTRA_MVN_ARGS_RAW           额外 Maven 参数
  VERIFY_RUNTIME=0|1           是否执行运行时路径校验（默认 0）
  VERIFY_STAGE                 full-compat|payment-core-only|disabled（默认 full-compat）
  VERIFY_BASE_URL              运行时校验 base url（默认 http://127.0.0.1:48080）
  VERIFY_TIMEOUT_SECONDS       运行时校验超时（默认 8）
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

mkdir -p "${ARTIFACT_LOG_DIR}" "${ARTIFACT_SUREFIRE_DIR}"
exec > >(tee -a "${ARTIFACT_RUN_LOG}") 2>&1

finalize_artifacts() {
  local rc=$?
  local surefire_report_count
  local runtime_verify_rc="${runtime_verify_rc:-0}"
  local runtime_verify_expectation="${runtime_verify_expectation:-skipped}"

  while IFS= read -r report_file; do
    cp --parents "${report_file}" "${ARTIFACT_SUREFIRE_DIR}/" 2>/dev/null || true
  done < <(find yudao-module-member yudao-module-mall/yudao-module-trade -type f -path "*/target/surefire-reports/*" 2>/dev/null)
  surefire_report_count="$(find "${ARTIFACT_SUREFIRE_DIR}" -type f | wc -l | tr -d '[:space:]')"

  {
    echo "run_id=${RUN_ID}"
    echo "run_clean=${RUN_CLEAN}"
    echo "byte_buddy_experimental=${BYTE_BUDDY_EXPERIMENTAL}"
    echo "extra_mvn_args=${EXTRA_MVN_ARGS_RAW}"
    echo "verify_runtime=${VERIFY_RUNTIME}"
    echo "verify_stage=${VERIFY_STAGE}"
    echo "verify_base_url=${VERIFY_BASE_URL}"
    echo "verify_timeout_seconds=${VERIFY_TIMEOUT_SECONDS}"
    echo "runtime_verify_expectation=${runtime_verify_expectation}"
    echo "runtime_verify_rc=${runtime_verify_rc}"
    echo "pipeline_exit_code=${rc}"
    echo "log_file=${ARTIFACT_RUN_LOG}"
    echo "runtime_log=${ARTIFACT_RUNTIME_LOG}"
    echo "surefire_report_count=${surefire_report_count}"
  } > "${ARTIFACT_SUMMARY_FILE}"

  {
    echo "# Non-Payment Secondary Baseline"
    echo
    echo "- run_id: \`${RUN_ID}\`"
    echo "- pipeline_exit_code: \`${rc}\`"
    echo "- runtime_verify_rc: \`${runtime_verify_rc}\`"
    echo "- verify_stage: \`${VERIFY_STAGE}\`"
    echo
    echo "## Files"
    echo
    echo "- summary: \`summary.txt\`"
    echo "- run log: \`logs/run.log\`"
    echo "- runtime verify log: \`logs/runtime_verify.log\`"
    echo "- surefire reports: \`surefire-reports/\`"
  } > "${ARTIFACT_INDEX_FILE}"

  echo "[non-payment-baseline] artifact_dir=${ARTIFACT_DIR}"
  echo "[non-payment-baseline] summary=${ARTIFACT_SUMMARY_FILE}"
  return "${rc}"
}

trap finalize_artifacts EXIT

case "${VERIFY_STAGE}" in
  full-compat|payment-core-only|disabled)
    ;;
  *)
    echo "[non-payment-baseline] invalid VERIFY_STAGE=${VERIFY_STAGE}" >&2
    exit 1
    ;;
esac

mvn_cmd=(mvn)
if [[ "${RUN_CLEAN}" == "1" ]]; then
  mvn_cmd+=(clean)
fi
mvn_cmd+=(
  -pl yudao-module-member,yudao-module-mall/yudao-module-trade
  -am
  test
  -Dtest=CrmebFrontWechatCompatControllerTest,CrmebFrontOrderCompatControllerTest,CrmebAdminOrderCompatControllerTest,CrmebAdminStoreOrderCompatControllerTest
  -Dsurefire.failIfNoSpecifiedTests=false
)
if [[ "${BYTE_BUDDY_EXPERIMENTAL}" == "1" ]]; then
  mvn_cmd+=(-DargLine=-Dnet.bytebuddy.experimental=true)
fi
if [[ -n "${EXTRA_MVN_ARGS_RAW}" ]]; then
  read -r -a extra_args <<< "${EXTRA_MVN_ARGS_RAW}"
  mvn_cmd+=("${extra_args[@]}")
fi

echo "[non-payment-baseline] step=module-tests"
echo "[non-payment-baseline] command=${mvn_cmd[*]}"
"${mvn_cmd[@]}"

runtime_verify_rc=0
runtime_verify_expectation="skipped"
if [[ "${VERIFY_RUNTIME}" == "1" ]]; then
  NON_PAYMENT_PATHS=(
    "/api/front/order/list"
    "/api/front/order/detail/1"
    "/api/front/order/data"
    "/api/front/recharge/index"
    "/api/front/recharge/bill/record?type=all&page=1&limit=10"
    "/api/admin/store/order/list"
    "/api/admin/store/order/info?orderNo=smoke_order_no"
    "/api/admin/store/order/refund/ticket/list"
  )
  expected_csv=""
  for path in "${NON_PAYMENT_PATHS[@]}"; do
    if [[ -z "${expected_csv}" ]]; then
      expected_csv="${path}"
    else
      expected_csv="${expected_csv},${path}"
    fi
  done

  verify_cmd=(bash script/dev/verify_crmeb_cutover_paths.sh --base-url "${VERIFY_BASE_URL}" --timeout-seconds "${VERIFY_TIMEOUT_SECONDS}")
  if [[ "${VERIFY_STAGE}" == "full-compat" ]]; then
    runtime_verify_expectation="enabled_non_410"
    verify_cmd+=(--expect-enabled "${expected_csv}")
  else
    runtime_verify_expectation="disabled_410"
    verify_cmd+=(--expect-disabled "${expected_csv}")
  fi

  echo "[non-payment-baseline] step=runtime-verify stage=${VERIFY_STAGE}" | tee "${ARTIFACT_RUNTIME_LOG}"
  echo "[non-payment-baseline] command=${verify_cmd[*]}" | tee -a "${ARTIFACT_RUNTIME_LOG}"
  set +e
  "${verify_cmd[@]}" 2>&1 | tee -a "${ARTIFACT_RUNTIME_LOG}"
  runtime_verify_rc=$?
  set -e
  if [[ "${runtime_verify_rc}" -ne 0 ]]; then
    echo "[non-payment-baseline] runtime verify failed: rc=${runtime_verify_rc}" >&2
    exit "${runtime_verify_rc}"
  fi
else
  echo "[non-payment-baseline] skip runtime verify"
fi

echo "[non-payment-baseline] result=PASS"
