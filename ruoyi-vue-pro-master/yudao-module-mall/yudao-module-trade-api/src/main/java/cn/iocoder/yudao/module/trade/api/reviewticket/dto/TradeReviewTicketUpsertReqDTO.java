package cn.iocoder.yudao.module.trade.api.reviewticket.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 统一工单幂等写入请求 DTO。
 */
@Data
@Accessors(chain = true)
public class TradeReviewTicketUpsertReqDTO {

    /**
     * 工单类型
     */
    private Integer ticketType;
    /**
     * 售后单 ID（非售后工单可为空）
     */
    private Long afterSaleId;
    /**
     * 订单 ID
     */
    private Long orderId;
    /**
     * 订单项 ID
     */
    private Long orderItemId;
    /**
     * 用户 ID
     */
    private Long userId;
    /**
     * 来源业务号（幂等键组成）
     */
    private String sourceBizNo;
    /**
     * 命中规则编码
     */
    private String ruleCode;
    /**
     * 命中原因
     */
    private String decisionReason;
    /**
     * 严重级别
     */
    private String severity;
    /**
     * 升级对象
     */
    private String escalateTo;
    /**
     * SLA 分钟
     */
    private Integer slaMinutes;
    /**
     * 备注
     */
    private String remark;
    /**
     * 最近动作编码
     */
    private String actionCode;

}
