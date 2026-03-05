# Window B Handoff - 生命周期批次台账详情 + 复核执行（overlay）

## 变更范围
- 分支：`feat/ui-four-account-reconcile-ops`
- 仅修改：`overlay-vue3`、菜单 SQL、handoff
- 未改动：Java 后端、治理文档、无关未跟踪文件

## 本次交付
1. 生命周期批次台账页增强（`lifecycleBatchLog`）
- 列表筛选：`batchNo/targetLifecycleStatus/operator/source/createTime`
- 列表操作：
  - 查看详情（调用 `GET /product/store/lifecycle-batch-log/get?id=...`）
  - 复制批次号/来源号
  - 复核执行（调用 `POST /product/store/lifecycle-guard/recheck-by-batch/execute`，二次确认）
  - 跳转复核历史（携带 `batchNo/logId` query）
- 详情抽屉：
  - 基础信息、统计信息、`guardRuleVersion`
  - `guardConfigSnapshotJson` 结构化展示 + 原文保留
  - `detailJson` 结构化展示（`details/blocked/warnings`）+ 原文保留
  - `detailJson` 非法时显示“明细解析失败（原文保留）”，页面不崩
- 复核结果弹窗：展示 `total/blocked/warning` 与逐店明细（含 `blocked/warnings/guardItems`）

2. 生命周期复核历史页新增（`lifecycleRecheckLog`）
- 分页筛选：`recheckNo/logId/batchNo/targetLifecycleStatus/operator/source/createTime`
- 详情抽屉：
  - 基础信息、统计信息、`guardRuleVersion`
  - `guardConfigSnapshotJson` 结构化展示 + 原文
  - `detailJson` 结构化展示（含 `blocked/warnings/guardItems`）+ 原文
  - 异常 JSON 降级展示，不抛异常
- 支持从 query 回放筛选（`batchNo/recheckNo/logId`）

3. API 封装补齐
- `GET /product/store/lifecycle-batch-log/page`
- `GET /product/store/lifecycle-batch-log/get`
- `POST /product/store/lifecycle-guard/recheck-by-batch/execute`
- `GET /product/store/lifecycle-recheck-log/page`
- `GET /product/store/lifecycle-recheck-log/get`

4. 菜单 SQL
- 新增幂等脚本，确保门店管理下挂载：
  - `store-lifecycle-batch-log`
  - `store-lifecycle-recheck-log`
- 同步 admin/operator 角色授权

## 手工验收清单（步骤 + 预期）
1. 入口
- 步骤：进入门店管理，打开“Lifecycle Batch Log”，点击“复核历史”
- 预期：进入复核历史页，路由无死链

2. 列表筛选
- 步骤：在批次台账页按批次号/来源/时间筛选
- 预期：列表返回符合条件的数据，分页可切换

3. 批次详情
- 步骤：点击“查看详情”
- 预期：详情抽屉展示基础信息、统计、规则版本、配置快照、明细表与原文

4. 复核执行
- 步骤：点击“复核执行”并确认
- 预期：出现二次确认；执行后弹出结果（total/blocked/warning + 明细）

5. 复核历史分页
- 步骤：进入复核历史页，执行筛选 + 翻页 + 详情查看
- 预期：筛选生效，分页正常，详情可打开

6. 异常 JSON 降级
- 步骤：构造/定位 `detailJson` 或 `guardConfigSnapshotJson` 非法记录并打开详情
- 预期：显示“明细解析失败（原文保留）”或配置解析失败提示，页面不崩溃，原文仍可查看

7. 复制便捷操作
- 步骤：点击“复制批次号/复制来源号”
- 预期：复制成功提示；空值场景提示“为空，无法复制”

## 说明
- `overlay-vue3` 下无 `package.json`，未执行 pnpm lint（按项目约束）。
