# Window B Handoff - 门店生命周期变更单审批（overlay 最小版）

## 分支与范围
- 分支：`feat/ui-four-account-reconcile-ops`
- 仅改：`overlay-vue3`、`sql/mysql/hxy`、handoff
- 未改：Java 后端与治理文档

## 本次改动
1. API 封装新增
- `src/api/mall/store/lifecycleChangeOrder.ts`
- 覆盖接口：
  - `POST /product/store/lifecycle-change-order/create`
  - `POST /product/store/lifecycle-change-order/submit`
  - `POST /product/store/lifecycle-change-order/approve`
  - `POST /product/store/lifecycle-change-order/reject`
  - `POST /product/store/lifecycle-change-order/cancel`
  - `GET /product/store/lifecycle-change-order/get`
  - `GET /product/store/lifecycle-change-order/page`

2. 页面新增（最小可运营）
- `src/views/mall/store/lifecycleChangeOrder/index.vue`
- 功能：
  - 筛选：`orderNo/storeId/status/fromLifecycleStatus/toLifecycleStatus/applyOperator/createTime`
  - 列表分页：门店、状态、申请人、申请时间、审批信息
  - 详情抽屉：`reason + guardSnapshotJson`（解析失败降级“原文保留”）
  - 操作：提交/审批通过/驳回/取消（按状态显隐）
  - 高风险操作二次确认
  - 空值统一 `--`
  - 复制 `orderNo/storeId`

3. 跳转联动（同风格）
- 在生命周期批次台账页、复核历史页增加“变更单审批”快捷跳转
- 在门店管理主列表页增加 `Lifecycle Change Order` 快捷入口按钮

4. 菜单 SQL
- 新增幂等脚本：`2026-03-05-hxy-store-lifecycle-change-order-menu.sql`
- 挂载到 `store-master` 下并授权 admin/operator

## 手工验收清单
1. 入口
- 步骤：从门店管理菜单进入 `Lifecycle Change Order`；从批次台账/复核历史点击“变更单审批”
- 预期：均可正常跳转，无死链

2. 筛选分页
- 步骤：按 `orderNo/storeId/status/from/to/applyOperator/createTime` 组合查询并翻页
- 预期：筛选与分页生效，列表稳定显示

3. 详情抽屉
- 步骤：点击“查看详情”
- 预期：展示基础信息、reason、guardSnapshotJson 结构化内容与原文

4. 状态操作
- 步骤：草稿/驳回单执行“提交”，待审批单执行“审批通过/驳回”，草稿/待审批单执行“取消”
- 预期：按钮按状态显隐；操作前二次确认；执行后提示成功并刷新列表

5. 复制便捷
- 步骤：点击“复制单号/复制门店ID”
- 预期：成功提示；空值时显示“为空，无法复制”

6. 异常降级
- 步骤：使用非法 `guardSnapshotJson` 记录打开详情
- 预期：显示“guardSnapshotJson 解析失败（原文保留）”，页面不崩溃

## 备注
- `overlay-vue3` 无 `package.json`，按约束不执行 pnpm lint。
