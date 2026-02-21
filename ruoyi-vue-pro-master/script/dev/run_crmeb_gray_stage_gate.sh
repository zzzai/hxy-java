#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

STAGE="${STAGE:-full-compat}"
BASE_URL="${BASE_URL:-http://127.0.0.1:48080}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-8}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d_%H%M%S)_$RANDOM}"
ARTIFACT_BASE_DIR="${ARTIFACT_BASE_DIR:-${ROOT_DIR}/.tmp/crmeb_gray_stage_gate}"
EXTRA_EXPECT_DISABLED="${EXTRA_EXPECT_DISABLED:-}"
EXTRA_EXPECT_ENABLED="${EXTRA_EXPECT_ENABLED:-}"
VERIFY_RUOYI_BASELINE="${VERIFY_RUOYI_BASELINE:-1}"
DIAGNOSE_ENTRYPOINT="${DIAGNOSE_ENTRYPOINT:-1}"

usage() {
  cat <<'EOF'
Usage:
  script/dev/run_crmeb_gray_stage_gate.sh \
    --stage full-compat \
    --base-url http://127.0.0.1:48080

Stages:
  full-compat       全量兼容阶段：旧接口应返回非 410
  payment-core-only 支付核心阶段：仅支付核心接口放行
  disabled          完全下线阶段：旧接口应统一返回 410

Options:
  --stage VALUE                Stage value: full-compat|payment-core-only|disabled
  --base-url URL               Server base URL (default: http://127.0.0.1:48080)
  --timeout-seconds N          Curl timeout seconds (default: 8)
  --run-id ID                  Override run id
  --artifact-base-dir DIR      Artifact base dir (default: .tmp/crmeb_gray_stage_gate)
  --extra-expect-disabled CSV  Additional disabled paths
  --extra-expect-enabled CSV   Additional enabled paths
  --verify-ruoyi-baseline 0|1  Verify /admin-api + /app-api runtime baseline (default: 1)
  --diagnose-entrypoint 0|1    Generate routing diagnosis artifact (default: 1)
  -h, --help                   Show this help
EOF
}

csv_join() {
  local -n arr_ref="$1"
  local result=""
  local item=""
  for item in "${arr_ref[@]}"; do
    if [[ -z "${result}" ]]; then
      result="${item}"
    else
      result="${result},${item}"
    fi
  done
  printf '%s' "${result}"
}

append_csv_to_array() {
  local raw="$1"
  local -n arr_ref="$2"
  local item=""
  IFS=',' read -r -a items <<< "${raw}"
  for item in "${items[@]}"; do
    item="${item#"${item%%[![:space:]]*}"}"
    item="${item%"${item##*[![:space:]]}"}"
    [[ -z "${item}" ]] && continue
    arr_ref+=("${item}")
  done
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --stage)
      STAGE="${2:-}"
      shift 2
      ;;
    --base-url)
      BASE_URL="${2:-}"
      shift 2
      ;;
    --timeout-seconds)
      TIMEOUT_SECONDS="${2:-8}"
      shift 2
      ;;
    --run-id)
      RUN_ID="${2:-}"
      shift 2
      ;;
    --artifact-base-dir)
      ARTIFACT_BASE_DIR="${2:-}"
      shift 2
      ;;
    --extra-expect-disabled)
      EXTRA_EXPECT_DISABLED="${2:-}"
      shift 2
      ;;
    --extra-expect-enabled)
      EXTRA_EXPECT_ENABLED="${2:-}"
      shift 2
      ;;
    --verify-ruoyi-baseline)
      VERIFY_RUOYI_BASELINE="${2:-1}"
      shift 2
      ;;
    --diagnose-entrypoint)
      DIAGNOSE_ENTRYPOINT="${2:-1}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[gray-gate] unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

case "${STAGE}" in
  full-compat|payment-core-only|disabled)
    ;;
  *)
    echo "[gray-gate] invalid stage: ${STAGE}" >&2
    usage
    exit 1
    ;;
