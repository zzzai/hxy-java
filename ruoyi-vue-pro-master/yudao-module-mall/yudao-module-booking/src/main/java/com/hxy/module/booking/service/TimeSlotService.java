package com.hxy.module.booking.service;

import com.hxy.module.booking.dal.dataobject.TimeSlotDO;

import java.time.LocalDate;
import java.util.List;

/**
 * 时间槽 Service 接口
 */
public interface TimeSlotService {

    /**
     * 获取时间槽
     */
    TimeSlotDO getTimeSlot(Long id);

    /**
     * 获取门店指定日期的可用时间槽
     */
    List<TimeSlotDO> getAvailableTimeSlotsByStoreAndDate(Long storeId, LocalDate date);

    /**
     * 获取技师指定日期的时间槽
     */
    List<TimeSlotDO> getTimeSlotsByTechnicianAndDate(Long technicianId, LocalDate date);

    /**
     * 锁定时间槽（用于预约）
     *
     * @param slotId 时间槽ID
     * @param userId 用户ID
     * @return 是否锁定成功
     */
    boolean lockTimeSlot(Long slotId, Long userId);

    /**
     * 释放时间槽锁定
     */
    void releaseTimeSlot(Long slotId);

    /**
     * 确认预约（锁定→已预约）
     */
    void confirmBooking(Long slotId, Long bookingOrderId);

    /**
     * 完成服务
     */
    void completeService(Long slotId);

    /**
     * 取消预约
     */
    void cancelBooking(Long slotId);

    /**
     * 释放过期锁定的时间槽（定时任务调用）
     */
    int releaseExpiredLocks();

}
