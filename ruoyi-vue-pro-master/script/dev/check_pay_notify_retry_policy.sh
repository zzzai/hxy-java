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

TEST_CASE="PayNotifyServicePolicyTest"
PAY_NOTIFY_IMPL="yudao-module-pay/src/main/java/cn/iocoder/yudao/module/pay/service/notify/PayNotifyServiceImpl.java"
PAY_NOTIFY_TASK_DO="yudao-module-pay/src/main/java/cn/iocoder/yudao/module/pay/dal/dataobject/notify/PayNotifyTaskDO.java"

require_pattern() {
  local pattern="$1"
  local file="$2"
  local message="$3"
  if ! rg -q "${pattern}" "${file}"; then
    echo "[pay-notify-retry-policy] result=BLOCK reason=${message} file=${file}"
    exit 1
  fi
}

echo "[pay-notify-retry-policy] module=yudao-module-pay"
echo "[pay-notify-retry-policy] test=${TEST_CASE}"
echo "[pay-notify-retry-policy] goals=${MVN_GOALS[*]}"

"${MVN_BIN}" -pl "yudao-module-pay" -am "${MVN_GOALS[@]}" \
  -Dtest="${TEST_CASE}" \
  "${SUREFIRE_FLAGS[@]}" \
  "${EXTRA_MVN_ARGS[@]}"

echo "[pay-notify-retry-policy] step=static_audit"
require_pattern "NOTIFY_FREQUENCY" "${PAY_NOTIFY_TASK_DO}" "missing retry frequency curve"
require_pattern "notifyLockCoreRedisDAO\\.lock\\(" "${PAY_NOTIFY_IMPL}" "missing distributed lock protection"
require_pattern "setNextNotifyTime\\(addTime\\(Duration\\.ofSeconds\\(PayNotifyTaskDO\\.NOTIFY_FREQUENCY" "${PAY_NOTIFY_IMPL}" "missing retry schedule update"
require_pattern "setStatus\\(PayNotifyStatusEnum\\.FAILURE\\.getStatus\\(\\)\\)" "${PAY_NOTIFY_IMPL}" "missing final failure transition"
require_pattern "public static final int NOTIFY_TIMEOUT = 120" "${PAY_NOTIFY_IMPL}" "missing notify timeout guard"

echo "[pay-notify-retry-policy] result=PASS"
