package cn.iocoder.yudao.module.trade.api.ticketsla.dto;

import lombok.Data;

/**
 * SLA 工单规则匹配请求
 */
@Data
public class TradeTicketSlaRuleMatchReqDTO {

    /**
     * 工单类型
     */
    private Integer ticketType;
    /**
     * 命中规则编码
     */
    private String ruleCode;
    /**
     * 严重级别
     */
    private String severity;
    /**
     * 门店 ID
     */
    private Long storeId;

}
