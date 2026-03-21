package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.dal.dataobject.BookingReviewManagerAccountRoutingDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface BookingReviewManagerAccountRoutingMapper extends BaseMapperX<BookingReviewManagerAccountRoutingDO> {

    default BookingReviewManagerAccountRoutingDO selectLatestByStoreId(Long storeId) {
        return selectOne(new LambdaQueryWrapperX<BookingReviewManagerAccountRoutingDO>()
                .eq(BookingReviewManagerAccountRoutingDO::getStoreId, storeId)
                .orderByDesc(BookingReviewManagerAccountRoutingDO::getLastVerifiedTime,
                        BookingReviewManagerAccountRoutingDO::getId)
                .last("LIMIT 1"));
    }

    default BookingReviewManagerAccountRoutingDO selectEffectiveByStoreId(Long storeId, String bindingStatus, LocalDateTime now) {
        return selectOne(new LambdaQueryWrapperX<BookingReviewManagerAccountRoutingDO>()
                .eq(BookingReviewManagerAccountRoutingDO::getStoreId, storeId)
                .eq(BookingReviewManagerAccountRoutingDO::getBindingStatus, bindingStatus)
                .and(wrapper -> wrapper.isNull(BookingReviewManagerAccountRoutingDO::getEffectiveTime)
                        .or()
                        .le(BookingReviewManagerAccountRoutingDO::getEffectiveTime, now))
                .and(wrapper -> wrapper.isNull(BookingReviewManagerAccountRoutingDO::getExpireTime)
                        .or()
                        .gt(BookingReviewManagerAccountRoutingDO::getExpireTime, now))
                .orderByDesc(BookingReviewManagerAccountRoutingDO::getLastVerifiedTime,
                        BookingReviewManagerAccountRoutingDO::getId)
                .last("LIMIT 1"));
    }
}
