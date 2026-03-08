# MiniApp Growth KPI and Experiment Plan v1 (2026-03-09)

## 1. 目标与边界
- 目标：构建“可实验、可判定、可止损”的增长指标体系，支撑 RB1-P0 至 RB2-P1 迭代。
- 对齐文档：
  - `docs/plans/2026-03-09-miniapp-event-taxonomy-v2.md`
  - `docs/products/miniapp/2026-03-09-miniapp-feature-inventory-and-release-matrix-v1.md`
- 统计原则：
  - 成功指标只认后端结果（`resultCode`/业务台账），不认前端动效。
  - 降级流量单独统计（`degraded=true`），不得混入成功口径。

## 2. 指标树（北极星/一级/二级）

### 2.1 北极星指标（NSM）
- `NSM = 有效交易完成用户数（7日）`
- 定义：7 日内出现 `trade_pay_result_view.resultCode=SUCCESS` 且无退款完成（`trade_refund_progress_view.progressCode=REFUND_SUCCESS`）的去重用户数。

### 2.2 一级指标
1. `AARRR-激活`：新访客下单意向率。
2. `转化`：下单到支付成功转化率。
3. `履约体验`：履约按时完成率。
4. `售后健康`：售后申请率、退款完成 SLA。
5. `增长效率`：券/积分驱动转化 uplift。

### 2.3 二级指标（可直接进看板）
- 激活：`trade_order_submit_uv / app_page_view_uv`。
- 支付成功率：`pay_success_order_cnt / order_submit_cnt`。
- 支付降级率：`trade_pay_result_degraded_cnt / trade_pay_result_view_cnt`。
- 履约按时率：`fulfillment_on_time_cnt / fulfillment_done_cnt`。
- 售后申请率：`trade_after_sale_create_cnt / pay_success_order_cnt`。
- 退款 SLA 达标率：`refund_complete_within_sla_cnt / refund_complete_cnt`。
- 领券转化 uplift：`coupon_exposed_group_pay_rate - control_pay_rate`。

## 3. 指标到事件/业务对象映射

| 指标 | 事件映射 | 台账/对象映射 | 主键追踪 |
|---|---|---|---|
| NSM | `trade_pay_result_view(resultCode=SUCCESS)`、`trade_refund_progress_view` | `trade_order`、`trade_after_sale` | `userId + orderId` |
| 下单意向率 | `app_page_view`、`trade_order_submit` | `trade_order` | `sessionId + userId` |
| 支付成功率 | `trade_order_submit`、`trade_pay_result_view` | `trade_order.pay_status/pay_price` | `orderId` |
| 支付降级率 | `trade_pay_result_degraded` | `trade_order` + pay 聚合响应 | `orderId + degradeReason` |
| 履约按时率 | `fulfillment_status_change`、`fulfillment_sla_breach` | `fulfillment` 业务单（按 `fulfillmentId`） | `orderId + fulfillmentId` |
| 售后申请率 | `trade_after_sale_create` | `trade_after_sale` | `afterSaleId + orderId` |
| 退款 SLA 达标率 | `trade_refund_progress_view` | `trade_after_sale.refund_time` | `afterSaleId + payRefundId` |
| 券转化 uplift | `marketing_coupon_take`、`trade_pay_result_view` | `coupon ledger + trade_order` | `userId + orderId + experimentId` |

## 4. 实验框架

### 4.1 实验清单与假设

| 实验ID | 目标页面/能力 | 假设 | 主要指标 | 守护指标 |
|---|---|---|---|---|
| `EXP-GRW-01` | 支付结果页（RB1-P0） | 明确“处理中+重试引导”可提高最终支付成功率 | 支付成功率 | 支付降级率、投诉率 |
| `EXP-GRW-02` | 领券中心（RB1-P0） | 券门槛文案重写可提升领券后支付转化 | 券后支付率 uplift | 券成本率、退款率 |
| `EXP-GRW-03` | 履约进度页（RB2-P1） | ETA 与状态可视化可降低售后申请率 | 售后申请率下降 | 履约超时率、客服触达率 |
| `EXP-GRW-04` | 退款进度页（RB1-P0） | 清晰进度文案可降低重复查询与投诉 | 退款页面重复访问率下降 | 退款完成 SLA |

### 4.2 样本口径
- 实验单位：`userId`（匿名用户用 `sessionId` 兜底）。
- 分流规则：`hash(userId)%100` 固定桶分流，A/B 不可跨桶。
- 去重规则：每用户每实验只计首次暴露（`first_exposure`）。
- 排除样本：
  - 命中 `risk_compliance_intercept` 且 `riskLevel=P0`。
  - 主链路降级无主键（`degraded=true` 且缺 `orderId/afterSaleId`）。

### 4.3 样本量与统计阈值
- 显著性：`alpha=0.05`，检验效能 `power=0.8`。
- 最小可检测提升（MDE）：
  - 支付转化类：`+1.0pp`。
  - 体验类（售后率/投诉率）：`-0.5pp`。
- 二项指标样本量公式：
  - `n_per_group = 2 * (Zα/2 + Zβ)^2 * p*(1-p) / MDE^2`
- 示例：基线支付成功率 `p=8%`，`MDE=1pp`，则每组约 `11.5k` 用户。

### 4.4 判定阈值
- 胜出：主要指标达到 MDE 且 `p-value < 0.05`，守护指标无显著劣化。
- 持平：主要指标未达 MDE 或置信区间跨 0。
- 失败：主要指标显著下降或守护指标超阈值。

### 4.5 止损规则
- 即时止损（任一命中即停）：
  - 支付成功率相对对照组下降 `>=5%`（连续 2 天）。
  - `risk_fake_success_block` 日增幅 `>=30%`。
  - 退款投诉率提升 `>=20%`。
- 预算止损：券/积分实验的增量成本超过预算上限 `110%` 自动停实验。

## 5. 风险分级与拦截规则（经营视角）

| 风险 | 分级 | 条件 | 处理 |
|---|---|---|---|
| 合规风险 | P0 | 试验文案命中禁语或误导条款 | 立即下线实验并冻结发布 |
| 误导营销 | P1 | 宣传成功率与真实成功率偏差 > 5pp | 暂停实验并更正文案 |
| 假成功动效 | P0 | 后端未成功前展示成功态 | 全量回滚并标记版本不可发布 |
| 指标漂移 | P1 | 事件字段缺失率 > 2% | 停止判定，先修埋点 |

## 6. 运营监控与验收
### 6.1 验收标准
1. 每个 KPI 可追溯到“事件 + 台账 + 对象主键”。
2. 每个实验有明确样本口径、判定阈值、止损规则。
3. 看板可分 `route/experimentId/degraded/errorCode` 维度。
4. 降级和错误事件不会计入成功指标。

### 6.2 运营看板指标
- `NSM 周趋势`、`一级指标漏斗`、`实验 uplift 排行`。
- `degraded rate`、`errorCode TopN`、`riskTag TopN`。
- `实验止损触发次数` 与 `平均恢复时长`。

## 7. 版本发布与回溯要求
- 每次实验发布需记录：`experimentId/version/startTime/endTime/owner`。
- 回溯口径固定按事件时间 `eventTime`，禁止按落库时间替代。
- 审计查询最小键集：`experimentId + userId/sessionId + orderId + eventId`。
