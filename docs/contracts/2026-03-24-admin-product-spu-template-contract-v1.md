# Admin Product SPU / Template Contract v1 (2026-03-24)

## 1. 目标与真值来源
- 覆盖能力：`ADM-001 总部商品 SPU 管理 / 新增编辑`、`ADM-002 商品模板校验 / SKU 自动生成`。
- 只认真实页面、真实 API 文件、真实 controller 注解路径与正式 PRD。
- 真值输入：
  - PRD：`docs/products/2026-03-15-hxy-admin-product-spu-and-template-prd-v1.md`
  - 页面：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/spu/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/spu/form/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/product/template/index.vue`
  - API：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/product/template.ts`
  - Controller：
    - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/controller/admin/spu/ProductSpuController.java`
    - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/controller/admin/template/ProductTemplateGenerateController.java`

## 2. 能力绑定矩阵

| 能力 | 页面入口 | 核心接口 / controller | 当前真值 |
|---|---|---|---|
| `ADM-001` SPU 管理 | `mall/product/spu/index`; `mall/product/spu/form/index` | `/product/spu/*`; `ProductSpuController` | 页面 / controller 已闭环；SPU CRUD 与状态管理为后台真值 |
| `ADM-002` 模板校验 / SKU 自动生成 | `mall/product/template/index` | `/product/template/*`; `ProductTemplateGenerateController` | 页面 / API / controller 已闭环；模板校验与 SKU 生成预览为后台真值 |

## 3. Canonical Interface Matrix

| 能力 | method + path | 关键 request/body 真值 | 关键 response 真值 | 合法空态 / 观察态 | 禁止误写 |
|---|---|---|---|---|---|
| `ADM-001` SPU 创建 / 编辑 | `POST /product/spu/create`; `PUT /product/spu/update` | `ProductSpuSaveReqVO` | `Long id` / `Boolean` | 无 | 不能把保存成功写成前台已上架 |
| `ADM-001` SPU 状态 / 删除 | `PUT /product/spu/update-status`; `DELETE /product/spu/delete` | `id`,`status` | `Boolean` | 无 | 不能把状态切换写成门店端全部生效 |
| `ADM-001` SPU 查询 | `GET /product/spu/get-detail`; `GET /product/spu/page`; `GET /product/spu/list`; `GET /product/spu/list-all-simple`; `GET /product/spu/get-count` | `id`,`spuIds`,`pageVO` | 详情、分页、计数映射 | 空分页 / 空列表合法 | 不能把空列表写成 controller 异常 |
| `ADM-001` SPU 导出 | `GET /product/spu/export-excel` | `ProductSpuPageReqVO` | 文件流 | 无命中时文件内容可能为空 | 不能把导出能力写成 release evidence |
| `ADM-002` 模板校验 | `POST /product/template/validate` | `categoryId`,`templateVersionId`,`items[]` | `pass`,`errors[]`,`warnings[]` | `warnings[]` 合法 | 不能把 `warnings` 写成阻断错误 |
| `ADM-002` 生成预览 | `POST /product/template/sku-generator/preview` | `spuId`,`categoryId`,`baseSku`,`specSelections[]` | `taskNo`,`combinationCount`,`truncated`,`items[]` | `truncated=true` 合法 | 不能把 preview 写成已真实落 SKU |
| `ADM-002` 提交生成 | `POST /product/template/sku-generator/commit` | `taskNo`,`idempotencyKey` | `accepted`,`idempotentHit`,`status` | `idempotentHit=true` 合法 | 不能把 accepted 写成后续页面已全部刷新 |

## 4. 边界与禁止误写
- `preview` 只表示生成组合预览，不等于 SKU 已创建。
- `commit` 返回 `accepted` 只表示后台受理，不等于门店商品、库存、价格链路已同步完成。
- `warnings[]` 属于观察态，不得直接升级为失败码，也不得反向写成“无需人工确认”。
- 本文只描述 admin 产品主数据与模板生成 contract，不替代门店 SPU / SKU 运维 contract。

## 5. 当前结论
- `ADM-001`、`ADM-002` 已具备独立后台 contract。
- 解决的是文档体系闭环，不是发布能力升级。
