# MiniApp 页面 API 字段字典 v1（2026-03-09）

## 1. 文档目标
- 建立“页面 -> 路由 -> API -> 请求字段 -> 响应字段 -> Owner -> 验收口径”统一字典。
- 约束：字段口径以冻结契约为准；错误处理按错误码，不按 message 分支。

## 2. 字段字典（执行版）

| 页面 | 路由 | API | 请求字段（关键） | 响应字段（关键） | Owner | 验收口径 |
|---|---|---|---|---|---|---|
| 首页 | `/pages/home/index` | `GET /promotion/activity/home-blocks`（规划）、`GET /promotion/coupon-template/page`、`GET /promotion/point-activity/page`、`GET /product/spu/page` | `storeId`, `scene`, `memberId?`, `pageNo`, `pageSize`, `categoryId?` | `blocks[]`, `template[]`, `activity[]`, `list[]`, `degraded`, `degradeReason` | Product + Promotion | 首屏可用、活动真值一致、降级不白屏 |
| 项目页（服务目录） | `/pages/service/catalog`（规划，当前可由 `/pages/goods/list` 承接） | `GET /product/catalog/page`（规划）、`GET /product/spu/page`、`POST /booking/addon-intent/submit`（规划） | `storeId`, `categoryId`, `pageNo`, `pageSize`, `catalogVersion?`, `parentOrderId`, `skuId`, `quantity`, `clientToken` | `list[]:{spuId,skuId,price,stock,bookable,catalogVersion}`, `intentId`, `acceptedQuantity`, `status`, `degraded` | Product + Booking | 切类目价格库存一致；冲突可解释可恢复 |
| 预约列表 | `/pages/booking/list` | `GET /booking/order/list`, `GET /booking/order/list-by-status` | `status?`, `pageNo`, `pageSize` | `list[]:{id,orderNo,status,payPrice,payRefundId,refundTime}`, `total` | Booking | 列表筛选正确，空态可恢复 |
| 预约详情/排期 | `/pages/booking/order-detail`, `/pages/booking/schedule` | `GET /booking/order/get`, `GET /booking/slot/list-by-technician`, `POST /booking/order/create` | `id`, `technicianId`, `date`, `storeId`, `timeSlotId` | `order:{...}`, `slots[]:{id,startTime,endTime,status}`, `bookingOrderId`, `degraded` | Booking | 时段冲突 fail-close，改约路径明确 |
| 支付结果 | `/pages/pay/result` | `GET /trade/order/pay-result` | `orderId`, `sync?` | `orderId`, `payOrderId`, `payOrderStatus`, `payResultCode`, `degraded`, `degradeReason` | Trade + Pay | SUCCESS/WAITING/REFUNDED/CLOSED 可判定 |
| 售后申请 | `/pages/order/aftersale/apply` | `POST /trade/after-sale/create` | `orderItemId`, `way`, `type`, `refundPrice`, `applyReason` | `afterSaleId` | Trade | 提交成功、业务阻断、参数错误可区分 |
| 售后列表 | `/pages/order/aftersale/list` | `GET /trade/after-sale/page` | `statuses?`, `pageNo`, `pageSize` | `list[]:{id,status,refundPrice,refundLimitSource}`, `total` | Trade | 空列表不报错，筛选稳定 |
| 售后详情 | `/pages/order/aftersale/detail` | `GET /trade/after-sale/get` | `id` | `id`, `status`, `refundPrice`, `payRefundId`, `refundTime`, `refundLimitSource` | Trade | 详情可查，字段完整 |
| 退款进度 | `/pages/refund/progress` | `GET /trade/after-sale/refund-progress` | `afterSaleId?`, `orderId?` | `afterSaleStatus`, `payRefundStatus`, `progressCode`, `progressDesc`, `channelErrorCode`, `degraded` | Trade + Pay | pay缺失可回退售后状态 |
| 会员资产-钱包 | `/pages/user/wallet/money` | `GET /pay/wallet/get`, `GET /pay/wallet-transaction/page`, `GET /pay/wallet-transaction/get-summary` | `pageNo`, `pageSize`, `type`, `createTime[0]`, `createTime[1]` | `balance`, `list[]:{id,title,price,createTime}`, `totalIncome`, `totalExpense` | Member + Pay | 收支与余额口径一致 |
| 会员资产-积分 | `/pages/user/wallet/score` | `GET /member/point/record/page` | `pageNo`, `pageSize`, `addStatus?`, `createTime[0]`, `createTime[1]` | `list[]:{id,title,description,point,createTime}`, `total` | Member | 正负向流水展示正确 |
| 会员资产-优惠券 | `/pages/coupon/list` | `GET /promotion/coupon-template/page`, `GET /promotion/coupon/page`, `POST /promotion/coupon/take` | `pageNo`, `pageSize`, `status?`, `templateId` | `template[]`, `coupon[]`, `takeResult` | Promotion | 领取失败不假成功，状态更新一致 |
| 搜索页 | `/pages/index/search` | （入口页，触发商品检索 API） | `keyword` | `searchHistory[]`（本地），路由参数下发 | Product | 关键词透传到结果页，历史可维护 |
| 搜索结果页 | `/pages/search/index`（规划） / `/pages/goods/list`（现行） | `GET /product/search/page`（规划）/ `GET /product/spu/page`（现行） | `q|keyword`, `storeId?`, `pageNo`, `pageSize`, `sort?`, `categoryId?` | `list[]:{spuId|id,title|spuName,price,stock,score?}`, `suggestions[]?`, `degraded` | Product | 保留 query，可重试，不丢上下文 |
| 礼品卡模板与下单 | `/pages/gift-card/*` | `GET /promotion/gift-card/template/page`, `POST /promotion/gift-card/order/create`, `GET /promotion/gift-card/order/get` | `pageNo`, `pageSize`, `status?`, `templateId`, `quantity`, `sendScene`, `receiverMemberId?`, `clientToken`, `orderId` | `list[]:{templateId,title,faceValue,stock,validDays}`, `orderId`, `giftCardBatchNo`, `amountTotal`, `cards[]`, `degraded` | Trade + Member | 全生命周期字段可追溯 |
| 礼品卡核销/退款 | `/pages/gift-card/*` | `POST /promotion/gift-card/redeem`, `POST /promotion/gift-card/refund/apply` | `cardNo`, `redeemCode`, `clientToken`, `orderId`, `reason`, `payRefundId?` | `redeemRecordId`, `cardStatus`, `afterSaleId`, `refundStatus`, `degraded`, `degradeReason` | Trade | 核销冲突与退款状态可解释 |
| 邀请总览与绑定 | `/pages/referral/*` | `POST /promotion/referral/bind-inviter`, `GET /promotion/referral/overview` | `inviteCode|inviterMemberId`, `clientToken`, `memberId?` | `refereeMemberId`, `inviterMemberId`, `bindStatus`, `idempotentHit`, `referralCode`, `totalInvites`, `effectiveInvites`, `pendingRewardAmount`, `rewardBalance`, `degraded` | Promotion | 绑定规则与到账状态清晰 |
| 邀请奖励台账 | `/pages/referral/*` | `GET /promotion/referral/reward-ledger/page`, `POST /promotion/referral/reward/settle`, `POST /promotion/referral/reward/retry` | `pageNo`, `pageSize`, `status?`, `runId`, `batchSize`, `dryRun`, `ids[]` | `list[]:{ledgerId,orderId,sourceBizNo,rewardAmount,status,runId,payRefundId}`, `successCount`, `skipCount`, `failCount`, `retrySuccess`, `retryFail`, `degraded` | Promotion + Member | 台账与结算结果一致，失败可补偿 |
| 技师动态列表 | `/pages/technician/feed` | `GET /booking/technician/feed/page` | `storeId`, `pageNo`, `pageSize`, `lastId?`, `technicianId?` | `list[]:{postId,technicianId,content,media[],likeCount,commentCount,publishTime,degraded}`, `hasMore` | Booking + Content Ops | 浏览可用，审核异常可降级 |
| 技师动态互动 | `/pages/technician/feed` | `POST /booking/technician/feed/like`, `POST /booking/technician/feed/comment/create` | `postId`, `action`, `clientToken`, `content` | `postId`, `liked`, `likeCount`, `commentId`, `status`, `idempotentHit`, `degraded` | Booking + Content Ops | 同键同参幂等、同键异参冲突可判定 |

## 3. 执行约束
- 字段新增只允许向后兼容，不允许重命名/删字段。
- 对 `RESERVED_DISABLED` 错误码场景，默认禁用；若线上返回按 P1 配置异常处理。
- 验收口径统一：每行至少覆盖 happy/error/degraded 三条路径。
