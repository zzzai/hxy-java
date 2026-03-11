# MiniApp Product Catalog Customer Recovery SOP v1 (2026-03-11)

## 1. 目标与适用范围
- 目标：把商品目录与互动域的客服接待、用户恢复动作、升级矩阵和回访闭环固化，避免 `search-lite`/`search-canonical` 混算、收藏 typo path 漂移、评论/足迹删除伪成功等问题再次出现。
- 适用页面与接口：
  - 搜索输入 / 商品列表：`/pages/index/search`、`/pages/goods/list` -> `GET /product/spu/page`
  - 商品详情：`/pages/goods/index` -> `GET /product/spu/get-detail`
  - 收藏：`/pages/user/goods-collect`、商品详情底栏 -> `/product/favorite/*`
  - 浏览历史：`/pages/user/goods-log` -> `/product/browse-history/*`
  - 评论：`/pages/goods/comment/list`、`/pages/goods/comment/add` -> `GET /product/comment/page`、`POST /trade/order/item/create-comment`
- 对齐基线：
  - `docs/products/miniapp/2026-03-10-miniapp-product-catalog-interaction-prd-v1.md`
  - `docs/contracts/2026-03-10-miniapp-product-catalog-contract-v1.md`
  - `docs/plans/2026-03-10-miniapp-product-catalog-kpi-and-alerting-v1.md`
  - `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`
  - `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md`
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
- 当前 capability 真值：
  - `CAP-PRODUCT-001 product.catalog-browse = ACTIVE`
  - `CAP-PRODUCT-002 product.detail-comment-collect-history = PLANNED_RESERVED`
  - `CAP-PRODUCT-003 product.search-canonical = PLANNED_RESERVED`
  - 本文补齐的是 catalog / interaction 的客服恢复口径，不自动把 `PLANNED_RESERVED` 互动链路升为 `ACTIVE`

## 2. 处理原则
- `search-lite` 与 `search-canonical` 必须分池、分分母、分告警，不得混算。
- 当前唯一真实搜索主链路是：`/pages/index/search -> /pages/goods/list -> GET /product/spu/page`。
- 收藏状态真实路径固定为 `GET /product/favorite/exits`，不得改写成 `/exists`。
- 商品详情错误只按 `1008005000 / 1008005003` 分支，不按 message 猜测。
- 收藏/评论写链路一律 `FAIL_CLOSE`；删除/清空浏览历史失败不得伪造“系统成功”。
- 评论发布必须全部提交成功后才允许离页；任一子项失败都必须保留当前页和已输入内容。
- 本域当前没有新增服务端 `degraded/degradeReason` 字段；允许的查询降级只有：`[]`、`null`、空页、空态。
- 所有工单必须携带：`runId/orderId/payRefundId/sourceBizNo/errorCode`，无值统一填 `"0"`；搜索场景额外带 `route/keyword/spuId`。

## 3. 客服接待分层

| 层级 | 角色 | 职责 | 首响时限 |
|---|---|---|---|
| `L1` | 客服一线 | 判断搜索/详情/收藏/足迹/评论场景，指导用户刷新、回退、重试 | 5 分钟 |
| `L2` | 客服组长 / 商品运营支撑 | 复核是否为商品下架、收藏态脏读、评论提交失败、搜索空态异常 | 15 分钟 |
| `L3` | Product on-call / Search Owner / 发布负责人 | 执行回滚、关入口、切回默认列表、修复口径混算 | P0 5 分钟，P1 15 分钟 |

## 4. 标准处理流程

### 4.1 接待步骤
1. 先确认用户场景：搜索、商品详情、收藏、浏览历史、评论列表、评论发布。
2. 采集页面路由、关键词、商品 ID、错误码、发生时间和截图。
3. 判断是查询型问题还是写操作失败：
   - 查询型：允许刷新、保留上次成功数据、回到上一级页面
   - 写操作：必须停留当前页、保留当前状态或输入内容
4. 按场景表执行标准话术与恢复动作。

### 4.2 转人工条件
满足任一条件必须转 `L2/L3`：
- 搜索结果连续两次刷新仍异常，且疑似命中 canonical 保留路径或 `1008009904`。
- 商品详情命中 `1008005000/1008005003` 仍无法解释商品状态。
- 收藏态、足迹删除、评论提交出现前端状态与接口返回不一致。
- 同类问题 15 分钟内出现 3 单及以上。
- 发现 `search-lite` 与 `search-canonical` 指标或样本被混算。

### 4.3 回访闭环
1. 恢复后 24 小时内回访。
2. 回访说明必须包含：是查询异常还是写操作失败、是否需要重新操作、是否已恢复到稳定入口。
3. 工单状态按 `Open -> Ack -> Mitigating -> Resolved -> Closed` 闭环。

## 5. 工单字段模板

| 字段 | 说明 | 规则 |
|---|---|---|
| `ticketType` | `PRODUCT_CATALOG_INCIDENT` / `SEARCH_POOL_INCIDENT` | 必填 |
| `scene` | `search-lite/detail/favorite/history/comment` | 必填 |
| `route` | 页面路由 | 必填 |
| `keyword` | 搜索词 | 无值填 `"0"` |
| `spuId` | 商品主键 | 无值填 `"0"` |
| `runId` | 发布/巡检批次 | 无值填 `"0"` |
| `orderId` | 订单主键 | 无值填 `"0"` |
| `payRefundId` | 退款主键 | 无值填 `"0"` |
| `sourceBizNo` | 业务流水号 | 无值填 `"0"` |
| `errorCode` | 错误码或 `"0"` | 必填 |
| `recoveryAction` | 已执行动作 | 必填 |
| `poolType` | `search-lite` / `search-canonical` / `catalog-browse` / `interaction` | 必填 |

