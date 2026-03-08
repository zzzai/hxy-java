# MiniApp Commercial Model and Unit Economics v1 (2026-03-09)

## 1. 目标与口径
- 目标：建立单店经营模型，统一收入、成本、毛利、ROI 的可核算口径。
- 对齐文档：
  - `docs/plans/2026-03-09-miniapp-event-taxonomy-v2.md`
  - `docs/products/miniapp/2026-03-09-miniapp-feature-inventory-and-release-matrix-v1.md`
- 核心对象：`storeId`、`orderId`、`afterSaleId`、`payRefundId`、`campaignId`。

## 2. 收入结构

| 收入项 | 定义 | 口径公式 | 数据来源 |
|---|---|---|---|
| 商品/服务实收 | 订单支付成功后的实收 | `sum(payPrice)` | `trade_order.pay_price` + `trade_pay_result_view` |
| 履约附加收入 | 配送费/服务附加费 | `sum(deliveryPrice + serviceFee)` | `trade_order.delivery_price`、履约台账 |
| 礼品卡沉淀收益 | 期内未兑付余额收益（审慎） | `giftCardSold - giftCardRedeemed - giftCardRefunded` | 礼品卡台账（对象键 `giftCardId/orderId`） |
| 其他营销返佣 | 平台合作返佣收入 | `sum(rebateAmount)` | 营销结算台账 |

## 3. 成本结构（重点）

### 3.1 券成本
- 定义：用户核销优惠券导致的让利成本。
- 公式：`couponCost = sum(couponFaceValueUsed)`。
- 事件与对象映射：`marketing_coupon_take` -> `orderId/userId/campaignId`。

### 3.2 积分成本
- 定义：积分抵扣与积分兑换产生的财务成本。
- 公式：`pointCost = sum(pointUsed * pointCostPerUnit)`。
- 建议参数：`pointCostPerUnit` 按财务月度口径维护（默认 0.01 元/积分）。

### 3.3 礼品卡成本
- 定义：礼品卡赠送补贴 + 兑换成本 + 退款成本。
- 公式：`giftCardCost = subsidyCost + redeemCost + giftCardRefundCost`。

### 3.4 获客成本（CAC）
- 定义：投放、渠道、活动、内容生产带来的新增付费用户成本。
- 公式：`CAC = acquisitionSpend / newPayingUsers`。
- `newPayingUsers` 定义：周期内首次出现 `trade_pay_result_view.resultCode=SUCCESS` 的去重用户。

## 4. 毛利口径与 ROI 公式

### 4.1 单店毛利
- `GrossProfit = RevenueNet - VariableCost`
- `RevenueNet = payIncome + deliveryServiceIncome + otherIncome - refundAmount`
- `VariableCost = couponCost + pointCost + giftCardCost + paymentChannelFee + fulfillmentVariableCost`
- `GrossMargin = GrossProfit / RevenueNet`

### 4.2 单店投放 ROI
- `ROI = (IncrementalGrossProfit - MarketingSpend) / MarketingSpend`
- 增量毛利定义：
  - `IncrementalGrossProfit = ExperimentGroupGrossProfit - ControlGroupGrossProfit`

### 4.3 回本周期
- `PaybackDays = MarketingSpend / DailyIncrementalGrossProfit`

## 5. 与埋点/台账字段映射（可追溯）

| 经营指标 | 埋点字段 | 台账字段 | 主键 |
|---|---|---|---|
| 实收收入 | `trade_pay_result_view.resultCode` | `trade_order.pay_price/pay_status` | `orderId + storeId` |
| 退款金额 | `trade_refund_progress_view.progressCode` | `trade_after_sale.refund_price/refund_time` | `afterSaleId + payRefundId` |
| 券成本 | `marketing_coupon_take` | `coupon ledger.used_amount` | `orderId + campaignId` |
| 积分成本 | `marketing_points_activity_view` | `member_point_record.change_amount` | `userId + orderId` |
| 礼品卡成本 | `app_page_view(route=/gift-card/*)` | `gift_card_ledger.subsidy/redeem/refund` | `giftCardId + orderId` |
| 降级损耗 | `degraded=true,degradeReason` | `hxy_booking_refund_notify_log`、`hxy_booking_refund_replay_run_log` | `orderId + runId` |
| 经营风险 | `risk_compliance_intercept/risk_fake_success_block` | 内容/审核台账 | `contentId + route` |

## 6. 敏感性分析（单店）

### 6.1 关键参数
- `AOV`（客单价）
- `payConversion`（支付转化）
- `couponRate`（券使用率）
- `refundRate`（退款率）
- `CAC`

### 6.2 场景分析（示例）

| 场景 | AOV | 支付转化率 | 券成本率 | 退款率 | CAC | 结论 |
|---|---:|---:|---:|---:|---:|---|
| 乐观 | +8% | +1.5pp | -0.8pp | -0.3pp | -10% | ROI 显著提升，建议加预算 |
| 基线 | 0 | 0 | 0 | 0 | 0 | 维持预算，优化履约效率 |
| 悲观 | -5% | -1.0pp | +1.2pp | +0.6pp | +15% | 触发止损，压缩投放与补贴 |

### 6.3 止损阈值（经营）
- 单店 `GrossMargin` 连续 7 天低于 `15%`：进入红线复盘。
- 单店 `ROI < 0` 且连续 14 天：暂停增长投放。
- `couponCostRate > 12%` 或 `refundRate > 8%`：降档活动强度。

## 7. 风险分级与拦截规则

| 风险 | 分级 | 触发条件 | 拦截动作 |
|---|---|---|---|
| 合规风险 | P0 | 内容/营销触发监管风险词或违规承诺 | 立即停投并冻结活动资产 |
| 误导性营销 | P1 | 宣传优惠力度与实际核销差异超阈值 | 下线文案并强制复审 |
| 假成功动效 | P0 | 支付/退款/履约未成功却展示成功 | 回滚版本，相关收入不计入经营转化 |
| 口径漂移 | P1 | 同指标在事件与台账偏差 > 2% | 停止对外报表，先做数据修复 |

## 8. 验收与运营监控指标
### 8.1 验收清单
1. 每个经营指标都有可追溯事件和台账字段。
2. 单店模型可输出：收入、成本、毛利、ROI、回本周期。
3. 模型支持按 `storeId/campaignId/experimentId` 切片。
4. 降级/错误事件可回溯到 `orderId/payRefundId/runId`。

### 8.2 运营看板指标
- 单店：`RevenueNet/GrossProfit/GrossMargin/ROI/PaybackDays`。
- 成本：`couponCostRate/pointCostRate/giftCardCostRate/CAC`。
- 风险：`riskLevel` 分布、`degraded rate`、`errorCode TopN`。
- 健康：`payConversion/refundRate/fulfillmentOnTimeRate`。

## 9. 实施建议
- 周期：日看板 + 周复盘 + 月结算。
- 对账：经营看板与财务台账每周一次差异对账，偏差阈值 `<=1%`。
- 发布门禁：新增实验/活动上线前必须提交“成本上限 + 止损阈值 + 口径映射”。
