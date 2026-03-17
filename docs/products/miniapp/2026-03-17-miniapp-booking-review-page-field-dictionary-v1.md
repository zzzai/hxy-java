# MiniApp Booking Review Page Field Dictionary v1（2026-03-17）

## 1. 目标
- 固定 booking review 当前真实页面、真实 route、真实 API、真实 admin overlay 的字段字典。
- 不按设计草案或产品想象扩写未落地字段。

## 2. 路由与页面字段

### 2.1 用户侧页面

| 页面 | 当前 route 真值 | 字段 / 参数 | 来源 | 当前说明 |
|---|---|---|---|---|
| 评价提交页 | `/pages/booking/review-add` | `bookingOrderId` | route query | 主参数 |
| 评价提交页 | `/pages/booking/review-add` | `orderId` | route query | 页面兼容回退读取 |
| 评价提交页 | `/pages/booking/review-add` | `id` | route query | 页面兼容回退读取 |
| 评价结果页 | `/pages/booking/review-result` | `reviewId` | route query | 成功后透传 |
| 评价结果页 | `/pages/booking/review-result` | `bookingOrderId` | route query | 返回订单详情使用 |
| 我的评价页 | `/pages/booking/review-list` | 无必填 route 参数 | N/A | 通过 summary + page API 自加载 |

### 2.2 用户侧提交页字段

| 字段 | 类型 | 当前 UI 是否承接 | 当前请求是否发送 | 当前说明 |
|---|---|---|---|---|
| `bookingOrderId` | number | 是 | 是 | 从 route 读取 |
| `overallScore` | number | 是 | 是 | 默认 `5` |
| `serviceScore` | number | 是 | 是 | 默认 `5` |
| `technicianScore` | number | 是 | 是 | 默认 `5` |
| `environmentScore` | number | 是 | 是 | 默认 `5` |
| `tags[]` | string[] | 是 | 是 | UI 限制最多 8 个 |
| `content` | string | 是 | 是 | UI 当前 `maxlength=300` |
| `anonymous` | boolean | 是 | 是 | 默认 `false` |
| `source` | string | 否（固定值） | 是 | 当前固定发送 `order_detail` |
| `picUrls[]` | string[] | 是 | 是 | 当前通过 `s-uploader` 上传成功回调写入并随提交发送，最多 9 张 |

### 2.3 用户侧结果页字段

| 字段 | 类型 | 来源 | 当前说明 |
|---|---|---|---|
| `reviewId` | number | route query | 决定成功/未完成文案 |
| `bookingOrderId` | number | route query | 返回订单详情使用 |

### 2.4 用户侧列表页字段

| 区域 | 字段 | 来源 | 当前说明 |
|---|---|---|---|
| summary | `totalCount` | `GET /booking/review/summary` | 已展示 |
| summary | `positiveCount` | `GET /booking/review/summary` | 已展示 |
| summary | `neutralCount` | `GET /booking/review/summary` | 已展示 |
| summary | `negativeCount` | `GET /booking/review/summary` | 已展示 |
| summary | `averageScore` | `GET /booking/review/summary` | 后端返回，当前页面未展示 |
| tabs | `reviewLevel` | 本地 tab value | `undefined / 1 / 2 / 3` |
| list item | `id` | `GET /booking/review/page` | 已展示 |
| list item | `bookingOrderId` | `GET /booking/review/page` | 已展示 |
| list item | `overallScore` | `GET /booking/review/page` | 已展示 |
| list item | `content` | `GET /booking/review/page` | 已展示 |
| list item | `replyContent` | `GET /booking/review/page` | 已展示 |
| list item | `submitTime` | `GET /booking/review/page` | 已展示 |
| list item | `serviceScore / technicianScore / environmentScore` | `GET /booking/review/page` | 当前页面未展示 |

## 3. API 字段字典

### 3.1 App API

#### `GET /booking/review/eligibility`

| 字段 | 方向 | 类型 | 当前说明 |
|---|---|---|---|
| `bookingOrderId` | request | number | 必填 |
| `bookingOrderId` | response | number | 回显 |
| `eligible` | response | boolean | 是否可评价 |
| `alreadyReviewed` | response | boolean | 是否已有评价 |
| `reviewId` | response | number | 已评价时返回 |
| `reason` | response | string | `ORDER_NOT_EXISTS / NOT_OWNER / ALREADY_REVIEWED / ORDER_NOT_COMPLETED` |

#### `POST /booking/review/create`

| 字段 | 方向 | 类型 | 当前说明 |
|---|---|---|---|
| `bookingOrderId` | request | number | 必填 |
| `overallScore` | request | number | 必填，1-5 |
| `serviceScore` | request | number | 可选，1-5 |
| `technicianScore` | request | number | 可选，1-5 |
| `environmentScore` | request | number | 可选，1-5 |
| `tags` | request | string[] | 最多 8 个 |
| `content` | request | string | 后端最多 1024；当前 UI 300 |
| `picUrls` | request | string[] | 后端最多 9 张；当前提交页已发送 |
| `anonymous` | request | boolean | 可选 |
| `source` | request | string | 当前固定 `order_detail` |
| `data` | response | number | 成功时返回 `reviewId` |

