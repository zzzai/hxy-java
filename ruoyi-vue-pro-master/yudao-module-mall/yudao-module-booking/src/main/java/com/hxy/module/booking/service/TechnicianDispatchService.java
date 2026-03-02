package com.hxy.module.booking.service;

import com.hxy.module.booking.dal.dataobject.TechnicianDO;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 技师派单 Service 接口
 */
public interface TechnicianDispatchService {

    /**
     * 排钟自动分配技师
     *
     * @param storeId   门店ID
     * @param date      预约日期
     * @param startTime 开始时间
     * @return 最优技师，无可用技师返回 null
     */
    TechnicianDO autoAssignTechnician(Long storeId, LocalDate date, LocalTime startTime);

}
