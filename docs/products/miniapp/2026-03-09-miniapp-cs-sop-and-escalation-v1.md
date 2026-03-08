# MiniApp 客服处置与升级 SOP v1 (2026-03-09)

## 1. 目标与对齐基线
- 目标：建立可执行的客服分级处置机制，覆盖告警识别、时限响应、升级路径、回访闭环。
- 对齐文档：
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
  - `docs/plans/2026-03-08-miniapp-degrade-retry-playbook-v1.md`
- 处置原则：
  - 按错误码驱动处置，不按错误文案自由发挥。
  - 先保障主链路可用，再做补偿与升级。
  - 降级不等于成功，必须有可追踪审计记录。

## 2. 错误码分级（P0/P1/P2）

| 级别 | 错误码/锚点 | 风险定义 | 默认升级策略 |
|---|---|---|---|
| P0 | `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT(1030004012)` | 退款幂等冲突，存在串单或资金风险 | 立即升级到值班技术负责人和财务值班 |
| P1 | `AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED(1011000125)` | 售后被履约子项阻断，用户退款失败 | 30 分钟内升级到交易域 on-call |
| P2 | `ORDER_NOT_FOUND(1011000011)` | 订单查询缺失，页面需可恢复 | 连续 3 次重试失败后升级 P1 工单 |
| P2 | `AFTER_SALE_NOT_FOUND(1011000100)` | 售后查询缺失，通常可回退列表 | 记录告警并观察趋势 |
| P2 | `BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS(1030004016)` | runId 不存在，批次审计链路中断 | 记录审计告警，必要时转运维排查 |
| P2 | `PRICE_CALCULATE_POINT_TOTAL_LIMIT_COUNT(1011003004)` | 积分规则命中，业务可解释限制 | 无需升级，指导用户调整购买 |
| P2 | `TICKET_SYNC_DEGRADED` | 下游工单系统降级，主链路成功 | 监控告警 + 异步重试，不阻断主流程 |
| P2 | `PAY_ORDER_NOT_FOUND`(degradeReason) | 支付侧聚合缺失，结果页降级 | 监控支付依赖，超过阈值升级 P1 |

## 3. 处理时限（SLA）

| 级别 | 首响时限 | 初步处置时限 | 升级时限 | 闭环时限 |
|---|---:|---:|---:|---:|
| P0 | 5 分钟 | 15 分钟 | 15 分钟内完成升级 | 24 小时内完成用户回访与工单归档 |
| P1 | 15 分钟 | 30 分钟 | 30 分钟内完成升级 | 48 小时内完成处理说明与回访 |
| P2 | 30 分钟 | 2 小时 | 满足触发条件时升级 | 3 个自然日内完成闭环记录 |

## 4. 升级路径
- P0：客服一线(L1) -> 客服组长(L2) -> 交易/预约 on-call -> 值班技术负责人 -> 财务值班。
- P1：客服一线(L1) -> 客服组长(L2) -> 对应业务域 on-call（trade/booking/promotion）。
- P2：客服一线(L1) 自处置；达到升级触发条件后转 P1 路径。

## 5. 可执行动作清单（每条动作可审计）

| 动作ID | 触发条件 | 执行人 | 执行动作 | 审计字段（必须） | 验证标准 |
|---|---|---|---|---|---|
| CS-01 | 命中任一矩阵错误码 | 客服一线(L1) | 记录工单并回传“错误码+页面路由+时间点”；指导用户执行对应恢复动作（重试/回退列表/稍后重试） | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 工单中可检索到 5 个字段；用户拿到明确下一步动作 |
| CS-02 | `1030004012` | 客服组长(L2) | 立即停止用户侧重复重试，锁定同 `orderId/payRefundId` 请求，发起 P0 升级 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 15 分钟内升级完成；无重复冲突写入 |
| CS-03 | `1011000125` | 客服一线(L1) | 引导用户转人工审核通道，补充售后单与订单信息，30 分钟内转 P1 工单 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 工单状态变更为“人工复核中”；用户收到处理时限 |
| CS-04 | `1011000011` 连续 3 次重试失败 | 客服组长(L2) | 按矩阵策略从 P2 升级 P1，通知 trade on-call 排查订单聚合链路 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 升级记录存在；页面无空白崩溃（保活错误态） |
| CS-05 | `1011000100` | 客服一线(L1) | 指导用户回退售后列表并刷新；若列表可用则不升级，仅记录 warning | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 用户可返回售后列表；工单记录“已恢复/无需升级” |
| CS-06 | `1030004016` | 运营值班 + 客服组长 | 校验 runId 是否输入错误；若 run 列表可刷新恢复则记录审计 warning，不阻断主链路 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | run 页可继续访问；告警归档到审计台账 |
| CS-07 | `1011003004` | 客服一线(L1) | 解释积分限购规则，指导调整数量/商品组合，不发起技术升级 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 用户可继续下单或明确放弃；无误升级工单 |
| CS-08 | `TICKET_SYNC_DEGRADED` | 运营值班 | 确认“主链路成功+工单降级”状态，触发后台重试任务并监控恢复 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 主流程成功率不下降；重试任务可追踪 |
| CS-09 | `PAY_ORDER_NOT_FOUND` | 客服一线(L1) + 支付 on-call | 指导用户下拉刷新，最多 3 次；超过阈值升级支付依赖告警 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 页面展示 `WAITING + warning`；无假成功动效 |
| CS-10 | 任一升级单处理完成 | 客服一线(L1) | 执行回访：告知结果、补救动作、复发入口；更新工单为“已闭环” | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 回访记录完整（时间/渠道/结果）；闭环状态可审计 |

## 6. 回访闭环标准
- 必须包含：问题编号、错误码、影响范围、临时措施、长期修复项、回访结果。
- P0/P1 问题需补充“是否再次触发”与“预防措施 owner + 计划日期”。
- 闭环判定：用户确认 + 系统验证通过 + 工单状态更新为 `CLOSED`。

## 7. 降级语义对齐说明
- 严格对齐 `degrade-retry-playbook`：
  - `degraded=true` 场景可用但带 warning，不做失败页。
  - 业务冲突码（如 `1030004012`）不自动重试，直接进入人工/运营流。
  - 自动重试上限 3 次；超过阈值必须升级。
