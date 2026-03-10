# MiniApp Domain Alert Owner Routing v1 (2026-03-10)

## 1. 目标
- 把 `content / brokerage / catalog / marketing-expansion / reserved-expansion` 的告警 owner、升级链、SLA、人工接管入口统一成单一真值。
- 本文补充 `docs/plans/2026-03-09-miniapp-alert-routing-and-oncall-sla-v1.md`，只收口剩余域和混合 `ACTIVE / PLANNED_RESERVED` 场景。

## 2. 统一规则
- 五键固定为：`runId/orderId/payRefundId/sourceBizNo/errorCode`。
- 不适用字段统一填字符串 `"0"`；禁止留空、`null`、`-`、`UNKNOWN`。
- `degraded=true` 只进入 `degraded_pool` 告警，不触发主成功率 / 主 ROI 的恢复结论。
- `RESERVED_DISABLED` 在关闭态或越权范围返回，统一认定为“误发布”。

## 3. 默认响应 SLA

| 等级 | 首响 SLA | 缓解 SLA | 关闭 SLA | 典型适用 |
|---|---|---|---|---|
| `P0` | 5 分钟 | 15 分钟 | 4 小时 | 首屏不可用、主口径污染、关键五键缺失、越级放量 |
| `P1` | 15 分钟 | 1 小时 | 24 小时 | 成功率/错误率/降级率超阈值、`RESERVED_DISABLED` 误返回、资金链路异常 |
| `P2` | 30 分钟 | 4 小时 | 72 小时 | 趋势预警、局部异常、轻微转化波动 |

## 4. 逐域路由矩阵

