# MiniApp Booking Review Contract v1（2026-03-17）

## 1. 目标与适用范围
- 目标：固定 booking review 当前真实 canonical route、frontend API、backend controller、admin overlay 绑定关系。
- 适用范围：
  - miniapp 用户端评价入口、提交、结果、列表
  - booking review app controller
  - booking review admin controller 与 overlay 页面
- 当前结论：`Doc Closed / Can Develop / Cannot Release`。

## 2. 当前单一真值来源
- 前端：
  - `yudao-mall-uniapp/pages.json`
  - `yudao-mall-uniapp/pages/booking/order-list.vue`
  - `yudao-mall-uniapp/pages/booking/order-detail.vue`
  - `yudao-mall-uniapp/pages/booking/review-add.vue`
  - `yudao-mall-uniapp/pages/booking/review-result.vue`
  - `yudao-mall-uniapp/pages/booking/review-list.vue`
  - `yudao-mall-uniapp/pages/booking/logic.js`
  - `yudao-mall-uniapp/sheep/api/trade/review.js`
- 后端：
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppBookingReviewController.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/admin/BookingReviewController.java`
- 后台 overlay：
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/booking/review.ts`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/index.vue`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/detail/index.vue`
  - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/booking/review/dashboard/index.vue`

## 3. Route 真值

### 3.1 用户侧 route
| 能力 | 当前 route 真值 | 说明 |
|---|---|---|
| 评价提交页 | `/pages/booking/review-add` | 不是 `/pages/booking/review/add` |
| 评价结果页 | `/pages/booking/review-result` | 成功 / 失败结果页 |
| 我的评价页 | `/pages/booking/review-list` | 列表 + summary |
| 订单列表入口 | `/pages/booking/order-list` | `去评价` CTA 来源页 |
| 订单详情入口 | `/pages/booking/order-detail` | `去评价` CTA 来源页 |

### 3.2 后台 overlay route
| 能力 | 当前 route 真值 | 说明 |
|---|---|---|
| 评价台账 | `/mall/booking/review` | 页面文件已存在 |
| 评价详情 | `/mall/booking/review/detail?id=` | 通过 query `id` 打开 |
| 评价看板 | `/mall/booking/review/dashboard` | 汇总页 |

## 4. 用户端 canonical API

| 能力 | method + path | 前端调用点 | 后端 controller | 当前说明 |
|---|---|---|---|---|
| 评价资格校验 | `GET /booking/review/eligibility` | `pages/booking/logic.js -> loadReviewEligibility`; `pages/booking/review-add.vue -> loadEligibility` | `AppBookingReviewController#getEligibility` | `bookingOrderId` 必填 |
| 创建评价 | `POST /booking/review/create` | `pages/booking/review-add.vue -> onSubmit` | `AppBookingReviewController#createReview` | 当前页面固定 `source=order_detail` |
| 我的评价分页 | `GET /booking/review/page` | `pages/booking/review-list.vue -> getList` | `AppBookingReviewController#getReviewPage` | 合法空态 `list=[]` |
| 我的评价详情 | `GET /booking/review/get` | `sheep/api/trade/review.js` 已导出，当前 miniapp 页面 `未核出已消费` | `AppBookingReviewController#getReview` | 当前请求参数只认 `id` |
| 我的评价汇总 | `GET /booking/review/summary` | `pages/booking/review-list.vue -> loadSummary` | `AppBookingReviewController#getSummary` | 合法空态为全 0 |

## 5. 后台 canonical API

| 能力 | method + path | 前端调用点 | 后端 controller | 权限 |
|---|---|---|---|---|
| 评价台账分页 | `GET /booking/review/page` | `overlay review/index.vue -> getList` | `BookingReviewController#page` | `booking:review:query` |
| 评价详情 | `GET /booking/review/get` | `overlay review/detail/index.vue -> loadDetail` | `BookingReviewController#get` | `booking:review:query` |
| 回复评价 | `POST /booking/review/reply` | `overlay review/detail/index.vue -> submitReply` | `BookingReviewController#reply` | `booking:review:update` |
| 更新跟进状态 | `POST /booking/review/follow-status` | `overlay review/detail/index.vue -> submitFollowStatus` | `BookingReviewController#updateFollowStatus` | `booking:review:update` |
| 看板汇总 | `GET /booking/review/dashboard-summary` | `overlay review/dashboard/index.vue -> getSummary` | `BookingReviewController#dashboardSummary` | `booking:review:query` |

## 6. 页面到接口绑定真值

### 6.1 用户侧绑定
| 页面 | 当前绑定 | 说明 |
|---|---|---|
| `/pages/booking/order-list` | 只在已完成订单上调用 `GET /booking/review/eligibility` 决定是否显示 CTA | 当前并不直接提交评价 |
| `/pages/booking/order-detail` | 只在已完成订单上调用 `GET /booking/review/eligibility` 决定是否显示 CTA | 当前并不直接提交评价 |
| `/pages/booking/review-add` | 进入时先读 `eligibility`，提交时调用 `create` | 提交成功才跳结果页 |
| `/pages/booking/review-result` | 不发 API | 纯结果承接页 |
| `/pages/booking/review-list` | 调 `summary + page` | 当前不消费 `getReview` |

### 6.2 后台绑定
| 页面 | 当前绑定 | 说明 |
|---|---|---|
| `/mall/booking/review` | `page` | 当前只做列表、筛选、跳详情、跳看板 |
| `/mall/booking/review/detail` | `get + reply + follow-status` | 当前没有删除、审核、隐藏接口 |
| `/mall/booking/review/dashboard` | `dashboard-summary` | 当前只有统计卡片，没有明细 drill-down |

## 7. 当前 mismatch / gap
1. 设计草案中的 route 结构是 `/pages/booking/review/add`，当前实际代码使用 `/pages/booking/review-add`。
2. `getReview` 已在前端 API 导出，但当前 miniapp 页面没有真实消费点。
3. `AppBookingReviewCreateReqVO` 支持 `picUrls`，当前 miniapp 提交页已通过 `s-uploader` 发送该字段；图片历史/详情回显能力仍需按页面真值单独核定。
4. `serviceOrderId` 在后端 DO/VO 中存在，当前创建逻辑会按 `payOrderId -> TradeServiceOrderApi.listTraceByPayOrderId` 做 best-effort 回填；trace 未命中或异常时仍允许写 `null`。
5. 当前没有独立 booking review runtime gate，也没有 release sample pack。
6. 当前没有服务端 `degraded=true / degradeReason` 字段证据。

## 8. Contract 级 No-Go
1. 不得把 booking review 写成商品评论能力的子别名。
2. 不得把 `GET /booking/review/get` 的请求参数改写成 `reviewId`。
3. 不得把后台 overlay 文件存在外推成“已具备生产放量证据”。
4. 不得把 `code=0` 的写接口样本直接外推成 release-ready。
5. 不得把 `serviceOrderId` 的 best-effort 回填写成稳定强绑定，也不得补造 `degraded=true / degradeReason`。