#### `GET /booking/review/page`

| 字段 | 方向 | 类型 | 当前说明 |
|---|---|---|---|
| `pageNo` | request | number | 必填 |
| `pageSize` | request | number | 必填 |
| `reviewLevel` | request | number | tab 过滤值 |
| `list[]` | response | object[] | 合法空态可为 `[]` |
| `total` | response | number | 合法空态可为 `0` |

#### `GET /booking/review/get`

| 字段 | 方向 | 类型 | 当前说明 |
|---|---|---|---|
| `id` | request | number | 当前只认 `id`，不是 `reviewId` |
| `serviceOrderId` | response | number | 当前由后端按 `payOrderId -> TradeServiceOrderApi.listTraceByPayOrderId` best-effort 回填；trace 未命中或异常时允许为 `null` |
| `storeId / technicianId / memberId` | response | number | 已返回 |
| `overallScore / serviceScore / technicianScore / environmentScore` | response | number | 已返回 |
| `tags / picUrls` | response | array | 已返回；`picUrls` 当前提交页可生产，但 miniapp 页面仍未核出真实 `getReview` 消费点 |
| `reviewLevel / riskLevel / displayStatus / followStatus / auditStatus` | response | number | 已返回 |
| `replyStatus / replyContent / replyTime` | response | mixed | 已返回 |

#### `GET /booking/review/summary`

| 字段 | 方向 | 类型 | 当前说明 |
|---|---|---|---|
| `totalCount` | response | number | 合法空态可为 `0` |
| `positiveCount` | response | number | 合法空态可为 `0` |
| `neutralCount` | response | number | 合法空态可为 `0` |
| `negativeCount` | response | number | 合法空态可为 `0` |
| `averageScore` | response | number | 后端返回，当前列表页未展示 |

### 3.2 Admin API

#### `GET /booking/review/page`

| 字段 | 方向 | 类型 | 当前说明 |
|---|---|---|---|
| `id` | request | number | 可选 |
| `bookingOrderId` | request | number | 可选 |
| `storeId` | request | number | 可选 |
| `technicianId` | request | number | 可选 |
| `memberId` | request | number | 可选 |
| `reviewLevel` | request | number | 可选 |
| `riskLevel` | request | number | 可选 |
| `followStatus` | request | number | 可选 |
| `replyStatus` | request | boolean | 可选 |
| `submitTime[]` | request | string[] | 时间范围 |
| `list[] / total` | response | mixed | 合法空态 `[] / 0` |

#### `POST /booking/review/reply`

| 字段 | 方向 | 类型 | 当前说明 |
|---|---|---|---|
| `reviewId` | request | number | 必填 |
| `replyContent` | request | string | 必填 |
| `data` | response | boolean | 当前成功返回 `true` |

#### `POST /booking/review/follow-status`

| 字段 | 方向 | 类型 | 当前说明 |
|---|---|---|---|
| `reviewId` | request | number | 必填 |
| `followStatus` | request | number | 必填 |
| `followResult` | request | string | 可选 |
| `data` | response | boolean | 当前成功返回 `true` |

#### `GET /booking/review/dashboard-summary`

| 字段 | 方向 | 类型 | 当前说明 |
|---|---|---|---|
| `totalCount` | response | number | 合法空态可为 `0` |
| `positiveCount` | response | number | 合法空态可为 `0` |
| `neutralCount` | response | number | 合法空态可为 `0` |
| `negativeCount` | response | number | 合法空态可为 `0` |
| `pendingFollowCount` | response | number | 已返回 |
| `urgentCount` | response | number | 已返回 |
| `repliedCount` | response | number | 已返回 |

## 4. 派生状态字段字典

| 字段 | 枚举值 | 当前真值 |
|---|---|---|
| `reviewLevel` | `1/2/3` | `好评 / 中评 / 差评` |
| `riskLevel` | `0/1/2` | `正常 / 关注 / 紧急` |
| `displayStatus` | `0/1/2` | `可展示 / 已隐藏 / 待审核` |
| `followStatus` | `0/1/2/3/4` | `无需跟进 / 待跟进 / 跟进中 / 已解决 / 已关闭` |
| `replyStatus` | `true/false` | `已回复 / 未回复` |
| `auditStatus` | `0/2` | `pass / manual_review` |

## 5. 当前字段级 No-Go
1. 不得把 `picUrls` 提交链路已接通外推成“历史页 / 详情页图片回显已经全部闭环”。
2. 不得把 `serviceOrderId` 写成“稳定强绑定且必然非空”。
3. 不得把 `averageScore` 写成“当前页面已展示”。
4. 不得把 `GET /booking/review/get` 的请求参数写成 `reviewId`。
5. 不得把后台 `code=0` 直接写成发布成功样本。
