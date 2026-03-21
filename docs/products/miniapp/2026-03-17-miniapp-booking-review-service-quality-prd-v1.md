# MiniApp Booking Review Service Quality PRD v1（2026-03-17）

## 1. 目标与当前结论
- 目标：把“预约服务评价”收口为 booking 域内的独立服务质量反馈能力，而不是复用商品评论真值。
- 当前代码真值来源：
  - `yudao-mall-uniapp/pages/booking/order-list.vue`
  - `yudao-mall-uniapp/pages/booking/order-detail.vue`
  - `yudao-mall-uniapp/pages/booking/review-add.vue`
  - `yudao-mall-uniapp/pages/booking/review-detail.vue`
  - `yudao-mall-uniapp/pages/booking/review-result.vue`
  - `yudao-mall-uniapp/pages/booking/review-list.vue`
  - `yudao-mall-uniapp/sheep/api/trade/review.js`
  - `AppBookingReviewController`
  - `BookingReviewController`
  - `BookingReviewNotifyOutboxController`
  - `BookingReviewManagerAccountRoutingController`
  - `BookingReviewNotifyDispatchJob`
  - `BookingReviewManagerTodoSlaReminderJob`
  - overlay 中的 `review/index.vue`、`review/detail/index.vue`、`review/notifyOutbox/index.vue`、`review/managerRouting/index.vue`
- 当前产品结论：`Doc Closed / Can Develop / Cannot Release`。
- 当前不得写成：
  - 已放量能力
  - 商品评论 alias
  - 自动奖励 / 自动补偿能力
  - 已有可放量的店长通知系统

## 2. 能力边界

### 2.1 已落地范围
1. 用户端从 booking 订单列表、订单详情进入评价。
2. 用户端支持评价资格校验、评价提交、结果页、我的评价列表、评价详情回看。
3. 后台支持评价台账、详情、回复、跟进状态更新、汇总看板。
4. 后台差评记录已新增“店长待办”层，支持认领、首次处理、闭环三类后台动作。
5. 评价记录绑定 booking 订单、门店、技师、会员、服务商品维度。
6. 差评当前已补齐店长通知工程骨架：
   - `storeId -> managerAdminUserId + managerWecomUserId`
   - 同一条差评生成 `IN_APP / WECOM` 两条 outbox
   - `FAILED` 人工重试
   - 认领超时 / 首次处理超时 / 闭环超时提醒继续复用同一套 outbox

### 2.2 当前未落地范围
1. 自动好评奖励。
2. 自动差评补偿。
3. 区域负责人、客服负责人、技师负责人自动升级链路。
4. 公域评价展示、点赞、评论互动。
5. 独立 feature flag / rollout control / runtime sample pack。
6. 评价图片历史 / 详情回显的 release 级证据。
7. 当前虽已新增账号级通知工程实现，但不承诺 release-ready，不承诺真实模板与真实送达已验证。
8. 当前联系人快照仍只认 `contactName/contactMobile`；账号级通知只认 `managerAdminUserId / managerWecomUserId`，未绑定时必须进入 `BLOCKED_NO_OWNER`。

## 3. 页面与入口真值

