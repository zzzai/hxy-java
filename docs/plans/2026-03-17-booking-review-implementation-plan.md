# Booking Review Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a new booking-linked service-quality review domain for the miniapp, backend booking module, and admin recovery workflow without reusing product comment runtime truth.

**Architecture:** Introduce a dedicated `booking review` domain inside the booking module with its own DO/Mapper/Service/Controller/API surface. Wire miniapp review entry points from booking order pages, add admin review recovery operations, and keep release truth conservative until routes, APIs, tests, and operational docs all land.

**Tech Stack:** Vue 3 uniapp, existing `sheep` request layer, Spring Boot booking module, MyBatis mapper/dataobject pattern, existing booking test suites, admin Vue overlay conventions, markdown governance docs.

---

### Task 1: Add Booking Review Data Model And Persistence

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/BookingReviewDO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/BookingReviewMapper.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/BookingReviewLevelEnum.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/BookingReviewFollowStatusEnum.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/BookingReviewDisplayStatusEnum.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/ErrorCodeConstants.java`
- Create: `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-17-hxy-booking-review-init.sql`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/dal/mysql/BookingReviewMapperTest.java`

**Step 1: Write the failing mapper test**

```java
@Test
void testInsertAndSelectByBookingOrderId() {
    BookingReviewDO review = new BookingReviewDO();
    review.setBookingOrderId(1001L);
    review.setStoreId(2001L);
    review.setTechnicianId(3001L);
    review.setOverallScore(2);
    mapper.insert(review);

    BookingReviewDO actual = mapper.selectOne(BookingReviewDO::getBookingOrderId, 1001L);
    assertNotNull(actual);
    assertEquals(2, actual.getOverallScore());
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -pl ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking -Dtest=BookingReviewMapperTest test`
Expected: FAIL because `BookingReviewDO` and `BookingReviewMapper` do not exist.

**Step 3: Write minimal persistence implementation**

Add:
- `BookingReviewDO` with the approved keys and status fields
- `BookingReviewMapper` with insert and simple query support
- enum types for review level and follow status
- SQL init script for the review table and essential indexes
- minimal error code anchors for duplicate review, ineligible review, and review-not-found

**Step 4: Run test to verify it passes**

Run: `mvn -pl ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking -Dtest=BookingReviewMapperTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/BookingReviewDO.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/BookingReviewMapper.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/BookingReviewLevelEnum.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/BookingReviewFollowStatusEnum.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/BookingReviewDisplayStatusEnum.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/ErrorCodeConstants.java \
  ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-17-hxy-booking-review-init.sql \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/dal/mysql/BookingReviewMapperTest.java

git commit -m "feat(booking): add review persistence model"
```

### Task 2: Add Booking Review Service And App APIs

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingReviewService.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewServiceImpl.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/convert/BookingReviewConvert.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppBookingReviewController.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/vo/AppBookingReviewCreateReqVO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/vo/AppBookingReviewEligibilityRespVO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/vo/AppBookingReviewRespVO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/vo/AppBookingReviewPageReqVO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/vo/AppBookingReviewSummaryRespVO.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewServiceImplTest.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/app/AppBookingReviewControllerTest.java`

**Step 1: Write the failing service and controller tests**

```java
@Test
void testCreateReviewRejectsIncompleteOrder() {
    AppBookingReviewCreateReqVO req = new AppBookingReviewCreateReqVO();
    req.setBookingOrderId(1001L);
    req.setOverallScore(2);
    assertThrows(ServiceException.class, () -> service.createReview(10L, req));
}

