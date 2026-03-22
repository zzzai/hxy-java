# Booking Review Manager Routing Coverage Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 booking review 店长路由核查页补齐全量覆盖率摘要和缺失绑定运营视图。

**Architecture:** 后端新增 manager routing summary 接口，并在 service 层基于真实路由快照做汇总与筛选；前端 manager routing 页消费 summary 与新筛选条件，继续保持 admin-only、只读、不可在线改绑。

**Tech Stack:** Spring Boot / MyBatis / Vue 3 overlay / Node built-in test / JUnit / Markdown docs

---

### Task 1: 固定设计和实现计划

**Files:**
- Create: `docs/plans/2026-03-22-booking-review-manager-routing-coverage-design.md`
- Create: `docs/plans/2026-03-22-booking-review-manager-routing-coverage-implementation-plan.md`

**Step 1:** 固定 summary + quick filters 方案与 No-Go。

### Task 2: 先写红灯测试

**Files:**
- Modify: `tests/booking-review-admin-manager-routing.test.mjs`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewManagerAccountRoutingQueryServiceImplTest.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewManagerAccountRoutingControllerTest.java`

**Step 1:** Node 红灯覆盖：
- summary type / method
- manager routing 页的覆盖率卡片
- 缺失绑定快捷筛选

**Step 2:** Java 红灯覆盖：
- summary 汇总
- routing filter 生效
- controller summary endpoint

**Step 3:** 运行红灯：
- `node --test tests/booking-review-admin-manager-routing.test.mjs`
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewManagerAccountRoutingControllerTest,BookingReviewManagerAccountRoutingQueryServiceImplTest test`

### Task 3: 实现后端 summary 与过滤

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingReviewManagerAccountRoutingQueryService.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewManagerAccountRoutingController.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewManagerAccountRoutingPageReqVO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewManagerAccountRoutingSummaryRespVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/BookingReviewManagerAccountRoutingMapper.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewManagerAccountRoutingQueryServiceImpl.java`

**Step 1:** 扩展分页请求参数：
- `routingStatus`
- `appRoutingStatus`
- `wecomRoutingStatus`

**Step 2:** 新增批量读取 latest routing 的 mapper 能力。

**Step 3:** 在 service 中：
- 拉取符合基础筛选条件的门店全集
- 构建 routing 快照
- 生成 summary
- 应用 routing filters
- 手动分页返回

**Step 4:** controller 新增 `summary` endpoint。

### Task 4: 实现前端覆盖率视图

**Files:**
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/managerRouting/index.vue`

**Step 1:** API 新增 summary 类型和请求方法。

**Step 2:** 页面新增：
- 覆盖率摘要卡片
- 快捷筛选按钮
- 新的 routing filter 参数

**Step 3:** 点击快捷筛选时同步拉取 summary + page。

### Task 5: 更新 backlog / evidence 并做总验证

**Files:**
- Modify: `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`
- Modify: `docs/plans/2026-03-21-miniapp-booking-review-admin-evidence-ledger-v1.md`

**Step 1:** backlog 记入“manager routing coverage ops view”。

**Step 2:** evidence ledger 补“覆盖率摘要 / 缺失绑定运营视图”的证据。

**Step 3:** 运行总验证：
- `node --test tests/booking-review-admin-manager-routing.test.mjs tests/booking-review-admin-query-helpers.test.mjs tests/booking-review-admin-notify-outbox.test.mjs tests/booking-review-admin-ledger-efficiency.test.mjs`
- `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewManagerAccountRoutingControllerTest,BookingReviewManagerAccountRoutingQueryServiceImplTest,BookingReviewNotifyOutboxControllerTest,BookingReviewNotifyOutboxServiceTest test`
- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

### Task 6: 提交实现

**Step 1:** 提交 manager routing coverage ops 这一批改动。
