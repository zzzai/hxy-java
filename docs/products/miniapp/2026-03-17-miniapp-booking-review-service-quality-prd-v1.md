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
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppBookingReviewController.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewController.java`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/index.vue`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/dashboard/index.vue`
- 当前产品结论：`Doc Closed / Can Develop / Cannot Release`。
- 当前不得写成：
  - 已放量能力
  - 商品评论 alias
  - 自动奖励 / 自动补偿能力
  - 已有可放量的店长即时通知系统

## 2. 能力边界

### 2.1 已落地范围
1. 用户端从 booking 订单列表、订单详情进入评价。
2. 用户端支持评价资格校验、评价提交、结果页、我的评价列表、评价详情回看。
3. 后台支持评价台账、详情、回复、跟进状态更新、汇总看板。
4. 后台差评记录已新增“店长待办”层，支持认领、首次处理、闭环三类后台动作。
5. 评价记录绑定 booking 订单、门店、技师、会员、服务商品维度。

### 2.2 当前未落地范围
1. 自动好评奖励。
2. 自动差评补偿。
3. 自动通知店长 / 技师组长 / 客服负责人的消息通道。
4. 公域评价展示、点赞、评论互动。
5. 独立 feature flag / rollout control / runtime sample pack。
6. 评价图片历史 / 详情回显的 release 级证据。
7. 当前虽已新增账号级通知工程骨架（routing truth / notify outbox / admin 观测 / `IN_APP` 占位派发），但不承诺 release-ready，不承诺真实模板已配置，也不承诺微信或短信。
8. 当前联系人快照仍只认 `contactName/contactMobile`；账号级通知只认 `storeId -> managerAdminUserId` 路由表，未绑定时必须进入 `BLOCKED_NO_OWNER`。

## 3. 页面与入口真值

| 页面 / 入口 | 当前真值 | 说明 | 当前状态 |
|---|---|---|---|
| 订单列表评价入口 | `/pages/booking/order-list` 中 `order.status === 4` 且 `reviewEligibility.eligible === true` 时显示 `去评价` | 通过 `loadReviewEligibility` 判断，不靠本地猜测 | 已落地 |
| 订单详情评价入口 | `/pages/booking/order-detail` 中 `state.order.status === 4` 且 `state.reviewEligibility.eligible === true` 时显示 `去评价` | 页面 `onShow` 会刷新资格 | 已落地 |
| 评价提交页 | `/pages/booking/review-add` | 实际 route 不是设计草案里的 `/pages/booking/review/add` | 已落地 |
| 评价详情页 | `/pages/booking/review-detail` | 从我的评价列表进入，展示评分、标签、图片、商家回复 | 已落地 |
| 评价结果页 | `/pages/booking/review-result` | 成功后允许返回订单详情或查看我的评价 | 已落地 |
| 我的评价页 | `/pages/booking/review-list` | 展示 summary + list，点击卡片进入 `review-detail` | 已落地 |
| 后台评价台账 | `/mall/booking/review` | 真实页面文件已存在于 overlay | 已落地 |
| 后台评价详情 | `/mall/booking/review/detail` | 支持回复、跟进状态更新、店长待办认领 / 首次处理 / 闭环 | 已落地 |
| 后台评价看板 | `/mall/booking/review/dashboard` | 展示总量、好中差评、待处理、紧急、已回复、店长待办 SLA 统计 | 已落地 |

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
- 页面当前没有“立即联系店长”按钮。
- 页面当前没有奖励承诺文案。
- 页面当前也没有“查看店长处理进度”或“店长账号已受理”的对外状态。

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
- 后端已返回但页面未展示：
  - `averageScore`
  - 更细的 service / technician / environment 评分

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
- 当前页面动作：
  - 返回我的评价
  - 查看订单详情
- 当前不展示：
  - `serviceOrderId`
  - `riskLevel / followStatus / auditStatus`
  - 其他后台恢复字段

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
  - `managerTodoStatus`
  - `managerSlaStatus`
  - `replyStatus`
  - `submitTime[]`
- 当前列表字段：
  - `id`
  - `bookingOrderId`
  - `storeId`
  - `technicianId`
  - `memberId`
  - `overallScore`
  - `reviewLevel`
  - `riskLevel`
  - `followStatus`
  - `managerContactName`
  - `managerContactMobile`
  - `managerTodoStatus`
  - `managerClaimDeadlineAt`
  - `managerFirstActionDeadlineAt`
  - `managerCloseDeadlineAt`
  - `replyStatus`
  - `content`
  - `submitTime`
  - `replyTime`
- 当前操作：
  - 详情
  - 跳转看板

### 5.2 详情页 `/mall/booking/review/detail`
- 当前展示字段：
  - 订单、门店、技师、会员、SPU、SKU、评分、标签、内容、图片、展示状态、跟进状态、回复状态
- 当前操作：
  - 回复评价
  - 更新跟进状态
  - 认领店长待办
  - 记录店长待办首次处理
  - 标记店长待办闭环
  - 刷新
  - 返回列表
- 当前权限：
  - `booking:review:query`
  - `booking:review:update`

### 5.3 看板页 `/mall/booking/review/dashboard`
- 当前卡片：
  - `totalCount`
  - `positiveCount`
  - `neutralCount`
  - `negativeCount`
  - `pendingFollowCount`
  - `urgentCount`
  - `repliedCount`
  - `managerTodoPendingCount`
  - `managerTodoClaimTimeoutCount`
  - `managerTodoFirstActionTimeoutCount`
  - `managerTodoCloseTimeoutCount`
  - `managerTodoClosedCount`
- 当前仅是统计与运营解读，不代表消息推送或 SLA 自动化已经上线。

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

### 6.3 运营恢复原则
1. 当前差评只进入人工恢复队列，不自动补偿。
2. 当前好评不触发自动奖励。
3. 当前差评已具备“提交即写通知意图 -> outbox -> 异步派发”的工程链路，但失败不会影响用户提交成功。
4. 当前“店长待办”联系人快照仍固定来自门店 `contactName/contactMobile`；账号级派发只认 `booking_review_manager_account_routing.managerAdminUserId`。
5. 当前后台登录操作人 `managerClaimedByUserId / managerLatestActionByUserId` 仅代表执行动作的人，不代表门店店长账号真值。
6. 当前 `BLOCKED_NO_OWNER` 是合法阻断态，不得误写成发送失败，更不得误写成已通知成功。

## 7. 用户可见结构态与恢复动作

| 场景 | 当前用户可见口径 | 当前恢复动作 |
|---|---|---|
| 订单不可评价 | `暂无可评价订单` / `当前订单不属于你，暂不可评价` / `服务未完成，暂不可评价` | 返回订单页，等待完成或切换正确账号 |
| 已评价 | `该订单已评价，可直接查看历史评价` | 跳转我的评价或保持当前页 |
| 提交失败 | `提交失败，请稍后重试` | 表单内容保留，不自动跳结果页 |
| 我的评价为空 | `暂无评价` | 无评价时只展示空态，不伪造成功样本 |
| 后台列表空 | `list=[]` / dashboard `0` | 只算合法空态，不算运营成功样本 |

## 8. 当前工程差距与 No-Go
1. `picUrls[]` 已接通提交链路，且 member 端 `review-detail` 已可回看；但历史 / 详情 / 后台运营回显样本仍未作为 release 证据闭环。
2. `serviceOrderId` 当前改为后端按 `payOrderId -> TradeServiceOrderApi.listTraceByPayOrderId` best-effort 回填；trace 未命中或异常时仍允许为 `null`，不能写成稳定强绑定。
3. 当前虽已具备 booking review 专属 notify outbox、admin 观测页与 `IN_APP` 占位派发 job，但仍缺真实模板配置、运行样本、发布门禁与放量证据。
4. 当前没有自动奖励、自动补偿、自动申诉闭环。
5. 当前没有专门的 booking review runtime release gate 与发布样本包。
6. 当前门店主数据只稳定提供 `contactName/contactMobile` 联系人快照；routing 表虽已存在，但其数据覆盖和发布证据未闭环，因此不能外推成账号级通知系统已正式上线。
7. 历史差评待办字段如果尚未初始化，当前只会在首次认领 / 首次处理 / 闭环写动作时补齐，不能误写成系统会在读链路自动修复全量历史记录。
8. 因此当前结论只能是：`Can Develop / Cannot Release`。

## 9. 单一真值引用
- 产品：`docs/products/miniapp/2026-03-17-miniapp-booking-review-service-quality-prd-v1.md`
- 字段：`docs/products/miniapp/2026-03-17-miniapp-booking-review-page-field-dictionary-v1.md`
- Contract：`docs/contracts/2026-03-17-miniapp-booking-review-contract-v1.md`
- ErrorCode / FailureMode：`docs/contracts/2026-03-17-miniapp-booking-review-errorcode-and-failure-mode-v1.md`
- Runbook：`docs/plans/2026-03-17-miniapp-booking-review-service-recovery-runbook-v1.md`
- Release Gate：`docs/plans/2026-03-17-miniapp-booking-review-release-gate-v1.md`
- Final Integration：`docs/products/miniapp/2026-03-17-miniapp-booking-review-final-integration-review-v1.md`
- 历史边界审计：`docs/products/miniapp/2026-03-19-miniapp-booking-review-history-and-boundary-audit-v1.md`
- 店长归属真值：`docs/products/miniapp/2026-03-19-miniapp-booking-review-manager-ownership-truth-review-v1.md`
