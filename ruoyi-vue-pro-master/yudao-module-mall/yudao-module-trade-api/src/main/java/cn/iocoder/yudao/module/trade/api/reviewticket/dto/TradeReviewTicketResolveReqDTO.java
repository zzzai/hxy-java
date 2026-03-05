package cn.iocoder.yudao.module.trade.api.reviewticket.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 统一工单按来源收口请求 DTO。
 */
@Data
@Accessors(chain = true)
public class TradeReviewTicketResolveReqDTO {

    /**
     * 工单类型
     */
    private Integer ticketType;
    /**
     * 来源业务号（幂等键组成）
     */
    private String sourceBizNo;
    /**
     * 收口人 ID
     */
    private Long resolverId;
    /**
     * 收口人类型
     */
    private Integer resolverType;
    /**
     * 收口动作编码
     */
    private String resolveActionCode;
    /**
     * 收口业务号
     */
    private String resolveBizNo;
    /**
     * 收口备注
     */
    private String resolveRemark;

}
