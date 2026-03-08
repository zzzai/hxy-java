# MiniApp Addbook Conflict Spec v1 (2026-03-09)

## 1. 目标与范围
- 目标：收口小程序“加钟/升级/加项目”（addbook）链路的冲突处理与幂等语义，避免重复创建与串单。
- 适用分支：`feat/ui-four-account-reconcile-ops`
- 本版约束：仅定义契约，不改业务代码；错误码优先复用现有稳定锚点。

## 2. API 列表（路由、方法、请求、响应）

| 路由 | 方法 | 状态 | 请求（关键字段） | 响应（关键字段） |
|---|---|---|---|---|
| `/app-api/booking/addon/create` | `POST` | 已有 | body: `parentOrderId`, `addonType(1=EXTEND,2=UPGRADE,3=ADD_ITEM)`, `spuId?`, `skuId?`; header: `X-Idempotency-Key?` | `CommonResult<Long>`（`data=addonOrderId`）；兼容扩展 header：`X-Idempotent-Hit?`、`X-Degrade-Reason?` |
| `/booking/order/get` | `GET` | 已有 | query: `id` | `id`, `orderNo`, `status`, `parentOrderId`, `addonType`, `payPrice` |
| `/booking/order/list-by-status` | `GET` | 已有 | query: `status` | `list[]`（含 `id/orderNo/status/parentOrderId/addonType/payPrice`） |
| `/booking/order/update-refunded` | `POST` | 已有 | body: `payRefundId`, `merchantRefundId` | `true/false`（用于退款回调冲突复用校验） |

## 3. 幂等键与冲突策略

### 3.1 幂等键
- 主键：`ADDON:<userId>:<parentOrderId>:<addonType>:<spuId|0>:<skuId|0>:<clientToken>`
- `clientToken` 来源优先级：
  1. `X-Idempotency-Key`
  2. 服务端派生键（请求字段标准化后 hash）

### 3.2 冲突处理
- 同幂等键 + 同请求摘要：返回同一 `addonOrderId`（幂等命中，不重复创建）。
- 同幂等键 + 不同请求摘要：返回冲突错误（当前复用 `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012)` 语义）。
- 资源冲突：
  - 时间槽不可用：`TIME_SLOT_NOT_AVAILABLE(1030003001)`
  - 排班冲突：`SCHEDULE_CONFLICT(1030002001)`
- 业务前置不满足：
  - 父订单不存在：`BOOKING_ORDER_NOT_EXISTS(1030004000)`
  - 父订单状态不允许加单：`BOOKING_ORDER_STATUS_ERROR(1030004001)`

## 4. 错误码清单（稳定、可检索）

| 锚点 | 编码 | 用途 | 兼容策略 |
|---|---:|---|---|
| `BOOKING_ORDER_NOT_EXISTS` | `1030004000` | 父订单不存在 | 保持不变 |
| `BOOKING_ORDER_STATUS_ERROR` | `1030004001` | 父订单状态不允许加单 | 保持不变 |
| `TIME_SLOT_NOT_AVAILABLE` | `1030003001` | 下个时段不可用/锁定失败 | 保持不变 |
| `SCHEDULE_CONFLICT` | `1030002001` | 排班时间冲突 | 保持不变 |
| `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT` | `1030004012` | 复用为 addbook 同键异参冲突 | 本版不新增数值码；后续若引入 `BOOKING_ADDON_IDEMPOTENT_CONFLICT`，需双写 1 个小版本并保留旧码映射 |
| `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS` | `1030004016` | 运行批次/审计查询 runId 不存在 | 保持不变 |
| `TICKET_SYNC_DEGRADED` | warning tag | 下游同步降级标记 | 作为可检索标签，不影响 HTTP 成功码 |

## 5. fail-open / degrade 语义
- 商品中心（SPU/SKU）读取异常：允许降级创建（`serviceName` 回退默认文案，价格回退到兼容值），记录 `degradeReason`。
- 工单/审计同步失败：主链路不回滚，打 `TICKET_SYNC_DEGRADED`，进入异步补偿。
- 时段冲突、状态冲突属于业务冲突，`fail-close` 返回业务错误，不做静默成功。

## 6. 审计字段要求
- 所有关键日志必须包含：`runId`, `orderId`, `payRefundId`, `sourceBizNo`, `errorCode`。
- addbook 非退款场景要求：
  - `payRefundId` 填 `0`（或约定空值）
  - `sourceBizNo` 推荐：`ADDON:<parentOrderId>:<addonType>:<addonOrderId|PENDING>`
- 缺失字段必须产出 `finance-log-validate` 告警。

## 7. 与现有契约兼容说明（向后兼容）
- `POST /app-api/booking/addon/create` 保持 `CommonResult<Long>` 不变。
- 新增信息仅通过可选 header / 日志输出，旧客户端无需改造。
- 本版不新增数值错误码；仅复用既有稳定码与 warning tag，避免破坏既有解析逻辑。
