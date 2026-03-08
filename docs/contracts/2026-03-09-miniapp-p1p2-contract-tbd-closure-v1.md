# MiniApp P1/P2 Contract Closure v1 (2026-03-09)

## 1. 目标与对齐
- 目标：完成 P1/P2 场景契约补齐，消除占位错误码。
- 对齐文档：
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
  - `docs/plans/2026-03-08-miniapp-degrade-retry-playbook-v1.md`

## 2. 场景逐项补齐（P1/P2）

| 场景 | 路由/API | 请求字段（关键） | 响应字段（关键） | 错误码 | 降级语义 | 向后兼容说明 |
|---|---|---|---|---|---|---|
| 首页运营位 | `GET /promotion/activity/home-blocks`（规划） | `storeId`, `scene`, `memberId?`, `version?` | `blocks[]:{id,type,title,spuId?,couponTemplateId?,priority}`, `degraded`, `degradeReason` | `POINT_ACTIVITY_NOT_EXISTS(1013007000)`, `CATEGORY_NOT_EXISTS(1008001000)`, `MINIAPP_HOME_ACTIVITY_CONTEXT_MISMATCH(1008009901, RESERVED_DISABLED)` | 活动真值不一致时可 fail-open：主 CTA 保留，异常卡片降级隐藏并打 warning | 新字段仅增量：`degraded/degradeReason` 可选，旧端忽略不崩溃 |
| 项目页同源刷新 | `GET /product/catalog/page`（规划） | `storeId`, `categoryId`, `pageNo`, `pageSize`, `catalogVersion?` | `list[]:{spuId,skuId,price,stock,bookable,catalogVersion}`, `degraded` | `CATEGORY_NOT_EXISTS(1008001000)`, `SKU_NOT_EXISTS(1008006000)`, `MINIAPP_CATALOG_VERSION_MISMATCH(1008009902, RESERVED_DISABLED)` | 版本不一致时 fail-open：前端强制全量刷新，保留已选筛选条件 | 新增 `catalogVersion` 为可选字段；未下发时沿用旧渲染逻辑 |
| 加购冲突处理 | `POST /booking/addon-intent/submit`（规划） | `parentOrderId`, `skuId`, `quantity`, `clientToken`, `storeId` | `intentId`, `acceptedQuantity`, `status`, `degraded` | `SKU_STOCK_NOT_ENOUGH(1008006004)`, `STORE_SKU_STOCK_BIZ_KEY_CONFLICT(1008009006)`, `MINIAPP_ADDON_INTENT_CONFLICT(1008009903, RESERVED_DISABLED)` | 库存冲突 fail-close；回显服务端可购数量，不自动成功 | 返回体增量字段 `acceptedQuantity` 可缺省；旧端按失败提示处理 |
| 预约排期 | `GET /booking/slot/list-by-technician`（已有） + `POST /booking/order/create`（已有） | `technicianId`, `date`, `storeId`, `timeSlotId` | `slots[]:{id,startTime,endTime,status}`, `bookingOrderId?`, `degraded` | `SCHEDULE_CONFLICT(1030002001)`, `TIME_SLOT_NOT_AVAILABLE(1030003001)`, `TIME_SLOT_ALREADY_BOOKED(1030003002)` | 时段冲突 fail-close；仅在下游统计异常时 fail-open（不影响下单事实） | 不改现有字段语义，仅允许增加 `degraded` 可选标记 |
| 会员资产账本 | `GET /member/asset-ledger/page`（规划） | `memberId`, `pageNo`, `pageSize`, `assetType?` | `list[]:{ledgerId,assetType,bizType,amount,balanceAfter,sourceBizNo,runId}`, `degraded` | `USER_NOT_EXISTS(1004001000)`, `COUPON_NOT_EXISTS(1013005000)`, `POINT_RECORD_BIZ_NOT_SUPPORT(1004008000)`, `MINIAPP_ASSET_LEDGER_MISMATCH(1004009901, RESERVED_DISABLED)` | 下游汇总异常 fail-open：展示局部降级标签，不隐藏已确认账目 | 新增账本字段为增量，不改变既有优惠券/积分接口 |
| 搜索发现 | `GET /product/search/page`（规划） | `q`, `storeId?`, `pageNo`, `pageSize`, `sort?` | `list[]:{spuId,skuId,title,price,stock,score}`, `suggestions[]`, `degraded` | `CATEGORY_NOT_EXISTS(1008001000)`, `MINIAPP_SEARCH_QUERY_INVALID(1008009904, RESERVED_DISABLED)` | 搜索依赖异常 fail-open：保留 query，返回空结果 + 建议词 | 旧端仅消费 `list[]` 时保持兼容；`suggestions` 为可选 |
| 礼品卡域 | `POST /promotion/gift-card/order/create`、`POST /promotion/gift-card/redeem`（规划） | `templateId/orderId/cardNo/redeemCode/clientToken/memberId` | `orderId`, `giftCardBatchNo`, `cardStatus`, `afterSaleId?`, `degraded` | `ORDER_NOT_FOUND(1011000011)`, `COUPON_TEMPLATE_NOT_EXISTS(1013004000)`, `GIFT_CARD_ORDER_NOT_FOUND(1011009901, RESERVED_DISABLED)`, `GIFT_CARD_REDEEM_CONFLICT(1011009902, RESERVED_DISABLED)` | 发卡通知失败可 fail-open：订单成功 + `ISSUE_PENDING`；核销冲突 fail-close | 新接口增量上线，旧端未接入不受影响 |
| 邀请有礼 | `POST /promotion/referral/bind-inviter`、`GET /promotion/referral/reward-ledger/page`（规划） | `inviteCode/inviterMemberId/refereeMemberId/clientToken/pageNo/pageSize` | `bindStatus`, `rewardLedger[]:{ledgerId,orderId,rewardAmount,status,sourceBizNo,runId}`, `degraded` | `USER_NOT_EXISTS(1004001000)`, `ORDER_NOT_FOUND(1011000011)`, `REFERRAL_BIND_CONFLICT(1013009901, RESERVED_DISABLED)`, `REFERRAL_REWARD_LEDGER_MISMATCH(1013009902, RESERVED_DISABLED)` | 奖励到账延迟 fail-open：展示 `PROCESSING`，不伪成功到账 | 绑定与账本接口为新增，不改既有下单与会员接口 |
| 技师 feed | `GET /booking/technician/feed/page`、`POST /booking/technician/feed/like`（规划） | `storeId`, `technicianId?`, `postId`, `action`, `clientToken`, `pageNo/pageSize` | `posts[]:{postId,technicianId,content,media[],likeCount,commentCount,publishTime}`, `liked`, `degraded` | `TECHNICIAN_NOT_EXISTS(1030001000)`, `TECHNICIAN_DISABLED(1030001001)`, `TECHNICIAN_FEED_AUDIT_BLOCKED(1030009901, RESERVED_DISABLED)` | 审核系统异常可 fail-open：浏览主链路可用，互动能力降级 | feed 字段新增可选；旧端可仅消费技师基础信息 |

