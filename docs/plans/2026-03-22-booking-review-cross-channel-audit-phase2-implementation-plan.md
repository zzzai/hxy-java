# Booking Review 跨通道派发审计二期 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 让 notify outbox 台账能够按评价聚合展示双通道整体状态，并在每一条 row 上明确当前跨通道审计结论。

**Architecture:** 后端统一基于 reviewId 聚合最新双通道 outbox 状态，前端消费 summary 和行级 audit 字段，不引入新的写操作或新的派发逻辑。

**Tech Stack:** Vue 3 + Element Plus + TypeScript, Spring Boot + MyBatis, Node test, JUnit 5 + Mockito

---

### Task 1: 前端失败测试先行

**Files:**
- Modify: `tests/booking-review-admin-notify-outbox.test.mjs`

**Step 1: Write the failing test**
- 断言 API 暴露 notify outbox summary 类型和方法
- 断言 notify outbox 页面存在“跨通道审计概览”“跨通道结论”“跨通道说明”等文案

**Step 2: Run test to verify it fails**
- `node --test tests/booking-review-admin-notify-outbox.test.mjs`

### Task 2: 后端失败测试先行

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewNotifyOutboxControllerTest.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceTest.java`

**Step 1: Write the failing test**
- 断言 service 能按 `reviewId` 聚合出 summary
- 断言 controller 能返回 summary
- 断言 page row 的 resp 已带跨通道审计字段

**Step 2: Run test to verify it fails**
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewNotifyOutboxControllerTest,BookingReviewNotifyOutboxServiceTest test`

### Task 3: 后端实现

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewNotifyOutboxController.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewNotifyOutboxRespVO.java`
- Create or Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewNotifyOutboxSummaryRespVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewNotifyOutboxPageReqVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingReviewNotifyOutboxService.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/BookingReviewNotifyOutboxMapper.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/support/BookingReviewAdminPrioritySupport.java`

**Step 1: Write minimal implementation**
- 新增 summary 查询
- 新增 review 级 audit snapshot 派生
- 给 page row 注入 `reviewAuditStage / reviewAuditLabel / reviewAuditDetail`

### Task 4: 前端实现

**Files:**
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/notifyOutbox/index.vue`

**Step 1: Write minimal implementation**
- 增加 summary 类型与请求方法
- 页面新增“跨通道审计概览”卡片与表格列

### Task 5: 文档同步

**Files:**
- Modify: `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`
- Modify: `docs/plans/2026-03-21-miniapp-booking-review-admin-evidence-ledger-v1.md`

### Task 6: 完整验证与提交

**Step 1: Run tests**
- `node --test tests/booking-review-admin-notify-outbox.test.mjs tests/booking-review-admin-detail-timeline.test.mjs tests/booking-review-admin-manager-routing.test.mjs`
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewNotifyOutboxControllerTest,BookingReviewNotifyOutboxServiceTest,BookingReviewManagerAccountRoutingControllerTest,BookingReviewManagerAccountRoutingQueryServiceImplTest test`

**Step 2: Run hygiene**
- `git diff --check`

**Step 3: Commit**
- `git commit -m "feat(booking-review): add cross-channel audit phase 2"`
