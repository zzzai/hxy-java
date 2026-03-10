# Window A Handoff - MiniApp Doc Completion Integration (2026-03-10)

## 1. Objective
- Complete the remaining documentation-gap master closure for the miniapp user-side release scope.
- Keep 03-09 Frozen baseline unchanged while integrating 03-10 truth-closure inputs into a single A-side source.
- Enforce the rule: document truth first, development second.

## 2. Delivered Changes
1. Added `docs/products/miniapp/2026-03-10-miniapp-doc-completion-master-plan-v1.md`
2. Added `docs/products/miniapp/2026-03-10-miniapp-member-route-truth-and-active-planned-closure-v1.md`
3. Added `docs/products/miniapp/2026-03-10-miniapp-booking-route-api-truth-review-v1.md`
4. Updated `docs/products/miniapp/2026-03-08-miniapp-doc-gap-closure-index-v1.md`
5. Updated `docs/products/miniapp/2026-03-09-miniapp-ready-to-frozen-review-v1.md`
6. Updated `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
7. Updated `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`
8. Added `hxy/07_memory_archive/handoffs/2026-03-10/miniapp-doc-completion-integration-window-a.md`

## 3. Integration Outcome
- 03-10 completion closure now has one A-side master plan that lists:
  - remaining domain doc gaps
  - target file paths
  - owner window
  - dependency order
  - whether the gap blocks development
- Member domain now has an explicit route-truth closure:
  - `/pages/public/login` is not the runtime truth; actual entry is `component:s-auth-modal` plus `/pages/index/login`
  - `/pages/user/index` is corrected to `/pages/index/user`
  - `/pages/user/sign-in` is corrected to `/pages/app/sign`
  - `/pages/user/level`, `/pages/profile/assets`, `/pages/user/tag` stay out of `ACTIVE`
- Booking domain now has an explicit front/back/doc truth review:
  - query chain remains the only safe `ACTIVE` part
  - `create / cancel / addon` remain blocked because FE and BE method/path do not match
- Capability ledger and coverage matrix now cite these 03-10 A-side truth-closure docs as evidence, without promoting the batch to Frozen.

## 4. Hard Blockers
1. Booking contract blocker
   - FE still uses:
     - `GET /booking/technician/list-by-store`
     - `GET /booking/time-slot/list`
     - `PUT /booking/order/cancel`
     - `POST /booking/addon/create`
   - BE currently exposes:
     - `GET /booking/technician/list`
     - `GET /booking/slot/list`
     - `GET /booking/slot/list-by-technician`
     - `POST /booking/order/cancel`
     - `POST /app-api/booking/addon/create`
   - Until C publishes canonical contract truth, booking create/cancel/addon cannot enter Frozen.
2. Member route-truth blocker
   - Route aliases still exist in 03-10 member docs and related cross-doc references.
   - Missing pages remain real missing pages, not deferred-active placeholders:
     - `/pages/user/level`
     - `/pages/profile/assets`
     - `/pages/user/tag`
   - `/member/asset-ledger/page` remains `PLANNED_RESERVED`.
3. Remaining domain doc-gap blocker
   - Content / Brokerage / Product Catalog / Marketing Expansion / Reserved Activation still depend on B/C/D outputs.
   - These rows remain `Pending window output` in the index and cannot be frozen by A alone.

## 5. B/C/D Coordination Notes
1. Window B
   - All user-side route descriptions must use real uniapp paths, not prototype aliases.
   - `degraded / degradeReason / errorCode` remain mandatory page-level inputs for booking, member, and content flows.
   - Missing member pages must stay absent in PRD scope until the actual page route exists.
2. Window C
   - Publish booking canonical `method + path` truth for technician list, slot list, cancel, and addon.
   - Keep `ACTIVE_SET` and `PLANNED_RESERVED` separated; `/member/asset-ledger/page` must not leak into active scope.
   - Error handling remains code-driven only; do not branch on `message`.
3. Window D
   - Booking and member release gates must treat these A-side docs as Ready evidence, not Frozen approval.
   - `degraded=true` samples stay outside main KPI and ROI denominators.
   - Validation evidence must carry `runId / orderId / payRefundId / sourceBizNo / errorCode`; non-applicable keys use agreed placeholder policy only in runbook scope.

## 6. Freeze Status
- 03-09 Frozen baseline remains unchanged at `39/39 Frozen`.
- 03-10 A-side completion docs are integrated as `Ready`.
- No 03-10 doc in this batch is promoted to Frozen.

## 7. Next Closure Order
1. C window: `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md`
2. B/C/D windows: content/customer service pack
3. B/C/D windows: brokerage/distribution pack
4. B/C/D windows: product catalog interaction pack
5. B/C/D windows: marketing expansion and reserved-activation pack
