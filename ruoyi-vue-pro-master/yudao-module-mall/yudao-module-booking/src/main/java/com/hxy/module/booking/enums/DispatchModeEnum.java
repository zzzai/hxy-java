package com.hxy.module.booking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 派单模式枚举
 */
@Getter
@AllArgsConstructor
public enum DispatchModeEnum {

    DESIGNATED(1, "点钟"),
    AUTO_ASSIGN(2, "排钟");

    private final Integer mode;
    private final String name;

}
