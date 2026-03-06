# Booking Refund Notify Hardening - Window A Handoff

## 1. 变更摘要

1. 新增 booking 退款回调入口：`POST /booking/order/update-refunded`（`PermitAll`）。
2. 回调入口解析 `merchantRefundId`，兼容两种格式：
   - `{orderId}-refund`
   - `{orderId}`
3. `booking_order` 审计字段增强：
   - `pay_refund_id`
   - `refund_time`
4. 幂等收口：
   - 已退款且 `pay_refund_id` 相同 -> 幂等成功
   - 已退款但 `pay_refund_id` 不同 -> 抛 `BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT`
5. 一致性校验保持强约束：
   - 退款单存在
   - 退款状态 SUCCESS
   - 退款金额与 `payPrice` 一致
   - 商户单号/退款号匹配

## 2. 关键文件

1. `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/controller/app/AppBookingOrderController.java`
2. `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingOrderServiceImpl.java`
3. `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/dal/dataobject/BookingOrderDO.java`
4. `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/ErrorCodeConstants.java`
5. `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-06-hxy-booking-order-refund-notify-audit.sql`

## 3. 测试覆盖

1. `BookingOrderServiceImplTest`：新增
   - 已退款同退款单号幂等
   - 已退款不同退款单号冲突
   - 已退款但无 `pay_refund_id` 的审计补录
   - 回调成功落 `pay_refund_id/refund_time`
2. 新增 `AppBookingOrderControllerTest`：
   - 成功解析 `{orderId}-refund` / `{orderId}`
   - 非法 `merchantRefundId` 报错
   - 冲突错误码透传

## 4. 联调注意

1. 支付侧必须保证 `merchantRefundId` 按约定传值（`{orderId}-refund` 或 `{orderId}`）。
2. 回调重试必须复用同一个 `payRefundId`，否则会触发幂等冲突错误码。
3. 建议运维对账优先使用 `booking_order.pay_refund_id` 进行订单-退款单绑定核对。
