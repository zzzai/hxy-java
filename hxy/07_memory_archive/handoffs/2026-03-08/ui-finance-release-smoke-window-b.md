# UI Finance Release Smoke - Window B

- 日期：2026-03-08
- 分支：`feat/ui-four-account-reconcile-ops`
- 范围：`overlay-vue3` 展示一致性收口（不改后端）

## 本批收口结论

1. 退款回调重放页
- 结果弹窗与运行明细统一统计口径：`attempted/success/skip/fail`。
- 财务字段展示统一：`payRefundId/runId/resultCode/warningTag/ticketSyncStatus`。
- 后端字段缺失与接口缺失统一提示：`后端版本不支持，已降级展示`。

2. 四账汇总页
- 提成/冲正 summary 统一展示：`refundPriceSum/settledCommissionAmountSum/reversalCommissionAmountAbsSum/activeCommissionAmountSum/expectedReversalAmountSum`。
- 缺字段时统一降级提示：`后端版本不支持，已降级展示`，并回退列表近似聚合。

3. 售后详情页
- 展示联动字段：`refundLimitSource`、异常类型、`refundEvidenceJson` 结构化结果。
- 非法 JSON 降级保留原文；旧后端缺字段统一提示：`后端版本不支持，已降级展示`。

4. 埋点注释
- 已在三页降级关键路径补 `埋点注释`，建议统一落地事件：`finance_ui_backend_unsupported(scene=...)`。

## 联调验收清单（步骤 + 预期 + 异常分支）

1. 退款回调日志列表字段一致性
- 步骤：进入 `退款回调日志`，执行任意查询。
- 预期：可见 `payRefundId/runId/resultCode/warningTag/ticketSyncStatus`；空值显示 `--`。
- 异常分支：若后端缺字段，顶部出现 `后端版本不支持，已降级展示` + 缺失字段列表，页面可继续操作。

2. 重放结果弹窗统计口径
- 步骤：勾选失败记录，分别触发 `预演重放` 与 `执行重放`。
- 预期：弹窗与成功提示文案都显示 `attempted/success/skip/fail`；details 表格可展示财务字段。
- 异常分支：旧后端不支持批量协议时自动降级旧接口逐条执行，不白屏。

3. 重放批次历史与详情降级链路
- 步骤：打开 `重放批次历史` -> 进入任一批次详情 -> 刷新汇总与运行明细。
- 预期：接口正常时展示汇总和运行明细；接口不可用时统一提示 `后端版本不支持，已降级展示`。
- 异常分支：`summary/detail/get/detail/page/sync-tickets` 任一不可用时，仅降级对应模块，其它模块可继续使用。

4. 四账退款佣金审计筛选
- 步骤：使用 `提成冲正类型(mismatchType)` + 业务日期 + 退款时间 + 订单ID/退款单ID 查询。
- 预期：筛选条件透传并生效；列表展示 `activeCommissionAmount`（提成计提）与冲正相关字段。
- 异常分支：后端仅返回旧字段时，前端兼容 `refundExceptionType <-> mismatchType`，查询不报错。

5. 四账 summary 提成/冲正聚合
- 步骤：执行审计查询并观察 summary 卡片。
- 预期：有聚合字段时展示后端值；缺字段时展示前端近似聚合，且出现统一降级提示。
- 异常分支：`refund-audit-summary` 全接口不可用时，保留列表和分页能力，展示降级说明。

6. 售后详情证据与异常类型
- 步骤：打开售后详情，验证 `refundLimitSource/异常类型/refundEvidenceJson` 展示。
- 预期：合法 JSON 结构化展示；非法 JSON 告警并保留原文。
- 异常分支：旧后端缺退款联调字段时，页面顶部提示 `后端版本不支持，已降级展示` + 缺失字段列表。

7. 退款错误码提示一致性
- 步骤：触发退款动作并模拟 `1011000125/1030004011/1030004012`。
- 预期：提示文案明确（上限命中/merchantRefundId 非法/幂等冲突），并附原始信息。
- 异常分支：未知错误码仍回退后端原始错误信息，不吞错。

8. 空值兜底一致性
- 步骤：选择一组缺省数据（字段 null/undefined）访问三个页面。
- 预期：统一展示 `--` 或 `-`（按页面现有常量），无 `null/undefined` 泄漏。
- 异常分支：任何字段缺失均不抛前端异常，不影响页面主流程。
