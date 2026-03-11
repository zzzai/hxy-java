# Window A Handoff - MiniApp Core Trade Pay After-sale PRD Pack (2026-03-12)

## 1. Objective
- Close the remaining user-side core-chain PRD gap by splitting trade, pay, and after-sale out of the service blueprint into standalone PRDs.

## 2. Delivered Changes
1. Added `docs/products/miniapp/2026-03-12-miniapp-trade-checkout-order-prd-v1.md`
2. Added `docs/products/miniapp/2026-03-12-miniapp-pay-submit-result-prd-v1.md`
3. Added `docs/products/miniapp/2026-03-12-miniapp-after-sale-refund-prd-v1.md`
4. Updated `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`
5. Updated `docs/products/miniapp/2026-03-08-miniapp-doc-gap-closure-index-v1.md`
6. Added `hxy/07_memory_archive/handoffs/2026-03-12/miniapp-core-trade-pay-aftersale-prd-pack-window-a.md`

## 3. Current Judgment
- User-side trade / pay / after-sale are no longer described only by the service blueprint.
- These three chains now each have independent PRD truth.
- Current project snapshot becomes:
  - `Frozen = 39`
  - `Ready = 43`
  - `Draft = 0`

## 4. Practical Effect
1. Trade
   - Checkout, create, order detail, order list, receive, cancel, delete now have one product truth.
   - Legacy detail fallback is explicitly marked as compatibility only.
2. Pay
   - Channel-code lookup, pay submit, pay-order query, and pay-result return path are now separated from trade PRD.
3. After-sale
   - After-sale create, list, detail, log, delivery, and refund-progress now have one product truth.
   - Refund progress is tied to the real API, not a fictional standalone route.

## 5. Coordination Notes
- Window B
  - Core user-side PRD gaps are now materially reduced; next product priority can move to technician-feed full PRD or booking/member blocker closure support.
- Window C
  - Keep legacy compatibility behavior out of canonical truth, especially `/trade/order/get` fallback.
- Window D
  - Acceptance should treat trade/pay/after-sale as separate chains, not one monolithic “journey” block, and continue to keep degraded query behavior out of success metrics.
