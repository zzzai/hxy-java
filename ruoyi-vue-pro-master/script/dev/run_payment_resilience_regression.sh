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

PAY_ORDER_TESTS="PayOrderServiceTest#testCreateOrder_exists+testNotifyOrderSuccess_order_paid+testNotifyOrderSuccess_order_closed+testNotifyOrderClosed_orderExtension_paid+testNotifyOrderClosed_orderExtension_waiting+testSyncOrder_orderSuccess+testSyncOrder_orderClosed+testExpireOrder_success+testExpireOrder_weixinCloseOrder_failed"
PAY_REFUND_TESTS="PayRefundServiceTest#testNotifyRefundSuccess_isSuccess+testNotifyRefundSuccess_failure+testNotifyRefundFailure_isSuccess+testNotifyRefundFailure_isFailure+testNotifyRefundFailure_success+testSyncRefund_waiting+testSyncRefund_success+testSyncRefund_failure+testSyncRefund_exception"
PAY_NOTIFY_CONTROLLER_TESTS="PayNotifyControllerTest#shouldAck204AndProcessOrderNotifyAsync+shouldAck200SuccessAndProcessRefundNotifyAsync+shouldFallbackToSyncWhenNotifyExecutorRejected+shouldAckWithinSlaWhenAsyncTaskIsSlow"
PAY_NOTIFY_POLICY_TESTS="PayNotifyServicePolicyTest#shouldMarkSuccessWithoutSchedulingNextRetry+shouldScheduleRequestSuccessRetryOnBusinessFailure+shouldScheduleRequestFailureRetryOnException+shouldMarkFailureWhenRetryTimesExhausted"
TEST_SET="${PAY_ORDER_TESTS},${PAY_REFUND_TESTS},${PAY_NOTIFY_CONTROLLER_TESTS},${PAY_NOTIFY_POLICY_TESTS}"

START_TS="$(date +%s)"
echo "[pay-resilience] module=yudao-module-pay"
echo "[pay-resilience] tests=${TEST_SET}"
echo "[pay-resilience] goals=${MVN_GOALS[*]}"

"${MVN_BIN}" -pl "yudao-module-pay" -am "${MVN_GOALS[@]}" \
  -Dtest="${TEST_SET}" \
  "${SUREFIRE_FLAGS[@]}" \
  "${EXTRA_MVN_ARGS[@]}"

END_TS="$(date +%s)"
echo "[pay-resilience] result=PASS elapsed=$((END_TS - START_TS))s"
