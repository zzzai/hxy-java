# MiniApp 搜索与发现 PRD v1（2026-03-09）

## 0. 文档定位
- 目标：补齐「搜索与发现」从意图输入到商品转化的产品规范，确保 UI 评审缺口可落地。
- 约束基线：
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`

## 1. 场景目标与非目标
### 1.1 场景目标
- 缩短「搜索意图 -> 结果筛选 -> 商品详情 -> 下单」路径。
- 保证发现内容（活动标签、券标签、积分可兑）与后端真值一致。
- 在类目/活动依赖异常时保留可浏览能力并提供显式恢复动作。

### 1.2 非目标
- 不设计搜索排序算法（召回、重排策略不在本 PRD）。
- 不新增商品域或活动域错误码语义。
- 不覆盖直播/内容社区发现场景。

## 2. 页面信息架构与关键流程
### 2.1 IA（搜索与发现）
| 页面路由 | 角色 | 关键能力 | 依赖接口 |
|---|---|---|---|
| `/pages/index/search` | 意图输入 | 搜索词输入、历史记录、快捷触达 | 本地历史 + 商品列表查询 |
| `/pages/goods/list` | 发现承接 | 关键词/分类结果、排序筛选、营销信息拼接 | `GET /product/spu/page`, `GET /trade/order/settlement-product` |
| `/pages/goods/index` | 转化页 | 商品详情、活动信息、加购/下单入口 | `GET /product/spu/get-detail` |
| `/pages/index/category` | 类目发现 | 分类浏览与快捷筛选 | `GET /product/category/list` |
| `/pages/coupon/center`（可选联动） | 活动转化 | 券领取后回流搜索结果 | 券接口组 |

### 2.2 关键流程
1. 用户输入关键词，进入商品列表并可按销量/价格/新品筛选。
2. 商品列表拉取结算营销信息，展示活动或价格提示。
3. 用户进入商品详情并执行下单，失败时按错误码给出恢复动作。

## 3. 状态机映射（引用统一状态机）
统一引用：`docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`

| 发现对象 | 统一状态机 | 前端规则 |
|---|---|---|
| 积分活动 | `ONLINE | OFFLINE | SOLD_OUT | EXPIRED` | 仅 `ONLINE` 展示可兑 CTA |
| 券模板 | `AVAILABLE | EXPIRED | OUT_OF_STOCK` | 非 `AVAILABLE` 不展示“立即领取” |
| 用户券 | `UNUSED | LOCKED | USED | EXPIRED` | 搜索结果仅展示可用数量，不承诺可抵扣 |
| 下单支付视图 | `WAITING | SUCCESS | REFUNDED | CLOSED` | 从发现链路进入支付后，结果页按统一态展示 |

## 4. 错误码与降级语义
| Code/Key | 触发场景 | 前端动作 | 降级语义 |
|---|---|---|---|
| `PROMOTION_COUPON_*` | 发现页联动领券失败 | 阻断成功动效，展示错误码 | 保持搜索结果列表可操作 |
| `PRICE_CALCULATE_POINT_TOTAL_LIMIT_COUNT(1011003004)` | 积分兑换规则超限 | 提示调整数量/规格 | 不清空搜索上下文 |
| `ORDER_NOT_FOUND(1011000011)` | 发现链路回流订单不存在 | 展示可恢复空态与返回列表动作 | 不白屏，不误导成功 |
| `TICKET_SYNC_DEGRADED` | 关联工单/审计链路降级 | 主流程保持成功，展示 warning 标签 | fail-open |
| `version mismatch`（降级信号） | 类目与库存版本不一致 | 强制刷新列表与库存 | 用户可继续浏览，重试有上限 |

## 5. 埋点事件（最小集）
| 事件名 | 触发时机 | 必填属性 |
|---|---|---|
| `page_view` | 进入搜索页/结果页 | `route`, `keyword?`, `categoryId?` |
| `search_submit` | 提交搜索词 | `keyword`, `source`, `historyHit` |
| `search_result_view` | 结果页加载完成 | `keyword`, `resultCount`, `sortField`, `sortAsc` |
| `search_filter_change` | 切换筛选条件 | `sortField`, `sortAsc`, `tabName` |
| `spu_click` | 点击商品卡片 | `spuId`, `position`, `keyword?`, `categoryId?` |
| `order_submit` | 从发现链路进入下单提交 | `spuId`, `orderId`, `resultCode`, `errorCode?` |

## 6. 验收清单
### 6.1 Happy Path
- [ ] 搜索词可驱动商品列表查询，筛选与排序可生效。
- [ ] 商品列表到详情到下单链路可闭环。
- [ ] 发现页营销标签与后端活动状态一致。

### 6.2 业务错误
- [ ] 领券失败展示 `PROMOTION_COUPON_*`，不展示成功动效。
- [ ] 积分规则超限展示 `1011003004` 并可继续调整。
- [ ] 回流订单缺失展示 `1011000011` 可恢复空态。

### 6.3 降级路径
- [ ] 类目/库存版本不一致时触发刷新，不出现空白列表。
- [ ] `TICKET_SYNC_DEGRADED` 仅 warning，不阻断主链路。
- [ ] 网络超时时支持显式重试与返回上一步。
