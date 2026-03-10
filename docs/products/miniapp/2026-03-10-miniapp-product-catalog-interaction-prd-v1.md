# MiniApp 商品目录与互动域 PRD v1（2026-03-10）

## 0. 文档定位
- 目标：把商品详情、收藏、浏览历史、评论、搜索 lite、搜索 canonical 的真实运行链路与规划边界一次性冻结。
- 分支：`feat/ui-four-account-reconcile-ops`
- 约束：
  - 只认当前真实页面、真实路由、真实 app controller。
  - 不把 `/pages/search/index` 之类冻结文档 route 写成当前运行时真值。
  - 对运行中的 typo path 也按真值记录，例如 `/product/favorite/exits`。
- 对齐基线：
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
  - `docs/products/miniapp/2026-03-09-miniapp-search-discovery-prd-v1.md`
  - `docs/plans/2026-03-10-miniapp-product-catalog-kpi-and-alerting-v1.md`

## 1. 业务目标与非目标

### 1.1 业务目标
- 让用户可以稳定完成“搜索 / 列表浏览 -> 商品详情 -> 收藏 / 浏览沉淀 -> 评论”闭环。
- 明确哪些能力已经真实上线，哪些只是冻结文档里的 canonical 规划。
- 固定商品详情的附加互动：收藏、客服、营销聚合、评论预览。

### 1.2 非目标
- 不设计搜索算法、召回、排序策略。
- 不新增 `/pages/search/index`、热词页、猜你想搜页。
- 不新增评论编辑、追评、评论删除页。

## 2. 域能力总览

| 能力 | 真实页面 / 路由 | 状态 | 真实 API | 结论 |
|---|---|---|---|---|
| 搜索 lite 输入 | `/pages/index/search` | `ACTIVE` | 本地历史 + `/product/spu/page` 间接承接 | 输入词后直接跳商品列表 |
| 搜索结果 / 商品列表 | `/pages/goods/list` | `ACTIVE` | `GET /product/spu/page` `GET /trade/order/settlement-product` | 当前真实搜索承接页 |
| 商品详情 | `/pages/goods/index?id=` | `ACTIVE` | `GET /product/spu/get-detail` `GET /product/favorite/exits` `GET /promotion/activity/list-by-spu-id` | 详情主承接页 |
| 收藏页 | `/pages/user/goods-collect` | `ACTIVE` | `/product/favorite/page` `/product/favorite/create` `/product/favorite/delete` | 已上线 |
| 浏览历史 | `/pages/user/goods-log` | `ACTIVE` | `/product/browse-history/page` `/product/browse-history/delete` `/product/browse-history/clean` | 已上线 |
| 评论列表 | `/pages/goods/comment/list?id=` | `ACTIVE` | `GET /product/comment/page` | 已上线 |
| 评论发布 | `/pages/goods/comment/add?id=` | `ACTIVE` | `POST /trade/order/item/create-comment` | 已上线 |
| 搜索 canonical | 当前无真实用户页 | `PLANNED_RESERVED` | `GET /product/search/page` | 只存在冻结文档和开关约束 |
| `/pages/search/index` | 当前无真实页面 | `缺页能力` | 无当前 uniapp route | 不能写成 runtime 真值 |

## 3. 用户场景与页面流转

### 3.1 搜索 lite
1. 用户进入 `/pages/index/search` 输入关键词。
2. 页面把关键词存入本地 `searchHistory`。
3. 页面跳 `/pages/goods/list?keyword={keyword}`。
4. 商品列表页按 `keyword` 或 `categoryId` 调 `GET /product/spu/page`。

### 3.2 商品详情与互动
1. 用户从商品列表或其他商品入口进入 `/pages/goods/index?id={spuId}`。
2. 页面拉取 `GET /product/spu/get-detail`。
3. 登录用户再拉 `GET /product/favorite/exits` 确认收藏状态。
4. 页面并行拉营销聚合、优惠券、结算扩展；收藏、客服、分享在底栏承接。
5. 打开详情接口时，后端同时增加浏览量并写浏览历史。

### 3.3 收藏 / 浏览历史 / 评论
1. 收藏页通过 `/product/favorite/page` 展示已收藏商品并支持批量取消。
2. 浏览历史页通过 `/product/browse-history/page` 展示历史，并支持删除 / 清空。
3. 评论列表页按 `spuId + type` 查询评论；评论发布页按订单项逐条提交评论。

## 4. 页面 route 真值

| 页面 route | 真实参数 | 页面角色 | 当前真值说明 |
|---|---|---|---|
| `/pages/index/search` | 无 | 搜索词输入页 | 当前真实输入页，不是 `/pages/search/index` |
| `/pages/goods/list` | `keyword?` `categoryId?` | 搜索结果 / 商品列表 | 同时承接搜索词与分类筛选 |
| `/pages/goods/index` | `id` | 商品详情页 | 必须有 `spuId` |
| `/pages/user/goods-collect` | 无 | 收藏页 | 仅页面内编辑，不改 route |
| `/pages/user/goods-log` | 无 | 浏览历史页 | 仅页面内编辑，不改 route |
| `/pages/goods/comment/list` | `id` | 评论列表页 | `id` 实际是 `spuId` |
| `/pages/goods/comment/add` | `id` | 评论发布页 | `id` 实际是 `orderId`，不是 `spuId` |
| `/pages/search/index` | 无真实 route | 规划态 | 只能存在于冻结 / 规划文档，不能冒充运行时页面 |

