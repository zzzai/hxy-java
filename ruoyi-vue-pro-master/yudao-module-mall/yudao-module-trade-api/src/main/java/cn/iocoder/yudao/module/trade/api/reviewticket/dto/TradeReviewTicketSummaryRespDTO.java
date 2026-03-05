package cn.iocoder.yudao.module.trade.api.reviewticket.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 统一工单摘要响应 DTO。
 */
@Data
@Accessors(chain = true)
public class TradeReviewTicketSummaryRespDTO implements Serializable {

    /**
     * 工单 ID
     */
    private Long id;
    /**
     * 工单类型
     */
    private Integer ticketType;
    /**
     * 来源业务号
     */
    private String sourceBizNo;
    /**
     * 工单状态
     */
    private Integer status;
    /**
     * 严重级别
     */
    private String severity;
}

