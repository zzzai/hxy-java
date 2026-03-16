package com.hxy.module.booking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预约服务评价展示状态
 */
@Getter
@AllArgsConstructor
public enum BookingReviewDisplayStatusEnum {

    VISIBLE(0, "可展示"),
    HIDDEN(1, "已隐藏"),
    REVIEW_PENDING(2, "待审核");

    private final Integer status;
    private final String name;
}
