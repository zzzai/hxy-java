# Window A Handoff - MiniApp Business Function Truth Ledger (2026-03-12)

## 1. Objective
- Convert the current repo truth into a single ledger for:
  - business function list
  - page / API truth
  - PRD completeness
  - blocker judgment

## 2. Delivered Changes
1. Added `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`
2. Updated `docs/products/miniapp/2026-03-08-miniapp-doc-gap-closure-index-v1.md`
3. Added `hxy/07_memory_archive/handoffs/2026-03-12/miniapp-business-function-ledger-window-a.md`

## 3. Current Judgment
- User-side mainline PRDs are mostly in place.
- The dominant gap is no longer “missing PRD everywhere”; it is “PRD exists but route/API/runtime truth is not yet closed”.
- The clearest PRD blank area is the admin finance-ops domain:
  - four-account reconcile
  - refund notify replay
  - technician commission / settlement

## 4. Highest-Priority Blockers
1. Booking create / cancel / addon truth drift still blocks runtime promotion.
2. Member level / asset hub / tag center still block because real pages do not exist.
3. Product canonical search and bargain still block because there is no real user page.
4. Finance-ops admin capabilities still lack dedicated PRDs even though pages and APIs already exist.

## 5. Coordination Notes
- Window B
  - Product-layer next priority is finance-ops PRDs, then upgrade technician-feed from policy to full PRD.
- Window C
  - Booking and member reserved edges remain code-truth first; do not let PRD completion overwrite API truth.
- Window D
  - Acceptance scope must keep separating existing ACTIVE chains from Ready-only or Planned-only chains.
