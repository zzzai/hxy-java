package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.dal.dataobject.BookingReviewNotifyOutboxDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BookingReviewNotifyOutboxMapper extends BaseMapperX<BookingReviewNotifyOutboxDO> {

    default BookingReviewNotifyOutboxDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(BookingReviewNotifyOutboxDO::getIdempotencyKey, idempotencyKey);
    }

    default List<BookingReviewNotifyOutboxDO> selectListByBizId(Long bizId) {
        return selectList(new LambdaQueryWrapperX<BookingReviewNotifyOutboxDO>()
                .eq(BookingReviewNotifyOutboxDO::getBizId, bizId)
                .orderByAsc(BookingReviewNotifyOutboxDO::getId));
    }
}
