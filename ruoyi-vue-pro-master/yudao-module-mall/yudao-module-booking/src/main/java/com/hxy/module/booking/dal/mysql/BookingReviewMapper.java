package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.controller.admin.vo.BookingReviewPageReqVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewPageReqVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import com.hxy.module.booking.enums.BookingReviewLevelEnum;
import com.hxy.module.booking.enums.BookingReviewManagerTodoStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BookingReviewMapper extends BaseMapperX<BookingReviewDO> {

    default LambdaQueryWrapperX<BookingReviewDO> buildAdminQuery(BookingReviewPageReqVO reqVO) {
        LambdaQueryWrapperX<BookingReviewDO> query = new LambdaQueryWrapperX<>();
        query.eqIfPresent(BookingReviewDO::getId, reqVO.getId());
        query.eqIfPresent(BookingReviewDO::getBookingOrderId, reqVO.getBookingOrderId());
        query.eqIfPresent(BookingReviewDO::getStoreId, reqVO.getStoreId());
        query.eqIfPresent(BookingReviewDO::getTechnicianId, reqVO.getTechnicianId());
        query.eqIfPresent(BookingReviewDO::getMemberId, reqVO.getMemberId());
        query.eqIfPresent(BookingReviewDO::getReviewLevel, reqVO.getReviewLevel());
        query.eqIfPresent(BookingReviewDO::getRiskLevel, reqVO.getRiskLevel());
        query.eqIfPresent(BookingReviewDO::getFollowStatus, reqVO.getFollowStatus());
        query.eqIfPresent(BookingReviewDO::getManagerTodoStatus, reqVO.getManagerTodoStatus());
        query.eqIfPresent(BookingReviewDO::getReplyStatus, reqVO.getReplyStatus());
        query.betweenIfPresent(BookingReviewDO::getSubmitTime, reqVO.getSubmitTime());
        query.orderByDesc(BookingReviewDO::getRiskLevel, BookingReviewDO::getSubmitTime, BookingReviewDO::getId);
        if (Boolean.TRUE.equals(reqVO.getOnlyManagerTodo())) {
            query.isNotNull(BookingReviewDO::getManagerTodoStatus);
        }
        if (Boolean.TRUE.equals(reqVO.getOnlyPendingInit())) {
            query.eq(BookingReviewDO::getReviewLevel, BookingReviewLevelEnum.NEGATIVE.getLevel())
                    .isNull(BookingReviewDO::getManagerTodoStatus);
        }
        return query;
    }

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

    default PageResult<BookingReviewDO> selectPageByMemberId(Long memberId, AppBookingReviewPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BookingReviewDO>()
                .eq(BookingReviewDO::getMemberId, memberId)
                .eqIfPresent(BookingReviewDO::getReviewLevel, reqVO.getReviewLevel())
                .orderByDesc(BookingReviewDO::getSubmitTime, BookingReviewDO::getId));
    }

    default PageResult<BookingReviewDO> selectAdminPage(BookingReviewPageReqVO reqVO) {
        return selectPage(reqVO, buildAdminQuery(reqVO));
    }

    default List<BookingReviewDO> selectAdminList(BookingReviewPageReqVO reqVO) {
        return selectList(buildAdminQuery(reqVO));
    }

    default BookingReviewDO selectByIdAndMemberId(Long id, Long memberId) {
        return selectOne(new LambdaQueryWrapperX<BookingReviewDO>()
                .eq(BookingReviewDO::getId, id)
                .eq(BookingReviewDO::getMemberId, memberId));
    }

    default List<BookingReviewDO> selectManagerTodoSlaReminderCandidates(LocalDateTime now, Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 200 : Math.min(limit, 1000);
        return selectList(new LambdaQueryWrapperX<BookingReviewDO>()
                .eq(BookingReviewDO::getReviewLevel, BookingReviewLevelEnum.NEGATIVE.getLevel())
                .isNotNull(BookingReviewDO::getManagerTodoStatus)
                .ne(BookingReviewDO::getManagerTodoStatus, BookingReviewManagerTodoStatusEnum.CLOSED.getStatus())
                .and(wrapper -> wrapper.and(claim -> claim.isNull(BookingReviewDO::getManagerClaimedAt)
                                .isNotNull(BookingReviewDO::getManagerClaimDeadlineAt)
                                .lt(BookingReviewDO::getManagerClaimDeadlineAt, now))
                        .or(firstAction -> firstAction.isNull(BookingReviewDO::getManagerFirstActionAt)
                                .isNotNull(BookingReviewDO::getManagerFirstActionDeadlineAt)
                                .lt(BookingReviewDO::getManagerFirstActionDeadlineAt, now))
                        .or(close -> close.isNotNull(BookingReviewDO::getManagerCloseDeadlineAt)
                                .lt(BookingReviewDO::getManagerCloseDeadlineAt, now)))
                .orderByAsc(BookingReviewDO::getSubmitTime, BookingReviewDO::getId)
                .last("LIMIT " + safeLimit));
    }
}
