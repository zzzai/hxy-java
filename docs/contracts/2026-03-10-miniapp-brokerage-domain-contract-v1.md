# MiniApp Brokerage Domain Contract v1 (2026-03-10)

## 1. 目标与真值来源
- 目标：冻结分销/佣金域当前真实 `controllerPath + method + path + request/response`，覆盖绑定、概览、排行、团队、佣金记录、提现、商品佣金区间。
- 约束：
  - 只认当前前端 API 文件、当前页面调用、当前 app controller。
  - 不允许 wildcard API、`TBD_*`、基于返回文案的分支判断。
  - 资金类写操作统一按 errorCode 驱动，禁止“看提示文案判断是否到账”。
- 真值输入：
  - 前端 API：`yudao-mall-uniapp/sheep/api/trade/brokerage.js`
  - 前端调用：
    - `/pages/commission/team.vue`
    - `/pages/commission/commission-ranking.vue`
    - `/pages/commission/promoter.vue`
    - `/pages/commission/wallet.vue`
    - `/pages/commission/withdraw.vue`
    - `/pages/commission/order.vue`
    - `/pages/commission/goods.vue`
    - `yudao-mall-uniapp/sheep/platform/share.js`
  - 后端 controller：
    - `AppBrokerageUserController`
    - `AppBrokerageRecordController`
    - `AppBrokerageWithdrawController`
  - 文档基线：
    - `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
    - `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`
    - `docs/plans/2026-03-10-miniapp-brokerage-domain-runbook-v1.md`

## 2. 状态定义
- `ACTIVE`：本版 brokerage 无接口进入该状态。
- `ACTIVE_BE_ONLY`：本版 brokerage 无前端缺绑接口。
- `PLANNED_RESERVED`：接口与页面真实存在，但整域仍处于 `BACKLOG-DOC-GAP` 收口中，不能计入当前发布 `ACTIVE`。
- `BLOCKED`：本版无旧 path 漂移阻断项。

## 3. 页面与前端调用方

| 页面/组件 | 前端调用方 |
|---|---|
| `yudao-mall-uniapp/sheep/platform/share.js` | `BrokerageApi.bindBrokerageUser` |
| `/pages/commission/components/commission-auth.vue`、`/pages/commission/withdraw.vue` | `BrokerageApi.getBrokerageUser` |
| `/pages/commission/team.vue`、`/pages/commission/wallet.vue`、`/pages/commission/components/account-info.vue` | `BrokerageApi.getBrokerageUserSummary` |
| `/pages/commission/order.vue`、`/pages/commission/wallet.vue`、`/pages/commission/components/commission-log.vue` | `BrokerageApi.getBrokerageRecordPage` |
| `/pages/commission/wallet.vue`、`/pages/commission/withdraw.vue` | `BrokerageApi.createBrokerageWithdraw` |
| `/pages/commission/wallet.vue` | `BrokerageApi.getBrokerageWithdrawPage`、`BrokerageApi.getBrokerageWithdraw` |
| `/pages/commission/goods.vue` | `BrokerageApi.getProductBrokeragePrice` |
| `/pages/commission/commission-ranking.vue` | `BrokerageApi.getRankByPrice`、`BrokerageApi.getBrokerageUserChildSummaryPageByPrice` |
| `/pages/commission/promoter.vue` | `BrokerageApi.getBrokerageUserRankPageByUserCount` |
| `/pages/commission/team.vue` | `BrokerageApi.getBrokerageUserChildSummaryPage` |

## 4. Brokerage Canonical Contract

| 场景 | 页面/前端调用方 | controllerPath | method + path | request params/body/query | response 字段 | canonical errorCode | 状态 | failureMode | retryClass | degrade 语义 | 发布口径 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 绑定推广员 | `sheep/platform/share.js` / `BrokerageApi.bindBrokerageUser` | `AppBrokerageUserController#bindBrokerageUser` | `PUT /trade/brokerage-user/bind` | body:`bindUserId(Long)` | `true` | `BROKERAGE_BIND_SELF(1011007002)`、`BROKERAGE_BIND_USER_NOT_ENABLED(1011007003)`、`BROKERAGE_BIND_MODE_REGISTER(1011007005)`、`BROKERAGE_BIND_OVERRIDE(1011007006)`、`BROKERAGE_BIND_LOOP(1011007007)` | `PLANNED_RESERVED` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | 无 `degraded` 字段；绑定冲突或无资格时必须直接按 errorCode 阻断 | 真实 path 已落地，但整域仍未进入发布 `ACTIVE`；不得因为 share 入口可用就改写 capability 状态 |
| 获取个人分销信息 | `/pages/commission/components/commission-auth.vue`、`/pages/commission/withdraw.vue` / `BrokerageApi.getBrokerageUser` | `AppBrokerageUserController#getBrokerageUser` | `GET /trade/brokerage-user/get` | 无 | `{brokerageEnabled,brokeragePrice,frozenPrice}` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | controller 内部 `getOrCreate`，无记录时也返回结构化对象 | 仅作为 Ready 态 domain truth，不计入 `ACTIVE` 发布范围 |
| 获取个人分销统计 | `/pages/commission/team.vue`、`/pages/commission/wallet.vue`、`components/account-info.vue` / `BrokerageApi.getBrokerageUserSummary` | `AppBrokerageUserController#getBrokerageUserSummary` | `GET /trade/brokerage-user/get-summary` | 无 | `{yesterdayPrice,withdrawPrice,brokeragePrice,frozenPrice,firstBrokerageUserCount,secondBrokerageUserCount}` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 无 `degraded` 字段；统计空值按 `0` 返回，不做文案推断 | 仍属 brokerage.center Ready 态，不能纳入当前发布已上线能力 |
| 获取分销记录分页 | `/pages/commission/order.vue`、`/pages/commission/wallet.vue`、`components/commission-log.vue` / `BrokerageApi.getBrokerageRecordPage` | `AppBrokerageRecordController#getBrokerageRecordPage` | `GET /trade/brokerage-record/page` | query:`pageNo`,`pageSize`,`createTime?[]`,`bizType?`,`status?` | `PageResult<{id,bizId,title,price,status,statusName,createTime,finishTime}>` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 空页为合法空态；无服务端 `degraded` 字段 | 真实分页协议已固定；前端手工拼 query string 不改变 canonical query 键集合 |
| 创建分销提现 | `/pages/commission/wallet.vue`、`/pages/commission/withdraw.vue` / `BrokerageApi.createBrokerageWithdraw` | `AppBrokerageWithdrawController#createBrokerageWithdraw` | `POST /trade/brokerage-withdraw/create` | body:`type`,`price`,`userAccount`,`userName?`,`qrCodeUrl?`,`bankName?`,`bankAddress?`,`transferChannelCode?` | `withdrawId(Long)` | `BROKERAGE_WITHDRAW_MIN_PRICE(1011008002)`、`BROKERAGE_WITHDRAW_USER_BALANCE_NOT_ENOUGH(1011008003)` | `PLANNED_RESERVED` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | 无 `degraded` 字段；提现失败必须显式回显错误码，不允许前端预设“提现成功” | 提现属资金写链路；即使 path 对齐，也只能保持 `PLANNED_RESERVED`，等待 B 侧业务口径冻结 |
| 获取提现分页 | `/pages/commission/wallet.vue` / `BrokerageApi.getBrokerageWithdrawPage` | `AppBrokerageWithdrawController#getBrokerageWithdrawPage` | `GET /trade/brokerage-withdraw/page` | query:`pageNo`,`pageSize`,`createTime?[]` | `PageResult<{id,type,typeName,status,statusName,price,createTime,payTransferId?,transferChannelPackageInfo?,transferChannelMchId?}>` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 空页为合法空态；无 `degraded` 字段 | 只作为资金查询真值，不代表提现域已通过发布门禁 |
| 获取提现详情 | `/pages/commission/wallet.vue` / `BrokerageApi.getBrokerageWithdraw` | `AppBrokerageWithdrawController#getBrokerageWithdraw` | `GET /trade/brokerage-withdraw/get` | query:`id(Long)` | `{id,type,typeName,status,statusName,price,createTime,payTransferId?,transferChannelPackageInfo?,transferChannelMchId?}` 或 `null` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 不存在或非本人提现单时返回 `null`，这是当前 controller 真值 | 客服/财务不能把 `null` 解释成“已到账”；只能提示刷新或回退列表 |
| 获取商品佣金区间 | `/pages/commission/goods.vue` / `BrokerageApi.getProductBrokeragePrice` | `AppBrokerageRecordController#getProductBrokeragePrice` | `GET /trade/brokerage-record/get-product-brokerage-price` | query:`spuId(Long)` | `{enabled,brokerageMinPrice,brokerageMaxPrice}` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 无 `degraded` 字段；若无佣金规则则应返回 `enabled=false` 而非前端猜测 | 商品页佣金展示也不构成 brokerage 已上线依据 |
| 获取佣金排行名次 | `/pages/commission/commission-ranking.vue` / `BrokerageApi.getRankByPrice` | `AppBrokerageUserController#getRankByPrice` | `GET /trade/brokerage-user/get-rank-by-price` | query:`times=start`,`times=end`，格式 `yyyy-MM-dd HH:mm:ss` | `rank(Integer)` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 无 `degraded` 字段；排行为空时返回当前服务端结果 | 真实协议使用双值重复 query `times`，不得改写成标签或非结构化文本协议 |
| 获取佣金排行分页 | `/pages/commission/commission-ranking.vue` / `BrokerageApi.getBrokerageUserChildSummaryPageByPrice` | `AppBrokerageUserController#getBrokerageUserChildSummaryPageByPrice` | `GET /trade/brokerage-user/rank-page-by-price` | query:`pageNo`,`pageSize`,`times=start`,`times=end` | `PageResult<{id,nickname,avatar,brokeragePrice}>` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 空页为合法空态；无 `degraded` 字段 | 排行分页是领域 Ready 态输入，不自动等价于可发布能力 |
| 获取团队排行分页（按人数） | `/pages/commission/promoter.vue` / `BrokerageApi.getBrokerageUserRankPageByUserCount` | `AppBrokerageUserController#getBrokerageUserRankPageByUserCount` | `GET /trade/brokerage-user/rank-page-by-user-count` | query:`pageNo`,`pageSize`,`times=start`,`times=end` | `PageResult<{id,nickname,avatar,brokerageUserCount}>` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 空页为合法空态；无 `degraded` 字段 | 仍只作为 Ready 态 contract 真值 |
| 获取下级分销统计分页 | `/pages/commission/team.vue` / `BrokerageApi.getBrokerageUserChildSummaryPage` | `AppBrokerageUserController#getBrokerageUserChildSummaryPage` | `GET /trade/brokerage-user/child-summary-page` | query:`pageNo`,`pageSize`,`nickname?`,`sortingField?`,`level(1|2)` | `PageResult<{id,nickname,avatar,brokeragePrice,brokerageOrderCount,brokerageUserCount,brokerageTime}>` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 空页为合法空态；无 `degraded` 字段 | 团队页当前真实 path 已固定，但整域仍不能提升为 `ACTIVE` |

