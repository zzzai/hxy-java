# MiniApp Experiment Registry and Governance v1 (2026-03-09)

## 1. 目标
- 统一实验登记、准入、运行、停机、回滚、复盘流程，避免“无样本约束、无止损、无归因”的实验风险。
- 对齐：
  - `docs/plans/2026-03-09-miniapp-event-taxonomy-v2.md`
  - `docs/products/miniapp/2026-03-09-miniapp-feature-inventory-and-release-matrix-v1.md`

## 2. 实验登记模板（强制字段）

| 字段 | 必填 | 说明 | 示例 |
|---|---|---|---|
| `experimentId` | 是 | 全局唯一，格式 `EXP-<域>-<序号>` | `EXP-GRW-05` |
| `experimentName` | 是 | 实验名称 | `支付结果页重试引导v2` |
| `owner` | 是 | 责任人 | `growth_ops_01` |
| `scope` | 是 | 页面/功能范围 | `/pages/pay/result` |
| `hypothesis` | 是 | 业务假设 | `优化提示可提升支付完成率` |
| `primaryMetric` | 是 | 主指标编码 | `TRADE_SUBMIT_TO_PAY_RATE` |
| `guardMetrics` | 是 | 守护指标列表 | `TRADE_PAY_DEGRADED_RATE,AS_APPLY_RATE` |
| `sampleUnit` | 是 | 样本单位 | `userId` |
| `sampleTarget` | 是 | 每组目标样本量 | `12000` |
| `trafficPlan` | 是 | 分流比例 | `A:50/B:50` |
| `startTime/endTime` | 是 | 运行窗口 | `2026-03-10~2026-03-24` |
| `rollbackGate` | 是 | 回滚闸门 | `pay_success_drop>=5%` |
| `relatedOrderId` | 是 | 关联订单键策略 | `orderId` |
| `relatedAfterSaleId` | 是 | 关联售后键策略 | `afterSaleId` |
| `relatedPayRefundId` | 是 | 关联退款键策略 | `payRefundId` |
| `relatedStoreId` | 是 | 关联门店键策略 | `storeId` |
| `status` | 是 | `draft/running/stopped/completed` | `running` |

> 约束：实验数据必须可关联 `orderId/afterSaleId/payRefundId/storeId` 四类对象（可为空但须有字段）。

## 3. 准入门槛

| 准入项 | 门槛 | 不满足处理 |
|---|---|---|
| 事件可追踪 | 主指标和守护指标均有事件+台账映射 | 禁止上线 |
| 样本量 | `n_per_group` 达到最小样本量 | 延后上线 |
| 风险评估 | 合规评审通过（无 P0 风险） | 退回重审 |
| 降级策略 | 已定义 `degraded_pool` 统计和止损 | 禁止上线 |
| 回滚预案 | 明确回滚触发和执行Owner | 禁止上线 |

## 4. 样本量与统计口径
- 显著性水平：`alpha=0.05`，效能 `power=0.8`。
- 二项指标样本量：
  - `n_per_group = 2 * (Zα/2 + Zβ)^2 * p*(1-p) / MDE^2`
- 默认参数：
  - 支付类实验 `MDE=1.0pp`
  - 售后/履约体验类 `MDE=0.5pp`
- 去重规则：
  - 用户级实验：`userId` 去重；匿名流量 `sessionId` 兜底。
  - 订单级实验：`orderId` 去重。

## 5. 停机线（Stop Line）

| 规则ID | 触发条件 | 等级 | 动作 |
|---|---|---|---|
| `STOP-P0-FAKE-SUCCESS` | `risk_fake_success_block > 0`（关键交易页） | P0 | 立即停机 + 回滚 |
| `STOP-P0-ROI` | `STORE_ROI < 0` 且连续7天恶化 | P0 | 停机并冻结预算 |
| `STOP-P1-CONVERSION` | 主指标相对对照组下降 >=5% 连续2天 | P1 | 停机复核 |
| `STOP-P1-DEGRADE` | `degraded_rate > 3%` 连续2小时 | P1 | 降低流量并修复 |
| `STOP-P2-DQ` | 关键字段缺失率 >2% | P2 | 暂停判定，修复埋点 |

## 6. 回滚条件与执行

| 场景 | 回滚条件 | 回滚动作 | 完成SLA |
|---|---|---|---|
| 功能风险 | 命中 P0 停机线 | 立即切回对照策略 | 15 分钟 |
| 数据风险 | DQ 规则 BLOCK | 冻结实验判定，保留样本 | 30 分钟 |
| 经营风险 | ROI、毛利连续下滑超阈值 | 关闭实验流量，停投 | 1 小时 |

## 7. 实验运行治理流程
1. `登记`：创建实验记录并完成四键关联定义。
2. `评审`：增长、交易、合规、数据四方评审。
3. `上线`：按固定哈希分桶，记录版本号。
4. `监控`：主指标、守护指标、降级池实时监控。
5. `停机/回滚`：命中停机线自动告警，人工确认执行。
6. `复盘`：按模板产出结论并更新实验库。

## 8. 复盘模板（标准）

| 模块 | 必填内容 |
|---|---|
| 背景 | 假设、目标、范围、实验ID |
| 数据 | 样本规模、主守护指标、显著性 |
| 结果 | 胜出/持平/失败，增益或损失 |
| 风险 | 错误码TopN、降级率、异常工单 |
| 经营影响 | 对收入/成本/毛利/ROI 的变化 |
| 决策 | 推全量/继续试验/停机回滚 |
| 追踪 | `runId/orderId/payRefundId/sourceBizNo/errorCode` 检索样例 |

## 9. 与事件和对象映射

| 治理对象 | 事件字段 | 业务对象字段 | 检索键 |
|---|---|---|---|
| 实验曝光 | `experimentId,eventId,eventTime` | `experiment_registry` | `experimentId+userId` |
| 交易转化 | `resultCode,errorCode,degraded` | `trade_order` | `experimentId+orderId` |
| 售后影响 | `afterSaleId,progressCode` | `trade_after_sale` | `experimentId+afterSaleId` |
| 退款影响 | `payRefundId` | `pay_refund/trade_after_sale` | `experimentId+payRefundId` |
| 门店影响 | `storeId` | `store_daily_finance_ledger` | `experimentId+storeId` |

## 10. 验收标准
1. 所有实验记录都能关联四类主对象键（`orderId/afterSaleId/payRefundId/storeId`）。
2. 停机线、回滚条件、Owner、SLA 明确且可执行。
3. 实验结论可复现并可追溯到具体事件和台账。
4. `degraded_pool` 单独统计，不进入主转化和主ROI。
