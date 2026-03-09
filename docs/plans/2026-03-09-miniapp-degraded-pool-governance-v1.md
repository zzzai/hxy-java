# MiniApp Degraded Pool Governance v1 (2026-03-09)

## 1. 目标
- 把降级流量从主经营口径中解耦，形成“可观测、可处置、可复盘”的独立治理机制。
- 对齐文档：
  - `docs/plans/2026-03-09-miniapp-store-operations-dashboard-spec-v1.md`
  - `docs/plans/2026-03-09-miniapp-event-taxonomy-v2.md`
  - `docs/plans/2026-03-09-miniapp-data-quality-slo-and-alerting-v1.md`

## 2. degraded_pool 口径
- `degraded_pool` 定义：满足以下任一条件的流量记录。
  1. 事件字段 `degraded=true`。
  2. 关键业务对象关联缺失（`orderId/afterSaleId/payRefundId` 缺失）但仍返回业务可继续。
  3. 下游 fail-open 场景命中（例如支付聚合缺 pay 单、工单同步降级）。
- `main_pool` 定义：`degraded=false` 且关键字段完整、状态可达、金额可平衡。

## 3. 入池规则（Inflow Rules）

| 规则ID | 条件 | 入池动作 | 审计字段 |
|---|---|---|---|
| `DP-IN-01` | `degraded=true` | 直接入 `degraded_pool` | `runId/orderId/errorCode/degradeReason` |
| `DP-IN-02` | 关键对象键缺失但返回成功码 | 入池并标记 `MISSING_KEY_DEGRADE` | `sourceBizNo/orderId/payRefundId` |
| `DP-IN-03` | 下游调用超时/异常触发 fail-open | 入池并标记 `DOWNSTREAM_FAIL_OPEN` | `runId/errorCode/sourceBizNo` |
| `DP-IN-04` | DQ 规则 WARN 且影响主口径可信度 | 临时入池 `DQ_WARN_DEGRADE` | `runId/errorCode` |

## 4. 出池规则（Outflow Rules）

| 规则ID | 出池条件 | 出池动作 | 验证要求 |
|---|---|---|---|
| `DP-OUT-01` | 同主键重试后 `degraded=false` 且字段完整 | 迁回 `main_pool` | 连续2个窗口稳定 |
| `DP-OUT-02` | 关联对象补齐并通过 DQ 检查 | 迁回 `main_pool` | `DQ-REF-*` 全部通过 |
| `DP-OUT-03` | 状态与金额校验恢复合法 | 迁回 `main_pool` | `DQ-STA-*`/`DQ-AMT-*` 通过 |
| `DP-OUT-04` | 超过最大保留期（7天）仍未恢复 | 转入异常归档池 | 人工工单复盘必填 |

## 5. 指标排除规则（强约束）
- `degraded_pool` 流量不得计入以下主口径：
  - 主成功率（支付成功率、履约按时率、退款SLA达标率）
  - 主ROI（门店ROI、实验ROI）
  - 北极星指标（7日有效交易用户）
- `degraded_pool` 只计入：
  - 降级率、恢复率、降级损耗、降级停留时长、降级错误码TopN

## 6. 治理指标

| 指标 | 公式 | 目标阈值 | 告警级别 |
|---|---|---|---|
| `degraded_rate` | `degraded_pool_cnt/total_cnt` | <= 3% | P1 |
| `degraded_recover_rate_24h` | `24h内出池数/24h内入池数` | >= 80% | P1 |
| `degraded_avg_stay_minutes` | `sum(出池时间-入池时间)/出池数` | <= 60 min | P2 |
| `degraded_loss_ratio` | `degraded_loss_amount/revenue_net` | <= 1.5% | P1 |
| `degraded_top1_error_ratio` | `top1_error_cnt/degraded_fail_cnt` | <= 40% | P2 |

## 7. 告警与处置

| 触发条件 | 告警 | 处置SLA | Owner |
|---|---|---|---|
| `degraded_rate > 3%` 连续2窗口 | P1 | 1小时 | 交易域Owner |
| `degraded_loss_ratio > 1.5%` 日级 | P1 | 1小时 | 经营Owner |
| 关键交易页出现 `risk_fake_success_block` | P0 | 15分钟 | 合规+技术值班 |
| 入池记录缺审计字段 | P0 | 15分钟 | 数据治理值班 |

## 8. 复盘模板（降级专项）
1. 入池来源分布（按规则ID）。
2. 高频错误码 TopN 与影响范围。
3. 恢复路径（自动恢复/人工修复/版本回滚）。
4. 损耗评估（收入/转化/工单影响）。
5. 防复发动作（规则、代码、SOP、门禁）。

## 9. 验收标准
1. 入池/出池规则可自动执行并可审计。
2. 主口径看板已排除降级池流量。
3. 降级池可按 `runId/orderId/payRefundId/sourceBizNo/errorCode` 检索。
4. 每次 P0/P1 降级事件都有工单和复盘记录。
