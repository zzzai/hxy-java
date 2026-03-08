# MiniApp Data Quality SLO and Alerting v1 (2026-03-09)

## 1. 目标
- 建立可执行的数据质量 SLO 与告警机制，确保经营、实验、看板指标可依赖。
- 关键检索键统一：`runId/orderId/payRefundId/sourceBizNo/errorCode`。

## 2. DQ 规则总表

| 规则ID | 维度 | 检查逻辑 | 严重级别 | SLO目标 | 检查频率 | 数据源 |
|---|---|---|---|---|---|---|
| `DQ-UNI-01` | 唯一性 | `eventId` 全局唯一，重复率=0 | BLOCK | 100% | 5分钟 | 事件明细表 |
| `DQ-UNI-02` | 唯一性 | `experimentId+userId+bucket` 唯一分桶 | BLOCK | 100% | 15分钟 | 实验分流表 |
| `DQ-REF-01` | 关联完整性 | `trade_pay_result_view.orderId` 必须可关联 `trade_order` | BLOCK | >=99.9% | 5分钟 | 事件 + 订单台账 |
| `DQ-REF-02` | 关联完整性 | `afterSaleId` 可关联 `trade_after_sale` | WARN | >=99.5% | 15分钟 | 事件 + 售后台账 |
| `DQ-REF-03` | 关联完整性 | `payRefundId` 可关联退款对象 | WARN | >=99.5% | 15分钟 | 事件 + 退款台账 |
| `DQ-STA-01` | 状态可达性 | 支付状态迁移合法（WAITING->SUCCESS/REFUND/CLOSED） | BLOCK | 100% | 5分钟 | 支付聚合事件 |
| `DQ-STA-02` | 状态可达性 | 售后进度码与售后状态映射合法 | BLOCK | 100% | 5分钟 | 售后事件 + 售后台账 |
| `DQ-AMT-01` | 金额平衡 | `pay_income - refund_amount = revenue_net` | BLOCK | 偏差<=0.1% | 小时 | 经营台账 |
| `DQ-AMT-02` | 金额平衡 | 券/积分/礼品卡成本分项与总成本一致 | WARN | 偏差<=0.5% | 小时 | 成本台账 |
| `DQ-TMP-01` | 时序合法 | `eventTime <= now+5min` 且不早于业务创建时间 | BLOCK | 100% | 5分钟 | 事件+业务对象 |
| `DQ-TMP-02` | 时序合法 | `refundTime >= payTime` | BLOCK | 100% | 15分钟 | 订单+退款台账 |
| `DQ-IDM-01` | 幂等一致 | `sourceBizNo` 同键重放结果一致 | BLOCK | 100% | 15分钟 | 重放/补偿台账 |
| `DQ-IDM-02` | 幂等一致 | `orderId+payRefundId` 多次回调无冲突漂移 | WARN | >=99.9% | 15分钟 | 回调日志台账 |

## 3. BLOCK/WARN 分级与处置时限

| 等级 | 定义 | 响应SLA | 修复SLA | 升级规则 |
|---|---|---|---|---|
| BLOCK | 影响主口径正确性或导致不可审计 | 15分钟 | 4小时 | 超时自动升级到域负责人+值班经理 |
| WARN | 对趋势分析有影响但不阻断主口径 | 1小时 | 24小时 | 连续3次WARN升级为BLOCK复核 |

## 4. 工单SLA

| 工单类型 | 触发条件 | Owner | 首响SLA | 关闭SLA |
|---|---|---|---|---|
| `DQ_BLOCK_INCIDENT` | 任一 BLOCK 规则触发 | 数据治理值班 | 15分钟 | 4小时 |
| `DQ_WARN_TASK` | WARN 规则触发 | 指标Owner | 1小时 | 24小时 |
| `DQ_RECURRING_DEFECT` | 同规则7天内重复>=3次 | 域负责人 | 30分钟 | 72小时（含根因） |

## 5. 告警策略

| 告警策略 | 条件 | 通知渠道 | 去重窗口 |
|---|---|---|---|
| 实时告警 | BLOCK 即时触发 | IM + 电话 + 工单 | 10分钟 |
| 批量告警 | WARN 聚合每30分钟 | IM + 工单 | 30分钟 |
| 趋势告警 | 规则异常率连续上升3个窗口 | IM | 1小时 |

## 6. 检索与审计标准
- 每条告警必须包含：`runId/orderId/payRefundId/sourceBizNo/errorCode`。
- 缺任一键时自动补 `UNKNOWN` 并记录 `dq_missing_key_warn`。
- 所有修复记录要附“影响指标范围”和“回填策略”。

## 7. 与经营看板和实验治理联动
- 看板联动：
  - BLOCK 期间冻结受影响指标对外展示（展示“数据核验中”）。
- 实验联动：
  - 命中 `DQ-UNI-01`、`DQ-REF-01`、`DQ-TMP-01` 自动暂停实验判定。
  - `degraded_pool` 缺键事件不允许进入 ROI/转化判定。

## 8. 验收标准
1. 所有核心规则都有自动检查、分级、SLA、Owner。
2. 告警可通过五键检索并追踪修复闭环。
3. BLOCK/WARN 处置时限可在工单系统核验。
4. 经营看板、实验平台已接入 DQ 状态信号。
