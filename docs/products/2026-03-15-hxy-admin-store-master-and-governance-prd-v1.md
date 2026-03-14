# HXY Admin Store Master And Governance PRD v1（2026-03-15）

## 1. 文档目标与边界
- 目标：把当前后台 `ADM-003` 门店主数据管理、`ADM-004` 门店分类管理、`ADM-005` 门店标签管理、`ADM-006` 门店标签组管理，收口为一份正式 PRD。
- 本文覆盖：
  - 门店列表、详情、新增/编辑、删除
  - 门店分类、标签、标签组的治理口径
  - 门店标签批量维护、生命周期辅助只读检查、批量分类/标签/生命周期治理入口
- 本文不覆盖：
  - 生命周期变更单、生命周期批次 / 复核台账
  - 门店商品 SPU / SKU、库存调整、跨店调拨
  - 小程序侧门店曝光和预约承接页

## 2. 单一真值来源
### 2.1 页面真值
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/category/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/tag/index.vue`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/tag-group/index.vue`

### 2.2 API / Controller 真值
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/store/store.ts`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/store/category.ts`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/store/tag.ts`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/store/group.ts`
- `ProductStoreController`
- `ProductStoreCategoryController`
- `ProductStoreTagController`
- `ProductStoreTagGroupController`

### 2.3 上游产品参考
- `hxy/01_product/HXY-门店管理产品设计-万店版-v1-2026-02-28.md`
- `hxy/01_product/HXY-总部门店权责利模型-v1-2026-02-22.md`
- `docs/products/2026-03-15-hxy-admin-store-lifecycle-governance-prd-v1.md`

## 3. 当前业务结论
1. 当前后台已经具备完整的门店主数据治理壳层，不是“只有门店列表”。
2. 门店主数据的最小对象真值固定为：
   - `code` `name` `shortName`
   - `categoryId`
   - `status` `lifecycleStatus`
   - `contactName` `contactMobile`
   - `provinceCode` `cityCode` `districtCode` `address`
   - `longitude` `latitude`
   - `openingTime` `closingTime`
   - `sort` `remark`
   - `tagIds`
3. 当前后台门店治理分为四层：
   - 门店主档
   - 门店分类
   - 门店标签
   - 门店标签组
4. 标签组定义的是标签治理规则，而不是标签值本身。其当前最小规则真值为：
   - `required`
   - `mutex`
   - `editableByStore`
   - `status`
   - `sort`
5. 门店主数据页除单店 CRUD 外，还提供治理辅助能力：
   - `simple-list`
   - `tag-ids`
   - `check-launch-readiness`
   - `lifecycle-guard`
   - `batch/category`
   - `batch/tags`
   - `batch/lifecycle`
6. 这些治理接口当前是后台真实运行能力，但本文只把它们定义为“门店治理辅助入口”，不把它们扩写成生命周期审批主链。

## 4. 角色与使用场景
| 角色 | 目标 | 使用页面 |
|---|---|---|
| 总部门店运营 | 建档、维护门店基础信息和联系 / 营业信息 | `mall/store/index` |
| 区域运营 | 调整门店分类、标签、生命周期辅助治理 | `mall/store/index`; `mall/store/category/index`; `mall/store/tag/index` |
| 治理管理员 | 维护标签组规则，决定标签是否必填、互斥、可否由门店编辑 | `mall/store/tag-group/index` |
| 发布 / 审核人员 | 在门店主数据侧检查门店是否满足上线准备度或守卫要求 | `mall/store/index` |

## 5. 页面与能力边界
### 5.1 门店主数据页
页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/index.vue`

核心能力：
1. 门店分页：`GET /product/store/page`
2. 门店详情：`GET /product/store/get`
3. 门店保存：`POST /product/store/save`
4. 门店删除：`DELETE /product/store/delete`
5. 门店简表：`GET /product/store/simple-list`
6. 门店标签：`GET /product/store/tag-ids`
7. 上线准备度检查：`GET /product/store/check-launch-readiness`
8. 单门店守卫检查：`GET /product/store/lifecycle-guard`
9. 批量守卫检查：`POST /product/store/lifecycle-guard/batch`
10. 批量分类变更：`POST /product/store/batch/category`
11. 批量标签变更：`POST /product/store/batch/tags`
12. 批量生命周期治理：`POST /product/store/batch/lifecycle`

固定边界：
1. 本页是主档治理入口，不是生命周期审批页。
2. `update-lifecycle` 与批量生命周期接口属于治理辅助入口，审批化生命周期流转真值以 `ADM-011~013` 为准。
3. 上线准备度和守卫检查是只读判断，不直接替代正式放量或发布门禁。

### 5.2 门店分类页
页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/category/index.vue`

能力：
1. 列表：`GET /product/store-category/list`
2. 详情：`GET /product/store-category/get`
3. 保存：`POST /product/store-category/save`
4. 删除：`DELETE /product/store-category/delete`

最小字段：
- `code`
- `name`
- `parentId`
- `level`
- `status`
- `sort`
- `remark`

固定边界：
1. 分类是门店主档的治理字典，不是商品类目。
2. 当前文档不把门店分类外推为营销或结算口径分组。

