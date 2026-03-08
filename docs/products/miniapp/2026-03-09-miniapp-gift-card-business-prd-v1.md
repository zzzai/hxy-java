# MiniApp 礼品卡业务 PRD v1（2026-03-09）

## 0. 文档定位与契约对齐
- 文档目标：定义礼品卡从购买到退款的产品执行规则，补齐 UI 评审中的核心业务缺口。
- 对齐契约：
  - `docs/contracts/2026-03-09-miniapp-gift-card-domain-contract-v1.md`
  - `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
- 约束：不新增与契约冲突的错误码语义，不修改既有主链路成功判定。

## 1. 用户流程（主流程 + 异常流程）
### 1.1 主流程
1. 用户进入礼品卡模板列表，选择模板与数量。
2. 用户选择送礼场景（自用/赠送），提交创建订单。
3. 支付成功后创建礼品卡批次并可查看订单详情。
4. 受赠人领取或核销礼品卡。
5. 符合退款条件时发起退款申请，进入退款状态追踪。

### 1.2 异常流程
1. 创建订单幂等冲突（同键异参）时，阻断创建并提示冲突处理。
2. 库存不足或模板不存在时，返回业务错误并保留页面输入态。
3. 发卡通知链路失败时，订单保持成功但礼品卡进入待补发状态。
4. 退款单同步失败时不回滚主退款流程，进入降级提示与异步补偿。

## 2. 业务规则与状态流转（引用统一状态机）
统一状态机引用：`docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`

### 2.1 业务规则
- 下单幂等键：`GIFT_ORDER:<memberId>:<templateId>:<clientToken>`。
- 核销幂等键：`GIFT_REDEEM:<memberId>:<cardNo>:<clientToken>`。
- 退款幂等键：`GIFT_REFUND:<orderId>:<clientToken>`。
- 同键同参：幂等命中，返回首次结果；同键异参：返回冲突错误。
- 卡状态冲突（已核销/已退款）必须 fail-close，禁止静默成功。

### 2.2 状态流转
| 对象 | 状态流转 | 说明 |
|---|---|---|
| 礼品卡订单 | `CREATED -> PAID -> ISSUED -> REDEEMED` 或 `PAID -> REFUND_PENDING -> REFUNDED` | 支付成功是订单主链路真值；发卡失败不逆转支付成功 |
| 退款进度 | `REFUND_PENDING -> REFUND_PROCESSING -> REFUND_SUCCESS / REFUND_FAILED` | 与统一退款进度码保持一致 |
| 工单/审计联动 | `SYNC_PENDING -> SYNCED`（失败标记 `TICKET_SYNC_DEGRADED`） | 降级不改变订单成功态 |

## 3. 错误码与降级语义（与现有契约一致）
| Code/Key | 触发场景 | 前端动作 | 降级语义 |
|---|---|---|---|
| `USER_NOT_EXISTS(1004001000)` | 购买人/受赠人不存在 | 阻断提交，提示校验失败 | 无降级，需修正输入 |
| `ORDER_NOT_FOUND(1011000011)` | 礼品卡订单不存在 | 展示空态并返回列表 | 无降级，保持页面可退出 |
| `COUPON_TEMPLATE_NOT_EXISTS(1013004000)` | 礼品卡模板不存在 | 阻断购买并刷新模板列表 | 无降级 |
| `POINT_ACTIVITY_UPDATE_STOCK_FAIL(1013007008)` | 库存不足/扣减失败 | 阻断支付前确认 | 无降级 |
| `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012)` | 同键异参冲突 | 阻断重复提交并提示联系客服 | 无降级 |
| `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS(1030004016)` | runId 查询不存在 | 展示可重试态 | 无降级 |
| `TICKET_SYNC_DEGRADED` | 通知/工单链路异常 | 展示 warning，不回滚主业务 | fail-open，异步补偿 |
| `degraded=true` + `degradeReason` | 发卡/通知/退款同步部分失败 | 保持订单主链路可见成功，显示补偿提示 | 主链路可用优先 |

## 4. 客服/申诉/人工兜底规则
- 客服入口必显：创建冲突、核销冲突、退款失败均提供“联系客服”动作。
- 申诉最小信息：`orderId`, `cardNo`, `sourceBizNo`, `runId`, `errorCode`, `payRefundId`。
- 人工兜底规则：
  1. 发卡失败且已支付：人工补发卡，不允许改写支付结果。
  2. 通知失败：可人工补通知，不影响卡状态。
  3. 退款同步失败：先确认退款主状态，再人工补录工单。

## 5. 验收清单
### 5.1 Happy Path
- [ ] 模板选择 -> 订单创建 -> 支付 -> 发卡 -> 领取/核销闭环可跑通。
- [ ] 退款申请后可看到一致的退款进度状态。

### 5.2 业务错误
- [ ] `1013004000` 模板不存在时阻断提交并提示可恢复动作。
- [ ] `1013007008` 库存不足时不进入支付。
- [ ] `1030004012` 幂等冲突时不重复创建订单。

### 5.3 Degraded Path
- [ ] 发卡通知失败返回 `degraded=true`，订单仍保持成功可见。
- [ ] `TICKET_SYNC_DEGRADED` 仅 warning，不回滚主流程。
- [ ] runId 缺失场景可重试且不白屏。
