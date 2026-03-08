# MiniApp Gift Card Domain Contract v1 (2026-03-09)

## 1. 目标与范围
- 目标：定义礼品卡购买/赠送/领取/核销/退款最小可用契约，补齐 P0 缺口。
- 本版定位：契约冻结文档（Domain Contract），不包含业务代码改动。
- 错误码策略：优先复用现有稳定数值码，不新增新的数值段。

## 2. API 列表（路由、方法、请求、响应）

| 路由 | 方法 | 状态 | 请求（关键字段） | 响应（关键字段） |
|---|---|---|---|---|
| `/promotion/gift-card/template/page` | `GET` | 规划 | `pageNo`, `pageSize`, `status?` | `list[]:{templateId,title,faceValue,stock,validDays}`, `total` |
| `/promotion/gift-card/order/create` | `POST` | 规划 | `templateId`, `quantity`, `sendScene(SELF|GIFT)`, `receiverMemberId?`, `clientToken` | `orderId`, `giftCardBatchNo`, `amountTotal`, `degraded`, `degradeReason` |
| `/promotion/gift-card/order/get` | `GET` | 规划 | `orderId` | `orderId`, `status`, `cards[]:{cardNo,status,receiverMemberId}`, `degraded` |
| `/promotion/gift-card/redeem` | `POST` | 规划 | `cardNo`, `redeemCode`, `clientToken` | `redeemRecordId`, `memberId`, `cardStatus`, `degraded` |
| `/promotion/gift-card/refund/apply` | `POST` | 规划 | `orderId`, `reason`, `payRefundId?`, `clientToken` | `afterSaleId`, `refundStatus`, `degraded`, `degradeReason` |

## 3. 幂等键与冲突策略
- 下单幂等键：`GIFT_ORDER:<memberId>:<templateId>:<clientToken>`
- 领取/核销幂等键：`GIFT_REDEEM:<memberId>:<cardNo>:<clientToken>`
- 退款申请幂等键：`GIFT_REFUND:<orderId>:<clientToken>`

冲突策略：
- 同键同参：返回首次结果（幂等命中）。
- 同键异参：返回冲突错误（当前复用 `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012)`）。
- 卡状态冲突（已核销/已退款）：返回业务冲突，不静默成功。

## 4. 错误码清单（稳定、可检索）

| 锚点 | 编码 | 用途 | 兼容策略 |
|---|---:|---|---|
| `USER_NOT_EXISTS` | `1004001000` | 购买人/受赠人不存在 | 保持不变 |
| `ORDER_NOT_FOUND` | `1011000011` | 礼品卡订单不存在 | 保持不变 |
| `COUPON_TEMPLATE_NOT_EXISTS` | `1013004000` | 礼品卡模板不存在（复用模板不存在语义） | 本版不新增数值码；后续可引入 gift-card 专属码并双写迁移 |
| `POINT_ACTIVITY_UPDATE_STOCK_FAIL` | `1013007008` | 库存不足/扣减失败（复用库存失败语义） | 保持可检索映射 |
| `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT` | `1030004012` | 同键异参冲突 | 如后续新增域内冲突码，至少保留 1 个小版本兼容映射 |
| `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS` | `1030004016` | runId 审计查询不存在 | 保持不变 |
| `TICKET_SYNC_DEGRADED` | warning tag | 工单/通知链路降级 | 仅标记，不改变主链路成功语义 |

## 5. fail-open / degrade 语义
- 支付成功后发卡失败：主订单保持成功，礼品卡状态置 `ISSUE_PENDING`，异步补偿发卡。
- 赠送通知失败：礼品卡创建成功但通知降级，返回 `degraded=true`。
- 退款单同步失败：主退款流程不回滚，记录 `TICKET_SYNC_DEGRADED`。

## 6. 审计字段要求
- 日志与审计表必须可检索：`runId`, `orderId`, `payRefundId`, `sourceBizNo`, `errorCode`。
- 推荐 `sourceBizNo`：
  - 下单：`GIFT_ORDER:<orderId>`
  - 核销：`GIFT_REDEEM:<cardNo>`
  - 退款：`GIFT_REFUND:<orderId>:<payRefundId|0>`
- 非退款流程 `payRefundId` 允许填 `0`，但字段不可缺失。

## 7. 与现有契约兼容说明（向后兼容）
- 全部新增字段遵循“可选、可忽略”原则，不破坏旧端解析。
- 现有订单/售后/会员接口保持不变，仅新增 gift-card 域接口。
- 如后续引入礼品卡专属错误码，需提供“新旧码并存 + 别名映射”过渡窗口。
