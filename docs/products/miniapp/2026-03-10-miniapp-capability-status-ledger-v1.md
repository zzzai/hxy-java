# MiniApp 全域能力状态台账 v1（2026-03-10）

## 1. 目标与适用范围
- 目标：给小程序用户侧 + 后端 app API 建立一份可执行的能力状态台账，作为后续封版、门禁和文档补齐的单一真值输入。
- 适用范围：当前仓内真实存在的用户侧前端页面 `yudao-mall-uniapp`、对应后端 app 控制器、以及 2026-03-08/03-09 已冻结文档包。
- 本文只使用三种状态：
  - `ACTIVE`：前端页面存在，前后端 API 路径/方法对齐，且已有验收口径文档。
  - `PLANNED_RESERVED`：任一条件缺失，或能力被 `RESERVED_DISABLED` 门禁保护，或当前仅有文档/部分代码而未形成可验收闭环。
  - `DEPRECATED`：历史原型/别名路由已不应继续作为执行真值。

## 2. 状态判定规则

| 判定项 | 通过标准 | 证据来源 |
|---|---|---|
| 前端承接 | `pages.json` 中存在真实路由，且页面文件/页面入口存在 | `yudao-mall-uniapp/pages.json`、`yudao-mall-uniapp/pages/**` |
| 前后端契约对齐 | 前端实际请求 `method + path` 与后端控制器映射一致 | `yudao-mall-uniapp/sheep/api/**`、`ruoyi-vue-pro-master/**/App*Controller.java` |
| 验收口径 | 已存在 PRD / journey / acceptance / rulebook 中至少一份可执行验收口径 | `docs/products/miniapp/**`、`docs/contracts/**`、`docs/plans/**` |

### 2.1 判定补充
- 仅“后端接口存在 + 前端页面存在”但没有验收口径，不得标记为 `ACTIVE`。
- 仅“文档已冻结”但前端或后端未实现，不得标记为 `ACTIVE`。
- `RESERVED_DISABLED` 能力在开关默认 `off` 时，一律记为 `PLANNED_RESERVED`。

