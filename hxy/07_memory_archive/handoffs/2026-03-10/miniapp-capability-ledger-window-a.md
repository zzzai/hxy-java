# Window A Handoff - MiniApp Capability Ledger & Doc Coverage (2026-03-10)

## 1. Objective
- Build an evidence-based single source for:
  - capability status (`ACTIVE / PLANNED_RESERVED / DEPRECATED`)
  - domain-level doc coverage score
  - next P0/P1 doc closure order
- Keep 03-09 frozen pack unchanged while adding 03-10 readiness artifacts.

## 2. Delivered Changes
1. Added `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
2. Added `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`
3. Updated `docs/products/miniapp/2026-03-08-miniapp-doc-gap-closure-index-v1.md`
4. Added `hxy/07_memory_archive/handoffs/2026-03-10/miniapp-capability-ledger-window-a.md`

## 3. Core Integration Outcome
- Capability status is now judged by three hard checks:
  - real front-end route exists
  - front/back `method + path` align
  - acceptance docs exist
- `ACTIVE` is therefore narrower than “code exists”.
- Booking domain was intentionally downgraded for create/cancel/addon paths because the current code reality does not meet the above rule.
- Historical prototype aliases were explicitly marked `DEPRECATED` so they stop acting as route truth.

## 4. Hard Findings Captured in Ledger
1. `booking/technician/list-by-store` on FE does not match backend `GET /booking/technician/list`.
2. `booking/time-slot/list` on FE does not match backend `GET /booking/slot/list` / `list-by-technician`.
3. FE uses `PUT /booking/order/cancel`, backend exposes `POST /booking/order/cancel`.
4. FE uses `POST /booking/addon/create`, backend controller is `POST /app-api/booking/addon/create`.
5. gift-card / referral / technician-feed remain `P2/RB3-P2` and gated off.

## 5. Coverage Outcome
- Highest coverage: trade/pay, after-sale/refund, promotion/coupon/point.
- Medium coverage with execution risk: booking, member, product/search.
- Lowest coverage: content/customer service and brokerage.
- P0 fill order in matrix:
  1. booking user-side real API alignment
  2. member account domain pack
  3. content/customer service pack
  4. brokerage/distribution pack

## 6. Coordination Notes for B/C/D
1. Window B
   - UI route truth should use real uniapp paths, not prototype aliases.
   - Booking create/cancel/addon should not be described as already active until FE request paths/methods are corrected.
2. Window C
   - Canonical API docs should absorb booking real-path mismatch as a blocking correction item.
   - Reserved domains remain gated by `miniapp.gift-card`, `miniapp.referral`, `miniapp.technician-feed.audit`.
3. Window D
   - Release and KPI runbooks should treat booking create/cancel/addon as non-active paths until code alignment is proven.
   - `degraded_pool` and `reserved-disabled` monitoring remain valid and unchanged.

## 7. Freeze Status Impact
- 03-09 Frozen pack remains unchanged.
- 03-10 docs are intended as `Ready` inputs for later freeze, not automatic Frozen.