## 5. 域级 canonical 约束
- 分销域当前没有服务端 `degraded/degradeReason` 字段；所有“处理中”“待审核”都必须来自显式状态字段，而不是模糊非结构化文本。
- 所有时间范围查询统一用 `times=start&times=end` 或 `createTime[]` 真实 query 结构，不接受自定义文本时间段别名。
- 提现详情的微信转账附加字段只有在：
  - `status = AUDIT_SUCCESS`
  - `type = WECHAT_API`
  - `payTransferId != null`
  才可能返回 `transferChannelPackageInfo/transferChannelMchId`。

## 6. 发布口径
- 当前分支分销页和后端 controller 全部真实存在，但 capability ledger 仍固定为 `CAP-BROKERAGE-001 = PLANNED_RESERVED / BACKLOG-DOC-GAP`。
- 因此本文件完成后，只能说明 brokerage contract 真值已补齐，不能直接把分销中心、提现、排行记为已上线 `ACTIVE`。
- 任何窗口都不得把“页面可访问”“接口已回 200”当成 brokerage 正式放行依据；正式放行仍需：
  - B 侧分销 PRD 冻结
  - A 侧 capability / freeze review 更新
  - D 侧 runbook 与资金门禁闭环一致

## 7. 跨窗口联调约束
- A 窗口
  - 不得因为分销页面和 controller 已存在，就把 `CAP-BROKERAGE-001` 提前改成 `ACTIVE`。
- B 窗口
  - 所有提现失败、绑定冲突都必须按显式 `errorCode` 做 UI/客服口径，不得用“处理中/系统繁忙”兜底覆盖。
- D 窗口
  - 资金类写链路无 `degraded` 字段；任何降级口径只能来自 runbook 的人工/运营动作，不能伪装成后端自动 fail-open。