## 5. 页面 -> API -> 字段关系

### 5.1 搜索 lite / 商品列表

| 页面 | API | 请求字段 | 响应字段 | 页面使用 |
|---|---|---|---|---|
| `/pages/goods/list` | `GET /product/spu/page` | `pageNo` `pageSize` `sortField?` `sortAsc?` `categoryId?` `keyword?` | `list[].id` `name` `picUrl` `price` `marketPrice` `introduction` `salesCount` `stock` `total` | 搜索结果与商品列表 |
| `/pages/goods/list` | `GET /trade/order/settlement-product` | `ids` | `rewardActivity` `skus[]` 等营销扩展字段 | 拼接活动标签和 SKU 促销价 |

### 5.2 商品详情

| 页面 | API | 请求字段 | 响应字段 | 页面使用 |
|---|---|---|---|---|
| `/pages/goods/index` | `GET /product/spu/get-detail` | `id` | `id` `name` `introduction` `description` `categoryId` `picUrl` `sliderPicUrls` `price` `marketPrice` `stock` `salesCount` `skus[]` | 商品详情主数据 |
| `/pages/goods/index` | `GET /product/favorite/exits` | `spuId` | `Boolean` | 收藏状态；注意真实 path 为 `exits` |
| `/pages/goods/index` | `GET /promotion/activity/list-by-spu-id` | `spuId` | `[{id,type,name,spuId,startTime,endTime}]` | 秒杀 / 拼团 / 砍价活动聚合列表 |
| `/pages/goods/index` | `GET /promotion/reward-activity/get` | `id` | `id` `title` `rules[]` `startTime` `endTime` | 满减送活动时间与规则详情 |

### 5.3 收藏 / 浏览历史

| 页面 | API | 请求字段 | 响应字段 | 页面使用 |
|---|---|---|---|---|
| `/pages/user/goods-collect` | `GET /product/favorite/page` | `pageNo` `pageSize` | `list[].id` `spuId` `spuName` `picUrl` `price` `total` | 收藏列表 |
| `/pages/goods/index` `/pages/user/goods-collect` | `POST /product/favorite/create` | `spuId` | `Long favoriteId` | 添加收藏 |
| `/pages/goods/index` `/pages/user/goods-collect` | `DELETE /product/favorite/delete` | `spuId` | `Boolean` | 取消收藏 |
| `/pages/user/goods-log` | `GET /product/browse-history/page` | `pageNo` `pageSize` | `list[].id` `spuId` `spuName` `picUrl` `price` `salesCount` `stock` `total` | 浏览历史列表 |
| `/pages/user/goods-log` | `DELETE /product/browse-history/delete` | `spuIds[]` | `Boolean` | 删除选中足迹 |
| `/pages/user/goods-log` | `DELETE /product/browse-history/clean` | 无 | `Boolean` | 清空足迹 |

### 5.4 评论

| 页面 | API | 请求字段 | 响应字段 | 页面使用 |
|---|---|---|---|---|
| `/pages/goods/comment/list` | `GET /product/comment/page` | `spuId` `pageNo` `pageSize` `type` | `list[].userId` `userNickname` `userAvatar` `anonymous` `replyStatus` `replyContent` `createTime` `scores` `descriptionScores` `benefitScores` `content` `picUrls` `spuId` `spuName` `skuId` `skuProperties` `total` | 评论列表与 tab 筛选 |
| `/pages/goods/comment/add` | `POST /trade/order/item/create-comment` | `anonymous` `orderItemId` `descriptionScores` `benefitScores` `content` `picUrls[]` | `Long commentId` | 逐条提交订单项评论 |

## 6. `search-lite` 与 `search-canonical` 当前状态差异

| 项目 | `search-lite` | `search-canonical` |
|---|---|---|
| 当前状态 | `ACTIVE` | `PLANNED_RESERVED` |
| 真实输入页 | `/pages/index/search` | `/pages/search/index`（不存在真实 route） |
| 真实结果页 | `/pages/goods/list` | 规划态结果页 |
| 真实 API | `GET /product/spu/page` | `GET /product/search/page` |
| 真实 query | `keyword` | `q` |
| 历史记录 | 本地 `searchHistory` | 规划态未落用户页 |
| 开关约束 | 无单独 reserved gate | 受 `miniapp.search.validation=off` 和 `1008009904 MINIAPP_SEARCH_QUERY_INVALID` 约束 |
| 发布要求 | 可按当前页面发布 | 未有真实 route、真实联调、真实验收，不得宣称上线 |

## 7. `ACTIVE / PLANNED_RESERVED / 缺页能力` 分层

