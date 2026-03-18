# MiniApp 全域能力状态台账 v1（2026-03-10）

## 1. 目标与适用范围
- 目标：给小程序用户侧 + 后端 app API 建立一份可执行的能力状态台账，作为后续封版、门禁和文档补齐的单一真值输入。
- 适用范围：当前仓内真实存在的用户侧前端页面 `yudao-mall-uniapp`、对应后端 app 控制器、以及 2026-03-08/03-09/03-10/03-11/03-12 已落盘文档包。
- 本文只使用三种状态：
  - `ACTIVE`：前端页面或真实入口存在，前后端 API 路径/方法对齐，且已有验收/运行口径文档。
  - `PLANNED_RESERVED`：任一条件缺失，或能力仍受 `RESERVED_DISABLED` / scope gate 保护，或 contract 明确保留为 `Ready / ACTIVE_BE_ONLY / BLOCKED` 边界。
  - `DEPRECATED`：历史原型/别名路由已不应继续作为执行真值。

## 2. 状态判定规则

| 判定项 | 通过标准 | 证据来源 |
|---|---|---|
| 前端承接 | `pages.json` 中存在真实路由，或已有真实组件/初始化入口 | `yudao-mall-uniapp/pages.json`、`yudao-mall-uniapp/pages/**`、`yudao-mall-uniapp/sheep/store/**` |
| 前后端契约对齐 | 前端实际请求 `method + path` 与后端控制器映射一致 | `yudao-mall-uniapp/sheep/api/**`、`ruoyi-vue-pro-master/**/App*Controller.java` |
| 验收口径 | 已存在 PRD / contract / acceptance / SOP / runbook 至少一份可执行口径 | `docs/products/miniapp/**`、`docs/contracts/**`、`docs/plans/**` |

