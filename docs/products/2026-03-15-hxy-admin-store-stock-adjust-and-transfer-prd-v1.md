# HXY Admin Store Stock Adjust And Transfer PRD v1（2026-03-15）

## 1. 文档目标与边界
- 目标：把当前后台 `ADM-009` 门店 SKU 库存调整单与 `ADM-010` 门店 SKU 跨店调拨单，收口为一份可执行的正式 PRD。
- 本文覆盖：
  - 库存调整单列表、草稿创建、提交审批、审批通过、驳回、取消、详情查看
  - 跨店调拨单列表、草稿创建、提交审批、审批通过、驳回、取消、详情查看
  - 两条审批链对应的最小字段、状态机、审计口径、失败边界
- 本文不覆盖：
  - 门店商品 SPU / SKU 映射和门店价库存日常运营
  - 无审批链的手工库存直调接口 `manual-stock-adjust`
  - 四账对账、提成结算、售后退款等财务或交易域后台能力

## 2. 单一真值来源
### 2.1 页面真值
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/stockAdjustOrder/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/transferOrder/index.vue`

### 2.2 API / Controller 真值
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/product/storeSkuStockAdjustOrder.ts`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/product/storeSkuTransferOrder.ts`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/controller/admin/store/ProductStoreSkuController.java`

### 2.3 上游产品 / 架构参考
- `hxy/00_governance/HXY-架构决策记录-ADR-v1.md`
- `hxy/07_memory_archive/handoffs/2026-03-06/store-sku-stock-adjust-order-window-a.md`
- `hxy/07_memory_archive/handoffs/2026-03-06/window-a-p1-stock-transfer-four-account-summary.md`
- `docs/products/2026-03-15-hxy-admin-store-product-mapping-and-sku-ops-prd-v1.md`

## 3. 当前业务结论
1. `ADM-009` 和 `ADM-010` 都是当前后台真实运行的审批页，不是规划态能力。
2. 两条链路共用相同审批状态机：
   - `0 草稿`
   - `10 待审批`
   - `20 已通过`
   - `30 已驳回`
   - `40 已取消`
3. 库存调整单当前支持的业务类型只认页面枚举真值：
   - `REPLENISH_IN`
   - `TRANSFER_IN`
   - `TRANSFER_OUT`
   - `STOCKTAKE`
   - `LOSS`
   - `SCRAP`
4. 两个创建弹窗都会默认写入 `applySource='ADMIN_UI'`，因此当前后台产品口径应视为“管理后台人工发起的审批单”，不是系统自动发单。
5. 审批通过后的工程真值：
   - 库存调整单：复用现有库存变更主链，库存流水 `bizNo=orderNo`，`bizType=MANUAL_<业务类型>`
   - 跨店调拨单：审批通过后生成双向库存流水，源门店 `MANUAL_TRANSFER_OUT`，目标门店 `MANUAL_TRANSFER_IN`，幂等键同样固定为 `bizNo=orderNo`
6. 当前页面和 API 未核到服务端 `degraded=true / degradeReason` 语义，不能在产品文档里臆造后端降级字段。

## 4. 角色与使用场景
| 角色 | 目标 | 使用页面 |
|---|---|---|
| 门店运营 | 对单店库存做审批化增减、盘点差异更正、损耗/报废登记 | `mall/store/stockAdjustOrder/index` |
| 区域/总部审批人 | 审批库存调整单，决定通过、驳回或取消 | `mall/store/stockAdjustOrder/index` |
| 供应链运营 | 发起门店间调拨申请，跟踪源/目标门店库存调拨流转 | `mall/store/transferOrder/index` |
| 供应链审批人 | 审批跨店调拨单，判断是否放行库存出入库 | `mall/store/transferOrder/index` |

