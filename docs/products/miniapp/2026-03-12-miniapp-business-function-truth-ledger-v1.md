# MiniApp 业务功能真值台账与 PRD 完整度 v1（2026-03-12）

## 1. 目标与范围
- 目标：把当前项目的业务功能收口到“页面真值 + 接口真值 + 对应 PRD 完整度 + 是否阻断开发”四个维度，作为后续开发排期与 PRD 补齐的单一真值。
- 统计范围：
  - 用户侧小程序前端：`yudao-mall-uniapp/pages.json`
  - 用户侧前端 API：`yudao-mall-uniapp/sheep/api/`
  - 用户侧 app 后端接口：`ruoyi-vue-pro-master/**/controller/app/`
  - 后台运营独立业务能力：`ruoyi-vue-pro-master/**/controller/admin/` 中已形成业务页面或明确业务链路的 booking 财务闭环能力
- 不纳入本台账：
  - `dict / tenant / area / file` 等基础设施接口
  - 已废弃 alias route
  - 仅作技术支撑、不形成独立业务能力的 config 查询

## 2. 判定规则

### 2.1 页面 / 接口真值
- 页面真值：只认当前代码库真实存在的 uniapp 页面，或后台已存在的 admin 页面文件。
- 接口真值：只认当前 controller 和前端 API 文件中真实存在的 `method + path`。
- 若文档口径与真实 route / API 不一致，以代码真值为准。

### 2.2 PRD 完整度
- `完整`：有独立 PRD，覆盖业务目标、页面边界、关键规则、保留能力边界，可直接作为产品输入。
- `较完整`：有 PRD 或蓝图，但仍需要 route truth / truth review 文档配合，单靠 PRD 不能避免落地歧义。
- `部分完整`：有 product policy、蓝图或局部说明，但还不是完整独立 PRD。
- `缺失`：当前未找到对应产品 PRD。

### 2.3 是否阻断开发
- `否`：不阻断该功能继续开发或维护。
- `部分`：不阻断存量主链维护，但阻断该能力升为 `ACTIVE`、`Frozen Candidate` 或放量范围。
- `是`：当前缺口直接阻断该能力继续开发、联调、放量或验收。

## 3. 用户侧小程序业务功能清单

