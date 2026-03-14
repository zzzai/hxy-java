# HXY Admin Store Product Mapping And SKU Ops PRD v1（2026-03-15）

## 1. 文档目标与边界
- 目标：把当前后台 `ADM-007` 门店商品 SPU 映射和 `ADM-008` 门店 SKU 价库存运营能力，收口为一份正式 PRD。
- 本文覆盖：
  - 门店 SPU 映射的查询、新增、编辑、删除、批量铺货
  - 门店 SKU 映射的查询、新增、编辑、删除、批量铺货、批量调整
  - 门店 SKU 价库存覆写、库存流水查询与批量重试
- 本文不覆盖：
  - 总部商品 SPU 主数据与模板生成（已由 `docs/products/2026-03-15-hxy-admin-product-spu-and-template-prd-v1.md` 负责）
  - 库存调整单审批、跨店调拨单审批
  - 门店生命周期治理
  - 小程序端商品详情、搜索、交易结算前台承接

## 2. 单一真值来源
### 2.1 页面真值
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/store/spu/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/store/sku/index.vue`

### 2.2 API 真值
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/product/storeSpu.ts`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/product/storeSku.ts`

### 2.3 Controller 真值
- `ProductStoreSpuController`
- `ProductStoreSkuController`

### 2.4 上游产品 / 架构参考
- `docs/products/2026-03-15-hxy-admin-product-spu-and-template-prd-v1.md`
- `hxy/linshi/荷小悦O2O多门店（服务+实物）SPU_SKU多门店完整结构说明（最新完整版）.md`
- `docs/plans/2026-03-01-store-sku-chain-priority-plan.md`
- `docs/plans/2026-03-01-hxy-next-delivery-plan.md`

## 3. 当前业务结论
1. 当前后台门店商品链路已经具备真实页面、真实 API 文件与真实 controller，不是规划态。
2. 当前门店商品后台必须拆成两层理解：
   - `store-spu`：决定某门店上架哪个总部 SPU
   - `store-sku`：决定该门店下具体 SKU 的销售状态、价格、库存、排序与运营备注
3. 因此，`store-spu` 是“门店卖什么”的层，`store-sku` 是“门店如何卖”的层。
4. 当前后台显式支持的商品类型仍以：
   - `1 = 实物`
   - `2 = 服务`
   为主真值。
5. `store-sku` controller 同时承载了库存调整单、跨店调拨单、库存流水批量重试等扩展能力，但本文只冻结“门店商品映射与日常价库存运营”主链，不把审批单链路混进来。

## 4. 角色与使用场景
| 角色 | 目标 | 使用页面 |
|---|---|---|
| 总部商品运营 | 把总部 SPU 铺到指定门店，控制门店商品上下架与排序 | `mall/product/store/spu/index` |
| 区域 / 门店运营 | 维护门店 SKU 的销售状态、售价、划线价、库存、备注 | `mall/product/store/sku/index` |
| 商品 / 供应链运营 | 查询库存流水、对失败流水执行批量重试 | `mall/product/store/sku/index` |

## 5. 页面与能力边界
### 5.1 门店 SPU 映射页
页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/store/spu/index.vue`

能力：
1. 分页查询：`GET /product/store-spu/page`
2. 单条查询：`GET /product/store-spu/get`
3. 保存映射：`POST /product/store-spu/save`
4. 批量铺货：`POST /product/store-spu/batch-save`
5. 删除映射：`DELETE /product/store-spu/delete`
6. 选项加载：
   - `GET /product/store-spu/store-options`
   - `GET /product/store-spu/spu-options`

页面固定动作：
1. 按 `storeId`、`spuId`、`productType`、`saleStatus` 查询。
2. 单条新增 / 编辑时，需要选择门店、商品类型、SPU、销售状态、排序、备注。
3. 批量铺货时，需要选择多个门店和单个 SPU，统一写入销售状态、排序和备注。

固定边界：
1. `store-spu` 不负责价格和库存覆写。
2. `store-spu` 的销售状态是“这个门店是否上架该 SPU”的门店级开关，不等于 SKU 级销售状态。
3. 若后续门店对同一 SPU 下的不同 SKU 需要不同价库存，应进入 `store-sku` 层处理，而不是继续往 `store-spu` 塞字段。