| 页面 / 入口 | 当前真值 | 说明 | 当前状态 |
|---|---|---|---|
| 订单列表评价入口 | `/pages/booking/order-list` 中 `order.status === 4` 且 `reviewEligibility.eligible === true` 时显示 `去评价` | 通过 `loadReviewEligibility` 判断，不靠本地猜测 | 已落地 |
| 订单详情评价入口 | `/pages/booking/order-detail` 中 `state.order.status === 4` 且 `state.reviewEligibility.eligible === true` 时显示 `去评价` | 页面 `onShow` 会刷新资格 | 已落地 |
| 评价提交页 | `/pages/booking/review-add` | 实际 route 不是设计草案里的 `/pages/booking/review/add` | 已落地 |
| 评价详情页 | `/pages/booking/review-detail` | 从我的评价列表进入，展示评分、标签、图片、商家回复 | 已落地 |
| 评价结果页 | `/pages/booking/review-result` | 成功后允许返回订单详情或查看我的评价 | 已落地 |
| 我的评价页 | `/pages/booking/review-list` | 展示 summary + list，点击卡片进入 `review-detail` | 已落地 |
| 后台评价台账 | `/mall/booking/review` | 支持风险、SLA、店长待办快捷筛选与快捷动作 | 已落地 |
| 后台评价详情 | `/mall/booking/review/detail` | 支持回复、跟进状态更新、店长待办动作与通知观测块 | 已落地 |
| 后台通知台账 | `/mall/booking/review/notify-outbox` | 支持查看 App / 企微双通道出站记录、诊断与重试 | 已落地 |
| 后台店长路由页 | `/mall/booking/review/manager-routing` | 只读核查 App / 企微账号路由 | 已落地 |
| 后台评价看板 | `/mall/booking/review/dashboard` | 展示总量、风险、回复、店长待办 SLA 统计 | 已落地 |

## 4. 用户端页面 PRD

### 4.1 `/pages/booking/review-add`

#### 页面目标
- 让用户在完成服务后快速给出服务质量反馈。
- 差评优先沉淀为服务恢复输入，而不是内容资产。

#### 当前页面字段
- 已实现输入：
  - `overallScore`
  - `serviceScore`
  - `technicianScore`
  - `environmentScore`
  - `tags[]`
  - `content`
  - `picUrls[]`
  - `anonymous`
- 当前固定来源：
  - `source = order_detail`

#### 当前页面规则
1. 默认四个评分都初始化为 `5`。
2. 标签最多选择 `8` 个。
3. 当前页面内置固定标签 6 个：
   - `服务专业`
   - `沟通清晰`
   - `环境整洁`
   - `响应及时`
   - `体验一般`
   - `需要改进`
4. 文本输入框当前 UI 限制 `300` 字，但后端 VO 允许 `1024` 字；两者还未完全对齐。
5. 页面接受 `bookingOrderId`，并兼容从 `orderId` / `id` 回退读取订单 ID。

#### 不可误写的点
- 页面当前图片只保证提交链路，不能外推成历史 / 详情图片回显已经闭环。
- 页面当前没有补偿申请。
- 页面当前没有奖励承诺文案。
- 页面当前没有向用户展示“店长已接收通知”的对外状态。

### 4.2 `/pages/booking/review-result`
- 成功态：显示 `提交成功`，可跳转“查看我的评价”或“返回订单详情”。
- 失败态：显示 `提交未完成`，可返回订单详情后重试。
- 当前输入参数：`reviewId`、`bookingOrderId`。

### 4.3 `/pages/booking/review-list`
- 顶部 summary：
  - `totalCount`
  - `positiveCount`
  - `neutralCount`
  - `negativeCount`
- Tab 过滤：
  - 全部
  - 好评（`reviewLevel=1`）
  - 中评（`reviewLevel=2`）
  - 差评（`reviewLevel=3`）
- 列表卡片当前稳定展示字段：
  - `id`
  - `bookingOrderId`
  - `overallScore`
  - `content`
  - `replyContent`
  - `submitTime`
- 当前列表动作：
  - 点击卡片进入 `/pages/booking/review-detail?id=<reviewId>`

### 4.4 `/pages/booking/review-detail`
- 当前入口：
  - 只从 `/pages/booking/review-list` 进入
- 当前展示字段：
  - `reviewLevel`
  - `submitTime`
  - `bookingOrderId`
  - `overallScore`
  - `serviceScore`
  - `technicianScore`
  - `environmentScore`
  - `tags`
  - `content`
  - `picUrls`
  - `replyContent`
- 当前不展示：
  - `serviceOrderId`
  - `riskLevel / followStatus / auditStatus`
  - 后台恢复字段

## 5. 后台恢复台 PRD

