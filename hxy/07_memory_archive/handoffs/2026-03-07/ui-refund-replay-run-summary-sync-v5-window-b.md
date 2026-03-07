# UI refund replay run summary + sync tickets V5 (window B)

- Date: 2026-03-07
- Branch: `feat/ui-four-account-reconcile-ops`
- Scope: overlay `refundNotifyLog/replay-run` 页面增强；不改 Java 后端

## 1. 交付内容

1. 批次汇总看板（详情抽屉内）
- 对接 `GET /booking/refund-notify-log/replay-run-log/summary?runId=xxx`
- 展示字段：
  - `runStatus/triggerSource/operator/dryRun/start/end`
  - `scanned/success/skip/fail/warning`
  - `topFailCodes/topWarningTags`（已转可读标签）
- 接口缺失降级：展示“后端版本暂不支持”，页面可继续使用。

2. 失败工单同步操作
- 新增按钮：
  - `同步失败工单`（`onlyFail=true`）
  - `同步全部明细工单`（`onlyFail=false`）
- 对接 `POST /booking/refund-notify-log/replay-run-log/sync-tickets`
- 结果展示：`attempted/success/failed/failedIds`
- 提供失败ID复制。

3. 列表筛选增强
- 重放批次历史筛选新增：
  - `triggerSource`
  - `hasWarning`
  - `minFailCount`
- 与现有分页、状态、操作人、时间范围共存。
- 时间筛选兼容：同时保留既有 `timeRange`，并附带 `startTime` 参数给新后端。

4. 降级与兼容
- `summary`、`sync-tickets` 任一接口不存在时：
  - 页面不崩溃
  - 展示“后端版本暂不支持”
  - 原有重放能力不受影响
- 空值统一 `--`
- 新增错误信息复制能力（汇总、同步、详情错误位）。

## 2. 改动文件

- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/refundNotifyLog/index.vue`

## 3. SQL

- 本批未新增 SQL。
- 原因：按钮权限沿用既有权限（`query/replay`），无需新增菜单或按钮权限。

## 4. 手工验收清单（建议）

1. 批次汇总展示
- 打开“重放批次历史” -> 任一批次“详情”
- 检查汇总字段与 Top 标签渲染
- 在旧后端验证“后端版本暂不支持”提示与页面稳定性

2. 同步工单
- 在详情中执行“同步失败工单”与“同步全部明细工单”
- 核对 `attempted/success/failed/failedIds`
- 验证失败ID复制按钮可用
- 在旧后端验证降级提示

3. 筛选
- 组合条件：`triggerSource + hasWarning + minFailCount + 时间范围`
- 确认翻页后条件保留与列表结果变化符合预期
