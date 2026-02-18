package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.booking.BookingPO;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 预约Dao
 * 
 * MyBatis Plus Mapper
 * 
 * @author 荷小悦架构师
 * @date 2026-02-12
 */
public interface BookingDao extends BaseMapper<BookingPO> {
    
    /**
     * 根据预约编号查询
     */
    BookingPO selectByBookingNo(@Param("bookingNo") String bookingNo);
    
    /**
     * 查询用户预约列表
     */
    List<BookingPO> selectByUserId(@Param("userId") Integer userId, @Param("status") Integer status);
    
    /**
     * 查询技师预约列表
     */
    List<BookingPO> selectByTechnicianAndTimeRange(
        @Param("technicianId") Integer technicianId,
        @Param("startTime") Date startTime,
        @Param("endTime") Date endTime);
    
    /**
     * 查询门店预约列表
     */
    List<BookingPO> selectByStoreAndDate(@Param("storeId") Integer storeId, @Param("date") Date date);
    
    /**
     * 查询超时未支付的预约
     */
    List<BookingPO> selectTimeoutPendingBookings(@Param("timeoutMinutes") int timeoutMinutes);
}


