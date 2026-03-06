# Window B Handoff - 退款回调重放 V3 管理端收口（自动重放 + 批次历史 + runId）

## 日期
- 2026-03-06

## 分支
- `feat/ui-four-account-reconcile-ops`

## 交付范围
1. API 契约补齐（overlay）
- 文件：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/refundNotifyLog.ts`
- 新增接口：
  - `replayDue(data)` -> `POST /booking/refund-notify-log/replay-due`
  - `getReplayRunLogPage(params)` -> `GET /booking/refund-notify-log/replay-run-log/page`
  - `getReplayRunLog(id)` -> `GET /booking/refund-notify-log/replay-run-log/get`
- 类型补齐字段：
  - `runId`
  - `triggerSource`
  - `operator`
  - `dryRun`
  - `limitSize`
  - `scannedCount`
  - `successCount`
  - `skipCount`
  - `failCount`
  - `status`
  - `errorMsg`
  - `startTime`
  - `endTime`

2. 页面增强（overlay）
- 文件：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/refundNotifyLog/index.vue`
- 新增“自动补偿重放”入口：
  - 可配置 `dryRun` 与 `limit`
  - 触发前二次确认
  - 触发后展示 `runId + 扫描/成功/跳过/失败 + details`
- 新增“重放批次历史”弹窗：
  - 支持按 `runId/status/operator/timeRange` 筛选
  - 支持分页
  - 支持单条详情抽屉（`errorMsg`、统计、起止时间）
- runId 追溯：
  - 结果弹窗与历史列表均可直接查看 runId
- 保持空值统一 `--`

3. 交互降级
- replay / replay-due / run-log 接口异常时页面不崩溃，仅提示错误。
- details 为空时明确提示“无重放明细”。
- 后端未升级场景：
  - `replay-due` 未上线：提示“后端未升级 V3，暂不支持自动补偿重放入口”
  - `replay-run-log` 未上线：历史弹窗提示不可用并降级
  - 旧后端仅支持 `id` 重放时，手工执行支持逐条降级调用

4. SQL 菜单/权限（幂等，仅补按钮）
- 文件：`ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-06-hxy-booking-refund-notify-log-menu-v3.sql`
- 仅补按钮权限，不重复创建页面菜单：
  - `booking:refund-notify-log:replay-due`（自动补偿重放触发）
  - `booking:refund-notify-log:replay-run-log:query`（重放批次历史查询）

## 手工验收清单
1. 入口可见性
- 步骤：具备对应权限账号进入退款回调日志管理页。
- 预期：可见“自动补偿重放”“重放批次历史”按钮；无权限账号不可见。

2. 自动补偿重放触发
- 步骤：设置 `limit` 与 `dryRun`，点击“自动补偿重放”。
- 预期：先二次确认；确认后发起 `replay-due` 请求。

3. 自动重放结果汇总
- 步骤：触发成功后查看结果弹窗。
- 预期：展示 `runId/scanned/success/skip/fail` 与 `status/startTime/endTime`。

4. 自动重放明细
- 步骤：查看结果弹窗 details 表格。
- 预期：可见明细结果状态 Tag（SUCCESS/SKIP/FAIL）；details 为空时明确提示。

5. 批次历史筛选分页
- 步骤：打开“重放批次历史”，按 `runId/status/operator/timeRange` 查询并翻页。
- 预期：请求参数透传正确，列表与分页联动。

6. 批次详情追溯
- 步骤：历史列表点“详情”。
- 预期：抽屉展示 `runId`、统计、起止时间、`errorMsg`。

7. 后端未升级降级
- 步骤：在未上线 V3 接口环境触发自动重放或查看批次历史。
- 预期：页面不崩溃，给出“后端未升级 V3”提示。

## 手工验收结果（本地）
- 当前会话未接入联调后端，以上为代码级自检项，未执行真实接口联调。