esac

RUN_ID="${RUN_ID//[^a-zA-Z0-9._-]/_}"
OUT_DIR="${ARTIFACT_BASE_DIR}/${RUN_ID}"
LOG_DIR="${OUT_DIR}/logs"
mkdir -p "${LOG_DIR}"
LOG_FILE="${LOG_DIR}/gray_gate.log"
SUMMARY_FILE="${OUT_DIR}/summary.txt"
INDEX_FILE="${OUT_DIR}/artifact_index.md"

echo "[gray-gate] stage=${STAGE}"
echo "[gray-gate] base_url=${BASE_URL}"
echo "[gray-gate] timeout_seconds=${TIMEOUT_SECONDS}"
echo "[gray-gate] verify_ruoyi_baseline=${VERIFY_RUOYI_BASELINE}"
echo "[gray-gate] diagnose_entrypoint=${DIAGNOSE_ENTRYPOINT}"
echo "[gray-gate] out_dir=${OUT_DIR}"

FULL_COMPAT_EXPECT_ENABLED=(
  "/api/front/order/list"
  "/api/front/order/detail/1"
  "/api/front/order/data"
  "/api/front/recharge/index"
  "/api/front/recharge/bill/record?type=all&page=1&limit=10"
  "/api/front/pay/get/config"
  "/api/admin/store/order/list"
  "/api/admin/store/order/info?orderNo=smoke_order_no"
  "/api/admin/store/order/refund/ticket/list"
  "/api/admin/system/config/check?k=pay_routine_appid"
)

FULL_DISABLED_EXPECT=(
  "/api/front/wechat/authorize/program/login"
  "/api/front/order/pre/order"
  "/api/front/order/load/pre/smoke_pre_order"
  "/api/front/order/computed/price"
  "/api/front/order/create"
  "/api/front/order/get/pay/config"
  "/api/front/order/refund"
  "/api/front/order/refund/reason"
  "/api/front/pay/payment"
  "/api/front/pay/queryPayResult?orderNo=smoke_order_no"
  "/api/front/pay/get/config"
  "/api/front/order/list"
  "/api/front/recharge/index"
  "/api/admin/payment/callback/wechat"
  "/api/admin/payment/callback/wechat/refund"
  "/api/admin/system/config/check?k=pay_routine_appid"
  "/api/admin/store/order/refund/ticket/list"
  "/api/admin/store/order/list"
)

set +e
diagnosis_rc=0
diagnosis_summary_file=""
diagnosis_index_file=""
if [[ "${DIAGNOSE_ENTRYPOINT}" == "1" ]]; then
  diag_run_id="diag_${RUN_ID}"
  set +e
  {
    echo "[gray-gate] begin entrypoint diagnosis"
    RUN_ID="${diag_run_id}" \
    ARTIFACT_BASE_DIR="${OUT_DIR}/diagnosis" \
    BASE_URL="${BASE_URL}" \
    TIMEOUT_SECONDS="${TIMEOUT_SECONDS}" \
    REQUIRE_RUOYI=0 \
    REQUIRE_HEALTH=0 \
    bash script/dev/diagnose_ruoyi_entrypoint.sh
  } 2>&1 | tee -a "${LOG_FILE}"
  diagnosis_rc=$?
  set -e
  diagnosis_summary_file="${OUT_DIR}/diagnosis/${diag_run_id}/summary.txt"
  diagnosis_index_file="${OUT_DIR}/diagnosis/${diag_run_id}/artifact_index.md"
else
  echo "[gray-gate] skip entrypoint diagnosis" | tee -a "${LOG_FILE}"
fi

