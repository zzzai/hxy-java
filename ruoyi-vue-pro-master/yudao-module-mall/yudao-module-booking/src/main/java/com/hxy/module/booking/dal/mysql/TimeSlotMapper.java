package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.dal.dataobject.TimeSlotDO;
import com.hxy.module.booking.enums.TimeSlotStatusEnum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TimeSlotMapper extends BaseMapperX<TimeSlotDO> {

    /**
     * 悲观锁查询（FOR UPDATE）
     */
    @Select("SELECT * FROM booking_time_slot WHERE id = #{id} FOR UPDATE")
    TimeSlotDO selectByIdForUpdate(@Param("id") Long id);

    /**
     * 锁定时间槽（CAS更新）
     */
    @Update("UPDATE booking_time_slot SET status = #{newStatus}, lock_user_id = #{userId}, " +
            "lock_expire_time = #{expireTime}, update_time = NOW() " +
            "WHERE id = #{id} AND status = #{oldStatus}")
    int lockTimeSlot(@Param("id") Long id, @Param("oldStatus") Integer oldStatus,
                     @Param("newStatus") Integer newStatus, @Param("userId") Long userId,
                     @Param("expireTime") LocalDateTime expireTime);

    /**
     * 释放过期锁定的时间槽
     */
    @Update("UPDATE booking_time_slot SET status = 0, lock_user_id = NULL, " +
            "lock_expire_time = NULL, update_time = NOW() " +
            "WHERE status = 1 AND lock_expire_time < #{now}")
    int releaseExpiredLocks(@Param("now") LocalDateTime now);

    default List<TimeSlotDO> selectListByScheduleId(Long scheduleId) {
        return selectList(new LambdaQueryWrapperX<TimeSlotDO>()
                .eq(TimeSlotDO::getScheduleId, scheduleId)
                .orderByAsc(TimeSlotDO::getStartTime));
    }

    default List<TimeSlotDO> selectListByTechnicianIdAndDate(Long technicianId, LocalDate slotDate) {
        return selectList(new LambdaQueryWrapperX<TimeSlotDO>()
                .eq(TimeSlotDO::getTechnicianId, technicianId)
                .eq(TimeSlotDO::getSlotDate, slotDate)
                .orderByAsc(TimeSlotDO::getStartTime));
    }

    default List<TimeSlotDO> selectAvailableByStoreIdAndDate(Long storeId, LocalDate slotDate) {
        return selectList(new LambdaQueryWrapperX<TimeSlotDO>()
                .eq(TimeSlotDO::getStoreId, storeId)
                .eq(TimeSlotDO::getSlotDate, slotDate)
                .eq(TimeSlotDO::getStatus, TimeSlotStatusEnum.AVAILABLE.getStatus())
                .orderByAsc(TimeSlotDO::getStartTime));
    }

    default List<TimeSlotDO> selectListByStoreIdAndDateRange(Long storeId, LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<TimeSlotDO>()
                .eq(TimeSlotDO::getStoreId, storeId)
                .ge(TimeSlotDO::getSlotDate, startDate)
                .le(TimeSlotDO::getSlotDate, endDate)
                .orderByAsc(TimeSlotDO::getSlotDate)
                .orderByAsc(TimeSlotDO::getStartTime));
    }

}
