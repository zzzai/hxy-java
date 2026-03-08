# MiniApp Event Taxonomy v2 (2026-03-09)

## 1. 目标与范围
- 目标：建立可直接实施的埋点分类法，统一事件命名、字段口径、归因规则、风险拦截与运营指标。
- 范围：小程序核心链路（访问、交易、履约、售后、营销、风控与降级）。
- 命名规范：`{domain}_{object}_{action}`，小写下划线；禁止同义多名。

## 2. 事件字典（实施清单）

| 事件名 | 说明 | 必填 | 枚举/取值 | 示例 |
|---|---|---|---|---|
| `app_page_view` | 页面曝光 | 是 | `route` 见路由表 | `route=/pages/order/detail` |
| `trade_order_submit` | 提交订单 | 是 | `orderType=normal/booking` | `orderId=202603090001` |
| `trade_pay_result_view` | 支付结果页曝光 | 是 | `payResultCode=WAITING/SUCCESS/REFUNDED/CLOSED` | `payResultCode=SUCCESS` |
| `trade_pay_result_degraded` | 支付结果降级命中 | 否 | `degradeReason=PAY_ORDER_NOT_FOUND/...` | `degradeReason=PAY_ORDER_NOT_FOUND` |
| `trade_after_sale_create` | 发起售后 | 是 | `afterSaleType=refund/refund_return` | `afterSaleId=8801` |
| `trade_refund_progress_view` | 退款进度曝光 | 是 | `progressCode=REFUND_PENDING/REFUND_PROCESSING/REFUND_SUCCESS/REFUND_FAILED` | `progressCode=REFUND_PROCESSING` |
| `fulfillment_status_update` | 履约状态变更 | 是 | `fulfillmentStatus=WAIT_ACCEPT/IN_SERVICE/DONE/FAIL` | `fulfillmentStatus=DONE` |
| `logistics_track_view` | 物流轨迹曝光 | 否 | `logisticsType=express/self_pickup/service` | `logisticsType=service` |
| `marketing_coupon_take` | 领券行为 | 否 | `result=success/fail` | `result=success` |
| `marketing_points_activity_view` | 积分活动曝光 | 否 | `activityType=points_mall/signin/exchange` | `activityType=points_mall` |
| `risk_compliance_intercept` | 合规拦截命中 | 是 | `riskLevel=P0/P1/P2` | `riskLevel=P0` |
| `risk_fake_success_block` | 假成功动效拦截 | 否 | `scene=pay/coupon/refund` | `scene=pay` |

## 3. 字段字典（统一口径）

| 字段 | 必填 | 类型 | 枚举/规则 | 示例 |
|---|---|---|---|---|
| `eventId` | 是 | string | 全局唯一 UUID | `a3d2...` |
| `eventTime` | 是 | string | ISO8601，UTC+8 | `2026-03-09T14:20:31+08:00` |
| `route` | 是 | string | 对齐 IA 路由表 | `/pages/pay/result` |
| `sessionId` | 是 | string | 会话级唯一 | `sess_92ab` |
| `userId` | 否 | string | 脱敏后上报 | `u_1024` |
| `orderId` | 否 | string | 交易订单主键 | `202603090001` |
| `afterSaleId` | 否 | string | 售后单主键 | `8801` |
| `payRefundId` | 否 | string | 支付退款单主键 | `9300001` |
| `resultCode` | 否 | string | 业务结果码（成功/处理中/失败） | `SUCCESS` |
| `errorCode` | 否 | string | 错误码字符串化 | `1030004012` |
| `degraded` | 否 | bool | 是否降级 | `true` |
| `degradeReason` | 否 | string | 降级原因码 | `PAY_ORDER_NOT_FOUND` |
| `channel` | 是 | string | `miniapp/wechat` | `wechat` |
| `utmSource` | 否 | string | 渠道来源 | `douyin` |
| `riskLevel` | 否 | string | `P0/P1/P2` | `P1` |
| `riskTag` | 否 | string | 风险标签 | `MISLEADING_MARKETING` |

## 4. 口径定义与归因规则
### 4.1 统计口径
- 曝光：事件成功上报且 `eventTime` 在有效窗口内计 1。
- 转化：按“上游曝光 -> 下游行为”漏斗链计算，不跨用户。
- 成功：以服务端确认结果为准，不以前端动画或 toast 为准。

### 4.2 归因规则
- 主归因：`last_non_direct_click`，归因窗口 7 天。
- 辅归因：若无外部来源，归因为 `utmSource=direct`。
- 交易归因键：`userId + orderId`；售后归因键：`userId + afterSaleId`。
- 降级事件必须关联原业务主键（`orderId/afterSaleId/payRefundId` 之一），确保可审计。

## 5. 风险分级与拦截规则

| 风险类型 | 分级 | 命中条件 | 拦截动作 |
|---|---|---|---|
| 合规风险 | P0 | 敏感违规词、监管禁语、未披露限制性条款 | 前端阻断发布/投放，记录 `risk_compliance_intercept` |
| 误导性营销 | P1 | 文案暗示“必得/秒到账/无条件成功”等与事实不符 | 阻断活动上线，要求运营改稿并复审 |
| 假成功动效 | P0 | 后端未确认成功前触发成功动画/庆祝反馈 | 强制降级为处理中态，触发 `risk_fake_success_block` |
| 降级不可追踪 | P1 | `degraded=true` 但缺 `degradeReason` 或主键字段 | 拦截埋点发布，拒绝入库 |

## 6. 与既有文档映射
- `analytics-funnel`：本文件事件集是 funnel 的可执行化版本，保留原 funnel 阶段顺序并补充风险/降级事件。
- `copy-terminology`：字段 `riskTag/resultCode/errorCode` 的文案展示遵循术语规范，禁止同义漂移。
- `motion-accessibility`：成功态相关事件必须受“无假成功动效”约束，弱性能或降级场景仍需文本可见状态。

## 7. 验收与运营监控指标
### 7.1 验收清单
1. 事件名与字段全部通过命名检查，无同义重复。
2. 每个 `degraded=true` 事件均有 `degradeReason`。
3. 支付/退款成功指标仅由服务端确认驱动。
4. 风险拦截事件可按 `riskLevel/riskTag/orderId` 检索。

### 7.2 监控指标（看板）
- `埋点覆盖率`：核心事件日覆盖率 >= 99%。
- `降级命中率`：`trade_pay_result_degraded / trade_pay_result_view`。
- `假成功拦截率`：`risk_fake_success_block` 日趋势与版本对比。
- `错误码可追踪率`：含 `errorCode` 的失败事件占比 >= 98%。
- `归因缺失率`：关键事件缺 `sessionId/channel/route` 占比 < 1%。
