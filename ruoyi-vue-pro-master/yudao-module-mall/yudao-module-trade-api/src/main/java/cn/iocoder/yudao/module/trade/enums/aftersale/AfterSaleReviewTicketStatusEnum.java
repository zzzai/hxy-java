package cn.iocoder.yudao.module.trade.enums.aftersale;

import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 售后人工复核工单状态枚举
 *
 * @author HXY
 */
@Getter
@AllArgsConstructor
public enum AfterSaleReviewTicketStatusEnum implements ArrayValuable<Integer> {

    PENDING(0, "待处理"),
    RESOLVED(10, "已收口");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(AfterSaleReviewTicketStatusEnum::getStatus)
            .toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static boolean isPending(Integer status) {
        return ObjUtil.equal(PENDING.status, status);
    }

}