| 业务域 | 等级 | 典型触发 | Owner | 升级链 | SLA | 人工接管入口 | 五键补位规则 |
|---|---|---|---|---|---|---|---|
| `content` | `P0` | 首页 DIY 不可用、聊天完全不可发、`content_fake_success_block` 命中 | Content Ops Owner + Product on-call + SRE | 客服一线 -> 客服组长 -> 域 on-call -> SRE/发布负责人 | `5m / 15m / 4h` | `4.2 转人工` + `CONTENT_CS_INCIDENT` 工单 + 切回基础首页模板/上一稳定快照 | `runId=发布/巡检批次或 0`；`orderId/payRefundId/sourceBizNo=0`；`errorCode=实际码或 0` |
| `content` | `P1` | 文章详情异常、FAQ 跳转失效、聊天历史拉取异常、DIY 模块失效 | Content on-call | 客服组长 -> Content on-call -> 发布负责人 | `15m / 1h / 24h` | `4.2 转人工` + 内容运营下线对应文章/FAQ/DIY 模块 | `runId=巡检批次或 0`；`orderId/payRefundId/sourceBizNo=0`；`errorCode=实际码或 0` |
| `content` | `P2` | 单篇文章错版、FAQ 单条失效、聊天延迟 | 值班客服 + 内容运营 | 值班客服 -> 内容运营 | `30m / 4h / 72h` | 客服一线建单并回访；必要时转 L2 人工坐席 | 同上 |
| `brokerage` | `P0` | `1030007011` 冲正幂等冲突、资金异常无法追溯、闭环证据缺失 | 技术 on-call + 财务负责人 | 技术值班 -> 财务负责人 -> 风控负责人 -> 发布负责人 | `5m / 15m / 4h` | `BRO-05` 手工冲正复核 + 冻结相关批次/用户 + `BRO-08` 申诉工单 | `runId=结算/重试批次`；`orderId/payRefundId/sourceBizNo/errorCode` 用真实值，不得回填 `0` 掩盖 |
| `brokerage` | `P1` | 结算成功率下滑、提现失败率 `>2%`、审核超时率 `>5%`、冻结余额异常率 `>0.5%` | 财务值班 / 风控 Owner | 财务值班 -> 财务负责人 -> 发布负责人 | `15m / 1h / 24h` | `BRO-03/BRO-04/BRO-06/BRO-07`；必要时使用 `booking/commission-settlement/notify-outbox-retry` 做人工补发 | `runId=批次 ID 或 0`；`orderId/payRefundId/sourceBizNo=真实值，无值填 0`；`errorCode=实际码或 0` |
| `brokerage` | `P2` | 申诉积压、outbox 堵塞趋势、轻微结算波动 | 客服组长 / 财务系统 Owner | 指标 Owner -> 财务负责人 | `30m / 4h / 72h` | `BRO-08` 用户申诉工单 + 人工回访 | `runId=0` 可接受；资金主键有值必须透传 |
| `catalog` | `P0` | 降级样本混入主口径、`search-lite` 与 `search-canonical` 混算、五键缺失导致不可追责 | Product Domain Owner + 数据治理值班 | Product Owner -> 数据治理值班 -> 发布负责人 | `5m / 15m / 4h` | 冻结发布口径，先切回默认商品列表或只读详情，再修正分池 | `runId=发布/巡检批次或 0`；`orderId/payRefundId/sourceBizNo=0`；`errorCode=0` 或实际码 |
| `catalog` | `P1` | 商品详情错误率超阈值、`1008009902/1008009904` 误返回、search-lite 错误率超阈值 | Product on-call / Search Owner | Search Owner -> Product on-call -> SRE/发布负责人 | `15m / 1h / 24h` | `7. 回滚和降级动作`：关闭 `miniapp.catalog.version-guard` / `miniapp.search.validation`，保留 query 回退默认列表 | `runId=灰度/巡检批次或 0`；`orderId/payRefundId/sourceBizNo=0`；`errorCode=1008009902/1008009904` 或实际码 |
| `catalog` | `P2` | 合法空结果波动、search-lite 转化率下滑、评论/收藏/历史内部联调异常 | Search Owner / Product Domain Owner | 指标 Owner -> 域负责人 | `30m / 4h / 72h` | 暂停评论/收藏/历史入口展示，仅保留已上线 browse/detail | 同上 |
| `marketing-expansion` | `P0` | 页面价与结算价冲突、结算冲突率 `>1%`、库存异常导致超卖、活动越级放量 | 值班经理 + 技术 on-call | 运营值班 -> 值班经理 -> 技术负责人 -> 发布负责人 | `5m / 15m / 4h` | `MKT-07` 兜底关闭 + 恢复上一稳定配置快照 + 同步客服公告 | `runId=活动批次/灰度批次`；`orderId/payRefundId/sourceBizNo=有值透传，无值填 0`；`errorCode=实际码或 0` |
| `marketing-expansion` | `P1` | 秒杀/拼团/砍价/满减送错误率超阈值、库存低于强制降级阈值、投诉 15 分钟爆发 3 倍 | 运营值班 + 商品/产品 on-call | 运营值班 -> 商品/产品 on-call -> 值班经理 | `15m / 1h / 24h` | `MKT-04/MKT-05`：停放量、锁库存、回退配置 | `runId=活动/灰度批次`；`orderId/payRefundId/sourceBizNo` 按订单透传；纯配置告警可填 `0` |
| `marketing-expansion` | `P2` | ROI / 转化趋势预警，但未伤及主链路 | 运营值班 | 运营值班 -> 值班经理 | `30m / 4h / 72h` | 停止继续放量，维持当前阶段，重算 ROI 口径 | `runId=活动批次`；`errorCode=0`；其余无值填 `0` |
| `reserved-expansion` | `P0` | 未完成页面/controller/激活 checklist 却越级改 `ACTIVE` 或直接进灰度 | 发布负责人 + 对应域 Owner | 发布值班 -> 对应域 Owner -> SRE -> 技术/业务负责人 | `5m / 15m / 4h` | `Activation Checklist` 第 3/5/7 节 + capability ledger 回退 + 立即移出发布范围 | `runId=发布/巡检批次`；`orderId/payRefundId/sourceBizNo=0`；`errorCode=0` |
| `reserved-expansion` | `P1` | `RESERVED_DISABLED` 误返回、灰度阶段指标越阈值 | 对应域 on-call + SRE | SRE -> 对应域 on-call -> 发布负责人 | `15m / 1h / 24h` | `Gray Acceptance Runbook` 第 6/8 节：关闭 `miniapp.gift-card` / `miniapp.referral` / `miniapp.technician-feed.audit`，回退上一阶段 | `runId=灰度/巡检批次`；`orderId/payRefundId/sourceBizNo=有值透传，无值填 0`；`errorCode=保留码真实值` |
| `reserved-expansion` | `P2` | 样本量不足、轻微指标波动、局部投诉抬头 | 对应域 Owner | 指标 Owner -> 域负责人 | `30m / 4h / 72h` | 保持当前灰度阶段不放量，继续补样本或人工复核 | `runId=灰度批次`；无业务主键填 `0` |

## 5. 人工接管入口索引

| 业务域 | 固定入口 |
|---|---|
| `content` | `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-sop-v1.md` 第 `4.2`、`6.4` 节 |
| `brokerage` | `docs/plans/2026-03-10-miniapp-brokerage-domain-runbook-v1.md` 的 `BRO-03/BRO-04/BRO-05/BRO-08`，以及 `booking/commission-settlement/notify-outbox-*` |
| `catalog` | `docs/plans/2026-03-10-miniapp-product-catalog-kpi-and-alerting-v1.md` 第 `7` 节 |
| `marketing-expansion` | `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-ops-playbook-v1.md` 的 `MKT-04/MKT-05/MKT-07` |
| `reserved-expansion` | `docs/plans/2026-03-10-miniapp-reserved-expansion-activation-checklist-v1.md` 第 `6/7` 节；`docs/plans/2026-03-10-miniapp-reserved-expansion-gray-acceptance-runbook-v1.md` 第 `6/8/10` 节 |

## 6. 验收标准
1. 五个业务域都能映射到唯一主责、升级链和人工接管入口。
2. 告警工单、发布门禁、回滚记录三处的五键值保持一致。
3. 任一误发布或越级放量都能在 SLA 内回滚并复盘。
