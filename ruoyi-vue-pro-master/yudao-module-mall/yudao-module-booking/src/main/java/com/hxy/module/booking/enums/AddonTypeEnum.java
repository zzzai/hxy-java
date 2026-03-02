package com.hxy.module.booking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 加钟类型枚举
 */
@Getter
@AllArgsConstructor
public enum AddonTypeEnum {

    EXTEND(1, "加钟"),
    UPGRADE(2, "升级"),
    ADD_ITEM(3, "加项目");

    private final Integer type;
    private final String name;

}
