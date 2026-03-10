# MiniApp Product Catalog Contract v1 (2026-03-10)

## 1. 目标与真值来源
- 目标：冻结商品目录域当前真实 `controllerPath + method + path + request/response`，覆盖类目、商品列表/详情、评价、收藏、浏览历史，并明确 `search-lite` 与缺失 controller 的规划路径边界。
- 约束：
  - 只认当前前端 API 文件、当前页面调用、当前 app controller。
  - 不允许 wildcard API、`TBD_*`、基于返回文案的分支判断。
  - 没有真实 app controller 的路径固定记为 `BLOCKED`。
- 真值输入：
  - 前端 API：
    - `yudao-mall-uniapp/sheep/api/product/category.js`
    - `yudao-mall-uniapp/sheep/api/product/spu.js`
    - `yudao-mall-uniapp/sheep/api/product/comment.js`
    - `yudao-mall-uniapp/sheep/api/product/favorite.js`
    - `yudao-mall-uniapp/sheep/api/product/history.js`
  - 前端调用：
    - `/pages/index/category.vue`
    - `/pages/goods/list.vue`
    - `/pages/goods/index.vue`
    - `/pages/goods/comment/list.vue`
    - `/pages/user/goods-collect.vue`
    - `/pages/user/goods-log.vue`
    - `pages/chat/components/select-popup.vue`
  - 后端 controller：
    - `AppCategoryController`
    - `AppProductSpuController`
    - `AppProductCommentController`
    - `AppFavoriteController`
    - `AppProductBrowseHistoryController`
  - 文档基线：
    - `docs/products/miniapp/2026-03-09-miniapp-search-discovery-prd-v1.md`
    - `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
    - `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`
    - `docs/plans/2026-03-10-miniapp-product-catalog-kpi-and-alerting-v1.md`

## 2. 状态定义
- `ACTIVE`：当前前端真实绑定且允许进入当前发布口径。
- `ACTIVE_BE_ONLY`：后端真实存在，但当前前端未绑定。
- `PLANNED_RESERVED`：当前接口真实存在，但 detail/comment/collect/history 套件仍未进入正式发布冻结集。
- `BLOCKED`：当前文档/规划路径存在，但本分支没有真实 app controller，禁止进入 `ACTIVE`。

## 3. 页面与前端调用方

| 页面/组件 | 前端调用方 |
|---|---|
| `/pages/index/category.vue` | `CategoryApi.getCategoryList`、`SpuApi.getSpuPage` |
| `/pages/goods/list.vue`、`/pages/activity/index.vue`、`/pages/commission/goods.vue` | `SpuApi.getSpuPage` |
| `/pages/goods/index.vue`、`/pages/goods/point.vue`、`/pages/goods/groupon.vue`、`/pages/goods/seckill.vue` | `SpuApi.getSpuDetail` |
| `/pages/index/cart.vue`、`sheep/components/s-goods-card.vue`、`sheep/components/s-goods-shelves.vue` | `SpuApi.getSpuListByIds` |
| `/pages/goods/comment/list.vue`、`pages/goods/components/detail/detail-comment-card.vue` | `CommentApi.getCommentPage` |
| `/pages/user/goods-collect.vue`、`pages/goods/components/detail/detail-tabbar.vue` | `FavoriteApi.getFavoritePage`、`FavoriteApi.isFavoriteExists`、`FavoriteApi.createFavorite`、`FavoriteApi.deleteFavorite` |
| `/pages/user/goods-log.vue`、`pages/chat/components/select-popup.vue` | `SpuHistoryApi.getBrowseHistoryPage`、`SpuHistoryApi.deleteBrowseHistory`、`SpuHistoryApi.cleanBrowseHistory` |

## 4. Product Catalog Canonical Contract

| 场景 | 页面/前端调用方 | controllerPath | method + path | request params/body/query | response 字段 | canonical errorCode | 状态 | failureMode | retryClass | degrade 语义 | 发布口径 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 获取类目列表 | `/pages/index/category.vue` / `CategoryApi.getCategoryList` | `AppCategoryController#getProductCategoryList` | `GET /product/category/list` | 无 | `list[]:{id,parentId,name,picUrl}` | `-` | `ACTIVE` | `FAIL_OPEN` | `REFRESH_ONCE` | 空列表为合法空态；无服务端 `degraded` 字段 | 属于 `product.catalog-browse` 当前真实 `ACTIVE` 范围 |
| 按类目 ID 获取类目列表 | `/pages/coupon/detail.vue` / `CategoryApi.getCategoryListByIds` | `AppCategoryController#getProductCategoryList(ids)` | `GET /product/category/list-by-ids` | query:`ids[]` | `list[]:{id,parentId,name,picUrl}` | `-` | `ACTIVE` | `FAIL_OPEN` | `REFRESH_ONCE` | `ids` 为空时真实返回 `[]` | 当前真实被页面使用，允许计入发布口径 |
| 商品分页 / search-lite 真值 | `/pages/index/category.vue`、`/pages/goods/list.vue`、`/pages/activity/index.vue`、`/pages/commission/goods.vue` / `SpuApi.getSpuPage` | `AppProductSpuController#getSpuPage` | `GET /product/spu/page` | query:`pageNo`,`pageSize`,`ids?[]`,`categoryId?`,`categoryIds?[]`,`keyword?`,`sortField?`,`sortAsc?` | `PageResult<{id,name,introduction,categoryId,picUrl,sliderPicUrls,specType,price,marketPrice,stock,salesCount,deliveryTypes}>` | `-` | `ACTIVE` | `FAIL_OPEN` | `REFRESH_ONCE` | 空页为合法空态；当前真实搜索承接就是 `keyword -> /product/spu/page` | 这是当前唯一真实 `search-lite` 路径；A 窗口不得再把它与 `/product/search/page` 混算 |
| 按商品 ID 获取商品列表 | `/pages/index/cart.vue`、`s-goods-card`、`s-goods-shelves` / `SpuApi.getSpuListByIds` | `AppProductSpuController#getSpuList` | `GET /product/spu/list-by-ids` | query:`ids[]` | `list[]:{id,name,introduction,categoryId,picUrl,sliderPicUrls,specType,price,marketPrice,stock,salesCount,deliveryTypes}` | `-` | `ACTIVE` | `FAIL_OPEN` | `REFRESH_ONCE` | `ids` 为空或无命中时返回 `[]` | 当前被购物车/卡片组件真实消费，允许进入发布口径 |
| 商品详情 | `/pages/goods/index.vue`、`/pages/goods/point.vue`、`/pages/goods/groupon.vue`、`/pages/goods/seckill.vue` / `SpuApi.getSpuDetail` | `AppProductSpuController#getSpuDetail` | `GET /product/spu/get-detail` | query:`id(Long)` | `{id,name,introduction,description,categoryId,productType,picUrl,sliderPicUrls,specType,price,marketPrice,stock,skus[]:{id,properties,price,marketPrice,vipPrice,picUrl,stock,weight,volume},salesCount}` | `SPU_NOT_EXISTS(1008005000)`、`SPU_NOT_ENABLE(1008005003)` | `ACTIVE` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | 无服务端 `degraded` 字段；详情不存在或未上架直接阻断 | 属于当前 `product.catalog-browse` 真实 `ACTIVE` 能力 |
| 商品评价分页 | `/pages/goods/comment/list.vue`、`detail-comment-card` / `CommentApi.getCommentPage` | `AppProductCommentController#getCommentPage` | `GET /product/comment/page` | query:`pageNo`,`pageSize`,`spuId`,`type(0/1/2/3)` | `PageResult<{userId,userNickname,userAvatar,id,anonymous,orderId,orderItemId,replyStatus,replyUserId,replyContent,replyTime,additionalContent,additionalPicUrls,additionalTime,createTime,spuId,spuName,skuId,skuProperties,scores,descriptionScores,benefitScores,content,picUrls}>` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 空页为合法空态；匿名评论由后端改写 `userNickname` | 该接口真实存在，但 `product.detail-comment-collect-history` 整体仍未冻结为 `ACTIVE` |
| 收藏分页 | `/pages/user/goods-collect.vue` / `FavoriteApi.getFavoritePage` | `AppFavoriteController#getFavoritePage` | `GET /product/favorite/page` | query:`pageNo`,`pageSize` | `PageResult<{id,spuId,spuName,picUrl,price}>` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 空页为合法空态；无服务端 `degraded` 字段 | 收藏域仍处于 Ready/补文档阶段，不能计入 `ACTIVE` |
| 检查是否已收藏 | `/pages/goods/components/detail/detail-tabbar.vue` / `FavoriteApi.isFavoriteExists` | `AppFavoriteController#isFavoriteExists` | `GET /product/favorite/exits` | query:`spuId(Long)` | `true/false` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 布尔结果是唯一真值，不允许前端本地缓存伪造 | 当前 FE 已绑定，但仍属于 detail-comment-collect-history Ready 态 |
| 新增收藏 | `/pages/goods/components/detail/detail-tabbar.vue` / `FavoriteApi.createFavorite` | `AppFavoriteController#createFavorite` | `POST /product/favorite/create` | body:`spuId(Long)` | `favoriteId(Long)` | `FAVORITE_EXISTS(1008008000)` | `PLANNED_RESERVED` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | 无 `degraded` 字段；重复收藏必须显式报错，不允许静默成功 | 写链路未进入正式发布冻结集 |
| 取消收藏 | `/pages/goods/components/detail/detail-tabbar.vue`、`/pages/user/goods-collect.vue` / `FavoriteApi.deleteFavorite` | `AppFavoriteController#deleteFavorite` | `DELETE /product/favorite/delete` | body:`spuId(Long)` | `true` | `FAVORITE_NOT_EXISTS(1008008001)` | `PLANNED_RESERVED` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | 无 `degraded` 字段；取消不存在收藏时按错误码处理 | 写链路未进入正式发布冻结集 |
| 获取收藏数量 | 当前无 FE 绑定 | `AppFavoriteController#getFavoriteCount` | `GET /product/favorite/get-count` | 无 | `count(Long)` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 无 `degraded` 字段；返回 `0` 为合法空态 | 后端已存在，前端未消费；不得在无 contract 约束下自行接入 |
| 浏览历史分页 | `/pages/user/goods-log.vue`、`pages/chat/components/select-popup.vue` / `SpuHistoryApi.getBrowseHistoryPage` | `AppProductBrowseHistoryController#getBrowseHistoryPage` | `GET /product/browse-history/page` | query:`pageNo`,`pageSize`,`createTime?[]`；当前 FE 文件写的是 GET + `data`，contract 以 controller query 口径为准 | `PageResult<{id,spuId,spuName,picUrl,price,salesCount,stock}>` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 空页为合法空态；不返回 `degraded` 字段 | 浏览历史当前真实 controller 已存在，但 FE 请求参数写法必须以此契约校正 |
| 删除浏览历史 | `/pages/user/goods-log.vue` / `SpuHistoryApi.deleteBrowseHistory` | `AppProductBrowseHistoryController#deleteBrowseHistory` | `DELETE /product/browse-history/delete` | body:`spuIds[]` | `true` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `NO_AUTO_RETRY` | 幂等删除；无命中也不应伪造系统异常 | 仍属于 detail-comment-collect-history Ready 态 |
| 清空浏览历史 | `/pages/user/goods-log.vue` / `SpuHistoryApi.cleanBrowseHistory` | `AppProductBrowseHistoryController#deleteBrowseHistory(clean)` | `DELETE /product/browse-history/clean` | 无 body | `true` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `NO_AUTO_RETRY` | 幂等清空；无命中也返回成功 | 仍属于 detail-comment-collect-history Ready 态 |
| 规划搜索 canonical | 当前无真实 FE/API 绑定 | `N/A（当前分支无 app controller）` | `GET /product/search/page` | query:`q`,`storeId?`,`pageNo`,`pageSize`,`sort?` | `N/A` | `MINIAPP_SEARCH_QUERY_INVALID(1008009904, RESERVED_DISABLED)` | `BLOCKED` | `N/A` | `N/A` | 当前没有服务端 contract 可执行体，禁止伪造 `degraded` 协议 | 该路径只能停留在规划/门禁文档，绝不能被记为当前 `ACTIVE` |
| 规划目录分页 canonical | 当前无真实 FE/API 绑定 | `N/A（当前分支无 app controller）` | `GET /product/catalog/page` | query:`storeId`,`categoryId`,`pageNo`,`pageSize`,`catalogVersion?` | `N/A` | `MINIAPP_CATALOG_VERSION_MISMATCH(1008009902, RESERVED_DISABLED)` | `BLOCKED` | `N/A` | `N/A` | 当前没有服务端 contract 可执行体，禁止把版本守卫文档误写成真实接口 | 规划路径不能进入 allowlist；当前真实目录承接仍是 `/product/spu/page` |

