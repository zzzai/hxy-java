# Booking Refund Callback Consistency - Window A Handoff

## 1. 变更摘要

1. 预约退款回调 `updateOrderRefunded` 新增支付退款单强校验：
   - 退款单存在
   - 状态为成功
   - 退款金额与预约订单 `payPrice` 一致
   - 商户单号/商户退款号匹配（兼容历史 `merchantRefundId=orderId`）
2. 预约主动退款 `refundOrder` 的 `merchantRefundId` 统一收口为 `orderId-refund` 生成函数。
3. 新增 booking 错误码：
   - `BOOKING_ORDER_REFUND_NOT_FOUND`
   - `BOOKING_ORDER_REFUND_STATUS_INVALID`
   - `BOOKING_ORDER_REFUND_PRICE_MISMATCH`
   - `BOOKING_ORDER_REFUND_BIZ_NO_MISMATCH`

## 2. 关键文件

1. `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/enums/ErrorCodeConstants.java`
2. `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/main/java/com/hxy/module/booking/service/impl/BookingOrderServiceImpl.java`
3. `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-booking/src/test/java/com/hxy/module/booking/service/impl/BookingOrderServiceImplTest.java`

## 3. 测试与回归

1. `BookingOrderServiceImplTest` 新增失败场景：退款单不存在、金额不一致、退款单号不匹配。
2. 原有回调幂等场景保留：已退款直接返回，不重复执行取消与冲正。

## 4. 联调注意

1. 支付回调必须传入真实 `payRefundId`，否则会被 `BOOKING_ORDER_REFUND_NOT_FOUND` 拦截。
2. 对存量历史数据兼容：`merchantRefundId` 支持 `orderId-refund` 与 `orderId` 两种格式。
3. 当前仍保持“主链路 fail-open”边界：仅强化退款回调一致性校验，不变更订单状态机定义。
