package com.hxy.module.booking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预约服务评价等级
 */
@Getter
@AllArgsConstructor
public enum BookingReviewLevelEnum {

    POSITIVE(1, "好评"),
    NEUTRAL(2, "中评"),
    NEGATIVE(3, "差评");

    private final Integer level;
    private final String name;
}
