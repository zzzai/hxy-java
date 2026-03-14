# HXY Admin Product SPU And Template PRD v1（2026-03-15）

## 1. 文档目标与边界
- 目标：把当前后台 `ADM-001` 总部商品 SPU 管理与 `ADM-002` 商品模板 / SKU 自动生成能力，收口为一份可执行的正式 PRD。
- 本文覆盖：
  - 总部商品 SPU 列表、上下架、删除、新增、编辑、详情回显
  - 服务商品 / 实物商品的后台运行时分流
  - 类目模板校验、SKU 组合预览、SKU 生成提交联调台
- 本文不覆盖：
  - 门店商品 SPU / SKU 映射、门店价库存运营
  - 库存调整单、跨店调拨
  - 小程序商品详情、搜索、商品列表前台承接
  - 类目、品牌、属性后台独立产品文档；本文只在 SPU 主链里引用其最小必需字段

## 2. 单一真值来源
### 2.1 页面真值
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/spu/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/spu/form/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/template/index.vue`

### 2.2 API / Controller 真值
- SPU 页面当前在 `overlay` 审查范围内主要依赖页面内直调 `request`：
  - `/product/spu/*`
  - `/product/service-spu/*`
  - `/product/physical-spu/*`
- 模板页面当前可核到独立 API 文件：
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/product/template.ts`
- 后端 controller：
  - `ProductSpuController`
  - `ProductServiceSpuController`
  - `ProductPhysicalSpuController`
  - `ProductTemplateGenerateController`

### 2.3 上游产品 / 架构参考
- `hxy/01_product/HXY-瑞幸双商品体系对标-RuoYi落地方案-v1-2026-02-22.md`
- `hxy/linshi/荷小悦O2O多门店（服务+实物）SPU_SKU多门店完整结构说明（最新完整版）.md`
- `docs/plans/2026-03-01-hxy-sku-template-api-draft.md`
- `docs/plans/2026-03-02-p0-template-booking-ci-design.md`

## 3. 当前业务结论
1. 当前后台已经存在真实 SPU 管理页面与真实模板联调页，不是规划态。
2. 当前 SPU 后台采用“运行时 baseUrl 分流”：
   - 默认：`/product/spu`
   - 服务商品：`/product/service-spu`
   - 实物商品：`/product/physical-spu`
3. 当前后台页面对 `productType` 的显式运行时支持只核到：
   - `1 = 实物`
   - `2 = 服务`
4. 虽然保存 VO 允许 `productType=3/4`（卡项 / 虚拟），但当前 overlay 页面没有单独路由、单独页面模式或明确交互真值，不得在产品口径里假写成现网已完整支持。
5. 模板页当前定位是“联调台 / 操作台”，不是已嵌入 SPU 表单的强制前置步骤。
6. 因此，当前正式产品口径应固定为：
   - SPU 管理：已是后台真实运行能力
   - 模板与 SKU 自动生成：已是后台真实联调能力
   - 但“模板生成已完全嵌入商品主流程”这一说法当前不成立

## 4. 角色与使用场景
| 角色 | 目标 | 使用页面 |
|---|---|---|
| 总部商品运营 | 维护商品主数据、上下架、删除、查看分组统计 | `mall/product/spu/index` |
| 总部商品运营 | 创建 / 编辑单个 SPU，维护基础信息、SKU、配送、详情与其它设置 | `mall/product/spu/form` |
| 商品模板 / 类目治理运营 | 校验类目模板约束，预览规格组合，提交 SKU 生成任务 | `mall/product/template/index` |
| 技术 / 联调支持 | 验证模板版本、规格组合、幂等提交与预览任务行为 | `mall/product/template/index` |

## 5. 页面与能力边界
### 5.1 SPU 列表页
页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/spu/index.vue`

能力：
1. 查询 SPU 列表：`GET {baseUrl}/page`
2. 查询 tabs 计数：`GET {baseUrl}/get-count`
3. 上下架 / 入仓 / 回收站状态切换：`PUT {baseUrl}/update-status`
4. 删除：`DELETE {baseUrl}/delete?id={id}`
5. 新增 / 编辑跳转：进入 SPU 表单页

运行时分流：
- 若路由命中服务商品页，`baseUrl = /product/service-spu`
- 若路由命中实物商品页，`baseUrl = /product/physical-spu`
- 其他默认 `baseUrl = /product/spu`

固定边界：
1. 当前列表页真值是“同一套页面 + 运行时路径分流”，不是三套完全独立页面。
2. 页面内存在 `@/api/mall/product/spu` 与 `@/api/mall/product/category` 引用，但在当前 `overlay` 审查范围内未核到对应本地 API 文件；因此本文以页面实际请求路径为准，不冻结不存在的本地 API 文件路径。
3. `productType=3/4` 没有单独页面模式，不得在本页产品文档中扩写成卡项 / 虚拟商品已完整支持。

### 5.2 SPU 表单页
页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/spu/form/index.vue`

表单分区：
1. 商品信息
2. 商品规格
3. 配送设置
4. 商品详情
5. 其它设置

能力：
1. 详情回显：`GET {baseUrl}/get-detail?id={id}`
2. 创建：`POST {baseUrl}/create`
3. 更新：`PUT {baseUrl}/update`
4. 通过 query / route runtime 写入 `productType`

提交前固定动作：
1. 校验各分区表单
2. 校验 `name` 非空
3. 将所有 SKU 名称回填为 SPU 名称
4. 价格相关字段由元转分
5. 轮播图对象转 URL 数组
6. 若路由带 `productType`，写入请求体

固定边界：
1. 表单当前没有把模板校验 / SKU 自动生成内嵌为必经流程。
2. 因此“保存 SPU 前必须走模板校验页”当前不成立。
3. 若后续需要把模板校验变成主流程，必须新增页面集成设计，不得直接把现有联调台等同为正式嵌入流程。

### 5.3 模板联调台
页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/template/index.vue`

页面固定分成三段：
1. 模板校验
2. SKU 预览
3. SKU 提交

能力：
1. 模板校验：`POST /product/template/validate`
2. SKU 预览：`POST /product/template/sku-generator/preview`
3. SKU 提交：`POST /product/template/sku-generator/commit`

页面当前输入模型：
- 基础输入：`categoryId`、`templateVersionId`、`spuId`
- 模板项：`attributeId`、`attrRole`、`required`、`affectsPrice`、`affectsStock`
- 预览基础 SKU：`price`、`marketPrice`、`costPrice`、`stock`
- 规格维度：`attributeId + optionIds[]`
- 提交字段：`taskNo`、`idempotencyKey`

固定边界：
1. 该页当前是联调台，不是面向门店运营的大规模批量配置页。
2. 该页主要服务于总部商品治理、模板规则验证、SKU 生成预演与提交。
3. 本页当前没有 `degraded` 返回字段，也没有“预览成功但后台异步慢慢完成”的弱一致 UI 设计；提交结果以返回 `accepted/status/idempotentHit` 为准。

## 6. 关键业务对象与字段最小集
### 6.1 SPU 保存最小字段
以 `ProductSpuSaveReqVO` 为准，正式 PRD 最小集固定为：
- 基础字段：
  - `id?`
  - `name`
  - `keyword`
  - `introduction`
  - `description`
  - `categoryId`
  - `brandId`
  - `picUrl`
  - `sliderPicUrls[]`
  - `sort`
- 类型 / 模板：
  - `productType?`
  - `templateVersionId?`
- 配送：
  - `deliveryTypes[]`
  - `deliveryTemplateId?`
- 营销 / 统计：
  - `giveIntegral`
  - `subCommissionType`
  - `virtualSalesCount?`
  - `salesCount?`
  - `browseCount?`
- SKU：
  - `specType`
  - `skus[]`

### 6.2 SKU 表单最小字段
以当前表单回显和提交前处理为准，最小字段为：
- `name`
- `price`
- `marketPrice`
- `costPrice`
- `barCode`
- `picUrl`
- `stock`
- `weight`
- `volume`
- `firstBrokeragePrice`
- `secondBrokeragePrice`

### 6.3 模板校验最小字段
- 请求：
  - `categoryId`
  - `templateVersionId?`
  - `items[]`
- `items[]` 最小字段：
  - `attributeId`
  - `attrRole`
  - `required`
  - `affectsPrice`
  - `affectsStock`
- 返回：
  - `pass`
  - `errors[]:{code,message}`
  - `warnings[]:{code,message}`

### 6.4 SKU 预览最小字段
- 请求：
  - `spuId`
  - `categoryId`
  - `templateVersionId?`
  - `baseSku:{price,marketPrice,costPrice,stock}`
  - `specSelections[]:{attributeId,optionIds[]}`
- 返回：
  - `taskNo`
  - `combinationCount`
  - `truncated`
  - `items[]:{specHash,specSummary,existsSkuId?,suggestedSku}`

### 6.5 SKU 提交最小字段
- 请求：
  - `taskNo`
  - `idempotencyKey`
- 返回：
  - `taskNo`
  - `status`
  - `accepted`
  - `idempotentHit`

## 7. 关键流程
### 7.1 SPU 列表管理流程
1. 运营进入 SPU 列表页。
2. 页面根据当前路由解析 `baseUrl`。
3. 拉取分页数据与 tabs 统计。
4. 运营可做：筛选、查看、上下架、删除、进入编辑。
5. 状态变更成功后，列表与 tabs 计数一并刷新。

### 7.2 SPU 新增 / 编辑流程
1. 运营进入表单页。
2. 若有 `id`，先回显详情。
3. 运营完成信息、规格、配送、详情、其它设置。
4. 提交前完成字段校验、图片归一、金额单位转换、SKU 名称回填。
5. 根据当前运行时 `baseUrl` 调用 create / update。

### 7.3 模板校验流程
1. 运营填写 `categoryId` 和模板项列表。
2. 调用 `POST /product/template/validate`。
3. 页面按返回 `pass/errors/warnings` 展示结果。
4. `pass=false` 时，属于业务校验失败，不等于系统异常。

### 7.4 SKU 预览流程
1. 运营填写 `spuId`、`categoryId`、可选 `templateVersionId`。
2. 填写基础 SKU 价格 / 成本 / 库存。
3. 填写至少一个规格维度及选项集合。
4. 调用 `POST /product/template/sku-generator/preview`。
5. 返回 `taskNo`、组合数、是否截断、组合建议。
6. 页面将 `taskNo` 回写到提交区。

### 7.5 SKU 提交流程
1. 必须先有 `taskNo`。
2. 必须有 `idempotencyKey`。
3. 调用 `POST /product/template/sku-generator/commit`。
4. 返回 `accepted=true` 表示已受理。
5. 若 `idempotentHit=true`，表示命中已有提交任务，不应当被当成失败。

## 8. 状态、空态、错误与降级
### 8.1 SPU 列表 / 表单
- 空态：
  - 列表为空是合法结果，不是异常。
- 错误：
  - 表单字段校验失败时，阻断提交。
  - `name` 为空时，前端直接 fail-close，提示“商品名称不能为空”。
- 降级：
  - 当前未核到服务端 `degraded=true / degradeReason` 语义；不得在 PRD 中臆造服务端降级字段。

### 8.2 模板校验
- 空态：
  - 无错误、无告警是合法结果。
- 错误：
  - `CATEGORY_TEMPLATE_NOT_EXISTS(1008015000)`
  - `CATEGORY_TEMPLATE_VERSION_SNAPSHOT_REQUIRED(1008015013)`
  - `CATEGORY_TEMPLATE_SKU_SPEC_AFFECT_FLAG_INVALID(1008015011)`
  - `CATEGORY_TEMPLATE_NON_SPEC_AFFECT_FORBIDDEN(1008015012)`
  - `CATEGORY_TEMPLATE_SKU_SPEC_DATA_TYPE_INVALID(1008015007)`
  - `CATEGORY_TEMPLATE_SERVICE_STOCK_AFFECT_FORBIDDEN(1008015008)`
- 固定口径：
  - 校验失败应展示结构化错误列表，不得只吐 message 文本后让运营自行猜测。

### 8.3 SKU 预览
- 空态：
  - 规格组合为空不是合法成功，必须阻断并提示。
- 错误：
  - `SKU_GENERATE_SPEC_SELECTION_EMPTY(1008015009)`
  - `SKU_GENERATE_COMBINATION_EXCEED_LIMIT(1008015002)`
  - `CATEGORY_TEMPLATE_NOT_EXISTS(1008015000)`
  - `CATEGORY_TEMPLATE_VERSION_SNAPSHOT_REQUIRED(1008015013)`
- 特殊语义：
  - `truncated=true` 表示组合数超出预览上限，但预览结果仍可展示截断后的组合，不是系统故障。

### 8.4 SKU 提交
- 错误：
  - `SKU_GENERATE_PREVIEW_TASK_REQUIRED(1008015006)`
  - `SKU_GENERATE_TASK_STATUS_INVALID(1008015004)`
- 幂等：
  - `idempotentHit=true` 属于可接受成功态，不是失败态。
- 固定口径：
  - 提交返回 `accepted=true` 仅表示提交任务被受理或命中幂等，不等于“所有目标 SKU 已经人工确认无误”。

## 9. 禁止性边界
1. 不得把模板联调台写成“SPU 表单保存前强制步骤”，因为当前代码真值不是这样。
2. 不得把 `productType=3/4` 写成后台当前已完整支持的页面能力。
3. 不得把不存在于当前 `overlay` 审查范围内的 `@/api/mall/product/spu`、`category` 本地文件路径当成正式真值文件冻结。
4. 不得把 `idempotentHit=true` 当作失败处理。
5. 不得把 `truncated=true` 当成预览失败。
6. 不得臆造 `degraded=true / degradeReason` 服务端字段。

## 10. 验收标准
### 10.1 ADM-001 总部商品 SPU 管理
- [ ] 能区分默认 / 服务 / 实物三种运行时 `baseUrl`
- [ ] 列表分页、tabs 计数、状态切换、删除、编辑跳转口径清晰
- [ ] 表单最小字段和提交前处理动作明确
- [ ] `productType=3/4` 未被误写成当前已完整支持

### 10.2 ADM-002 模板 / SKU 自动生成
- [ ] 模板校验、SKU 预览、SKU 提交三段流程边界明确
- [ ] `taskNo`、`idempotencyKey`、`truncated`、`idempotentHit` 语义明确
- [ ] 结构化错误码与 UI 动作对齐
- [ ] 未把模板联调台误写成商品主流程强制节点

## 11. 最终结论
1. `ADM-001` 与 `ADM-002` 当前都应视为后台真实存在的能力，不再停留在历史设计稿。
2. `ADM-001` 的产品真值是“SPU 管理页面 + 表单页面 + 运行时按商品类型分流的后台管理能力”。
3. `ADM-002` 的产品真值是“模板校验 + SKU 生成预览 + 幂等提交的后台联调能力”。
4. 本文落盘后，`ADM-001`、`ADM-002` 在全项目业务台账中的 PRD 完整度应提升为 `完整`。
5. 后续补文档的重点应转向：
   - `ADM-007` / `ADM-008` 门店商品 SPU / SKU 运营
   - `ADM-009` / `ADM-010` 库存调整与跨店调拨
   - `ADM-011` ~ `ADM-016` 生命周期与售后运营后台
