# Admin Finance Ops Core Runbook v1 (2026-03-24)

## 1. 目标与范围
- 目标：为 `BO-001` ~ `BO-003` 提供后台运营执行顺序、审计键和失败处理规则。
- 范围只覆盖后台 admin 操作，不覆盖 app 用户端，不覆盖发布审批，不覆盖生产放量结论。
- 当前固定阶段：`ACTIVE_ADMIN / 可维护 / 可联调 / 仍需独立 release evidence`。

## 2. 操作入口

| 能力 | 页面入口 | 核心动作 |
|---|---|---|
| `BO-001` 四账对账 | `mall/booking/fourAccountReconcile/index` | 查看台账、查看详情、触发 `run`、看汇总、同步退款审计工单 |
| `BO-002` 退款回调日志 / 重放 | `mall/booking/refundNotifyLog/index` | 查看失败日志、单条或批量重放、查看运行日志、同步重放工单 |
| `BO-003` 技师提成结算 / 通知补偿 | `mall/booking/commission-settlement/index`; `mall/booking/commission-settlement/outbox/index` | 创建结算单、提交、审批、驳回、打款、查看出站补偿并重试 |

## 3. 审计键最小集

| 场景 | 审计键 |
|---|---|
| 四账对账 | `reconcileNo`,`bizDate`,`source`,`relatedTicketId`,`runId` |
| 退款回调重放 | `notifyLogId`,`orderId`,`merchantRefundId`,`payRefundId`,`runId`,`operator`,`dryRun` |
| 技师提成结算 | `settlementId`,`settlementNo`,`storeId`,`technicianId`,`reviewerId`,`payerId`,`payVoucherNo` |
| 通知补偿 | `outboxId`,`settlementId`,`notifyType`,`channel`,`lastActionCode`,`lastActionBizNo` |

## 4. 标准操作顺序

### 4.1 `BO-001` 四账对账
1. 先看 `summary` / `refund-audit-summary`，确认是日常观察还是异常聚合。
2. 再看 `page` 和 `get`，定位 `reconcileNo`、问题项和关联工单状态。
3. 只有明确需要重跑时才触发 `run`。
4. 如果问题落在退款佣金审计，再执行 `refund-commission-audit/sync-tickets`。
5. 写回结论时只写“任务已受理 / 工单已同步 / 仍待人工复核”，不写“线上已完全恢复”。

### 4.2 `BO-002` 退款回调日志 / 重放 / 运行日志
1. 在 `page` 中先筛 `status=fail` 或指定 `errorCode`。
2. 优先用 `dryRun=true` 验证重放范围和返回结构，再做真实 `replay`。
3. 对批量到期重放使用 `replay-due`，并记录 `limit` 与 `operator`。
4. 重放完成后必须看 `replay-run-log/page`、`summary`、`detail/page`，确认 `success/skip/fail` 明细。
5. 需要转人工追踪时，再执行 `replay-run-log/sync-tickets`。

### 4.3 `BO-003` 技师提成结算 / 通知补偿
1. 在结算台账页创建结算单，先收集 `commissionIds[]` 与备注。
2. 提交时明确 `slaMinutes`；审批 / 驳回必须保留审核备注或驳回原因。
3. 打款动作必须带 `payVoucherNo` 和 `payRemark`，之后立即回读详情与日志。
4. 若通知链路异常，切换到 `outbox/index` 查看出站分页，再执行单条或批量重试。
5. 重试后仍需回读 `notify-outbox-page`，确认 `retriedCount / skipped*Count`，不能只看接口 200。

## 5. 失败处理与人工接管
- 对账：
  - `run` 受理后没有解决证据时，保持“观察中”并转人工核账。
  - `ticketSummaryDegraded=true` 时，只能说明工单摘要退化，不能跳过人工判断。
- 重放：
  - `dryRun` 样本不进入真实成功率。
  - `failCount > 0` 或 `topFailCodes` 集中时，暂停扩大重放范围，先定位失败模式。
- 结算：
  - 审批 / 打款后若页面回读无状态变化，按“伪成功待核”处理。
  - 出站重试若仍 `skipped` 或重复失败，进入人工补偿，不写成已送达。

## 6. 回滚 / 暂停规则
- 四账对账：异常集中时停止继续触发 `run`，保留当前台账，改为只读排查。
- 退款回调重放：失败模式不清时停止批量重放，只保留单条演练或单条实操。
- 技师提成结算：审批、打款、通知补偿任一环节出现伪成功迹象时，暂停后续批量操作。

## 7. 升级路径
- `P0`：批量重放或批量补偿产生集中失败，或对账 run 后异常扩大。
- `P1`：单批次失败可复现，但范围可控。
- `P2`：合法空态、单条 skip、单条 warning，需要继续观察。

## 8. 当前结论
- 本 runbook 已使 `BO-001` ~ `BO-003` 具备独立后台运行指引。
- 这不代表 release gate 已解除；后续仍需要真实样本、灰度、回滚与 sign-off 证据。
