# MiniApp Catalog Sync Consistency Spec v1 (2026-03-08)

## 1. Problem
Category switch can show stale price/stock/availability if list and detail not refreshed from one source.

## 2. Consistency Contract
- On category switch, reload these fields from same API snapshot/version:
  - `price`
  - `stock`
  - `bookable`
  - `activityTag`
- UI must not merge old cache fields into new category result.

## 3. Conflict Handling
- `stock <= 0`: disable add-to-cart and show explicit reason.
- Add-to-cart response conflict: show server quantity and refresh card.
- If version mismatch detected, force full category refresh.

## 4. UX Requirements
- Show loading skeleton during refresh.
- Keep selected category state stable while data updates.
- Expose "refresh" entry for manual retry.
