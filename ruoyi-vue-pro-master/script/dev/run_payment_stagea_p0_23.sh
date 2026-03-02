#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'USAGE'
Usage:
  script/dev/run_payment_stagea_p0_23.sh

Env:
  RUN_ID=<id>                       执行 ID（默认时间戳）
  ARTIFACT_BASE_DIR=<dir>           产物目录（默认 .tmp/payment_stagea_p0_23）
  REQUIRE_NAMING_GUARD=0|1          命名门禁失败是否阻断（默认 1）
  REQUIRE_MEMORY_GUARD=0|1          记忆门禁失败是否阻断（默认 1）
  RUN_SERVER_GATEWAY_TEST=0|1       是否执行网关集成单测（默认 1）
  GIT_DIFF_RANGE=<base...head>      记忆门禁差异范围（可选）

Exit Code:
  0 PASS
  2 BLOCK（门禁失败）
  1 执行异常
USAGE
  exit 0
fi

RUN_ID="${RUN_ID:-$(date +%Y%m%d_%H%M%S)_$RANDOM}"
ARTIFACT_BASE_DIR="${ARTIFACT_BASE_DIR:-${ROOT_DIR}/.tmp/payment_stagea_p0_23}"
ARTIFACT_DIR="${ARTIFACT_BASE_DIR}/${RUN_ID}"
LOG_DIR="${ARTIFACT_DIR}/logs"
SUMMARY_FILE="${ARTIFACT_DIR}/summary.txt"
FINAL_GATE_LOG="${LOG_DIR}/final_gate.log"
RUN_LOG="${LOG_DIR}/run.log"
NAMING_GUARD_LOG="${LOG_DIR}/naming_guard.log"
MEMORY_GUARD_LOG="${LOG_DIR}/memory_guard.log"
PRODUCT_TEST_LOG="${LOG_DIR}/product_template_binding_test.log"
TRADE_TEST_LOG="${LOG_DIR}/trade_template_fallback_test.log"
BOOKING_TEST_LOG="${LOG_DIR}/booking_placeholder_test.log"
SERVER_GATEWAY_TEST_LOG="${LOG_DIR}/server_booking_gateway_test.log"

REQUIRE_NAMING_GUARD="${REQUIRE_NAMING_GUARD:-1}"
REQUIRE_MEMORY_GUARD="${REQUIRE_MEMORY_GUARD:-1}"
RUN_SERVER_GATEWAY_TEST="${RUN_SERVER_GATEWAY_TEST:-1}"

for flag in "${REQUIRE_NAMING_GUARD}" "${REQUIRE_MEMORY_GUARD}" "${RUN_SERVER_GATEWAY_TEST}"; do
  if ! [[ "${flag}" =~ ^[01]$ ]]; then
    echo "Invalid flag value: ${flag} (expect 0|1)" >&2
    exit 1
  fi
done

mkdir -p "${LOG_DIR}"
exec > >(tee -a "${RUN_LOG}") 2>&1

naming_guard_rc="SKIP"
memory_guard_rc="SKIP"
product_test_rc="SKIP"
trade_test_rc="SKIP"
booking_test_rc="SKIP"
server_gateway_test_rc="SKIP"

finalize() {
  local rc=$?
  local pipeline_rc="${PIPELINE_EXIT_CODE:-$rc}"

  {
    echo "run_id=${RUN_ID}"
    echo "pipeline_exit_code=${pipeline_rc}"
    echo "require_naming_guard=${REQUIRE_NAMING_GUARD}"
    echo "require_memory_guard=${REQUIRE_MEMORY_GUARD}"
    echo "run_server_gateway_test=${RUN_SERVER_GATEWAY_TEST}"
    echo "naming_guard_rc=${naming_guard_rc}"
    echo "memory_guard_rc=${memory_guard_rc}"
    echo "product_test_rc=${product_test_rc}"
    echo "trade_test_rc=${trade_test_rc}"
    echo "booking_test_rc=${booking_test_rc}"
    echo "server_gateway_test_rc=${server_gateway_test_rc}"
    echo "run_log=${RUN_LOG}"
    echo "final_gate_log=${FINAL_GATE_LOG}"
    echo "naming_guard_log=${NAMING_GUARD_LOG}"
    echo "memory_guard_log=${MEMORY_GUARD_LOG}"
    echo "product_test_log=${PRODUCT_TEST_LOG}"
    echo "trade_test_log=${TRADE_TEST_LOG}"
    echo "booking_test_log=${BOOKING_TEST_LOG}"
    echo "server_gateway_test_log=${SERVER_GATEWAY_TEST_LOG}"
  } > "${SUMMARY_FILE}"

  {
    echo "payment_stagea_p0_23_gate"
    echo "generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "pipeline_exit_code=${pipeline_rc}"
    if [[ "${pipeline_rc}" == "0" ]]; then
      echo "decision=PASS"
    else
      echo "decision=BLOCK"
    fi
    echo "summary=${SUMMARY_FILE}"
  } > "${FINAL_GATE_LOG}"

  echo "[stageA-p0-23] artifact_dir=${ARTIFACT_DIR}"
  echo "[stageA-p0-23] summary=${SUMMARY_FILE}"
  return "${pipeline_rc}"
}
trap finalize EXIT

