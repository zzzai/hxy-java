package cn.iocoder.yudao.module.trade.api.ticketsla.dto;

import lombok.Data;

/**
 * SLA 工单规则匹配结果
 */
@Data
public class TradeTicketSlaRuleMatchRespDTO {

    /**
     * 是否匹配到规则
     */
    private Boolean matched;
    /**
     * 命中层级
     */
    private Integer matchLevel;
    /**
     * 命中规则 ID
     */
    private Long ruleId;
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
     * 升级对象
     */
    private String escalateTo;
    /**
     * SLA 分钟
     */
    private Integer slaMinutes;
    /**
     * 预警提前分钟
     */
    private Integer warnLeadMinutes;
    /**
     * 升级延迟分钟
     */
    private Integer escalateDelayMinutes;
    /**
     * 优先级
     */
    private Integer priority;

}
