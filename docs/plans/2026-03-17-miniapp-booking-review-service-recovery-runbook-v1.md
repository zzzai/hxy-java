# MiniApp Booking Review Service Recovery Runbook v1（2026-03-17）

## 1. 目标与当前边界
- 目标：把 booking review 当前已落地的恢复链路固定成可执行 runbook。
- 当前覆盖：
  - 用户评价提交后的人工恢复
  - 后台回复、跟进、看板查看
  - 店长待办台账 / 详情 / 看板治理
  - 差评通知双通道 outbox、人工重试、SLA reminder
- 当前不覆盖：
  - 自动好评奖励
  - 自动差评补偿
  - 区域负责人多级升级
  - 发布级 rollout / gate / runtime 样本
- 当前发布结论：`Can Develop / Cannot Release`。

## 2. 当前系统能力真值

### 2.1 已有系统能力
1. 用户可从订单列表 / 订单详情进入评价。
2. 差评会在数据层自动派生为：
   - `reviewLevel=差评`
   - `riskLevel=紧急`
   - `displayStatus=review_pending`
   - `followStatus=待跟进`
   - `auditStatus=manual_review`
3. 后台已有台账、详情、回复、跟进状态更新、看板页面。
4. 差评当前可进入后台“店长待办”池，系统会写入 10 分钟认领 / 30 分钟首次处理 / 24 小时闭环截止时间。
5. 历史差评如果尚未初始化 `managerTodo*` 字段，当前只有在首次执行店长待办写动作时才会 lazy-init；list / dashboard 不会在读链路自动补齐。
6. 当前后台已补齐 notify outbox 台账、manager routing 只读核查、阻断诊断、动作审计，以及台账级快捷筛选 / 快捷动作。
7. 同一条差评当前会按门店路由生成两条独立通知记录：
   - `IN_APP`
   - `WECOM`
8. 若店长 App 账号或店长企微账号缺失，只阻断对应通道，不影响另一通道记录；阻断状态统一为 `BLOCKED_NO_OWNER`，具体原因写入 `lastErrorMsg`。
9. SLA 提醒当前会生成三类通知意图：
   - `MANAGER_CLAIM_TIMEOUT`
   - `MANAGER_FIRST_ACTION_TIMEOUT`
   - `MANAGER_CLOSE_TIMEOUT`
10. 企微通知当前走共享机器人 sender，是否可派发取决于：
   - `hxy.booking.review.notify.wecom.enabled`
   - `hxy.booking.review.notify.wecom.webhook-url`
   - `managerWecomUserId`

### 2.2 当前未有系统能力
1. 没有发布级的双通道送达样本与灰度证据。
2. 没有自动把问题升级到技师负责人、客服负责人或区域负责人。
3. 没有自动补偿或奖励。
4. 没有独立 feature flag / rollout control / runtime gate。
5. 当前店长账号路由虽已进入工程真值，但还不能写成“全量门店都已完成账号绑定”。

## 3. 推荐角色分工

| 角色 | 当前职责 | 系统是否自动承接 |
|---|---|---|
| 运营值班 / 服务恢复 owner | 每日查看评价台账、筛选差评、执行必要的后台动作 | 是，台账与详情页已提供快捷筛选和快捷动作 |
| 店长 | 处理门店环境、接待、履约体验问题 | 部分，系统会生成 App / 企微通知意图，但送达是否成立仍取决于路由与通道配置 |
| 技师负责人 | 处理技师服务质量问题 | 否，当前只能人工转派 |
| 客服恢复 owner | 负责回访、致歉、人工补偿决策 | 否，当前只支持后台回复 / 跟进状态记录 |
| 区域负责人 | 处理重大或反复差评 | 否，当前无自动升级链路 |

补充说明：
- 当前“店长”对外真值仍然分两层：
  - 联系人快照：`contactName/contactMobile`
  - 账号路由：`managerAdminUserId / managerWecomUserId`
- 不能把任一层单独误写成“门店店长通知已全面上线”。

## 4. 人工恢复分池规则

