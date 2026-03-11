# MiniApp 财务运营四账对账 PRD v1（2026-03-12）

## 0. 文档定位
- 目标：把 booking 财务运营里的“四账对账”能力收口为可执行产品文档，明确后台页面、真实接口、操作边界、告警与工单协同口径。
- 适用对象：财务运营、客服主管、对账值班、后端、前端后台实现。
- 真实代码基线：
  - 页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/fourAccountReconcile/index.vue`
  - 后台 API：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/fourAccountReconcile.ts`
  - Controller：`ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/FourAccountReconcileController.java`
- 约束：只认当前真实 admin 页面和真实 `method + path`；不把计划中的数据看板、假想工单页写成已上线能力。

## 1. 产品目标
1. 支持按业务日期、来源、状态、问题编码查看四账对账结果。
2. 支持人工触发单日 / 单来源对账运行。
3. 支持查看汇总指标、异常金额、未闭环工单数。
4. 支持查看退款与提成冲正审计结果，并对异常订单执行“同步工单”。
5. 支持在 `ticket summary degraded` 时明确告警降级，不把降级结果伪装成正常统计。

## 2. 业务定义
“四账”在当前系统中至少包含以下金额口径：
- 交易金额：`tradeAmount`
- 履约金额：`fulfillmentAmount`
- 提成金额：`commissionAmount`
- 分账金额：`splitAmount`

关键差额字段：
- 交易 vs 履约差额：`tradeMinusFulfillment`
- 交易 vs 提成+分账差额：`tradeMinusCommissionSplit`

关键目标不是“列表可查”，而是：
- 异常能定位
- 工单能联动
- 退款导致的提成冲正异常可追
- 人工重跑与补单有明确入口

## 3. 页面真值与操作入口

### 3.1 主页面真值
- 页面文件：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/fourAccountReconcile/index.vue`
- 当前页面已承接两块能力：
  1. 四账对账主列表与汇总卡片
  2. 退款提成冲正审计列表与汇总卡片

### 3.2 页面筛选项
- 业务日期 `bizDate`
- 状态 `status`
  - `10` 通过
  - `20` 告警
- 工单关联 `relatedTicketLinked`
- 来源 `source`
  - `JOB_DAILY`
  - `MANUAL`
- 问题编码 `issueCode`
- 退款审计补充筛选：
  - 退款时间范围 `refundTimeRange`
  - 审计状态 `refundAuditStatus`
  - 提成冲正类型 `mismatchType`
  - 上限来源 `refundLimitSource`
  - 订单关键词 / 订单ID / 退款单ID
  - 同步条数上限 `limit`

### 3.3 页面核心动作
1. 查询对账列表
2. 查看对账详情
3. 人工执行对账 `run`
4. 查看汇总 `summary`
5. 查看退款审计汇总 `refund-audit-summary`
6. 查询退款提成冲正审计列表
7. 执行“同步工单”

## 4. 接口真值

| 动作 | Method + Path | 说明 |
|---|---|---|
| 分页查询对账结果 | `GET /booking/four-account-reconcile/page` | 查询对账列表 |
| 查询对账详情 | `GET /booking/four-account-reconcile/get` | 查看单条对账详情 |
| 手工执行对账 | `POST /booking/four-account-reconcile/run` | 按业务日期、来源发起对账 |
| 查询对账汇总 | `GET /booking/four-account-reconcile/summary` | 总数、通过数、告警数、差额汇总、未闭环工单数 |
| 查询退款审计汇总 | `GET /booking/four-account-reconcile/refund-audit-summary` | 退款金额、提成金额、异常分布、工单未闭环数 |
| 查询退款提成冲正审计列表 | `GET /booking/four-account-reconcile/refund-commission-audit-page` | 订单级异常明细 |
| 审计异常同步工单 | `POST /booking/four-account-reconcile/refund-commission-audit/sync-tickets` | 把异常订单同步到工单系统 |

## 5. 关键字段真值

### 5.1 对账主列表字段
- `reconcileNo`
- `bizDate`
- `sourceBizNo`
- `tradeAmount`
- `fulfillmentAmount`
- `commissionAmount`
- `splitAmount`
- `tradeMinusFulfillment`
- `tradeMinusCommissionSplit`
- `status`
- `issueCount`
- `issueCodes`
- `issueDetailJson`
- `source`
- `operator`
- `reconciledAt`
- `relatedTicketId`
- `relatedTicketStatus`
- `relatedTicketSeverity`

### 5.2 汇总字段
- `totalCount`
- `passCount`
- `warnCount`
- `tradeMinusFulfillmentSum`
- `tradeMinusCommissionSplitSum`
- `unresolvedTicketCount`
- `ticketSummaryDegraded`

### 5.3 退款提成冲正审计字段
- `orderId`
- `tradeOrderNo`
- `userId`
- `refundPrice`
- `settledCommissionAmount`
- `reversalCommissionAmountAbs`
- `activeCommissionAmount`
- `expectedReversalAmount`
- `mismatchType`
- `refundAuditStatus`
- `refundExceptionType`
- `refundLimitSource`
- `payRefundId`
- `refundTime`
- `refundEvidenceJson`
- `refundAuditRemark`
- `mismatchReason`

## 6. 状态与业务规则

### 6.1 对账状态
- `10 = 通过`
- `20 = 告警`

规则：
- 告警不等于失败，但必须进入异常跟进。
- 只要存在未闭环工单，财务运营不能把当日对账视为完全闭环。

### 6.2 退款提成冲正异常类型
- `REFUND_WITHOUT_REVERSAL`
- `REVERSAL_WITHOUT_REFUND`
- `REVERSAL_AMOUNT_MISMATCH`

规则：
- 任一异常类型都必须可追到订单、退款单、证据链和工单同步结果。
- “工单同步成功”不等于“财务风险解除”，只代表进入处理流程。

### 6.3 降级与回退规则
- 当 `summary` 或 `refund-audit-summary` 不可用时，页面允许退回列表近似统计。
- 但必须明确展示：
  - `summaryFallback`
  - `summaryFallbackReason`
  - `ticketSummaryDegraded`
- 禁止把降级统计伪装成精确口径。

## 7. 角色分工
- 财务运营：查看差额、确认异常、发起工单同步、跟踪未闭环工单。
- 客服主管：针对退款与提成冲正异常，协调客服外呼或工单说明。
- 后端：保证对账运行、汇总、审计、工单同步链路稳定。
- 前端后台：保留降级提示、工单状态和证据字段，不得做“静默吞错”。

## 8. 验收标准
1. 手工运行对账后，可在列表中看到 `source=MANUAL` 的记录。
2. 汇总卡片与列表字段一致；若接口降级，必须可见 warning 标识。
3. 退款审计列表至少能稳定展示：订单、退款金额、提成金额、异常类型、退款单号、证据链摘要。
4. 点击同步工单后，返回值必须包含成功 / 失败 / attempted 计数，不能只返回布尔值。
5. 页面必须区分：
   - 通过
   - 告警
   - 汇总降级
   - 工单未闭环

## 9. 非目标
- 不在本 PRD 中定义工单系统页面。
- 不在本 PRD 中定义财务科目或会计记账规则。
- 不把对账汇总降级当成“功能完成”。
