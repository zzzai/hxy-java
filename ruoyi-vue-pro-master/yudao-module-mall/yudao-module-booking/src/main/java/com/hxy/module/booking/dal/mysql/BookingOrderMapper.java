package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface BookingOrderMapper extends BaseMapperX<BookingOrderDO> {

    default BookingOrderDO selectByOrderNo(String orderNo) {
        return selectOne(new LambdaQueryWrapperX<BookingOrderDO>()
                .eq(BookingOrderDO::getOrderNo, orderNo));
    }

    default List<BookingOrderDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<BookingOrderDO>()
                .eq(BookingOrderDO::getUserId, userId)
                .orderByDesc(BookingOrderDO::getCreateTime));
    }

    default List<BookingOrderDO> selectListByUserIdAndStatus(Long userId, Integer status) {
        return selectList(new LambdaQueryWrapperX<BookingOrderDO>()
                .eq(BookingOrderDO::getUserId, userId)
                .eq(BookingOrderDO::getStatus, status)
                .orderByDesc(BookingOrderDO::getCreateTime));
    }

    default List<BookingOrderDO> selectListByTechnicianIdAndDate(Long technicianId, LocalDate bookingDate) {
        return selectList(new LambdaQueryWrapperX<BookingOrderDO>()
                .eq(BookingOrderDO::getTechnicianId, technicianId)
                .eq(BookingOrderDO::getBookingDate, bookingDate)
                .orderByAsc(BookingOrderDO::getBookingStartTime));
    }

    default List<BookingOrderDO> selectListByStoreIdAndDate(Long storeId, LocalDate bookingDate) {
        return selectList(new LambdaQueryWrapperX<BookingOrderDO>()
                .eq(BookingOrderDO::getStoreId, storeId)
                .eq(BookingOrderDO::getBookingDate, bookingDate)
                .orderByAsc(BookingOrderDO::getBookingStartTime));
    }

    default List<BookingOrderDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<BookingOrderDO>()
                .eq(BookingOrderDO::getStatus, status));
    }

    default BookingOrderDO selectByPayOrderId(Long payOrderId) {
        return selectOne(new LambdaQueryWrapperX<BookingOrderDO>()
                .eq(BookingOrderDO::getPayOrderId, payOrderId));
    }

}