## 3. 文档证据缩写
- `ED-01` `docs/products/miniapp/2026-03-09-miniapp-feature-inventory-and-release-matrix-v1.md`
- `ED-02` `docs/products/miniapp/2026-03-09-miniapp-user-journey-service-blueprint-v1.md`
- `ED-03` `docs/products/miniapp/2026-03-09-miniapp-release-acceptance-testbook-v1.md`
- `ED-04` `docs/products/miniapp/2026-03-09-miniapp-business-rulebook-v1.md`
- `ED-05` `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`
- `ED-06` `docs/products/miniapp/2026-03-09-miniapp-member-asset-ledger-prd-v1.md`
- `ED-07` `docs/products/miniapp/2026-03-09-miniapp-search-discovery-prd-v1.md`
- `ED-08` `docs/products/miniapp/2026-03-09-miniapp-home-growth-prd-v1.md`
- `ED-09` `docs/products/miniapp/2026-03-09-miniapp-fulfillment-logistics-prd-v1.md`
- `ED-10` `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`
- `ED-11` `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
- `ED-12` `docs/contracts/2026-03-09-miniapp-reserved-disabled-gate-spec-v1.md`
- `ED-13` `docs/products/miniapp/2026-03-09-miniapp-feature-priority-alignment-v1.md`
- `ED-14` `docs/products/miniapp/2026-03-09-miniapp-gift-card-business-prd-v1.md`
- `ED-15` `docs/products/miniapp/2026-03-09-miniapp-referral-business-prd-v1.md`
- `ED-16` `docs/products/miniapp/2026-03-09-miniapp-technician-feed-product-policy-v1.md`
- `ED-17` `docs/products/miniapp/2026-03-09-miniapp-cs-sop-and-escalation-v1.md`
- `ED-18` `docs/products/miniapp/2026-03-09-miniapp-operation-config-playbook-v1.md`
- `ED-19` `docs/products/miniapp/2026-03-09-miniapp-release-decision-pack-v1.md`
- `ED-20` `docs/products/miniapp/2026-03-09-miniapp-product-doc-consistency-audit-v1.md`
- `ED-21` `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md`
- `ED-22` `docs/products/miniapp/2026-03-10-miniapp-member-page-api-field-dictionary-v1.md`
- `ED-23` `docs/products/miniapp/2026-03-10-miniapp-member-user-facing-errorcopy-v1.md`
- `ED-24` `docs/contracts/2026-03-10-miniapp-member-domain-contract-v1.md`
- `ED-25` `docs/contracts/2026-03-10-miniapp-active-vs-planned-api-matrix-v1.md`
- `ED-26` `docs/plans/2026-03-10-miniapp-member-domain-kpi-and-alerts-v1.md`
- `ED-27` `docs/plans/2026-03-10-miniapp-active-planned-gate-runbook-v1.md`
- `ED-28` `docs/plans/2026-03-10-miniapp-member-domain-sla-routing-v1.md`
- `ED-29` `docs/products/miniapp/2026-03-10-miniapp-doc-completion-master-plan-v1.md`
- `ED-30` `docs/products/miniapp/2026-03-10-miniapp-member-route-truth-and-active-planned-closure-v1.md`
- `ED-31` `docs/products/miniapp/2026-03-10-miniapp-booking-route-api-truth-review-v1.md`

## 4. 关键代码证据与硬缺口

### 4.1 已确认的 booking 前后端不一致
1. 技师列表：前端请求 `GET /booking/technician/list-by-store`，后端真实暴露 `GET /booking/technician/list`。
   - 前端：`yudao-mall-uniapp/sheep/api/trade/booking.js:7`
   - 后端：`ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppTechnicianController.java:22`
2. 时段列表：前端请求 `GET /booking/time-slot/list`，后端真实暴露 `GET /booking/slot/list` / `GET /booking/slot/list-by-technician`。
   - 前端：`yudao-mall-uniapp/sheep/api/trade/booking.js:24`
   - 后端：`ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppTimeSlotController.java`
3. 预约取消：前端发 `PUT /booking/order/cancel`，后端真实暴露 `POST /booking/order/cancel`。
   - 前端：`yudao-mall-uniapp/sheep/api/trade/booking.js:60`
   - 后端：`ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppBookingOrderController.java:91`
4. 加钟/升级：前端发 `POST /booking/addon/create`，后端控制器声明为 `POST /app-api/booking/addon/create`。
   - 前端：`yudao-mall-uniapp/sheep/api/trade/booking.js:69`
   - 后端：`ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppBookingAddonController.java:23`

### 4.2 预留能力当前真实状态
- gift-card：前端无页面、后端无 app 控制器，且门禁 `miniapp.gift-card=off`，状态只能是 `PLANNED_RESERVED`。
- referral：前端无页面、后端无 app 控制器，且门禁 `miniapp.referral=off`，状态只能是 `PLANNED_RESERVED`。
- technician-feed：前端无页面、后端无 feed 控制器，且门禁 `miniapp.technician-feed.audit=off`，状态只能是 `PLANNED_RESERVED`。

### 4.3 已确认的 member 路由真值漂移
1. 03-10 member PRD 将登录页写为 `/pages/public/login`，但当前用户侧真实路由是 `yudao-mall-uniapp/pages/index/login.vue`，且该页主要承担 H5/社交登录回调，日常登录 UI 仍以全局 auth modal 为主。
2. 03-10 member PRD 将个人中心写为 `/pages/user/index`，但真实 tab 路由是 `yudao-mall-uniapp/pages/index/user.vue`。
3. 03-10 member PRD 将签到写为 `/pages/user/sign-in`，但真实路由是 `yudao-mall-uniapp/pages/app/sign.vue`。
4. 03-10 member PRD 将地址写为 `/pages/address/list`，但真实路由是 `yudao-mall-uniapp/pages/user/address/list`。
5. 03-10 member PRD 将积分商城写为 `/pages/point/mall`，但真实路由是 `yudao-mall-uniapp/pages/activity/point/list`。
6. 03-10 member PRD 声明了 `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag`；当前分支均无真实页面文件，因此不能按 `ACTIVE` 处理。

### 4.4 03-10 A 侧真值收口输入
1. `ED-29` 明确了剩余文档缺口、Owner、前置依赖和“先文档后开发”顺序，可作为 content / brokerage / catalog / marketing expansion 的补齐总计划。
2. `ED-30` 将 member 域从“历史 alias route”切回真实 uniapp route，并固定 `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 为当前不可升 `ACTIVE` 的缺页能力。
3. `ED-31` 将 booking 域前端实际 `booking.js`、后端 app controller 和现有 PRD / canonical doc 三方真值并排校对，明确 `create / cancel / addon` 仍是阻断项。
4. 上述三份文档属于 03-10 `Ready` 收口输入，只补证据链，不改变 03-09 Frozen 基线。

