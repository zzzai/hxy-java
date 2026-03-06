# Window B Handoff - 退款回调补偿 V2 前端对接（overlay-vue3）

## 日期
- 2026-03-06

## 分支
- `feat/ui-four-account-reconcile-ops`

## 变更概览
1. API 升级（退款回调日志）
- 文件：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/refundNotifyLog.ts`
- `replay` 请求支持：`dryRun + ids`，并保留 `id` 兼容旧后端。
- `replay` 响应适配：`successCount/skipCount/failCount/details[]`。
- `page` 字段补齐：
  - `lastReplayOperator`
  - `lastReplayTime`
  - `lastReplayResult`
  - `lastReplayRemark`

2. 页面升级（退款回调日志管理）
- 文件：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/refundNotifyLog/index.vue`
- 新增能力：
  - 批量勾选（仅失败记录可选）
  - `预演重放（dry-run）` 按钮
  - `执行重放` 按钮（二次确认）
  - 结果弹窗：汇总 + details 明细表
  - 审计字段列展示：最近重放结果/操作人/时间/备注
  - 结果状态 Tag：`SUCCESS / SKIP / FAIL`
- 交互降级：
  - replay 异常不崩页，错误码/错误信息可读化提示
  - details 为空时给明确提示
  - 旧后端兼容：
    - dry-run 不支持时提示升级
    - execute 自动降级逐条调用旧接口（`id`）
- 空值统一：`--`

3. SQL 菜单脚本（仅补权限）
- 文件：`ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-06-hxy-booking-refund-notify-log-menu-v2.sql`
- 仅在已存在 `booking-refund-notify-log` 菜单下补齐按钮权限，不重复建菜单。
- 幂等插入：
  - `booking:refund-notify-log:query`
  - `booking:refund-notify-log:replay`

## 手工验收步骤与预期
1. 权限补齐（SQL）
- 步骤：执行 `...menu-v2.sql`。
- 预期：不新建页面菜单；仅补齐缺失按钮权限与角色绑定；重复执行无重复数据。

2. 列表字段与审计字段
- 步骤：进入退款回调日志管理页，查看列表列。
- 预期：显示 `lastReplayOperator/lastReplayTime/lastReplayResult/lastReplayRemark`；旧后端无字段时显示 `--`。

3. dry-run 预演
- 步骤：勾选失败记录，点击“预演重放（dry-run）”。
- 预期：
  - 接口请求携带 `dryRun=true + ids[]`
  - 弹窗展示 success/skip/fail 汇总
  - details 有数据时表格展示明细，空时明确提示“无重放明细”

4. 执行重放
- 步骤：勾选失败记录，点击“执行重放”。
- 预期：先二次确认；确认后请求 `dryRun=false + ids[]`；结果弹窗展示汇总与 details。

5. 失败原因可读化
- 步骤：构造失败明细（含 1030004011/4013/4014）。
- 预期：明细“结果说明”可读化展示，且保留后端原始信息。

6. 异常降级
- 步骤：
  - 场景A：replay 接口报错
  - 场景B：后端返回无 details
  - 场景C：旧后端（仅支持 `id`）
- 预期：
  - A：页面不崩，toast 提示可读
  - B：弹窗提示“后端未返回 details”
  - C：dry-run 给升级提示；执行重放自动降级逐条旧接口并给提示

## 手工验收结果（本地）
- 当前会话未连接真实业务后端，手工联调项未在本地执行。
- 建议按上述清单在联调环境逐项打钩验收。
