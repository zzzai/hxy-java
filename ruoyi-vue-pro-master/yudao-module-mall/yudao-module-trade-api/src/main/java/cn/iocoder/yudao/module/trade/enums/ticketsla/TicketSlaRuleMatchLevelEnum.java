package cn.iocoder.yudao.module.trade.enums.ticketsla;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SLA 规则命中层级
 */
@Getter
@AllArgsConstructor
public enum TicketSlaRuleMatchLevelEnum {

    RULE(1, "规则级"),
    TYPE_SEVERITY(2, "工单类型+严重级别"),
    TYPE_DEFAULT(3, "工单类型默认"),
    GLOBAL_DEFAULT(4, "全局默认"),
    NONE(99, "未命中");

    private final Integer code;
    private final String name;

}
