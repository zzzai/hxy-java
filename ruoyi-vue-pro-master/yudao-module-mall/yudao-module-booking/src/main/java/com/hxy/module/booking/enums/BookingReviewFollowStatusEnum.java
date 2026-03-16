package com.hxy.module.booking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预约服务评价跟进状态
 */
@Getter
@AllArgsConstructor
public enum BookingReviewFollowStatusEnum {

    NONE(0, "无需跟进"),
    PENDING(1, "待跟进"),
    PROCESSING(2, "跟进中"),
    RESOLVED(3, "已解决"),
    CLOSED(4, "已关闭");

    private final Integer status;
    private final String name;
}
