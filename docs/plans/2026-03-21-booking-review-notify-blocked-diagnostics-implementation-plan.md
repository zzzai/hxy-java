# Booking Review Notify Blocked Diagnostics Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 booking review notify outbox 补齐标准化阻断诊断字段和运营可读展示，让运营快速判断“为什么不能重试、缺什么主数据”。

**Architecture:** 不改数据库结构，在 controller response 层统一派生诊断字段，前端台账页和详情页直接消费这些字段。这样先解决“可观测”和“可运营”，把路由真值闭环留给后续 P1-2。

**Tech Stack:** Spring Boot、JUnit5、Vue 3 overlay、Node test、Markdown docs。

---

### Task 1: 冻结设计与计划文档

**Files:**
- Create: `docs/plans/2026-03-21-booking-review-notify-blocked-diagnostics-design.md`
- Create: `docs/plans/2026-03-21-booking-review-notify-blocked-diagnostics-implementation-plan.md`

**Step 1:** 写设计文档，固定诊断码、边界、UI 行为。

**Step 2:** 写实现计划文档，拆成 TDD 步骤。

**Step 3:** 运行格式检查。

Run: `git diff --check -- docs/plans/2026-03-21-booking-review-notify-blocked-diagnostics-design.md docs/plans/2026-03-21-booking-review-notify-blocked-diagnostics-implementation-plan.md`

Expected: PASS

### Task 2: 先写红灯测试

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewNotifyOutboxControllerTest.java`
- Modify: `tests/booking-review-admin-notify-outbox.test.mjs`

**Step 1: Write the failing test**
- controller test 覆盖标准化诊断字段
- node test 覆盖快捷筛选、诊断结论、修复建议、重试控制

**Step 2: Run test to verify it fails**

Run:
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewNotifyOutboxControllerTest test`
- `node --test tests/booking-review-admin-notify-outbox.test.mjs`

Expected: FAIL

### Task 3: 实现后端标准化诊断字段

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewNotifyOutboxRespVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewNotifyOutboxController.java`

**Step 1:** 在 response VO 增加诊断字段。

**Step 2:** 在 controller `toResp(...)` 统一派生：
- `diagnosticCode`
- `diagnosticLabel`
- `diagnosticDetail`
- `repairHint`
- `manualRetryAllowed`

**Step 3:** 跑 controller test。

Run: `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewNotifyOutboxControllerTest test`

Expected: PASS

### Task 4: 实现前端诊断展示与快捷筛选

**Files:**
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/notifyOutbox/index.vue`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`

**Step 1:** 扩展前端类型，接收诊断字段。

**Step 2:** 台账页增加：
- 快捷筛选按钮
- 诊断结论
- 修复建议
- 按 `manualRetryAllowed` 控制按钮

**Step 3:** 详情页展示标准化诊断说明。

**Step 4:** 跑 node test。

Run: `node --test tests/booking-review-admin-notify-outbox.test.mjs`

Expected: PASS

### Task 5: 最终回归与提交

**Files:**
- Verify only

**Step 1:** 跑回归。

Run:
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest,BookingReviewNotifyOutboxControllerTest,BookingReviewServiceImplTest,BookingReviewNotifyOutboxServiceTest,BookingReviewNotifyDispatchJobTest test`
- `node --test tests/booking-review-admin-history-scan.test.mjs tests/booking-review-admin-query-helpers.test.mjs tests/booking-review-admin-detail-timeline.test.mjs tests/booking-review-admin-notify-outbox.test.mjs`
- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

Expected: PASS

**Step 2:** Commit

```bash
git add docs/plans/2026-03-21-booking-review-notify-blocked-diagnostics-design.md docs/plans/2026-03-21-booking-review-notify-blocked-diagnostics-implementation-plan.md ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewNotifyOutboxRespVO.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewNotifyOutboxController.java ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/notifyOutbox/index.vue ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewNotifyOutboxControllerTest.java tests/booking-review-admin-notify-outbox.test.mjs
git commit -m "feat(booking-review): add notify blocked diagnostics"
```
