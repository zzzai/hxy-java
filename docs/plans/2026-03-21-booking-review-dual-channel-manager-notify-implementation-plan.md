# Booking Review Dual Channel Manager Notify Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 booking review 差评治理补齐“店长 App + 店长企微”双通道通知、双通道路由真值、后台观测、SLA 自动提醒与证据包，并保持当前结论为 `Can Develop / Cannot Release`。

**Architecture:** 保持“差评提交成功 -> 写 notify outbox -> 异步派发”的解耦链路，但把单通道 `IN_APP` 扩成双通道独立出站。路由真值固定为 `storeId -> managerAdminUserId + managerWecomUserId`，同一条差评按店长命中后展开两条独立 outbox，每条记录独立状态、失败原因、重试和审计；SLA 提醒继续复用同一套 outbox/dispatch 机制。

**Tech Stack:** Spring Boot、MyBatis-Plus、JUnit5、Vue 3 overlay、Node test、Quartz Job、Markdown docs。

---

### Task 1: 冻结双通道设计和实施计划

**Files:**
- Create: `docs/plans/2026-03-21-booking-review-dual-channel-manager-notify-implementation-plan.md`
- Create: `docs/plans/2026-03-21-booking-review-dual-channel-manager-notify-design.md`
- Modify: `docs/plans/2026-03-21-booking-review-manager-notify-routing-design.md`

**Step 1: 写设计文档**
- 固定业务场景：`1000` 门店、默认 `1 店 1 店长`、共享企微发送端、差评生成 `App / 企微` 两条出站记录。
- 固定工程边界：真实发送失败不伪成功；路由缺失/账号缺失进入阻断；仍然 `Cannot Release`。

**Step 2: 写实施计划**
- 明确 schema、service、job、UI、测试、文档的落点。

**Step 3: 运行格式检查**
Run: `git diff --check -- docs/plans/2026-03-21-booking-review-dual-channel-manager-notify-implementation-plan.md docs/plans/2026-03-21-booking-review-dual-channel-manager-notify-design.md docs/plans/2026-03-21-booking-review-manager-notify-routing-design.md`
Expected: PASS

### Task 2: 先写双通道红灯测试

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceTest.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewManagerAccountRoutingQueryServiceImplTest.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/controller/admin/BookingReviewNotifyOutboxControllerTest.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/job/BookingReviewNotifyDispatchJobTest.java`
- Modify: `tests/booking-review-admin-notify-outbox.test.mjs`
- Modify: `tests/booking-review-admin-manager-routing.test.mjs`
- Create: `tests/booking-review-admin-sla-reminder.test.mjs`

**Step 1: 写失败测试**
- 差评创建后生成两条 outbox：`IN_APP` / `WECOM`。
- route 缺 App 账号时只阻断 App，缺企微账号时只阻断企微。
- controller/UI 能展示通道级阻断和通道级接收账号。
- job 能处理双通道批量派发和 SLA 提醒。

**Step 2: 跑红灯**
Run:
- `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewNotifyOutboxServiceTest,BookingReviewManagerAccountRoutingQueryServiceImplTest,BookingReviewNotifyOutboxControllerTest,BookingReviewNotifyDispatchJobTest test`
- `node --test tests/booking-review-admin-notify-outbox.test.mjs tests/booking-review-admin-manager-routing.test.mjs tests/booking-review-admin-sla-reminder.test.mjs`
Expected: FAIL，失败点明确指向双通道字段/行为尚未实现。

### Task 3: 扩展路由真值模型为双通道

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/BookingReviewManagerAccountRoutingDO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/BookingReviewManagerAccountRoutingMapper.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewManagerAccountRoutingRespVO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewManagerAccountRoutingQueryServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/resources/sql/create_tables.sql`
- Modify: `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-21-hxy-booking-review-manager-notify-routing.sql`

**Step 1: 增加路由字段**
- 新增 `managerWecomUserId`。
- 新增通道级可派发结论：App / 企微各自是否 ready。

**Step 2: 跑 routing 测试**
Run: `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewManagerAccountRoutingQueryServiceImplTest test`
Expected: 仍可能部分 FAIL，但新的字段和 VO 已可编译。

### Task 4: 扩展 outbox 模型为双通道独立记录

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/BookingReviewNotifyOutboxDO.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/mysql/BookingReviewNotifyOutboxMapper.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingReviewNotifyOutboxService.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/resources/sql/create_tables.sql`
- Modify: `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-21-hxy-booking-review-manager-notify-routing.sql`

**Step 1: 最小 schema 调整**
- 新增通道接收目标字段：例如 `receiverAccount`。
- 幂等键带 `channel`，确保同一条差评会有两条独立记录。

**Step 2: 实现双通道创建逻辑**
- `IN_APP` 记录取 `managerAdminUserId`。
- `WECOM` 记录取 `managerWecomUserId`。
- 缺任一通道账号时，仅阻断对应通道记录。

**Step 3: 跑 service 红转绿**
Run: `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewNotifyOutboxServiceTest test`
Expected: PASS

### Task 5: 接入真实发送器并保持通道隔离

**Files:**
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/BookingReviewWecomRobotSender.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewWecomRobotSenderImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceImpl.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceTest.java`

