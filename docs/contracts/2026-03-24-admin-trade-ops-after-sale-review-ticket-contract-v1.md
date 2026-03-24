# Admin Trade Ops After-sale / Review Ticket Contract v1 (2026-03-24)

## 1. 目标与真值来源
- 覆盖能力：`ADM-014 售后单管理 / 详情`、`ADM-015 售后人工复核工单`、`ADM-016 售后工单 SLA 路由规则`。
- 真值输入：
  - PRD：`docs/products/2026-03-15-hxy-admin-after-sale-review-ticket-prd-v1.md`
  - 页面：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/afterSale/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/afterSale/detail/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/reviewTicket/index.vue`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/views/mall/trade/reviewTicketRoute/index.vue`
  - API：
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/trade/afterSale/index.ts`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/trade/reviewTicket/index.ts`
    - `ruoyi-vue-pro-master/script/docker/hxy-ui-admin/overlay-vue3/src/api/mall/trade/reviewTicketRoute/index.ts`
  - Controller：
    - `AfterSaleController`
    - `AfterSaleReviewTicketController`
    - `AfterSaleReviewTicketRouteController`

## 2. 能力绑定矩阵

| 能力 | 页面入口 | controller / path | 当前真值 |
|---|---|---|---|
| `ADM-014` 售后单管理 / 详情 | `mall/trade/afterSale/index`; `mall/trade/afterSale/detail/index` | `/trade/after-sale/*`; `AfterSaleController` | 售后分页、详情、同意、拒绝、收货、拒收、退款都以此为准 |
| `ADM-015` 人工复核工单 | `mall/trade/reviewTicket/index` | `/trade/after-sale/review-ticket/*`; `AfterSaleReviewTicketController` | 工单分页、详情、单条收口、批量收口、通知出站观察都以此为准 |
| `ADM-016` SLA 路由规则 | `mall/trade/reviewTicketRoute/index` | `/trade/after-sale/review-ticket-route/*`; `AfterSaleReviewTicketRouteController` | 路由规则 CRUD、批量启停、启用列表、路由解析都以此为准 |

## 3. Canonical Interface Matrix

| 能力 | method + path | 关键 request / body 真值 | 关键 response 真值 | 合法空态 / 观察态 | 禁止误写 |
|---|---|---|---|---|---|
| `ADM-014` 售后分页 / 详情 | `GET /trade/after-sale/page`; `GET /trade/after-sale/get-detail` | `userId`,`no`,`status`,`type`,`way`,`orderNo`,`spuName`,`refund*`,`createTime[]`,`id` | `PageResult<AfterSaleRespPageItemVO>`；`AfterSaleDetailRespVO` | 空分页合法 | 不能把无售后单写成异常 |
| `ADM-014` 售后动作 | `PUT /trade/after-sale/agree|disagree|receive|refuse|refund` | `id` 或驳回 / 拒收请求体 | `Boolean` | 无 | 不能把布尔成功写成支付退款一定到账 |
| `ADM-015` 工单分页 / 详情 / 创建 / 收口 | `GET /trade/after-sale/review-ticket/page|get`; `POST /trade/after-sale/review-ticket/create`; `PUT /trade/after-sale/review-ticket/resolve`; `POST /trade/after-sale/review-ticket/batch-resolve` | `status`,`severity`,`routeId`,`orderId`,`userId`,`sourceBizNo`,`resolveActionCode`,`resolveBizNo`,`resolveRemark` | 工单详情；批量收口 `success/skipped` | 空分页合法；`skipped` 合法 | 不能把工单收口写成售后、退款、履约全部闭环 |
| `ADM-015` 工单通知出站 | `GET /trade/after-sale/review-ticket/notify-outbox-page`; `POST /trade/after-sale/review-ticket/notify-outbox-batch-retry` | `ticketId`,`status`,`notifyType`,`channel`,`lastActionCode`,`ids[]` | 出站分页；批量重试结果 | 无出站记录合法 | 不能把出站记录写成接收方已完成处理 |
| `ADM-016` 路由规则 CRUD / 启停 | `POST /trade/after-sale/review-ticket-route/create`; `PUT /trade/after-sale/review-ticket-route/update`; `DELETE /trade/after-sale/review-ticket-route/delete`; `POST /trade/after-sale/review-ticket-route/batch-delete`; `POST /trade/after-sale/review-ticket-route/batch-update-enabled` | `scope`,`ruleCode`,`ticketType`,`severity`,`escalateTo`,`slaMinutes`,`enabled`,`sort`,`ids[]` | `Long id`、`Boolean`、`Integer affected` | 无 | 不能把配置变更写成所有新工单已即时按新规则落单 |
| `ADM-016` 路由规则查询 / 解析 | `GET /trade/after-sale/review-ticket-route/get|page|list-enabled|resolve` | `id`,`scope`,`ruleCode`,`ticketType`,`severity`,`enabled` | 路由详情 / 分页 / 启用列表 / 解析结果 | 空分页 / 空启用列表合法 | 不能把 `resolve` 结果写成线上所有工单已按该结果执行 |

## 4. 边界说明
- `refund` 接口成功只表示后台退款动作受理成功，仍需结合支付侧证据判断。
- review ticket 的 `notify-outbox-page` 与 `batch-retry` 只说明通知观察 / 补偿链路，不说明接收方已闭环。
- route `resolve` 是规则解析真值，不等于历史工单都已迁移或线上路由已全部重算。

## 5. 当前结论
- `ADM-014` ~ `ADM-016` 已具备独立后台 contract。
- 这不代表售后或工单链路可以越过 release 门禁。
