package com.zbkj.admin.task.booking;

import com.zbkj.service.service.ServiceBookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 预约锁单超时释放任务
 */
@Slf4j
@Component
public class BookingOrderTimeoutReleaseTask {

    @Autowired
    private ServiceBookingService serviceBookingService;

    /**
     * 每分钟执行一次释放
     */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 45_000L)
    public void releaseTimeoutLocks() {
        try {
            Integer released = serviceBookingService.releaseExpiredLocks(200);
            if (released > 0) {
                log.info("booking timeout release finished, released={}", released);
            }
        } catch (Exception e) {
            log.error("booking timeout release failed", e);
        }
    }
}
