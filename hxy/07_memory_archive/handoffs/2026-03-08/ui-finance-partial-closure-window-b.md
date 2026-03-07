# UI 财务运营联调收口（Window B）

- 日期：2026-03-08
- 分支：`feat/ui-four-account-reconcile-ops`
- 范围：仅 `overlay-vue3` 与菜单权限 SQL；未改后端 Java

## 本批变更摘要

1. 退款补偿页（`refundNotifyLog`）
- 主列表补齐财务视角字段展示：`payRefundId`、`runId`、`resultCode`、`warningTag`、`ticketSyncStatus`
- 详情抽屉补齐同口径字段
- 批量结果弹窗统一展示：`attempted/success/skip/fail + details`
- 当后端缺失财务字段时，页面降级提示“后端版本暂不支持”，不阻断查询和弹窗

2. 四账退款佣金审计页（`fourAccountReconcile`）
- 审计筛选明确提成/冲正类型：`mismatchType`
- 列表与详情补齐提成计提字段展示：`activeCommissionAmount`
- summary 区块补充提成/冲正聚合展示：
  - `refundPriceSum`
  - `settledCommissionAmountSum`
  - `reversalCommissionAmountAbsSum`
  - `activeCommissionAmountSum`
  - `expectedReversalAmountSum`
- 上述聚合字段缺失时自动回退前端近似聚合，并标记“提成/冲正汇总降级”

3. 售后详情联动（`afterSale/detail`）
- 明确展示：`refundLimitSource`、异常类型、`refundEvidenceJson` 结构化结果
- 非法 JSON 时保留原文展示，不抛前端异常

4. 菜单与按钮权限 SQL（幂等）
- 新增本批按钮权限脚本，仅补按钮，不重复建菜单：
  - `booking:refund-notify-log:replay-run-log:sync-tickets`
  - `booking:commission:refund-audit:sync-tickets`

## 手工验收清单（步骤 + 预期 + 降级场景）

1. 退款回调日志主列表财务字段
- 步骤：打开“退款回调日志”，执行查询。
- 预期：列表可见 `payRefundId/runId/resultCode/warningTag/ticketSyncStatus` 列；空值显示 `--`。
- 降级场景：后端未返回字段时，顶部出现“后端版本暂不支持”提示，页面不崩。

2. 退款重放批量结果弹窗统一统计口径
- 步骤：勾选失败记录，分别执行 dry-run 与执行重放。
- 预期：弹窗展示 `attempted/success/skip/fail` 与 details 明细。
- 降级场景：后端明细缺字段时出现“后端版本暂不支持（批量结果明细缺少财务字段，已降级展示）”。

3. 重放批次明细看板财务字段
- 步骤：进入“重放批次历史”->“详情”，查看运行明细看板。
- 预期：可见 `runId/payRefundId/resultCode/warningTag/ticketSyncStatus`。
- 降级场景：后端缺字段时提示“后端版本暂不支持（运行明细缺少财务字段，已降级展示）”。

4. 四账退款佣金审计筛选
- 步骤：在“退款佣金审计”使用“提成冲正类型”筛选并查询。
- 预期：筛选条件生效，列表按 `mismatchType` 返回。
- 降级场景：后端仅支持旧字段时，页面仍可查询并回退到兼容参数。

5. 四账提成/冲正字段展示
- 步骤：查看审计列表与“查看证据”详情抽屉。
- 预期：展示 `activeCommissionAmount`（提成计提）以及冲正相关金额字段。
- 降级场景：字段缺失时显示 `--`，不影响列表与详情打开。

6. 四账 summary 聚合
- 步骤：执行审计查询，观察 summary 卡片与降级标签。
- 预期：后端返回聚合字段时直接展示；缺失时采用前端近似聚合并出现“提成/冲正汇总降级”。
- 降级场景：`refund-audit-summary` 接口不可用时，显示“退款审计汇总已降级”，列表仍可用。

7. 售后详情证据 JSON 解析
- 步骤：打开售后详情，分别验证合法/非法 `refundEvidenceJson`。
- 预期：合法 JSON 展示结构化键值；非法 JSON 显示告警并保留原文。
- 降级场景：任何解析异常均不抛前端错误、不白屏。

8. 按钮权限 SQL 幂等
- 步骤：执行 `2026-03-08-hxy-finance-ops-button-perms.sql` 两次。
- 预期：首次补齐按钮权限；二次执行无重复数据。
- 降级场景：页面菜单不存在时脚本跳过插入，不影响其他菜单。
