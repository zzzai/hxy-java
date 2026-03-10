# MiniApp 营销扩展域 PRD v1（2026-03-10）

## 0. 文档定位
- 目标：用真实页面、真实 app API、真实活动承接边界，冻结秒杀、拼团、砍价、满减送、活动聚合的产品口径。
- 分支：`feat/ui-four-account-reconcile-ops`
- 约束：
  - 只认当前 uniapp 可访问页面和真实 app controller。
  - 不把后台存在的砍价 API 直接写成已产品化页面。
  - 不把 `s-activity-pop -> /pages/activity/index` 误写成统一营销活动中心。
- 对齐基线：
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
  - `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
  - `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-ops-playbook-v1.md`

## 1. 业务目标与非目标

### 1.1 业务目标
- 让秒杀、拼团、满减送在小程序内有清晰的真实用户入口、真实状态机、真实错误恢复动作。
- 明确商品详情页里的活动聚合只是“活动入口聚合”，不是统一营销中心。
- 明确砍价当前处于“后端存在但用户侧未产品化”状态，防止误发布。

### 1.2 非目标
- 不新增砍价页面、统一活动首页、活动聚合 tab 页。
- 不设计活动投放策略、预算、算法推荐。
- 不把后台活动配置页映射成用户侧页面。

## 2. 能力总览

| 能力 | 真实页面 / 入口 | 状态 | 真实 API | 结论 |
|---|---|---|---|---|
| 秒杀列表 | `/pages/activity/seckill/list` | `ACTIVE` | `/promotion/seckill-config/list` `/promotion/seckill-activity/page` | 真实秒杀列表页 |
| 秒杀详情 / 下单 | `/pages/goods/seckill?id=` | `ACTIVE` | `/promotion/seckill-activity/get-detail` `/product/spu/get-detail` | 活动详情与下单页 |
| 拼团列表 | `/pages/activity/groupon/list` | `ACTIVE` | `/promotion/combination-record/get-summary` `/promotion/combination-activity/page` | 真实拼团活动列表页 |
| 拼团商品页 | `/pages/goods/groupon?id=` | `ACTIVE` | `/promotion/combination-activity/get-detail` `/product/spu/get-detail` | 开团 / 参团入口 |
| 我的拼团 | `/pages/activity/groupon/order` | `ACTIVE` | `/promotion/combination-record/page` | 我的拼团记录页 |
| 拼团详情 | `/pages/activity/groupon/detail?id=` | `ACTIVE` | `/promotion/combination-record/get-detail` | 拼团进度、邀请好友页 |
| 满减送单活动页 | `/pages/activity/index?activityId=` | `ACTIVE` | `/promotion/reward-activity/get` `/product/spu/page` `/trade/order/settlement-product` | 当前只承接指定活动页 |
| 商品详情活动聚合 | `/pages/goods/index?id=` | `ACTIVE`（受类型约束） | `/promotion/activity/list-by-spu-id` `/promotion/reward-activity/get` | 秒杀 / 拼团可跳；砍价不可跳 |
| 砍价 app API | 当前无真实用户页 | `PLANNED_RESERVED` | `/promotion/bargain-activity/*` `/promotion/bargain-record/*` `/promotion/bargain-help/*` | 后端已存在，前端未产品化 |
| 统一活动聚合 / 首页活动块 | 当前无真实用户页 | `PLANNED_RESERVED` | 冻结文档中的聚合接口 | 纯规划，不是运行时能力 |

## 3. 用户场景与页面流转

### 3.1 秒杀
1. 用户进入 `/pages/activity/seckill/list` 查看时间段和活动商品。
2. 用户点击“马上抢”进入 `/pages/goods/seckill?id={activityId}`。
3. 秒杀详情页拉活动详情和商品详情，组装活动价、库存、限购、倒计时。
4. 用户确认规格后走订单确认页，`buy_type=seckill`。

### 3.2 拼团
1. 用户进入 `/pages/activity/groupon/list` 查看拼团活动。
2. 用户点击商品进入 `/pages/goods/groupon?id={activityId}`。
3. 用户可开团或参团；下单时透传 `combinationActivityId`，参团场景还要传 `combinationHeadId`。
4. 用户可在 `/pages/activity/groupon/order` 查看我的拼团记录。
5. 用户可在 `/pages/activity/groupon/detail?id={recordId}` 查看拼团状态并邀请好友。

### 3.3 满减送与活动聚合
1. 用户在普通商品详情页打开活动弹窗。
2. 满减送 CTA 真实跳 `/pages/activity/index?activityId={id}`。
3. 商品详情页活动聚合接口可能返回秒杀、拼团、砍价；当前前端只对 `type=1` 秒杀和 `type=3` 拼团做可跳转处理。
4. 若返回 `type=2` 砍价，当前前端无对应页面，必须隐藏或忽略。

## 4. 页面 route 真值

| 页面 route | 真实参数 | 页面角色 | 当前真值说明 |
|---|---|---|---|
| `/pages/activity/seckill/list` | 无 | 秒杀列表页 | 真实列表页，时间段来自接口，不靠本地常量 |
| `/pages/goods/seckill` | `id` | 秒杀活动详情页 | `id` 是活动 ID，不是 `spuId` |
| `/pages/activity/groupon/list` | 无 | 拼团列表页 | 列表页同时展示参与人数摘要 |
| `/pages/goods/groupon` | `id` | 拼团商品页 | `id` 是活动 ID，不是拼团记录 ID |
| `/pages/activity/groupon/order` | `type?` | 我的拼团记录页 | `type` 只切 tab，不改业务主键 |
| `/pages/activity/groupon/detail` | `id` | 拼团记录详情页 | `id` 是拼团记录 ID |
| `/pages/activity/index` | `activityId` | 满减送单活动页 | 不是统一活动聚合页 |
| `/pages/goods/bargain` | 无真实 route | 缺页能力 | 当前前端没有任何 bargain 页面 |

## 5. 页面 -> API -> 字段关系

### 5.1 秒杀

| 页面 | API | 请求字段 | 响应字段 | 页面使用 |
|---|---|---|---|---|
| `/pages/activity/seckill/list` | `GET /promotion/seckill-config/list` | 无 | `list[].id` `startTime` `endTime` | 时间段轮播与状态 |
| `/pages/activity/seckill/list` | `GET /promotion/seckill-activity/page` | `configId` `pageNo` `pageSize` | `list[].id` `spuId` `name` `seckillPrice` `stock` `totalStock` `unitName` `sliderPicUrls` `total` | 秒杀活动列表 |
| `/pages/goods/seckill` | `GET /promotion/seckill-activity/get-detail` | `id` | `id` `spuId` `startTime` `endTime` `singleLimitCount` `products[]` `stock` `totalStock` | 秒杀详情、活动价、限购 |
| `/pages/goods/seckill` | `GET /product/spu/get-detail` | `id=spuId` | `id` `name` `picUrl` `sliderPicUrls` `price` `marketPrice` `stock` `skus[]` | 商品详情基础数据 |

### 5.2 拼团

| 页面 | API | 请求字段 | 响应字段 | 页面使用 |
|---|---|---|---|---|
| `/pages/activity/groupon/list` | `GET /promotion/combination-record/get-summary` | 无 | `avatars[]` `userCount` | 列表页顶部参与人数摘要 |
| `/pages/activity/groupon/list` | `GET /promotion/combination-activity/page` | `pageNo` `pageSize` | `list[].id` `spuId` `name` `picUrl` `introduction` `combinationPrice` `total` | 拼团活动列表 |
| `/pages/goods/groupon` | `GET /promotion/combination-activity/get-detail` | `id` | `id` `spuId` `status` `startTime` `endTime` `price` `products[]` | 开团 / 参团页 |
| `/pages/goods/groupon` | `GET /product/spu/get-detail` | `id=spuId` | `id` `name` `picUrl` `sliderPicUrls` `price` `marketPrice` `stock` `skus[]` | 商品详情基础数据 |
| `/pages/activity/groupon/order` | `GET /promotion/combination-record/page` | `pageNo` `pageSize` `status?` | `list[].id` `orderId` `picUrl` `spuName` `combinationPrice` `userSize` `status` `total` | 我的拼团列表 |
| `/pages/activity/groupon/detail` | `GET /promotion/combination-record/get-detail` | `id` | `headRecord` `memberRecords[]` `orderId` | 拼团进度与邀请页 |

### 5.3 满减送与活动聚合

| 页面 | API | 请求字段 | 响应字段 | 页面使用 |
|---|---|---|---|---|
| `/pages/goods/index` | `GET /promotion/activity/list-by-spu-id` | `spuId` | `[{id,type,name,spuId,startTime,endTime}]` | 秒杀 / 砍价 / 拼团聚合入口 |
| `/pages/goods/index` `/pages/activity/index` | `GET /promotion/reward-activity/get` | `id` | `id` `title` `rules[]` `startTime` `endTime` `productScope` `productSpuIds` | 满减送标题、规则、时间窗、商品范围 |
| `/pages/activity/index` | `GET /product/spu/page` | `ids?` `categoryIds?` `pageNo` `pageSize` | `list[].id` `name` `picUrl` `price` `marketPrice` `total` | 满减送活动商品列表 |
| `/pages/activity/index` | `GET /trade/order/settlement-product` | `ids` | `rewardActivity` 等营销扩展字段 | 商品列表活动标签 |

### 5.4 砍价：后端存在但前端未产品化

| 能力 | 真实 API | 当前状态 |
|---|---|---|
| 砍价活动列表 / 分页 / 详情 | `/promotion/bargain-activity/list` `/promotion/bargain-activity/page` `/promotion/bargain-activity/get-detail` | 后端存在，前端无页面 |
| 砍价记录 | `/promotion/bargain-record/get-summary` `/page` `/get-detail` `/create` | 后端存在，前端无页面 |
| 砍价助力 | `/promotion/bargain-help/list` `/create` | 后端存在，前端无页面 |

## 6. `ACTIVE / PLANNED_RESERVED / 缺页能力` 分层

### 6.1 `ACTIVE`
- 秒杀列表、秒杀详情 / 下单。
- 拼团列表、拼团商品页、我的拼团、拼团详情。
- 满减送单活动页。
- 商品详情营销聚合，但只对秒杀和拼团提供可跳转入口。

### 6.2 `PLANNED_RESERVED`

#### 6.2.1 后端存在但未产品化
- 砍价活动、砍价记录、砍价助力。
- 商品详情聚合中若返回 `type=2`，只能作为后端存在信号，不能视为已上线用户能力。

#### 6.2.2 纯规划能力
- 统一营销活动中心页。
- 首页活动聚合块 / 活动首页 tab。
- 统一活动搜索、统一活动排序、统一活动回放页。

### 6.3 缺页能力
- `/pages/goods/bargain`
- `/pages/activity/bargain/list`
- `/pages/activity/bargain/detail`
- 统一活动首页 / 聚合首页

## 7. 错误码与用户恢复动作

| 错误码 | 场景 | 用户侧动作 | 产品约束 |
|---|---|---|---|
| `1013008000 SECKILL_ACTIVITY_NOT_EXISTS` | 秒杀详情页 | 回商品列表或上一页 | 不得展示“活动进行中” |
| `1013008007 SECKILL_JOIN_ACTIVITY_TIME_ERROR` | 秒杀时间窗外下单 | 刷新活动时间并禁用 CTA | 不得继续放开购买按钮 |
| `1013008008 SECKILL_JOIN_ACTIVITY_STATUS_CLOSED` | 秒杀已关闭 | 回退上一页 | 不得宣称抢购成功 |
| `1013008009 SECKILL_JOIN_ACTIVITY_SINGLE_LIMIT_COUNT_EXCEED` | 超单次限购 | 保留规格弹窗，提示调整数量 | 不得直接建单 |
| `1013008010 SECKILL_JOIN_ACTIVITY_PRODUCT_NOT_EXISTS` | 活动商品不存在 | 回秒杀列表 | 不得回落成普通秒杀成功页 |
| `1011003003 PRICE_CALCULATE_SECKILL_TOTAL_LIMIT_COUNT` | 秒杀总限购超限 | 停留当前页并提示数量超限 | 不得继续支付 |
| `1013010000 COMBINATION_ACTIVITY_NOT_EXISTS` | 拼团活动不存在 | 回拼团列表或上一页 | 不得展示成团按钮 |
| `1013010004 COMBINATION_ACTIVITY_STATUS_DISABLE` | 拼团已关闭 | 关闭开团 / 参团 CTA | 不得继续邀请好友 |
| `1013010005 COMBINATION_JOIN_ACTIVITY_PRODUCT_NOT_EXISTS` | 拼团商品不存在 | 回列表 | 不得创建拼团单 |
| `1013010006 COMBINATION_ACTIVITY_UPDATE_STOCK_FAIL` | 拼团库存不足 | 刷新库存并阻断下单 | 不得宣称拼团成功 |
| `1013011000-1013011009 COMBINATION_RECORD_*` | 拼团记录不存在 / 人数满 / 超时 / 已参与等 | 留在拼团详情页或回活动页重试 | 不得伪造“已参团” |
| `1013006000 REWARD_ACTIVITY_NOT_EXISTS` | 满减送活动页 | 回普通商品详情或上一页 | 不得继续展示优惠规则 |
| `1013006005 REWARD_ACTIVITY_SCOPE_EXISTS` | 满减送规则冲突 | 隐藏冲突规则并转运营处理 | 不得继续宣称优惠可用 |
| `1013012000-1013014004 BARGAIN_*` | 砍价后端接口 | 当前前端若命中即视为误接 / 误路由 | 不得尝试深链到不存在页面 |

## 8. 降级语义与禁止伪成功规则

| 场景 | 降级类型 | 允许行为 | 禁止行为 |
|---|---|---|---|
| 商品详情活动聚合失败 | `fail-open` | 保留普通商品购买能力，隐藏营销 tip | 伪装成活动仍可参加 |
| 秒杀详情加载失败 | `fail-close` | 回退列表或普通商品页 | 继续保留秒杀价和 CTA |
| 拼团活动 / 记录失败 | `fail-close` | 回退列表或详情页重刷 | 宣称已拼团成功 |
| 满减送活动页失败 | `fail-close` | 回普通商品详情或返回上一页 | 展示过期的规则与赠品 |
| 砍价聚合命中 | `fail-close` | 当前前端直接忽略 `type=2` 或隐藏入口 | deep-link 到不存在的 bargain 页面 |

### 8.1 禁止伪成功
- 进入 `/pages/goods/seckill`、`/pages/goods/groupon` 不代表活动可下单，必须以后端活动状态和时间窗为准。
- 拼团详情页展示“恭喜您~拼团成功”只能发生在拼团记录 `status=1` 且用户自身有对应记录时。
- 满减送活动页只要 `GET /promotion/reward-activity/get` 失败，就不能继续渲染活动可用状态。

## 9. 是否阻断开发、是否阻断发布

| 判断项 | 结论 | 说明 |
|---|---|---|
| 现有秒杀 / 拼团 / 满减送页面继续开发是否阻断 | `否` | 真实 route 与真实 app API 已存在 |
| 把砍价前端页面纳入开发是否阻断 | `是` | 当前无任何 bargain uniapp 页面与路由 |
| 仅按当前已承接活动发布是否阻断 | `否` | 仅限秒杀、拼团、满减送、商品详情聚合 |
| 若把 bargain 或统一活动中心写成已上线是否阻断发布 | `是` | 会造成营销能力假冻结与错误联调 |

## 10. 验收清单
- [ ] 秒杀、拼团、满减送、活动聚合的真实 route 与真实 API 被明确记录。
- [ ] 砍价被明确归类为“后端存在但前端未产品化”，没有被写成已上线页面。
- [ ] 文档明确 `s-activity-pop -> /pages/activity/index` 只是满减送单活动页，不是统一营销中心。
- [ ] 商品详情活动聚合返回 `type=2 bargain` 时的隐藏 / 忽略规则明确。
- [ ] 错误码、恢复动作、降级语义覆盖秒杀、拼团、满减送、砍价误接四类场景。
