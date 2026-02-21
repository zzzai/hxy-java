#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

MVN_BIN="${MVN_BIN:-mvn}"
RUN_CLEAN="${RUN_CLEAN:-0}"
BYTE_BUDDY_EXPERIMENTAL="${BYTE_BUDDY_EXPERIMENTAL:-1}"
ITERATIONS="${ITERATIONS:-40}"
ASYNC_SLEEP_MS="${ASYNC_SLEEP_MS:-350}"
MAX_ACK_MS="${MAX_ACK_MS:-180}"
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

echo "[stageA-p0-5-8] step=check_trade_pay_boundary"
bash script/dev/check_trade_pay_boundary.sh

echo "[stageA-p0-5-8] step=check_crmeb_compat_freeze"
bash script/dev/check_crmeb_compat_freeze.sh

echo "[stageA-p0-5-8] step=pay_notify_ack_sla"
ITERATIONS="${ITERATIONS}" \
ASYNC_SLEEP_MS="${ASYNC_SLEEP_MS}" \
MAX_ACK_MS="${MAX_ACK_MS}" \
RUN_CLEAN=0 \
BYTE_BUDDY_EXPERIMENTAL="${BYTE_BUDDY_EXPERIMENTAL}" \
EXTRA_MVN_ARGS_RAW="${EXTRA_MVN_ARGS_RAW:-}" \
bash script/dev/check_pay_notify_ack_sla.sh

PAY_ORDER_TESTS="PayOrderServiceTest#testCreateOrder_success+testCreateOrder_exists+testNotifyOrderSuccess_order_paid+testNotifyOrderSuccess_order_waiting+testNotifyOrderSuccess_order_closed+testNotifyOrderSuccess_order_refund+testNotifyOrderClosed_orderExtension_refund+testNotifyOrderClosed_orderExtension_waiting"
PAY_REFUND_TESTS="PayRefundServiceTest#testNotifyRefundSuccess_isSuccess+testNotifyRefundSuccess_success+testNotifyRefundFailure_isFailure+testNotifyRefundFailure_success"
PAY_NOTIFY_TESTS="PayNotifyControllerTest#shouldAck204AndProcessOrderNotifyAsync+shouldAck200SuccessAndProcessRefundNotifyAsync+shouldFallbackToSyncWhenNotifyExecutorRejected+shouldAckWithinSlaWhenAsyncTaskIsSlow"
TEST_SET="${PAY_ORDER_TESTS},${PAY_REFUND_TESTS},${PAY_NOTIFY_TESTS}"

echo "[stageA-p0-5-8] step=payment_tests"
echo "[stageA-p0-5-8] tests=${TEST_SET}"
"${MVN_BIN}" -pl "yudao-module-pay" -am "${MVN_GOALS[@]}" \
  -Dtest="${TEST_SET}" \
  -Dpay.notify.sla.iterations="${ITERATIONS}" \
  -Dpay.notify.sla.async-sleep-ms="${ASYNC_SLEEP_MS}" \
  -Dpay.notify.sla.max-ack-ms="${MAX_ACK_MS}" \
  "${SUREFIRE_FLAGS[@]}" \
  "${EXTRA_MVN_ARGS[@]}"

echo "[stageA-p0-5-8] result=PASS"
