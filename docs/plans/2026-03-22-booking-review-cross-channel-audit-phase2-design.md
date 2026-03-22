# Booking Review 跨通道派发审计二期设计

## 1. 目标
- 把 notify outbox 从“单条出站记录视角”补成“按评价聚合的跨通道审计视角”。
- 让运营快速回答：
  - 同一条差评两条通道当前整体是什么状态
  - 是双通道阻断、单通道失败，还是人工重试后待复核
  - 当前是否存在跨通道状态分裂
- 保持 admin-only 审计增强，不改真实派发、重试、通道发送和发布口径。

## 2. 当前真值
- notify outbox 页已经具备：
  - 单条记录的状态、动作码、动作原因、重试次数、修复建议
  - 行级筛选：状态 / 渠道 / 最近动作
- 详情页已经具备：
  - 双通道摘要
  - App / 企微分通道卡片
  - 最近一条通知真值
- 当前仍缺：
  - notify outbox 台账页的“按评价聚合”摘要
  - 行级的跨通道结论
  - 全局审计卡片

## 3. 方案对比

### 方案 A：只在前端基于当前页 10 条数据做聚合
- 优点：快。
- 缺点：分页后会失真，summary 不能代表真实范围。

### 方案 B：后端统一按评价聚合审计摘要，前端消费
- 优点：summary 与行级跨通道结论都有单一真值；后续 `P2` 告警联动可复用。
- 缺点：需要补 mapper / service / controller / 前端类型。
- 结论：采用。

### 方案 C：直接引入“按评价聚合列表页”
- 优点：展示更完整。
- 缺点：新增一套列表与交互，超出本批最小价值闭环。

## 4. 设计结论

### 4.1 后端新增审计摘要
- 新增 `notify-outbox/summary` 接口。
- 统计维度按 `reviewId` 聚合当前筛选范围内的最新双通道状态：
  - `totalReviewCount`
  - `dualSentReviewCount`
  - `blockedReviewCount`
  - `failedReviewCount`
  - `manualRetryPendingReviewCount`
  - `divergedReviewCount`

### 4.2 行级补充跨通道结论
- 在 `BookingReviewNotifyOutboxRespVO` 中新增：
  - `reviewAuditStage`
  - `reviewAuditLabel`
  - `reviewAuditDetail`
- 每一条 row 仍然是单条 outbox，但会额外告诉运营“同评价双通道整体结论”。

### 4.3 审计阶段规则
- `DUAL_SENT`：两条通道最新记录都已发送
- `ANY_BLOCKED`：存在阻断
- `ANY_FAILED`：存在失败
- `MANUAL_RETRY_PENDING`：存在人工重试后待复核
- `DIVERGED`：双通道状态不一致
- `PENDING_DISPATCH`：当前整体仍处于待派发

### 4.4 前端工作台增强
- notify outbox 页新增“跨通道审计概览”卡片
- 表格新增：
  - `跨通道结论`
  - `跨通道说明`
- 保留现有单条记录操作、状态筛选、人工重试入口

## 5. 明确不做
- 不改 dispatch job 行为
- 不改失败重试策略
- 不改企微 sender / App sender
- 不把 summary 卡片写成真实送达率

## 6. 验证
- Node：notify outbox 页与 API 类型测试新增 summary / 跨通道字段断言
- Java：
  - controller 测试覆盖 `summary`
  - service 测试覆盖 review 级聚合摘要与行级 audit stage 派生
