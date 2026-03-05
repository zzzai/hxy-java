# Window B Handoff - 生命周期变更单运营化增强（overlay）

## 分支与范围
- 分支：`feat/ui-four-account-reconcile-ops`
- 仅改动：overlay API、overlay 页面、菜单 SQL、handoff
- 未改动：Java 后端、治理文档

## 本批交付
1. API 适配增强
- 文件：`src/api/mall/store/lifecycleChangeOrder.ts`
- 新增/补齐分页查询参数：
  - `overdue`
  - `lastActionCode`
  - `lastActionOperator`
- 新增/补齐返回字段：
  - `submitTime`
  - `slaDeadlineTime`
  - `overdue`
  - `lastActionCode`
  - `lastActionOperator`
  - `lastActionTime`
  - 保持字段可选，兼容后端未返回场景

2. 页面运营化增强
- 文件：`src/views/mall/store/lifecycleChangeOrder/index.vue`
- 筛选增强：
  - 新增 `overdue/lastActionCode/lastActionOperator`
- 快捷筛选：
  - `全部`
  - `待审批`
  - `已超时`
- 列表新增列：
  - `submitTime`
  - `slaDeadlineTime`
  - `lastActionCode`
  - `lastActionOperator`
  - `lastActionTime`
  - `SLA 状态 Tag`（正常/即将超时/已超时）
- 详情抽屉增强：
  - 新增展示 `submitTime/slaDeadlineTime/lastAction*/guardBlocked/guardWarnings`
  - 空值统一 `--`
  - `guardSnapshotJson` 保持：结构化 + 原文 + 解析失败降级（原文保留）
- 交互与动作：
  - 提交/审批通过/驳回/取消仍按状态显隐
  - 高风险动作二次确认
  - 操作成功后刷新当前分页，保留现有筛选条件
  - 复制 `orderNo/storeId` 保持成功/失败提示

3. 菜单 SQL 幂等加强
- 文件：`sql/mysql/hxy/2026-03-05-hxy-store-lifecycle-change-order-menu.sql`
- 调整点：
  - 插入菜单时按全局 `path` 去重，避免重复插入
  - 菜单 ID 解析增加 fallback（按 path 兜底），确保仅补权限关联时可生效

## 手工验收清单（步骤 + 预期）
1. 入口
- 步骤：进入“Lifecycle Change Order”页
- 预期：页面正常打开；从相关生命周期页跳转无死链

2. 筛选
- 步骤：按 `overdue/lastActionCode/lastActionOperator` + 原有条件联合查询
- 预期：列表按条件返回；分页正常

3. 快捷筛选
- 步骤：点击“全部/待审批/已超时”
- 预期：状态与超时条件自动切换并刷新；结果符合预期

4. 列表 SLA 展示
- 步骤：观察待审批记录 SLA 列
- 预期：显示 `正常/即将超时/已超时` tag；非待审批记录显示 `--`

5. 详情抽屉
- 步骤：打开任意记录详情
- 预期：显示新增时间/动作字段与守卫字段；空值为 `--`

6. guardSnapshotJson 降级
- 步骤：打开 `guardSnapshotJson` 非法记录
- 预期：显示解析失败提示，原文仍可查看，页面不崩溃

7. 状态动作
- 步骤：按状态执行提交/通过/驳回/取消
- 预期：按钮按状态显隐；二次确认后执行；完成后保留筛选并刷新当前页

## 备注
- `overlay-vue3` 无 `package.json`，按约束未执行 pnpm lint。
