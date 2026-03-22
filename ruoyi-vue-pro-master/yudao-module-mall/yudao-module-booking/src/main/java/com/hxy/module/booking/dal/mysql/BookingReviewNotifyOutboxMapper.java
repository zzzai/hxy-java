package com.hxy.module.booking.dal.mysql;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.controller.admin.vo.BookingReviewNotifyOutboxPageReqVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewNotifyOutboxDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
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

    default List<BookingReviewNotifyOutboxDO> selectListByBizIds(List<Long> bizIds) {
        if (bizIds == null || bizIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<BookingReviewNotifyOutboxDO>()
                .in(BookingReviewNotifyOutboxDO::getBizId, bizIds)
                .orderByDesc(BookingReviewNotifyOutboxDO::getBizId, BookingReviewNotifyOutboxDO::getId));
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
        return selectPage(reqVO, buildPageQuery(reqVO));
    }

    default List<BookingReviewNotifyOutboxDO> selectList(BookingReviewNotifyOutboxPageReqVO reqVO) {
        return selectList(buildPageQuery(reqVO));
    }

    default LambdaQueryWrapperX<BookingReviewNotifyOutboxDO> buildPageQuery(BookingReviewNotifyOutboxPageReqVO reqVO) {
        return new LambdaQueryWrapperX<BookingReviewNotifyOutboxDO>()
                .eqIfPresent(BookingReviewNotifyOutboxDO::getBizId, reqVO.getReviewId())
                .eqIfPresent(BookingReviewNotifyOutboxDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(BookingReviewNotifyOutboxDO::getReceiverRole, reqVO.getReceiverRole())
                .eqIfPresent(BookingReviewNotifyOutboxDO::getReceiverUserId, reqVO.getReceiverUserId())
                .eqIfPresent(BookingReviewNotifyOutboxDO::getReceiverAccount, reqVO.getReceiverAccount())
                .eqIfPresent(BookingReviewNotifyOutboxDO::getStatus, reqVO.getStatus())
                .eqIfPresent(BookingReviewNotifyOutboxDO::getChannel, reqVO.getChannel())
                .eqIfPresent(BookingReviewNotifyOutboxDO::getNotifyType, reqVO.getNotifyType())
                .eqIfPresent(BookingReviewNotifyOutboxDO::getLastActionCode, reqVO.getLastActionCode())
                .orderByDesc(BookingReviewNotifyOutboxDO::getId);
    }

    default List<BookingReviewNotifyOutboxDO> selectDispatchableList(LocalDateTime now, Integer limit, Integer maxRetryCount) {
        int safeLimit = Math.max(1, Math.min(ObjectUtil.defaultIfNull(limit, 200), 1000));
        int safeMaxRetryCount = Math.max(1, ObjectUtil.defaultIfNull(maxRetryCount, 5));
        LambdaQueryWrapperX<BookingReviewNotifyOutboxDO> query = new LambdaQueryWrapperX<>();
        query.in(BookingReviewNotifyOutboxDO::getStatus, "PENDING", "FAILED");
        query.and(wrapper -> wrapper.isNotNull(BookingReviewNotifyOutboxDO::getReceiverUserId)
                .or()
                .isNotNull(BookingReviewNotifyOutboxDO::getReceiverAccount));
        query.lt(BookingReviewNotifyOutboxDO::getRetryCount, safeMaxRetryCount);
        if (now != null) {
            query.le(BookingReviewNotifyOutboxDO::getNextRetryTime, now);
        }
        query.orderByAsc(BookingReviewNotifyOutboxDO::getId);
        query.last("LIMIT " + safeLimit);
        return selectList(query);
    }

    default int updateByIdAndStatus(Long id, String status, BookingReviewNotifyOutboxDO updateObj) {
        return update(updateObj, new LambdaQueryWrapperX<BookingReviewNotifyOutboxDO>()
                .eq(BookingReviewNotifyOutboxDO::getId, id)
                .eq(BookingReviewNotifyOutboxDO::getStatus, status));
    }
}
