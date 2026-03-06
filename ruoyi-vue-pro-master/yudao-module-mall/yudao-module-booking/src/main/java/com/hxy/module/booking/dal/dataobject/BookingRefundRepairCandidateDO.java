package com.hxy.module.booking.dal.dataobject;

import lombok.Data;

/**
 * booking 退款补偿扫描候选
 */
@Data
public class BookingRefundRepairCandidateDO {

    /**
     * 预约订单ID
     */
    private Long orderId;

    /**
     * 支付退款单ID
     */
    private Long payRefundId;

    /**
     * 商户退款单号
     */
    private String merchantRefundId;
}
