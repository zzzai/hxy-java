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

TEST_CASE="PayNotifyControllerTest#shouldAckWithinSlaWhenAsyncTaskIsSlow"

echo "[pay-notify-sla] module=yudao-module-pay"
echo "[pay-notify-sla] test=${TEST_CASE}"
echo "[pay-notify-sla] iterations=${ITERATIONS} async_sleep_ms=${ASYNC_SLEEP_MS} max_ack_ms=${MAX_ACK_MS}"
echo "[pay-notify-sla] goals=${MVN_GOALS[*]}"

"${MVN_BIN}" -pl "yudao-module-pay" -am "${MVN_GOALS[@]}" \
  -Dtest="${TEST_CASE}" \
  -Dpay.notify.sla.iterations="${ITERATIONS}" \
  -Dpay.notify.sla.async-sleep-ms="${ASYNC_SLEEP_MS}" \
  -Dpay.notify.sla.max-ack-ms="${MAX_ACK_MS}" \
  "${SUREFIRE_FLAGS[@]}" \
  "${EXTRA_MVN_ARGS[@]}"

echo "[pay-notify-sla] result=PASS"
