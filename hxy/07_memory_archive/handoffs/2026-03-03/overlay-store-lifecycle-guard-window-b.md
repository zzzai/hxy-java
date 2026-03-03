# overlay-store-lifecycle-guard-window-b

## 背景
在主仓 worktree `feat/ui-store-lifecycle-guard` 中，为 `overlay-vue3` 门店管理页接入“生命周期守卫预检（单店+批量）”，实现“先预检再执行生命周期变更”。

## 变更文件
1. `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/store/store.ts`
2. `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/index.vue`

## 主要实现
- API 扩展：新增 `HxyStoreLifecycleGuardItem`、`HxyStoreLifecycleGuardResp`，以及
  - `getStoreLifecycleGuard(id, lifecycleStatus)`
  - `getStoreLifecycleGuardBatch({ storeIds, lifecycleStatus, reason? })`
- 门店页接入：新增单店/批量“生命周期变更”入口与统一弹窗。
- 提交流程：`预检 -> 规则判定 -> 执行变更`
  - 存在 blocked：禁止提交。
  - 仅 warnings：二次确认后允许提交。
- 结果展示：展示 blocked 数、warning 数，并按门店可展开查看 guardItems（`guardKey/count/mode/blocked`）。
- 同步补齐生命周期展示：新增 `35=停业` 的文本/颜色与下拉选项。

## 验证
- 已尝试命令：
  - `pnpm -C ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3 lint`
- 结果：无法执行。
  - 原因 1：环境未安装 `pnpm`（Volta 报错找不到可执行文件）。
  - 原因 2：改用 `corepack pnpm` 后，目标目录缺少 `package.json`，无法作为 pnpm importer 运行 lint。
- 人工验证点：
  1. 单店生命周期变更：选择目标状态后点击“执行预检”，确认出现 blocked/warnings/guardItems。
  2. blocked 场景：确认“预检并提交”被阻断，未调用更新成功提示。
  3. warning-only 场景：确认出现二次确认，确认后可提交成功。
  4. 批量场景：勾选多门店后打开批量变更，确认按门店折叠展示预检详情。
