# MiniApp 运营配置执行 Playbook v1 (2026-03-09)

## 1. 目标与对齐基线
- 目标：沉淀活动运营配置的可执行手册，覆盖上下架、库存阈值、降级开关、灰度策略与回滚。
- 对齐文档：
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
  - `docs/plans/2026-03-08-miniapp-degrade-retry-playbook-v1.md`
- 执行原则：
  - 主链路优先，允许 fail-open 的场景必须有降级标记。
  - 运营动作必须可审计、可回滚、可验证。

## 2. 关键配置面
- 活动配置：活动状态（上架/下架）、生效时间窗、适用门店与用户范围。
- 库存配置：活动库存阈值、告警阈值、售罄策略。
- 降级开关：支付聚合降级、退款进度回退、工单同步降级、券领取超时保护。
- 灰度配置：按门店/用户分组逐步放量，支持一键暂停和回滚。

## 3. 可执行动作清单（每条动作可审计）

| 动作ID | 触发条件 | 执行人 | 执行动作 | 审计字段（必须） | 验证标准 |
|---|---|---|---|---|---|
| OPS-01 | 新活动满足上线条件（素材/库存/时间窗已审） | 运营值班 | 执行活动上架，记录活动批次与生效时间 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 活动页可见且价格/库存一致；无异常码突增 |
| OPS-02 | 活动结束/违规/紧急止损 | 运营值班 + 值班经理 | 执行活动下架并同步通知客服话术 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 活动入口下线生效；历史订单不受影响 |
| OPS-03 | 库存低于阈值（如 <10%） | 运营值班 | 开启库存告警，触发补货或提前结束策略 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 告警可见；库存不会出现负数或超卖 |
| OPS-04 | 库存扣减异常/超卖风险 | 运营值班 + 商品 on-call | 立即下调活动流量并执行临时下架或限购 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 超卖告警停止增长；下单成功率恢复稳定 |
| OPS-05 | 命中 `PAY_ORDER_NOT_FOUND` 且趋势升高 | 支付 on-call + 运营值班 | 启用支付聚合降级策略：结果页显示 `WAITING + warning`，引导手动重试（最多 3 次） | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 无假成功动画；支付结果页可恢复且无空白屏 |
| OPS-06 | 退款进度缺失 pay 退款单 | 交易 on-call | 启用退款进度回退策略：按售后状态兜底展示，前端按 5s*3 重试 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 退款页有可解释状态；不返回 500 崩页 |
| OPS-07 | 命中 `TICKET_SYNC_DEGRADED` | 运维值班 | 保持主链路成功，标记 warning 并触发后台重试任务 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 主交易/退款链路成功率稳定；重试任务可追踪 |
| OPS-08 | 券领取接口超时 | 运营值班 + 前端值班 | 开启“领取成功动效保护”：未收到后端确认不得展示成功 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 不出现“假成功”；用户可幂等重试 |
| OPS-09 | 类目切换出现价格/库存陈旧 | 商品 on-call | 启用“目录强制刷新”策略，触发版本不一致即全量刷新 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 切类目后价格库存一致；陈旧数据消失 |
| OPS-10 | 新功能上线窗口 | 运营值班 + 技术值班 | 按灰度计划放量：5% -> 20% -> 50% -> 100%，每阶段观察 30 分钟 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 每阶段指标达标后再推进；异常立即暂停 |
| OPS-11 | 灰度期间出现 P0/P1 告警 | 值班经理 | 立即冻结放量，维持当前比例并进入故障处置流程 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 5 分钟内停止放量；异常曲线收敛 |
| OPS-12 | 触发回滚条件（错误率阈值超标/核心漏斗下跌） | 值班经理 + 运维值班 | 执行回滚：关闭新增开关 -> 恢复上一稳定配置 -> 重放必要补偿任务 -> 通知客服模板更新 | `runId/orderId/payRefundId/sourceBizNo/errorCode` | 15 分钟内回到稳定版本；核心指标恢复基线区间 |

## 4. 灰度策略
- 灰度分组优先级：内部账号 -> 白名单门店 -> 低风险门店 -> 全量。
- 阶段门槛：
  - 错误码 `1030004012` 不得新增。
  - `TICKET_SYNC_DEGRADED` 允许出现但必须可重试恢复。
  - 交易主链路成功率不低于基线。
- 任一阶段失败，执行 `OPS-12` 回滚。

## 5. 回滚步骤（标准化）
1. 冻结变更：停止灰度推进与活动新配置发布。
2. 切回稳定配置：恢复上一版本开关与活动状态。
3. 补偿执行：对降级期间积压任务执行重试（如工单同步/状态刷新）。
4. 验证恢复：检查错误码趋势、关键链路成功率、客服工单量。
5. 发布公告：同步客服与运营话术，标注恢复时间与影响范围。

## 6. 验证标准（统一）
- 所有动作均能在审计系统检索到 `runId/orderId/payRefundId/sourceBizNo/errorCode`。
- 降级行为严格对齐 `degrade-retry-playbook`：
  - 主链路优先（fail-open where allowed）
  - 自动重试有界（最多 3 次）
  - 冲突码不自动重试，转人工/运营处置。
- 错误码解释严格对齐 `miniapp-errorcode-recovery-matrix`，不得新增未定义解释口径。
