package cn.iocoder.yudao.module.trade.enums.aftersale;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 售后复核工单路由规则作用域
 */
@Getter
@AllArgsConstructor
public enum AfterSaleReviewTicketRouteScopeEnum implements ArrayValuable<String> {

    /**
     * 基于规则编码匹配：ruleCode
     */
    RULE("RULE"),
    /**
     * 基于工单类型 + 严重级别匹配：ticketType + severity
     */
    TYPE_SEVERITY("TYPE_SEVERITY"),
    /**
     * 基于工单类型匹配：ticketType
     */
    TYPE_DEFAULT("TYPE_DEFAULT"),
    /**
     * 全局默认兜底
     */
    GLOBAL_DEFAULT("GLOBAL_DEFAULT");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(AfterSaleReviewTicketRouteScopeEnum::getScope)
            .toArray(String[]::new);

    private final String scope;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
