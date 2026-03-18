package com.hxy.module.booking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预约服务评价差评触发类型
 */
@Getter
@AllArgsConstructor
public enum BookingReviewNegativeTriggerTypeEnum {

    REVIEW_LEVEL_NEGATIVE("REVIEW_LEVEL_NEGATIVE", "差评命中"),
    LOW_SCORE_NEGATIVE("LOW_SCORE_NEGATIVE", "低分命中");

    private final String type;
    private final String name;
}