## 6. 场景话术与恢复动作

### 6.1 搜索 lite / 搜索结果

| 场景 | 标准话术 | 恢复动作 | 禁止行为 |
|---|---|---|---|
| 搜索结果为空但疑似异常 | 当前结果暂未加载完成，请下拉刷新或返回重新搜索。 | 保留关键词，回退到 `/pages/goods/list` 默认刷新一次。 | 不得直接切换到 `/product/search/page`。 |
| 命中 `1008009904 MINIAPP_SEARCH_QUERY_INVALID` | 当前搜索入口暂不可用，请返回后重新输入关键词。 | 记录 `poolType=search-canonical`，按误接保留路径处理。 | 不得把该错误码挂到 `search-lite`。 |
| 搜索分池混算 | 当前搜索服务正在切换，请稍后再试。 | 立即升级 Search Owner，冻结发布口径。 | 不得把 canonical 样本并入 lite 成功率。 |

### 6.2 商品详情异常

| 错误码 / 场景 | 标准话术 | 恢复动作 | 禁止行为 |
|---|---|---|---|
| `1008005000 SPU_NOT_EXISTS` | 当前商品暂不可查看，请返回商品列表重新选择。 | 返回上一页或商品列表。 | 不得显示“商品正常售卖中”。 |
| `1008005003 SPU_NOT_ENABLE` | 当前商品已下架或暂不可售，请选择其他商品。 | 返回商品列表并刷新。 | 不得继续展示购买 CTA。 |
| 营销聚合返回 `type=2 bargain` | 当前活动入口暂不可用，请继续浏览商品详情。 | 隐藏或忽略 bargain 卡片。 | 不得跳到不存在的砍价页面。 |

### 6.3 收藏异常

| 场景 | 标准话术 | 恢复动作 | 禁止行为 |
|---|---|---|---|
| 收藏状态获取失败 | 当前收藏状态暂不可用，请稍后刷新。 | 维持当前 UI，不本地猜测收藏态。 | 不得把 `/product/favorite/exits` 改成 `/exists` 后重试。 |
| `1008008000 FAVORITE_EXISTS` | 当前商品已在收藏列表中。 | 保持当前收藏态，不重复提交。 | 不得继续弹“收藏成功”。 |
| `1008008001 FAVORITE_NOT_EXISTS` | 当前收藏记录已失效，请刷新收藏列表。 | 刷新收藏页或详情页收藏态。 | 不得把失败操作视作删除成功。 |
| 收藏删除失败 | 当前操作未完成，请保持原状态后重试。 | 保持旧状态，允许手动再删一次。 | 不得先行从 UI 移除该商品。 |

### 6.4 浏览历史异常

| 场景 | 标准话术 | 恢复动作 | 禁止行为 |
|---|---|---|---|
| 历史列表加载失败 | 当前浏览记录暂不可用，请稍后下拉刷新。 | 保留上次成功数据；若无数据则展示系统空态。 | 不得把请求失败解释成自然空记录。 |
| 删除 / 清空失败 | 当前删除未完成，请稍后再试。 | 保持旧列表状态。 | 不得先把 UI 清空再补错误提示。 |
| 幂等清空返回成功 | 当前记录已清空。 | 允许前端刷新列表。 | 不得重复报系统错误。 |

### 6.5 评论异常

| 场景 | 标准话术 | 恢复动作 | 禁止行为 |
|---|---|---|---|
| 评论列表加载失败 | 当前评论暂不可用，请稍后刷新。 | 保留上次成功评论卡片；无历史数据则显示空态。 | 不得把错误态伪装成“暂无评论”。 |
| 评论提交失败 | 当前评论未提交成功，请检查内容后重试。 | 保留当前输入内容和图片，禁止离页。 | 不得直接返回上一页或展示“评论成功”。 |
| 部分订单项评论失败 | 当前仍有评论未成功，请完成后再离开。 | 标记失败订单项，继续停留当前页。 | 不得把部分成功当成整体成功。 |

## 7. 升级矩阵

| 级别 | 典型场景 | 升级对象 | 时限 |
|---|---|---|---|
| `P0` | 搜索分池混算、canonical 保留能力误进发布口径、评论/收藏写链路出现伪成功 | Search Owner + Product on-call + 发布负责人 | 5 分钟 |
| `P1` | 商品详情错误率超阈值、收藏/足迹删除异常集中、`1008009902/1008009904` 误返回 | Product on-call / Search Owner | 15 分钟 |
| `P2` | 单个商品详情异常、个别评论加载失败、短时空结果波动 | 值班客服 + 域负责人 | 30 分钟 |

## 8. 人工接管与回滚动作
- `search-lite`
  - 保留关键词，回退默认商品列表，不切 canonical。
- `search-canonical`
  - 关闭 `miniapp.search.validation`，停止灰度并回收样本。
- `catalog version guard`
  - 命中 `1008009902` 时关闭 `miniapp.catalog.version-guard`，恢复稳定目录读取。
- `comment / favorite / history`
  - 暂停有问题的写入口，只保留只读详情和已加载成功的展示数据。

## 9. 验收清单
- [ ] `search-lite` 与 `search-canonical` 的客服话术和工单分池明确分开。
- [ ] 文档明确收藏状态路径真值是 `/product/favorite/exits`。
- [ ] 文档明确 `1008005000 / 1008005003 / 1008008000 / 1008008001 / 1008009902 / 1008009904` 的恢复动作。
- [ ] 文档明确评论必须全部成功后才允许离页。
- [ ] 文档明确足迹/收藏删除失败时保持旧状态，不允许伪成功。