### 5.2 门店 SKU 运营页
页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/store/sku/index.vue`

能力：
1. 分页查询：`GET /product/store-sku/page`
2. 单条查询：`GET /product/store-sku/get`
3. 保存：`POST /product/store-sku/save`
4. 批量铺货：`POST /product/store-sku/batch-save`
5. 批量调整：`POST /product/store-sku/batch-adjust`
6. 手工库存调整（直调入口）：`POST /product/store-sku/manual-stock-adjust`
7. 库存流水分页：`GET /product/store-sku/stock-flow/page`
8. 库存流水批量重试：`POST /product/store-sku/stock-flow/batch-retry`
9. 删除：`DELETE /product/store-sku/delete`
10. 选项加载：
   - `GET /product/store-sku/store-options`
   - `GET /product/store-sku/spu-options`
   - `GET /product/store-sku/sku-options`

页面固定动作：
1. 按门店、SPU、SKU、销售状态、库存流水条件做查询。
2. 对单个门店 SKU 维护：
   - 销售状态
   - 销售价
   - 划线价
   - 库存
   - 排序
   - 备注
3. 支持对多个门店 / 单 SPU / 单 SKU 做批量铺货或批量调整。
4. 支持对库存流水失败记录做批量重试。

固定边界：
1. `store-sku` 是门店运营覆写层，不是总部主数据层。
2. `store-sku` 可以改门店销售状态、售价、库存，但不应回写总部 SPU / SKU 主数据。
3. 库存调整单、调拨单虽然共用同一个 controller，但不属于本页的主产品闭环，后续在独立 PRD 中单独定义审批状态机和风控边界。

## 6. 关键业务对象与字段最小集
### 6.1 门店 SPU 映射最小字段
以 `storeSpu.ts` 为准：
- `id?`
- `storeId`
- `storeName?`
- `spuId?`
- `spuName?`
- `productType?`
- `saleStatus?`
- `sort?`
- `remark?`
- `createTime?`
- `updateTime?`

### 6.2 门店 SPU 批量铺货最小字段
- `storeIds[]`
- `spuId?`
- `saleStatus?`
- `sort?`
- `remark?`

### 6.3 门店 SKU 映射最小字段
以 `storeSku.ts` 为准：
- `id?`
- `storeId`
- `storeName?`
- `spuId?`
- `spuName?`
- `skuId?`
- `skuSpecText?`
- `saleStatus?`
- `salePrice?`
- `marketPrice?`
- `stock?`
- `sort?`
- `remark?`
- `createTime?`
- `updateTime?`

### 6.4 门店 SKU 批量操作最小字段
#### 批量铺货
- `storeIds[]`
- `spuId?`
- `skuId?`
- `saleStatus?`
- `salePrice?`
- `marketPrice?`
- `stock?`
- `sort?`
- `remark?`

#### 批量调整
- `storeIds[]`
- `spuId?`
- `skuId?`
- `saleStatus?`
- `salePrice?`
- `marketPrice?`
- `stock?`
- `remark?`

### 6.5 手工库存调整最小字段
- `storeId`
- `bizType`
- `bizNo`
- `remark?`
- `items[]:{skuId,incrCount}`

### 6.6 库存流水最小字段
- `id?`
- `storeId?`
- `storeName?`
- `skuId?`
- `bizType?`
- `bizNo?`
- `incrCount?`
- `status?`
- `retryCount?`
- `nextRetryTime?`
- `lastErrorMsg?`
- `executeTime?`
- `lastRetryOperator?`
- `lastRetrySource?`
- `operator?`
- `source?`
- `canRetry?`
- `retryable?`
- `allowRetry?`
- `canBatchRetry?`

## 7. 关键流程
### 7.1 门店 SPU 单条映射流程
1. 运营选择门店。
2. 选择商品类型。
3. 在对应商品类型下选择总部 SPU。
4. 填写销售状态、排序、备注。
5. 调用 `POST /product/store-spu/save`。
6. 保存后，该门店获得对应总部 SPU 的门店级上架映射。

### 7.2 门店 SPU 批量铺货流程
1. 运营选择商品类型。
2. 选择多个门店。
3. 选择一个总部 SPU。
4. 统一设置销售状态、排序、备注。
5. 调用 `POST /product/store-spu/batch-save`。
6. 批量铺货完成后，目标门店都具备该 SPU 的门店映射记录。

### 7.3 门店 SKU 单条运营流程
1. 运营选择门店、SPU、SKU。
2. 设置销售状态、销售价、划线价、库存、排序、备注。
3. 调用 `POST /product/store-sku/save`。
4. 保存后，门店获得对该 SKU 的实际售卖覆写值。

### 7.4 门店 SKU 批量铺货 / 批量调整流程
1. 运营选择一组门店。
2. 选择 SPU、可选 SKU。
3. 对目标门店批量设置：销售状态、价格、库存、备注。
4. 根据意图调用：
   - `POST /product/store-sku/batch-save`
   - `POST /product/store-sku/batch-adjust`
5. 批量结果写回后，目标门店的 SKU 运营值统一更新。

### 7.5 手工库存调整流程
1. 运营选择单门店。
2. 选择 `bizType`、填写 `bizNo` 和备注。
3. 录入一组 `skuId + incrCount`。
4. 调用 `POST /product/store-sku/manual-stock-adjust`。
5. 系统写入库存流水，并按后端主链处理库存变更。

### 7.6 库存流水查询 / 重试流程
1. 运营按门店、SKU、业务类型、业务单号、状态、操作人、来源、执行时间查询流水。
2. 若流水处于失败或可重试状态，允许进入批量重试。
3. 调用 `POST /product/store-sku/stock-flow/batch-retry`。
4. 返回 `success / skipped / failed` 结构化结果。
5. 批量重试后，运营继续通过库存流水页观察最终状态。

## 8. 状态、空态、错误与降级
### 8.1 门店 SPU / SKU 查询
- 空态：
  - 列表为空是合法结果，不是错误。
  - `store-options`、`spu-options`、`sku-options` 空列表也是合法结果。
- 错误：
  - 当前页面未核到服务端 `degraded=true / degradeReason` 语义，不得臆造后端降级字段。

### 8.2 保存与批量操作
- 固定口径：
  - `save / batch-save / batch-adjust` 成功必须以接口成功返回为准。
  - 当前没有独立的“部分成功” UI 协议定义，不得把未确认完成的批量操作描述为成功。
- 禁止性口径：
  - 不得把总部主数据变更和门店运营覆写混为一个提交动作。

### 8.3 手工库存调整
- 当前口径：
  - 该接口属于运营直调入口，必须提供 `bizType` 与 `bizNo`，不能作为匿名库存改写接口使用。
- 风险边界：
  - 手工库存调整已是实际能力，但审批化链路在独立 PRD 中定义；本文不把“直调接口存在”外推成“审批链已闭环”。

### 8.4 库存流水批量重试
- 返回结构：
  - `totalCount`
  - `successCount`
  - `skippedCount`
  - `failedCount`
  - `items[]`
- 固定口径：
  - `SKIPPED` 不是失败，但也不能计作成功修复。
  - 批量重试结果必须按结构化 `SUCCESS / SKIPPED / FAILED` 解释，不能只看 message。

## 9. 禁止性边界
1. 不得把 `store-spu` 当成价格和库存运营层。
2. 不得把 `store-sku` 当成总部商品主数据层。
3. 不得把手工库存调整直调入口写成“库存审批链已闭环”。
4. 不得把库存调整单、跨店调拨单审批流程混入本 PRD 主体；它们必须在独立 PRD 中定义。
5. 不得因为库存流水支持批量重试，就把所有失败链路默认视为可自动恢复。
6. 不得在产品口径里扩写 `productType=3/4` 的门店商品后台完整支持能力，当前显式真值仍以 `1/2` 为主。

## 10. 验收标准
### 10.1 ADM-007 门店商品 SPU 映射
- [ ] 单条映射与批量铺货边界明确
- [ ] `store-spu` 只负责门店级上架映射，不负责价格库存
- [ ] 商品类型 `1/2` 的现网真值明确

### 10.2 ADM-008 门店 SKU 价库存运营
- [ ] 单条保存、批量铺货、批量调整边界明确
- [ ] 门店 SKU 覆写最小字段明确
- [ ] 库存流水查询 / 批量重试口径明确
- [ ] 手工库存调整已定义为真实运营入口，但未被误写成审批链闭环

## 11. 最终结论
1. `ADM-007` 的产品真值是“总部 SPU 到门店上架映射”的后台运营能力。
2. `ADM-008` 的产品真值是“门店 SKU 价库存覆写、库存流水观察与重试”的后台运营能力。
3. 本文落盘后，`ADM-007`、`ADM-008` 在全项目业务台账中的 PRD 完整度应提升为 `完整`。
4. 后续后台文档补齐重点应继续推进：
   - `ADM-009` / `ADM-010` 库存调整单与跨店调拨 PRD
   - `ADM-011` ~ `ADM-013` 生命周期治理 PRD
   - `ADM-014` ~ `ADM-016` 售后后台运营 PRD
