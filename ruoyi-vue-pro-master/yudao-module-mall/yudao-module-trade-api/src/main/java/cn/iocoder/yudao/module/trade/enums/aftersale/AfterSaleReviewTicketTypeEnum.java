package cn.iocoder.yudao.module.trade.enums.aftersale;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 统一工单类型
 */
@Getter
@AllArgsConstructor
public enum AfterSaleReviewTicketTypeEnum implements ArrayValuable<Integer> {

    AFTER_SALE(10, "售后复核"),
    SERVICE_FULFILLMENT(20, "服务履约"),
    COMMISSION_DISPUTE(30, "提成争议");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(AfterSaleReviewTicketTypeEnum::getType)
            .toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