## 3. 去占位符收口映射
- 首页活动上下文异常 -> `MINIAPP_HOME_ACTIVITY_CONTEXT_MISMATCH(1008009901, RESERVED_DISABLED)`
- 项目页版本不一致 -> `MINIAPP_CATALOG_VERSION_MISMATCH(1008009902, RESERVED_DISABLED)`
- 加购冲突占位 -> `MINIAPP_ADDON_INTENT_CONFLICT(1008009903, RESERVED_DISABLED)`
- 资产账本不一致 -> `MINIAPP_ASSET_LEDGER_MISMATCH(1004009901, RESERVED_DISABLED)`
- 搜索入参非法 -> `MINIAPP_SEARCH_QUERY_INVALID(1008009904, RESERVED_DISABLED)`
- 礼品卡错误码占位 -> `GIFT_CARD_ORDER_NOT_FOUND(1011009901, RESERVED_DISABLED)` / `GIFT_CARD_REDEEM_CONFLICT(1011009902, RESERVED_DISABLED)`
- 邀请错误码占位 -> `REFERRAL_BIND_CONFLICT(1013009901, RESERVED_DISABLED)` / `REFERRAL_REWARD_LEDGER_MISMATCH(1013009902, RESERVED_DISABLED)`
- 技师 feed 审核阻断占位 -> `TECHNICIAN_FEED_AUDIT_BLOCKED(1030009901, RESERVED_DISABLED)`

## 4. 通用兼容策略
- 仅允许响应字段“向后兼容新增”，禁止删除/重命名已冻结字段。
- `RESERVED_DISABLED` 错误码未生效前不得在生产返回；如误返回按配置异常处理。
- 冲突码默认 fail-close；下游依赖异常默认 fail-open + `degraded` 标记 + 可检索审计。
