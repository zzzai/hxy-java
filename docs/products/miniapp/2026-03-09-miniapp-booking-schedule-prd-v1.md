# MiniApp 预约排期 PRD v1（2026-03-09）

## 0. 文档定位
- 目标：补齐预约排期的产品口径，解决 UI 评审里“预约链路可用但规范不完整”的问题。
- 约束基线：
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`

## 1. 场景目标与非目标
### 1.1 场景目标
- 打通「选技师 -> 选时段 -> 下单支付 -> 履约 -> 退款/售后」预约主链路。
- 将预约页状态展示与统一状态机对齐，避免“状态文案正确但流转错误”。
- 在支付、回放、工单同步异常时提供可恢复动作，不阻断用户主流程。

### 1.2 非目标
- 不定义技师排班算法（班次生成、容量优化不在本 PRD）。
- 不新增预约域错误码，仅消费已冻结码与降级语义。
- 不涉及门店端运营后台改造。

## 2. 页面信息架构与关键流程
### 2.1 IA（预约排期）
| 页面路由 | 角色 | 关键能力 | 依赖接口 |
|---|---|---|---|
| `/pages/booking/technician-list` | 选择入口 | 技师列表与筛选 | `GET /booking/technician/list-by-store` |
| `/pages/booking/technician-detail` | 排期选择 | 技师档案 + 可预约时段 | `GET /booking/technician/get`, `GET /booking/time-slot/list` |
| `/pages/booking/order-confirm` | 下单确认 | 确认时段与费用 | `POST /booking/order/create` |
| `/pages/pay/index` / `/pages/pay/result` | 支付闭环 | 支付、结果确认 | 支付聚合接口 |
| `/pages/booking/order-list` | 进度管理 | 按状态筛选、取消、去支付 | `GET /booking/order/list`, `PUT /booking/order/cancel` |
| `/pages/booking/order-detail` | 单据详情 | 预约详情、退款进度跳转 | `GET /booking/order/get` |

### 2.2 关键流程
1. 用户在技师详情页选择日期与时段，创建预约订单。
2. 预约单进入支付，支付结果回流后刷新预约列表状态。
3. 已支付预约可进入履约；异常退款进入退款进度与售后链路。

## 3. 状态机映射（引用统一状态机）
统一引用：`docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`

| 预约展示态 | 统一状态机映射 | 前端动作 |
|---|---|---|
| 待支付（status=0） | `CREATED` | 展示“去支付/取消” |
| 已支付（status=1） | `PAID` | 展示“查看详情/改约入口（如开放）” |
| 服务中（status=3） | `SERVING` | 展示“服务进度/联系客服” |
| 已完成（status=4） | `FINISHED` | 展示“评价/售后入口” |
| 已退款（status=5） | `REFUNDED`（退款侧路径） | 展示退款完成态 |
| 已取消（status=2） | 终止展示态（不反向驱动主状态机） | 保持只读，不允许回退流转 |

## 4. 错误码与降级语义
| Code/Key | 触发场景 | 前端动作 | 降级语义 |
|---|---|---|---|
| `BOOKING_ORDER_NOT_EXISTS` | 预约详情/列表目标不存在 | 展示空态 + 返回列表 | 不白屏，保持页面可退出 |
| `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012)` | 退款通知冲突（同单不同退款单） | 阻断自动重试，提示人工复核 | 保留现态，不重复提交 |
| `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS(1030004016)` | 重放 runId 缺失 | 展示可刷新态，不崩溃 | 允许运营刷新批次后重试 |
| `PAY_ORDER_NOT_FOUND`（`degradeReason`） | 支付结果查询缺失 | 展示待确认并支持手动刷新 | fail-open，不阻断预约主链路 |
| `TICKET_SYNC_DEGRADED` | 下游工单同步异常 | 主流程成功 + warning | 后台异步重试，不回滚用户态 |

## 5. 埋点事件（最小集）
| 事件名 | 触发时机 | 必填属性 |
|---|---|---|
| `page_view` | 进入预约相关页面 | `route`, `storeId?`, `technicianId?` |
| `booking_slot_view` | 可预约时段加载完成 | `technicianId`, `date`, `slotCount` |
| `booking_slot_select` | 选择时段 | `technicianId`, `slotId`, `bookingDate` |
| `booking_order_submit` | 创建预约单 | `bookingOrderId`, `payPrice`, `resultCode`, `errorCode?` |
| `pay_result_view` | 支付结果页展示 | `orderId`, `payResultCode`, `degraded` |
| `booking_order_cancel` | 取消预约 | `bookingOrderId`, `statusBefore`, `resultCode` |

## 6. 验收清单
### 6.1 Happy Path
- [ ] 可完成选技师、选时段、创建预约、支付回流。
- [ ] 预约列表按状态筛选与详情跳转正确。
- [ ] 取消预约成功后列表状态即时更新。

### 6.2 业务错误
- [ ] `BOOKING_ORDER_NOT_EXISTS` 返回空态，不白屏。
- [ ] `1030004012` 出现时阻断自动重试并提示人工复核。
- [ ] `1030004016` 出现时保留页面可操作（刷新/返回）。

### 6.3 降级路径
- [ ] 支付查询缺失时按 `PAY_ORDER_NOT_FOUND` 展示待确认，不误判成功。
- [ ] 工单同步异常时打 warning（`TICKET_SYNC_DEGRADED`），主链路不回滚。
- [ ] 网络异常可进入统一兜底页并可重试。
