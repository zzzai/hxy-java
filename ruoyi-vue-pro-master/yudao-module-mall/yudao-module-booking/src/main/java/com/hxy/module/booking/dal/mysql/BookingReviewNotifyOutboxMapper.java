package com.hxy.module.booking.dal.mysql;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.controller.admin.vo.BookingReviewNotifyOutboxPageReqVO;
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

    default List<BookingReviewNotifyOutboxDO> selectListByReviewAndStatus(Long reviewId, String status, Integer limit) {
        int safeLimit = Math.max(1, Math.min(ObjectUtil.defaultIfNull(limit, 10), 100));
        return selectList(new LambdaQueryWrapperX<BookingReviewNotifyOutboxDO>()
                .eq(BookingReviewNotifyOutboxDO::getBizId, reviewId)
                .eqIfPresent(BookingReviewNotifyOutboxDO::getStatus, status)
                .orderByDesc(BookingReviewNotifyOutboxDO::getId)
                .last("LIMIT " + safeLimit));
    }

    default PageResult<BookingReviewNotifyOutboxDO> selectPage(BookingReviewNotifyOutboxPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BookingReviewNotifyOutboxDO>()
                .eqIfPresent(BookingReviewNotifyOutboxDO::getBizId, reqVO.getReviewId())
                .eqIfPresent(BookingReviewNotifyOutboxDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(BookingReviewNotifyOutboxDO::getReceiverRole, reqVO.getReceiverRole())
                .eqIfPresent(BookingReviewNotifyOutboxDO::getReceiverUserId, reqVO.getReceiverUserId())
                .eqIfPresent(BookingReviewNotifyOutboxDO::getStatus, reqVO.getStatus())
                .eqIfPresent(BookingReviewNotifyOutboxDO::getChannel, reqVO.getChannel())
                .eqIfPresent(BookingReviewNotifyOutboxDO::getNotifyType, reqVO.getNotifyType())
                .orderByDesc(BookingReviewNotifyOutboxDO::getId));
    }
}
