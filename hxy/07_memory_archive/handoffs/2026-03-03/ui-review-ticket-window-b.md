# UI Review Ticket Window-B Handoff

## 改动概览

### 1) 新增 API 封装
- 文件：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/trade/reviewTicket/index.ts`
- 新增接口：
  - `GET /trade/after-sale/review-ticket/page`
  - `GET /trade/after-sale/review-ticket/get?id=`
  - `PUT /trade/after-sale/review-ticket/resolve`
- 新增类型：`ReviewTicketVO`、`ReviewTicketPageReqVO`、`ReviewTicketResolveReqVO`

### 2) 新增页面（最小闭环）
- 文件：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/reviewTicket/index.vue`
- 功能：
  - 分页查询 + 筛选：
    `ticketType, status, severity, escalateTo, routeId, routeScope, overdue, lastActionCode, sourceBizNo, afterSaleId, orderId, userId, createTime`
  - 列表展示包含：
    `id, ticketType, severity, escalateTo, routeId, routeScope, routeDecisionOrder, overdue, status, lastActionCode, lastActionTime, createTime`
  - `查看详情` 弹窗（调用 `/get`）
  - `收口(resolve)` 弹窗（字段：`id, resolveActionCode, resolveBizNo, resolveRemark`，调用 `/resolve`）
  - 基础错误提示与按钮防重复提交（resolve）

### 3) 路由与菜单接入（按 overlay 现有模式）
- 文件：`ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-03-hxy-review-ticket-menu.sql`
- 内容：
  - 在 `trade/after-sale` 下新增页面菜单：
    - path: `review-ticket`
    - component: `mall/trade/reviewTicket/index`
    - component_name: `MallTradeReviewTicketIndex`
  - 新增按钮权限菜单：
    - `trade:after-sale:query`
    - `trade:after-sale:refund`
  - 授权 admin/operator 角色

## 验证结果

### 命令
1. `git diff --check`
- 结果：通过

2. `pnpm -C ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3 lint`
- 结果：失败
- 原因：环境无可执行 `pnpm`（Volta 提示找不到 pnpm）

3. `COREPACK_HOME=/tmp/corepack corepack pnpm -C ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3 lint`
- 结果：失败
- 原因：目录无 `package.json`，无法作为 pnpm importer 执行 lint

### 手工验证清单
1. 菜单注入后进入「交易中心 -> 售后管理 -> 售后人工复核工单」页面，确认可正常打开。
2. 分页筛选：逐项验证筛选条件生效（尤其 `routeScope / overdue / createTime`）。
3. 详情弹窗：点击「查看详情」，字段与后端返回一致。
4. 收口弹窗：对待处理工单执行收口，确认列表状态切换为“已收口”，详情字段刷新。
5. 异常提示：模拟接口异常时，页面出现可读错误信息。

## 已知风险
1. 当前 overlay 目录缺少 Node 工程清单，无法在本目录跑 lint/type-check；需在完整前端工程中再跑一次自动化校验。
2. 菜单 SQL 依赖 `trade` 与 `after-sale` 父菜单已存在；若缺失需先补父菜单。

## 回滚方式
1. 回滚本次提交：`git revert <commit>`
2. 若仅回滚菜单：撤销 SQL 文件执行或删除对应菜单与角色菜单关联。