### 5.1 台账页 `/mall/booking/review`
- 当前筛选字段：
  - `id`
  - `bookingOrderId`
  - `storeId`
  - `technicianId`
  - `memberId`
  - `reviewLevel`
  - `riskLevel`
  - `followStatus`
  - `onlyManagerTodo`
  - `onlyPendingInit`
  - `managerTodoStatus`
  - `managerSlaStatus`
  - `replyStatus`
  - `submitTime[]`
- 当前快捷视图：
  - `待认领优先`
  - `认领超时提醒`
  - `首次处理超时提醒`
  - `闭环超时提醒`
  - `历史待初始化`
- 当前操作：
  - 详情
  - 快速认领
  - 记录首次处理
  - 标记闭环

### 5.2 详情页 `/mall/booking/review/detail`
- 当前展示字段：
  - 订单、门店、技师、会员、SPU、SKU、评分、标签、内容、图片、展示状态、跟进状态、回复状态
  - 店长待办字段
  - 通知观测字段
- 当前操作：
  - 回复评价
  - 更新跟进状态
  - 认领店长待办
  - 记录店长待办首次处理
  - 标记店长待办闭环
  - 查看店长路由

### 5.3 通知台账 `/mall/booking/review/notify-outbox`
- 当前筛选字段：
  - `reviewId`
  - `storeId`
  - `receiverUserId`
  - `receiverAccount`
  - `status`
  - `channel`
  - `lastActionCode`
- 当前列表字段：
  - `reviewId`
  - `storeId`
  - `receiverRole`
  - `receiverUserId`
  - `receiverAccount`
  - `notifyType`
  - `channel`
  - `status`
  - `diagnosticLabel`
  - `repairHint`
  - `actionLabel`
  - `actionOperatorLabel`
  - `actionReason`
- 当前允许动作：
  - `FAILED` 人工重试
  - 查看评价
  - 查看店长路由

### 5.4 店长路由页 `/mall/booking/review/manager-routing`
- 当前展示字段：
  - `storeId`
  - `storeName`
  - `contactName`
  - `contactMobile`
  - `managerAdminUserId`
  - `managerWecomUserId`
  - `bindingStatus`
  - `effectiveTime / expireTime`
  - `appRoutingLabel / wecomRoutingLabel`
  - `repairHint`
- 当前页面性质：
  - 只读核查页
  - 不提供在线改绑

## 6. 业务规则真值

### 6.1 资格与重复评价
1. 只有已完成预约订单可评价。
2. 只允许订单所属会员评价自己的订单。
3. 一个 booking 订单只允许一条主评价记录。
4. 已评价订单再次进入时，资格接口返回 `eligible=false`、`alreadyReviewed=true`。

### 6.2 评分派生规则
| 规则 | 当前实现 |
|---|---|
| `overallScore <= 2` | `reviewLevel=差评`，`riskLevel=紧急`，`displayStatus=review_pending`，`followStatus=待跟进`，`auditStatus=manual_review` |
| `overallScore == 3` | `reviewLevel=中评`，`riskLevel=关注` |
| `overallScore >= 4` | `reviewLevel=好评`，`riskLevel=正常` |

### 6.3 通知与恢复原则
1. 差评创建后立即生成通知意图，不等于立即送达成功。
2. 同一条差评当前拆成 `IN_APP / WECOM` 两条独立 outbox。
3. 缺 App 账号只阻断 App，缺企微账号只阻断企微；任一通道失败不影响评价主链路成功。
4. `FAILED` 可人工重试，`BLOCKED_NO_OWNER` 必须先修路由或通道，再谈补发。
5. 当前好评不触发自动奖励，差评不触发自动补偿。

## 7. 当前阶段结论
1. booking review 当前已经完成“用户评价 + admin 恢复 + 双通道通知工程实现”的第一轮闭环。
2. 但这仍然不是 release-ready 结论。
3. 当前唯一正确口径仍是：`Doc Closed / Can Develop / Cannot Release`。
