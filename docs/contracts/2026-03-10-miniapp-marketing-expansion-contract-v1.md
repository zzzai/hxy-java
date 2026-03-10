# MiniApp Marketing Expansion Contract v1 (2026-03-10)

## 1. 目标与真值来源
- 目标：冻结营销扩展域当前真实 `controllerPath + method + path + request/response`，覆盖活动聚合、拼团、秒杀、满减送，以及当前仅后端存在的砍价能力。
- 约束：
  - 只认当前前端 API 文件、当前页面调用、当前 app controller 真值。
  - 不允许 wildcard API、`TBD_*`、基于返回文案的分支判断。
  - 当前无前端绑定但后端已存在的砍价接口，统一显式标记 `ACTIVE_BE_ONLY`。
- 真值输入：
  - 前端 API：
    - `yudao-mall-uniapp/sheep/api/promotion/activity.js`
    - `yudao-mall-uniapp/sheep/api/promotion/combination.js`
    - `yudao-mall-uniapp/sheep/api/promotion/seckill.js`
    - `yudao-mall-uniapp/sheep/api/promotion/rewardActivity.js`
  - 前端调用：
    - `/pages/goods/index.vue`
    - `/pages/activity/index.vue`
    - `/pages/activity/groupon/list.vue`
    - `/pages/activity/groupon/order.vue`
    - `/pages/activity/groupon/detail.vue`
    - `/pages/goods/groupon.vue`
    - `/pages/activity/seckill/list.vue`
    - `/pages/goods/seckill.vue`
    - `sheep/components/s-groupon-block/*`
    - `sheep/components/s-seckill-block/*`
  - 后端 controller：
    - `AppActivityController`
    - `AppCombinationActivityController`
    - `AppCombinationRecordController`
    - `AppSeckillConfigController`
    - `AppSeckillActivityController`
    - `AppRewardActivityController`
    - `AppBargainActivityController`
    - `AppBargainRecordController`
    - `AppBargainHelpController`
  - 文档基线：
    - `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-ops-playbook-v1.md`
    - `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
    - `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`

## 2. 状态定义
- `ACTIVE`：本版 marketing-expansion 无接口进入该状态。
- `ACTIVE_BE_ONLY`：后端真实存在，但当前前端没有 API/页面绑定。
- `PLANNED_RESERVED`：前端真实调用存在，但整域 `promotion.activity-growth` 仍未进入当前发布 `ACTIVE`。
- `BLOCKED`：本版无旧 path 漂移阻断项。

## 3. 页面与前端调用方

| 页面/组件 | 前端调用方 |
|---|---|
| `/pages/goods/index.vue` | `ActivityApi.getActivityListBySpuId`、`RewardActivityApi.getRewardActivity` |
| `/pages/activity/index.vue` | `RewardActivityApi.getRewardActivity` |
| `/pages/activity/groupon/list.vue` | `CombinationApi.getCombinationRecordSummary`、`CombinationApi.getCombinationActivityPage` |
| `/pages/activity/groupon/order.vue` | `CombinationApi.getCombinationRecordPage` |
| `/pages/activity/groupon/detail.vue` | `CombinationApi.getCombinationRecordDetail`、`CombinationApi.getCombinationActivity` |
| `/pages/goods/groupon.vue`、`sheep/components/s-groupon-block/*`、`pages/goods/components/groupon/groupon-card-list.vue` | `CombinationApi.getCombinationActivity`、`CombinationApi.getCombinationActivityListByIds`、`CombinationApi.getHeadCombinationRecordList` |
| `/pages/activity/seckill/list.vue` | `SeckillApi.getSeckillConfigList`、`SeckillApi.getSeckillActivityPage` |
| `/pages/goods/seckill.vue`、`sheep/components/s-seckill-block/*` | `SeckillApi.getSeckillActivity`、`SeckillApi.getSeckillActivityListByIds` |
| 当前无 FE API 文件/页面绑定 | 砍价 `AppBargain*Controller` 全部接口 |

## 4. Marketing Expansion Canonical Contract

| 场景 | 页面/前端调用方 | controllerPath | method + path | request params/body/query | response 字段 | canonical errorCode | 状态 | failureMode | retryClass | degrade 语义 | 发布口径 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 商品营销活动聚合 | `/pages/goods/index.vue` / `ActivityApi.getActivityListBySpuId` | `AppActivityController#getActivityListBySpuId` | `GET /promotion/activity/list-by-spu-id` | query:`spuId(Long)` | `list[]:{id,type,name,spuId,startTime,endTime}` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 返回的是“每种活动至多一条”的聚合结果；空列表为合法空态 | 活动聚合已被 FE 使用，但整个 activity-growth 域仍不允许记为 `ACTIVE` |
| 拼团活动分页 | `/pages/activity/groupon/list.vue` / `CombinationApi.getCombinationActivityPage` | `AppCombinationActivityController#getCombinationActivityPage` | `GET /promotion/combination-activity/page` | query:`pageNo`,`pageSize` | `PageResult<{id,name,userSize,spuId,spuName,picUrl,marketPrice,combinationPrice}>` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 空页为合法空态；无服务端 `degraded` 字段 | 虽有真实页面，但拼团整域仍处于 Ready/补文档阶段 |
| 拼团活动详情 | `/pages/activity/groupon/detail.vue`、`/pages/goods/groupon.vue` / `CombinationApi.getCombinationActivity` | `AppCombinationActivityController#getCombinationActivityDetail` | `GET /promotion/combination-activity/get-detail` | query:`id(Long)` | `{id,name,status,startTime,endTime,userSize,successCount,spuId,totalLimitCount?,singleLimitCount?,products[]:{skuId,combinationPrice}}` 或 `null` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 活动关闭或不存在时返回 `null`，不得前端伪造“已结束活动详情” | 仍不能提升为 `ACTIVE` 发布能力 |
| 拼团活动按 ID 列表 | `sheep/components/s-groupon-block/*` / `CombinationApi.getCombinationActivityListByIds` | `AppCombinationActivityController#getCombinationActivityListByIds` | `GET /promotion/combination-activity/list-by-ids` | query:`ids[]` | `list[]:{id,name,userSize,spuId,spuName,picUrl,marketPrice,combinationPrice}` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 无命中返回 `[]`；只返回启用活动 | 组件级真实接口存在，但域级状态仍不是 `ACTIVE` |
| 拼团头部开团记录 | `pages/goods/components/groupon/groupon-card-list.vue` / `CombinationApi.getHeadCombinationRecordList` | `AppCombinationRecordController#getHeadCombinationRecordList` | `GET /promotion/combination-record/get-head-list` | query:`activityId?`,`status`,`count<=20` | `list[]:{id,activityId,nickname,avatar,expireTime,userSize,userCount,status,orderId,spuName,picUrl,count,combinationPrice}` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 空列表为合法空态 | 拼团记录链路仍属 Ready 态 |
| 我的拼团记录分页 | `/pages/activity/groupon/order.vue` / `CombinationApi.getCombinationRecordPage` | `AppCombinationRecordController#getCombinationRecordPage` | `GET /promotion/combination-record/page` | query:`pageNo`,`pageSize`,`status?` | `PageResult<{id,activityId,nickname,avatar,expireTime,userSize,userCount,status,orderId,spuName,picUrl,count,combinationPrice}>` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 空页为合法空态 | 不能因为订单页已运行就把拼团能力计入已上线营销扩展 |
| 拼团记录详情 | `/pages/activity/groupon/detail.vue` / `CombinationApi.getCombinationRecordDetail` | `AppCombinationRecordController#getCombinationRecordDetail` | `GET /promotion/combination-record/get-detail` | query:`id(Long)` | `{headRecord,memberRecords[],orderId}` 或 `null` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 记录不存在时返回 `null`，不得按提示文案做恢复分支 | 仍属 Ready 态 |
| 拼团记录概要 | `/pages/activity/groupon/list.vue` / `CombinationApi.getCombinationRecordSummary` | `AppCombinationRecordController#getCombinationRecordSummary` | `GET /promotion/combination-record/get-summary` | 无 | `{userCount,avatars[]}` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 无人参团时返回 `userCount=0, avatars=[]` | 仅作为页面摘要真值，不能单独推高域级状态 |
| 秒杀时间段列表 | `/pages/activity/seckill/list.vue` / `SeckillApi.getSeckillConfigList` | `AppSeckillConfigController#getSeckillConfigList` | `GET /promotion/seckill-config/list` | 无 | `list[]:{id,startTime,endTime,sliderPicUrls}` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 空列表为合法空态 | 秒杀页面存在，但秒杀域仍不是 `ACTIVE` |
| 当前秒杀活动 | 当前无 FE 绑定 | `AppSeckillActivityController#getNowSeckillActivity` | `GET /promotion/seckill-activity/get-now` | 无 | `{config,activities[]}` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 当前无秒杀时返回空结构对象；有 10 秒缓存 | 后端真实存在但前端未实际调用，不得默认纳入页面 contract |
| 秒杀活动分页 | `/pages/activity/seckill/list.vue` / `SeckillApi.getSeckillActivityPage` | `AppSeckillActivityController#getSeckillActivityPage` | `GET /promotion/seckill-activity/page` | query:`pageNo`,`pageSize`,`configId?` | `PageResult<{id,name,spuId,spuName,picUrl,marketPrice,status,stock,totalStock,seckillPrice}>` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 空页为合法空态 | 秒杀域仍处于 Ready 态 |
| 秒杀活动详情 | `/pages/goods/seckill.vue` / `SeckillApi.getSeckillActivity` | `AppSeckillActivityController#getSeckillActivity` | `GET /promotion/seckill-activity/get-detail` | query:`id(Long)` | `{id,name,status,startTime,endTime,spuId,totalLimitCount?,singleLimitCount?,stock,totalStock,products[]:{skuId,seckillPrice,stock}}` 或 `null` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 活动不存在/关闭返回 `null` | 仍不允许进入当前发布 `ACTIVE` |
| 秒杀活动按 ID 列表 | `sheep/components/s-seckill-block/*` / `SeckillApi.getSeckillActivityListByIds` | `AppSeckillActivityController#getCombinationActivityListByIds` | `GET /promotion/seckill-activity/list-by-ids` | query:`ids[]` | `list[]:{id,name,spuId,spuName,picUrl,marketPrice,status,stock,totalStock,seckillPrice}` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 无命中返回 `[]` | 组件级真实接口存在，但域级状态保持 Ready |
| 满减送活动详情 | `/pages/activity/index.vue`、`/pages/goods/index.vue` / `RewardActivityApi.getRewardActivity` | `AppRewardActivityController#getRewardActivity` | `GET /promotion/reward-activity/get` | query:`id(Long)` | `{id,status,name,startTime,endTime,conditionType,productScope,productScopeValues,rules[]:{limit,discountPrice?,point?,description}}` 或 `null` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 活动不存在返回 `null`；不返回服务端 `degraded` 字段 | 满减送当前只能作为 Ready 态 contract 真值，不可计入 `ACTIVE` |
| 砍价活动列表 | 当前无 FE 绑定 | `AppBargainActivityController#getBargainActivityList` | `GET /promotion/bargain-activity/list` | query:`count?` | `list[]:{id,name,startTime,endTime,spuId,skuId,stock,picUrl,marketPrice,bargainMinPrice}` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 空列表为合法空态；当前无页面消费 | 后端真实存在，前端未绑定；不得凭 ops 文档把砍价记为已上线 |
| 砍价活动分页 | 当前无 FE 绑定 | `AppBargainActivityController#getBargainActivityPage` | `GET /promotion/bargain-activity/page` | query:`pageNo`,`pageSize` | `PageResult<{id,name,startTime,endTime,spuId,skuId,stock,picUrl,marketPrice,bargainMinPrice}>` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 空页为合法空态 | 仅代表后端真值存在 |
| 砍价活动详情 | 当前无 FE 绑定 | `AppBargainActivityController#getBargainActivityDetail` | `GET /promotion/bargain-activity/get-detail` | query:`id(Long)` | `{id,name,startTime,endTime,spuId,skuId,price,description,stock,picUrl,marketPrice,bargainFirstPrice,bargainMinPrice,successUserCount}` 或 `null` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 活动不存在返回 `null` | 当前无前端消费，不得进入 FE allowlist |
| 砍价记录概要 | 当前无 FE 绑定 | `AppBargainRecordController#getBargainRecordSummary` | `GET /promotion/bargain-record/get-summary` | 无 | `{successUserCount,successList[]}` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 无成功样本时返回空结构 | 当前只存在后端真值 |
| 砍价记录详情 | 当前无 FE 绑定 | `AppBargainRecordController#getBargainRecordDetail` | `GET /promotion/bargain-record/get-detail` | query:`id?`,`activityId?`，二者至少一项 | `{id,userId,spuId,skuId,activityId,bargainFirstPrice,bargainPrice,status,orderId?,payStatus?,payOrderId?,helpAction}` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 当前 controller 对空 `id/activityId` 走断言，不在前端 contract 范围内 | 后端真实存在，当前无 FE 消费 |
| 我的砍价记录分页 | 当前无 FE 绑定 | `AppBargainRecordController#getBargainRecordPage` | `GET /promotion/bargain-record/page` | query:`pageNo`,`pageSize` | `PageResult<{id,spuId,skuId,activityId,status,bargainPrice,activityName,endTime,picUrl,orderId?,payStatus?,payOrderId?}>` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 空页为合法空态 | 后端真实存在，当前无 FE 消费 |
| 创建砍价记录 | 当前无 FE 绑定 | `AppBargainRecordController#createBargainRecord` | `POST /promotion/bargain-record/create` | body:`activityId(Long)` | `recordId(Long)` | `BARGAIN_ACTIVITY_NOT_EXISTS(1013012000)`、`BARGAIN_RECORD_CREATE_FAIL_EXISTS(1013013001)`、`BARGAIN_RECORD_CREATE_FAIL_LIMIT(1013013002)` | `ACTIVE_BE_ONLY` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | 无 `degraded` 字段；重复参与或超限必须直接失败 | 当前无 FE/API 文件，不得自行接入后误记上线 |
| 创建砍价助力 | 当前无 FE 绑定 | `AppBargainHelpController#createBargainHelp` | `POST /promotion/bargain-help/create` | body:`recordId(Long)` | `reducePrice(Integer)` | `BARGAIN_HELP_CREATE_FAIL_RECORD_NOT_IN_PROCESS(1013014000)`、`BARGAIN_HELP_CREATE_FAIL_RECORD_SELF(1013014001)`、`BARGAIN_HELP_CREATE_FAIL_LIMIT(1013014002)`、`BARGAIN_HELP_CREATE_FAIL_HELP_EXISTS(1013014004)` | `ACTIVE_BE_ONLY` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | 无 `degraded` 字段；助力失败必须按 errorCode 做显式恢复 | 当前无 FE/API 文件，不得默认纳入 FE 发布能力 |
| 砍价助力列表 | 当前无 FE 绑定 | `AppBargainHelpController#getBargainHelpList` | `GET /promotion/bargain-help/list` | query:`recordId(Long)` | `list[]:{userId,nickname,avatar,reducePrice,createTime}` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 空列表为合法空态 | 仅代表后端真值存在 |

## 5. 域级 canonical 约束
- `GET /promotion/activity/list-by-spu-id` 只负责返回“商品当前可参与的营销活动摘要”，不是首页聚合活动配置，不得与 `/promotion/activity/home-blocks` 混用。
- 本域当前没有服务端 `degraded/degradeReason` 字段；降级口径来自：
  - 查询接口返回 `[]` / `null`
  - D 侧 ops playbook 中的人工/运营关闭动作
- 拼团、秒杀、满减送当前前端链路虽存在，但 capability ledger 仍固定为 `promotion.activity-growth = PLANNED_RESERVED`。
- 砍价 controller 已存在，但当前缺真实 FE API 文件和页面绑定，因此统一只能是 `ACTIVE_BE_ONLY`。

## 6. 发布口径
- 当前分支营销扩展域没有任何接口可直接升为 `ACTIVE`。
- 拼团、秒杀、满减送、活动聚合：
  - 前端真实在调
  - 但仍只到 `PLANNED_RESERVED`
  - 不能计入正式发布口径
- 砍价：
  - 后端真实存在
  - 当前无 FE 绑定
  - 固定 `ACTIVE_BE_ONLY`

## 7. 跨窗口联调约束
- A 窗口
  - 不能把“页面上能看到活动卡片”当成营销扩展域已上线；当前只能视为 Ready/BE-only 真值输入。
- B 窗口
  - 拼团/秒杀/满减送详情返回 `null`、砍价查询返回 `[]` 都是合法协议，不能靠提示文案推断“活动结束/失效”。
- D 窗口
  - 本域没有服务端 `degraded` 字段；所有关闭、回退、隐藏 CTA 都应继续引用 `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-ops-playbook-v1.md` 的运营动作，而不是杜撰后端降级返回。
