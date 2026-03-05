# Window B Handoff - 四账对账台账页 + 工单联动入口

## 日期
- 2026-03-05

## 分支
- `feat/ui-four-account-reconcile-center`

## 变更范围
- 仅改前端 overlay（API + 页面）
- 新增菜单 SQL（幂等）
- 新增本 handoff 文档
- 未改 `booking/trade` 后端代码，未改治理文档

## 变更文件
1. `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/fourAccountReconcile.ts`
2. `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/fourAccountReconcile/index.vue`
3. `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-05-hxy-four-account-reconcile-menu.sql`
4. `hxy/07_memory_archive/handoffs/2026-03-05/ui-four-account-reconcile-window-b.md`

## 功能说明
- 新增四账对账 API 封装
  - `GET /booking/four-account-reconcile/page`
  - `POST /booking/four-account-reconcile/run`
- 新增管理端“四账对账台账”页面
  - 筛选：`bizDate / status / source / issueCode`
  - 列：`bizDate`、`trade/fulfillment/commission/split`、`tradeMinusFulfillment`、`tradeMinusCommissionSplit`、`status`、`issueCodes`、`operator`、`reconciledAt`
  - 按钮：`手工执行对账`（弹窗默认昨日，可清空以走后端默认）
  - 行内入口：`查看关联工单`，跳转 `/review-ticket` 并带 query：
    - `ticketType=40`
    - `sourceBizNo=FOUR_ACCOUNT_RECONCILE:<bizDate>`
- 新增菜单 SQL（挂载 booking 管理菜单，带角色授权与按钮权限）

## 验证记录
1. `git diff --check`
   - 结果：PASS（无输出）
2. `pnpm -C ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3 lint`
   - 结果：FAIL
   - 原因：`overlay-vue3` 目录不存在 `package.json`，无法直接执行 pnpm 脚本

## 手工验证清单
1. 执行菜单 SQL 后，确认“booking 管理”下出现“`四账对账台账`”。
2. 进入页面，按 `bizDate/status/source/issueCode` 搜索，确认分页返回与列表展示。
3. 点击“手工执行对账”：
   - 默认日期应为昨日；
   - 清空日期提交时，后端应按默认昨日执行；
   - 成功后列表刷新。
4. 任一行点击“查看关联工单”，确认跳转到工单页 URL 带：
   - `ticketType=40`
   - `sourceBizNo=FOUR_ACCOUNT_RECONCILE:<bizDate>`

## 备注
- 当前工作区存在窗口 A 的后端改动，本次提交未触碰这些文件。
