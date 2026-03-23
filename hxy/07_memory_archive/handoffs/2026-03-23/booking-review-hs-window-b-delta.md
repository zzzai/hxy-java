# Booking Review High Standard Window B Delta Handoff（2026-03-23）

## 1. 本轮目标
- 吸收窗口 C 已正式稳定的 contract delta，仅同步前端 TS contract、dashboard 只读观察聚合、notify outbox summary `baseScope` 展示。
- 本轮不扩展新功能，不改 Java 后端，不改 `review/index.vue`，不改 gate / final integration / `.codex`。

## 2. 本轮改动
1. `review.ts` 正式吸收 `BookingReview` 新稳定返回字段：
   - `managerTimeoutCategory`
   - `priorityReasonCode`
   - `notifyAuditStage`
2. `review.ts` 正式吸收 `BookingReviewDashboardSummary` 新稳定聚合字段：
   - `priorityP0Count`
   - `priorityP1Count`
   - `priorityP2Count`
   - `priorityP3Count`
   - `managerTimeoutDueSoonCount`
   - `managerTimeoutCount`
   - `notifyAuditBlockedCount`
   - `notifyAuditFailedCount`
   - `notifyAuditManualRetryPendingCount`
   - `notifyAuditDivergedCount`
3. `review.ts` 正式吸收 `BookingReviewNotifyOutboxSummary.baseScope`。
4. dashboard 页面新增“高标准只读观察聚合”，只读消费上述关键新聚合字段。
5. notify outbox 页面新增 “跨通道摘要 base scope” 说明，只作为摘要聚合基线文案，不参与状态分支。

## 3. 当前真值

### 3.1 query 字段真值
- 仍只认：
  - `id`
  - `bookingOrderId`
  - `storeId`
  - `technicianId`
  - `memberId`
  - `reviewLevel`
  - `riskLevel`
  - `followStatus`
  - `onlyManagerTodo`
  - `onlyPendingInit`
  - `managerTodoStatus`
  - `managerSlaStatus`
  - `replyStatus`
  - `submitTime[]`
- 仍不认：
  - `priorityLevel`
  - `priorityReason`
  - `notifyRiskSummary`
  - `managerTimeoutCategory`
  - `priorityReasonCode`
  - `notifyAuditStage`

### 3.2 return 字段真值
- 当前前端正式吸收：
  - `managerSlaStage`
  - `managerTimeoutCategory`
  - `priorityLevel`
  - `priorityReason`
  - `priorityReasonCode`
  - `notifyRiskSummary`
  - `notifyAuditStage`
  - 既有 deadline / claimed / firstAction / closedAt 字段
- 其中：
  - `priorityReason` / `notifyRiskSummary` 继续只作展示文案
  - `managerTimeoutCategory` / `priorityReasonCode` / `notifyAuditStage` 作为稳定 status/code 吸收，但本轮不新增前端分支逻辑

### 3.3 dashboard 聚合字段真值
- 既有聚合继续保留：
  - `totalCount`
  - `positiveCount`
  - `neutralCount`
  - `negativeCount`
  - `pendingFollowCount`
  - `urgentCount`
  - `repliedCount`
  - `managerTodoPendingCount`
  - `managerTodoClaimTimeoutCount`
  - `managerTodoClaimDueSoonCount`
  - `managerTodoFirstActionTimeoutCount`
  - `managerTodoFirstActionDueSoonCount`
  - `managerTodoCloseTimeoutCount`
  - `managerTodoCloseDueSoonCount`
  - `managerTodoClosedCount`
- 本轮新增正式吸收：
  - `priorityP0Count`
  - `priorityP1Count`
  - `priorityP2Count`
  - `priorityP3Count`
  - `managerTimeoutDueSoonCount`
  - `managerTimeoutCount`
  - `notifyAuditBlockedCount`
  - `notifyAuditFailedCount`
  - `notifyAuditManualRetryPendingCount`
  - `notifyAuditDivergedCount`
- 当前 dashboard 对上述新增字段仅作只读观察聚合展示，不代表升级成功率、送达率或 release capability。

### 3.4 summary baseScope 真值
- `BookingReviewNotifyOutboxSummary.baseScope` 当前只用于说明“跨通道摘要按什么聚合基线生成”。
- 页面展示为“跨通道摘要 base scope”。
- 当前不据此切换动作入口，不据此改变 query，不据此推导成功/失败/闭环结论。

## 4. 联调边界
1. 不按 `message` 分支；仍只按正式字段分支。
2. 不补造错误码；当前仍未核出稳定 admin-only 错误码。
3. 不补造 `degraded=true / degradeReason`。
4. `fail-close` 规则不变：
   - `managerTodoStatus === 4` 或 `managerClosedAt` 存在时优先 `CLOSED`
5. 观察态 / 非发布能力边界不变：
   - due soon、manual retry pending、diverged 仍是观察态
   - `SENT` 只表示已派发记录存在
   - dashboard 新聚合只表示后台观察口径，不代表 release-ready

## 5. 本轮涉及文件
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/dashboard/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/notifyOutbox/index.vue`
- `tests/booking-review-admin-ledger-efficiency.test.mjs`
- `tests/booking-review-admin-notify-outbox.test.mjs`
- `hxy/07_memory_archive/handoffs/2026-03-23/booking-review-hs-window-b-delta.md`
