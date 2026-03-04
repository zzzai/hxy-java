package cn.iocoder.yudao.module.trade.enums.ticketsla;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * SLA 规则作用域类型
 */
@Getter
@AllArgsConstructor
public enum TicketSlaRuleScopeTypeEnum implements ArrayValuable<Integer> {

    GLOBAL(1, "全局"),
    STORE(2, "门店");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(TicketSlaRuleScopeTypeEnum::getCode)
            .toArray(Integer[]::new);

    private final Integer code;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static boolean isGlobal(Integer code) {
        return GLOBAL.code.equals(code);
    }

    public static boolean isStore(Integer code) {
        return STORE.code.equals(code);
    }

}
