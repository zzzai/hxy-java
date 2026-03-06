package com.hxy.module.booking.dal.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 四账退款-提成巡检行（查询模型）
 */
@Data
public class FourAccountRefundCommissionAuditRow {

    private Long orderId;
    private String tradeOrderNo;
    private Long userId;
    private LocalDateTime payTime;
    private Integer refundPrice;
    private Integer settledCommissionAmount;
    private Integer reversalCommissionAmountAbs;
    private Integer activeCommissionAmount;

}
