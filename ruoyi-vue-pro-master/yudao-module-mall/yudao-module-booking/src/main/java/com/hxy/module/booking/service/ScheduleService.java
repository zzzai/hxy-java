package com.hxy.module.booking.service;

import com.hxy.module.booking.dal.dataobject.TechnicianScheduleDO;
import com.hxy.module.booking.dal.dataobject.TimeSlotDO;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班 Service 接口
 */
public interface ScheduleService {

    /**
     * 创建排班
     */
    Long createSchedule(TechnicianScheduleDO schedule);

    /**
     * 批量创建排班（含时间槽生成）
     */
    void batchCreateSchedule(Long technicianId, LocalDate startDate, LocalDate endDate);

    /**
     * 更新排班
     */
    void updateSchedule(TechnicianScheduleDO schedule);

    /**
     * 删除排班
     */
    void deleteSchedule(Long id);

    /**
     * 获取排班
     */
    TechnicianScheduleDO getSchedule(Long id);

    /**
     * 获取技师指定日期的排班
     */
    TechnicianScheduleDO getScheduleByTechnicianAndDate(Long technicianId, LocalDate date);

    /**
     * 获取技师日期范围内的排班列表
     */
    List<TechnicianScheduleDO> getScheduleListByTechnicianAndDateRange(Long technicianId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取门店指定日期的排班列表
     */
    List<TechnicianScheduleDO> getScheduleListByStoreAndDate(Long storeId, LocalDate date);

    /**
     * 设置休息日
     */
    void setRestDay(Long scheduleId, Boolean isRestDay, String remark);

    /**
     * 生成时间槽
     */
    void generateTimeSlots(Long scheduleId);

}
