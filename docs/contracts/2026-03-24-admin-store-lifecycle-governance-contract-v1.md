# Admin Store Lifecycle Governance Contract v1 (2026-03-24)

## 1. 目标与真值来源
- 覆盖能力：`ADM-011 门店生命周期批次执行日志 / 批次复核执行`、`ADM-012 门店生命周期变更单`、`ADM-013 门店生命周期复核日志`。
- 真值输入：
  - PRD：`docs/products/2026-03-15-hxy-admin-store-lifecycle-governance-prd-v1.md`
  - 页面：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/lifecycleBatchLog/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/lifecycleChangeOrder/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/lifecycleRecheckLog/index.vue`
  - API：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/store/lifecycleBatchLog.ts`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/store/lifecycleChangeOrder.ts`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/store/lifecycleRecheckLog.ts`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/store/store.ts`
  - Controller：
    - `ProductStoreLifecycleBatchLogController`
    - `ProductStoreController`
    - `ProductStoreLifecycleRecheckLogController`

## 2. 能力绑定矩阵

| 能力 | 页面入口 | controller / path | 当前真值 |
|---|---|---|---|
| `ADM-011` 生命周期批次执行日志 / 按批次复核执行 | `mall/store/lifecycleBatchLog/index` | `/product/store/lifecycle-batch-log/*`; `/product/store/lifecycle-guard/recheck-by-batch/execute` | 批次台账、守卫快照、按批次复核执行以此为准 |
| `ADM-012` 生命周期变更单 | `mall/store/lifecycleChangeOrder/index` | `/product/store/lifecycle-change-order/*`; `ProductStoreController` | 创建、提交、审批、驳回、取消、分页、详情以此为准 |
| `ADM-013` 生命周期复核日志 | `mall/store/lifecycleRecheckLog/index` | `/product/store/lifecycle-recheck-log/*` | 复核日志分页与详情以此为准 |

## 3. Canonical Interface Matrix

| 能力 | method + path | 关键 request / body 真值 | 关键 response 真值 | 合法空态 / 观察态 | 禁止误写 |
|---|---|---|---|---|---|
| `ADM-011` 批次日志查询 | `GET /product/store/lifecycle-batch-log/page|get` | `batchNo`,`targetLifecycleStatus`,`operator`,`source`,`createTime[]`,`id` | 分页、详情、`detailView`,`detailParseError` | 空分页合法；`detailParseError=true` 也是观察态 | 不能把日志存在写成生命周期已生效 |
| `ADM-011` 按批次复核执行 | `POST /product/store/lifecycle-guard/recheck-by-batch/execute` | `logId` 或 `batchNo` | `recheckNo`,`blockedCount`,`warningCount`,`details[]` | `blockedCount=0` 合法 | 不能把复核执行成功写成门店已可放量 |
| `ADM-012` 变更单主链 | `POST /product/store/lifecycle-change-order/create|submit|approve|reject|cancel`; `GET /product/store/lifecycle-change-order/get|page` | `storeId`,`toLifecycleStatus`,`reason`,`id`,`remark` | `Long id`、`Boolean`、详情 / 分页 | 空分页合法 | 不能把布尔成功写成门店交易链路同步完成 |
| `ADM-013` 复核日志查询 | `GET /product/store/lifecycle-recheck-log/page|get` | `recheckNo`,`logId`,`batchNo`,`targetLifecycleStatus`,`operator`,`source`,`createTime[]`,`id` | 分页 / 详情 / `details[]` | 空分页合法 | 不能把复核日志样本当 release evidence |

## 4. 边界说明
- `blocked`,`warnings[]`,`guardItems[]` 是治理观察字段，不是对外发布码。
- `detailParseError=true` 说明明细解析退化，不能直接写成 controller 不可用。
- 生命周期变更单成功只代表后台状态机动作受理成功，不等于门店交易、预约、库存链路全部完成切换。

## 5. 当前结论
- `ADM-011` ~ `ADM-013` 已具备独立后台 contract。
- 本文收口的是治理 contract，不改变 release-ready 结论。
