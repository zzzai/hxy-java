# UI refund replay V4 detail sync audit (window B)

- Date: 2026-03-07
- Branch: `feat/ui-four-account-reconcile-ops`
- Scope: overlay only (`refundNotifyLog` 页面); no Java/SQL changes

## 1. 本批交付

1. run 明细看板接入
- 新增接口接入：
  - `GET /booking/refund-notify-log/replay-run-log/detail/page`
  - `GET /booking/refund-notify-log/replay-run-log/detail/get`
- 在“重放批次详情”抽屉增加“运行明细看板”：
  - 字段展示：`resultStatus/resultCode/warningTag/ticketSyncStatus/ticketId/ticketSyncTime/错误信息`
  - 支持 `notifyLogId` 查询、状态筛选、分页、详情查看

2. 工单同步审计视图增强
- `POST /booking/refund-notify-log/replay-run-log/sync-tickets` 请求支持：
  - `dryRun`
  - `forceResync`
- 交互新增：
  - `预演同步(dryRun)`
  - `执行同步`
  - `forceResync` 开关（默认关）
- 结果新增弹窗：
  - 汇总：`attempted/success/skip/fail`
  - 明细：`notifyLogId/resultCode/warningTag/ticketSyncStatus/ticketId/ticketSyncTime/错误信息`
  - 支持复制失败项 `notifyLogId/错误码`

3. 兼容降级
- `detail/page`、`detail/get`、`sync-tickets` 任一接口/字段不支持时：
  - 页面不崩溃
  - 明确提示“后端版本暂不支持”
  - 不影响既有 `replay/replay-due` 主链路
- 空值统一按 `--` 展示
- 错误信息支持复制

## 2. 变更文件

- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/refundNotifyLog.ts`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/refundNotifyLog/index.vue`

## 3. 手工验收清单

1. 筛选
- 进入“重放批次历史”->“详情”，在“运行明细看板”使用：
  - `notifyLogId`
  - `resultStatus`
  - `ticketSyncStatus`
  - `warningTag`
- 检查筛选结果变化正确

2. 分页
- 在明细看板切换页码/每页条数，检查请求与结果一致

3. 详情
- 点击明细行“详情”，检查字段完整展示；错误信息可复制
- 若 `detail/get` 不支持，出现“后端版本暂不支持”提示

4. 预演同步
- 打开 `forceResync` 开关不同状态，执行“预演同步(dryRun)”
- 校验结果弹窗中的 `attempted/success/skip/fail + details`

5. 执行同步
- 执行“执行同步”，校验结果弹窗与“工单同步审计”区域汇总
- 验证“复制失败项 notifyLogId/错误码”可用

6. 降级
- 在低版本后端验证：
  - detail/sync 不支持时显示“后端版本暂不支持”
  - 页面其他功能（列表、replay、replay-due）保持可用

7. 空值与错误提示
- 缺失字段显示 `--`
- 请求失败时有明确错误提示，且错误信息可复制