run_step() {
  local log_file="$1"
  shift
  set +e
  "$@" 2>&1 | tee "${log_file}"
  local rc="${PIPESTATUS[0]}"
  set -e
  return "${rc}"
}

echo "[stageA-p0-23] step=naming-guard"
if run_step "${NAMING_GUARD_LOG}" bash script/dev/check_hxy_naming_guard.sh; then
  naming_guard_rc=0
else
  naming_guard_rc=$?
  if [[ "${REQUIRE_NAMING_GUARD}" == "1" ]]; then
    echo "[stageA-p0-23] result=BLOCK (naming guard rc=${naming_guard_rc})" >&2
    PIPELINE_EXIT_CODE=2
    exit 2
  fi
  echo "[stageA-p0-23] warn: naming guard rc=${naming_guard_rc}, continue (require=0)"
fi

echo "[stageA-p0-23] step=memory-guard"
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
if [[ "${memory_guard_rc}" != "0" ]]; then
  if [[ "${REQUIRE_MEMORY_GUARD}" == "1" ]]; then
    echo "[stageA-p0-23] result=BLOCK (memory guard rc=${memory_guard_rc})" >&2
    PIPELINE_EXIT_CODE=2
    exit 2
  fi
  echo "[stageA-p0-23] warn: memory guard rc=${memory_guard_rc}, continue (require=0)"
fi

echo "[stageA-p0-23] step=test-product-template-binding"
if run_step "${PRODUCT_TEST_LOG}" \
  mvn -f pom.xml \
    -pl yudao-module-mall/yudao-module-product -am \
    -Dtest=ProductSpuTemplateVersionBindingTest \
    -Dsurefire.failIfNoSpecifiedTests=false test; then
  product_test_rc=0
else
  product_test_rc=$?
  PIPELINE_EXIT_CODE="${product_test_rc}"
  exit "${product_test_rc}"
fi

echo "[stageA-p0-23] step=test-trade-template-fallback"
if run_step "${TRADE_TEST_LOG}" \
  mvn -f pom.xml \
    -pl yudao-module-mall/yudao-module-trade -am \
    -Dtest=TradePriceServiceTemplateVersionValidationTest \
    -Dsurefire.failIfNoSpecifiedTests=false test; then
  trade_test_rc=0
else
  trade_test_rc=$?
  PIPELINE_EXIT_CODE="${trade_test_rc}"
  exit "${trade_test_rc}"
fi

echo "[stageA-p0-23] step=test-booking-placeholder"
if run_step "${BOOKING_TEST_LOG}" \
  mvn -f pom.xml \
    -pl yudao-module-mall/yudao-module-booking -am \
    -Dtest=BookingOrderServiceImplTest \
    -Dsurefire.failIfNoSpecifiedTests=false test; then
  booking_test_rc=0
else
  booking_test_rc=$?
  PIPELINE_EXIT_CODE="${booking_test_rc}"
  exit "${booking_test_rc}"
fi

if [[ "${RUN_SERVER_GATEWAY_TEST}" == "1" ]]; then
  echo "[stageA-p0-23] step=test-server-booking-gateway"
  if run_step "${SERVER_GATEWAY_TEST_LOG}" \
    mvn -f pom.xml \
      -pl yudao-server -am \
      -Dtest=ServerTradeServiceBookingGatewayTest \
      -Dsurefire.failIfNoSpecifiedTests=false clean test; then
    server_gateway_test_rc=0
  else
    server_gateway_test_rc=$?
    PIPELINE_EXIT_CODE="${server_gateway_test_rc}"
    exit "${server_gateway_test_rc}"
  fi
else
  echo "[stageA-p0-23] skip step=test-server-booking-gateway"
fi

echo "[stageA-p0-23] result=PASS"
PIPELINE_EXIT_CODE=0
exit 0
