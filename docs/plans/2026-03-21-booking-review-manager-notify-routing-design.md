# Booking Review Manager Notify Routing Design（2026-03-21）

## 1. 背景
- booking review 当前已经具备：
  - 差评提交
  - admin-only 店长待办治理
  - 台账 / 详情 / 看板 / 历史治理扫描页
- 当前单一真值已冻结：
  - “店长”只冻结到门店联系人快照 `contactName/contactMobile`
  - 当前没有稳定 `storeId -> managerAdminUserId`
  - 当前没有 booking review 专属自动通知链路
- 用户已确认本轮设计边界：
  - 只做账号级通知前置设计
  - 第一版通知对象只到“门店店长账号”
  - 差评提交成功后立即触发
  - 不接受 `contactMobile` 直接作为自动通知目标

## 2. 目标
- 设计一套可演进到“门店店长账号通知”的最小闭环方案。
- 保证差评提交主链路与通知链路分离。
- 在账号归属真值未闭环前，也能诚实表达“立即触发通知意图”，而不是伪装成“已发送成功”。

## 3. 能力边界

### 3.1 本轮要设计的内容
1. 差评提交成功后的通知触发点。
2. `booking review notify outbox` 数据模型。
3. `storeId -> managerAdminUserId` 账号归属真值要求。
4. 通知状态机、阻断池和后台可观测面。
5. 第一版实施顺序与 No-Go。

### 3.2 本轮明确不做的内容
1. 真实自动通知派发上线。
2. 基于 `contactMobile` 的自动短信兜底。
3. 客服负责人 / 区域负责人升级链路。
4. 奖励、补偿、审批、feature flag、release-ready 结论。

## 4. 方案比较

### 方案 A：评价提交后同步直发通知
- 做法：在 `createReview()` 成功后直接发送通知。
- 优点：链路短。
- 缺点：
  - 强耦合评价提交与消息发送。
  - 发送失败会污染主链路语义。
  - 与仓内现有 outbox 模式不一致。
- 结论：否决。

### 方案 B：评价提交后写 Notify Outbox，再异步派发
- 做法：
  - `createReview()` 成功且为差评后，立即写通知意图。
  - 异步派发器根据账号归属结果，把记录推进到 `PENDING / SENT / FAILED / BLOCKED_NO_OWNER`。
- 优点：
  - 与既有 `TechnicianCommissionSettlementNotifyOutbox` 模式一致。
  - 可幂等、可重试、可审计。
  - 不阻塞评价提交主链路。
  - 方便以后扩展客服负责人或区域负责人。
- 缺点：
  - 需要新增 outbox 和后台可观测面。
- 结论：采用。

### 方案 C：定时扫描差评后延迟触发通知
- 做法：靠定时任务扫描差评再决定是否通知。
- 优点：最保守。
- 缺点：不符合“差评提交成功后立即触发”的业务要求。
- 结论：否决。

## 5. 核心设计决策

### 5.1 触发点
- 唯一稳定触发点固定为：
  - `BookingReviewServiceImpl.createReview(...)` 成功提交评价记录后
  - 且 `reviewLevel = NEGATIVE`
- “立即触发”的工程语义固定为：
  - 立即写出通知意图，不等于立即发送成功。

### 5.2 账号归属模型
- 第一版通知对象只允许：`门店店长后台账号`。
- 归属关系必须冻结为类似：
  - `storeId -> managerAdminUserId`
- 该关系至少应表达：
  - `storeId`
  - `managerAdminUserId`
  - `bindingStatus`
  - `effectiveTime`
  - `expireTime`
  - `source`
  - `lastVerifiedTime`
- 在上述真值模型未闭环前：
  - 可以生成 outbox
  - 不允许真实派发
  - 必须进入 `BLOCKED_NO_OWNER`

### 5.3 Notify Outbox 模型
建议新增独立数据模型：`booking_review_notify_outbox`

最小字段建议：
- `id`
- `bizType`：固定 `BOOKING_REVIEW_NEGATIVE`
- `bizId`：`reviewId`
- `storeId`
- `receiverRole`：固定 `STORE_MANAGER`
- `receiverUserId`
- `notifyType`：固定 `NEGATIVE_REVIEW_CREATED`
- `channel`：第一版固定 `IN_APP`
- `status`：`PENDING / SENT / FAILED / BLOCKED_NO_OWNER`
- `retryCount`
- `nextRetryTime`
- `sentTime`
- `lastErrorMsg`
- `idempotencyKey`
- `payloadSnapshot`
- `lastActionCode`
- `lastActionBizNo`
- `lastActionTime`

推荐幂等键：
- `booking_review:negative_created:{reviewId}:{receiverUserId}`

### 5.4 状态机
- `PENDING`
  - 已生成通知意图
  - 已找到有效店长账号
  - 等待派发
- `SENT`
  - 已发送成功
- `FAILED`
  - 已找到接收人，但发送失败，可重试
- `BLOCKED_NO_OWNER`
  - 当前门店没有有效 `managerAdminUserId`
  - 这是主数据阻断，不是发送失败

## 6. 后台可观测面

### 6.1 评价详情页
建议补充只读通知块：
- 通知状态
- 接收角色
- 接收账号 ID
- 渠道
- 最近发送时间
- 最近失败原因
- 若为 `BLOCKED_NO_OWNER`，明确展示“缺门店店长账号绑定”

### 6.2 Notify Outbox 台账
建议新增 admin-only 台账：
- 筛选字段：
  - `status`
  - `storeId`
  - `reviewId`
  - `receiverUserId`
  - `createTime`
- 列表字段：
  - `reviewId`
  - `storeId`
  - `receiverUserId`
  - `status`
  - `channel`
  - `retryCount`
  - `lastErrorMsg`
  - `lastActionTime`
- 允许动作：
  - 手工重试 `FAILED`
  - 查看阻断原因
- 明确不提供：
  - 强制改接收人
  - 越权派发

## 7. 测试策略
1. `createReview()` 差评时生成 outbox，非差评不生成。
2. 有有效 `managerAdminUserId` 时进入 `PENDING`，无 owner 时进入 `BLOCKED_NO_OWNER`。
3. 派发器支持 `PENDING -> SENT`、`FAILED` 重试、幂等保护。
4. 详情页与 outbox 台账都能正确展示通知状态与阻断原因。

## 8. No-Go
1. 不得把 `contactMobile` 当自动通知目标。
2. 不得把 `managerClaimedByUserId` 当门店店长账号。
3. 不得把 `outbox` 已写出写成“店长已收到通知”。
4. 不得在没有稳定 `managerAdminUserId` 真值前宣称“自动通知已上线”。
5. 不得扩展到客服负责人、区域负责人升级链路。
6. 不得让通知失败影响用户评价提交成功。
7. 不得把本专题写成 release-ready。

## 9. 推荐实施顺序
1. 先冻结 `manager account routing truth`。
2. 再新增 `booking review notify outbox`。
3. 再补后台可观测面。
4. 最后才接真实 `IN_APP` 派发器。

## 10. 单一真值引用
- `docs/products/miniapp/2026-03-21-miniapp-booking-review-manager-account-routing-truth-v1.md`
- `docs/products/miniapp/2026-03-19-miniapp-booking-review-manager-ownership-truth-review-v1.md`
- `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
