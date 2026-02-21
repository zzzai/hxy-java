#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

MVN_BIN="${MVN_BIN:-mvn}"
RUN_CLEAN="${RUN_CLEAN:-0}"
BYTE_BUDDY_EXPERIMENTAL="${BYTE_BUDDY_EXPERIMENTAL:-1}"
RUN_UNSTABLE_SUITES="${RUN_UNSTABLE_SUITES:-0}"
FAST_TRACK_PAYMENT_ONLY="${FAST_TRACK_PAYMENT_ONLY:-0}"
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

run_suite() {
  local name="$1"
  local module="$2"
  local tests="$3"
  local goals=("${MVN_GOALS[@]}")
  if [[ "${4:-}" == "clean-first" ]]; then
    goals=("clean" "test")
  fi

  echo
  echo "[crmeb-compat] suite=${name}"
  echo "[crmeb-compat] module=${module}"
  echo "[crmeb-compat] tests=${tests}"
  echo "[crmeb-compat] goals=${goals[*]}"

  "${MVN_BIN}" -pl "${module}" -am "${goals[@]}" \
    -Dtest="${tests}" \
    "${SUREFIRE_FLAGS[@]}" \
    "${EXTRA_MVN_ARGS[@]}"
}

START_TS="$(date +%s)"
echo "[crmeb-compat] fast_track_payment_only=${FAST_TRACK_PAYMENT_ONLY}"
echo "[crmeb-compat] run script/dev/check_trade_pay_boundary.sh"
script/dev/check_trade_pay_boundary.sh
echo "[crmeb-compat] run script/dev/check_crmeb_compat_freeze.sh"
script/dev/check_crmeb_compat_freeze.sh

run_suite \
  "pay-compat" \
  "yudao-module-pay" \
  "CrmebFrontPayCompatControllerTest,CrmebFrontRechargeCompatControllerTest,CrmebAdminPayCallbackCompatControllerTest,CrmebAdminRefundCompatControllerTest,CrmebAdminSystemConfigCompatControllerTest"

if [[ "${FAST_TRACK_PAYMENT_ONLY}" == "1" ]]; then
  run_suite \
    "trade-payment-core-compat" \
    "yudao-module-mall/yudao-module-trade" \
    "CrmebAdminStoreOrderCompatControllerTest#shouldReturnEmptyRefundTicketListWhenNoTicket+shouldAgreeAndRefundWhenConfirmByOrderNo+shouldRefundWhenConfirmByQueryParams"
  echo "[crmeb-compat] skip suite=trade-compat (fast-track payment-only mode)"
else
  run_suite \
    "trade-compat" \
    "yudao-module-mall/yudao-module-trade" \
    "CrmebFrontOrderCompatControllerTest,CrmebAdminStoreOrderCompatControllerTest"
fi

run_suite \
  "server-compat" \
  "yudao-server" \
  "CrmebCompatInterceptorTest"

if [[ "${RUN_UNSTABLE_SUITES}" == "1" ]]; then
  if [[ "${FAST_TRACK_PAYMENT_ONLY}" == "1" ]]; then
    echo "[crmeb-compat] skip suite=member-compat-unstable (fast-track payment-only mode)"
  else
    # Member module has hit stale class artifacts in incremental runs.
    # Force clean for this suite to keep regression deterministic.
    if [[ "${RUN_CLEAN}" != "1" ]]; then
      echo "[crmeb-compat] info: forcing clean for member-compat-unstable to avoid stale class artifacts"
    fi
    run_suite \
      "member-compat-unstable" \
      "yudao-module-member" \
      "CrmebFrontWechatCompatControllerTest" \
      "clean-first"
  fi
fi

END_TS="$(date +%s)"
echo
echo "[crmeb-compat] all suites passed, elapsed=$((END_TS - START_TS))s"
