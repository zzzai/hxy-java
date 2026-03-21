# Booking Review Dual Channel Manager Notify Design（2026-03-21）

## 1. 背景
- 当前 booking review 已具备：
  - 差评提交
  - 店长待办台账 / 详情 / 看板
  - `notify outbox + dispatch job + manual retry`
  - `storeId -> managerAdminUserId` 路由只读核查
- 当前剩余真实缺口：
  - 同一条差评只会生成一条 `IN_APP` outbox
  - 没有企微接收人字段
  - 没有共享企微发送端
  - 没有通道级观测
  - 没有针对差评治理 SLA 的自动提醒
- 已冻结业务场景：
  - `1000` 家门店
  - 默认 `1 店 1 店长`
  - 同一条差评按 `storeId` 精准命中店长
  - 同时发 `店长端 App + 店长企微`
  - 集团共用一个企微发送端
  - 采用“同一条差评生成两条出站记录”的方案

## 2. 目标
- 把 booking review 店长通知从单通道工程骨架升级为双通道可执行实现。
- 让每个通道拥有独立状态、失败原因、重试和审计。
- 让后台能明确看到每个门店在 App / 企微两侧是否可派发。
- 为认领超时、首次处理超时、闭环超时提供自动提醒能力。
- 保持当前结论：`Can Develop / Cannot Release`。

## 3. 不做的内容
1. 不做自动好评奖励。
2. 不做自动差评补偿。
3. 不做区域负责人多级升级。
4. 不把当前能力写成 release-ready。

## 4. 方案比较

### 方案 A：同一条差评只保留一条总记录，再记录子通道明细
- 优点：表面上记录数更少。
- 缺点：
  - 重试、失败原因、人工审计都要拆二级结构。
  - 对当前 outbox/job/controller/page 改造更重。
  - 运维排障时可读性差。
- 结论：不采用。

### 方案 B：同一条差评生成两条出站记录，分别对应 App / 企微
- 优点：
  - 每个通道天然独立状态。
  - 最符合 outbox 模式。
  - 后续扩短信、区域负责人更自然。
- 缺点：台账记录数会增加。
- 结论：采用。

### 方案 C：同步直发 App + 企微，不落 outbox
- 优点：链路短。
- 缺点：
  - 差评提交主链路会被发送失败污染。
  - 无法独立审计和补偿。
- 结论：不采用。

## 5. 核心设计

### 5.1 路由真值
固定为：
- `storeId -> managerAdminUserId`
- `storeId -> managerWecomUserId`

其中：
- `managerAdminUserId` 供 `IN_APP` 使用
- `managerWecomUserId` 供 `WECOM` 使用
- 共享企微发送端配置不放在路由表，走统一配置中心

### 5.2 出站模型
同一条差评会生成两条 outbox：
- `channel = IN_APP`
- `channel = WECOM`

每条记录独立维护：
- `status`
- `retryCount`
- `nextRetryTime`
- `sentTime`
- `lastErrorMsg`
- `lastActionCode / lastActionBizNo / lastActionTime`

建议幂等键：
- `booking_review:{notifyType}:{reviewId}:{channel}:{receiver}`

### 5.3 阻断规则
- 缺 `managerAdminUserId`：只阻断 `IN_APP`
- 缺 `managerWecomUserId`：只阻断 `WECOM`
- 共享企微发送端未启用：只阻断 `WECOM`
- 任何一个通道失败，不影响另一通道状态
- 通知链路失败，不影响评价提交成功

### 5.4 发送器
- `IN_APP`：继续复用 `NotifySendService`
- `WECOM`：新增共享企微发送器，按统一配置发送
- 企微发送返回异常或关闭时，必须落 `FAILED` / `BLOCKED_*`，不能伪成功

### 5.5 SLA 自动提醒
至少覆盖三类：
- `CLAIM_TIMEOUT`
- `FIRST_ACTION_TIMEOUT`
- `CLOSE_TIMEOUT`

提醒规则：
- 基于当前 `managerTodo` 真实字段判断
- 每种超时事件按差评 + 通道做幂等提醒
- 继续走双通道 outbox，不新增旁路线

## 6. 后台观测

### 6.1 Notify Outbox 台账
必须能看见：
- 通道
- 通道接收账号
- 通道阻断原因
- 通道最近动作
- 通道级手工重试入口

### 6.2 评价详情
必须能看见：
- 同一条评价下 App / 企微两条记录
- 每条的当前状态
- 每条的错误原因与修复建议

### 6.3 路由核查页
必须能看见：
- App 账号绑定状态
- 企微账号绑定状态
- 路由是否有效
- 修复建议

## 7. 测试策略
1. 后端 service test：双通道创建、双通道派发、双通道阻断。
2. controller test：通道级诊断码、修复建议、人工重试限制。
3. node test：台账 / 详情 / 路由页都能展示双通道真实状态。
4. job test：SLA 提醒与双通道 dispatch 都具备幂等和限流。

## 8. No-Go
1. 不得把共享企微发送端接入写成“已正式上线”。
2. 不得把 `code=0` 但未真正送达写成成功样本。
3. 不得把“有 outbox”写成“店长已读”。
4. 不得把治理完成写成 booking review 已可放量。

## 9. 当前结论
- 本轮目标是把工程能力一次性补齐。
- 发布口径仍保持：`Doc Closed / Can Develop / Cannot Release / No-Go`。
