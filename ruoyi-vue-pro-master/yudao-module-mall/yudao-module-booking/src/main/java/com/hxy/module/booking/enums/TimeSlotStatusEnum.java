package com.hxy.module.booking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 时间槽状态枚举
 */
@Getter
@AllArgsConstructor
public enum TimeSlotStatusEnum {

    AVAILABLE(0, "可预约"),
    LOCKED(1, "已锁定"),
    BOOKED(2, "已预约"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    private final Integer status;
    private final String name;

}
