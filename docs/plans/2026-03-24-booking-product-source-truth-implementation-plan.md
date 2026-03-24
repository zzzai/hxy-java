# Booking Product Source Truth Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a single booking product-source flow so `create` and `addon` only use service-catalog selection results as `spuId/skuId` truth.

**Architecture:** Add a booking-specific service selection page that reuses existing product list/detail APIs, then thread explicit `spuId/skuId` route params through `technician-detail -> service-select -> order-confirm` and `order-detail/addon -> service-select -> addon`. Keep planning-only catalog APIs untouched and lock behavior with tests before production edits.

**Tech Stack:** uni-app Vue 3 pages, existing `SpuApi` / `BookingApi`, Node test suite, markdown docs.

---

### Task 1: Lock route truth with failing tests

**Files:**
- Modify: `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
- Modify: `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`

**Step 1: Write the failing test**
- Add assertions that:
  - `technician-detail` no longer jumps directly to `order-confirm` without product source.
  - `order-confirm` submit payload must originate from explicit route `spuId/skuId`, not slot fallback.
  - `addon` submit must require product source for `UPGRADE / ADD_ITEM`.

**Step 2: Run test to verify it fails**
Run: `node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
Expected: FAIL because current page flow still uses slot fallback and addon lacks product-source enforcement.

**Step 3: Commit**
```bash
git add yudao-mall-uniapp/tests/booking-page-smoke.test.mjs yudao-mall-uniapp/tests/booking-api-alignment.test.mjs
git commit -m "test(booking): lock product source route truth"
```

### Task 2: Add booking service selection page

**Files:**
- Create: `yudao-mall-uniapp/pages/booking/service-select.vue`
- Modify: `yudao-mall-uniapp/pages.json`
- Test: `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`

**Step 1: Write the failing test**
- Assert new route exists in `pages.json`.
- Assert page requires explicit selection before confirming.
- Assert selected result contains `spuId/skuId` and target flow metadata.

**Step 2: Run test to verify it fails**
Run: `node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
Expected: FAIL because route/page do not exist yet.

**Step 3: Write minimal implementation**
- Add page route.
- Implement simple service selection page with:
  - `SpuApi.getSpuPage`
  - item click -> `SpuApi.getSpuDetail`
  - SKU selection using existing truth
  - confirm button -> route back to `order-confirm` or `addon`

**Step 4: Run test to verify it passes**
Run: `node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
Expected: PASS for new route and selection flow assertions.

**Step 5: Commit**
```bash
git add yudao-mall-uniapp/pages/booking/service-select.vue yudao-mall-uniapp/pages.json yudao-mall-uniapp/tests/booking-page-smoke.test.mjs
git commit -m "feat(booking): add service selection page"
```

### Task 3: Rewire create flow to explicit product source

**Files:**
- Modify: `yudao-mall-uniapp/pages/booking/technician-detail.vue`
- Modify: `yudao-mall-uniapp/pages/booking/order-confirm.vue`
- Modify: `yudao-mall-uniapp/pages/booking/logic.js`
- Test: `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
- Test: `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`

**Step 1: Write the failing test**
- Assert `technician-detail` routes to service-select with `flow=create`.
- Assert `order-confirm` submit uses route `spuId/skuId` only.
- Assert missing product source blocks submit.

**Step 2: Run test to verify it fails**
Run: `node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
Expected: FAIL on old direct route and slot fallback behavior.

**Step 3: Write minimal implementation**
- Add helper route method in `logic.js`.
- Change `technician-detail` confirm jump.
- Change `order-confirm` load/submit logic to read explicit `spuId/skuId` from options and validate presence.

**Step 4: Run test to verify it passes**
Run: `node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
Expected: PASS.

**Step 5: Commit**
```bash
git add yudao-mall-uniapp/pages/booking/technician-detail.vue yudao-mall-uniapp/pages/booking/order-confirm.vue yudao-mall-uniapp/pages/booking/logic.js yudao-mall-uniapp/tests/booking-page-smoke.test.mjs yudao-mall-uniapp/tests/booking-api-alignment.test.mjs
git commit -m "feat(booking): wire create flow to explicit product source"
```

### Task 4: Rewire addon flow by addon type

**Files:**
- Modify: `yudao-mall-uniapp/pages/booking/addon.vue`
- Modify: `yudao-mall-uniapp/pages/booking/order-detail.vue`
- Modify: `yudao-mall-uniapp/pages/booking/logic.js`
- Test: `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
- Test: `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`

**Step 1: Write the failing test**
- Assert `addonType=EXTEND_TIME` may reuse parent order product.
- Assert `addonType=UPGRADE / ADD_ITEM` cannot submit without explicit selected `spuId/skuId`.
- Assert route to service-select carries `flow=addon` and `parentOrderId`.

**Step 2: Run test to verify it fails**
Run: `node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
Expected: FAIL on old addon submit path.

**Step 3: Write minimal implementation**
- Update addon page state and submit gate.
- Add “选择服务项目” path for `UPGRADE / ADD_ITEM`.
- Route from order detail / addon to service-select and back.

**Step 4: Run test to verify it passes**
Run: `node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
Expected: PASS.

**Step 5: Commit**
```bash
git add yudao-mall-uniapp/pages/booking/addon.vue yudao-mall-uniapp/pages/booking/order-detail.vue yudao-mall-uniapp/pages/booking/logic.js yudao-mall-uniapp/tests/booking-page-smoke.test.mjs yudao-mall-uniapp/tests/booking-api-alignment.test.mjs
git commit -m "feat(booking): wire addon flow to product source truth"
```

### Task 5: Sync docs to new truth

**Files:**
- Modify: `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`
- Modify: `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-page-field-dictionary-v1.md`
- Modify: `docs/products/miniapp/2026-03-24-miniapp-booking-write-chain-closure-review-v1.md`
- Modify: `docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md`

**Step 1: Write the failing test**
- Add or extend existing booking smoke/ledger assertions if available to match new route truth and blocker wording.

**Step 2: Run test to verify it fails**
Run: `node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
Expected: FAIL if docs-backed route truth assertions are still stale.

**Step 3: Write minimal implementation**
- Update docs to state:
  - service-select is the only source of `spuId/skuId`
  - create no longer reads product source from slot
  - addon `UPGRADE / ADD_ITEM` require explicit selection
  - remaining blocker becomes mostly release evidence

**Step 4: Run test to verify it passes**
Run: `node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
Expected: PASS.

**Step 5: Commit**
```bash
git add docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md docs/products/miniapp/2026-03-16-miniapp-booking-runtime-page-field-dictionary-v1.md docs/products/miniapp/2026-03-24-miniapp-booking-write-chain-closure-review-v1.md docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md
git commit -m "docs(booking): sync product source truth"
```

### Task 6: Full verification

**Files:**
- Verify only

**Step 1: Run targeted tests**
Run: `node --test yudao-mall-uniapp/tests/booking-api-alignment.test.mjs yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
Expected: PASS.

**Step 2: Run formatting / guard checks**
Run: `git diff --check`
Expected: PASS.

Run: `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
Expected: PASS.

Run: `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
Expected: PASS.

**Step 3: Commit verification if needed**
```bash
git status --short
```
Expected: clean worktree.
