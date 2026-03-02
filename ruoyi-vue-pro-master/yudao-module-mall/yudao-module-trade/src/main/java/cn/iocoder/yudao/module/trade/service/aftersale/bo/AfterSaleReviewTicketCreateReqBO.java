package cn.iocoder.yudao.module.trade.service.aftersale.bo;

import lombok.Data;

/**
 * 统一工单创建参数
 */
@Data
public class AfterSaleReviewTicketCreateReqBO {

    private Integer ticketType;
    private Long afterSaleId;
    private Long orderId;
    private Long orderItemId;
    private Long userId;
    private String sourceBizNo;
    private String ruleCode;
    private String decisionReason;
    private String severity;
    private String escalateTo;
    private Integer slaMinutes;
    private String remark;
}
