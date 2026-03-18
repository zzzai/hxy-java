package com.hxy.module.booking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预约服务评价店长待办状态
 */
@Getter
@AllArgsConstructor
public enum BookingReviewManagerTodoStatusEnum {

    PENDING_CLAIM(1, "待认领"),
    CLAIMED(2, "已认领"),
    PROCESSING(3, "处理中"),
    CLOSED(4, "已闭环");

    private final Integer status;
    private final String name;
}