### 7.1 `ACTIVE`
- 搜索 lite：`/pages/index/search -> /pages/goods/list -> /product/spu/page`
- 商品详情、收藏、浏览历史、评论列表、评论发布。
- 商品详情自动写浏览记录。

### 7.2 `PLANNED_RESERVED`
- canonical search：`/pages/search/index + /product/search/page`
- query 校验门禁：`miniapp.search.validation`

### 7.3 缺页能力
- `/pages/search/index`
- 搜索建议词 / 热词页 / 搜索纠错页
- 评论编辑 / 追评 / 删除页
- 收藏导出 / 一键清空收藏页

## 8. 错误码与用户恢复动作

| 错误码 | 场景 | 用户侧动作 | 产品约束 |
|---|---|---|---|
| `1008005000 SPU_NOT_EXISTS` | 商品详情 / 商品跳转 | 展示“商品不存在或已下架”，允许回商品列表 | 不得继续展示旧商品图文 |
| `1008005003 SPU_NOT_ENABLE` | 商品详情读取到下架商品 | 展示“商品不存在或已下架”并回退 | 不得继续下单 |
| `1008001000 CATEGORY_NOT_EXISTS` | 商品列表类目筛选 | 清掉无效 `categoryId` 后刷新列表 | 不得假装筛选成功 |
| `1008007000 COMMENT_NOT_EXISTS` | 评论详情 / 评论查询异常 | 刷新评论列表；失败则回空态 | 不得保留假评论数 |
| `1008007001 COMMENT_ORDER_EXISTS` | 重复提交评论 | 留在评论页，标记该订单项已评价 | 不得自动返回成功页 |
| `1011000019 ORDER_COMMENT_FAIL_STATUS_NOT_COMPLETED` | 未完成订单提交评论 | 阻断提交，引导回订单页 | 不得创建半评论态 |
| `1011000020 ORDER_COMMENT_STATUS_NOT_FALSE` | 已评价订单重复评价 | 阻断提交并提示查看已有评价 | 不得重复写入评论 |
| `1008008000 FAVORITE_EXISTS` | 重复收藏 | 刷新收藏按钮为已收藏 | 不得重复弹成功 |
| `1008008001 FAVORITE_NOT_EXISTS` | 取消不存在的收藏 | 刷新收藏列表 | 不得继续显示“取消成功” |
| `1008009904 MINIAPP_SEARCH_QUERY_INVALID` | canonical search 非法 query | 保留 query，提示修正后重试 | 当前 lite 路径命中该码即视为误路由 |

## 9. 降级语义与禁止伪成功规则

| 场景 | 降级类型 | 允许行为 | 禁止行为 |
|---|---|---|---|
| 商品详情加载失败 | `fail-close` | 展示售罄 / 不存在空态并回列表 | 假装详情打开成功 |
| 搜索 lite 请求失败 | `fail-open` | 保留 query，展示空结果或默认商品列表 | 自动跳 canonical success 态 |
| 评论列表失败 | `fail-open` | 展示“期待你的第一个评价”或重试 | 把异常空列表当自然空态计成功 |
| 收藏 / 取消收藏失败 | `fail-close` | 保持按钮旧状态，允许重试 | 直接切换按钮并提示成功 |
| 删除足迹 / 清空足迹失败 | `fail-close` | 保持原列表并提示重试 | 先删 UI 再回滚 |
| 评论发布失败 | `fail-close` | 停留当前页，标记失败项 | 部分成功后直接返回上页 |

### 9.1 禁止伪成功
- `POST /trade/order/item/create-comment` 只有所有订单项都成功提交，才允许离开评论发布页。
- 收藏、取消收藏、删除足迹、清空足迹任何非 0 返回都不能弹成功态。
- `GET /product/spu/get-detail` 触发浏览记录写入是后端副作用，页面不得额外提示“已记录浏览成功”。

## 10. 是否阻断开发、是否阻断发布

| 判断项 | 结论 | 说明 |
|---|---|---|
| 已上线目录 / 互动能力继续开发是否阻断 | `否` | 搜索 lite、商品详情、收藏、历史、评论都有真实前后端闭环 |
| 把 canonical search 直接纳入开发是否阻断 | `是` | 当前无真实 route，且受 reserved gate 保护 |
| 仅按当前真实 route 发布是否阻断 | `否` | 仅限 `search-lite + goods/list + goods/index + collect/log/comment` |
| 若把 `/pages/search/index` 或 `/product/search/page` 写成已上线是否阻断发布 | `是` | 会产生搜索能力假冻结 |

## 11. 验收清单
- [ ] 文档明确 `search-lite` 和 `search-canonical` 的 route、API、query、状态差异。
- [ ] `GET /product/favorite/exits` 按真实 path 记录，没有被擅自改写成 `/exists`。
- [ ] 商品详情、收藏、浏览历史、评论的 page -> API -> field 关系可直接给 A/C 对齐。
- [ ] 商品详情的浏览记录副作用、评论逐条提交、收藏 fail-close 规则被明确记录。
- [ ] canonical search 仍被标为 `PLANNED_RESERVED`，没有被混入现网能力。
