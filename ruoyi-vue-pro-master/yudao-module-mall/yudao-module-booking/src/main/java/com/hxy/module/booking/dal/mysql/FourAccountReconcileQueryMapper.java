package com.hxy.module.booking.dal.mysql;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface FourAccountReconcileQueryMapper {

    @Select("SELECT COALESCE(SUM(pay_price - IFNULL(refund_price, 0)), 0) " +
            "FROM trade_order " +
            "WHERE pay_status = 1 " +
            "AND pay_time >= #{beginTime} " +
            "AND pay_time < #{endTime} " +
            "AND deleted = 0")
    Integer selectTradeNetAmount(@Param("beginTime") LocalDateTime beginTime,
                                 @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COALESCE(SUM(pay_price), 0) " +
            "FROM booking_order " +
            "WHERE status IN (1, 2, 3, 5) " +
            "AND pay_time >= #{beginTime} " +
            "AND pay_time < #{endTime} " +
            "AND deleted = 0")
    Integer selectFulfillmentAmount(@Param("beginTime") LocalDateTime beginTime,
                                    @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COALESCE(SUM(commission_amount), 0) " +
            "FROM technician_commission " +
            "WHERE status IN (0, 1) " +
            "AND create_time >= #{beginTime} " +
            "AND create_time < #{endTime} " +
            "AND deleted = 0")
    Integer selectCommissionAmount(@Param("beginTime") LocalDateTime beginTime,
                                   @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COALESCE(SUM(price), 0) " +
            "FROM trade_brokerage_record " +
            "WHERE biz_type = 1 " +
            "AND status IN (0, 1) " +
            "AND create_time >= #{beginTime} " +
            "AND create_time < #{endTime} " +
            "AND deleted = 0")
    Integer selectSplitAmount(@Param("beginTime") LocalDateTime beginTime,
                              @Param("endTime") LocalDateTime endTime);
}