## 5. 能力清单

### 5.1 Trade / Pay

| capabilityId | domain | pageRoute | backendApi | status | priority | releaseBatch | owner | evidenceDoc | statusReason / Gate |
|---|---|---|---|---|---|---|---|---|---|
| CAP-TRADE-001 | trade.checkout | `/pages/index/cart`; `/pages/order/confirm` | `GET /trade/order/settlement`; `POST /trade/order/create` | ACTIVE | P0 | RB1-P0 | Trade Domain Owner | `ED-01/ED-02/ED-03/ED-04` | 下单、结算、创建订单均有真实前端承接与验收用例 `AC-03` |
| CAP-PAY-001 | pay.submit-result | `/pages/pay/index`; `/pages/pay/result` | `POST /pay/order/submit`; `GET /trade/order/pay-result`; `GET /pay/order/get` | ACTIVE | P0 | RB1-P0 | Pay + Trade Domain Owner | `ED-01/ED-02/ED-03/ED-10/ED-11` | 支付成功、`PAY_ORDER_NOT_FOUND` 降级待确认均有冻结口径 |
| CAP-TRADE-002 | trade.order-lifecycle | `/pages/order/list`; `/pages/order/detail` | `GET /trade/order/page`; `GET /trade/order/get-count`; `GET /trade/order/get-detail`; `PUT /trade/order/receive`; `DELETE /trade/order/cancel`; `DELETE /trade/order/delete` | ACTIVE | P0 | RB1-P0 | Trade Domain Owner | `ED-01/ED-02/ED-03/ED-04/ED-10` | 列表、详情、计数、收货/取消/删除在前后端与验收文档中闭环 |
| CAP-TRADE-003 | trade.after-sale | `/pages/order/aftersale/apply`; `/pages/order/aftersale/list`; `/pages/order/aftersale/detail`; `/pages/order/aftersale/log`; `/pages/order/aftersale/return-delivery` | `POST /trade/after-sale/create`; `GET /trade/after-sale/page`; `GET /trade/after-sale/get`; `GET /trade/after-sale-log/list`; `PUT /trade/after-sale/delivery`; `DELETE /trade/after-sale/cancel`; `GET /trade/after-sale/refund-progress` | ACTIVE | P0 | RB1-P0 | Trade Domain Owner | `ED-01/ED-02/ED-03/ED-04/ED-10/ED-11` | 售后申请、详情、日志、回寄、退款进度均有冻结错误码与降级语义 |
| CAP-TRADE-004 | trade.logistics | `/pages/order/express/log` | `GET /trade/order/get-express-track-list` | ACTIVE | P1 | RB2-P1 | Trade Domain Owner | `ED-02/ED-09/ED-10` | 物流轨迹页与履约口径 PRD 已存在，可单独验收 |

### 5.2 Member

