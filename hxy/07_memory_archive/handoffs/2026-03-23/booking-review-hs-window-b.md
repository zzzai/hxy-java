# Booking Review High Standard Window B Handoff（2026-03-23）

## 1. 本轮目标
- 以 B 窗口身份收口 booking review 前端真值，修复 `CLOSED` fallback、query/return/dashboard 字段边界，以及 notify outbox 页面语义误导。

## 2. 本轮已落地
1. 列表页把 `managerTodoStatus === 4` 与 `managerClosedAt` 统一 fail-close 到 `CLOSED`，不再回落成 `NORMAL`。
2. `queryHelpers.mjs` 导出当前前端单一真值：
   - `BOOKING_REVIEW_LEDGER_QUERY_FIELDS`
   - `BOOKING_REVIEW_DISPLAY_ONLY_RETURN_FIELDS`
   - `BOOKING_REVIEW_DASHBOARD_SUMMARY_FIELDS`
3. 台账和看板明确：
   - `priorityLevel / priorityReason / notifyRiskSummary` 只是返回展示字段
   - `managerTodoClosedCount` 只是后台待办 `CLOSED` 计数
4. notify outbox 页面明确区分：
   - 列表子集
   - review 维度跨通道审计摘要
5. notify outbox 摘要请求不再携带会把子集误读成全貌的筛选字段：
   - `receiverUserId`
   - `receiverAccount`
   - `status`
   - `channel`
   - `lastActionCode`

## 3. 本轮涉及文件
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/dashboard/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/notifyOutbox/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/queryHelpers.mjs`
- `tests/booking-review-admin-ledger-efficiency.test.mjs`
- `tests/booking-review-admin-notify-outbox.test.mjs`
- `docs/products/miniapp/2026-03-23-miniapp-booking-review-high-standard-frontend-truth-prd-v1.md`
- `hxy/07_memory_archive/handoffs/2026-03-23/booking-review-hs-window-b.md`

## 4. 当前真值

### 4.1 Query 字段真值
- 只认：
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
- 不认：
  - `priorityLevel`
  - `priorityReason`
  - `notifyRiskSummary`

### 4.2 Return 字段真值
- 当前前端正式吸收：
  - `managerSlaStage`
  - `managerClaimDeadlineAt`
  - `managerFirstActionDeadlineAt`
  - `managerCloseDeadlineAt`
  - `managerClosedAt`
  - `priorityLevel`
  - `priorityReason`
  - `notifyRiskSummary`
- 当前不补造：
  - `managerTimeoutCategory`
  - `priorityReasonCode`
  - `notifyAuditStage`
  - admin 专属稳定错误码
  - `degraded=true / degradeReason`

### 4.3 前端 fallback 行为
- `managerTodoStatus === 4` 或 `managerClosedAt` 已存在时，统一展示 `CLOSED`。
- `managerSlaStage` 缺失时，只按 deadline/claim/action 的已知规则回退。
- 差评且 `managerTodoStatus` 缺失时，只展示 `PENDING_INIT`。
- 不从 `message`、自由文案、`priorityReason`、`notifyRiskSummary` 反推状态分支。

### 4.4 fail-close / 观察态 / 非发布能力边界
- fail-close：
  - 明确闭环信号优先于 `NORMAL`
- 观察态：
  - `CLAIM_DUE_SOON / FIRST_ACTION_DUE_SOON / CLOSE_DUE_SOON`
  - `MANUAL_RETRY_PENDING`
  - `DIVERGED`
- blocker：
  - `CLAIM_TIMEOUT / FIRST_ACTION_TIMEOUT / CLOSE_TIMEOUT`
  - `BLOCKED_NO_OWNER`
  - `FAILED`
- 非发布能力：
  - `SENT` 只表示已派发记录存在
  - `managerTodoClosedCount` 只表示后台待办 `CLOSED`
  - 页面仍是 admin-only，`Can Release = No`

## 5. 联调注意点
1. 若 C 窗口后续正式提交 `managerTimeoutCategory / priorityReasonCode / notifyAuditStage`，前端类型与测试需同步更新；在正式提交前不得补造。
2. notify outbox 审计摘要当前是 review 维度观察真值，不等于全链路送达率，不等于门店已处理完成。
3. 不按 `message` 文案分支；只按已正式提交的字段与状态机派生结果分支。
4. 当前未核出稳定 admin-only 错误码；继续保持“未核出”，不要前端补码。
5. 当前未核出真实 `degraded=true / degradeReason`；不要在前端制造假降级分支。

## 6. 验证
- 本轮代码验证以指定命令为准：
  - `node --test tests/booking-review-admin-ledger-efficiency.test.mjs tests/booking-review-admin-notify-outbox.test.mjs`
  - `git diff --check`
  - `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
  - `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

## 7. 结论
- 本轮属于前端真值校正与语义降噪。
- 当前 booking review admin overlay 继续保持：
  - `已完成开发`
  - `已完成联调`
  - `admin-only 已可用`
  - `Can Release = No`
  - `Release Decision = No-Go`