| 命中条件 | 当前分池 | 建议动作 |
|---|---|---|
| `overallScore <= 2` | 紧急恢复池 | 立即查看详情，检查 notify outbox 与店长待办状态 |
| `overallScore == 3` | 关注池 | 结合内容与标签决定是否人工跟进 |
| `overallScore >= 4` | 常规池 | 进入统计，不触发即时人工升级 |
| 命中负向标签 + 差评 | 紧急恢复池 | 人工确认是否需要店长 / 技师负责人介入 |

## 5. 当前 SLA 真值
1. 差评店长待办创建后，系统会写入：
   - 10 分钟认领截止
   - 30 分钟首次处理截止
   - 24 小时闭环截止
2. 后台台账、详情、看板都能观察这些截止时间。
3. 当前代码已新增自动提醒出站任务，但提醒语义仍属于 admin-only 治理增强，不代表发布级通知已经验收完成。
4. 当历史差评缺少 booking order 行时，系统仍会按 `submitTime` 回填触发类型与 SLA 截止时间；店长联系人快照可能继续为空。

## 6. 后台操作流程

### 6.1 台账巡检
1. 进入 `/mall/booking/review`。
2. 按 `riskLevel=2`、`followStatus=1`、`replyStatus=false` 优先筛选。
3. 如需店长协同，可叠加：
   - `onlyManagerTodo=true`
   - `managerTodoStatus`
   - `managerSlaStatus`
4. 可直接使用快捷入口：
   - `待认领优先`
   - `认领超时提醒`
   - `首次处理超时提醒`
   - `闭环超时提醒`
   - `历史待初始化`

### 6.2 详情核查
1. 打开 `/mall/booking/review/detail?id={reviewId}`。
2. 先看评分、标签、内容、图片、回复状态、跟进状态。
3. 再看店长待办区块：
   - `managerTodoStatus`
   - `managerClaimDeadlineAt`
   - `managerFirstActionDeadlineAt`
   - `managerCloseDeadlineAt`
4. 再看通知区块：
   - 同一条评价下是否同时存在 `IN_APP / WECOM`
   - 通道当前状态、接收账号、诊断结论、修复建议

### 6.3 阻断处理
1. 若状态为 `BLOCKED_NO_OWNER`：
   - 打开 `查看店长路由`
   - 确认是 `NO_ROUTE / APP_MISSING / WECOM_MISSING / CHANNEL_DISABLED` 哪一种
2. 若状态为 `FAILED`：
   - 先看 `lastErrorMsg`
   - 修复后再执行人工重试
3. 若某一通道已 `SENT`，另一通道阻断：
   - 只修阻断通道，不要把已发送通道重复当故障处理

### 6.4 SLA 提醒巡检
1. 进入台账后优先查看三类超时提醒入口。
2. 若提醒已生成但通道阻断：
   - 优先修路由或通道配置
3. 若提醒已发送：
   - 仍需回到评价详情，核对店长待办是否真正推进
4. 不得把“提醒 outbox 已 SENT”写成“门店问题已闭环”。

## 7. 当前恢复口径
1. 用户评价提交成功，不等于店长已收到消息。
2. 通知失败或阻断，不影响评价主链路成功。
3. `IN_APP / WECOM` 两条记录必须独立看待、独立审计、独立重试。
4. `BLOCKED_NO_OWNER:NO_APP_ACCOUNT`、`BLOCKED_NO_OWNER:NO_WECOM_ACCOUNT`、`BLOCKED_NO_OWNER:CHANNEL_DISABLED` 都属于 No-Go 证据，不是 warning。
5. 当前没有服务端 `degraded=true / degradeReason` 真实证据。

## 8. No-Go
1. 不得把双通道 outbox 写成“双通道通知已正式上线”。
2. 不得把共享企微 sender 工程接入写成“企微到人必达已验证”。
3. 不得把提醒任务存在写成“SLA 自动化治理已 fully closed”。
4. 不得把联系人手机号写成当前自动通知目标。
5. 不得把奖励、补偿、区域负责人升级写成当前能力。
