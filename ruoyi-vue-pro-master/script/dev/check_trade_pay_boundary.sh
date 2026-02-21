#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

FORBIDDEN_REGEX='cn\.iocoder\.yudao\.module\.pay\.(dal|service|framework)\.'
TARGETS=(
  "yudao-module-mall/*/src/main/java"
  "yudao-module-member/src/main/java"
  "yudao-module-system/src/main/java"
  "yudao-module-infra/src/main/java"
)

echo "[pay-boundary] root=${ROOT_DIR}"
echo "[pay-boundary] rule=non-pay-modules must not depend on pay.dal/service/framework"

violations="$(rg -n "${FORBIDDEN_REGEX}" "${TARGETS[@]}" 2>/dev/null || true)"
violations="$(printf '%s\n' "${violations}" | rg -v '/yudao-module-pay/' || true)"

if [[ -n "${violations//[[:space:]]/}" ]]; then
  echo "[pay-boundary][FAIL] found forbidden dependencies:" >&2
  printf '%s\n' "${violations}" >&2
  exit 2
fi

required_files=(
  "yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/TradeOrderUpdateServiceImpl.java:import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;"
  "yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/order/TradeOrderUpdateServiceImpl.java:import cn.iocoder.yudao.module.pay.api.refund.PayRefundApi;"
  "yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/aftersale/AfterSaleServiceImpl.java:import cn.iocoder.yudao.module.pay.api.refund.PayRefundApi;"
)

for item in "${required_files[@]}"; do
  file="${item%%:*}"
  needle="${item#*:}"
  if ! rg -qF "${needle}" "${file}"; then
    echo "[pay-boundary][FAIL] required API dependency missing: ${file} -> ${needle}" >&2
    exit 2
  fi
done

echo "[pay-boundary] result=PASS"
