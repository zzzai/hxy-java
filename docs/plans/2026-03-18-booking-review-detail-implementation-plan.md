# Booking Review Detail Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a member-side booking review detail page so users can open a past review from `review-list` and read full score, tags, images, and merchant reply using the existing booking review app API.

**Architecture:** Reuse the current booking review domain and the existing `GET /booking/review/get` endpoint. Add a new miniapp route and page, wire navigation from the review list, keep internal recovery fields hidden from users, and update truth docs conservatively after UI and tests land.

**Tech Stack:** Vue 3 uniapp, existing `sheep` router and request layer, Node built-in test runner, markdown governance docs.

---

### Task 1: Add failing smoke coverage for the new route and detail consumption

**Files:**
- Modify: `yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs`
- Test: `yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs`

**Step 1: Write the failing test**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (file) => fs.readFileSync(new URL(`../${file}`, import.meta.url), 'utf8');

test('pages.json registers booking review detail route', () => {
  const pages = read('pages.json');
  assert.match(pages, /"path": "review-detail"/);
});

test('review list navigates to review detail and detail page uses getReview', () => {
  const listSource = read('pages/booking/review-list.vue');
  const detailSource = read('pages/booking/review-detail.vue');
  assert.match(listSource, /review-detail/);
  assert.match(detailSource, /getReview/);
});
```

**Step 2: Run test to verify it fails**

Run: `node --test yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs`
Expected: FAIL because `review-detail` route and page do not exist yet.

**Step 3: Do not implement yet**

Stop after the red result. The purpose of this task is to prove the new route/page behavior is not already present.

**Step 4: Commit nothing yet**

No commit in this task. Implementation will follow in later tasks.

### Task 2: Register the new miniapp route

**Files:**
- Modify: `yudao-mall-uniapp/pages.json`
- Test: `yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs`

**Step 1: Write minimal implementation**

Add a new booking subpage entry:

```json
{
  "path": "review-detail",
  "style": {
    "navigationBarTitleText": "评价详情"
  },
  "meta": {
    "auth": true
  }
}
```

Place it next to the existing booking review routes to keep package truth readable.

**Step 2: Run test to verify partial progress**

Run: `node --test yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs`
Expected: The route assertion may pass, but the detail page assertion should still fail because `review-detail.vue` is not created yet.

**Step 3: Commit nothing yet**

No commit in this task. Keep working until the page exists and tests are green.

### Task 3: Implement `review-detail.vue` with explicit empty/failure states

**Files:**
- Create: `yudao-mall-uniapp/pages/booking/review-detail.vue`
- Test: `yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs`

**Step 1: Write the minimal page implementation**

Create a page that:
- reads `id` from route query
- calls `BookingReviewApi.getReview(id)` on load
- shows an explicit empty/error state when `id` is invalid or request fails
- shows these fields on success:
  - `reviewLevel`
  - `submitTime`
  - `bookingOrderId`
  - `overallScore`
  - `serviceScore`
  - `technicianScore`
  - `environmentScore`
  - `tags`
  - `content`
  - `picUrls`
  - `replyContent`
- hides internal fields such as `serviceOrderId`

Use clear, service-oriented section blocks instead of a dense debug dump.

**Step 2: Run test to verify it passes**

Run: `node --test yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs`
Expected: PASS for route and detail-page-consumption assertions.

**Step 3: Refactor lightly if needed**

Keep only the minimum helpers required for:
- review level label
- invalid state detection
- image grid / reply empty-state handling

Do not add extra actions beyond the approved scope.

### Task 4: Wire navigation from `review-list.vue`

**Files:**
- Modify: `yudao-mall-uniapp/pages/booking/review-list.vue`
- Test: `yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs`

**Step 1: Add navigation behavior**

Make each review card open the new detail page:

```js
function goReviewDetail(id) {
  if (!id) return;
  sheep.$router.go('/pages/booking/review-detail', { id });
}
```

Bind the card container or a clear CTA area to this action.

**Step 2: Preserve list-page truth**

Do not turn the list into a heavy detail surface.
Keep the card summary concise:
- level
- submit time
- booking order id
- overall score
- text preview
- reply hint if present

**Step 3: Run test to verify it stays green**

Run: `node --test yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs`
Expected: PASS.

### Task 5: Add focused page behavior coverage for detail success and empty states

**Files:**
- Create: `yudao-mall-uniapp/tests/booking-review-detail-page.test.mjs`
- Test: `yudao-mall-uniapp/tests/booking-review-detail-page.test.mjs`

**Step 1: Write the failing test**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (file) => fs.readFileSync(new URL(`../${file}`, import.meta.url), 'utf8');

test('review detail defines success fields and explicit empty-state copy', () => {
  const source = read('pages/booking/review-detail.vue');
  assert.match(source, /bookingOrderId/);
  assert.match(source, /overallScore/);
  assert.match(source, /serviceScore/);
  assert.match(source, /technicianScore/);
  assert.match(source, /environmentScore/);
  assert.match(source, /picUrls/);
  assert.match(source, /replyContent/);
  assert.match(source, /评价不存在或参数异常/);
});
```

