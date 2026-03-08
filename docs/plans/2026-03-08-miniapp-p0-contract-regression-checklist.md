# 2026-03-08 MiniApp P0 Contract Regression Checklist

## 1. 用例清单（接口/场景/预期）

| 接口/域 | 场景 | 预期 | 覆盖测试 |
|---|---|---|---|
| `GET /trade/order/pay-result` | 正常返回（字段完整） | 返回 `code=0`，`data` 含 `orderId/orderNo/payOrderId/orderStatus/orderPayStatus/payload pay fields/payResultCode/payResultDesc` | `AppTradeOrderControllerTest#getOrderPayResult_shouldReturnFullPayloadWhenPayOrderExists` |
| `GET /trade/order/pay-result` | 查无数据 | 返回 `code=0` 且 `data=null`，不抛 500 | `AppTradeOrderControllerTest#getOrderPayResult_shouldReturnNullWhenOrderNotExists` |
| `GET /trade/order/pay-result` | 降级返回（pay 侧缺失） | 当 `payOrderId` 存在且 pay 查询缺失时，`degraded=true`、`degradeReason=PAY_ORDER_NOT_FOUND`，语义稳定 | `AppTradeOrderControllerTest#getOrderPayResult_shouldMarkDegradedWhenPayOrderMissing` |
| `GET /trade/after-sale/refund-progress` | 正常返回（字段完整） | 返回 `code=0`，`data` 含 `afterSale/order/payRefund/progress` 全量关键字段 | `AppAfterSaleControllerTest#getRefundProgress_shouldReturnFullPayloadWhenAfterSaleAndPayRefundExists` |
| `GET /trade/after-sale/refund-progress` | 查无数据 | 返回 `code=0` 且 `data=null`，不抛 500 | `AppAfterSaleControllerTest#getRefundProgress_shouldReturnNullWhenAfterSaleNotExists` |
| `booking` 错误码锚点 | `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT` | 固定为 `1030004012` | `BookingOrderServiceImplTest#testErrorCodeAnchor_bookingRefundIdempotentConflict_shouldStayStable` |
| `booking` 错误码锚点 | `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS` | 固定为 `1030004016`，空 runId 触发该码 | `BookingRefundNotifyLogServiceTest#replayRunIdErrorCodeAnchor_shouldStayStable` |
| `trade` 错误码锚点 | `AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED` | 固定为 `1011000125` | `AppAfterSaleControllerTest#errorCodeAnchor_shouldKeepAfterSaleBundleRefundCodeStable` |
| `booking` fail-open | 下游异常不阻断主链路且可检索 | `tradeServiceOrderApi.cancelByPayOrderId` 异常时，主链路退款成功，`getOrder` 可检索到 `REFUNDED + payRefundId` | `BookingOrderServiceImplTest#testUpdateOrderRefunded_tradeSyncFailOpenAndResultRetrievable` |

## 2. 已通过项

- 上表所有用例已通过。
- 定向回归命令执行结果：`Tests run: 65, Failures: 0, Errors: 0, Skipped: 0`。

## 3. 未覆盖项

- `GET /trade/order/pay-result` 的 `sync=true` 分支（含同步后重查）未在本批覆盖。
- `GET /trade/order/pay-result` 在 `payOrderStatus=REFUND/CLOSED` 的完整结果矩阵未覆盖。
- `GET /trade/after-sale/refund-progress` 同时传 `orderId + afterSaleId` 的优先级分支未覆盖。
- 当前为 controller/service 级回归，未新增 HTTP 序列化层（MockMvc）契约快照测试。

## 4. 风险与后续建议

### P0

- 建议补一组 API 层 JSON 契约快照（含 `null` 字段策略），避免客户端反序列化语义漂移。
- 建议把 `degradeReason` 可选值（如 `PAY_ORDER_NOT_FOUND`）沉淀为集中常量并加入接口文档，避免多端解释不一致。

### P1

- 建议补齐支付结果/退款进度状态矩阵（WAITING/SUCCESS/REFUND/FAILURE/CLOSED）自动化覆盖。
- `trade` 错误码段存在与 SLA 常量同码现象（`1011000125`），建议后续统一排查并制定冲突治理清单。
