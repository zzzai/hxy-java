# Booking Review Pending Init Filter Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 booking review 后台台账增加“历史未初始化差评”真实筛选和提示，帮助运营识别尚未进入店长待办池的历史差评。

**Architecture:** 后端在 admin page query 上新增 `onlyPendingInit` 布尔筛选，并把语义固定为 `reviewLevel = 3 && managerTodoStatus IS NULL`；前端台账只消费该筛选并补充提示文案，不改变写路径、详情页或看板统计。

**Tech Stack:** Java/Spring Boot/JUnit、Vue 3 overlay、Node built-in test、Markdown docs。

---

### Task 1: 落设计与计划文档

**Files:**
- Create: `docs/plans/2026-03-21-booking-review-pending-init-filter-design.md`
- Create: `docs/plans/2026-03-21-booking-review-pending-init-filter-implementation-plan.md`

**Step 1:** 写设计文档，冻结范围、真值定义、No-Go。

**Step 2:** 写实施计划，明确后端筛选、前端筛选、测试和验证命令。

### Task 2: 先写后端红灯测试

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/dal/mysql/BookingReviewMapperTest.java`

**Step 1:** 新增失败测试，插入多条评价记录，验证 `onlyPendingInit=true` 时只返回 `差评 + managerTodoStatus=null` 记录。

**Step 2:** 运行：
- `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewMapperTest test`

**Expected:** FAIL，因为当前 request VO 和 mapper 还不支持该筛选。

### Task 3: 实现后端筛选逻辑

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewPageReqVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/BookingReviewMapper.java`

**Step 1:** 在 `BookingReviewPageReqVO` 增加 `onlyPendingInit` 字段。

**Step 2:** 在 `buildAdminQuery(...)` 里追加 `reviewLevel = 3 && managerTodoStatus IS NULL` 条件。

**Step 3:** 运行：
- `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewMapperTest test`

**Expected:** PASS。

### Task 4: 先写前端红灯测试

**Files:**
- Modify: `tests/booking-review-admin-query-helpers.test.mjs`

**Step 1:** 新增失败测试，验证 `parseLedgerQuery()` 能解析 `onlyPendingInit=true`。

**Step 2:** 运行：
- `node --test tests/booking-review-admin-query-helpers.test.mjs`

**Expected:** FAIL，因为 helper 默认结构和布尔解析列表里还没有该字段。

### Task 5: 实现前端筛选与提示

**Files:**
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/queryHelpers.mjs`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/index.vue`

**Step 1:** 在 API 查询类型中增加 `onlyPendingInit?: boolean`。

**Step 2:** 在 query helper 默认结构和布尔解析列表中接入 `onlyPendingInit`。

**Step 3:** 在台账页面增加筛选项和只读提示文案。

**Step 4:** 运行：
- `node --test tests/booking-review-admin-query-helpers.test.mjs`

**Expected:** PASS。

### Task 6: 更新 backlog 并做最终验证

**Files:**
- Modify: `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`

**Step 1:** 把 `A4` 从“可进入设计”更新为“已落地（admin-only）”。

**Step 2:** 运行最终验证：
- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
- `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewMapperTest,BookingReviewControllerTest test`
- `node --test tests/booking-review-admin-query-helpers.test.mjs`

**Step 3:** 提交实现。
