# UI SLA Route Center Window-B Handoff

## 改动点

### 1) 规则编辑体验增强
- 文件：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/reviewTicketRoute/index.vue`
- scope 切换约束可视化：新增作用域约束提示（RULE / TYPE_SEVERITY / TYPE_DEFAULT / GLOBAL_DEFAULT）。
- 工单类型从输入框改为可读选项：
  - `10 售后复核`
  - `20 服务履约`
  - `30 提成争议`
- `ruleCode` / `escalateTo` 在输入失焦时自动 `trim + uppercase`。
- 表单校验改为按作用域动态约束，错误提示更清晰。

### 2) 命中预览（核心）
- 文件：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/reviewTicketRoute/index.vue`
- 新增“命中预览”区域，输入 `ruleCode / ticketType / severity` 后按固定优先级计算命中：
  - `RULE > TYPE_SEVERITY > TYPE_DEFAULT > GLOBAL_DEFAULT`
- 使用 `list-enabled` 数据在前端模拟命中，返回：
  - `routeId`
  - `scope`
  - `escalateTo`
  - `slaMinutes`
  - `sort`
  - `命中说明`
- 当未命中启用规则时，显示系统兜底说明（P1 / HQ_AFTER_SALE / 120）。

### 3) 批量运营能力
- 文件：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/reviewTicketRoute/index.vue`
- 表格新增多选。
- 新增批量操作：批量启用、批量停用、批量删除。
- 批量操作增加二次确认。
- 批量结果统一汇总提示：成功 N、失败 M、失败 ID 列表。

### 4) 查询可回放
- 文件：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/reviewTicketRoute/index.vue`
- 将筛选条件与分页写入 URL query。
- 页面刷新后从 URL query 恢复筛选与页码。

### 5) 交互鲁棒性
- 文件：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/reviewTicketRoute/index.vue`
- 搜索/提交/批量按钮防重复触发（loading 锁）。
- 单项与批量操作的异常统一显示可读 message，不再 silent swallow。

### 6) API 类型补强（不改接口签名）
- 文件：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/trade/reviewTicketRoute/index.ts`
- 新增 `ReviewTicketRoutePageReqVO`。
- `getReviewTicketRoutePage` 使用强类型参数。
- `getEnabledReviewTicketRouteList` 补充泛型返回 `ReviewTicketRouteVO[]`。

## 验证结果

### 已执行命令
1. `pnpm -C ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3 lint`
- 结果：失败
- 原因：当前环境无可执行 `pnpm`（Volta: Could not find executable "pnpm"）

2. `COREPACK_HOME=/tmp/corepack corepack pnpm -C ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3 lint`
- 结果：失败
- 原因：目录缺少 `package.json`，不是可执行 lint 的 pnpm importer

3. `git diff --check`
- 结果：通过（无空白错误）

### 手工验证清单（替代）
1. 查询条件回放：修改筛选+翻页后刷新页面，确认筛选与页码恢复。
2. 表单约束：切换 4 种 scope，确认字段禁用/必填/提示与预期一致。
3. 预览优先级：构造同 ticketType 的 TYPE_SEVERITY + TYPE_DEFAULT + GLOBAL_DEFAULT，确认命中顺序正确。
4. 批量启停删：多选后执行，确认二次确认弹窗与成功/失败汇总提示。
5. 异常提示：手动制造接口失败，确认页面有可读错误信息。

## 已知风险
- 前端命中预览按当前 `list-enabled` 列表顺序“后写覆盖前写”来贴近后端缓存构建行为；若后端未来改变加载策略，预览与实际命中可能出现偏差。
- 当前 overlay 目录缺少 Node 工程清单，无法在此工作目录完成自动化 lint/类型检查。

## 回滚方式
1. 软回滚本次功能提交：
   - `git revert a7296189ac`
2. 若需整体回退本分支到基线：
   - 回到基线分支重新拉取 `origin/feat/p1-package-refund-hardening` 并放弃该分支改动。