### 5.3 门店标签页
页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/tag/index.vue`

能力：
1. 列表：`GET /product/store-tag/list`
2. 详情：`GET /product/store-tag/get`
3. 保存：`POST /product/store-tag/save`
4. 删除：`DELETE /product/store-tag/delete`

最小字段：
- `code`
- `name`
- `groupId` `groupName`
- `status`
- `sort`
- `remark`

固定边界：
1. 标签必须挂在标签组治理规则下理解，不能脱离标签组独立解释互斥 / 必填语义。
2. 标签本身是门店标识能力，不是门店商品属性或用户画像标签。

### 5.4 门店标签组页
页面：`ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/tag-group/index.vue`

能力：
1. 列表：`GET /product/store-tag-group/list`
2. 详情：`GET /product/store-tag-group/get`
3. 保存：`POST /product/store-tag-group/save`
4. 删除：`DELETE /product/store-tag-group/delete`

最小字段：
- `code`
- `name`
- `required`
- `mutex`
- `editableByStore`
- `status`
- `sort`
- `remark`

固定边界：
1. 标签组定义的是治理约束，不直接挂门店实例值。
2. `editableByStore=1` 表示允许门店侧维护，但不代表总部失去治理权。

## 6. 关键业务对象与字段最小集
### 6.1 门店主档
- `id`
- `code` `name` `shortName`
- `categoryId` `categoryName`
- `status` `lifecycleStatus`
- `contactName` `contactMobile`
- `provinceCode` `cityCode` `districtCode` `address`
- `longitude` `latitude`
- `openingTime` `closingTime`
- `sort`
- `remark`
- `tagIds`
- `createTime`

### 6.2 门店上线准备度 / 守卫响应
- `storeId`
- `ready`
- `reasons[]`
- `targetLifecycleStatus`
- `blocked`
- `blockedCode`
- `blockedMessage`
- `warnings[]`
- `guardItems[]:{ guardKey, count, mode, blocked }`

### 6.3 分类 / 标签 / 标签组
分类：
- `id` `code` `name` `parentId` `level` `status` `sort` `remark`

标签：
- `id` `code` `name` `groupId` `groupName` `status` `sort` `remark`

标签组：
- `id` `code` `name` `required` `mutex` `editableByStore` `status` `sort` `remark`

## 7. 关键流程
### 7.1 门店建档与更新
1. 运营填写门店编码、名称、简称、分类、联系人、地址和营业时间。
2. 绑定一组标签 `tagIds`。
3. 调用 `POST /product/store/save`。
4. 保存后，门店主档进入分页列表，可继续参与发布准备度和守卫检查。

### 7.2 门店分类 / 标签治理
1. 治理管理员先维护分类树、标签组和标签字典。
2. 分类和标签生效后，门店主档通过单店或批量接口继承这些治理结果。
3. 标签组约束用于统一门店标签填写规则。

### 7.3 批量门店治理
1. 区域运营选择一组门店。
2. 根据治理目标调用：
   - `batch/category`
   - `batch/tags`
   - `batch/lifecycle`
3. 批量结果写回后，再通过主档页确认最终状态。

### 7.4 上线准备度 / 守卫检查
1. 发布或上线前，运营在门店主数据页对目标门店执行准备度和守卫检查。
2. 只读返回明确显示 `ready / blocked / warnings / guardItems`。
3. 若门店未满足条件，应先回到主档、商品、库存或生命周期治理链路修正，不得绕过检查直接宣布“可上线”。

## 8. 状态、空态、错误与降级
### 8.1 主档与治理字典空态
- 门店、分类、标签、标签组列表为空都是合法空态。
- `simple-list`、`tag-ids` 返回空列表也属于合法结果，不表示接口异常。

### 8.2 写操作口径
- `save / delete / batch/*` 成功必须以接口成功返回和列表 / 详情回读为准。
- 当前未核到统一服务端 `degraded=true / degradeReason` 返回，不能补造后端降级字段。

### 8.3 守卫与准备度检查
- `check-launch-readiness` 只回答“当前是否具备上线准备度”，不自动触发修复动作。
- `lifecycle-guard` 只回答守卫命中，不等于生命周期审批已经完成。

## 9. 禁止性边界
1. 不得把门店主档页写成生命周期审批页或供应链运营页。
2. 不得把门店分类、标签、标签组误写成商品类目、用户标签或营销标签。
3. 不得把守卫检查、准备度检查结果直接当成放量批准结论。
4. 不得把门店批量治理接口写成“无审计批量脚本”；它们仍属于后台受控治理入口。
5. 本文落盘后，不再以旧产品设计文档单独充当 `ADM-003~006` 的正式 PRD 替代品。

## 10. 验收标准
### 10.1 ADM-003 门店主数据管理
- [ ] 门店主档字段最小集明确
- [ ] 单店 CRUD 与批量治理边界明确
- [ ] 上线准备度 / 守卫检查边界明确

### 10.2 ADM-004 门店分类管理
- [ ] 分类字典字段和层级边界明确
- [ ] 分类与商品类目不混淆

### 10.3 ADM-005 门店标签管理
- [ ] 标签字段和标签组依赖关系明确
- [ ] 标签与用户 / 商品标签边界明确

### 10.4 ADM-006 门店标签组管理
- [ ] `required / mutex / editableByStore` 三类治理约束明确
- [ ] 标签组是治理规则层，不误写成实例值层

## 11. 最终结论
1. `ADM-003` 的产品真值是“门店主档与治理辅助入口”。
2. `ADM-004` 的产品真值是“门店分类字典治理”。
3. `ADM-005` 的产品真值是“门店标签字典治理”。
4. `ADM-006` 的产品真值是“门店标签组约束治理”。
5. 本文落盘后，`ADM-003` ~ `ADM-006` 在全项目业务台账中的 PRD 完整度应提升为 `完整`。
