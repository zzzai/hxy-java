# Window B Handoff - 门店 SKU 台账失败重试可视化收口

## 日期
- 2026-03-05

## 分支
- `feat/ui-store-sku-retry-audit`

## 本次范围
- 仅改 overlay 前端：
  - `overlay-vue3/src/api/mall/product/storeSku.ts`
  - `overlay-vue3/src/views/mall/product/store/sku/index.vue`
- 新增 handoff 文档：
  - `hxy/07_memory_archive/handoffs/2026-03-05/ui-store-sku-retry-audit-window-b.md`
- 未改后端 Java
- 未新增菜单 SQL（本次没有新增页面入口）

## 主要改动
1. 按状态一键全选失败项（保留手工勾选）
- 按钮文案更新为“按状态一键全选失败项”。
- 选择逻辑改为：仅勾选当前筛选结果中“可重试失败项”（失败状态 + 可重试标记兼容判断）。
- 不再清空已有手工勾选，仅增量勾选。

2. 批量重试结果明细增强 + 失败明细复制
- 重试结果弹窗新增明细列：`storeId`、`skuId`、`结果状态`（SUCCESS/SKIPPED/FAILED）、`失败原因`。
- 头部展示成功/跳过/失败汇总。
- 新增“复制失败明细”按钮，复制格式：
  - `id=<...>, storeId=<...>, skuId=<...>, reason=<...>`

3. stock-flow / batch-retry 审计字段展示与筛选
- 在库存流水筛选中新增 `operator/source` 条件，并透传查询参数。
- 列表审计列改为兼容展示：优先 `operator/source`，降级到 `lastRetryOperator/lastRetrySource`，最终 `-`。
- 批量重试明细审计展示兼容：优先 `operator/source`，降级 `retryOperator/retrySource`。

4. API 类型补齐（与后端字段兼容）
- stock-flow 列表类型补充：`operator/source` 与可重试兼容字段（`canRetry/retryable/allowRetry/canBatchRetry`）。
- stock-flow 分页请求补充：`operator/source`。
- batch-retry 明细项补充：`storeId/skuId`、`resultStatus`、`failReason/message`、`operator/source` 等兼容字段。

## 验证
1. 命令校验
- `git diff --check`：PASS

2. 说明
- `overlay-vue3` 无 `package.json`，无法执行 `pnpm lint`。

## 手工验收清单（步骤 + 预期）
1. 打开“库存流水台账”弹窗
- 步骤：进入门店 SKU 页面，点击“库存流水台账”。
- 预期：弹窗正常打开，无报错。

2. 审计筛选可用
- 步骤：填写 `operator`、`source` 后搜索。
- 预期：请求参数包含 `operator/source`；后端不支持时页面仍正常显示，不报错。

3. 按状态一键全选失败项
- 步骤：先手工勾选若干行，再点击“按状态一键全选失败项”。
- 预期：
  - 已手工勾选项保留；
  - 仅新增勾选当前筛选结果中的可重试失败项；
  - 无可重试失败项时提示文案正确。

4. 批量重试结果明细
- 步骤：勾选失败项后执行“批量重试失败流水”。
- 预期：弹窗展示成功/跳过/失败数量，明细包含 `storeId/skuId/结果状态/失败原因`。

5. 复制失败明细
- 步骤：在结果弹窗点击“复制失败明细”。
- 预期：成功提示；剪贴板文本逐行包含 `id/storeId/skuId/reason`。

6. 审计字段展示降级
- 步骤：观察列表与结果弹窗的操作人/来源列。
- 预期：
  - 新字段存在则展示新字段；
  - 新字段缺失回落旧字段；
  - 都缺失显示 `-`，不报错。