| capabilityId | domain | pageRoute | backendApi | status | priority | releaseBatch | owner | evidenceDoc | statusReason / Gate |
|---|---|---|---|---|---|---|---|---|---|
| CAP-MEMBER-001 | member.auth-social | `component:s-auth-modal`; `/pages/index/login`（H5/社交回调） | `/member/auth/*`; `/member/social-user/*` | ACTIVE | P1 | RB2-P1 | Member Domain Owner | `ED-21/ED-22/ED-23/ED-24/ED-25/ED-26/ED-27/ED-28/ED-30` | 会员登录/短信/微信一键登录已有前端组件、后端接口与错误码/SLA文档；真实 route 以 `ED-30` 为准，但 03-10 文档批次当前仍只到 `Ready` |
| CAP-MEMBER-002 | member.profile-security | `/pages/index/user`; `/pages/user/info`; `/pages/public/setting` | `GET /member/user/get`; `PUT /member/user/update`; `PUT /member/user/update-mobile*`; `PUT /member/user/update-password`; `PUT /member/user/reset-password`; `POST /member/auth/logout` | ACTIVE | P1 | RB2-P1 | Member Domain Owner | `ED-21/ED-22/ED-23/ED-24/ED-25/ED-26/ED-27/ED-28/ED-30` | 个人资料、手机号、密码修改链路前后端存在，个人中心主路由已由 `ED-30` 校正到 `/pages/index/user`；03-10 文档批次仍待后续 Frozen 评审 |
| CAP-MEMBER-003 | member.address | `/pages/user/address/list`; `/pages/user/address/edit` | `GET /member/address/list`; `GET /member/address/get`; `GET /member/address/get-default`; `POST /member/address/create`; `PUT /member/address/update`; `DELETE /member/address/delete` | ACTIVE | P0 | RB1-P0 | Member Domain Owner | `ED-01/ED-02/ED-04/ED-06/ED-10` | 真实路由已存在，地址 CRUD 与默认地址规则已冻结 |
| CAP-MEMBER-004 | member.wallet-ledger | `/pages/user/wallet/money`; `/pages/pay/recharge`; `/pages/pay/recharge-log` | `GET /pay/wallet/get`; `GET /pay/wallet-transaction/page`; `GET /pay/wallet-transaction/get-summary`; `GET /pay/wallet-recharge-package/list`; `POST /pay/wallet-recharge/create`; `GET /pay/wallet-recharge/page` | ACTIVE | P1 | RB2-P1 | Member + Pay Domain Owner | `ED-02/ED-06/ED-10` | 钱包余额、流水、充值套餐前后端已闭环，资产账本 PRD 可执行 |
| CAP-MEMBER-005 | member.point-ledger | `/pages/user/wallet/score` | `GET /member/point/record/page` | ACTIVE | P0 | RB1-P0 | Member Domain Owner | `ED-02/ED-06/ED-10/ED-11` | 积分流水已纳入资产账本 PRD 与 canonical API |
| CAP-MEMBER-006 | member.sign-in | `/pages/app/sign` | `GET /member/sign-in/config/list`; `GET /member/sign-in/record/get-summary`; `POST /member/sign-in/record/create`; `GET /member/sign-in/record/page` | ACTIVE | P1 | RB2-P1 | Member Growth Domain Owner | `ED-21/ED-22/ED-23/ED-24/ED-25/ED-26/ED-27/ED-28/ED-30` | 签到页和接口存在，03-10 member 包已补齐错误码、告警和人工接管口径；真实 route 以 `ED-30` 固定为 `/pages/app/sign` |
| CAP-MEMBER-007 | member.level-progress | `N/A（当前无真实 pageRoute）` | `GET /member/level/list`; `GET /member/experience-record/page` | PLANNED_RESERVED | P1 | RB2-P1 | Member Growth Domain Owner | `ED-21/ED-24/ED-25/ED-30` | 后端接口存在，但 `ED-30` 已确认当前分支缺 `pages/user/level.vue`，不能按 `ACTIVE` 处理 |
| CAP-MEMBER-008 | member.asset-overview | `/pages/user/wallet/money`; `/pages/coupon/list`; `N/A（/pages/profile/assets 缺失）` | `GET /pay/wallet/get`; `GET /promotion/coupon/get-unused-count`; `GET /member/point/record/page`; `GET /member/asset-ledger/page`（规划） | PLANNED_RESERVED | P1 | RB2-P1 | Member + Promotion Domain Owner | `ED-06/ED-21/ED-24/ED-25/ED-27/ED-30` | 分资产页已运行，但 `ED-30` 已确认聚合资产总览页缺真实 route，且 `/member/asset-ledger/page` 仍是规划态，不得误标 Active |

