# Admin Supply Chain Stock Approval Contract v1 (2026-03-24)

## 1. 目标与真值来源
- 覆盖能力：`ADM-009 门店库存调整单审批`、`ADM-010 跨店调拨单审批`。
- 真值输入：
  - PRD：`docs/products/2026-03-15-hxy-admin-store-stock-adjust-and-transfer-prd-v1.md`
  - 页面：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/stockAdjustOrder/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/transferOrder/index.vue`
  - API：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/product/storeSkuStockAdjustOrder.ts`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/product/storeSkuTransferOrder.ts`
  - Controller：`ProductStoreSkuController`

## 2. 能力绑定矩阵

| 能力 | 页面入口 | controller / path | 当前真值 |
|---|---|---|---|
| `ADM-009` 库存调整单审批 | `mall/store/stockAdjustOrder/index` | `/product/store-sku/stock-adjust-order/*` | 创建、提交、审批、驳回、取消、分页、详情都以此为准 |
| `ADM-010` 跨店调拨单审批 | `mall/store/transferOrder/index` | `/product/store-sku/transfer-order/*` | 创建、提交、审批、驳回、取消、分页、详情都以此为准 |

## 3. Canonical Interface Matrix

| 能力 | method + path | 关键 request / body 真值 | 关键 response 真值 | 合法空态 / 观察态 | 禁止误写 |
|---|---|---|---|---|---|
| `ADM-009` 调整单创建 / 提交 / 审批 / 驳回 / 取消 | `POST /product/store-sku/stock-adjust-order/create|submit|approve|reject|cancel` | `storeId`,`bizType`,`reason`,`items[{skuId,incrCount}]`,`id`,`remark` | `Long id` / `Boolean` | 无 | 不能把 `Boolean=true` 写成库存流水已全部执行成功 |
| `ADM-009` 调整单查询 | `GET /product/store-sku/stock-adjust-order/page|get` | `orderNo`,`storeId`,`status`,`bizType`,`applyOperator`,`createTime[]`,`id` | 分页 / 详情 | 空分页合法 | 不能把无单据写成错误 |
| `ADM-010` 调拨单创建 / 提交 / 审批 / 驳回 / 取消 | `POST /product/store-sku/transfer-order/create|submit|approve|reject|cancel` | `fromStoreId`,`toStoreId`,`reason`,`items[{skuId,quantity}]`,`id`,`remark` | `Long id` / `Boolean` | 无 | 不能把接口成功写成两边门店库存已核平 |
| `ADM-010` 调拨单查询 | `GET /product/store-sku/transfer-order/page|get` | `orderNo`,`fromStoreId`,`toStoreId`,`status`,`applyOperator`,`createTime[]`,`id` | 分页 / 详情 | 空分页合法 | 不能把详情存在写成履约已经完成 |

## 4. 边界说明
- 调整单 / 调拨单的 `detailJson`、`lastActionCode`、`approveTime` 是后台审批观察字段，不是前台履约证据。
- 创建或审批返回成功后，仍需以页面详情和后续库存流水观察为准，不能只看布尔值。
- 本文只覆盖审批单据 contract，不替代门店 SKU 主数据运维 contract。

## 5. 当前结论
- `ADM-009`、`ADM-010` 已具备独立后台 contract。
- 这不等于库存链路已经具备 release-ready 证据。
