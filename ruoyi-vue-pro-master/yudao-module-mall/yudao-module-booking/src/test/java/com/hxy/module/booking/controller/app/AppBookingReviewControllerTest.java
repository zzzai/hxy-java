package com.hxy.module.booking.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewCreateReqVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewEligibilityRespVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewPageReqVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewRespVO;
import com.hxy.module.booking.controller.app.vo.AppBookingReviewSummaryRespVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import com.hxy.module.booking.service.BookingReviewService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppBookingReviewControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppBookingReviewController controller;

    @Mock
    private BookingReviewService bookingReviewService;

    @Test
    void shouldGetEligibility() {
        AppBookingReviewEligibilityRespVO respVO = new AppBookingReviewEligibilityRespVO();
        respVO.setBookingOrderId(1001L);
        respVO.setEligible(Boolean.TRUE);
        when(bookingReviewService.getEligibility(10L, 1001L)).thenReturn(respVO);

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10L);

            CommonResult<AppBookingReviewEligibilityRespVO> result = controller.getEligibility(1001L);

            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertTrue(Boolean.TRUE.equals(result.getData().getEligible()));
        }

        verify(bookingReviewService).getEligibility(10L, 1001L);
    }

    @Test
    void shouldCreateReview() {
        AppBookingReviewCreateReqVO reqVO = new AppBookingReviewCreateReqVO();
        reqVO.setBookingOrderId(1002L);
        reqVO.setOverallScore(5);
        when(bookingReviewService.createReview(20L, reqVO)).thenReturn(9001L);

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(20L);

            CommonResult<Long> result = controller.createReview(reqVO);

            assertTrue(result.isSuccess());
            assertEquals(9001L, result.getData());
        }

        verify(bookingReviewService).createReview(20L, reqVO);
    }

    @Test
    void shouldGetReviewPage() {
        BookingReviewDO row = BookingReviewDO.builder()
                .id(9002L)
                .bookingOrderId(1003L)
                .overallScore(4)
                .submitTime(LocalDateTime.now().withNano(0))
                .build();
        AppBookingReviewPageReqVO reqVO = new AppBookingReviewPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        when(bookingReviewService.getReviewPage(30L, reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(30L);

            CommonResult<PageResult<AppBookingReviewRespVO>> result = controller.getReviewPage(reqVO);

            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals(1L, result.getData().getTotal());
            assertEquals(1, result.getData().getList().size());
        }

        verify(bookingReviewService).getReviewPage(30L, reqVO);
    }

    @Test
    void shouldGetReviewDetailAndSummary() {
        BookingReviewDO reviewRespVO = BookingReviewDO.builder()
                .id(9003L)
                .bookingOrderId(1004L)
                .overallScore(2)
                .build();
        AppBookingReviewSummaryRespVO summaryRespVO = new AppBookingReviewSummaryRespVO();
        summaryRespVO.setTotalCount(3L);
        summaryRespVO.setNegativeCount(1L);
        when(bookingReviewService.getReview(40L, 9003L)).thenReturn(reviewRespVO);
        when(bookingReviewService.getSummary(40L)).thenReturn(summaryRespVO);

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(40L);

            CommonResult<AppBookingReviewRespVO> reviewResult = controller.getReview(9003L);
            CommonResult<AppBookingReviewSummaryRespVO> summaryResult = controller.getSummary();

            assertTrue(reviewResult.isSuccess());
            assertEquals(9003L, reviewResult.getData().getId());
            assertTrue(summaryResult.isSuccess());
            assertEquals(3L, summaryResult.getData().getTotalCount());
            assertEquals(1L, summaryResult.getData().getNegativeCount());
        }

        verify(bookingReviewService).getReview(40L, 9003L);
        verify(bookingReviewService).getSummary(40L);
    }
}
