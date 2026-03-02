package com.hxy.module.booking.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 佣金结算单状态
 */
@Getter
@AllArgsConstructor
public enum CommissionSettlementStatusEnum {

    DRAFT(0, "草稿"),
    PENDING_REVIEW(10, "待审核"),
    APPROVED(20, "已审核"),
    REJECTED(30, "已驳回"),
    VOIDED(40, "已作废"),
    PAID(50, "已打款");

    private final Integer status;
    private final String name;

    public static Optional<CommissionSettlementStatusEnum> of(Integer status) {
        return Arrays.stream(values()).filter(e -> e.status.equals(status)).findFirst();
    }

}
