package com.hxy.module.booking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预约订单状态枚举
 */
@Getter
@AllArgsConstructor
public enum BookingOrderStatusEnum {

    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    IN_SERVICE(2, "服务中"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消"),
    REFUNDED(5, "已退款");

    private final Integer status;
    private final String name;

}