## 5. 域级 canonical 约束
- 当前真实搜索只有一条：
  - `/pages/index/search` 输入关键词
  - `/pages/goods/list` 透传到 `GET /product/spu/page`
- `search-lite` 与 `search-canonical` 必须分池治理：
  - `search-lite = /product/spu/page`
  - `search-canonical = /product/search/page`（当前 `BLOCKED`）
- `catalog version guard` 只是门禁能力，不等于已有 `/product/catalog/page` controller。
- 浏览历史分页当前 FE 文件使用 `GET + data`，但 controller 真实消费 query 参数；联调与代理层必须以本 contract 为准。

## 6. 发布口径
- 允许进入当前发布口径的产品目录主链路只有：
  - `GET /product/category/list`
  - `GET /product/category/list-by-ids`
  - `GET /product/spu/page`
  - `GET /product/spu/list-by-ids`
  - `GET /product/spu/get-detail`
- 收藏、浏览历史、评价虽然前后端真实存在，但 capability ledger 仍固定在 `product.detail-comment-collect-history = PLANNED_RESERVED`。
- `/product/search/page`、`/product/catalog/page` 在当前分支没有真实 controller，固定 `BLOCKED`。

## 7. 跨窗口联调约束
- A 窗口
  - 必须把 `search-lite` 与 `search-canonical` 拆开统计，不能再把 `/product/spu/page` 当成 `/product/search/page` 的别名。
- B 窗口
  - 详情不存在/未上架时只按 `1008005000 / 1008005003` 分支，不得按非结构化文本猜测“商品已下架”。
- D 窗口
  - `1008009902`、`1008009904` 是门禁/规划错误码，不得被记成当前已落地 controller 的正常错误样本。
