# Window B Handoff - 四账对账台账运营可读性增强（overlay）

## 日期
- 2026-03-05

## 分支
- `feat/ui-four-account-reconcile-ops`

## 改动范围
- 仅改 overlay 前端与 handoff：
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/fourAccountReconcile.ts`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/fourAccountReconcile/index.vue`
  - `hxy/07_memory_archive/handoffs/2026-03-05/ui-four-account-reconcile-ops-window-b.md`
- 未改 Java/SQL/后端接口签名。

## 功能增强摘要
1. 行级“差异详情”按钮
- 新增操作按钮“差异详情”。
- 弹窗解析并展示 `issueDetailJson`：
  - `tradeAmount / fulfillmentAmount / commissionAmount / splitAmount`
  - `tradeMinusFulfillment / tradeMinusCommissionSplit`
  - `issues` 数组（Tag 化）
- `issueDetailJson` 为空、非对象、非法 JSON 时统一显示“无可用明细”，不抛异常。

2. 关联工单展示增强
- 在“关联工单”列中展示：
  - `relatedTicketStatus`：`10=待处理`（warning）、`20=已收口`（success）
  - `relatedTicketSeverity`：`P0/P1/P2` 按级别着色；为空时显示 `-`

3. 操作列新增“复制来源号”
- 新增“复制来源号”按钮。
- 复制值固定格式：`FOUR_ACCOUNT_RECONCILE:${bizDate}`。
- `bizDate` 为空时提示不可复制。

4. API 类型补齐
- 补充 `relatedTicketStatus/relatedTicketSeverity` 语义类型。
- 补充 `FourAccountIssueDetail` 结构化类型，便于页面解析对齐。

## 验证记录
1. `git diff --check`
- 结果：PASS（无输出）

2. `test -f ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/package.json || echo overlay-vue3-package-json-missing`
- 结果：`overlay-vue3-package-json-missing`
- 说明：按项目约束不执行 `pnpm lint`。

## 手工验收清单（步骤 + 预期）
1. 正常详情解析
- 步骤：选一条 `issueDetailJson` 为合法 JSON 且含金额/issues 的记录，点击“差异详情”。
- 预期：弹窗展示金额字段与差额字段，issues 以标签显示。

2. 空值详情
- 步骤：选一条 `issueDetailJson` 为空（空串或 null）记录，点击“差异详情”。
- 预期：弹窗显示“无可用明细”，页面无报错。

3. 非法 JSON
- 步骤：让某条记录 `issueDetailJson` 为非法 JSON（如 `{bad json}`），点击“差异详情”。
- 预期：弹窗仍显示“无可用明细”，控制台无未捕获异常。

4. 关联工单状态/级别展示
- 步骤：查看含 `relatedTicketStatus/relatedTicketSeverity` 与缺失字段两类记录。
- 预期：
  - `10` 显示“待处理”warning tag；`20` 显示“已收口”success tag；
  - 严重级别显示 `P0/P1/P2` 对应 tag；为空显示 `-`。

5. 复制来源号
- 步骤：点击“复制来源号”。
- 预期：复制成功提示；剪贴板内容为 `FOUR_ACCOUNT_RECONCILE:<bizDate>`。

6. 跳转联动保持不变
- 步骤：点击“查看关联工单”。
- 预期：路由仍跳转 `/mall/trade/after-sale/review-ticket`，query 包含：
  - `ticketType=40`
  - `sourceBizNo=FOUR_ACCOUNT_RECONCILE:<bizDate>`

7. query 回放
- 步骤：在工单页保留上述 query 后刷新页面或复制 URL 重开。
- 预期：query 参数稳定保留并可用于复现过滤。
