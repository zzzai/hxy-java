# MiniApp Booking Review High Standard Frontend Truth PRD v1（2026-03-23）

## 1. 目标与边界
- 目标：把 booking review admin overlay 的前端语义、类型定义、fallback 兜底、notify outbox 页面口径收口到当前已正式提交的高标准真值。
- 本文只吸收当前仓库已核实存在的正式字段与页面能力，不吸收口头完成态。
- 本文不新增 Java 后端能力，不补造错误码，不补造 `degraded=true / degradeReason`，不把 admin-only 观察面写成 release-ready。

## 2. 当前前端必须坚持的单一真值

### 2.1 Query 字段真值
当前前端只认以下 query 字段：
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

明确不吸收为 query capability：
- `priorityLevel`
- `priorityReason`
- `notifyRiskSummary`

### 2.2 Return 字段真值
当前 `GET /booking/review/page` 与 `GET /booking/review/get` 前端已正式吸收的稳定返回字段包括：
- `managerSlaStage`
- `managerClaimDeadlineAt`
- `managerFirstActionDeadlineAt`
- `managerCloseDeadlineAt`
- `managerClosedAt`
- `priorityLevel`
- `priorityReason`
- `notifyRiskSummary`

其中必须固定的解释：
- `priorityLevel / priorityReason / notifyRiskSummary` 只是返回展示字段。
- 前端不得把 `priorityReason / notifyRiskSummary` 当稳定 code。
- 前端不得从 `message`、自由文案或缺字段文本反推状态分支。

### 2.3 Dashboard 字段真值
当前 `GET /booking/review/dashboard-summary` 前端只认以下已正式返回计数：
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

当前不吸收为已实现能力：
- `priorityP0Count ~ priorityP3Count`
- `managerTimeoutDueSoonCount`
- `notifyAudit*Count`

### 2.4 当前未正式提交，前端不得自造
以下字段或口径在当前仓库中未核出正式提交，前端本轮不得补造：
- `managerTimeoutCategory`
- `priorityReasonCode`
- `notifyAuditStage`
- 稳定 admin 专属错误码
- `degraded=true / degradeReason`

## 3. 前端语义收口

### 3.1 CLOSED fallback
- `managerTodoStatus === 4` 时，前端 fallback 必须直接展示 `CLOSED` 语义。
- `managerClosedAt` 已存在但 `managerSlaStage` 缺失、过旧或误回 `NORMAL` 时，前端仍必须 fail-close 到 `CLOSED`。
- 历史数据 / 缺字段场景下，前端不能把闭环态误报成进行中或正常态。
- 差评且 `managerTodoStatus` 为空时，前端仍只展示 `PENDING_INIT`，不做读路径自动补齐。

### 3.2 观察态与 blocker 边界
- `CLAIM_DUE_SOON / FIRST_ACTION_DUE_SOON / CLOSE_DUE_SOON` 仍是观察态。
- `CLAIM_TIMEOUT / FIRST_ACTION_TIMEOUT / CLOSE_TIMEOUT` 是超时 blocker 语义。
- notify outbox 中 `BLOCKED_NO_OWNER / FAILED` 是阻断或失败语义。
- `SENT` 只能解释为“已派发记录存在”，不能解释为“已送达”“已处理完成”“已闭环”。

### 3.3 admin-only / 非发布能力边界
- 看板、台账、notify outbox 当前都只是后台治理与观察面。
- `managerTodoClosedCount` 只表示后台待办收口，不代表外部通知成功或门店处理完成。
- 当前最终结论继续保持：
  - `Can Develop = Yes`
  - `Can Release = No`
  - `Release Decision = No-Go`

## 4. Notify Outbox 页面真值

### 4.1 必须区分的两层语义
- 列表结果：只展示当前筛选命中的 notify outbox 记录子集。
- 审计摘要：按 review 维度聚合同一条差评在 App / 企微的当前状态。

### 4.2 页面文案必须避免的误导
前端不得把审计摘要写成：
- 已送达
- 已闭环
- 已处理完成

前端应固定为：
- 已派发
- 待派发
- 存在阻断
- 存在失败
- 人工重试后待观察
- 跨通道状态分裂

### 4.3 摘要查询口径
前端调用 summary 时只保留 review 维度相关条件：
- `reviewId`
- `storeId`
- `receiverRole`
- `notifyType`

前端不再把以下列表子集条件传给审计摘要：
- `receiverUserId`
- `receiverAccount`
- `status`
- `channel`
- `lastActionCode`

## 5. 测试与验收
本轮前端测试必须覆盖：
- `CLOSED` fallback 与历史缺字段闭环态兜底
- query / return / dashboard 字段边界
- notify outbox 列表子集与 review 维度审计摘要口径拆分
- 不把旧文案回归成“已送达 / 已闭环 / 已处理完成”

本轮对应自动化：
- `tests/booking-review-admin-ledger-efficiency.test.mjs`
- `tests/booking-review-admin-notify-outbox.test.mjs`

## 6. 结论
- 本轮属于前端真值收口，不是新功能扩写。
- 本轮目的是防止前端继续沿用旧口径误报 `CLOSED`、误把子集当全貌、误把展示字段当 query capability。
- 本轮交付后，booking review admin overlay 的前端表达应与当前正式提交的 P0 真值一致，但 release 结论不变，继续 `No-Go`。
