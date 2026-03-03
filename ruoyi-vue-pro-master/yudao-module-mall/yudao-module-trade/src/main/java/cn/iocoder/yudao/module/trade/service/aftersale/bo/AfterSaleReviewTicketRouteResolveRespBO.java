package cn.iocoder.yudao.module.trade.service.aftersale.bo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 工单 SLA 路由命中结果
 */
@Data
@Accessors(chain = true)
public class AfterSaleReviewTicketRouteResolveRespBO {

    /**
     * 命中作用域
     */
    private String matchedScope;
    /**
     * 命中规则 ID（回退默认时为空）
     */
    private Long routeId;
    /**
     * 命中规则编码
     */
    private String ruleCode;
    /**
     * 命中工单类型
     */
    private Integer ticketType;
    /**
     * 命中严重级别
     */
    private String severity;
    /**
     * 升级责任方
     */
    private String escalateTo;
    /**
     * SLA（分钟）
     */
    private Integer slaMinutes;
    /**
     * 规则排序
     */
    private Integer sort;
    /**
     * 决策链说明
     */
    private String decisionOrder;

}
