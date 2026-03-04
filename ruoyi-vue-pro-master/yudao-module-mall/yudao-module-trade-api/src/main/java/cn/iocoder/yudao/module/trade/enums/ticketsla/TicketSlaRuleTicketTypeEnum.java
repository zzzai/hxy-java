package cn.iocoder.yudao.module.trade.enums.ticketsla;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * SLA 工单类型
 */
@Getter
@AllArgsConstructor
public enum TicketSlaRuleTicketTypeEnum implements ArrayValuable<Integer> {

    GLOBAL_DEFAULT(0, "全局默认"),
    AFTER_SALE_REVIEW(10, "售后复核"),
    SERVICE_FULFILLMENT(20, "服务履约"),
    COMMISSION_DISPUTE(30, "提成争议"),
    BOOKING_SETTLEMENT(40, "预约结算"),
    BOOKING_SETTLEMENT_NOTIFY(41, "预约结算预警/升级");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(TicketSlaRuleTicketTypeEnum::getType)
            .toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