set +e
{
  echo "[gray-gate] begin stage check"
  if [[ "${VERIFY_RUOYI_BASELINE}" == "1" ]]; then
    set +e
    bash script/dev/verify_ruoyi_runtime_baseline.sh \
      --base-url "${BASE_URL}" \
      --timeout-seconds "${TIMEOUT_SECONDS}"
    baseline_rc=$?
    set -e
    if [[ "${baseline_rc}" -ne 0 ]]; then
      echo "[gray-gate] ruoyi runtime baseline failed: rc=${baseline_rc}" >&2
      exit "${baseline_rc}"
    fi
  else
    echo "[gray-gate] skip ruoyi runtime baseline check"
  fi
  case "${STAGE}" in
    full-compat)
      if [[ -n "${EXTRA_EXPECT_ENABLED}" ]]; then
        append_csv_to_array "${EXTRA_EXPECT_ENABLED}" FULL_COMPAT_EXPECT_ENABLED
      fi
      bash script/dev/verify_crmeb_cutover_paths.sh \
        --base-url "${BASE_URL}" \
        --timeout-seconds "${TIMEOUT_SECONDS}" \
        --require-enabled-existing 1 \
        --expect-enabled "$(csv_join FULL_COMPAT_EXPECT_ENABLED)"
      ;;
    payment-core-only)
      bash script/dev/verify_payment_core_fast_track.sh \
        --base-url "${BASE_URL}" \
        --timeout-seconds "${TIMEOUT_SECONDS}" \
        --extra-expect-disabled "${EXTRA_EXPECT_DISABLED}" \
        --extra-expect-enabled "${EXTRA_EXPECT_ENABLED}"
      ;;
    disabled)
      if [[ -n "${EXTRA_EXPECT_DISABLED}" ]]; then
        append_csv_to_array "${EXTRA_EXPECT_DISABLED}" FULL_DISABLED_EXPECT
      fi
      bash script/dev/verify_crmeb_cutover_paths.sh \
        --base-url "${BASE_URL}" \
        --timeout-seconds "${TIMEOUT_SECONDS}" \
        --expect-disabled "$(csv_join FULL_DISABLED_EXPECT)"
      ;;
  esac
} 2>&1 | tee -a "${LOG_FILE}"
RC=$?
set -e

{
  echo "run_id=${RUN_ID}"
  echo "stage=${STAGE}"
  echo "base_url=${BASE_URL}"
  echo "timeout_seconds=${TIMEOUT_SECONDS}"
  echo "pipeline_exit_code=${RC}"
  echo "log_file=${LOG_FILE}"
  echo "out_dir=${OUT_DIR}"
  echo "verify_ruoyi_baseline=${VERIFY_RUOYI_BASELINE}"
  echo "diagnose_entrypoint=${DIAGNOSE_ENTRYPOINT}"
  echo "diagnosis_rc=${diagnosis_rc}"
  echo "diagnosis_summary_file=${diagnosis_summary_file}"
  echo "diagnosis_index_file=${diagnosis_index_file}"
} > "${SUMMARY_FILE}"

{
  echo "# CRMEB Gray Stage Gate"
  echo
  echo "- run_id: \`${RUN_ID}\`"
  echo "- stage: \`${STAGE}\`"
  echo "- base_url: \`${BASE_URL}\`"
  echo "- timeout_seconds: \`${TIMEOUT_SECONDS}\`"
  echo "- pipeline_exit_code: \`${RC}\`"
  echo
  echo "## Files"
  echo
  echo "- summary: \`summary.txt\`"
  echo "- log: \`logs/gray_gate.log\`"
  if [[ -n "${diagnosis_summary_file}" ]]; then
    echo "- diagnosis summary: \`${diagnosis_summary_file}\`"
  fi
  if [[ -n "${diagnosis_index_file}" ]]; then
    echo "- diagnosis index: \`${diagnosis_index_file}\`"
  fi
} > "${INDEX_FILE}"

if [[ "${RC}" -ne 0 ]]; then
  echo "[gray-gate] result=FAIL rc=${RC}" >&2
  exit "${RC}"
fi

echo "[gray-gate] result=PASS"
echo "[gray-gate] summary=${SUMMARY_FILE}"
