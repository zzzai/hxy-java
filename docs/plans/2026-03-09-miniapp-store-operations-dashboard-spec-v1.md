# MiniApp Store Operations Dashboard Spec v1 (2026-03-09)

## 1. 目标与范围
- 目标：定义门店经营看板的统一指标体系、口径、阈值和告警责任，确保“可采集、可计算、可处置”。
- 范围：增长、交易、履约、售后、门店经营五层指标。
- 对齐：
  - `docs/plans/2026-03-09-miniapp-event-taxonomy-v2.md`
  - `docs/products/miniapp/2026-03-09-miniapp-feature-inventory-and-release-matrix-v1.md`

## 2. 口径总则
- 指标事件时间统一使用 `eventTime`，不得以落库时间替代。
- 成功类指标只认后端确认（`resultCode=SUCCESS` 或业务台账终态）。
- `degraded=true` 流量必须单独分池（`degraded_pool`），不得计入主成功率、主转化率、ROI。
- 关键检索键：`runId/orderId/payRefundId/sourceBizNo/errorCode`。

## 3. 主池与降级池

| 池类型 | 判定条件 | 可计入指标 | 禁止计入指标 |
|---|---|---|---|
| `main_pool` | `degraded=false` 且关键字段完整 | 主转化率、主成功率、ROI、毛利 | - |
| `degraded_pool` | `degraded=true` 或关键字段缺失但可审计 | 降级率、降级恢复率、降级损耗 | 主成功率、主ROI、北极星指标 |

## 4. 指标分层定义（执行表）

| 层级 | 指标编码 | 指标名称 | 公式 | 口径定义 | 数据源（事件/台账） | 刷新频率 | 阈值 | 告警级别 | Owner |
|---|---|---|---|---|---|---|---|---|---|
| 增长 | `GROWTH_NSM_7D` | 北极星7日有效交易用户 | `count_distinct(userId)` | 7日 `trade_pay_result_view.resultCode=SUCCESS` 且不在退款成功集合 | `trade_pay_result_view` + `trade_refund_progress_view` + `trade_order` | T+1 日刷 | 周环比 < -5% | P1 | 增长负责人 |
| 增长 | `GROWTH_ACTIVATION_RATE` | 激活率 | `trade_order_submit_uv/app_page_view_uv` | 用户首次访问后触发下单意向 | `app_page_view` + `trade_order_submit` | 小时 | < 6% | P2 | 增长运营 |
| 增长 | `GROWTH_COUPON_UPLIFT` | 领券转化提升 | `exp_pay_rate-ctrl_pay_rate` | 同实验窗口，分流用户支付率差 | `marketing_coupon_take` + `trade_pay_result_view` + 实验登记表 | 日刷 | < 0pp | P1 | 实验Owner |
| 交易 | `TRADE_SUBMIT_TO_PAY_RATE` | 下单到支付成功率 | `pay_success_order_cnt/order_submit_cnt` | 只统计 `main_pool` | `trade_order_submit` + `trade_pay_result_view` + `trade_order` | 15分钟 | < 12% | P1 | 交易域Owner |
| 交易 | `TRADE_PAY_DEGRADED_RATE` | 支付降级率 | `trade_pay_result_degraded_cnt/trade_pay_result_view_cnt` | `degraded_pool` 专项 | `trade_pay_result_degraded` + `trade_pay_result_view` | 15分钟 | > 3% | P1 | 交易域Owner |
| 交易 | `TRADE_ERROR_TOPN_RATIO` | 高频错误码占比 | `topN_error_cnt/total_fail_cnt` | 错误码字符串化聚合 | `errorCode` 事件字段 + 错误台账 | 小时 | > 65% | P2 | 数据治理Owner |
| 履约 | `FULFILL_ON_TIME_RATE` | 履约按时率 | `on_time_done_cnt/fulfillment_done_cnt` | `actualCompleteTime<=slaDeadlineTime` | `fulfillment_status_change` + 履约台账 | 30分钟 | < 92% | P1 | 履约Owner |
| 履约 | `FULFILL_EXCEPTION_RATE` | 履约异常率 | `exception_cnt/status_change_cnt` | `exceptionType` 命中记录 | `fulfillment_exception_raise` + `fulfillment_status_change` | 30分钟 | > 5% | P1 | 履约Owner |
| 履约 | `FULFILL_FAKE_SUCCESS_BLOCK_CNT` | 假成功拦截次数 | `count(content_fake_success_block)` | 后端未确认成功前的成功表达拦截 | `content_fake_success_block` + 审核台账 | 日刷 | > 0（关键场景） | P0 | 合规Owner |
| 售后 | `AS_APPLY_RATE` | 售后申请率 | `after_sale_create_cnt/pay_success_order_cnt` | 支付成功订单中的售后申请占比 | `trade_after_sale_create` + `trade_order` | 30分钟 | > 8% | P1 | 售后Owner |
| 售后 | `AS_REFUND_SLA_RATE` | 退款SLA达标率 | `refund_within_sla_cnt/refund_complete_cnt` | 退款完成且在SLA内 | `trade_refund_progress_view` + `trade_after_sale` | 30分钟 | < 90% | P1 | 售后Owner |
| 售后 | `AS_BLOCK_CODE_1011000125` | 套餐履约超限拦截占比 | `code_1011000125_cnt/after_sale_apply_cnt` | 观测业务拦截强度 | `errorCode=1011000125` 事件 + 售后台账 | 日刷 | > 2% | P2 | 售后策略Owner |
| 门店经营 | `STORE_REVENUE_NET` | 门店净收入 | `pay_income-refund_amount` | 仅 `main_pool` 成功收入 | `trade_order` + `trade_after_sale` + 经营台账 | 日刷 | 日环比 < -10% | P1 | 门店经营Owner |
| 门店经营 | `STORE_GROSS_MARGIN` | 门店毛利率 | `gross_profit/revenue_net` | 见经营模型口径 | 经营模型台账 + 券/积分/礼品卡台账 | 日刷 | < 15% | P1 | 财务BP |
| 门店经营 | `STORE_ROI` | 门店ROI | `(incremental_gross_profit-marketing_spend)/marketing_spend` | 实验/投放分组ROI | 实验登记 + 经营台账 + 投放账单 | 日刷 | 连续7天<0 | P0 | 增长+财务 |
| 门店经营 | `STORE_DEGRADED_LOSS` | 降级损耗金额 | `sum(degraded_order_loss)` | 仅 `degraded_pool` 估算损耗 | `degraded` 事件 + 补偿/重放台账 | 日刷 | > 收入1.5% | P1 | 数据治理Owner |

