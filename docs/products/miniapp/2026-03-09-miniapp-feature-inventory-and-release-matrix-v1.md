# MiniApp Feature Inventory and Release Matrix v1 (2026-03-09)

## 1. Scope and Freeze Rule
- Branch: `feat/ui-four-account-reconcile-ops`
- Baseline docs:
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/products/miniapp/2026-03-08-miniapp-ia-routing-map-v1.md`
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
  - `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
- Rule:
  - Existing semantics are backward-compatible only.
  - Error handling must be code-driven, not message-driven.
  - Downstream dependency failures use fail-open where contract allows.
  - `RESERVED_DISABLED` codes can only appear after switch-on and gray release approval.

## 2. Release Batch Definition
- `RB1-P0`: transaction/service must-have for go-live.
- `RB2-P1`: growth and operational efficiency closure.
- `RB3-P2`: expansion features and traffic-scale optimization.

## 3. Feature Inventory and Release Matrix

| Page | Feature | Route | API | State Machine / Truth Source | Error Code Anchor | Degrade Semantics | Owner | Priority | Release Batch | Acceptance Criteria |
|---|---|---|---|---|---|---|---|---|---|---|
| 支付结果 | 支付聚合查询 | `/pages/pay/result` | `GET /trade/order/pay-result` | Trade/Pay aggregate; pay status as truth when available | `ORDER_NOT_FOUND(1011000011)` / `PAY_ORDER_NOT_FOUND` | pay单缺失返回 `degraded=true`，页面可展示并可重试 | Trade Domain Owner | P0 | RB1-P0 | 1 happy + 1 not-found + 1 degraded path |
| 订单列表 | 订单分页与计数 | `/pages/order/list` | `GET /trade/order/page` + `GET /trade/order/get-count` | Trade order state | `ORDER_NOT_FOUND(1011000011)` | 查询失败不崩页，保留筛选条件可重试 | Trade Domain Owner | P0 | RB1-P0 | 列表/筛选/空态/错误态均可恢复 |
| 订单详情 | 订单明细查询 | `/pages/order/detail` | `GET /trade/order/get-detail` | Trade order/item state | `ORDER_NOT_FOUND(1011000011)` | 详情加载失败回退到列表并提示错误码 | Trade Domain Owner | P0 | RB1-P0 | 详情字段完整，失败不阻断返回路径 |
| 售后申请 | 提交售后 | `/pages/after-sale/create` | `POST /trade/after-sale/create` | After-sale state `APPLY->...` | `AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED(1011000125)` | 业务拦截显示码+动作，不吞错 | Trade Domain Owner | P0 | RB1-P0 | 提交成功/业务阻断/参数错误可区分 |
| 售后列表 | 售后分页 | `/pages/after-sale/list` | `GET /trade/after-sale/page` | After-sale state | `AFTER_SALE_NOT_FOUND(1011000100)` | 空列表为有效结果，不作为异常 | Trade Domain Owner | P0 | RB1-P0 | 空态不报错，筛选条件稳定 |
| 售后详情 | 售后详情查询 | `/pages/after-sale/detail` | `GET /trade/after-sale/get` | After-sale + pay refund linkage | `AFTER_SALE_NOT_FOUND(1011000100)` | pay退款信息缺失时保留售后主信息 | Trade Domain Owner | P0 | RB1-P0 | 详情可查，pay缺失可降级展示 |
| 退款进度 | 退款状态聚合 | `/pages/refund/progress` | `GET /trade/after-sale/refund-progress` | Refund progress code as contract truth | `AFTER_SALE_NOT_FOUND(1011000100)` | pay退款单缺失按售后状态回退进度 | Trade Domain Owner | P0 | RB1-P0 | `REFUND_*` 进度码稳定且可重试 |
| 异常兜底 | 全局降级页 | `/pages/common/exception` | `N/A` | Route fallback state | `TICKET_SYNC_DEGRADED` | 展示可恢复动作（重试/联系客服） | MiniApp FE Owner | P0 | RB1-P0 | 任一阻断错误可跳转兜底页 |
| 预约列表 | 预约状态查询 | `/pages/booking/list` | `GET /booking/order/list` + `GET /booking/order/list-by-status` | Booking order state | `BOOKING_ORDER_NOT_EXISTS` | 状态筛选为空时回退全部列表 | Booking Domain Owner | P0 | RB1-P0 | 查询/筛选/空态可用 |
| 地址管理 | 地址增删改查 | `/pages/address/list` | `GET /member/address/get-default` + `GET /member/address/page` + `POST /member/address/create` + `PUT /member/address/update` + `DELETE /member/address/delete` | Member address state | `MEMBER_ADDRESS_*` | 无默认地址返回空并引导新增 | Member Domain Owner | P0 | RB1-P0 | CRUD + 默认地址切换可回归 |
| 领券中心 | 券模板与领取 | `/pages/coupon/center` | `GET /promotion/coupon-template/page` + `POST /promotion/coupon/take` | Coupon state machine | `PROMOTION_COUPON_*` | 领取失败不播“成功”动效，展示错误码 | Promotion Domain Owner | P0 | RB1-P0 | 领取成功/重复领取/活动失效路径 |
| 积分商城 | 活动+积分记录 | `/pages/point/mall` | `GET /promotion/point-activity/page` + `GET /member/point/record/page` | Points account + activity state | `PRICE_CALCULATE_POINT_TOTAL_LIMIT_COUNT(1011003004)` | 活动空数据返回空态，不阻断页面 | Promotion + Member Domain Owner | P0 | RB1-P0 | 活动页/记录页/规则限制提示可用 |
| 首页运营位 | 活动卡与店铺上下文一致性 | `/pages/home/index` | `GET /promotion/activity/home-blocks` | Activity truth binding + catalog version | `MINIAPP_HOME_ACTIVITY_CONTEXT_MISMATCH(1008009901, RESERVED_DISABLED)` | 动效降级（low-end/reduced-motion），不影响主CTA | Product Owner + Promotion Domain Owner | P1 | RB2-P1 | CTA唯一主路径，活动真值一致 |
| 项目页同源刷新 | 类目切换价库存可约同步 | `/pages/service/catalog` | `GET /product/catalog/page` | Catalog snapshot/version truth | `MINIAPP_CATALOG_VERSION_MISMATCH(1008009902, RESERVED_DISABLED)` | 版本不一致时强制全量刷新并提示 | Product Domain Owner | P1 | RB2-P1 | 切类目后无陈旧价格/库存 |
| 加购冲突处理 | 数量冲突与库存不足反馈 | `/pages/service/catalog` | `POST /booking/addon-intent/submit` | Booking/Product state | `MINIAPP_ADDON_INTENT_CONFLICT(1008009903, RESERVED_DISABLED)` / `SKU_STOCK_NOT_ENOUGH(1008006004)` | 冲突时回显服务端数量，不自动成功 | Booking + Product Domain Owner | P1 | RB2-P1 | 冲突、库存不足、重试路径清晰 |
| 预约排期 | 时段选择与冲突提醒 | `/pages/booking/schedule` | `GET /booking/slot/list-by-technician` + `POST /booking/order/create` | Booking schedule truth | `SCHEDULE_CONFLICT(1030002001)` / `TIME_SLOT_NOT_AVAILABLE(1030003001)` / `TIME_SLOT_ALREADY_BOOKED(1030003002)` | 冲突 fail-fast，给可选时段 | Booking Domain Owner | P1 | RB2-P1 | 可约时段、冲突、改约链路完整 |
| 会员资产总账 | 券/积分/权益一致账本 | `/pages/profile/assets` | `GET /member/asset-ledger/page` | Unified asset ledger truth | `MINIAPP_ASSET_LEDGER_MISMATCH(1004009901, RESERVED_DISABLED)` | 下游失败不吞数据，显示局部降级标签 | Member + Promotion Domain Owner | P1 | RB2-P1 | 资产变动可追溯、口径一致 |
| 搜索发现 | 全局搜索与召回排序 | `/pages/search/index` | `GET /product/search/page` | Search result truth | `MINIAPP_SEARCH_QUERY_INVALID(1008009904, RESERVED_DISABLED)` | 检索失败保留query并可重试 | Product Domain Owner | P1 | RB2-P1 | 热词/结果/空态/纠错路径可用 |
| 礼品卡域 | 购买/赠送/领取/核销/退款 | `/pages/gift-card/*` | `POST /promotion/gift-card/order/create` + `POST /promotion/gift-card/redeem` | Gift-card lifecycle truth | `GIFT_CARD_ORDER_NOT_FOUND(1011009901, RESERVED_DISABLED)` / `GIFT_CARD_REDEEM_CONFLICT(1011009902, RESERVED_DISABLED)` | 核销或退款失败返回码+补救动作 | Trade + Member Domain Owner | P2 | RB3-P2 | 全生命周期+幂等回归 |
| 邀请有礼 | 关系绑定与奖励到账 | `/pages/referral/*` | `POST /promotion/referral/bind-inviter` + `GET /promotion/referral/reward-ledger/page` | Referral reward ledger truth | `REFERRAL_BIND_CONFLICT(1013009901, RESERVED_DISABLED)` / `REFERRAL_REWARD_LEDGER_MISMATCH(1013009902, RESERVED_DISABLED)` | 到账延迟显示处理中，不伪成功 | Promotion Domain Owner | P2 | RB3-P2 | 关系绑定/奖励发放/冲正可追溯 |
| 技师动态广场 | feed流与互动 | `/pages/technician/feed` | `GET /booking/technician/feed/page` + `POST /booking/technician/feed/like` | Feed moderation truth | `TECHNICIAN_FEED_AUDIT_BLOCKED(1030009901, RESERVED_DISABLED)` | 审核阻断不影响浏览主链路 | Booking + Content Ops Owner | P2 | RB3-P2 | 发布/审核/展示/降级路径可验收 |

## 4. Cross-Cutting Acceptance Baseline
- 每个页面或功能至少包含：
  - 1条 happy path
  - 1条 business error code path
  - 1条 degraded/fail-open path
- 关键链路日志检索字段必须可查：`runId/orderId/payRefundId/sourceBizNo/errorCode`。
- `RESERVED_DISABLED` 错误码进入生产前必须满足：
  - 契约冻结完成
  - 门禁开关开启并灰度通过
  - SOP 与运营动作完成联调演练

## 5. Window Ownership and Delivery Dependency
- Window B (UI/PRD): home growth, booking schedule, member asset ledger, search discovery, release acceptance.
- Window C (contracts): addbook conflict, gift-card, referral, technician feed, canonical error register.
- Window D (data/compliance): event taxonomy v2, dashboard/experiment/DQ gates.
- Window A (integration): feature matrix freeze, index governance, release decision single source of truth.
