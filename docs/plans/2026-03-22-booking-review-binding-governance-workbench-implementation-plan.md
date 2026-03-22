# Booking Review 门店绑定治理工作台 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在现有店长路由核查页上补齐治理优先级、来源闭环和核验新鲜度视图，让运营能快速判断哪类门店需要立即治理、转给谁、下一步该补什么。

**Architecture:** 后端查询服务统一派生治理字段和汇总计数，前端只消费这些派生值并提供快捷筛选与表格展示，保持 admin-only、只读治理边界。

**Tech Stack:** Vue 3 + Element Plus + TypeScript, Spring Boot + MyBatis, Node test, JUnit 5 + Mockito

---

### Task 1: 前端失败测试先行

**Files:**
- Modify: `tests/booking-review-admin-manager-routing.test.mjs`

**Step 1: Write the failing test**
- 断言 API 类型暴露治理字段与治理汇总字段
- 断言页面存在“治理工作台概览”“只看立即治理”“只看来源待闭环”“只看长期未核验”“交接摘要”等文本

**Step 2: Run test to verify it fails**
- Run: `node --test tests/booking-review-admin-manager-routing.test.mjs`

**Step 3: Write minimal implementation**
- 暂不实施，此任务只负责写失败测试

**Step 4: Run test to verify it fails correctly**
- 预期因字段/文案不存在而失败

### Task 2: 后端失败测试先行

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewManagerAccountRoutingQueryServiceImplTest.java`

**Step 1: Write the failing test**
- 断言查询服务可派生：
  - `IMMEDIATE_FIX / VERIFY_SOURCE / OBSERVE_READY`
  - `UNVERIFIED / STALE_VERIFY / RECENT_VERIFY`
  - `SOURCE_PENDING / SOURCE_READY`
- 断言 summary 能统计 `immediateFixCount / verifySourceCount / staleVerifyCount / sourcePendingCount`
  - 以及 `observeReadyCount`

**Step 2: Run test to verify it fails**
- Run: `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewManagerAccountRoutingQueryServiceImplTest test`

### Task 3: 扩展后端 VO 与查询派生

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewManagerAccountRoutingRespVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewManagerAccountRoutingSummaryRespVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewManagerAccountRoutingPageReqVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewManagerAccountRoutingQueryServiceImpl.java`

**Step 1: Write minimal implementation**
- 新增治理字段与筛选字段
- 在查询服务中派生治理阶段、优先级、核验状态、来源闭环状态、治理归口和交接摘要
- 在 summary 中统计治理卡片所需计数

**Step 2: Run targeted backend tests**
- Run: `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewManagerAccountRoutingQueryServiceImplTest,BookingReviewManagerAccountRoutingControllerTest test`

### Task 4: 前端类型与工作台 UI 实现

**Files:**
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/managerRouting/index.vue`

**Step 1: Write minimal implementation**
- 增加治理字段类型
- 增加治理概览卡片与快捷筛选
- 表格新增治理相关列

**Step 2: Run targeted frontend tests**
- Run: `node --test tests/booking-review-admin-manager-routing.test.mjs`

### Task 5: 文档与 backlog 同步

**Files:**
- Modify: `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`
- Modify: `docs/products/miniapp/2026-03-21-miniapp-booking-review-manager-account-routing-truth-v1.md`
- Modify: `docs/plans/2026-03-21-miniapp-booking-review-admin-evidence-ledger-v1.md`

**Step 1: Update docs**
- 记录治理工作台已落地
- 强调仍是 admin-only / read-only / Can Develop / Cannot Release

### Task 6: 完整验证与提交

**Files:**
- Verify all modified files

**Step 1: Run tests**
- `node --test tests/booking-review-admin-manager-routing.test.mjs tests/booking-review-admin-notify-outbox.test.mjs`
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewManagerAccountRoutingControllerTest,BookingReviewManagerAccountRoutingQueryServiceImplTest,BookingReviewNotifyOutboxControllerTest,BookingReviewNotifyOutboxServiceTest test`

**Step 2: Run hygiene**
- `git diff --check`

**Step 3: Commit**
- `git add ...`
- `git commit -m "feat(booking-review): add binding governance workbench"`
