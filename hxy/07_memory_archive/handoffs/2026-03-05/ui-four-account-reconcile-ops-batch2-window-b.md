# Window B Handoff - 四账对账运营可读性增强（overlay）

## 日期
- 2026-03-05

## 分支
- `feat/ui-four-account-reconcile-ops`

## 变更范围
- 仅改 overlay 前端与 handoff：
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/fourAccountReconcile.ts`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/fourAccountReconcile/index.vue`
  - `hxy/07_memory_archive/handoffs/2026-03-05/ui-four-account-reconcile-ops-batch2-window-b.md`
- 未改 Java、SQL 表结构、治理文档。

## 功能实现
1. 四账台账列表增强
- 新增 `sourceBizNo` 列（无值时回退 `FOUR_ACCOUNT_RECONCILE:<bizDate>`，再兜底 `--`）。
- 工单信息拆分展示：
  - `relatedTicketId`
  - `relatedTicketStatus`（10=待处理，20=已收口，tag 颜色区分）
  - `relatedTicketSeverity`（P0/P1/P2 tag，空值 `--`）
- 四账状态保留可读标签（通过/告警）。

2. 详情抽屉 + get 接口
- 操作列新增“查看详情”，调用 `GET /booking/four-account-reconcile/get?id=xxx`。
- 抽屉展示：基础信息、金额差额、issueCodes。
- `issueDetailJson` 做结构化渲染（金额字段 + issues 标签）。
- `issueDetailJson` 非法 JSON 时显示：`明细解析失败（原文保留）`，并展示原文，不抛异常。

3. 工单联动
- 操作列提供“跳转工单”，携带：
  - `ticketType=40`
  - `sourceBizNo=FOUR_ACCOUNT_RECONCILE:<bizDate>`
- 无关联工单时按钮置灰，title 提示“暂无关联工单”。

4. 可用性细节
- 列表与详情空值统一 `--`。
- “复制来源号”按钮一键复制 `sourceBizNo`，成功/失败均提示。
- 使用现有 Element Plus 与项目样式，不引入冲突样式体系。

## 验证
1. `git diff --check`
- 结果：PASS（无输出）

2. `test -f ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/package.json || echo overlay-vue3-package-json-missing`
- 结果：`overlay-vue3-package-json-missing`
- 说明：按项目约束，不执行 `pnpm lint`。

## 手工验收清单（步骤 + 预期）
1. 列表字段展示
- 步骤：进入四账台账页面，查看列表列。
- 预期：可见 `sourceBizNo`、`relatedTicketId`、`relatedTicketStatus`、`relatedTicketSeverity`；空值显示 `--`。

2. 工单状态标签可读性
- 步骤：查看含状态 10/20 的记录。
- 预期：四账状态显示“通过/告警”标签；工单状态显示“待处理/已收口”标签。

3. 查看详情抽屉
- 步骤：点击“查看详情”。
- 预期：触发 `/booking/four-account-reconcile/get?id=...` 请求；抽屉展示基础信息、金额差额、issueCodes。

4. issueDetailJson 正常渲染
- 步骤：选一条合法 JSON 记录，打开详情。
- 预期：结构化区域展示金额字段和 issues 标签。

5. issueDetailJson 非法 JSON 降级
- 步骤：选一条非法 JSON 记录，打开详情。
- 预期：显示“明细解析失败（原文保留）”，页面不崩，并能看到原文。

6. 复制来源号
- 步骤：点击“复制来源号”。
- 预期：弹出成功提示，剪贴板内容为 `FOUR_ACCOUNT_RECONCILE:<bizDate>` 或后端返回值。

7. 工单跳转与置灰
- 步骤：
  - 有关联工单行点击“跳转工单”；
  - 无关联工单行观察按钮状态。
- 预期：
  - 有关联时跳转到工单页并带 `ticketType=40&sourceBizNo=...`；
  - 无关联时按钮置灰并有“暂无关联工单”提示。

8. query 回放
- 步骤：复制跳转后的工单页 URL，新窗口打开。
- 预期：query 参数可回放，工单页可按来源号复现筛选。
