package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogReplayReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogReplayRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogRespVO;
import com.hxy.module.booking.dal.dataobject.BookingRefundNotifyLogDO;
import com.hxy.module.booking.service.BookingRefundNotifyLogService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingRefundNotifyLogControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BookingRefundNotifyLogController controller;

    @Mock
    private BookingRefundNotifyLogService refundNotifyLogService;

    @Test
    void page_shouldDelegateService() {
        BookingRefundNotifyLogPageReqVO reqVO = new BookingRefundNotifyLogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setStatus("fail");

        BookingRefundNotifyLogDO row = new BookingRefundNotifyLogDO()
                .setId(1L)
                .setOrderId(1001L)
                .setMerchantRefundId("1001-refund")
                .setPayRefundId(9001L)
                .setStatus("fail");
        when(refundNotifyLogService.getNotifyLogPage(eq(reqVO)))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));

        CommonResult<PageResult<BookingRefundNotifyLogRespVO>> result = controller.page(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1L, result.getData().getList().get(0).getId());
        assertEquals(1001L, result.getData().getList().get(0).getOrderId());
        verify(refundNotifyLogService).getNotifyLogPage(eq(reqVO));
    }

    @Test
    void replay_shouldDelegateServiceWithBatchAndDryRun() {
        BookingRefundNotifyLogReplayReqVO reqVO = new BookingRefundNotifyLogReplayReqVO();
        reqVO.setIds(Arrays.asList(10L, 11L));
        reqVO.setDryRun(true);
        BookingRefundNotifyLogReplayRespVO mockResp = new BookingRefundNotifyLogReplayRespVO();
        mockResp.setSuccessCount(1);
        mockResp.setSkipCount(1);
        mockResp.setFailCount(0);
        when(refundNotifyLogService.replayFailedLogs(eq(Arrays.asList(10L, 11L)), eq(true), isNull(), isNull()))
                .thenReturn(mockResp);

        CommonResult<BookingRefundNotifyLogReplayRespVO> result = controller.replay(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().getSuccessCount());
        assertEquals(1, result.getData().getSkipCount());
        verify(refundNotifyLogService).replayFailedLogs(eq(Arrays.asList(10L, 11L)), eq(true), isNull(), isNull());
    }

    @Test
    void replay_shouldCompatSingleId() {
        BookingRefundNotifyLogReplayReqVO reqVO = new BookingRefundNotifyLogReplayReqVO();
        reqVO.setId(10L);
        BookingRefundNotifyLogReplayRespVO mockResp = new BookingRefundNotifyLogReplayRespVO();
        mockResp.setSuccessCount(1);
        when(refundNotifyLogService.replayFailedLogs(eq(Collections.singletonList(10L)), eq(false), isNull(), isNull()))
                .thenReturn(mockResp);

        CommonResult<BookingRefundNotifyLogReplayRespVO> result = controller.replay(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().getSuccessCount());
        verify(refundNotifyLogService).replayFailedLogs(eq(Collections.singletonList(10L)), eq(false), isNull(), isNull());
    }
}
