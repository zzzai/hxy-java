# Booking Review SLA Priority Ops Design（2026-03-21）

## 1. 背景
- booking review 当前已经具备：
  - 差评台账 / 详情 / 看板
  - 店长待办状态机
  - 双通道通知 outbox
  - SLA timeout 提醒
  - notify 阻断诊断与 manager routing 核查
- 当前后台真正剩余的问题，不是“看不到状态”，而是：
  - 值班人员仍要自己判断哪条最该先处理
  - 看板只能看到“已超时”，还不能稳定看到“即将超时”
  - SLA 风险与通知风险分散在多个页面，缺少统一优先级

## 2. 目标
- 在不新增复杂流程的前提下，把 booking review 后台升级成更可执行的值班工作台。
- 固定三类只读派生真值：
  - `managerSlaStage`
  - `priorityLevel / priorityReason`
  - `notifyRiskSummary`
- 支持三类“即将超时”入口：
  - `CLAIM_DUE_SOON`
  - `FIRST_ACTION_DUE_SOON`
  - `CLOSE_DUE_SOON`
- 继续保持结论：`Doc Closed / Can Develop / Cannot Release`。

## 3. 不做的内容
1. 不做自动奖励。
2. 不做自动补偿。
3. 不做区域负责人升级。
4. 不把当前值班增强写成 release-ready。
5. 不新增独立工作台主表。

## 4. 方案比较

### 方案 A：只做前端标签和本地排序
- 优点：改动快。
- 缺点：优先级规则散落在页面层，后端和看板无法共用。
- 结论：不采用。

### 方案 B：后端派生只读字段，前端消费
- 优点：
  - 规则单一真值
  - page / get / dashboard 可共享同一套判断
  - 后续若扩成更强的值班工作台，能沿用当前派生规则
- 缺点：
  - 要补 controller/service 响应与测试
- 结论：采用。

### 方案 C：新建独立 SLA 工作台页
- 优点：可做得更完整。
- 缺点：范围过大，当前会把 P2 做成 P3。
- 结论：暂不采用。

## 5. 核心设计

### 5.1 SLA 阶段（`managerSlaStage`）
当前冻结为：
- `PENDING_INIT`
- `NORMAL`
- `CLAIM_DUE_SOON`
- `CLAIM_TIMEOUT`
- `FIRST_ACTION_DUE_SOON`
- `FIRST_ACTION_TIMEOUT`
- `CLOSE_DUE_SOON`
- `CLOSE_TIMEOUT`
- `CLOSED`

建议的 due-soon 窗口：
- `CLAIM_DUE_SOON`：距离认领截止 `<= 5` 分钟，且尚未认领
- `FIRST_ACTION_DUE_SOON`：距离首次处理截止 `<= 10` 分钟，且尚未记录首次处理
- `CLOSE_DUE_SOON`：距离闭环截止 `<= 120` 分钟，且尚未闭环

### 5.2 优先级（`priorityLevel / priorityReason`）
建议四档：
- `P0`
  - `CLOSE_TIMEOUT`
  - 任一通道 `BLOCKED_NO_OWNER`
- `P1`
  - `FIRST_ACTION_TIMEOUT`
  - `CLAIM_TIMEOUT`
  - `CLOSE_DUE_SOON`
  - 任一通道 `FAILED`
- `P2`
  - `FIRST_ACTION_DUE_SOON`
  - `CLAIM_DUE_SOON`
  - `PENDING_INIT`
- `P3`
  - 其他正常待办 / 正常差评待观察

### 5.3 通知风险摘要（`notifyRiskSummary`）
- 只做只读摘要，不改 notify outbox 状态机。
- 典型口径：
  - `双通道阻断`
  - `App 阻断，企微待派发`
  - `企微发送失败`
  - `双通道待派发`
  - `双通道已派发`
  - `未核出通知记录`

### 5.4 看板增强
新增 3 张卡片：
- `即将认领超时`
- `即将首次处理超时`
- `即将闭环超时`

并继续支持一键跳台账。

### 5.5 台账增强
新增只读列：
- `优先级`
- `优先级原因`
- `通知风险`

继续使用现有筛选表单，不新增新的业务动作。

## 6. 测试策略
1. 后端 service test：
- due-soon 阶段识别
- dashboard due-soon 计数
- admin page 对 due-soon 的过滤
2. controller test：
- page/get 返回 `managerSlaStage / priorityLevel / priorityReason / notifyRiskSummary`
3. node test：
- 看板新增 due-soon 卡片与 query 映射
- 台账新增优先级 / 通知风险列
- API 类型补齐只读字段

## 7. No-Go
1. 不得把 due-soon 看板写成自动升级链路。
2. 不得把优先级派生写成发布门禁解除。
3. 不得把 notifyRiskSummary 写成真实送达回执。
4. 不得把当前 admin-only 值班增强写成 release-ready。

## 8. 当前结论
- 这是后台值班效率增强，不是外部通知闭环升级。
- 当前口径继续固定为：`Doc Closed / Can Develop / Cannot Release / No-Go`。
