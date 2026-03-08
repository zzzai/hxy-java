# MiniApp Referral Domain Contract v1 (2026-03-09)

## 1. 目标与范围
- 目标：定义“邀请有礼”从绑定邀请关系到奖励入账的闭环契约。
- 本版限制：仅冻结后端契约；不变更现有业务代码与数据结构。
- 错误码策略：优先复用现有码，保证多端稳定解析。

## 2. API 列表（路由、方法、请求、响应）

| 路由 | 方法 | 状态 | 请求（关键字段） | 响应（关键字段） |
|---|---|---|---|---|
| `/promotion/referral/bind-inviter` | `POST` | 规划 | `inviteCode` 或 `inviterMemberId`, `clientToken` | `refereeMemberId`, `inviterMemberId`, `bindStatus`, `idempotentHit` |
| `/promotion/referral/overview` | `GET` | 规划 | `memberId?`（默认当前用户） | `referralCode`, `totalInvites`, `effectiveInvites`, `pendingRewardAmount`, `rewardBalance`, `degraded` |
| `/promotion/referral/reward-ledger/page` | `GET` | 规划 | `pageNo`, `pageSize`, `status?` | `list[]:{ledgerId,orderId,sourceBizNo,rewardAmount,status,runId,payRefundId}`, `total` |
| `/promotion/referral/reward/settle` | `POST` | 规划（内部任务） | `runId`, `batchSize`, `dryRun` | `runId`, `successCount`, `skipCount`, `failCount`, `degraded` |
| `/promotion/referral/reward/retry` | `POST` | 规划（内部任务） | `runId`, `ids[]` | `retrySuccess`, `retryFail`, `failedIds[]` |

## 3. 幂等键与冲突策略
- 邀请绑定幂等键：`REF_BIND:<refereeMemberId>`（一个被邀请人只能绑定一次）。
- 奖励入账幂等键：`REF_REWARD:<orderId>:<rewardRole>:<memberId>`。
- 重试任务幂等键：`REF_RETRY:<runId>:<ledgerId>`。

冲突策略：
- 同键同参：幂等成功，返回既有记录。
- 同键异参：返回冲突错误（复用 `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012)`）。
- runId 不存在：返回 `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS(1030004016)`。

## 4. 错误码清单（稳定、可检索）

| 锚点 | 编码 | 用途 | 兼容策略 |
|---|---:|---|---|
| `USER_NOT_EXISTS` | `1004001000` | 邀请人/被邀请人不存在 | 保持不变 |
| `ORDER_NOT_FOUND` | `1011000011` | 奖励来源订单不存在 | 保持不变 |
| `POINT_RECORD_BIZ_NOT_SUPPORT` | `1004008000` | 积分奖励业务类型不支持 | 保持不变 |
| `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT` | `1030004012` | 绑定/入账同键异参冲突 | 后续若引入 referral 专属冲突码，需双写兼容 |
| `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS` | `1030004016` | 结算批次号不存在 | 保持不变 |
| `TICKET_SYNC_DEGRADED` | warning tag | 对账/工单同步降级标记 | 作为 warning 锚点保留 |

## 5. fail-open / degrade 语义
- 下单主链路优先：邀请奖励写账失败不回滚订单成功，状态置 `REWARD_PENDING`。
- 对账工单系统异常：返回 `degraded=true`，主流程继续，异步补偿。
- 通知发送失败：不影响绑定和入账主状态，追加重试任务。

## 6. 审计字段要求
- 全链路必须可按下列字段检索：`runId`, `orderId`, `payRefundId`, `sourceBizNo`, `errorCode`。
- 推荐 `sourceBizNo`：
  - 邀请绑定：`REF_BIND:<refereeMemberId>`
  - 奖励入账：`REF_REWARD:<orderId>:<memberId>`
- 非退款场景 `payRefundId` 统一填 `0`，不可省略。

## 7. 与现有契约兼容说明（向后兼容）
- Referral 域新增接口不影响既有 `trade/order`、`booking/order`、`member` 接口行为。
- 结果对象新增字段必须可选，旧端可忽略。
- 错误码保持现有码优先，避免前端解析规则大规模改造。
