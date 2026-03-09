# MiniApp Release API Canonical List v1 (2026-03-09)

## 1. 目标
- 将页面能力与后端 API 建立一一映射，形成可执行门禁清单。
- 禁止使用通配路径；每条能力必须绑定明确 `method + path`。
- 适用范围：P0（已上线）+ P1/P2（规划/预留）。

## 2. 页面能力 -> API Canonical List

| 页面 | 能力 | method | path | 请求关键字段 | 响应关键字段 | 错误码 | 降级语义 | 状态 |
|---|---|---|---|---|---|---|---|---|
| `/pages/pay/result` | 支付结果聚合查询 | GET | `/trade/order/pay-result` | `orderId`, `sync?` | `orderId`, `payOrderId`, `payResultCode`, `degraded`, `degradeReason` | `ORDER_NOT_FOUND(1011000011)`, `PAY_ORDER_NOT_FOUND` | pay 单缺失时 `degraded=true`，显示 `WAITING+warning` | ACTIVE |
| `/pages/order/list` | 订单分页 | GET | `/trade/order/page` | `status?`, `pageNo`, `pageSize` | `list[]`, `total` | `ORDER_NOT_FOUND(1011000011)` | 查询失败保活页面，保留筛选条件 | ACTIVE |
| `/pages/order/list` | 订单计数 | GET | `/trade/order/get-count` | 无 | `allCount`, `unpaidCount`, `afterSaleCount` | `ORDER_NOT_FOUND(1011000011)` | 计数查询失败不阻断列表渲染 | ACTIVE |
| `/pages/order/detail` | 订单详情 | GET | `/trade/order/get-detail` | `id`, `sync?` | `id`, `no`, `status`, `items[]` | `ORDER_NOT_FOUND(1011000011)` | sync 失败回退本地状态，不崩页 | ACTIVE |
| `/pages/after-sale/create` | 提交售后 | POST | `/trade/after-sale/create` | `orderItemId`, `type`, `refundPrice`, `applyReason` | `afterSaleId` | `AFTER_SALE_REFUND_FAIL_BUNDLE_CHILD_FULFILLED(1011000125)` | 业务冲突 fail-close，提示人工审核 | ACTIVE |
| `/pages/after-sale/list` | 售后分页 | GET | `/trade/after-sale/page` | `statuses?`, `pageNo`, `pageSize` | `list[]`, `total` | `AFTER_SALE_NOT_FOUND(1011000100)` | 空结果作为有效结果 | ACTIVE |
| `/pages/after-sale/detail` | 售后详情 | GET | `/trade/after-sale/get` | `id` | `id`, `status`, `refundPrice`, `payRefundId` | `AFTER_SALE_NOT_FOUND(1011000100)` | pay 退款信息缺失时保留售后主信息 | ACTIVE |
| `/pages/refund/progress` | 退款进度聚合 | GET | `/trade/after-sale/refund-progress` | `afterSaleId?`, `orderId?` | `progressCode`, `payRefundStatus`, `channelErrorCode` | `AFTER_SALE_NOT_FOUND(1011000100)` | pay 退款单缺失按售后状态回退进度 | ACTIVE |
| `/pages/booking/list` | 预约列表 | GET | `/booking/order/list` | 无 | `list[]:{id,orderNo,status,payPrice}` | `BOOKING_ORDER_NOT_EXISTS(1030004000)` | 查询失败保活列表页 | ACTIVE |
| `/pages/booking/list` | 预约按状态查询 | GET | `/booking/order/list-by-status` | `status` | `list[]:{id,orderNo,status,payPrice}` | `BOOKING_ORDER_NOT_EXISTS(1030004000)` | 状态为空回退全量列表 | ACTIVE |
| `/pages/address/list` | 地址列表 | GET | `/member/address/list` | 无 | `list[]:{id,name,mobile,defaultStatus}` | `ADDRESS_NOT_EXISTS(1004004000)` | 无地址返回空列表并引导新增 | ACTIVE |
| `/pages/address/list` | 新增地址 | POST | `/member/address/create` | `name`, `mobile`, `areaId`, `detailAddress` | `id` | `ADDRESS_NOT_EXISTS(1004004000)` | 参数异常 fail-close | ACTIVE |
| `/pages/address/list` | 更新地址 | PUT | `/member/address/update` | `id`, `name`, `mobile`, `areaId`, `detailAddress` | `true` | `ADDRESS_NOT_EXISTS(1004004000)` | 地址不存在直接阻断 | ACTIVE |
| `/pages/address/list` | 删除地址 | DELETE | `/member/address/delete` | `id` | `true` | `ADDRESS_NOT_EXISTS(1004004000)` | 删除失败不影响列表保活 | ACTIVE |
| `/pages/address/list` | 默认地址查询 | GET | `/member/address/get-default` | 无 | `id`, `name`, `mobile`, `detailAddress` | `ADDRESS_NOT_EXISTS(1004004000)` | 无默认地址返回 `null` | ACTIVE |
| `/pages/coupon/center` | 优惠券模板分页 | GET | `/promotion/coupon-template/page` | `pageNo`, `pageSize`, `spuId?`, `productScope?` | `list[]:{id,name,canTake}`, `total` | `COUPON_TEMPLATE_NOT_EXISTS(1013004000)` | 模板异常降级为空列表 | ACTIVE |
| `/pages/coupon/center` | 领取优惠券 | POST | `/promotion/coupon/take` | `templateId` | `canTakeAgain` | `COUPON_TEMPLATE_NOT_EXISTS(1013004000)` | 超时不播放成功动效，支持幂等重试 | ACTIVE |
| `/pages/point/mall` | 积分活动分页 | GET | `/promotion/point-activity/page` | `pageNo`, `pageSize` | `list[]:{id,point,price,spuId}`, `total` | `PRICE_CALCULATE_POINT_TOTAL_LIMIT_COUNT(1011003004)` | 活动异常降级为空态 | ACTIVE |
| `/pages/point/mall` | 积分活动详情 | GET | `/promotion/point-activity/get-detail` | `id` | `id`, `products[]`, `point`, `price` | `PRICE_CALCULATE_POINT_TOTAL_LIMIT_COUNT(1011003004)` | 详情异常回退列表 | ACTIVE |
| `/pages/point/mall` | 积分流水 | GET | `/member/point/record/page` | `pageNo`, `pageSize` | `list[]:{id,bizType,point,balance}`, `total` | `POINT_RECORD_BIZ_NOT_SUPPORT(1004008000)` | 流水异常不阻断活动页 | ACTIVE |
| `/pages/home/index` | 首页活动聚合 | GET | `/promotion/activity/home-blocks` | `storeId`, `scene`, `memberId?`, `version?` | `blocks[]`, `degraded`, `degradeReason` | `MINIAPP_HOME_ACTIVITY_CONTEXT_MISMATCH(1008009901, RESERVED_DISABLED)` | 上下文不一致时 fail-open：异常卡片隐藏 | PLANNED_RESERVED |
| `/pages/home/index` | 首页商品推荐 | GET | `/product/spu/page` | `categoryId?`, `pageNo`, `pageSize`, `sort?` | `list[]:{id,name,price,picUrl}`, `total` | `CATEGORY_NOT_EXISTS(1008001000)`, `SPU_NOT_EXISTS(1008005000)` | 分类异常回退默认分类 | ACTIVE |
| `/pages/service/catalog` | 类目树查询 | GET | `/product/category/list` | 无 | `list[]:{id,name,parentId,sort}` | `CATEGORY_NOT_EXISTS(1008001000)` | 类目异常回退首页推荐 | ACTIVE |
| `/pages/service/catalog` | 项目页目录分页（同源） | GET | `/product/catalog/page` | `storeId`, `categoryId`, `pageNo`, `pageSize`, `catalogVersion?` | `list[]:{spuId,skuId,price,stock,catalogVersion}`, `degraded` | `MINIAPP_CATALOG_VERSION_MISMATCH(1008009902, RESERVED_DISABLED)` | 版本不一致强制全量刷新 | PLANNED_RESERVED |
| `/pages/service/catalog` | 加购意图提交 | POST | `/booking/addon-intent/submit` | `parentOrderId`, `skuId`, `quantity`, `clientToken`, `storeId` | `intentId`, `acceptedQuantity`, `status` | `SKU_STOCK_NOT_ENOUGH(1008006004)`, `STORE_SKU_STOCK_BIZ_KEY_CONFLICT(1008009006)`, `MINIAPP_ADDON_INTENT_CONFLICT(1008009903, RESERVED_DISABLED)` | 冲突 fail-close，回显可购数量 | PLANNED_RESERVED |
| `/pages/booking/schedule` | 门店可用时段 | GET | `/booking/slot/list` | `storeId`, `date` | `list[]:{id,startTime,endTime,status}` | `TIME_SLOT_NOT_AVAILABLE(1030003001)` | 无可用时段返回空列表可改期 | ACTIVE |
| `/pages/booking/schedule` | 技师时段 | GET | `/booking/slot/list-by-technician` | `technicianId`, `date` | `list[]:{id,startTime,endTime,status}` | `SCHEDULE_CONFLICT(1030002001)`, `TIME_SLOT_ALREADY_BOOKED(1030003002)` | 冲突 fail-close，提示重选 | ACTIVE |
| `/pages/booking/schedule` | 创建预约订单 | POST | `/booking/order/create` | `timeSlotId`, `spuId`, `skuId`, `storeId`, `bookingDate`, `startTime` | `orderId` | `SCHEDULE_CONFLICT(1030002001)`, `TIME_SLOT_ALREADY_BOOKED(1030003002)` | 冲突直接阻断 | ACTIVE |
| `/pages/profile/assets` | 资产总账分页 | GET | `/member/asset-ledger/page` | `memberId`, `assetType?`, `pageNo`, `pageSize` | `list[]:{ledgerId,assetType,bizType,amount,balanceAfter,sourceBizNo,runId}`, `degraded` | `MINIAPP_ASSET_LEDGER_MISMATCH(1004009901, RESERVED_DISABLED)` | 汇总异常 fail-open，显示局部降级标签 | PLANNED_RESERVED |
| `/pages/profile/assets` | 优惠券资产分页 | GET | `/promotion/coupon/page` | `status?`, `pageNo`, `pageSize` | `list[]:{id,templateId,status,validEndTime}`, `total` | `COUPON_NOT_EXISTS(1013005000)` | 券资产失败不阻断其他资产 | ACTIVE |
| `/pages/profile/assets` | 积分资产流水 | GET | `/member/point/record/page` | `pageNo`, `pageSize` | `list[]:{id,point,bizType,balance}`, `total` | `POINT_RECORD_BIZ_NOT_SUPPORT(1004008000)` | 积分流水失败不阻断页面主框架 | ACTIVE |
| `/pages/search/index` | 搜索分页 | GET | `/product/search/page` | `q`, `storeId?`, `pageNo`, `pageSize`, `sort?` | `list[]:{spuId,skuId,title,price,stock,score}`, `suggestions[]`, `degraded` | `MINIAPP_SEARCH_QUERY_INVALID(1008009904, RESERVED_DISABLED)` | query 保留并提示修正 | PLANNED_RESERVED |
| `/pages/search/index` | 搜索结果降级兜底 | GET | `/product/spu/page` | `keyword?`, `pageNo`, `pageSize` | `list[]:{id,name,price}`, `total` | `CATEGORY_NOT_EXISTS(1008001000)` | 搜索服务异常时回退普通商品分页 | ACTIVE |
| `/pages/gift-card/list` | 礼品卡模板分页 | GET | `/promotion/gift-card/template/page` | `pageNo`, `pageSize`, `status?` | `list[]:{templateId,title,faceValue,stock,validDays}`, `total` | `COUPON_TEMPLATE_NOT_EXISTS(1013004000)` | 模板异常返回空列表 | PLANNED_RESERVED |
| `/pages/gift-card/order/create` | 礼品卡下单 | POST | `/promotion/gift-card/order/create` | `templateId`, `quantity`, `sendScene`, `receiverMemberId?`, `clientToken` | `orderId`, `giftCardBatchNo`, `amountTotal`, `degraded` | `GIFT_CARD_ORDER_NOT_FOUND(1011009901, RESERVED_DISABLED)` | 发卡失败 fail-open：订单成功+待补偿 | PLANNED_RESERVED |
| `/pages/gift-card/order/detail` | 礼品卡订单详情 | GET | `/promotion/gift-card/order/get` | `orderId` | `orderId`, `status`, `cards[]` | `GIFT_CARD_ORDER_NOT_FOUND(1011009901, RESERVED_DISABLED)` | 订单缺失返回 not-found 态 | PLANNED_RESERVED |
| `/pages/gift-card/redeem` | 礼品卡核销 | POST | `/promotion/gift-card/redeem` | `cardNo`, `redeemCode`, `clientToken` | `redeemRecordId`, `cardStatus`, `degraded` | `GIFT_CARD_REDEEM_CONFLICT(1011009902, RESERVED_DISABLED)` | 冲突 fail-close，不自动重试 | PLANNED_RESERVED |
| `/pages/gift-card/refund` | 礼品卡退款申请 | POST | `/promotion/gift-card/refund/apply` | `orderId`, `reason`, `payRefundId?`, `clientToken` | `afterSaleId`, `refundStatus`, `degraded` | `GIFT_CARD_REDEEM_CONFLICT(1011009902, RESERVED_DISABLED)` | 退款同步异常 fail-open，异步补偿 | PLANNED_RESERVED |
| `/pages/referral/bind` | 邀请关系绑定 | POST | `/promotion/referral/bind-inviter` | `inviteCode` 或 `inviterMemberId`, `clientToken` | `refereeMemberId`, `inviterMemberId`, `bindStatus` | `REFERRAL_BIND_CONFLICT(1013009901, RESERVED_DISABLED)` | 绑定冲突 fail-close | PLANNED_RESERVED |
| `/pages/referral/overview` | 邀请概览 | GET | `/promotion/referral/overview` | `memberId?` | `referralCode`, `totalInvites`, `rewardBalance`, `degraded` | `REFERRAL_REWARD_LEDGER_MISMATCH(1013009902, RESERVED_DISABLED)` | 账本延迟 fail-open，展示 PROCESSING | PLANNED_RESERVED |
| `/pages/referral/ledger` | 邀请奖励台账 | GET | `/promotion/referral/reward-ledger/page` | `pageNo`, `pageSize`, `status?` | `list[]:{ledgerId,orderId,rewardAmount,status,sourceBizNo,runId}`, `total` | `REFERRAL_REWARD_LEDGER_MISMATCH(1013009902, RESERVED_DISABLED)` | 对账异常不阻断页面主框架 | PLANNED_RESERVED |
| `/pages/technician/feed` | 技师列表 | GET | `/booking/technician/list` | `storeId` | `list[]:{id,name,avatar,level}` | `TECHNICIAN_NOT_EXISTS(1030001000)` | 列表异常降级为空态 | ACTIVE |
| `/pages/technician/feed` | 技师详情 | GET | `/booking/technician/get` | `id` | `id`, `name`, `avatar`, `intro`, `skillTags` | `TECHNICIAN_DISABLED(1030001001)` | 技师禁用时回退推荐其他技师 | ACTIVE |
| `/pages/technician/feed` | 动态分页 | GET | `/booking/technician/feed/page` | `storeId`, `technicianId?`, `pageNo`, `pageSize`, `lastId?` | `posts[]:{postId,technicianId,content,media[],likeCount,commentCount,publishTime}`, `degraded` | `TECHNICIAN_FEED_AUDIT_BLOCKED(1030009901, RESERVED_DISABLED)` | 审核异常 fail-open：浏览可用，互动降级 | PLANNED_RESERVED |
| `/pages/technician/feed` | 点赞/取消点赞 | POST | `/booking/technician/feed/like` | `postId`, `action`, `clientToken` | `postId`, `liked`, `likeCount`, `idempotentHit` | `TECHNICIAN_FEED_AUDIT_BLOCKED(1030009901, RESERVED_DISABLED)` | 审核阻断 fail-close | PLANNED_RESERVED |
| `/pages/technician/feed` | 发布评论 | POST | `/booking/technician/feed/comment/create` | `postId`, `content`, `clientToken` | `commentId`, `status`, `degraded` | `TECHNICIAN_FEED_AUDIT_BLOCKED(1030009901, RESERVED_DISABLED)` | 审核超时 fail-open：评论入队待审核 | PLANNED_RESERVED |

## 3. 门禁使用建议
- `ACTIVE` 接口：作为发布前必测必过清单。
- `PLANNED_RESERVED` 接口：必须配合 `RESERVED_DISABLED` 门禁开关，不可默认对外返回。
- 所有接口的审计日志必须可检索：`runId/orderId/payRefundId/sourceBizNo/errorCode`。