**Step 2: Run test to verify it fails**

Run: `node --test yudao-mall-uniapp/tests/booking-review-detail-page.test.mjs`
Expected: FAIL until the page source includes all required rendering and empty-state copy.

**Step 3: Adjust the page minimally to satisfy the approved design**

If the failure shows missing approved fields or empty-state copy, add only those missing pieces.

**Step 4: Run test to verify it passes**

Run: `node --test yudao-mall-uniapp/tests/booking-review-detail-page.test.mjs`
Expected: PASS.

### Task 6: Run the full miniapp review test set

**Files:**
- Test: `yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs`
- Test: `yudao-mall-uniapp/tests/booking-review-detail-page.test.mjs`
- Test: `yudao-mall-uniapp/tests/booking-review-api-alignment.test.mjs`

**Step 1: Run the combined test command**

Run: `node --test yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs yudao-mall-uniapp/tests/booking-review-detail-page.test.mjs yudao-mall-uniapp/tests/booking-review-api-alignment.test.mjs`
Expected: PASS.

**Step 2: If any test fails, fix only the minimal approved UI behavior**

Do not add out-of-scope actions such as customer service, re-review, or compensation entry.

### Task 7: Update booking review truth docs conservatively

**Files:**
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md`
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- Modify: `docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md`
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`

**Step 1: Update route and page truth**

Add the new route:
- `/pages/booking/review-detail`

Add the new binding truth:
- miniapp now consumes `GET /booking/review/get`

**Step 2: Keep release language conservative**

Explicitly preserve:
- `Doc Closed / Can Develop / Cannot Release`
- no feature flag / rollout / runtime sample pack proof
- no automatic reward / compensation / manager-notify evidence

**Step 3: Run formatting and guard checks for docs**

Run:
- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

Expected: PASS.

### Task 8: Verify the full change set and commit

**Files:**
- Modify: `yudao-mall-uniapp/pages.json`
- Modify: `yudao-mall-uniapp/pages/booking/review-list.vue`
- Create: `yudao-mall-uniapp/pages/booking/review-detail.vue`
- Modify: `yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs`
- Create: `yudao-mall-uniapp/tests/booking-review-detail-page.test.mjs`
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md`
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- Modify: `docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md`
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`

**Step 1: Run final verification**

Run:
- `node --test yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs yudao-mall-uniapp/tests/booking-review-detail-page.test.mjs yudao-mall-uniapp/tests/booking-review-api-alignment.test.mjs`
- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

Expected: PASS.

**Step 2: Commit**

```bash
git add yudao-mall-uniapp/pages.json \
  yudao-mall-uniapp/pages/booking/review-list.vue \
  yudao-mall-uniapp/pages/booking/review-detail.vue \
  yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs \
  yudao-mall-uniapp/tests/booking-review-detail-page.test.mjs \
  docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md \
  docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md \
  docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md \
  docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md

git commit -m "feat(booking-review): add member review detail page"
```
