# Booking Review Admin A1 A2 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 booking review 后台完成 A1 可读名称展示和 A2 看板带条件跳回台账。

**Architecture:** 后端在 admin review response 上做 best-effort enrich，补 `storeName / technicianName / memberNickname` 三个只读字段；前端只消费新增字段，不改业务状态机。看板到台账的筛选映射下沉为纯函数模块，通过 query 参数实现 drill-down。

**Tech Stack:** Java/Spring Boot/JUnit、Vue 3 overlay、Node built-in test、Markdown docs。

---

### Task 1: 落设计文档并冻结范围

**Files:**
- Create: `docs/plans/2026-03-19-booking-review-admin-a1-a2-design.md`
- Create: `docs/plans/2026-03-19-booking-review-admin-a1-a2-implementation-plan.md`

**Step 1:** 写设计文档，固定 A1/A2 范围、依赖、No-Go。

**Step 2:** 写实施计划，明确测试优先和文件清单。

**Step 3:** 提交 docs。

### Task 2: 先写 controller 红灯测试

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewControllerTest.java`

**Step 1:** 新增失败测试，验证 `page/get` 返回会带 `storeName / technicianName / memberNickname`。

**Step 2:** 运行：
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest test`

**Expected:** FAIL，因为当前响应里没有名称 enrich。

### Task 3: 实现后端响应增强

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewController.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewRespVO.java`

**Step 1:** 在 VO 里新增三个只读展示字段。

**Step 2:** 在 controller 里引入：
- `ProductStoreService`
- `TechnicianService`
- `MemberUserApi`

**Step 3:** 对 `page/get` 响应做 best-effort enrich。

**Step 4:** 运行：
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest test`

**Expected:** PASS。

### Task 4: 为看板到台账 query 逻辑先写纯函数红灯测试

**Files:**
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/queryHelpers.mjs`
- Create: `tests/booking-review-admin-query-helpers.test.mjs`

**Step 1:** 先写测试，验证：
- 看板卡片 key 会映射成正确 query
- 台账会把 query 正确解析为筛选状态

**Step 2:** 运行：
- `node --test tests/booking-review-admin-query-helpers.test.mjs`

**Expected:** FAIL，因为 helper 尚未实现。

### Task 5: 实现前端 helper 与页面消费

**Files:**
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/index.vue`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/dashboard/index.vue`
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/queryHelpers.mjs`

**Step 1:** 在 API 类型里声明新增只读字段。

**Step 2:** 实现 helper：
- `buildLedgerQueryFromDashboardCardKey`
- `parseLedgerQuery`

**Step 3:** 台账页接入 route query 解析。

**Step 4:** 看板卡片接入“查看台账”动作。

**Step 5:** 台账/详情展示名称 + ID。

**Step 6:** 运行：
- `node --test tests/booking-review-admin-query-helpers.test.mjs`

**Expected:** PASS。

### Task 6: 更新 backlog 文档并做最终验证

**Files:**
- Modify: `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`

**Step 1:** 把 `A1 / A2` 从候选态更新为已落地或已执行。

**Step 2:** 运行最终验证：
- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
- `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest,BookingReviewServiceImplTest test`
- `node --test tests/booking-review-admin-query-helpers.test.mjs`

**Step 3:** 提交实现。
