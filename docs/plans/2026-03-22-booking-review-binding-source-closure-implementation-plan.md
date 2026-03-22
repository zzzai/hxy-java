# Booking Review 真实绑定来源闭环 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在店长路由治理工作台中补齐“来源真值分层”，让运营知道当前是来源已确认、来源缺失、联系人待转绑定、联系人缺失还是来源待复核，并给出明确下一步动作。

**Architecture:** 后端查询服务基于现有路由表和门店联系人主数据派生 `sourceTruth*` 字段与来源概览计数，前端只读消费这些字段做卡片、筛选、列表和详情展示，不新增写链、不改变派发逻辑。

**Tech Stack:** Vue 3 + Element Plus + TypeScript, Spring Boot + MyBatis, Node test, JUnit 5 + Mockito

---

### Task 1: 写前端失败测试

**Files:**
- Modify: `tests/booking-review-admin-manager-routing.test.mjs`

**Step 1: Write the failing test**
- 断言 API 类型存在：
  - `sourceTruthStage`
  - `sourceTruthLabel`
  - `sourceTruthDetail`
  - `sourceTruthActionHint`
  - `routeConfirmedCount`
  - `sourceMissingCount`
  - `contactOnlyPendingBindCount`
  - `contactMissingCount`
  - `verifyStaleCount`
- 断言页面存在：
  - `来源闭环概览`
  - `只看来源已确认`
  - `只看来源缺失`
  - `只看联系人待转绑定`
  - `只看联系人缺失`
  - `只看来源待复核`
  - `来源结论`
  - `来源说明`
  - `下一步动作`

**Step 2: Run test to verify it fails**
- Run: `node --test tests/booking-review-admin-manager-routing.test.mjs`

### Task 2: 写后端失败测试

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewManagerAccountRoutingQueryServiceImplTest.java`

**Step 1: Write the failing test**
- 新增来源真值阶段断言：
  - `ROUTE_CONFIRMED`
  - `SOURCE_MISSING`
  - `CONTACT_ONLY_PENDING_BIND`
  - `CONTACT_MISSING`
  - `VERIFY_STALE`
- 新增 summary 计数断言：
  - `routeConfirmedCount`
  - `sourceMissingCount`
  - `contactOnlyPendingBindCount`
  - `contactMissingCount`
  - `verifyStaleCount`
- 新增按 `sourceTruthStage` 过滤断言

**Step 2: Run test to verify it fails**
- Run: `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewManagerAccountRoutingQueryServiceImplTest test`

### Task 3: 实现后端派生逻辑

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewManagerAccountRoutingRespVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewManagerAccountRoutingSummaryRespVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewManagerAccountRoutingPageReqVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewManagerAccountRoutingQueryServiceImpl.java`

**Step 1: Write minimal implementation**
- 在 VO 中增加 `sourceTruth*` 字段和 summary 计数
- 在分页请求中增加 `sourceTruthStage`
- 在查询服务中基于路由状态、`source`、`lastVerifiedTime`、联系人信息派生来源真值分层
- 在分页筛选和 summary 统计中接入来源真值维度

**Step 2: Run targeted backend tests**
- Run: `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewManagerAccountRoutingControllerTest,BookingReviewManagerAccountRoutingQueryServiceImplTest test`

### Task 4: 实现前端只读增强

**Files:**
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/managerRouting/index.vue`

**Step 1: Write minimal implementation**
- 增加 API 类型
- 增加来源闭环概览卡片
- 增加来源真值快捷筛选
- 在详情和列表中展示来源结论、来源说明、下一步动作

**Step 2: Run targeted frontend tests**
- Run: `node --test tests/booking-review-admin-manager-routing.test.mjs`

### Task 5: 同步文档与证据

**Files:**
- Modify: `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`
- Modify: `docs/products/miniapp/2026-03-21-miniapp-booking-review-manager-account-routing-truth-v1.md`
- Modify: `docs/plans/2026-03-21-miniapp-booking-review-admin-evidence-ledger-v1.md`

**Step 1: Update docs**
- 记录来源真值分层已落地
- 明确保留 `admin-only / read-only / Can Develop / Cannot Release`

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
- `git commit -m "feat(booking-review): close binding source truth"`
