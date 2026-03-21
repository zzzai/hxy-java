# Booking Review Manager Notify Routing Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 booking review 差评通知补齐账号路由真值、notify outbox 和后台可观测面，但保持当前能力只处于 `Can Develop / Cannot Release`。

**Architecture:** 采用“差评提交成功 -> 写 notify outbox -> 异步派发”的链路，避免把评价提交主链路和消息发送强耦合。所有通知目标只认稳定 `storeId -> managerAdminUserId` 映射；无 owner 时进入 `BLOCKED_NO_OWNER`，而不是伪发送。

**Tech Stack:** Spring Boot、MyBatis-Plus、JUnit5、Vue 3 overlay、Node test、Markdown docs。

---

### Task 1: 冻结账号路由真值与专题设计文档

**Files:**
- Create: `docs/products/miniapp/2026-03-21-miniapp-booking-review-manager-account-routing-truth-v1.md`
- Create: `docs/plans/2026-03-21-booking-review-manager-notify-routing-design.md`
- Modify: `docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`

**Step 1: 写真值审计文档**
- 明确当前只核到 `contactName/contactMobile`。
- 明确当前未核出稳定 `storeId -> managerAdminUserId`。
- 明确 `managerClaimedByUserId` 不是通知接收人。

**Step 2: 写设计文档**
- 固定 outbox 方案、状态机、后台可观测面、No-Go。

**Step 3: 更新 backlog**
- 将 N1 标记为“已完成设计，尚未进入实现”。

**Step 4: 运行格式检查**
Run: `git diff --check -- docs/products/miniapp/2026-03-21-miniapp-booking-review-manager-account-routing-truth-v1.md docs/plans/2026-03-21-booking-review-manager-notify-routing-design.md docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md`
Expected: PASS

**Step 5: Commit**
```bash
git add docs/products/miniapp/2026-03-21-miniapp-booking-review-manager-account-routing-truth-v1.md docs/plans/2026-03-21-booking-review-manager-notify-routing-design.md docs/plans/2026-03-19-booking-review-admin-ops-enhancement-backlog-v1.md
git commit -m "docs(booking-review): freeze manager notify routing design"
```

### Task 2: 先写账号路由红灯测试

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewServiceImplTest.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceTest.java`

**Step 1: Write the failing test**
新增测试覆盖：
- 差评提交成功且存在有效 `managerAdminUserId` 时，创建 `PENDING` outbox。
- 差评提交成功但无 owner 时，创建 `BLOCKED_NO_OWNER` outbox。
- 好评 / 中评不创建 outbox。
- 幂等键重复时不重复写。

**Step 2: Run test to verify it fails**
Run: `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewServiceImplTest,BookingReviewNotifyOutboxServiceTest test`
Expected: FAIL，因为通知路由与 outbox 尚不存在。

**Step 3: Commit red test**
```bash
git add ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewServiceImplTest.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceTest.java
git commit -m "test(booking-review): add notify routing red tests"
```

### Task 3: 实现 manager account routing truth 模型

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/BookingReviewManagerAccountRoutingDO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/BookingReviewManagerAccountRoutingMapper.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/resources/sql/create_tables.sql`
- Modify: `ruoyi-vue-pro-master/sql/mysql/hxy/<new-sql-file>.sql`

**Step 1: 写最小数据模型**
字段至少包括：
- `storeId`
- `managerAdminUserId`
- `bindingStatus`
- `effectiveTime`
- `expireTime`
- `source`
- `lastVerifiedTime`

**Step 2: 运行测试，确认编译通过**
Run: `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewNotifyOutboxServiceTest test`
Expected: 仍 FAIL，但 routing 相关类型已可用。

**Step 3: Commit**
```bash
git add ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/BookingReviewManagerAccountRoutingDO.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/BookingReviewManagerAccountRoutingMapper.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/resources/sql/create_tables.sql ruoyi-vue-pro-master/sql/mysql/hxy/<new-sql-file>.sql
git commit -m "feat(booking-review): add manager account routing model"
```

