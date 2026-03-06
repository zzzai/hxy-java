# Window A Handoff - Trade Bundle Child Ledger + Refund Consistency

## Context
- Branch: feat/ui-four-account-reconcile-ops
- Scope: trade/booking backend + SQL + governance docs only
- Goal: bundle child ledger persistence and refund-cap consistency closure

## Delivered
1. Added table `hxy_trade_order_item_bundle_child` and indexes via SQL:
   - `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-06-hxy-trade-order-item-bundle-child-ledger.sql`
2. Added trade ledger model and mapper:
   - `TradeOrderItemBundleChildDO`
   - `TradeOrderItemBundleChildMapper`
3. Order creation now persists bundle-child ledger rows with idempotent guard by `orderItemId`.
4. Service-order status sync now updates child-level `fulfillment_status`.
5. After-sale cap and execution path now prefer child ledger; fallback to snapshot is retained.
6. Added/updated tests for mapper, order create, service-order sync, and after-sale validation.
7. Updated governance docs (baseline/ADR/roadmap).

## Verification
- Target class red->green fixed:
  - `AfterSaleServiceImplBundleRefundValidationTest` (15/15 pass)
- Requested module test command passed:
  - `TradeOrderCreateServiceImplTest`, `TradeServiceOrderApiImplTest`

## Risks / Notes
- The requested test selector contains classes that may not be present/executed in current module test discovery (`AfterSaleServiceImplTest`, `AfterSaleRefundServiceImplTest`). Command still passed and executed matching tests.
- Repo has unrelated untracked files (`.codex/`, old handoffs); untouched.

## Next Suggested Step
- Frontend window can bind `refundLimitSource` and child-ledger evidence fields for operator visibility.
