# MiniApp P0 Contract Freeze v1 (2026-03-08)

## 1. 范围与冻结原则
- 分支：`feat/ui-four-account-reconcile-ops`
- 目标：小程序 P0 发布前冻结“页面路由 -> 后端契约 -> 错误码 -> 降级语义”。
- 冻结规则：
  - 仅允许向后兼容扩展字段；禁止删除/重命名已发布字段。
  - 错误码语义冻结，前后端按码处理，不依赖错误文案。
  - 跨域依赖异常统一 fail-open：主链路可用，返回降级标记并记录审计日志。

## 2. 页面契约矩阵（P0）

| 页面路由 | 后端接口清单 | 请求关键字段 | 响应关键字段 | 关键错误码 | 降级语义 | 能力状态 |
|---|---|---|---|---|---|---|
| `/pages/pay/result` 支付结果 | `GET /trade/order/pay-result` | `orderId`, `sync?` | `orderId`, `payOrderId`, `payOrderStatus`, `payResultCode`, `degraded`, `degradeReason` | `ORDER_NOT_FOUND(1011000011)` | pay 单缺失时 `degraded=true` 且 `degradeReason=PAY_ORDER_NOT_FOUND`，不抛 500 | 已有能力 |
| `/pages/order/list` 订单列表 | `GET /trade/order/page`, `GET /trade/order/get-count` | `status?`, `pageNo`, `pageSize` | `list[]`, `status`, `payStatus`, `refundStatus`, `count` | `ORDER_NOT_FOUND(1011000011)` | 查询失败前端显示重试态，不影响其他入口 | 已有能力 |
| `/pages/order/detail` 订单详情 | `GET /trade/order/get-detail` | `id`, `sync?` | `id`, `no`, `status`, `payStatus`, `refundStatus`, `items[]` | `ORDER_NOT_FOUND(1011000011)` | `sync` 失败时保留本地状态返回，不阻断详情展示 | 已有能力 |
| `/pages/after-sale/create` 售后申请 | `POST /trade/after-sale/create` | `orderItemId`, `way`, `type`, `refundPrice`, `applyReason` | `afterSaleId` | `AFTER_SALE_CREATE_FAIL_*`, `AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED(1011000125)` | 审核规则异常走人工复核工单链路，不阻断创建响应 | 已有能力 |
| `/pages/after-sale/list` 售后列表 | `GET /trade/after-sale/page` | `statuses?`, `pageNo`, `pageSize` | `list[]`, `status`, `refundPrice`, `refundLimitSource` | `AFTER_SALE_NOT_FOUND(1011000100)` | 空结果返回空列表，不作为错误 | 已有能力 |
| `/pages/after-sale/detail` 售后详情 | `GET /trade/after-sale/get` | `id` | `id`, `status`, `refundPrice`, `payRefundId`, `refundTime`, `refundLimitSource` | `AFTER_SALE_NOT_FOUND(1011000100)` | pay 侧退款单暂不可查时仍返回售后主信息 | 部分能力 |
| `/pages/refund/progress` 退款进度 | `GET /trade/after-sale/refund-progress` | `afterSaleId?`, `orderId?` | `afterSaleStatus`, `payRefundStatus`, `progressCode`, `channelErrorCode` | `AFTER_SALE_NOT_FOUND(1011000100)` | pay 退款单缺失时按售后状态回退进度，不抛 500 | 已有能力 |
| `/pages/booking/list` 预约列表 | `GET /booking/order/list`, `GET /booking/order/list-by-status` | `status?` | `id`, `orderNo`, `status`, `payPrice`, `payRefundId`, `refundTime` | `BOOKING_ORDER_NOT_EXISTS` | 状态筛选为空时返回全部，不报错 | 已有能力 |
| `/pages/address/list` 地址管理 | `GET/POST/PUT/DELETE /member/address/*` | `id`, `name`, `mobile`, `areaId`, `detailAddress` | `id`, `defaultStatus`, `list[]` | `MEMBER_ADDRESS_*` | 无默认地址时返回 `null`，前端引导新增 | 已有能力 |
| `/pages/coupon/center` 领券中心 | `GET /promotion/coupon-template/page`, `POST /promotion/coupon/take` | `templateId`, `pageNo`, `pageSize` | `template[]`, `takeResult` | `PROMOTION_COUPON_*` | 重复领取返回业务错误码，不抛系统异常 | 已有能力 |
| `/pages/point/mall` 积分商城 | `GET /promotion/point-activity/page`, `GET /member/point/record/page` | `pageNo`, `pageSize` | `activity[]`, `pointRecord[]` | `PRICE_CALCULATE_POINT_TOTAL_LIMIT_COUNT(1011003004)` | 活动页为空时返回空列表，不阻断页面渲染 | 已有能力 |

## 3. 新增聚合接口冻结（本批）

### 3.1 `GET /trade/order/pay-result`
- 入参：
  - `orderId`（必填）
  - `sync`（可选，默认 `false`）
- 出参冻结字段：
  - `orderId/orderNo/payOrderId/orderStatus/orderPayStatus/orderRefundStatus/orderRefundPrice`
  - `payOrderStatus/payOrderStatusName/paySuccessTime/payChannelCode`
  - `payResultCode`（`WAITING|SUCCESS|REFUNDED|CLOSED`）
  - `payResultDesc`
  - `degraded/degradeReason`
- 兼容要求：
  - 未找到订单返回 `null`（保持 app 端查询风格）。
  - pay 订单缺失不抛错，返回 `degraded=true`。

### 3.2 `GET /trade/after-sale/refund-progress`
- 入参：
  - `afterSaleId`（可选）
  - `orderId`（可选）
  - 至少一个非空，优先 `afterSaleId`。
- 出参冻结字段：
  - `afterSaleId/afterSaleNo/orderId/orderNo`
  - `afterSaleStatus/afterSaleStatusName/refundPrice/payRefundId`
  - `payRefundStatus/payRefundStatusName/merchantOrderId/merchantRefundId`
  - `refundTime/progressCode/progressDesc/channelErrorCode/channelErrorMsg`
- 进度码冻结：
  - `REFUND_PENDING|REFUND_PROCESSING|REFUND_SUCCESS|REFUND_FAILED`
- 兼容要求：
  - 未找到售后返回 `null`。
  - pay 退款单不可用时按售后状态回退进度。

## 4. 错误码冻结锚点
- `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT` = `1030004012`
- `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS` = `1030004016`
- `AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED` = `1011000125`
- `ORDER_NOT_FOUND` = `1011000011`
- `TICKET_SYNC_DEGRADED`（warning tag，降级可检索锚点）

## 5. 降级语义冻结
- 回调与重放链路：
  - 幂等冲突返回业务错误码，不做静默成功。
  - 同单同退款单号命中幂等成功。
- 四账联动与工单同步：
  - 下游 trade/工单异常 fail-open，主链路不回滚。
  - 审计日志必须包含 `runId/orderId/payRefundId/sourceBizNo/errorCode`，缺失字段写 `finance-log-validate` 告警。

## 6. 能力缺口（非本批 P0）
- 礼品卡购买/赠送/领取/核销/退款：缺失独立业务域接口（`gift-card`）。
- 邀请有礼闭环：缺失 referral 域接口与到账台账契约。
- 技师动态广场：缺失 feed/post 业务接口。
