# Admin Trade Ops After-sale / Review Ticket Runbook v1 (2026-03-24)

## 1. 目标与范围
- 为售后单管理、人工复核工单与 SLA 路由规则提供后台标准运行指引。
- 范围只覆盖后台交易运营动作，不覆盖用户端体验结论与发布放量审批。

## 2. 操作入口

| 能力 | 页面入口 | 核心操作 |
|---|---|---|
| `ADM-014` 售后单管理 / 详情 | `mall/trade/afterSale/index`; `mall/trade/afterSale/detail/index` | 查询售后单、查看详情、同意 / 拒绝 / 收货 / 拒收 / 退款 |
| `ADM-015` 人工复核工单 | `mall/trade/reviewTicket/index` | 查看工单池、单条收口、批量收口、通知补偿重试 |
| `ADM-016` SLA 路由规则 | `mall/trade/reviewTicketRoute/index` | 创建 / 编辑 / 删除规则、批量启停、查看启用列表、解析路由 |

## 3. 审计键最小集
- `afterSaleId`
- `reviewTicketId`
- `routeId`
- `orderId`
- `sourceBizNo`
- `resolveActionCode`
- `notifyOutboxId`
- `lastActionCode`
- `operator`

## 4. 标准操作顺序
1. 售后动作前先看详情页，确认退款金额、退款审计状态和异常类型。
2. 人工复核工单优先做单条详情确认，再做单条或批量收口。
3. 批量收口必须记录 `ids[] + resolveActionCode + resolveBizNo`。
4. 通知补偿只在工单动作已明确后执行，补偿后回读 outbox 页确认结果。
5. 路由规则变更前先查看当前启用规则与 `resolve` 结果，再做创建或启停。

## 5. 失败处理
- 售后动作返回成功但详情状态未更新时，按“受理成功、状态回读待核”处理。
- 工单批量收口出现 `skippedNotPendingCount` 或 `skippedNotFoundCount` 时，不写“批量已全部成功”。
- 路由规则批量启停后若 `list-enabled` 与预期不一致，暂停继续改动。

## 6. 回滚 / 暂停规则
- 售后动作异常时，暂停同类批量操作，只保留单条处理。
- 工单通知补偿连续失败时，暂停自动扩大补偿范围，转人工逐条核实。
- 路由规则误改时，以最近一次有效规则快照回滚，并重新验证 `resolve`。

## 7. 当前结论
- Trade Ops 域已具备独立后台 runbook。
- 当前仍只能写成后台治理与运维可用，不能写成 release-ready。
