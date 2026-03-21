# Booking Review SLA Priority Ops Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 booking review 后台补齐“即将超时 + 统一优先级 + 通知风险摘要”值班视图。

**Architecture:** 在后端统一派生 `managerSlaStage / priorityLevel / priorityReason / notifyRiskSummary`，并扩展 dashboard summary 以包含 due-soon 计数；前端台账和看板只消费新增只读字段，不改变任何已有业务状态机。

**Tech Stack:** Java/Spring Boot/JUnit、Vue 3 overlay、Node built-in test、Markdown docs。

---

### Task 1: 冻结设计和实施计划

**Files:**
- Create: `docs/plans/2026-03-21-booking-review-sla-priority-ops-design.md`
- Create: `docs/plans/2026-03-21-booking-review-sla-priority-ops-implementation-plan.md`

**Step 1:** 落设计文档，固定 due-soon 窗口、优先级规则和 No-Go。

**Step 2:** 落实施计划，明确 TDD、文件范围和验证命令。

### Task 2: 先写后端红灯测试

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewServiceImplTest.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewControllerTest.java`

**Step 1:** 在 service test 中写红灯：
- `CLAIM_DUE_SOON / FIRST_ACTION_DUE_SOON / CLOSE_DUE_SOON` 过滤
- dashboard summary 返回 due-soon 计数

**Step 2:** 在 controller test 中写红灯：
- page/get 返回 `managerSlaStage / priorityLevel / priorityReason / notifyRiskSummary`

**Step 3:** 运行：
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewServiceImplTest,BookingReviewControllerTest test`

**Expected:** FAIL，缺少新增派生字段和 due-soon 规则。

### Task 3: 实现后端派生规则与看板汇总

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/support/BookingReviewAdminPrioritySupport.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewRespVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewDashboardRespVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewController.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewPageReqVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/BookingReviewNotifyOutboxMapper.java`

**Step 1:** 抽公共 support，统一派生 `managerSlaStage` 和优先级规则。

**Step 2:** 扩展 `managerSlaStatus` 过滤，支持 due-soon 状态。

**Step 3:** 扩展 dashboard summary，增加三类 due-soon 计数。

**Step 4:** controller 批量读取 notify outbox，为 page/get 补 `notifyRiskSummary`。

**Step 5:** 运行：
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewServiceImplTest,BookingReviewControllerTest test`

**Expected:** PASS。

### Task 4: 先写前端红灯测试

**Files:**
- Modify: `tests/booking-review-admin-query-helpers.test.mjs`
- Modify: `tests/booking-review-admin-ledger-efficiency.test.mjs`

**Step 1:** 增加失败测试：
- 看板 due-soon 卡片 query 映射
- 台账存在优先级 / 通知风险列
- API 类型包含新增字段

**Step 2:** 运行：
- `node --test tests/booking-review-admin-query-helpers.test.mjs tests/booking-review-admin-ledger-efficiency.test.mjs`

**Expected:** FAIL。

### Task 5: 实现前端消费

**Files:**
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/queryHelpers.mjs`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/index.vue`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/dashboard/index.vue`
- Modify: `tests/booking-review-admin-query-helpers.test.mjs`
- Modify: `tests/booking-review-admin-ledger-efficiency.test.mjs`

**Step 1:** API 类型补齐新增字段。

**Step 2:** 看板新增 due-soon 卡片和 query 映射。

**Step 3:** 台账新增优先级 / 原因 / 通知风险列，并补 due-soon 快捷入口。

**Step 4:** 运行：
- `node --test tests/booking-review-admin-query-helpers.test.mjs tests/booking-review-admin-ledger-efficiency.test.mjs tests/booking-review-admin-notify-outbox.test.mjs tests/booking-review-admin-manager-routing.test.mjs`

**Expected:** PASS。

### Task 6: 更新 backlog 与最终验证

**Files:**
- Modify: `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`

**Step 1:** 把“值班优先级视图 / due-soon 入口”记入 backlog 现状。

**Step 2:** 运行最终验证：
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest,BookingReviewServiceImplTest,BookingReviewNotifyOutboxControllerTest,BookingReviewManagerAccountRoutingQueryServiceImplTest,BookingReviewNotifyOutboxServiceTest,BookingReviewManagerTodoSlaReminderJobTest test`
- `node --test tests/booking-review-admin-query-helpers.test.mjs tests/booking-review-admin-ledger-efficiency.test.mjs tests/booking-review-admin-history-scan.test.mjs tests/booking-review-admin-detail-timeline.test.mjs tests/booking-review-admin-notify-outbox.test.mjs tests/booking-review-admin-manager-routing.test.mjs tests/booking-review-admin-sla-reminder.test.mjs`
- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

**Step 3:** 提交实现。