### 2.1 判定补充
- 仅“后端接口存在 + 前端页面存在”但 contract 或验收口径明确保留在 `Ready / ACTIVE_BE_ONLY / BLOCKED` 的，不得强行升为 `ACTIVE`。
- `RESERVED_DISABLED` 能力在开关默认 `off` 时，一律记为 `PLANNED_RESERVED`。
- 03-10 之后的增量文档已持续落盘，但落盘不等于 capability 自动进入 `Frozen Candidate` 或发布 allowlist。

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
- `ED-32` `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-sop-v1.md`
- `ED-33` `docs/plans/2026-03-10-miniapp-brokerage-domain-runbook-v1.md`
- `ED-34` `docs/plans/2026-03-10-miniapp-product-catalog-kpi-and-alerting-v1.md`
- `ED-35` `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-ops-playbook-v1.md`
- `ED-36` `docs/plans/2026-03-10-miniapp-reserved-expansion-activation-checklist-v1.md`
- `ED-37` `docs/plans/2026-03-10-miniapp-reserved-expansion-gray-acceptance-runbook-v1.md`
- `ED-38` `docs/products/miniapp/2026-03-10-miniapp-doc-completion-final-review-v1.md`
- `ED-39` `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md`
- `ED-40` `docs/products/miniapp/2026-03-10-miniapp-brokerage-distribution-prd-v1.md`
- `ED-41` `docs/products/miniapp/2026-03-10-miniapp-product-catalog-interaction-prd-v1.md`
- `ED-42` `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-prd-v1.md`
- `ED-43` `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md`
- `ED-44` `docs/contracts/2026-03-10-miniapp-content-customer-service-contract-v1.md`
- `ED-45` `docs/contracts/2026-03-10-miniapp-brokerage-domain-contract-v1.md`
- `ED-46` `docs/contracts/2026-03-10-miniapp-product-catalog-contract-v1.md`
- `ED-47` `docs/contracts/2026-03-10-miniapp-marketing-expansion-contract-v1.md`
- `ED-48` `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`
- `ED-49` `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md`
- `ED-50` `docs/products/miniapp/2026-03-11-miniapp-brokerage-customer-service-sop-v1.md`
- `ED-51` `docs/products/miniapp/2026-03-11-miniapp-product-catalog-customer-recovery-sop-v1.md`
- `ED-52` `docs/plans/2026-03-11-miniapp-booking-runtime-closure-checklist-v1.md`
- `ED-53` `docs/plans/2026-03-11-miniapp-member-missing-page-activation-checklist-v1.md`
- `ED-54` `docs/plans/2026-03-11-miniapp-reserved-runtime-readiness-register-v1.md`
- `ED-55` `docs/products/miniapp/2026-03-12-miniapp-technician-feed-prd-v1.md`
- `ED-56` `docs/products/miniapp/2026-03-14-miniapp-runtime-blocker-final-integration-v1.md`
- `ED-57` `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-release-evidence-review-v1.md`
- `ED-58` `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-acceptance-and-recovery-prd-v1.md`
- `ED-59` `docs/contracts/2026-03-15-miniapp-booking-runtime-release-evidence-contract-v1.md`
- `ED-60` `docs/plans/2026-03-15-miniapp-booking-runtime-release-runbook-v1.md`
- `ED-61` `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-page-field-dictionary-v1.md`
- `ED-62` `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-user-structure-and-recovery-prd-v1.md`
- `ED-63` `docs/contracts/2026-03-15-miniapp-booking-runtime-canonical-api-and-errorcode-matrix-v1.md`
- `ED-64` `docs/plans/2026-03-16-miniapp-booking-runtime-release-gate-audit-v1.md`
- `ED-65` `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-gate-acceptance-sop-v1.md`
- `ED-66` `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-final-integration-review-v1.md`
- `ED-67` `docs/products/miniapp/2026-03-15-miniapp-finance-ops-technician-commission-admin-page-api-binding-truth-review-v1.md`
- `ED-68` `docs/plans/2026-03-15-miniapp-finance-ops-technician-commission-admin-evidence-ledger-v1.md`
- `ED-69` `docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- `ED-70` `docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md`
- `ED-71` `docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md`
- `ED-72` `docs/contracts/2026-03-17-miniapp-booking-review-errorcode-and-failure-mode-v1.md`
- `ED-73` `docs/plans/2026-03-17-miniapp-booking-review-service-recovery-runbook-v1.md`
- `ED-74` `docs/plans/2026-03-17-miniapp-booking-review-release-gate-v1.md`
- `ED-75` `docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`
- `ED-76` `docs/products/miniapp/2026-03-18-miniapp-booking-review-detail-acceptance-checklist-v1.md`

## 4. 关键代码证据与硬缺口

### 4.1 已确认的 booking runtime 当前真值
1. 前端 `yudao-mall-uniapp/sheep/api/trade/booking.js` 当前已固定为 canonical：
   - `GET /booking/technician/list`
   - `GET /booking/technician/get`
   - `GET /booking/slot/list-by-technician`
   - `GET /booking/order/list`
   - `GET /booking/order/get`
   - `POST /booking/order/create`
   - `POST /booking/order/cancel`
   - `POST /app-api/booking/addon/create`
2. `yudao-mall-uniapp/pages/booking/logic.js` 与 `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs` 已冻结写链失败分支：
   - create 失败不跳详情
   - cancel 失败不刷新
   - addon 失败不跳详情
3. 03-16 已正式冻结的 booking 页面 / contract 真值包括：
   - 技师页 backend 稳定字段只到 `id,name,avatar,introduction,tags,rating,serviceCount`
   - `title/specialties/status` 当前只是页面 fallback
   - `order-list` 当前按 `data.list/data.total` 读取，但 controller 返回 `data[]`
   - `payOrderId` 当前没有已提交响应绑定证据
   - `addon` 页当前只提交 `parentOrderId,addonType`
4. booking runtime page 当前可稳定引用的 errorCode 只认：
   - create：`1030003001`
   - cancel：`1030004000/1030004005/1030004006`
   - addon：`1030003001/1030004000/1030004001/1030004006`
5. `ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh` 当前成功输出固定为：
   - `doc_closed=YES`
   - `can_develop=YES`
   - `can_release=NO`
6. 因此 booking 当前已经从“旧 path 漂移阻断”切到“query-only 可维护、write-chain 不可放量、字段/绑定/发布证据仍未闭环”的边界。

### 4.2 member 缺页能力与 route truth
1. 当前真实用户页存在：`/pages/index/login`、`/pages/index/user`、`/pages/app/sign`、`/pages/user/address/list`、`/pages/user/wallet/money`、`/pages/user/wallet/score`。
2. 当前缺页能力仍包括：
   - `/pages/user/level`
   - `/pages/profile/assets`
   - `/pages/user/tag`
3. `/member/asset-ledger/page` 继续受 `miniapp.asset.ledger` 保护，未落 controller + FE 承接前不得计入 `ACTIVE`。

### 4.3 03-10 新增域真值补充
1. FAQ 只允许按“壳页”理解：`/pages/public/faq` 真实承接是跳转 `/pages/public/richtext?title=常见问题`。
2. `search-lite` 真值固定为 `/pages/index/search -> /pages/goods/list -> GET /product/spu/page`；`GET /product/search/page` 继续是 canonical 规划态。
3. 收藏状态真实路径是 `GET /product/favorite/exits`，不得在文档或联调口径中擅自更正为 `/exists`。
4. 分销资金字段真值固定为：`withdrawPrice`、`brokeragePrice`、`frozenPrice`；团队页后端字段是 `brokerageOrderCount`。
5. 本批 contract 没有新增服务端 `degraded/degradeReason` 字段；所有 fail-open/fail-close 继续按显式 errorCode、空列表、`null` 和 runbook 人工动作收口。

### 4.4 03-14 最终阻断集成结论
1. 当前“缺文档”问题已经清零，但 capability 台账仍必须区分“文档已闭环”和“工程未闭环”。
2. 当前 booking 的最终 release blocker 已从“旧 path 漂移”转为“shared gate 已接入但 booking 输出仍固定 `can_release=NO`，且页面字段/绑定/发布证据仍未闭环”；create / cancel / addon 仍缺发布级运行证据。
3. 当前唯一明确的 finance-ops capability blocker 仍是 `BO-004`：
   - 只核到 `/booking/commission/*` controller truth
   - 未核到独立后台页面文件
   - 未核到独立前端 API 文件
   - 写接口存在 `true` 但 no-op / 伪成功 风险
4. 03-15 的 `BO-004` page/API binding truth review 与 evidence ledger 已正式吸收，但它们只会强化“页面真值待核”的结论，不会把 `BO-004` 改写成后台页面闭环。
5. member / reserved 当前不属于“缺文档”，但属于“禁止误升为 runtime 已上线”的守门项：
   - `/pages/user/level`
   - `/pages/profile/assets`
   - `/pages/user/tag`
   - `gift-card / referral / technician-feed`

### 4.5 03-17 booking review 子域真值补充
1. booking review 当前是 booking 域内的独立服务质量反馈子域，不得写成商品评论 alias。
2. 当前真实用户 route 只认：
   - `/pages/booking/review-add`
   - `/pages/booking/review-detail`
   - `/pages/booking/review-result`
   - `/pages/booking/review-list`
3. 当前稳定 review errorCode 只认：
   - `1030008000` `BOOKING_REVIEW_NOT_EXISTS`
   - `1030008001` `BOOKING_REVIEW_ALREADY_EXISTS`
   - `1030008002` `BOOKING_REVIEW_NOT_ELIGIBLE`
   - 创建评价链路还可能命中 `1030004000 / 1030004006`
4. `GET /booking/review/eligibility` 当前是 `code=0 + eligible/reason` 结构态，不是稳定业务错误码分支。
5. `serviceOrderId` 当前改为后端按 `payOrderId -> TradeServiceOrderApi.listTraceByPayOrderId` best-effort 回填，trace 未命中或异常时仍可为空；`picUrls` 已在 miniapp 提交页接通上传并发送，但历史 / 详情回显证据仍未单独闭环。
6. 当前没有 booking review 独立 feature flag、rollout proof、runtime sample pack，也没有服务端 `degraded=true / degradeReason` 证据。
7. 后台 `BO-005` 当前已新增：
   - `POST /booking/review/manager-todo/claim`
   - `POST /booking/review/manager-todo/first-action`
   - `POST /booking/review/manager-todo/close`
   但这只代表 admin-only 店长待办成立，不代表自动通知链路成立。
8. 因此当前只允许：
   - 把 review history / summary 按 query-side `ACTIVE` 管理
   - 把 review submit / recovery 按 `Can Develop / Cannot Release` 管理
9. 03-18 已新增 `review-detail` 页面级 acceptance checklist，但它只会补强 query-side 页面验收，不会把 review submit / admin recovery 升为 release-ready。

## 5. 能力清单

### 5.1 Trade / Pay

| capabilityId | domain | pageRoute | backendApi | status | priority | releaseBatch | owner | evidenceDoc | statusReason / Gate |
|---|---|---|---|---|---|---|---|---|---|
| CAP-TRADE-001 | trade.checkout | `/pages/index/cart`; `/pages/order/confirm` | `GET /trade/order/settlement`; `POST /trade/order/create` | ACTIVE | P0 | RB1-P0 | Trade Domain Owner | `ED-01/ED-02/ED-03/ED-04/ED-10` | 下单、结算、创建订单均有真实前端承接与冻结验收口径 |
| CAP-PAY-001 | pay.submit-result | `/pages/pay/index`; `/pages/pay/result` | `POST /pay/order/submit`; `GET /trade/order/pay-result`; `GET /pay/order/get` | ACTIVE | P0 | RB1-P0 | Pay + Trade Domain Owner | `ED-01/ED-02/ED-03/ED-10/ED-11` | 支付结果、轮询、错误码和恢复动作都已冻结 |
| CAP-TRADE-002 | trade.order-lifecycle | `/pages/order/list`; `/pages/order/detail` | `GET /trade/order/page`; `GET /trade/order/get-count`; `GET /trade/order/get-detail`; `PUT /trade/order/receive`; `DELETE /trade/order/cancel`; `DELETE /trade/order/delete` | ACTIVE | P0 | RB1-P0 | Trade Domain Owner | `ED-01/ED-02/ED-03/ED-04/ED-10` | 列表、详情、收货/取消/删除链路在代码与文档中闭环 |
| CAP-TRADE-003 | trade.after-sale | `/pages/order/aftersale/apply`; `/pages/order/aftersale/list`; `/pages/order/aftersale/detail`; `/pages/order/aftersale/log`; `/pages/order/aftersale/return-delivery` | `POST /trade/after-sale/create`; `GET /trade/after-sale/page`; `GET /trade/after-sale/get`; `GET /trade/after-sale-log/list`; `PUT /trade/after-sale/delivery`; `DELETE /trade/after-sale/cancel`; `GET /trade/after-sale/refund-progress` | ACTIVE | P0 | RB1-P0 | Trade Domain Owner | `ED-01/ED-02/ED-03/ED-04/ED-10/ED-11` | 售后、退款进度、回寄、日志均属于既有 Frozen 基线 |
| CAP-TRADE-004 | trade.logistics | `/pages/order/express/log` | `GET /trade/order/get-express-track-list` | ACTIVE | P1 | RB2-P1 | Trade Domain Owner | `ED-02/ED-09/ED-10` | 物流轨迹页与履约口径已存在，可独立验收 |

### 5.2 Member

| capabilityId | domain | pageRoute | backendApi | status | priority | releaseBatch | owner | evidenceDoc | statusReason / Gate |
|---|---|---|---|---|---|---|---|---|---|
| CAP-MEMBER-001 | member.auth-social | `component:s-auth-modal`; `/pages/index/login` | `/member/auth/*`; `/member/social-user/*` | ACTIVE | P1 | RB2-P1 | Member Domain Owner | `ED-21/ED-22/ED-23/ED-24/ED-25/ED-26/ED-27/ED-28/ED-30` | 登录、短信登录、微信手机号登录已有真实组件/页面、contract、错误码和 SLA 口径 |
| CAP-MEMBER-002 | member.profile-security | `/pages/index/user`; `/pages/user/info`; `/pages/public/setting` | `GET /member/user/get`; `PUT /member/user/update`; `PUT /member/user/update-mobile*`; `PUT /member/user/update-password`; `PUT /member/user/reset-password`; `POST /member/auth/logout` | ACTIVE | P1 | RB2-P1 | Member Domain Owner | `ED-21/ED-22/ED-24/ED-25/ED-30` | 个人中心、资料、安全设置已按真实 route 收口 |
| CAP-MEMBER-003 | member.address | `/pages/user/address/list`; `/pages/user/address/edit` | `GET /member/address/list`; `GET /member/address/get`; `GET /member/address/get-default`; `POST /member/address/create`; `PUT /member/address/update`; `DELETE /member/address/delete` | ACTIVE | P0 | RB1-P0 | Member Domain Owner | `ED-01/ED-02/ED-04/ED-10/ED-21/ED-24` | 地址 CRUD 与默认地址规则已冻结；空地址返回 `[]/null` 有明确语义 |
| CAP-MEMBER-004 | member.wallet-point | `/pages/user/wallet/money`; `/pages/user/wallet/score`; `/pages/coupon/list`; `/pages/activity/point/list` | `GET /pay/wallet/get`; `GET /pay/wallet-transaction/page`; `GET /member/point/record/page`; `GET /promotion/coupon/page`; `GET /promotion/point-activity/page` | ACTIVE | P0 | RB1-P0 | Member + Promotion Domain Owner | `ED-03/ED-06/ED-10/ED-11/ED-21/ED-24` | 钱包、积分、券、积分商城已属于真实用户能力 |
| CAP-MEMBER-005 | member.sign-in | `/pages/app/sign` | `GET /member/sign-in/config/list`; `GET /member/sign-in/record/get-summary`; `POST /member/sign-in/record/create`; `GET /member/sign-in/record/page` | ACTIVE | P1 | RB2-P1 | Member Growth Domain Owner | `ED-21/ED-22/ED-23/ED-24/ED-26/ED-27/ED-28/ED-30` | 签到 route 与 contract 已校正；重复签到、积分上限等错误码已落盘 |
| CAP-MEMBER-006 | member.level-progress | `N/A（当前无真实 pageRoute）` | `GET /member/level/list`; `GET /member/experience-record/page` | PLANNED_RESERVED | P1 | RB2-P1 | Member Domain Owner | `ED-21/ED-22/ED-24/ED-30/ED-38/ED-53` | `/pages/user/level` 缺页；03-11 已补激活前置清单，等级链路不得按已上线页面表述 |
| CAP-MEMBER-007 | member.asset-hub | `N/A（当前无真实 pageRoute）` | `GET /member/asset-ledger/page` | PLANNED_RESERVED | P1 | RB2-P1 | Member Domain Owner | `ED-06/ED-21/ED-24/ED-25/ED-27/ED-30/ED-38/ED-53` | `/pages/profile/assets` 缺页，且 `miniapp.asset.ledger` 仍受保护；03-11 checklist 已固定退出条件 |
| CAP-MEMBER-008 | member.user-tag | `N/A（当前无真实 pageRoute）` | `N/A（当前无 app 端读取接口）` | PLANNED_RESERVED | P1 | RB2-P1 | Member Domain Owner | `ED-21/ED-25/ED-30/ED-38/ED-53` | `/pages/user/tag` 缺页，标签模块默认隐藏，不发未知 app API；03-11 checklist 已固定激活门槛 |

### 5.3 Product / Search / Catalog

| capabilityId | domain | pageRoute | backendApi | status | priority | releaseBatch | owner | evidenceDoc | statusReason / Gate |
|---|---|---|---|---|---|---|---|---|---|
| CAP-PRODUCT-001 | product.catalog-browse | `/pages/index/category`; `/pages/goods/list`; `/pages/goods/index` | `GET /product/category/list`; `GET /product/spu/page`; `GET /product/spu/get-detail`; `GET /trade/order/settlement-product` | ACTIVE | P0 | RB1-P0 | Product Domain Owner | `ED-07/ED-10/ED-11/ED-41/ED-46/ED-48/ED-49/ED-51` | 商品分类、商品列表、商品详情与 `search-lite` 主链路是真实 `ACTIVE`；03-11 已补用户恢复 SOP，明确 `search-lite` 只能走 `/product/spu/page` |
| CAP-PRODUCT-002 | product.detail-comment-collect-history | `/pages/user/goods-collect`; `/pages/user/goods-log`; `/pages/goods/comment/list`; `/pages/goods/comment/add` | `GET /product/comment/page`; `GET /product/favorite/page`; `GET /product/favorite/exits`; `POST /product/favorite/create`; `DELETE /product/favorite/delete`; `GET /product/browse-history/page`; `DELETE /product/browse-history/delete`; `DELETE /product/browse-history/clean`; `POST /trade/order/item/create-comment` | PLANNED_RESERVED | P1 | RB2-P1 | Product Domain Owner | `ED-11/ED-41/ED-46/ED-48/ED-49/ED-51` | contract 明确评论/收藏/足迹整体仍停留在 Ready / `PLANNED_RESERVED`，03-11 恢复 SOP 已补齐“删除失败保持旧状态”“评论必须全部成功后才允许离页”口径，但不能因页面已可访问就升 `ACTIVE` |
| CAP-PRODUCT-003 | product.search-canonical | `N/A（当前无真实用户页）` | `GET /product/search/page` | PLANNED_RESERVED | P2 | RB3-P2 | Search Owner | `ED-07/ED-11/ED-41/ED-46/ED-48/ED-49` | canonical search 仍受保留能力门禁；`1008009904` 只能绑定 canonical，不得污染 lite |

### 5.4 Promotion / Growth

| capabilityId | domain | pageRoute | backendApi | status | priority | releaseBatch | owner | evidenceDoc | statusReason / Gate |
|---|---|---|---|---|---|---|---|---|---|
| CAP-PROMO-001 | promotion.coupon-point-home | `/pages/coupon/list`; `/pages/coupon/detail`; `/pages/activity/point/list`; `/pages/index/index` | `GET /promotion/coupon-template/page`; `GET /promotion/coupon/page`; `POST /promotion/coupon/take`; `GET /promotion/point-activity/page`; `GET /promotion/diy-template/used` | ACTIVE | P0 | RB1-P0 | Promotion Domain Owner | `ED-01/ED-03/ED-08/ED-10/ED-11` | 券、积分商城、首页增长主链路继续属于 03-09 Frozen 基线 |
| CAP-PROMO-002 | promotion.activity-growth | `/pages/goods/seckill`; `/pages/goods/groupon`; `/pages/activity/index`; `/pages/activity/groupon/*`; `/pages/activity/seckill/list` | `/promotion/activity/list-by-spu-id`; `/promotion/combination-*`; `/promotion/seckill-*`; `/promotion/reward-activity/get` | PLANNED_RESERVED | P1 | RB2-P1 | Promotion Domain Owner | `ED-35/ED-42/ED-47/ED-48/ED-49` | 03-10 文档包已齐，但 contract 继续要求整域按 `PLANNED_RESERVED` 管理；活动聚合返回 `type=2 bargain` 时只能隐藏或忽略 |
| CAP-PROMO-003 | promotion.bargain-be-only | `N/A（当前无真实用户页）` | `/promotion/bargain-activity/*`; `/promotion/bargain-record/*`; `/promotion/bargain-help/*` | PLANNED_RESERVED | P2 | RB3-P2 | Promotion Domain Owner | `ED-42/ED-47/ED-48/ED-49` | 砍价仅属后端存在，当前无 FE route、无 FE API 文件、无页面绑定 |

### 5.5 Booking / Technician Service

| capabilityId | domain | pageRoute | backendApi | status | priority | releaseBatch | owner | evidenceDoc | statusReason / Gate |
|---|---|---|---|---|---|---|---|---|---|
| CAP-BOOKING-001 | booking.query-order | `/pages/booking/order-list`; `/pages/booking/order-detail` | `GET /booking/order/list`; `GET /booking/order/get` | ACTIVE | P0 | RB1-P0 | Booking Domain Owner | `ED-01/ED-03/ED-05/ED-10/ED-31/ED-43/ED-57/ED-58/ED-59/ED-60/ED-61/ED-63/ED-64/ED-65/ED-66` | 03-16 正式产出已确认订单查询面只能作为 query-only `ACTIVE`；`order-list` 返回结构漂移、`payOrderId` 未绑定、`success(null)` 空结果都不得被外推成写链或支付闭环 |
| CAP-BOOKING-002 | booking.query-technician | `/pages/booking/technician-list`; `/pages/booking/technician-detail` | `GET /booking/technician/list`; `GET /booking/technician/get`; `GET /booking/slot/list-by-technician` | ACTIVE | P1 | RB2-P1 | Booking Domain Owner | `ED-05/ED-31/ED-43/ED-52/ED-57/ED-58/ED-59/ED-60/ED-61/ED-62/ED-63/ED-64/ED-65/ED-66` | 技师列表/详情/时段查询在当前分支代码已对齐，并被 03-16 产品、contract、gate 一致确认为 query-only `ACTIVE`；`title/specialties/status` 仍只是页面 fallback，不得冲抵 create 发布证据 |
| CAP-BOOKING-003 | booking.create-chain | `/pages/booking/technician-list`; `/pages/booking/technician-detail`; `/pages/booking/order-confirm` | `GET /booking/technician/list`; `GET /booking/technician/get`; `GET /booking/slot/list-by-technician`; `POST /booking/order/create` | PLANNED_RESERVED | P1 | RB2-P1 | Booking Domain Owner | `ED-05/ED-11/ED-31/ED-43/ED-52/ED-57/ED-58/ED-59/ED-60/ED-61/ED-62/ED-63/ED-64/ED-65/ED-66` | 03-16 正式产出已共同确认 create 链当前只能写成 `Doc Closed + Can Develop + Cannot Release`；`loadTimeSlots(technicianId, null)` 与 `duration/spuId/skuId` 仍未闭环，shared gate 仍固定 `can_release=NO` |
| CAP-BOOKING-004 | booking.cancel | `/pages/booking/order-list`; `/pages/booking/order-detail` | `POST /booking/order/cancel` | PLANNED_RESERVED | P1 | RB2-P1 | Booking Domain Owner | `ED-05/ED-17/ED-18/ED-31/ED-43/ED-52/ED-57/ED-58/ED-59/ED-60/ED-61/ED-62/ED-63/ED-64/ED-65/ED-66` | canonical `POST + query(id,reason)` 已对齐；当前稳定 errorCode 只认 `1030004000/1030004005/1030004006`，但仍缺发布级状态变更样本与回放证据，因此继续阻断放量 |
| CAP-BOOKING-005 | booking.addon-upgrade | `/pages/booking/addon`; `/pages/booking/order-detail` | `POST /app-api/booking/addon/create` | PLANNED_RESERVED | P1 | RB2-P1 | Booking Domain Owner | `ED-05/ED-11/ED-18/ED-31/ED-43/ED-52/ED-57/ED-58/ED-59/ED-60/ED-61/ED-62/ED-63/ED-64/ED-65/ED-66` | add-on path 已对齐且失败不再伪成功；但页面当前只提交 `parentOrderId,addonType`，`upgrade / add-item` 仍存在 pseudo success / no-op risk，不能升为放量能力 |
| CAP-BOOKING-006 | booking.review-history | `/pages/booking/review-list`; `/pages/booking/review-detail` | `GET /booking/review/page`; `GET /booking/review/get`; `GET /booking/review/summary` | ACTIVE | P1 | RB2-P1 | Booking Domain Owner | `ED-69/ED-70/ED-71/ED-72/ED-73/ED-74/ED-75/ED-76` | booking review 历史 / 汇总已具备真实 route、API、controller、final review 与页面级 acceptance 文档；当前只允许按 query-side `ACTIVE` 管理，`averageScore` 未展示、`[]/0` 只算合法空态，`getReview` 已由 `review-detail` 消费但不改变放量结论 |
| CAP-BOOKING-007 | booking.review-submit | `/pages/booking/order-list`; `/pages/booking/order-detail`; `/pages/booking/review-add`; `/pages/booking/review-result` | `GET /booking/review/eligibility`; `POST /booking/review/create` | PLANNED_RESERVED | P1 | RB2-P1 | Booking Domain Owner | `ED-69/ED-70/ED-71/ED-72/ED-73/ED-74/ED-75/ED-76` | booking review 已作为 booking 新子域落地，但当前最终结论固定为 `Doc Closed / Can Develop / Cannot Release`；`ED-76` 仅补 query-side detail acceptance，不等于 submit / recovery 已 release-ready；缺 feature flag / rollout / runtime sample pack，且 `serviceOrderId` 仅 best-effort 回填且仍可为空、`picUrls` 仅完成提交链路、无自动奖励 / 补偿 / 店长通知证据 |

### 5.6 Content / Service / Brokerage

| capabilityId | domain | pageRoute | backendApi | status | priority | releaseBatch | owner | evidenceDoc | statusReason / Gate |
|---|---|---|---|---|---|---|---|---|---|
| CAP-CONTENT-001 | content.diy-template-page | `App 初始化`; `/pages/index/page` | `GET /promotion/diy-template/used`; `GET /promotion/diy-template/get`; `GET /promotion/diy-page/get` | ACTIVE | P1 | RB2-P1 | Content Ops Owner | `ED-32/ED-39/ED-44/ED-48/ED-49` | DIY 模板与自定义页已有真实入口、contract 和验收矩阵，可作为当前 content 域唯一明确 `ACTIVE` 能力 |
| CAP-CONTENT-002 | content.chat-article-faq-shell | `/pages/chat/index`; `/pages/public/richtext`; `/pages/public/faq`; `/pages/public/webview` | `GET /promotion/kefu-message/list`; `POST /promotion/kefu-message/send`; `GET /promotion/article/get` | PLANNED_RESERVED | P1 | RB2-P1 | Content Ops Owner | `ED-32/ED-39/ED-44/ED-48/ED-49` | 文档包已齐，但 contract 继续把聊天发送、消息列表、文章详情、FAQ 壳页固定在 `PLANNED_RESERVED` 口径；不得伪成功 |
| CAP-CONTENT-003 | content.article-list-category-be-only | `N/A（当前无真实用户页）` | `GET /promotion/article/list`; `GET /promotion/article/page`; `GET /promotion/article-category/list`; `PUT /promotion/article/add-browse-count`; `PUT /promotion/kefu-message/update-read-status` | PLANNED_RESERVED | P2 | RB3-P2 | Content Ops Owner | `ED-39/ED-44/ED-48/ED-49` | 后端存在但前端未消费，统一按保留能力管理 |
| CAP-BROKERAGE-001 | brokerage.runtime-pages | `/pages/commission/index`; `/pages/commission/wallet`; `/pages/commission/withdraw`; `/pages/commission/team`; `/pages/commission/commission-ranking`; `/pages/commission/promoter`; `/pages/commission/order`; `/pages/commission/goods` | `/trade/brokerage-user/*`; `/trade/brokerage-record/*`; `/trade/brokerage-withdraw/*` | PLANNED_RESERVED | P1 | RB2-P1 | Brokerage Domain Owner | `ED-33/ED-40/ED-45/ED-48/ED-49/ED-50` | 文档包已齐；03-11 已补客服/资金 SOP，明确 `withdrawPrice / brokeragePrice / frozenPrice` 与“提现申请成功仅代表建单成功，不代表到账成功”，但整域仍保持 `PLANNED_RESERVED` |

### 5.7 Reserved Expansion Domains

| capabilityId | domain | pageRoute | backendApi | status | priority | releaseBatch | owner | evidenceDoc | statusReason / Gate |
|---|---|---|---|---|---|---|---|---|---|
| CAP-RESERVED-001 | gift-card | `/pages/gift-card/*`（未实现） | `/promotion/gift-card/*`（未实现） | PLANNED_RESERVED | P2 | RB3-P2 | Trade Domain Owner | `ED-13/ED-14/ED-12/ED-19/ED-36/ED-37/ED-48/ED-49/ED-54` | 治理与灰度验收文档已齐，但前后端仍未落地；03-11 readiness register 已固定 `miniapp.gift-card=off` 下的 No-Go 证据 |
| CAP-RESERVED-002 | referral | `/pages/referral/*`（未实现） | `/promotion/referral/*`（未实现） | PLANNED_RESERVED | P2 | RB3-P2 | Promotion Domain Owner | `ED-13/ED-15/ED-12/ED-19/ED-36/ED-37/ED-48/ED-49/ED-54` | 治理与灰度验收文档已齐，但前后端仍未落地；03-11 readiness register 已固定 `miniapp.referral=off` 下的 No-Go 证据 |
| CAP-RESERVED-003 | technician-feed | `/pages/technician/feed`（未实现） | `/booking/technician/feed/*`（未实现） | PLANNED_RESERVED | P2 | RB3-P2 | Booking + Content Ops Owner | `ED-13/ED-16/ED-12/ED-19/ED-36/ED-37/ED-48/ED-49/ED-54/ED-55` | 已从 policy 升级到完整 PRD，但当前仍无真实页面、无真实 app controller、无运行样本；03-11 readiness register 已固定 `miniapp.technician-feed.audit=off` 下的 No-Go 证据 |

## 6. Deprecated 路由别名（不再作为执行真值）

| capabilityId | domain | pageRoute | backendApi | status | priority | releaseBatch | owner | evidenceDoc | statusReason / Gate |
|---|---|---|---|---|---|---|---|---|---|
| CAP-ALIAS-001 | route-alias.after-sale | `/pages/after-sale/create`; `/pages/after-sale/list`; `/pages/after-sale/detail` | 同交易售后接口 | DEPRECATED | P0 | N/A-DEPRECATED | MiniApp FE Owner | `ED-01/ED-10/ED-20` | 真实 uniapp 路由是 `/pages/order/aftersale/*` |
| CAP-ALIAS-002 | route-alias.member-login | `/pages/public/login`; `/pages/user/index`; `/pages/user/sign-in` | 对应 member 接口 | DEPRECATED | P0 | N/A-DEPRECATED | MiniApp FE Owner | `ED-21/ED-30/ED-38` | 真实入口分别是 `component:s-auth-modal + /pages/index/login`、`/pages/index/user`、`/pages/app/sign` |
| CAP-ALIAS-003 | route-alias.member-promo | `/pages/address/list`; `/pages/point/mall`; `/pages/coupon/center` | 对应 member/promotion 接口 | DEPRECATED | P0 | N/A-DEPRECATED | MiniApp FE Owner | `ED-01/ED-10/ED-20/ED-30` | 真实 uniapp 路由分别是 `/pages/user/address/list`、`/pages/activity/point/list`、`/pages/coupon/list` |
| CAP-ALIAS-004 | route-alias.search-booking | `/pages/search/index`; `/pages/booking/list` | `GET /product/search/page`; `GET /booking/order/list` | DEPRECATED | P0 | N/A-DEPRECATED | MiniApp FE Owner | `ED-05/ED-07/ED-31/ED-41` | 真实 route 是 `/pages/index/search`、`/pages/booking/order-list`；canonical search 当前也非运行时页面 |

## 7. 当前结论
1. 03-09 Frozen 基线继续覆盖 trade/pay、after-sale/refund、coupon/point/home-growth 主链路。
2. 03-10 文档包已从“缺文档”推进到“全部正式落盘”，但 capability 状态并没有因此自动升为 `ACTIVE`。
3. 当前最关键的假 Active 风险仍在 booking 域：把 query-only `ACTIVE`、gate `PASS`、空态或未绑定字段外推成 write-chain 可放量。
4. member 域继续守住三类能力不得假 Active：`/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag`。
5. content / brokerage / product-interaction / marketing-expansion 当前都必须按 contract 维持在 `PLANNED_RESERVED` 或混合 scope 管理；FAQ 壳页、收藏 typo path、`type=2 bargain`、提现到账口径都不得漂移。
6. gift-card / referral / technician-feed 仍是 `P2/RB3-P2` 规划态，只能按治理和灰度门禁管理，不得算进发布已上线能力。
7. 03-10 终审结论保持：`Frozen Candidate = 0`。
8. 03-14 最终阻断集成结论保持：`可进入真值修复开发，不可把 blocker scope 直接纳入放量范围`。
9. 03-16 booking 最终集成结论保持：`Doc Closed / Can Develop / Cannot Release`；`BO-004` 继续保持“仅接口闭环 + 页面真值待核”。
10. 03-17 booking review 子域已补齐 PRD / contract / failureMode / runbook / gate / final review，但当前只允许把 history / summary 记为 query-side `ACTIVE`，submit / recovery 继续 `Cannot Release`。
