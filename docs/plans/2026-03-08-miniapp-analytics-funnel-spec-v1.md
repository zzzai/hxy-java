# MiniApp Analytics Funnel Spec v1 (2026-03-08)

## 1. Funnel
- Entry -> Product View -> Add/Book Intent -> Pay -> Refund/After-sale -> Completion.

## 2. Required Events
- `page_view`
- `order_submit`
- `pay_result_view`
- `after_sale_create`
- `refund_progress_view`
- `coupon_take`
- `point_activity_view`

## 3. Event Dimensions
- `route`
- `orderId`
- `afterSaleId`
- `payRefundId`
- `activityId`
- `resultCode/errorCode`

## 4. KPI
- Pay success conversion
- After-sale rate
- Refund completion SLA
- Coupon take conversion
- Points mall activity conversion