### 5.3 Product / Search

| capabilityId | domain | pageRoute | backendApi | status | priority | releaseBatch | owner | evidenceDoc | statusReason / Gate |
|---|---|---|---|---|---|---|---|---|---|
| CAP-PRODUCT-001 | product.home-diy | `/pages/index/index`; `/pages/index/page` | `GET /promotion/diy-template/used`; `GET /promotion/diy-template/get`; `GET /promotion/diy-page/get` | ACTIVE | P1 | RB2-P1 | Product + Promotion Domain Owner | `ED-08/ED-10/ED-19` | 首页基础装配链路已有前端承接、PRD 和发布决策约束 |
| CAP-PRODUCT-002 | product.catalog-browse | `/pages/index/category`; `/pages/goods/list`; `/pages/goods/index` | `GET /product/category/list`; `GET /product/spu/page`; `GET /product/spu/get-detail` | ACTIVE | P1 | RB2-P1 | Product Domain Owner | `ED-02/ED-03/ED-07/ED-10` | 当前真实搜索/浏览是 `search-lite + spu.page`，非 canonical search |
| CAP-PRODUCT-003 | product.detail-comment-collect-history | `/pages/goods/comment/add`; `/pages/goods/comment/list`; `/pages/user/goods-collect`; `/pages/user/goods-log` | `GET /product/comment/page`; `POST /trade/order/item/create-comment`; `/product/favorite/*`; `/product/browse-history/*` | PLANNED_RESERVED | P1 | BACKLOG-DOC-GAP | Product Domain Owner | `N/A` | 代码能力存在，但缺独立产品验收文档，不能记 `ACTIVE` |
| CAP-PRODUCT-004 | product.search-lite | `/pages/index/search` -> `/pages/goods/list` | `GET /product/spu/page` | ACTIVE | P1 | RB2-P1 | Product Domain Owner | `ED-02/ED-03/ED-07/ED-10` | 当前已上线能力是搜索词透传 + `spu.page` 召回，不是 `product/search/page` |
| CAP-PRODUCT-005 | product.search-canonical | `/pages/search/index`（规划态） | `GET /product/search/page` | PLANNED_RESERVED | P2 | RB3-P2 | Search Owner | `ED-07/ED-10/ED-11/ED-19` | 仅文档冻结，受 `miniapp.search.validation=off` 保护 |

### 5.4 Promotion / Growth

| capabilityId | domain | pageRoute | backendApi | status | priority | releaseBatch | owner | evidenceDoc | statusReason / Gate |
|---|---|---|---|---|---|---|---|---|---|
| CAP-PROMO-001 | promotion.coupon | `/pages/coupon/list`; `/pages/coupon/detail` | `GET /promotion/coupon-template/page`; `GET /promotion/coupon/page`; `POST /promotion/coupon/take`; `GET /promotion/coupon/get` | ACTIVE | P0 | RB1-P0 | Promotion Domain Owner | `ED-01/ED-02/ED-03/ED-04/ED-10/ED-11` | 领券成功、重复领取、活动失效均有冻结验收口径 |
| CAP-PROMO-002 | promotion.point-mall | `/pages/activity/point/list`; `/pages/goods/point`; `/pages/user/wallet/score` | `GET /promotion/point-activity/page`; `GET /promotion/point-activity/get-detail`; `GET /promotion/point-activity/list-by-ids`; `GET /member/point/record/page` | ACTIVE | P0 | RB1-P0 | Promotion + Member Domain Owner | `ED-01/ED-03/ED-06/ED-10/ED-11` | 积分商品、活动详情、积分流水三端已成闭环 |
| CAP-PROMO-003 | promotion.activity-growth | `/pages/activity/index`; `/pages/activity/groupon/*`; `/pages/activity/seckill/list` | `/promotion/activity/*`; `/promotion/combination/*`; `/promotion/seckill/*`; `/promotion/reward-activity/*` | PLANNED_RESERVED | P1 | BACKLOG-DOC-GAP | Promotion Domain Owner | `N/A` | 前后端运行能力存在，但缺完整发布 PRD/contract/error/degrade 文档包 |
| CAP-PROMO-004 | promotion.home-context-guard | `/pages/index/index` | `N/A（门禁能力）` | PLANNED_RESERVED | P1 | RB2-P1 | Product on-call | `ED-08/ED-11/ED-12/ED-19` | `miniapp.home.context-check=off`，仅有门禁文档，不计入 `ACTIVE` |

