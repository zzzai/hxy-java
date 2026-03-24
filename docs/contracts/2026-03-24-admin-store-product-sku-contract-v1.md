# Admin Store Product / SKU Contract v1 (2026-03-24)

## 1. 目标与真值来源
- 覆盖能力：`ADM-007 门店商品 SPU 映射 / 上下架`、`ADM-008 门店 SKU 价库存管理 / 批量调整 / 库存流水重试`。
- 真值输入：
  - PRD：`docs/products/2026-03-15-hxy-admin-store-product-mapping-and-sku-ops-prd-v1.md`
  - 页面：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/store/spu/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/store/sku/index.vue`
  - API：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/product/storeSpu.ts`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/product/storeSku.ts`
  - Controller：
    - `ProductStoreSpuController`
    - `ProductStoreSkuController`

## 2. 能力绑定矩阵

| 能力 | 页面入口 | controller / path | 当前真值 |
|---|---|---|---|
| `ADM-007` 门店商品 SPU 映射 | `mall/product/store/spu/index` | `/product/store-spu/*`; `ProductStoreSpuController` | 门店 SPU 保存、批量映射、上下架、选项拉取以此为准 |
| `ADM-008` 门店 SKU 运维 | `mall/product/store/sku/index` | `/product/store-sku/*`; `ProductStoreSkuController` | SKU 价库存保存、批量调整、手工调库存、库存流水重试以此为准 |

## 3. Canonical Interface Matrix

| 能力 | method + path | 关键 request / body 真值 | 关键 response 真值 | 合法空态 / 观察态 | 禁止误写 |
|---|---|---|---|---|---|
| `ADM-007` SPU 映射 CRUD | `POST /product/store-spu/save`; `POST /product/store-spu/batch-save`; `DELETE /product/store-spu/delete`; `GET /product/store-spu/get|page` | `storeId`,`spuId`,`saleStatus`,`sort`,`remark`,`storeIds[]` | `Long id`、`Integer affected`、详情 / 分页 | 空分页合法 | 不能把 batch-save 成功写成所有门店前台立即可售 |
| `ADM-007` 选项接口 | `GET /product/store-spu/store-options`; `GET /product/store-spu/spu-options` | `keyword`,`productType` | 选项列表 | 空列表合法 | 不能把空选项写成接口异常 |
| `ADM-008` SKU CRUD / 批量调整 | `POST /product/store-sku/save`; `POST /product/store-sku/batch-save`; `POST /product/store-sku/batch-adjust`; `DELETE /product/store-sku/delete`; `GET /product/store-sku/get|page` | `storeId`,`spuId`,`skuId`,`salePrice`,`marketPrice`,`stock`,`storeIds[]` | `Long id`、`Integer affected`、详情 / 分页 | 空分页合法 | 不能把数量返回写成库存已到账证据 |
| `ADM-008` 手工调库存 | `POST /product/store-sku/manual-stock-adjust` | `storeId`,`bizType`,`bizNo`,`items[{skuId,incrCount}]` | `Integer affected` | 无 | 不能把提交成功写成库存流水全部落库成功 |
| `ADM-008` 库存流水页 / 重试 | `GET /product/store-sku/stock-flow/page`; `POST /product/store-sku/stock-flow/batch-retry` | `storeId`,`skuId`,`bizType`,`bizNo`,`status`,`executeTime[]`,`ids[]` | `PageResult<ProductStoreSkuStockFlowRespVO>`；`success/skipped/failed` | 空流水页合法；`skipped` 合法 | 不能把 batch-retry 成功写成所有下游系统已同步 |
| `ADM-008` SKU 选项接口 | `GET /product/store-sku/store-options`; `GET /product/store-sku/spu-options`; `GET /product/store-sku/sku-options` | `keyword`,`productType`,`spuId` | 选项列表 | 空列表合法 | 不能把选项缺失写成 controller 不可用 |

## 4. 边界说明
- `batch-save` / `batch-adjust` 返回的是影响条数，不是对外销售成功数。
- `manual-stock-adjust` 与 `stock-flow/batch-retry` 只能说明后台运维动作已受理，不等于库存对账或订单链路已经无差异。
- 库存流水页里的 `canRetry / retryable / allowRetry / canBatchRetry` 是运维判断字段，不等于实际重试已经成功。

## 5. 当前结论
- `ADM-007`、`ADM-008` 已具备独立后台 contract。
- 当前仍应按 admin-only 运维能力理解，不得误写为 release-ready。