| 功能ID | 业务域 | 业务功能 | 页面真值 | 接口 / Controller 真值 | 当前状态 | 对应 PRD | PRD 完整度 | 是否阻断开发 | 说明 |
|---|---|---|---|---|---|---|---|---|---|
| BF-001 | Trade | 购物车 / 结算 / 下单创建 | `/pages/index/cart`; `/pages/order/confirm` | `GET /trade/order/settlement`; `POST /trade/order/create`; `AppCartController`; `AppTradeOrderController` | ACTIVE | `docs/products/miniapp/2026-03-12-miniapp-trade-checkout-order-prd-v1.md` | 完整 | 否 | 已从服务蓝图拆出独立交易 PRD，当前以 checkout + create 为主真值 |
| BF-002 | Pay | 支付提交 / 支付结果 / 轮询 | `/pages/pay/index`; `/pages/pay/result` | `POST /pay/order/submit`; `GET /trade/order/pay-result`; `GET /pay/order/get`; `GET /pay/channel/get-enable-code-list`; `AppPayOrderController`; `AppPayChannelController` | ACTIVE | `docs/products/miniapp/2026-03-12-miniapp-pay-submit-result-prd-v1.md` | 完整 | 否 | 已拆出独立支付 PRD，明确渠道、支付单查询和结果回流边界 |
| BF-003 | Trade | 订单列表 / 详情 / 收货 / 取消 / 删除 | `/pages/order/list`; `/pages/order/detail` | `GET /trade/order/page`; `GET /trade/order/get-count`; `GET /trade/order/get-detail`; `PUT /trade/order/receive`; `DELETE /trade/order/cancel`; `DELETE /trade/order/delete`; `AppTradeOrderController` | ACTIVE | `docs/products/miniapp/2026-03-12-miniapp-trade-checkout-order-prd-v1.md` | 完整 | 否 | 订单生命周期已纳入独立交易 PRD，legacy fallback 不再作为产品真值 |
| BF-004 | Trade | 售后申请 / 退款进度 / 回寄 / 日志 | `/pages/order/aftersale/apply`; `/pages/order/aftersale/list`; `/pages/order/aftersale/detail`; `/pages/order/aftersale/log`; `/pages/order/aftersale/return-delivery` | `POST /trade/after-sale/create`; `GET /trade/after-sale/page`; `GET /trade/after-sale/get`; `GET /trade/after-sale-log/list`; `PUT /trade/after-sale/delivery`; `DELETE /trade/after-sale/cancel`; `GET /trade/after-sale/refund-progress`; `AppAfterSaleController`; `AppAfterSaleLogController` | ACTIVE | `docs/products/miniapp/2026-03-12-miniapp-after-sale-refund-prd-v1.md` | 完整 | 否 | 已拆出独立售后退款 PRD，退款进度按真实接口收口，不虚构独立页路由 |
| BF-005 | Fulfillment | 物流轨迹 / 履约口径 | `/pages/order/express/log` | `GET /trade/order/get-express-track-list`; `AppDeliverExpressController`; `AppDeliverPickUpStoreController` | ACTIVE | `docs/products/miniapp/2026-03-09-miniapp-fulfillment-logistics-prd-v1.md` | 完整 | 否 | 已有独立物流履约 PRD |
| BF-006 | Promotion | 首页增长 / 首页装修 / 券入口 / 积分商城入口 | `/pages/index/index`; `/pages/index/page`; `/pages/coupon/list`; `/pages/coupon/detail`; `/pages/activity/point/list` | `GET /promotion/diy-template/used`; `GET /promotion/diy-page/get`; `GET /promotion/coupon-template/page`; `GET /promotion/coupon/page`; `POST /promotion/coupon/take`; `GET /promotion/point-activity/page`; `AppDiyTemplateController`; `AppDiyPageController`; `AppCouponTemplateController`; `AppCouponController`; `AppPointActivityController`; `AppBannerController` | ACTIVE | `docs/products/miniapp/2026-03-09-miniapp-home-growth-prd-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-member-asset-ledger-prd-v1.md` | 完整 | 否 | 首页增长和券积分能力已被两份 PRD 覆盖 |
| BF-007 | Member | 登录 / 注册 / 微信手机号登录 / 社交绑定 | `component:s-auth-modal`; `/pages/index/login` | `/member/auth/*`; `/member/social-user/*`; `AppAuthController`; `AppSocialUserController`; `CrmebFrontWechatCompatController` | ACTIVE | `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | 完整 | 否 | 登录和社交绑定口径完整 |
| BF-008 | Member | 个人资料 / 安全设置 / 退出登录 | `/pages/index/user`; `/pages/user/info`; `/pages/public/setting` | `GET /member/user/get`; `PUT /member/user/update`; `PUT /member/user/update-mobile*`; `PUT /member/user/update-password`; `PUT /member/user/reset-password`; `POST /member/auth/logout`; `AppMemberUserController` | ACTIVE | `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | 完整 | 否 | 资料与安全设置已收口到真实 route |
| BF-009 | Member | 地址管理 | `/pages/user/address/list`; `/pages/user/address/edit` | `GET /member/address/list`; `GET /member/address/get`; `GET /member/address/get-default`; `POST /member/address/create`; `PUT /member/address/update`; `DELETE /member/address/delete`; `AppAddressController` | ACTIVE | `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | 完整 | 否 | 地址 CRUD 主链完整 |
| BF-010 | Member | 钱包 / 充值 / 钱包流水 / 积分流水 / 优惠券 / 积分商城 | `/pages/user/wallet/money`; `/pages/user/wallet/score`; `/pages/pay/recharge`; `/pages/pay/recharge-log`; `/pages/coupon/list`; `/pages/activity/point/list` | `GET /pay/wallet/get`; `GET /pay/wallet-transaction/page`; `POST /pay/wallet-recharge/create`; `GET /pay/wallet-recharge/page`; `GET /pay/wallet-recharge-package/page`; `GET /member/point/record/page`; `GET /promotion/coupon/page`; `GET /promotion/point-activity/page`; `AppPayWalletController`; `AppPayWalletRechargeController`; `AppPayWalletRechargePackageController`; `AppPayWalletTransactionController`; `AppMemberPointRecordController`; `AppCouponController`; `AppPointActivityController` | ACTIVE | `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-member-asset-ledger-prd-v1.md` | 完整 | 否 | 资产域 PRD 边界已较清楚 |
| BF-011 | Member | 签到 | `/pages/app/sign` | `GET /member/sign-in/config/list`; `GET /member/sign-in/record/get-summary`; `POST /member/sign-in/record/create`; `GET /member/sign-in/record/page`; `AppMemberSignInConfigController`; `AppMemberSignInRecordController` | ACTIVE | `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | 完整 | 否 | 重复签到、奖励发放边界已补齐 |
| BF-012 | Member | 会员等级 / 成长值 | `N/A（当前无真实 pageRoute）` | `GET /member/level/list`; `GET /member/experience-record/page`; `AppMemberLevelController`; `AppMemberExperienceRecordController` | PLANNED_RESERVED | `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | 完整 | 是 | `/pages/user/level` 未落地，直接阻断能力继续推进 |
| BF-013 | Member | 资产总览 / 统一资产台账 | `N/A（当前无真实 pageRoute）` | `GET /member/asset-ledger/page` | PLANNED_RESERVED | `docs/products/miniapp/2026-03-09-miniapp-member-asset-ledger-prd-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | 完整 | 是 | `/pages/profile/assets` 未落地，且接口仍受门禁保护 |
| BF-014 | Member | 用户标签中心 | `N/A（当前无真实 pageRoute）` | `N/A（当前无 app 端读取接口）` | PLANNED_RESERVED | `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | 完整 | 是 | `/pages/user/tag` 缺页，且没有 app 标签读取接口 |
| BF-015 | Product | 商品分类 / 搜索 lite / 商品详情 | `/pages/index/category`; `/pages/index/search`; `/pages/goods/list`; `/pages/goods/index` | `GET /product/category/list`; `GET /product/spu/page`; `GET /product/spu/get-detail`; `GET /trade/order/settlement-product`; `AppCategoryController`; `AppProductSpuController` | ACTIVE | `docs/products/miniapp/2026-03-10-miniapp-product-catalog-interaction-prd-v1.md`; `docs/products/miniapp/2026-03-09-miniapp-search-discovery-prd-v1.md` | 完整 | 否 | `search-lite` 已收口 |
| BF-016 | Product | 评论 / 收藏 / 浏览历史 | `/pages/goods/comment/list`; `/pages/goods/comment/add`; `/pages/user/goods-collect`; `/pages/user/goods-log` | `GET /product/comment/page`; `GET /product/favorite/page`; `GET /product/favorite/exits`; `POST /product/favorite/create`; `DELETE /product/favorite/delete`; `GET /product/browse-history/page`; `DELETE /product/browse-history/delete`; `DELETE /product/browse-history/clean`; `POST /trade/order/item/create-comment`; `AppProductCommentController`; `AppFavoriteController`; `AppProductBrowseHistoryController` | PLANNED_RESERVED | `docs/products/miniapp/2026-03-10-miniapp-product-catalog-interaction-prd-v1.md` | 完整 | 部分 | 页面存在，但当前真值仍不允许直接升 `ACTIVE` |
| BF-017 | Product | 搜索 canonical | `N/A（当前无真实用户页）` | `GET /product/search/page`; `AppProductSpuController` | PLANNED_RESERVED | `docs/products/miniapp/2026-03-09-miniapp-search-discovery-prd-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-product-catalog-interaction-prd-v1.md` | 完整 | 是 | 无真实用户页，且 `1008009904` 只允许绑定 canonical |
| BF-018 | Promotion | 秒杀 / 拼团 / 满减送 / 活动聚合 | `/pages/goods/seckill`; `/pages/goods/groupon`; `/pages/activity/index`; `/pages/activity/groupon/*`; `/pages/activity/seckill/list` | `/promotion/activity/list-by-spu-id`; `/promotion/combination-*`; `/promotion/seckill-*`; `/promotion/reward-activity/get`; `AppActivityController`; `AppCombinationActivityController`; `AppCombinationRecordController`; `AppSeckillActivityController`; `AppSeckillConfigController`; `AppRewardActivityController` | PLANNED_RESERVED | `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-prd-v1.md` | 完整 | 部分 | 页面与接口存在，但整域仍被文档固定为保留范围 |
| BF-019 | Promotion | 砍价 | `N/A（当前无真实用户页）` | `/promotion/bargain-activity/*`; `/promotion/bargain-record/*`; `/promotion/bargain-help/*`; `AppBargainActivityController`; `AppBargainRecordController`; `AppBargainHelpController` | PLANNED_RESERVED | `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-prd-v1.md` | 完整 | 是 | 当前仅后端存在，无前端页面与 API 绑定 |
| BF-020 | Booking | 预约列表 / 预约详情 | `/pages/booking/order-list`; `/pages/booking/order-detail` | `GET /booking/order/list`; `GET /booking/order/get`; `AppBookingOrderController` | ACTIVE | `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md` | 较完整 | 否 | 查询链路当前可独立视为 `ACTIVE` |
| BF-021 | Booking | 技师详情 | `/pages/booking/technician-detail` | `GET /booking/technician/get`; `AppTechnicianController` | ACTIVE | `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md` | 较完整 | 否 | 技师详情已对齐 |
| BF-022 | Booking | 预约创建 | `/pages/booking/technician-list`; `/pages/booking/order-confirm` | 目标真值应为 `GET /booking/technician/list`; `GET /booking/slot/list-by-technician`; `POST /booking/order/create`; `AppTechnicianController`; `AppTimeSlotController`; `AppBookingOrderController` | PLANNED_RESERVED | `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-booking-route-api-truth-review-v1.md` | 较完整 | 是 | FE 仍发 `list-by-store` / `time-slot/list`，method/path 漂移阻断继续开发 |
| BF-023 | Booking | 预约取消 | `/pages/booking/order-list`; `/pages/booking/order-detail` | 目标真值应为 `POST /booking/order/cancel`; `AppBookingOrderController` | PLANNED_RESERVED | `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-booking-route-api-truth-review-v1.md` | 较完整 | 是 | FE 仍发 `PUT /booking/order/cancel` |
| BF-024 | Booking | 加钟 / 升级 | `/pages/booking/addon` | 目标真值应为 `POST /app-api/booking/addon/create`; `AppBookingAddonController` | PLANNED_RESERVED | `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`; `docs/products/miniapp/2026-03-10-miniapp-booking-route-api-truth-review-v1.md` | 较完整 | 是 | FE 仍发 `/booking/addon/create`，与 BE 不一致 |
| BF-025 | Content | DIY 模板 / 自定义页 | `App 初始化`; `/pages/index/page` | `GET /promotion/diy-template/used`; `GET /promotion/diy-template/get`; `GET /promotion/diy-page/get`; `AppDiyTemplateController`; `AppDiyPageController` | ACTIVE | `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md` | 完整 | 否 | 当前 content 域唯一明确 `ACTIVE` 能力 |
| BF-026 | Content | 聊天 / 文章详情 / FAQ 壳页 / WebView | `/pages/chat/index`; `/pages/public/richtext`; `/pages/public/faq`; `/pages/public/webview` | `GET /promotion/kefu-message/list`; `POST /promotion/kefu-message/send`; `GET /promotion/article/get`; `AppKeFuMessageController`; `AppArticleController` | PLANNED_RESERVED | `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md` | 完整 | 部分 | FAQ 只是壳页；聊天发送失败必须 fail-close；BF-027 已拆出独立 PRD，不能再当整域已上线 |
| BF-027 | Content | 文章列表 / 分类 / 浏览回写 / 已读回写 | `N/A（当前无真实用户页）` | `GET /promotion/article/list`; `GET /promotion/article/page`; `GET /promotion/article-category/list`; `PUT /promotion/article/add-browse-count`; `PUT /promotion/kefu-message/update-read-status`; `AppArticleController`; `AppArticleCategoryController`; `AppKeFuMessageController` | PLANNED_RESERVED | `docs/products/miniapp/2026-03-12-miniapp-content-article-list-category-writeback-prd-v1.md` | 完整 | 是 | 5 条接口真值已独立冻结，但仍无真实用户页与真实用户动作，继续阻断产品化放量 |
| BF-028 | Brokerage | 分销中心 / 佣金 / 提现 / 团队 / 排行 / 推广订单 / 推广商品 | `/pages/commission/index`; `/pages/commission/wallet`; `/pages/commission/withdraw`; `/pages/commission/team`; `/pages/commission/commission-ranking`; `/pages/commission/promoter`; `/pages/commission/order`; `/pages/commission/goods` | `/trade/brokerage-user/*`; `/trade/brokerage-record/*`; `/trade/brokerage-withdraw/*`; `AppBrokerageUserController`; `AppBrokerageRecordController`; `AppBrokerageWithdrawController` | PLANNED_RESERVED | `docs/products/miniapp/2026-03-10-miniapp-brokerage-distribution-prd-v1.md` | 完整 | 部分 | PRD 已齐，但整域仍未升 `ACTIVE` |
| BF-029 | Reserved | 礼品卡 | `N/A（当前无真实用户页）` | `N/A（当前无真实 app controller 闭环）` | PLANNED_RESERVED | `docs/products/miniapp/2026-03-09-miniapp-gift-card-business-prd-v1.md` | 完整 | 是 | 只有规划 PRD，无 runtime 实现 |
| BF-030 | Reserved | 邀请有礼 | `N/A（当前无真实用户页）` | `N/A（当前无真实 app controller 闭环）` | PLANNED_RESERVED | `docs/products/miniapp/2026-03-09-miniapp-referral-business-prd-v1.md` | 完整 | 是 | 只有规划 PRD，无 runtime 实现 |
| BF-031 | Reserved | 技师动态 | `N/A（当前无真实用户页）` | `N/A（当前无真实 app controller 闭环）` | PLANNED_RESERVED | `docs/products/miniapp/2026-03-12-miniapp-technician-feed-prd-v1.md` | 完整 | 是 | PRD 已补齐，但当前仍无真实页面、无真实 app controller、无运行样本，继续阻断开发 |

## 4. 后台运营独立业务功能清单

| 功能ID | 业务域 | 业务功能 | 页面真值 | 接口 / Controller 真值 | 当前状态 | 对应 PRD | PRD 完整度 | 是否阻断开发 | 说明 |
|---|---|---|---|---|---|---|---|---|---|
| BO-001 | Finance Ops | 四账对账 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/fourAccountReconcile/index.vue` | `/booking/four-account-reconcile/page`; `/booking/four-account-reconcile/get`; `/booking/four-account-reconcile/summary`; `/booking/four-account-reconcile/refund-commission-audit-page`; `/booking/four-account-reconcile/refund-audit-summary`; `/booking/four-account-reconcile/refund-commission-audit/sync-tickets`; `/booking/four-account-reconcile/run`; `FourAccountReconcileController` | ACTIVE_ADMIN | `docs/products/miniapp/2026-03-12-miniapp-finance-ops-four-account-reconcile-prd-v1.md` | 完整 | 否 | PRD 已补齐；当前重点转为对账降级提示、工单同步与差额解释的一致执行 |
| BO-002 | Finance Ops | 退款回调日志 / 重放 / 重放运行日志 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/refundNotifyLog/index.vue` | `/booking/refund-notify-log/page`; `/booking/refund-notify-log/replay`; `/booking/refund-notify-log/replay-due`; `/booking/refund-notify-log/replay-run-log/page`; `/booking/refund-notify-log/replay-run-log/detail/page`; `/booking/refund-notify-log/replay-run-log/detail/get`; `/booking/refund-notify-log/replay-run-log/get`; `/booking/refund-notify-log/replay-run-log/summary`; `/booking/refund-notify-log/replay-run-log/sync-tickets`; `BookingRefundNotifyLogController` | ACTIVE_ADMIN | `docs/products/miniapp/2026-03-12-miniapp-finance-ops-refund-notify-replay-prd-v1.md` | 完整 | 否 | PRD 已补齐；当前重点转为 warning / partial-fail / ticketSyncStatus 的执行一致性 |
| BO-003 | Finance Ops | 技师提成结算 / 审核 / 驳回 / 打款 / 通知补偿 | `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/index.vue`; `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/commission-settlement/outbox/index.vue` | `/booking/commission-settlement/page`; `/booking/commission-settlement/get`; `/booking/commission-settlement/create`; `/booking/commission-settlement/submit`; `/booking/commission-settlement/approve`; `/booking/commission-settlement/reject`; `/booking/commission-settlement/pay`; `/booking/commission-settlement/log-list`; `/booking/commission-settlement/notify-outbox-page`; `/booking/commission-settlement/notify-outbox-retry`; `/booking/commission-settlement/notify-outbox-batch-retry`; `TechnicianCommissionSettlementController` | ACTIVE_ADMIN | `docs/products/miniapp/2026-03-12-miniapp-finance-ops-technician-commission-settlement-prd-v1.md` | 完整 | 否 | BO-003 / BO-004 已拆成两份独立 PRD；当前重点转为审核 SLA、打款凭证和通知重试规则执行 |
| BO-004 | Finance Ops | 技师提成明细 / 计提管理 | `未核出（审查范围内无独立后台页面文件，详见 2026-03-12 commission admin truth review / 2026-03-14 closure review）` | `GET /booking/commission/list-by-technician`; `GET /booking/commission/list-by-order`; `GET /booking/commission/pending-amount`; `POST /booking/commission/settle`; `POST /booking/commission/batch-settle`; `GET /booking/commission/config/list`; `POST /booking/commission/config/save`; `DELETE /booking/commission/config/delete`; `TechnicianCommissionController` | ACTIVE_ADMIN | `docs/products/miniapp/2026-03-14-miniapp-finance-ops-technician-commission-detail-config-prd-v1.md` | 完整 | 部分 | A/B/C/D 已分别补齐 truth review、独立 PRD、controller-only contract、SOP、runbook；当前单一结论继续固定为“仅接口闭环 + 页面真值待核”，不得写成后台页面闭环完成 |

## 5. 当前阻断项汇总

### 5.1 直接阻断开发 / 联调 / 放量的缺口
1. Booking 创建链路：`BF-022`
2. Booking 取消链路：`BF-023`
3. Booking 加钟升级链路：`BF-024`
4. Member 等级页：`BF-012`
5. Member 资产总览：`BF-013`
6. Member 标签中心：`BF-014`
7. Product canonical search：`BF-017`
8. Promotion 砍价：`BF-019`
9. Content 文章列表 / 分类 / 回写：`BF-027`
10. Reserved 三类规划能力：`BF-029` ~ `BF-031`
11. 技师提成管理独立页面真值仍未核出：`BO-004`

### 5.2 不阻断存量主链，但阻断升为 ACTIVE / Frozen 的缺口
1. Product 互动链路：`BF-016`
2. Promotion 活动扩展整域：`BF-018`
3. Content 聊天 / FAQ 壳页 / 文章详情：`BF-026`
4. Brokerage 整域：`BF-028`

## 6. 结论
1. 用户侧主链功能的 PRD 已基本齐备，问题重点已从“有没有 PRD”转为“PRD 对应的页面/API 真值是否闭环”。
2. 当前最强阻断不在首页、交易、地址、签到等已冻结主链，而在 booking、member 缺页能力、reserved 规划能力和后台运营财务闭环。
3. 后台运营财务闭环的主 PRD、独立 PRD、controller-only contract、SOP 与 runbook 已补齐，当前剩余问题转为：佣金管理独立页面真值与前端 API 绑定证据仍需继续收口；该能力当前可进入真值修复开发，但不可直接作为放量依据。
4. 技师动态的产品文档已升级为完整 PRD，当前剩余问题只在 runtime 未实现，不能因文档齐备就提前进入开发或放量。
5. `BO-004` 已新增后台页面真值审查文档、独立 PRD、03-14 closure review、controller-only contract、SOP 与 runbook，当前单一结论是“仅接口闭环 + 页面真值待核”，不是“后台页面已完全闭环”。
6. `docs/products/miniapp/2026-03-14-miniapp-runtime-blocker-final-integration-v1.md` 已把 `Booking / BO-004 / Member 缺页 / Reserved runtime` 的最终阻断口径统一固定为“可进入真值修复开发，不可直接放量”。

## 7. 主要依据
- `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
- `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`
- `docs/products/miniapp/2026-03-10-miniapp-doc-completion-final-review-v1.md`
- `docs/products/miniapp/2026-03-12-miniapp-technician-commission-admin-page-truth-review-v1.md`
- `docs/products/miniapp/2026-03-14-miniapp-finance-ops-admin-doc-closure-review-v1.md`
- `docs/contracts/2026-03-14-miniapp-finance-ops-technician-commission-admin-contract-v1.md`
- `docs/products/miniapp/2026-03-14-miniapp-runtime-blocker-final-integration-v1.md`
- `yudao-mall-uniapp/pages.json`
- `yudao-mall-uniapp/sheep/api/`
- `ruoyi-vue-pro-master/**/controller/app/`
- `ruoyi-vue-pro-master/**/controller/admin/`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/*`
- `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/*`