### Task 4: 实现 booking review notify outbox

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/BookingReviewNotifyOutboxDO.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/BookingReviewNotifyOutboxMapper.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingReviewNotifyOutboxService.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/resources/sql/create_tables.sql`
- Modify: `ruoyi-vue-pro-master/sql/mysql/hxy/<new-sql-file>.sql`

**Step 1: 最小实现字段与状态**
字段至少包括：
- `bizType`
- `bizId`
- `storeId`
- `receiverRole`
- `receiverUserId`
- `notifyType`
- `channel`
- `status`
- `retryCount`
- `nextRetryTime`
- `sentTime`
- `lastErrorMsg`
- `idempotencyKey`
- `payloadSnapshot`
- `lastActionCode / BizNo / Time`

**Step 2: 实现最小 service**
- 根据 `storeId -> managerAdminUserId` 决定写 `PENDING` 还是 `BLOCKED_NO_OWNER`。
- 保证幂等写入。

**Step 3: 运行测试，确认 outbox 逻辑通过**
Run: `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewNotifyOutboxServiceTest test`
Expected: PASS

**Step 4: Commit**
```bash
git add <all outbox files>
git commit -m "feat(booking-review): add notify outbox"
```

### Task 5: 把 outbox 接入 createReview 差评触发点

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingReviewService.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewServiceImplTest.java`

**Step 1: 实现最小接入**
- `createReview()` 成功提交后，如果 `reviewLevel = NEGATIVE`，调用 notify outbox service。
- 保证通知失败不回滚评价提交；只允许写出意图或阻断状态。

**Step 2: 运行测试确认通过**
Run: `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewServiceImplTest,BookingReviewNotifyOutboxServiceTest test`
Expected: PASS

**Step 3: Commit**
```bash
git add ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingReviewService.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewServiceImpl.java ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewServiceImplTest.java
git commit -m "feat(booking-review): trigger notify outbox on negative review"
```

### Task 6: 实现后台可观测面（详情 + outbox 台账）

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewController.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewNotifyOutboxController.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewNotifyOutbox*.java`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`
- Create: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/notifyOutbox/index.vue`
- Create: `tests/booking-review-admin-notify-outbox.test.mjs`

**Step 1: 先写前后端红灯测试**
- 详情页看到通知状态和阻断原因。
- outbox 台账存在筛选与查看能力。

**Step 2: 运行测试确认失败**
Run:
- `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest,BookingReviewNotifyOutboxControllerTest test`
- `node --test tests/booking-review-admin-notify-outbox.test.mjs`
Expected: FAIL

**Step 3: 最小实现**
- 详情页只读展示通知块。
- outbox 台账只提供查询、查看、重试失败项。
- 明确 `BLOCKED_NO_OWNER` 文案。

**Step 4: 运行测试确认通过**
Expected: PASS

**Step 5: Commit**
```bash
git add <controller/api/ui/test files>
git commit -m "feat(booking-review): add notify outbox observability"
```

### Task 7: 实现异步派发器（IN_APP 占位）

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/job/BookingReviewNotifyDispatchJob.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceImpl.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/job/BookingReviewNotifyDispatchJobTest.java`

**Step 1: 写失败测试**
- `PENDING -> SENT`
- 发送异常 -> `FAILED`
- 幂等保护

**Step 2: 运行测试确认失败**
Run: `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewNotifyDispatchJobTest,BookingReviewNotifyOutboxServiceTest test`
Expected: FAIL

**Step 3: 最小实现**
- 先做 `IN_APP` 占位派发。
- 失败时写 `lastErrorMsg`、递增 `retryCount`、设置 `nextRetryTime`。

**Step 4: 运行测试确认通过**
Expected: PASS

**Step 5: Commit**
```bash
git add <dispatch job files>
git commit -m "feat(booking-review): add notify dispatch job"
```

### Task 8: 文档收口与最终验证

**Files:**
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`
- Modify: `docs/products/miniapp/2026-03-21-miniapp-booking-review-manager-account-routing-truth-v1.md`
- Modify: `docs/plans/2026-03-21-booking-review-manager-notify-routing-design.md`

**Step 1: 更新文档口径**
- 若只完成意图 + 阻断池，则只能写 `Can Develop / Cannot Release`。
- 若未完成真实 `managerAdminUserId`，继续写 `BLOCKED_NO_OWNER` truth。

**Step 2: 最终验证**
Run:
- `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest,BookingReviewServiceImplTest,BookingReviewNotifyOutboxServiceTest,BookingReviewNotifyDispatchJobTest test`
- `node --test tests/booking-review-admin-history-scan.test.mjs tests/booking-review-admin-query-helpers.test.mjs tests/booking-review-admin-detail-timeline.test.mjs tests/booking-review-admin-notify-outbox.test.mjs`
- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
Expected: PASS

**Step 3: Final commit**
```bash
git add <all notify routing files>
git commit -m "feat(booking-review): add manager notify routing"
```
