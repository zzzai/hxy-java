package com.hxy.module.booking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 四账对账状态枚举
 */
@Getter
@AllArgsConstructor
public enum FourAccountReconcileStatusEnum {

    PASS(10, "通过"),
    WARN(20, "告警");

    private final Integer status;
    private final String name;
}

