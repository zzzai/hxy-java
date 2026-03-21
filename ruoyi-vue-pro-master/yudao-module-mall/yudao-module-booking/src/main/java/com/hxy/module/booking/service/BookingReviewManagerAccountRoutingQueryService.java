package com.hxy.module.booking.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingRespVO;

public interface BookingReviewManagerAccountRoutingQueryService {

    BookingReviewManagerAccountRoutingRespVO getRouting(Long storeId);

    PageResult<BookingReviewManagerAccountRoutingRespVO> getRoutingPage(
            BookingReviewManagerAccountRoutingPageReqVO reqVO);
}