### 5.5 Booking / Technician Service

| capabilityId | domain | pageRoute | backendApi | status | priority | releaseBatch | owner | evidenceDoc | statusReason / Gate |
|---|---|---|---|---|---|---|---|---|---|
| CAP-BOOKING-001 | booking.query | `/pages/booking/order-list`; `/pages/booking/order-detail` | `GET /booking/order/list`; `GET /booking/order/get` | ACTIVE | P0 | RB1-P0 | Booking Domain Owner | `ED-01/ED-02/ED-03/ED-05/ED-10/ED-31` | 查询与详情路径前后端一致，且 `ED-31` 已确认查询链路不在当前 booking 阻断项内 |
| CAP-BOOKING-002 | booking.create | `/pages/booking/technician-list`; `/pages/booking/technician-detail`; `/pages/booking/order-confirm` | 目标口径应为 `GET /booking/technician/list`; `GET /booking/slot/list-by-technician`; `POST /booking/order/create` | PLANNED_RESERVED | P1 | RB2-P1 | Booking Domain Owner | `ED-05/ED-10/ED-11/ED-31` | `ED-31` 已确认前端当前请求的 `list-by-store` 与 `time-slot/list` 不等于后端真实路径；待 C 窗口补 canonical contract 前不能记 `ACTIVE` |
| CAP-BOOKING-003 | booking.cancel | `/pages/booking/order-list` | 目标口径应为 `POST /booking/order/cancel` | PLANNED_RESERVED | P1 | RB2-P1 | Booking Domain Owner | `ED-05/ED-10/ED-17/ED-18/ED-31` | `ED-31` 已确认前端发 `PUT`、后端收 `POST`；方法不一致，当前只允许停留在 `Ready`/`Planned` 边界 |
| CAP-BOOKING-004 | booking.addon-upgrade | `/pages/booking/addon` | 目标口径应为 `POST /app-api/booking/addon/create` | PLANNED_RESERVED | P1 | RB2-P1 | Booking Domain Owner | `ED-05/ED-10/ED-11/ED-18/ED-31` | `ED-31` 已确认前端 `/booking/addon/create` 与后端 `/app-api/booking/addon/create` 不一致，且 `miniapp.addon.intent-idempotency=off` |

### 5.6 Content / Service / Brokerage

| capabilityId | domain | pageRoute | backendApi | status | priority | releaseBatch | owner | evidenceDoc | statusReason / Gate |
|---|---|---|---|---|---|---|---|---|---|
| CAP-CONTENT-001 | content.kefu-article-faq | `/pages/chat/index`; `/pages/public/richtext`; `/pages/public/faq`; `/pages/public/webview` | `/promotion/kefu-message/*`; `/promotion/article/get` | PLANNED_RESERVED | P1 | BACKLOG-DOC-GAP | Content Ops Owner | `ED-17/ED-29` | 客服与文章能力运行存在；`ED-29` 已登记待补 contract/error/degrade/runbook，但当前仍未形成发布级闭环 |
| CAP-BROKERAGE-001 | brokerage.center | `/pages/commission/*` | `/trade/brokerage-user/*`; `/trade/brokerage-record/*`; `/trade/brokerage-withdraw/*` | PLANNED_RESERVED | P2 | BACKLOG-DOC-GAP | Brokerage Domain Owner | `ED-29` | 分销页和后端控制器存在；`ED-29` 已固定补文档路径与 owner，但当前没有冻结产品文档包 |

### 5.7 Reserved Expansion Domains

