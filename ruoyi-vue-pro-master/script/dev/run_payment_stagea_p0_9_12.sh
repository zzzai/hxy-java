#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

MVN_BIN="${MVN_BIN:-mvn}"
RUN_CLEAN="${RUN_CLEAN:-0}"
BYTE_BUDDY_EXPERIMENTAL="${BYTE_BUDDY_EXPERIMENTAL:-1}"
SUREFIRE_FLAGS=("-Dsurefire.failIfNoSpecifiedTests=false")
EXTRA_MVN_ARGS=()
if [[ -n "${EXTRA_MVN_ARGS_RAW:-}" ]]; then
  # shellcheck disable=SC2206
  EXTRA_MVN_ARGS=(${EXTRA_MVN_ARGS_RAW})
fi

if [[ "${RUN_CLEAN}" == "1" ]]; then
  MVN_GOALS=("clean" "test")
else
  MVN_GOALS=("test")
fi

if [[ "${BYTE_BUDDY_EXPERIMENTAL}" == "1" ]]; then
  SUREFIRE_FLAGS+=("-DargLine=-Dnet.bytebuddy.experimental=true")
fi

echo "[stageA-p0-9-12] step=pay_notify_retry_policy"
RUN_CLEAN=0 \
BYTE_BUDDY_EXPERIMENTAL="${BYTE_BUDDY_EXPERIMENTAL}" \
EXTRA_MVN_ARGS_RAW="${EXTRA_MVN_ARGS_RAW:-}" \
bash script/dev/check_pay_notify_retry_policy.sh

PAY_ORDER_TESTS="PayOrderServiceTest#testSyncOrder_orderSuccess+testSyncOrder_orderClosed+testExpireOrder_success+testExpireOrder_weixinCloseOrder_failed"
PAY_REFUND_TESTS="PayRefundServiceTest#testSyncRefund_waiting+testSyncRefund_success+testSyncRefund_failure+testSyncRefund_exception+testNotifyRefundSuccess_success+testNotifyRefundFailure_success"
PAY_NOTIFY_POLICY_TESTS="PayNotifyServicePolicyTest#shouldMarkSuccessWithoutSchedulingNextRetry+shouldScheduleRequestSuccessRetryOnBusinessFailure+shouldScheduleRequestFailureRetryOnException+shouldMarkFailureWhenRetryTimesExhausted"
TEST_SET="${PAY_ORDER_TESTS},${PAY_REFUND_TESTS},${PAY_NOTIFY_POLICY_TESTS}"

echo "[stageA-p0-9-12] step=payment_tests"
echo "[stageA-p0-9-12] tests=${TEST_SET}"
"${MVN_BIN}" -pl "yudao-module-pay" -am "${MVN_GOALS[@]}" \
  -Dtest="${TEST_SET}" \
  "${SUREFIRE_FLAGS[@]}" \
  "${EXTRA_MVN_ARGS[@]}"

echo "[stageA-p0-9-12] result=PASS"