@Test
void testEligibilityReturnsTrueForCompletedUnreviewedOrder() {
    CommonResult<AppBookingReviewEligibilityRespVO> result = controller.getEligibility(1001L);
    assertTrue(result.getData().getEligible());
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -pl ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking -Dtest=BookingReviewServiceImplTest,AppBookingReviewControllerTest test`
Expected: FAIL because service/controller/VO classes do not exist.

**Step 3: Write minimal service and app controller implementation**

Implement:
- eligibility rule: only completed booking orders and only once
- create review rule: bind review to booking order, store, technician, and service item
- derive `reviewLevel` and initial `serviceFollowStatus`
- list/get/summary queries for member-side readback
- controller endpoints:
  - `GET /booking/review/eligibility`
  - `POST /booking/review/create`
  - `GET /booking/review/page`
  - `GET /booking/review/get`
  - `GET /booking/review/summary`

**Step 4: Run test to verify it passes**

Run: `mvn -pl ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking -Dtest=BookingReviewServiceImplTest,AppBookingReviewControllerTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingReviewService.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewServiceImpl.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/convert/BookingReviewConvert.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppBookingReviewController.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/vo/AppBookingReviewCreateReqVO.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/vo/AppBookingReviewEligibilityRespVO.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/vo/AppBookingReviewRespVO.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/vo/AppBookingReviewPageReqVO.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/vo/AppBookingReviewSummaryRespVO.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewServiceImplTest.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/app/AppBookingReviewControllerTest.java

git commit -m "feat(booking): add app booking review APIs"
```

### Task 3: Add Admin Review Recovery APIs

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewController.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewPageReqVO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewRespVO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewReplyReqVO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewFollowUpdateReqVO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewDashboardRespVO.java`
- Test: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewControllerTest.java`

**Step 1: Write the failing admin controller tests**

```java
@Test
void testPageReturnsLowScoreQueue() throws Exception {
    mockMvc.perform(get("/admin-api/booking/review/page").param("riskLevel", "urgent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.list").isArray());
}

@Test
void testReplyUpdatesReplyStatus() throws Exception {
    mockMvc.perform(post("/admin-api/booking/review/reply")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"reviewId\":1001,\"replyContent\":\"已联系用户\"}"))
        .andExpect(status().isOk());
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -pl ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest test`
Expected: FAIL because admin controller and request VOs do not exist.

**Step 3: Write minimal admin recovery implementation**

Implement:
- page query for review ledger and low-score queue
- detail query
- reply endpoint
- follow-up status update endpoint
- dashboard summary endpoint for store/technician/service aggregates
- permission and audit fields consistent with booking admin patterns

**Step 4: Run test to verify it passes**

Run: `mvn -pl ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewController.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewPageReqVO.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewRespVO.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewReplyReqVO.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewFollowUpdateReqVO.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewDashboardRespVO.java \
  ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewControllerTest.java

git commit -m "feat(booking-admin): add review recovery controllers"
```

### Task 4: Add Miniapp Review API Client And Routes

**Files:**
- Modify: `yudao-mall-uniapp/pages.json`
- Create: `yudao-mall-uniapp/sheep/api/trade/review.js`
- Create: `yudao-mall-uniapp/pages/booking/review-add.vue`
- Create: `yudao-mall-uniapp/pages/booking/review-result.vue`
- Create: `yudao-mall-uniapp/pages/booking/review-list.vue`
- Test: `yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs`
- Test: `yudao-mall-uniapp/tests/booking-review-api-alignment.test.mjs`

**Step 1: Write the failing frontend tests**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

test('pages.json registers booking review routes', () => {
  const pages = fs.readFileSync('yudao-mall-uniapp/pages.json', 'utf8');
  assert.match(pages, /"path": "review-add"/);
  assert.match(pages, /"path": "review-result"/);
  assert.match(pages, /"path": "review-list"/);
});
```

**Step 2: Run test to verify it fails**

Run: `node --test yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs yudao-mall-uniapp/tests/booking-review-api-alignment.test.mjs`
Expected: FAIL because routes and API file do not exist.

**Step 3: Write minimal miniapp route and API implementation**

Implement:
- new review routes in booking subpackage
- `ReviewApi` methods for eligibility/create/page/get/summary
- review add/result/list page shells with explicit empty and failure states
- keep page state naming aligned with the approved design

**Step 4: Run test to verify it passes**

Run: `node --test yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs yudao-mall-uniapp/tests/booking-review-api-alignment.test.mjs`
Expected: PASS.

**Step 5: Commit**

```bash
git add yudao-mall-uniapp/pages.json \
  yudao-mall-uniapp/sheep/api/trade/review.js \
  yudao-mall-uniapp/pages/booking/review-add.vue \
  yudao-mall-uniapp/pages/booking/review-result.vue \
  yudao-mall-uniapp/pages/booking/review-list.vue \
  yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs \
  yudao-mall-uniapp/tests/booking-review-api-alignment.test.mjs

git commit -m "feat(miniapp): add booking review pages and api client"
```

### Task 5: Wire Review Entry From Booking Order Pages

**Files:**
- Modify: `yudao-mall-uniapp/pages/booking/order-list.vue`
- Modify: `yudao-mall-uniapp/pages/booking/order-detail.vue`
- Modify: `yudao-mall-uniapp/pages/booking/logic.js`
- Modify: `yudao-mall-uniapp/sheep/api/trade/booking.js`
- Test: `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
- Test: `yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs`

**Step 1: Write the failing entry-point tests**

```js
test('completed booking orders expose review CTA', () => {
  const source = fs.readFileSync('yudao-mall-uniapp/pages/booking/order-detail.vue', 'utf8');
  assert.match(source, /去评价|评价/);
});
```

**Step 2: Run test to verify it fails**

Run: `node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs`
Expected: FAIL because review CTA and routing are not wired.

**Step 3: Write minimal CTA and navigation implementation**

Implement:
- CTA visibility only for completed and unreviewed bookings
- navigation from order list and order detail to `review-add`
- helper method to refresh review eligibility after submit
- keep cancel/addon existing behavior intact

**Step 4: Run test to verify it passes**

Run: `node --test yudao-mall-uniapp/tests/booking-page-smoke.test.mjs yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs`
Expected: PASS.

**Step 5: Commit**

```bash
git add yudao-mall-uniapp/pages/booking/order-list.vue \
  yudao-mall-uniapp/pages/booking/order-detail.vue \
  yudao-mall-uniapp/pages/booking/logic.js \
  yudao-mall-uniapp/sheep/api/trade/booking.js \
  yudao-mall-uniapp/tests/booking-page-smoke.test.mjs \
  yudao-mall-uniapp/tests/booking-review-page-smoke.test.mjs

git commit -m "feat(miniapp): wire booking review entry points"
```

### Task 6: Add Admin Overlay Review Pages

**Files:**
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/index.vue`
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/dashboard/index.vue`
- Test: `docs/plans/2026-03-17-miniapp-booking-review-service-recovery-runbook-v1.md`

**Step 1: Write the failing UI smoke checklist in the plan**

```md
- [ ] Review ledger page can filter by score, risk level, store, technician, and follow status.
- [ ] Low-score detail page can update reply and follow status.
- [ ] Dashboard page renders store/technician/service aggregates.
```

**Step 2: Run manual sanity check to verify it is missing**

Run: `find ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review -maxdepth 3 -type f`
Expected: no files found.

**Step 3: Write minimal admin page implementation**

Implement:
- API bindings for page/get/reply/follow-up/dashboard
- review ledger list page
- detail page for low-score handling
- dashboard summary page
- match existing booking admin page structure and visual conventions

**Step 4: Run sanity check to verify files exist**

Run: `find ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review -maxdepth 3 -type f | sort`
Expected: three view files listed.

**Step 5: Commit**

```bash
git add ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts \
  ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/index.vue \
  ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue \
  ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/dashboard/index.vue

git commit -m "feat(booking-admin-ui): add review recovery pages"
```

### Task 7: Close Docs, Gates, And Truth Ledgers

**Files:**
- Create: `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- Create: `docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md`
- Create: `docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md`
- Create: `docs/contracts/2026-03-17-miniapp-booking-review-errorcode-and-failure-mode-v1.md`
- Create: `docs/plans/2026-03-17-miniapp-booking-review-service-recovery-runbook-v1.md`
- Create: `docs/plans/2026-03-17-miniapp-booking-review-release-gate-v1.md`
- Create: `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`
- Modify: `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
- Modify: `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`
- Modify: `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`
- Modify: `docs/products/miniapp/2026-03-09-miniapp-release-decision-pack-v1.md`
- Modify: `docs/products/miniapp/2026-03-08-miniapp-doc-gap-closure-index-v1.md`

**Step 1: Write the failing truth checklist**

```md
- [ ] booking review is documented as a new planned domain, not a product comment alias.
- [ ] no ledger marks booking review as ACTIVE before route + API + controller evidence exists.
- [ ] release pack keeps booking review as Can Develop / Cannot Release until runtime evidence exists.
```

**Step 2: Run document grep to verify the new docs are missing**

Run: `rg -n "booking review" docs/products/miniapp docs/contracts docs/plans`
Expected: no authoritative booking review doc set yet.

**Step 3: Write the docs and update ledgers conservatively**

Implement:
- full PRD/contract/runbook/gate/final review set
- update ledgers and release docs without runtime inflation
- keep product comment truth and booking review truth separate

**Step 4: Run document checks**

Run: `git diff --check && CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh && CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
Expected: PASS.

**Step 5: Commit**

```bash
git add docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md \
  docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md \
  docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md \
  docs/contracts/2026-03-17-miniapp-booking-review-errorcode-and-failure-mode-v1.md \
  docs/plans/2026-03-17-miniapp-booking-review-service-recovery-runbook-v1.md \
  docs/plans/2026-03-17-miniapp-booking-review-release-gate-v1.md \
  docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md \
  docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md \
  docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md \
  docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md \
  docs/products/miniapp/2026-03-09-miniapp-release-decision-pack-v1.md \
  docs/products/miniapp/2026-03-08-miniapp-doc-gap-closure-index-v1.md

git commit -m "docs(booking-review): close review domain truth and gate docs"
```

