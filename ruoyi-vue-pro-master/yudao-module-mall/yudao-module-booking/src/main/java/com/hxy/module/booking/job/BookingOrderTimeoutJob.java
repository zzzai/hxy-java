package com.hxy.module.booking.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import com.hxy.module.booking.service.BookingOrderService;
import com.hxy.module.booking.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 预约订单超时取消任务
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingOrderTimeoutJob implements JobHandler {

    private final BookingOrderService bookingOrderService;
    private final TimeSlotService timeSlotService;

    @Override
    public String execute(String param) throws Exception {
        // 1. 释放过期锁定的时间槽
        int releasedSlots = timeSlotService.releaseExpiredLocks();

        // 2. 自动取消超时未支付订单
        int cancelledOrders = bookingOrderService.autoCancelTimeoutOrders();

        String result = String.format("释放过期时间槽: %d, 取消超时订单: %d", releasedSlots, cancelledOrders);
        log.info("[execute][{}]", result);
        return result;
    }

}