**Step 1: App 发送继续复用 `NotifySendService`**
- `IN_APP` 保持 `sendSingleNotifyToAdmin(...)`。

**Step 2: 新增企微发送器**
- 通过共享发送端配置读取：启用开关、webhook/token、应用名。
- 若通道未启用，返回通道阻断，不伪成功。
- 若 HTTP 发送失败，落 `FAILED`，并记录失败原因。

**Step 3: 跑派发测试**
Run: `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewNotifyOutboxServiceTest,BookingReviewNotifyDispatchJobTest test`
Expected: PASS

### Task 6: 扩展后台双通道观测面

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewNotifyOutboxController.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/vo/BookingReviewNotifyOutboxRespVO.java`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/notifyOutbox/index.vue`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/managerRouting/index.vue`

**Step 1: API 返回通道级字段**
- 返回 `receiverAccount`、通道标签、通道诊断。

**Step 2: 页面展示双通道**
- outbox 台账支持按 `IN_APP / WECOM` 过滤。
- 详情页能同时看到两条通道记录。
- routing 页同时显示 App / 企微绑定状态与修复建议。

**Step 3: 跑 Node 测试**
Run: `node --test tests/booking-review-admin-notify-outbox.test.mjs tests/booking-review-admin-manager-routing.test.mjs`
Expected: PASS

### Task 7: 增加 SLA 自动提醒与升级出站

**Files:**
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingReviewNotifyOutboxServiceImpl.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/job/BookingReviewManagerTodoSlaReminderJob.java`
- Modify: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/job/BookingReviewNotifyDispatchJobTest.java`
- Create: `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/job/BookingReviewManagerTodoSlaReminderJobTest.java`
- Modify: `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/index.vue`

**Step 1: 生成 SLA reminder outbox**
- 识别 `CLAIM_TIMEOUT / FIRST_ACTION_TIMEOUT / CLOSE_TIMEOUT`。
- 为每个超时评价生成对应 notifyType 的双通道提醒记录。
- 保持幂等，避免重复刷屏。

**Step 2: 看板增加提醒可见性**
- 列表页增加“已触发 SLA 提醒”视角或标记。

**Step 3: 跑测试**
Run:
- `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewManagerTodoSlaReminderJobTest,BookingReviewNotifyDispatchJobTest test`
- `node --test tests/booking-review-admin-ledger-efficiency.test.mjs tests/booking-review-admin-sla-reminder.test.mjs`
Expected: PASS

### Task 8: 更新证据、runbook、final review

**Files:**
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`
- Modify: `docs/plans/2026-03-17-miniapp-booking-review-release-gate-v1.md`
- Modify: `docs/plans/2026-03-17-miniapp-booking-review-service-recovery-runbook-v1.md`
- Modify: `docs/plans/2026-03-21-miniapp-booking-review-admin-evidence-ledger-v1.md`
- Modify: `docs/products/miniapp/2026-03-21-miniapp-booking-review-manager-account-routing-truth-v1.md`
- Modify: `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`

**Step 1: 更新真值**
- 记录双通道已具备工程能力。
- 明确共享企微发送端已接入工程实现，但仍缺 release proof 时不能改成 Ready。

**Step 2: 固定 No-Go**
- 继续写成 `Can Develop / Cannot Release`。
- 不外推到奖励、补偿、多级区域负责人升级。

### Task 9: 全量验证并提交

**Files:**
- Modify: `hxy/07_memory_archive/handoffs/2026-03-21/booking-review-dual-channel-manager-notify.md`

**Step 1: 跑后端验证**
Run: `cd ruoyi-vue-pro-master && mvn -pl yudao-module-mall/yudao-module-booking -Dtest=BookingReviewControllerTest,BookingReviewNotifyOutboxControllerTest,BookingReviewManagerAccountRoutingControllerTest,BookingReviewServiceImplTest,BookingReviewNotifyOutboxServiceTest,BookingReviewNotifyDispatchJobTest,BookingReviewManagerAccountRoutingQueryServiceImplTest,BookingReviewManagerTodoSlaReminderJobTest test`
Expected: PASS

**Step 2: 跑前端/静态验证**
Run: `node --test tests/booking-review-admin-history-scan.test.mjs tests/booking-review-admin-query-helpers.test.mjs tests/booking-review-admin-detail-timeline.test.mjs tests/booking-review-admin-notify-outbox.test.mjs tests/booking-review-admin-manager-routing.test.mjs tests/booking-review-admin-ledger-efficiency.test.mjs tests/booking-review-admin-sla-reminder.test.mjs`
Expected: PASS

**Step 3: 跑守卫与 diff 检查**
Run:
- `git diff --check`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
Expected: PASS

**Step 4: 提交**
```bash
git add docs/plans/2026-03-21-booking-review-dual-channel-manager-notify-implementation-plan.md docs/plans/2026-03-21-booking-review-dual-channel-manager-notify-design.md ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review tests docs/products/miniapp docs/plans hxy/07_memory_archive/handoffs/2026-03-21/booking-review-dual-channel-manager-notify.md
 git commit -m "feat(booking-review): add dual-channel manager notify"
```