## 5. 页面与能力边界
### 5.1 库存调整单页面
页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/stockAdjustOrder/index.vue`

查询能力：
1. 订单号：`orderNo`
2. 门店：`storeId`
3. 状态：`status`
4. 业务类型：`bizType`
5. 申请人：`applyOperator`
6. 最近动作编码 / 最近动作人：`lastActionCode` `lastActionOperator`
7. 申请时间范围：`createTime[]`

创建能力：
1. 门店：`storeId`
2. 业务类型：`bizType`
3. 原因：`reason`
4. 备注：`remark?`
5. 申请来源：`applySource?`
6. 明细：`items[]:{ skuId, incrCount }`

动作能力：
1. 草稿提交：`POST /product/store-sku/stock-adjust-order/submit`
2. 审批通过：`POST /product/store-sku/stock-adjust-order/approve`
3. 驳回：`POST /product/store-sku/stock-adjust-order/reject`
4. 取消：`POST /product/store-sku/stock-adjust-order/cancel`
5. 详情查看：`GET /product/store-sku/stock-adjust-order/get`
6. 分页查询：`GET /product/store-sku/stock-adjust-order/page`

固定边界：
1. 明细中 `incrCount` 可正可负，页面以 `增加 / 扣减` 标签显示。
2. 驳回时备注必填；审批通过和取消时备注可选。
3. 当前页面只定义审批单链路，不覆盖非审批化的手工库存直调。

### 5.2 跨店调拨单页面
页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/transferOrder/index.vue`

查询能力：
1. 调拨单号：`orderNo`
2. 调出门店：`fromStoreId`
3. 调入门店：`toStoreId`
4. 状态：`status`
5. 申请人：`applyOperator`
6. 最近动作编码 / 最近动作人：`lastActionCode` `lastActionOperator`
7. 申请时间范围：`createTime[]`

创建能力：
1. 调出门店：`fromStoreId`
2. 调入门店：`toStoreId`
3. 原因：`reason`
4. 备注：`remark?`
5. 申请来源：`applySource?`
6. 明细：`items[]:{ skuId, quantity }`

动作能力：
1. 草稿提交：`POST /product/store-sku/transfer-order/submit`
2. 审批通过：`POST /product/store-sku/transfer-order/approve`
3. 驳回：`POST /product/store-sku/transfer-order/reject`
4. 取消：`POST /product/store-sku/transfer-order/cancel`
5. 详情查看：`GET /product/store-sku/transfer-order/get`
6. 分页查询：`GET /product/store-sku/transfer-order/page`

固定边界：
1. 调出门店和调入门店不能相同。
2. 明细数量 `quantity` 必须大于 `0`。
3. 当前页面定义的是“审批调拨单”，不是库存盘点、报损、财务结算页面。

## 6. 关键业务对象与字段最小集
### 6.1 库存调整单
主对象最小集：
- `id`
- `orderNo`
- `storeId` `storeName`
- `bizType`
- `status`
- `reason`
- `remark`
- `applyOperator` `applySource`
- `approveOperator` `approveTime` `approveRemark`
- `lastActionCode` `lastActionOperator` `lastActionTime`
- `detailJson`
- `createTime`

明细最小集：
- `skuId`
- `incrCount`

### 6.2 跨店调拨单
主对象最小集：
- `id`
- `orderNo`
- `fromStoreId` `fromStoreName`
- `toStoreId` `toStoreName`
- `status`
- `reason`
- `remark`
- `applyOperator` `applySource`
- `approveOperator` `approveTime` `approveRemark`
- `lastActionCode` `lastActionOperator` `lastActionTime`
- `detailJson`
- `createTime`

明细最小集：
- `skuId`
- `quantity`

## 7. 关键流程
### 7.1 库存调整单创建与提交
1. 运营在调整单页点击“新建库存调整单”。
2. 填写 `storeId`、`bizType`、`reason`、可选 `remark` 和 `applySource`。
3. 录入一组 `skuId + incrCount` 明细。
4. 调用 `POST /product/store-sku/stock-adjust-order/create` 生成草稿单。
5. 草稿确认后调用 `POST /product/store-sku/stock-adjust-order/submit` 进入待审批。

