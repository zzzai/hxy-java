package cn.iocoder.yudao.module.trade.enums.order;

import cn.hutool.core.util.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 服务履约单状态
 *
 * @author HXY
 */
@Getter
@AllArgsConstructor
public enum TradeServiceOrderStatusEnum {

    WAIT_BOOKING(0, "待预约"),
    BOOKED(10, "已预约"),
    SERVING(20, "服务中"),
    FINISHED(30, "已完成"),
    CANCELLED(40, "已取消");

    private final Integer status;
    private final String name;

    public static boolean isWaitBooking(Integer status) {
        return ObjUtil.equal(WAIT_BOOKING.status, status);
    }

    public static boolean isBooked(Integer status) {
        return ObjUtil.equal(BOOKED.status, status);
    }

    public static boolean isServing(Integer status) {
        return ObjUtil.equal(SERVING.status, status);
    }

    public static String getNameByStatus(Integer status) {
        return Arrays.stream(values())
                .filter(item -> ObjUtil.equal(item.status, status))
                .map(TradeServiceOrderStatusEnum::getName)
                .findFirst()
                .orElse("UNKNOWN");
    }

}
