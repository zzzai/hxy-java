# MiniApp 邀请有礼业务 PRD v1（2026-03-09）

## 0. 文档定位与契约对齐
- 文档目标：明确邀请绑定、奖励入账、重试补偿的产品口径，支撑业务与客服落地。
- 对齐契约：
  - `docs/contracts/2026-03-09-miniapp-referral-domain-contract-v1.md`
  - `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
- 约束：沿用既有错误码语义，不定义冲突的新数值码。

## 1. 用户流程（主流程 + 异常流程）
### 1.1 主流程
1. 新用户通过邀请码或邀请人 ID 绑定关系。
2. 被邀请人完成有效订单后，系统生成奖励待入账记录。
3. 结算任务执行奖励入账，用户在邀请页查看奖励台账。
4. 用户可查看邀请总览（有效邀请数、待到账金额、余额）。

### 1.2 异常流程
1. 被邀请人重复绑定或同键异参冲突时，阻断并返回冲突错误。
2. 奖励来源订单不存在时，台账记录标记失败并可重试。
3. 结算 runId 不存在时，页面保持可刷新，不中断其他能力。
4. 对账/工单链路异常时，奖励主状态保留，走降级标记与异步补偿。

## 2. 业务规则与状态流转（引用统一状态机）
统一状态机引用：`docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`

### 2.1 业务规则
- 邀请绑定幂等键：`REF_BIND:<refereeMemberId>`（一个被邀请人仅可绑定一次）。
- 奖励入账幂等键：`REF_REWARD:<orderId>:<rewardRole>:<memberId>`。
- 重试幂等键：`REF_RETRY:<runId>:<ledgerId>`。
- 同键同参幂等成功；同键异参返回冲突错误。

### 2.2 状态流转
| 对象 | 状态流转 | 说明 |
|---|---|---|
| 邀请绑定 | `UNBOUND -> BOUND -> EFFECTIVE` | 有效态由有效订单驱动，不由前端文案驱动 |
| 奖励台账 | `REWARD_PENDING -> SETTLING -> SETTLED / SETTLE_FAILED` | 失败可重试，不影响邀请关系 |
| 降级标记 | `NORMAL -> DEGRADED`（`TICKET_SYNC_DEGRADED`） | 仅标记协同链路，主账务链路不回滚 |

## 3. 错误码与降级语义（与现有契约一致）
| Code/Key | 触发场景 | 前端动作 | 降级语义 |
|---|---|---|---|
| `USER_NOT_EXISTS(1004001000)` | 邀请人/被邀请人不存在 | 阻断绑定并提示修正 | 无降级 |
| `ORDER_NOT_FOUND(1011000011)` | 奖励来源订单不存在 | 台账标失败并提供刷新/重试入口 | 无降级 |
| `POINT_RECORD_BIZ_NOT_SUPPORT(1004008000)` | 奖励业务类型不支持 | 阻断入账并提示人工处理 | 无降级 |
| `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012)` | 绑定/入账同键异参冲突 | 阻断重复请求并提示联系客服 | 无降级 |
| `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS(1030004016)` | 结算批次不存在 | 页面可刷新重试，不崩溃 | 无降级 |
| `TICKET_SYNC_DEGRADED` | 对账工单降级 | 展示 warning，主流程继续 | fail-open |
| `degraded=true` | 对账/通知链路部分失败 | 不回滚邀请绑定与奖励主状态 | 异步补偿 |

## 4. 客服/申诉/人工兜底规则
- 客服处理入口：绑定冲突、奖励未到账、账实不一致必须可直接提交申诉。
- 申诉材料最小集：`refereeMemberId`, `inviterMemberId`, `ledgerId`, `orderId`, `runId`, `sourceBizNo`, `errorCode`, `payRefundId`。
- 人工兜底规则：
  1. 绑定冲突：以首次成功绑定记录为准，后续请求仅回显。
  2. 入账失败：人工补账需保留 `sourceBizNo` 并写明补录原因。
  3. 对账降级：先确认主账，再补工单与通知，禁止逆向冲销已生效奖励。

## 5. 验收清单
### 5.1 Happy Path
- [ ] 邀请绑定成功后可在总览页看到关系生效。
- [ ] 有效订单后奖励进入台账并可结算到余额。
- [ ] 台账分页查询与状态筛选可用。

### 5.2 业务错误
- [ ] `1004001000` 用户不存在时绑定被阻断。
- [ ] `1011000011` 来源订单缺失时台账可追溯。
- [ ] `1030004012` 同键异参冲突时无重复入账。

### 5.3 Degraded Path
- [ ] 对账降级返回 `TICKET_SYNC_DEGRADED`，主流程保持可用。
- [ ] `1030004016` runId 缺失时界面可刷新重试。
- [ ] 通知失败时仅标记降级，不回滚已生效绑定/奖励。