## 5. 告警分级与处置

| 告警级别 | 触发条件 | 响应时限 | 升级路径 |
|---|---|---|---|
| P0 | 支付/履约假成功、ROI连续失真、关键链路不可追踪 | 15 分钟 | 值班Owner -> 技术负责人 -> 业务负责人 |
| P1 | 成功率/履约率/退款SLA超阈值 | 1 小时 | 指标Owner -> 域负责人 |
| P2 | 次要波动、TopN集中、趋势异常 | 4 小时 | 指标Owner |

## 6. 指标到事件与业务对象映射

| 指标层 | 主要事件 | 主要对象 | 追踪主键 |
|---|---|---|---|
| 增长 | `app_page_view`、`trade_order_submit`、`marketing_coupon_take` | `experiment_registry`、`trade_order` | `experimentId+userId+orderId` |
| 交易 | `trade_pay_result_view`、`trade_pay_result_degraded` | `trade_order` | `orderId+errorCode` |
| 履约 | `fulfillment_status_change`、`fulfillment_exception_raise` | `fulfillment_order` | `storeId+orderId+fulfillmentId` |
| 售后 | `trade_after_sale_create`、`trade_refund_progress_view` | `trade_after_sale` | `afterSaleId+payRefundId` |
| 门店经营 | 成功/退款/降级复合事件 | `store_daily_finance_ledger` | `storeId+bizDate` |

## 7. 看板验收标准
1. 五层指标全部可出数，且可下钻到 `storeId/orderId/afterSaleId/payRefundId`。
2. 每个指标都有 Owner、阈值、告警级别与刷新频率。
3. `degraded_pool` 已独立展示，且不影响主成功率与ROI。
4. 任一异常可通过 `runId/orderId/payRefundId/sourceBizNo/errorCode` 追溯。