| capabilityId | domain | pageRoute | backendApi | status | priority | releaseBatch | owner | evidenceDoc | statusReason / Gate |
|---|---|---|---|---|---|---|---|---|---|
| CAP-RESERVED-001 | gift-card | `/pages/gift-card/*`（未实现） | `/promotion/gift-card/*`（未实现） | PLANNED_RESERVED | P2 | RB3-P2 | Trade Domain Owner | `ED-13/ED-14/ED-12/ED-19` | 文档冻结但前后端未落地，且 `miniapp.gift-card=off` |
| CAP-RESERVED-002 | referral | `/pages/referral/*`（未实现） | `/promotion/referral/*`（未实现） | PLANNED_RESERVED | P2 | RB3-P2 | Promotion Domain Owner | `ED-13/ED-15/ED-12/ED-19` | 文档冻结但前后端未落地，且 `miniapp.referral=off` |
| CAP-RESERVED-003 | technician-feed | `/pages/technician/feed`（未实现） | `/booking/technician/feed/*`（未实现） | PLANNED_RESERVED | P2 | RB3-P2 | Booking + Content Ops Owner | `ED-13/ED-16/ED-12/ED-19` | 文档冻结但前后端未落地，且 `miniapp.technician-feed.audit=off` |

## 6. Deprecated 路由别名（不再作为执行真值）

| capabilityId | domain | pageRoute | backendApi | status | priority | releaseBatch | owner | evidenceDoc | statusReason / Gate |
|---|---|---|---|---|---|---|---|---|---|
| CAP-ALIAS-001 | route-alias.after-sale | `/pages/after-sale/create`; `/pages/after-sale/list`; `/pages/after-sale/detail` | 同交易售后接口 | DEPRECATED | P0 | N/A-DEPRECATED | MiniApp FE Owner | `ED-01/ED-10/ED-20` | 原型/冻结文档中的历史别名；真实 uniapp 路由是 `/pages/order/aftersale/*` |
| CAP-ALIAS-002 | route-alias.refund-progress | `/pages/refund/progress` | `GET /trade/after-sale/refund-progress` | DEPRECATED | P0 | N/A-DEPRECATED | MiniApp FE Owner | `ED-01/ED-10/ED-20` | 当前用户侧真实页面收口在售后详情/日志链路，历史别名不再作为单一真值 |
| CAP-ALIAS-003 | route-alias.member-promo | `/pages/address/list`; `/pages/coupon/center`; `/pages/point/mall` | 对应 member/promotion 接口 | DEPRECATED | P0 | N/A-DEPRECATED | MiniApp FE Owner | `ED-01/ED-10/ED-20` | 真实 uniapp 路由分别是 `/pages/user/address/list`、`/pages/coupon/list`、`/pages/activity/point/list` |
| CAP-ALIAS-004 | route-alias.booking-list | `/pages/booking/list` | `GET /booking/order/list`; `GET /booking/order/list-by-status` | DEPRECATED | P0 | N/A-DEPRECATED | MiniApp FE Owner | `ED-01/ED-10/ED-20` | 真实 uniapp 路由为 `/pages/booking/order-list` |

## 7. 当前结论
1. 可直接视作 `ACTIVE` 的主链路集中在：交易支付、售后退款、地址、钱包/积分、优惠券、积分商城、基础首页 DIY、搜索 lite、预约查询。
2. 会员域已从“文档缺口”推进到“文档已补齐但 route truth 待归一”，当前可判定为 `ACTIVE` 的只包括登录组件链路、资料、安全、签到、地址、钱包/积分；等级页与资产总览仍不能升 Active。
3. 当前最关键的假 Active 风险仍在 booking 域：页面存在，但 `create/cancel/addon` 未达到“前后端方法路径对齐 + 验收口径”标准。
4. gift-card / referral / technician-feed 仍是 `P2/RB3-P2` 规划态，不得被误算进发布已上线能力。
5. 后续封版时，应以本文替代历史原型别名路由，避免继续使用 `/pages/after-sale/*`、`/pages/coupon/center`、`/pages/public/login`、`/pages/user/index` 等旧路径描述。
6. `ED-29/ED-30/ED-31` 只是 03-10 A 侧真值收口输入，不构成自动 Frozen 放行依据。
