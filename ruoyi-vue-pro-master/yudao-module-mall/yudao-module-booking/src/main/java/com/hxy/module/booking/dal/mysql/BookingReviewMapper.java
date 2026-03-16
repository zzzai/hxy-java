package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BookingReviewMapper extends BaseMapperX<BookingReviewDO> {

    default BookingReviewDO selectByBookingOrderId(Long bookingOrderId) {
        return selectOne(new LambdaQueryWrapperX<BookingReviewDO>()
                .eq(BookingReviewDO::getBookingOrderId, bookingOrderId)
                .orderByDesc(BookingReviewDO::getId)
                .last("LIMIT 1"));
    }

    default List<BookingReviewDO> selectListByMemberId(Long memberId) {
        return selectList(new LambdaQueryWrapperX<BookingReviewDO>()
                .eq(BookingReviewDO::getMemberId, memberId)
                .orderByDesc(BookingReviewDO::getSubmitTime, BookingReviewDO::getId));
    }

    default List<BookingReviewDO> selectListByStoreId(Long storeId) {
        return selectList(new LambdaQueryWrapperX<BookingReviewDO>()
                .eq(BookingReviewDO::getStoreId, storeId)
                .orderByDesc(BookingReviewDO::getSubmitTime, BookingReviewDO::getId));
    }
}
