#!/usr/bin/env bash
set -euo pipefail

# D6: 监控巡检 + 告警包装器

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CHECK_SCRIPT="${ROOT_DIR}/shell/payment_monitor_quickcheck.sh"
ALERT_SCRIPT="${ROOT_DIR}/shell/payment_alert_notify.sh"

if [[ ! -x "${CHECK_SCRIPT}" ]]; then
  echo "缺少巡检脚本: ${CHECK_SCRIPT}"
  exit 1
fi

tmp_out="$(mktemp)"
cleanup() {
  rm -f "${tmp_out}"
}
trap cleanup EXIT

set +e
"${CHECK_SCRIPT}" "$@" > "${tmp_out}" 2>&1
rc=$?
set -e

cat "${tmp_out}"

if [[ ${rc} -eq 2 ]]; then
  if [[ -x "${ALERT_SCRIPT}" ]]; then
    "${ALERT_SCRIPT}" \
      --title "支付监控告警" \
      --content "$(cat "${tmp_out}")" || true
  fi
fi

exit ${rc}
