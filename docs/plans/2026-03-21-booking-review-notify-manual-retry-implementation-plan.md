# Booking Review Notify Manual Retry Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 booking review notify outbox 补齐 `FAILED` 记录的后台手工重试闭环，保持当前能力只处于 `Can Develop / Cannot Release`。

**Architecture:** 复用既有 notify outbox + dispatch job 模型，只新增“人工重新入队”入口，不改派发通道、不改接收人模型。后端接口按 `ids[]` 设计，当前前端只做单条重试，保证范围最小且后续可扩。

**Tech Stack:** Spring Boot、MyBatis-Plus、JUnit5、Vue 3 overlay、Element Plus、Node test、Markdown docs。

---

### Task 1: 冻结设计与计划文档

**Files:**
- Create: `docs/plans/2026-03-21-booking-review-notify-manual-retry-design.md`
- Create: `docs/plans/2026-03-21-booking-review-notify-manual-retry-implementation-plan.md`

**Step 1:** 写设计文档，固定范围、状态约束、UI 交互、No-Go。

**Step 2:** 写实现计划文档，拆成 TDD 步骤。

**Step 3:** 运行格式检查。

Run: `git diff --check -- docs/plans/2026-03-21-booking-review-notify-manual-retry-design.md docs/plans/2026-03-21-booking-review-notify-manual-retry-implementation-plan.md`

Expected: PASS

**Step 4:** Commit

```bash
git add docs/plans/2026-03-21-booking-review-notify-manual-retry-design.md docs/plans/2026-03-21-booking-review-notify-manual-retry-implementation-plan.md
git commit -m "docs(booking-review): freeze notify manual retry design"
```

### Task 2: 先写后端红灯测试

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewNotifyOutboxControllerTest.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceTest.java`

**Step 1: Write the failing test**
- controller test 新增 retry 接口用例
- service test 新增：
  - `FAILED -> PENDING`
  - `SENT` 拒绝
  - `BLOCKED_NO_OWNER` 拒绝
  - 不存在记录报错

**Step 2: Run test to verify it fails**

Run: `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewNotifyOutboxControllerTest,BookingReviewNotifyOutboxServiceTest test`

Expected: FAIL，因为 retry API / service 尚未实现。

**Step 3:** Commit

```bash
git add ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewNotifyOutboxControllerTest.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceTest.java
git commit -m "test(booking-review): add notify manual retry red tests"
```

### Task 3: 实现后端 retry 接口与服务

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewNotifyOutboxRetryReqVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewNotifyOutboxController.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingReviewNotifyOutboxService.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/ErrorCodeConstants.java`

**Step 1: Write minimal implementation**
- 增加 retry request VO
- controller 新增 `POST /retry`
- service 新增 `retryNotifyOutbox(List<Long> ids, Long operatorId, String reason)`
- 只允许 `FAILED`
- 回写 `PENDING / nextRetryTime / sentTime / lastErrorMsg / lastAction*`

**Step 2: Run targeted tests**

Run: `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewNotifyOutboxControllerTest,BookingReviewNotifyOutboxServiceTest test`

Expected: PASS

**Step 3:** Commit

```bash
git add ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewNotifyOutboxRetryReqVO.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewNotifyOutboxController.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingReviewNotifyOutboxService.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceImpl.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/ErrorCodeConstants.java
git commit -m "feat(booking-review): add notify outbox manual retry api"
```

### Task 4: 先写前端红灯测试

**Files:**
- Modify: `tests/booking-review-admin-notify-outbox.test.mjs`

**Step 1: Write the failing test**
- API 暴露 `retryReviewNotifyOutbox`
- 页面包含 `重试`
- 页面只在 `FAILED` 状态判断里显示按钮
- 页面包含 `已重新入队`

**Step 2: Run test to verify it fails**

Run: `node --test tests/booking-review-admin-notify-outbox.test.mjs`

Expected: FAIL，因为页面和 API 尚未补齐。

**Step 3:** Commit

```bash
git add tests/booking-review-admin-notify-outbox.test.mjs
git commit -m "test(booking-review): add notify outbox retry ui red tests"
```

### Task 5: 实现前端 API 与页面重试交互

**Files:**
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/notifyOutbox/index.vue`

**Step 1: Write minimal implementation**
- API 增加 retry request type 与 `retryReviewNotifyOutbox`
- 页面增加行级 `重试` 按钮
- 弹出原因输入框，默认 `manual-retry`
- 成功提示 `已重新入队`
- 成功后刷新列表
- `BLOCKED_NO_OWNER` 不显示按钮

**Step 2: Run targeted tests**

Run: `node --test tests/booking-review-admin-notify-outbox.test.mjs`

Expected: PASS

**Step 3:** Commit

```bash
git add ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/notifyOutbox/index.vue
git commit -m "feat(booking-review): add notify outbox retry action"
```

### Task 6: 最终回归验证

**Files:**
- Verify only

**Step 1:** 跑后端回归

Run: `mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest,BookingReviewServiceImplTest,BookingReviewNotifyOutboxServiceTest,BookingReviewNotifyDispatchJobTest,BookingReviewNotifyOutboxControllerTest test`

Expected: PASS

**Step 2:** 跑前端回归

Run: `node --test tests/booking-review-admin-history-scan.test.mjs tests/booking-review-admin-query-helpers.test.mjs tests/booking-review-admin-detail-timeline.test.mjs tests/booking-review-admin-notify-outbox.test.mjs`

Expected: PASS

**Step 3:** 跑基础校验

Run:
- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

Expected: PASS

**Step 4:** Final commit

```bash
git add docs/plans/2026-03-21-booking-review-notify-manual-retry-design.md docs/plans/2026-03-21-booking-review-notify-manual-retry-implementation-plan.md ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewNotifyOutboxRetryReqVO.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewNotifyOutboxController.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingReviewNotifyOutboxService.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceImpl.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/ErrorCodeConstants.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewNotifyOutboxControllerTest.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceTest.java ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/notifyOutbox/index.vue tests/booking-review-admin-notify-outbox.test.mjs
git commit -m "feat(booking-review): add notify manual retry"
```
