# Admin Store Master Contract v1 (2026-03-24)

## 1. 目标与真值来源
- 覆盖能力：`ADM-003 门店主数据管理`、`ADM-004 门店分类管理`、`ADM-005 门店标签管理`、`ADM-006 门店标签组管理`。
- 真值输入：
  - PRD：`docs/products/2026-03-15-hxy-admin-store-master-and-governance-prd-v1.md`
  - 页面：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/category/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/tag/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/store/tag-group/index.vue`
  - API：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/store/store.ts`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/store/category.ts`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/store/tag.ts`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/store/group.ts`
  - Controller：
    - `ProductStoreController`
    - `ProductStoreCategoryController`
    - `ProductStoreTagController`
    - `ProductStoreTagGroupController`

## 2. 能力绑定矩阵

| 能力 | 页面入口 | controller / path | 当前真值 |
|---|---|---|---|
| `ADM-003` 门店主数据 | `mall/store/index` | `/product/store/*`; `ProductStoreController` | 门店 CRUD、simple-list、门店就绪度与生命周期守卫都以此为准 |
| `ADM-004` 门店分类 | `mall/store/category/index` | `/product/store-category/*`; `ProductStoreCategoryController` | 分类列表 / 保存 / 删除已成后台真值 |
| `ADM-005` 门店标签 | `mall/store/tag/index` | `/product/store-tag/*`; `ProductStoreTagController` | 标签列表 / 保存 / 删除已成后台真值 |
| `ADM-006` 门店标签组 | `mall/store/tag-group/index` | `/product/store-tag-group/*`; `ProductStoreTagGroupController` | 标签组列表 / 保存 / 删除已成后台真值 |

## 3. Canonical Interface Matrix

| 能力 | method + path | 关键 request / body 真值 | 关键 response 真值 | 合法空态 / 观察态 | 禁止误写 |
|---|---|---|---|---|---|
| `ADM-003` 门店 CRUD | `POST /product/store/save`; `DELETE /product/store/delete`; `GET /product/store/get`; `GET /product/store/page` | `HxyStore` / `id` / 分页过滤 | `Long id`、`Boolean`、详情 / 分页 | 空分页合法 | 不能把保存成功写成门店已上线 |
| `ADM-003` 辅助查询 | `GET /product/store/simple-list`; `GET /product/store/tag-ids` | `keyword`,`storeId` | 简单列表、tag id 列表 | 空列表合法 | 不能把 simple-list 空态写成错误 |
| `ADM-003` 生命周期守卫 / 就绪度 | `GET /product/store/check-launch-readiness`; `GET /product/store/lifecycle-guard`; `POST /product/store/lifecycle-guard/batch` | `id`,`lifecycleStatus`,`storeIds[]` | `ready`,`reasons[]`,`blocked`,`warnings[]`,`guardItems[]` | `ready=false`、`blocked=false` 都合法 | 不能把就绪检查通过写成可放量结论 |
| `ADM-003` 批量更新 | `POST /product/store/batch/category`; `POST /product/store/batch/tags`; `POST /product/store/batch/lifecycle`; `POST /product/store/batch/lifecycle/execute` | `storeIds[]`,`categoryId`,`tagIds[]`,`lifecycleStatus`,`reason` | `Boolean` / 执行结果 | 无命中或 warnings 合法 | 不能把批量执行成功写成所有门店前台已同步 |
| `ADM-004` 分类 | `GET /product/store-category/list|get`; `POST /product/store-category/save`; `DELETE /product/store-category/delete` | `code`,`name`,`parentId`,`status` | 列表 / 详情 / `Long id` / `Boolean` | 空列表合法 | 不能把删除成功写成已无门店引用 |
| `ADM-005` 标签 | `GET /product/store-tag/list|get`; `POST /product/store-tag/save`; `DELETE /product/store-tag/delete` | `code`,`name`,`groupId`,`status` | 列表 / 详情 / `Long id` / `Boolean` | 空列表合法 | 不能把标签保存写成门店已批量打标 |
| `ADM-006` 标签组 | `GET /product/store-tag-group/list|get`; `POST /product/store-tag-group/save`; `DELETE /product/store-tag-group/delete` | `code`,`name`,`required`,`mutex`,`editableByStore` | 列表 / 详情 / `Long id` / `Boolean` | 空列表合法 | 不能把标签组配置写成门店端已即时生效 |

## 4. 边界说明
- `check-launch-readiness.ready=true` 只表示门店主数据满足当前检查项，不等于门店交易、库存、预约链路全部可发布。
- `lifecycle-guard` 的 `warnings[]`、`guardItems[]` 是观察信息，不是服务端降级协议。
- 分类、标签、标签组保存成功只代表后台主数据保存成功，不等于历史门店数据已自动回填。

## 5. 当前结论
- `ADM-003` ~ `ADM-006` 已具备独立后台 contract。
- 当前解决的是 store master 文档真值缺口，不改变发布门禁结论。