### 7.2 库存调整单审批
1. 审批人在列表筛选待审批记录。
2. 通过详情查看 `detailJson` 明细与最近动作审计。
3. 根据结论调用：
   - `approve`
   - `reject`
   - `cancel`
4. 审批通过后，由后端库存主链生成对应库存流水并更新状态。

### 7.3 跨店调拨单创建与提交
1. 运营在调拨页选择调出门店与调入门店。
2. 填写 `reason`、可选 `remark`、`applySource`。
3. 录入一组 `skuId + quantity`。
4. 调用 `POST /product/store-sku/transfer-order/create` 生成草稿。
5. 草稿确认后调用 `POST /product/store-sku/transfer-order/submit` 进入待审批。

### 7.4 跨店调拨审批与落库
1. 审批人在待审批列表查看调拨单详情。
2. 根据业务判断调用 `approve / reject / cancel`。
3. 审批通过后，系统按 `orderNo` 作为幂等键，分别落源门店和目标门店库存流水。
4. 页面最终以状态回读和详情字段回写为准，而不是只看操作提示文案。

## 8. 状态、空态、错误与降级
### 8.1 查询与空态
- 列表为空是合法空态，不是错误。
- 详情页若 `detailJson` 为空，可显示“无可用明细”；若解析失败，页面会保留原文并展示 warning。

### 8.2 创建校验
- 库存调整单：
  - `storeId` 必填
  - `bizType` 必填
  - `reason` 必填
  - 至少一条 `items`
  - `incrCount` 不能为 `0`
- 跨店调拨单：
  - `fromStoreId` 和 `toStoreId` 必填且不能相同
  - `reason` 必填
  - 至少一条 `items`
  - `quantity` 必须大于 `0`

### 8.3 写操作成功口径
- 当前 controller 对 `submit / approve / reject / cancel` 返回 `Boolean`，但产品验收不能把“接口返回 true”直接等同于业务完成。
- 后续联调、验收和 runbook 口径必须继续以“列表刷新 / 详情回读后的状态变化”为主证据。

### 8.4 降级边界
- 当前未核到服务端 `degraded=true / degradeReason` 字段。
- 如库存审批动作失败，只能按普通失败处理，不得补造“软成功”或“部分通过”状态。

## 9. 禁止性边界
1. 不得把无审批链的 `manual-stock-adjust` 直调接口写进本 PRD 主链。
2. 不得把跨店调拨单和库存调整单混写成同一业务对象。
3. 不得把审批通过的 message 文案当成库存落库完成证据，必须以回读状态为准。
4. 不得把财务、提成、四账对账话术套用到供应链库存审批页面。
5. 不得再把历史 ADR、handoff 直接当成正式产品文档替代品；本文落盘后，以本文为准。

## 10. 验收标准
### 10.1 ADM-009 门店 SKU 库存调整单
- [ ] 页面、接口、controller 边界一致
- [ ] `bizType` 固定枚举明确
- [ ] 草稿、提交、审批、驳回、取消状态机明确
- [ ] 审批通过后的库存主链与幂等口径明确

### 10.2 ADM-010 门店 SKU 跨店调拨单
- [ ] 调出 / 调入门店约束明确
- [ ] 明细字段 `skuId + quantity` 明确
- [ ] 双向库存流水口径明确
- [ ] 成功判定以写后回读为主，而不是只看 `true`

## 11. 最终结论
1. `ADM-009` 的产品真值是“门店 SKU 库存审批化调整单”，而不是手工库存直调接口。
2. `ADM-010` 的产品真值是“门店间库存调拨审批单”，而不是财务或履约补单页面。
3. 本文落盘后，`ADM-009`、`ADM-010` 在全项目业务台账中的 PRD 完整度应提升为 `完整`。
4. 后续后台文档补齐重点应继续推进：
   - `ADM-011` ~ `ADM-013` 生命周期治理 PRD
   - `ADM-014` ~ `ADM-016` 售后后台运营 PRD
