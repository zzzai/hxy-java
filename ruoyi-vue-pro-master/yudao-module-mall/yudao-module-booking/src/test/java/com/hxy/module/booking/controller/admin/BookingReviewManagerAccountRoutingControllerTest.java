package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingSummaryRespVO;
import com.hxy.module.booking.service.BookingReviewManagerAccountRoutingQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingReviewManagerAccountRoutingControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BookingReviewManagerAccountRoutingController controller;

    @Mock
    private BookingReviewManagerAccountRoutingQueryService bookingReviewManagerAccountRoutingQueryService;

    @Test
    void shouldGetManagerRoutingSnapshot() {
        BookingReviewManagerAccountRoutingRespVO respVO = new BookingReviewManagerAccountRoutingRespVO();
        respVO.setStoreId(3001L);
        respVO.setStoreName("朝阳门店");
        respVO.setRoutingStatus("NO_ROUTE");
        respVO.setRoutingLabel("未绑定店长账号");

        when(bookingReviewManagerAccountRoutingQueryService.getRouting(3001L)).thenReturn(respVO);

        CommonResult<BookingReviewManagerAccountRoutingRespVO> result = controller.get(3001L);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("NO_ROUTE", result.getData().getRoutingStatus());
        verify(bookingReviewManagerAccountRoutingQueryService).getRouting(3001L);
    }

    @Test
    void shouldGetManagerRoutingPage() {
        BookingReviewManagerAccountRoutingPageReqVO reqVO = new BookingReviewManagerAccountRoutingPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        BookingReviewManagerAccountRoutingRespVO respVO = new BookingReviewManagerAccountRoutingRespVO();
        respVO.setStoreId(3002L);
        respVO.setRoutingStatus("ACTIVE_ROUTE");
        when(bookingReviewManagerAccountRoutingQueryService.getRoutingPage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(respVO), 1L));

        CommonResult<PageResult<BookingReviewManagerAccountRoutingRespVO>> result = controller.page(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals("ACTIVE_ROUTE", result.getData().getList().get(0).getRoutingStatus());
        verify(bookingReviewManagerAccountRoutingQueryService).getRoutingPage(reqVO);
    }

    @Test
    void shouldGetManagerRoutingCoverageSummary() {
        BookingReviewManagerAccountRoutingPageReqVO reqVO = new BookingReviewManagerAccountRoutingPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        BookingReviewManagerAccountRoutingSummaryRespVO summary = new BookingReviewManagerAccountRoutingSummaryRespVO();
        summary.setTotalStoreCount(1000L);
        summary.setDualReadyCount(820L);
        summary.setMissingAnyCount(180L);
        when(bookingReviewManagerAccountRoutingQueryService.getRoutingCoverageSummary(reqVO)).thenReturn(summary);

        CommonResult<BookingReviewManagerAccountRoutingSummaryRespVO> result = controller.summary(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1000L, result.getData().getTotalStoreCount());
        assertEquals(180L, result.getData().getMissingAnyCount());
        verify(bookingReviewManagerAccountRoutingQueryService).getRoutingCoverageSummary(reqVO);
    }
}
