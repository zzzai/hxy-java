package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hxy.module.booking.controller.admin.vo.BookingReviewPageReqVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewPageReqVO;
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

    default PageResult<BookingReviewDO> selectPageByMemberId(Long memberId, AppBookingReviewPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BookingReviewDO>()
                .eq(BookingReviewDO::getMemberId, memberId)
                .eqIfPresent(BookingReviewDO::getReviewLevel, reqVO.getReviewLevel())
                .orderByDesc(BookingReviewDO::getSubmitTime, BookingReviewDO::getId));
    }

    default PageResult<BookingReviewDO> selectAdminPage(BookingReviewPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BookingReviewDO>()
                .eqIfPresent(BookingReviewDO::getId, reqVO.getId())
                .eqIfPresent(BookingReviewDO::getBookingOrderId, reqVO.getBookingOrderId())
                .eqIfPresent(BookingReviewDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(BookingReviewDO::getTechnicianId, reqVO.getTechnicianId())
                .eqIfPresent(BookingReviewDO::getMemberId, reqVO.getMemberId())
                .eqIfPresent(BookingReviewDO::getReviewLevel, reqVO.getReviewLevel())
                .eqIfPresent(BookingReviewDO::getRiskLevel, reqVO.getRiskLevel())
                .eqIfPresent(BookingReviewDO::getFollowStatus, reqVO.getFollowStatus())
                .eqIfPresent(BookingReviewDO::getReplyStatus, reqVO.getReplyStatus())
                .betweenIfPresent(BookingReviewDO::getSubmitTime, reqVO.getSubmitTime())
                .orderByDesc(BookingReviewDO::getRiskLevel, BookingReviewDO::getSubmitTime, BookingReviewDO::getId));
    }

    default BookingReviewDO selectByIdAndMemberId(Long id, Long memberId) {
        return selectOne(new LambdaQueryWrapperX<BookingReviewDO>()
                .eq(BookingReviewDO::getId, id)
                .eq(BookingReviewDO::getMemberId, memberId));
    }
}
