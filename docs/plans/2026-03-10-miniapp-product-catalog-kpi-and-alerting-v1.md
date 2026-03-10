# MiniApp Product Catalog KPI and Alerting v1 (2026-03-10)

## 1. 目标
- 为商品目录域建立可执行的 KPI、告警和升级机制，覆盖商品详情、收藏、浏览历史、评论、搜索 lite、搜索 canonical。
- 当前状态约束：
  - `product.catalog-browse`、`product.search-lite` 为 `ACTIVE`
  - `product.detail-comment-collect-history`、`product.search-canonical` 为 `PLANNED_RESERVED`
- 对齐基线：
  - `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
  - `docs/products/miniapp/2026-03-09-miniapp-search-discovery-prd-v1.md`
  - `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`
  - `docs/contracts/2026-03-09-miniapp-reserved-disabled-gate-spec-v1.md`

## 2. 口径总则
- 成功率只认后端成功响应，不认前端展示成功。
- 空结果要区分：
  - 合法空结果：无搜索结果、无评论、无收藏、无浏览历史
  - 异常空结果：接口错误、降级错误、字段缺失导致的空展示
- `degraded=true` 单独入 `degraded_pool`，不得计入主成功率、主转化率。

## 3. 指标执行表

| 指标编码 | 指标 | 公式 | 数据源 | 刷新频率 | 阈值 | 告警等级 | Owner |
|---|---|---|---|---|---|---|---|
| `CAT-KPI-01` | 商品详情错误率 | `detail_error_cnt / detail_request_cnt` | `/product/spu/get-detail` 网关日志、商品详情埋点 | 5 分钟 | `>1%` | P1 | Product Domain Owner |
| `CAT-KPI-02` | 商品详情空结果率 | `detail_empty_cnt / detail_request_cnt` | 商品详情接口 + 详情页埋点 | 15 分钟 | `>2%` | P1 | Product Domain Owner |
| `CAT-KPI-03` | 收藏成功率 | `favorite_success_cnt / favorite_attempt_cnt` | `/product/favorite/*`、收藏埋点 | 15 分钟 | `<98%` | P1 | Product Domain Owner |
| `CAT-KPI-04` | 浏览历史写入成功率 | `history_write_success_cnt / history_view_cnt` | `/product/browse-history/*`、商品浏览埋点 | 15 分钟 | `<99%` | P2 | Product Domain Owner |
| `CAT-KPI-05` | 评论提交成功率 | `comment_submit_success_cnt / comment_submit_attempt_cnt` | `POST /trade/order/item/create-comment`、评论埋点 | 15 分钟 | `<97%` | P1 | Product Domain Owner |
| `CAT-KPI-06` | 评论列表空结果率 | `comment_empty_cnt / comment_list_cnt` | `GET /product/comment/page`、评论列表埋点 | 15 分钟 | 合法空结果单独统计；异常空结果 `>2%` | P2 | Product Domain Owner |
| `CAT-KPI-07` | 搜索 lite 转化率 | `goods_detail_click_uv / search_submit_uv` | `/pages/index/search`、`/pages/goods/list`、`/product/spu/page` | 15 分钟 | `<18%` | P2 | Search Owner |
| `CAT-KPI-08` | 搜索 lite 错误率 | `search_lite_error_cnt / search_submit_cnt` | `GET /product/spu/page`、搜索埋点 | 5 分钟 | `>1.5%` | P1 | Search Owner |
| `CAT-KPI-09` | 搜索 canonical 降级率 | `search_canonical_degraded_cnt / search_canonical_cnt` | `GET /product/search/page`、`degraded=true` 事件 | 5 分钟 | `>3%` | P1 | Search Owner |
| `CAT-KPI-10` | 搜索 canonical 恢复率 | `recovered_after_degrade_cnt / degraded_cnt` | 降级恢复事件、重试日志 | 15 分钟 | `<85%` | P2 | Search Owner |
| `CAT-KPI-11` | 目录版本冲突率 | `1008009902_cnt / catalog_request_cnt` | `GET /product/catalog/page`、错误码事件 | 5 分钟 | 开关开启时 `>0.5%` | P1 | Product on-call |
| `CAT-KPI-12` | 搜索 query 非法命中率 | `1008009904_cnt / search_canonical_cnt` | `GET /product/search/page`、错误码事件 | 5 分钟 | 开关关闭时 `>0`；开启时 `>1%` | P1 | Search Owner |

## 4. 指标分组说明

### 4.1 商品详情
- 主接口：`GET /product/spu/get-detail`
- 转化指标：详情页到下单/加购点击率
- 异常重点：详情接口错误、营销信息错误、无商品数据

### 4.2 收藏 / 浏览历史 / 评论
- 当前为 `PLANNED_RESERVED` 文档补齐阶段，但仍需先建立监控口径。
- 未进入 `ACTIVE` 前：
  - 只做内部指标与告警
  - 不得纳入对外发布成功率

### 4.3 搜索 lite
- 当前真实上线路径：`/pages/index/search -> /pages/goods/list -> GET /product/spu/page`
- 重点指标：空结果率、错误率、详情点击转化率

### 4.4 搜索 canonical
- 当前受 `miniapp.search.validation=off` 保护，为 `PLANNED_RESERVED`
- 重点错误码：`1008009904 MINIAPP_SEARCH_QUERY_INVALID`
- 开关关闭态命中即视为误返回，直接回滚

## 5. 空结果 / 错误率 / 降级率 / 恢复率 / 转化率判定

| 类型 | 判定标准 |
|---|---|
| 空结果率 | 只统计返回 200 但 `list=[]` 或主对象为空的场景，需再拆“合法空结果”和“异常空结果” |
| 错误率 | 4xx/5xx、结构化错误码返回、请求超时 |
| 降级率 | 响应中 `degraded=true` 或明确命中 fail-open 降级路径 |
| 恢复率 | 命中降级后在 1 个观察窗口内恢复成功的比例 |
| 转化率 | 搜索提交 UV -> 详情点击 UV -> 下单 UV 的阶段转化 |

## 6. 告警路由与升级时限

| 场景 | 触发条件 | 主责 | 首响 | 升级 |
|---|---|---|---|---|
| 商品详情错误率异常 | `CAT-KPI-01` 超阈值 | Product Domain Owner | 15 分钟 | 1 小时未缓解升级发布负责人 |
| 收藏/评论成功率下滑 | `CAT-KPI-03/05` 超阈值 | Product Domain Owner | 15 分钟 | 1 小时未缓解升级域负责人 |
| 搜索 lite 错误率异常 | `CAT-KPI-08` 超阈值 | Search Owner | 15 分钟 | 1 小时未缓解升级发布负责人 |
| 目录版本冲突异常 | `CAT-KPI-11` 超阈值 | Product on-call | 15 分钟 | 30 分钟未缓解升级发布负责人 |
| 搜索 canonical 误返回 | 开关关闭态命中 `1008009904` | Search Owner + SRE | 5 分钟 | 立即升级 P1 配置事故 |

## 7. 回滚和降级动作

| 触发条件 | 动作 |
|---|---|
| 详情错误率持续超阈值 | 回退商品详情营销扩展字段或隐藏异常模块 |
| 搜索 lite 错误率超阈值 | 保留搜索词，回退到默认商品列表并提示重试 |
| `1008009902` 激增 | 关闭 `miniapp.catalog.version-guard`，恢复旧目录读取 |
| `1008009904` 开关关闭态命中 | 关闭 `miniapp.search.validation`，停止灰度，按误返回处理 |
| 收藏/评论/历史异常集中 | 暂停入口展示，仅保留只读详情 |

## 8. 审计字段
- 所有告警和恢复记录必须包含：
  - `runId`
  - `orderId`
  - `payRefundId`
  - `sourceBizNo`
  - `errorCode`
  - `route`
  - `keyword`（搜索场景）

## 9. 验收标准
1. 商品详情、收藏、浏览历史、评论、搜索均有指标口径。
2. 空结果率、错误率、降级率、恢复率、转化率均有明确判定。
3. 告警路由和升级时限明确，支持 ACTIVE 与 PLANNED_RESERVED 混合治理。
4. `1008009902/1008009904` 的回滚条件与动作明确可执行。
