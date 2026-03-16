package com.hxy.module.booking.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewCreateReqVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewEligibilityRespVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewPageReqVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewSummaryRespVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;

@Validated
public interface BookingReviewService {

    AppBookingReviewEligibilityRespVO getEligibility(Long memberId, Long bookingOrderId);

    Long createReview(Long memberId, @Valid AppBookingReviewCreateReqVO reqVO);

    PageResult<BookingReviewDO> getReviewPage(Long memberId, AppBookingReviewPageReqVO reqVO);

    BookingReviewDO getReview(Long memberId, Long reviewId);

    AppBookingReviewSummaryRespVO getSummary(Long memberId);
}
