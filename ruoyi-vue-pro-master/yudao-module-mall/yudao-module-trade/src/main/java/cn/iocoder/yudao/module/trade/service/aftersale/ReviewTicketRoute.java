package cn.iocoder.yudao.module.trade.service.aftersale;

import lombok.Data;

/**
 * 工单路由配置
 */
@Data
public class ReviewTicketRoute {

    public static final String DECISION_ORDER = "RULE>TYPE_SEVERITY>TYPE_DEFAULT>GLOBAL_DEFAULT";

    /**
     * 严重等级
     */
    private String severity;
    /**
     * 升级责任方
     */
    private String escalateTo;
    /**
     * SLA 分钟
     */
    private Integer slaMinutes;
    /**
     * 命中路由 ID（兜底规则可能为空）
     */
    private Long routeId;
    /**
     * 命中作用域
     */
    private String matchedScope;
    /**
     * 决策顺序
     */
    private String decisionOrder;

    public ReviewTicketRoute(String severity, String escalateTo, Integer slaMinutes) {
        this(severity, escalateTo, slaMinutes, null, "", DECISION_ORDER);
    }

    public ReviewTicketRoute(String severity, String escalateTo, Integer slaMinutes,
                             Long routeId, String matchedScope, String decisionOrder) {
        this.severity = severity;
        this.escalateTo = escalateTo;
        this.slaMinutes = slaMinutes;
        this.routeId = routeId;
        this.matchedScope = matchedScope;
        this.decisionOrder = decisionOrder;
    }

}
