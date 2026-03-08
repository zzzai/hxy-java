# Window A Handoff - MiniApp P0 Contract Freeze (2026-03-08)

## 1. 变更摘要
- 完成小程序 P0 契约冻结清单：`docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`。
- trade app 新增两个聚合查询接口：
  - `GET /trade/order/pay-result`
  - `GET /trade/after-sale/refund-progress`
- 四账联动与工单同步异常日志补齐结构化字段输出（含 `runId/orderId/payRefundId/sourceBizNo/errorCode`）。
- 更新治理文档（事实基线/ADR/执行看板）。

## 2. 新增/增强接口

### 2.1 `GET /trade/order/pay-result`
- 入参：`orderId`(required), `sync`(optional)
- 主要返回：
  - `orderId/orderNo/payOrderId/orderStatus/orderPayStatus/orderRefundStatus/orderRefundPrice`
  - `payOrderStatus/payOrderStatusName/paySuccessTime/payChannelCode`
  - `payResultCode/payResultDesc`
  - `degraded/degradeReason`
- 降级：pay 订单缺失时 `degraded=true` 且 `degradeReason=PAY_ORDER_NOT_FOUND`。

### 2.2 `GET /trade/after-sale/refund-progress`
- 入参：`afterSaleId`(optional), `orderId`(optional)，至少一项非空
- 主要返回：
  - `afterSaleId/afterSaleNo/orderId/orderNo`
  - `afterSaleStatus/afterSaleStatusName/refundPrice/payRefundId`
  - `payRefundStatus/payRefundStatusName/merchantOrderId/merchantRefundId`
  - `refundTime/progressCode/progressDesc/channelErrorCode/channelErrorMsg`
- 进度码：`REFUND_PENDING|REFUND_PROCESSING|REFUND_SUCCESS|REFUND_FAILED`
- 降级：pay 退款单不可用时按售后状态回退进度。

## 3. 结构化日志约束（本次加固）
- 场景：
  - `booking_refund_refresh_four_account_fail`
  - `four_account_refund_commission_sync_ticket_fail`
  - `four_account_warn_ticket_upsert_fail`
  - `four_account_pass_ticket_resolve_fail`
- 字段：`runId/orderId/payRefundId/sourceBizNo/errorCode`
- 规则：字段缺失触发 `finance-log-validate` 告警，但不阻断主流程（fail-open 保持不变）。

## 4. 与窗口 B 联调注意点
- 新页面可直接改用聚合接口减少端侧拼装：
  - 支付结果页 -> `/trade/order/pay-result`
  - 退款进度页 -> `/trade/after-sale/refund-progress`
- UI 需按 `degraded/degradeReason` 与 `progressCode` 做降级文案显示。
- 旧接口仍可并存调用，避免一次性切换风险。

## 5. 与窗口 C 门禁联动点
- StageB 门禁建议增加以下锚点：
  - `/trade/order/pay-result`
  - `/trade/after-sale/refund-progress`
  - `payResultCode`
  - `progressCode`
  - `finance-audit][scene=four_account_*`
- required check context 名不变：`hxy-ops-stageb-p1-guard / ops-stageb-p1-guard`。
