package com.hxy.module.booking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 佣金结算状态枚举
 */
@Getter
@AllArgsConstructor
public enum CommissionStatusEnum {

    PENDING(0, "待结算"),
    SETTLED(1, "已结算"),
    CANCELLED(2, "已取消");

    private final Integer status;
    private final String name;

}
