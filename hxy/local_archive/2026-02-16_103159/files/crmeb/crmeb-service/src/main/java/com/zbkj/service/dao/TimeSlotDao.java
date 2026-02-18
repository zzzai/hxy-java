package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.booking.TimeSlotPO;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 时间槽Dao
 */
public interface TimeSlotDao extends BaseMapper<TimeSlotPO> {
    
    /**
     * 查询技师某天的时间槽
     */
    List<TimeSlotPO> findByTechnicianAndDate(
        @Param("technicianId") Integer technicianId,
        @Param("date") Date date
    );
    
    /**
     * 查询可用时间槽
     */
    List<TimeSlotPO> findAvailableSlots(
        @Param("technicianId") Integer technicianId,
        @Param("startTime") Date startTime,
        @Param("endTime") Date endTime
    );
}


