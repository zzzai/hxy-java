package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogReplayDueReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogReplayReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogReplayRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunDetailPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunDetailRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogSummaryRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogSyncTicketReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogSyncTicketRespVO;
import com.hxy.module.booking.dal.dataobject.BookingRefundNotifyLogDO;
import com.hxy.module.booking.dal.dataobject.BookingRefundReplayRunLogDO;
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

    @Test
    void replayDue_shouldDelegateService() {
        BookingRefundNotifyLogReplayDueReqVO reqVO = new BookingRefundNotifyLogReplayDueReqVO();
        reqVO.setLimit(20);
        reqVO.setDryRun(true);
        BookingRefundNotifyLogReplayRespVO mockResp = new BookingRefundNotifyLogReplayRespVO();
        mockResp.setRunId("RR123");
        mockResp.setSuccessCount(2);
        when(refundNotifyLogService.replayDueFailedLogs(eq(20), eq(true), isNull(), isNull(), eq("MANUAL")))
                .thenReturn(mockResp);

        CommonResult<BookingRefundNotifyLogReplayRespVO> result = controller.replayDue(reqVO);

        assertTrue(result.isSuccess());
        assertEquals("RR123", result.getData().getRunId());
        verify(refundNotifyLogService).replayDueFailedLogs(eq(20), eq(true), isNull(), isNull(), eq("MANUAL"));
    }

    @Test
    void replayDue_shouldUseDefaultWhenReqNull() {
        BookingRefundNotifyLogReplayRespVO mockResp = new BookingRefundNotifyLogReplayRespVO();
        mockResp.setRunId("RR-DEFAULT");
        mockResp.setSuccessCount(0);
        mockResp.setSkipCount(0);
        mockResp.setFailCount(0);
        when(refundNotifyLogService.replayDueFailedLogs(isNull(), eq(false), isNull(), isNull(), eq("MANUAL")))
                .thenReturn(mockResp);

        CommonResult<BookingRefundNotifyLogReplayRespVO> result = controller.replayDue(null);

        assertTrue(result.isSuccess());
        assertEquals("RR-DEFAULT", result.getData().getRunId());
        verify(refundNotifyLogService).replayDueFailedLogs(isNull(), eq(false), isNull(), isNull(), eq("MANUAL"));
    }

    @Test
    void replayRunLogPage_shouldDelegateService() {
        BookingRefundReplayRunLogPageReqVO reqVO = new BookingRefundReplayRunLogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setHasWarning(true);
        reqVO.setMinFailCount(1);
        reqVO.setTriggerSource("MANUAL");
        BookingRefundReplayRunLogDO row = new BookingRefundReplayRunLogDO()
                .setId(1L)
                .setRunId("RR001")
                .setStatus("success");
        when(refundNotifyLogService.getReplayRunLogPage(eq(reqVO)))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));

        CommonResult<PageResult<BookingRefundReplayRunLogRespVO>> result = controller.replayRunLogPage(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1L, result.getData().getTotal());
        assertEquals("RR001", result.getData().getList().get(0).getRunId());
        verify(refundNotifyLogService).getReplayRunLogPage(eq(reqVO));
    }

    @Test
    void replayRunLogGet_shouldDelegateService() {
        BookingRefundReplayRunLogDO row = new BookingRefundReplayRunLogDO()
                .setId(2L)
                .setRunId("RR002")
                .setStatus("partial_fail");
        when(refundNotifyLogService.getReplayRunLog(eq(2L))).thenReturn(row);

        CommonResult<BookingRefundReplayRunLogRespVO> result = controller.replayRunLogGet(2L);

        assertTrue(result.isSuccess());
        assertEquals("RR002", result.getData().getRunId());
        verify(refundNotifyLogService).getReplayRunLog(eq(2L));
    }

    @Test
    void replayRunDetailPage_shouldDelegateService() {
        BookingRefundReplayRunDetailPageReqVO reqVO = new BookingRefundReplayRunDetailPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setRunId("RR003");
        reqVO.setTicketSyncStatus("FAIL");
        BookingRefundReplayRunDetailRespVO row = new BookingRefundReplayRunDetailRespVO();
        row.setId(3L);
        row.setRunId("RR003");
        row.setNotifyLogId(100L);
        when(refundNotifyLogService.getReplayRunDetailPage(eq(reqVO)))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));

        CommonResult<PageResult<BookingRefundReplayRunDetailRespVO>> result = controller.replayRunDetailPage(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(3L, result.getData().getList().get(0).getId());
        verify(refundNotifyLogService).getReplayRunDetailPage(eq(reqVO));
    }

    @Test
    void replayRunDetailGet_shouldDelegateService() {
        BookingRefundReplayRunDetailRespVO detailRespVO = new BookingRefundReplayRunDetailRespVO();
        detailRespVO.setId(4L);
        detailRespVO.setRunId("RR004");
        when(refundNotifyLogService.getReplayRunDetail(eq(4L))).thenReturn(detailRespVO);

        CommonResult<BookingRefundReplayRunDetailRespVO> result = controller.replayRunDetailGet(4L);

        assertTrue(result.isSuccess());
        assertEquals("RR004", result.getData().getRunId());
        verify(refundNotifyLogService).getReplayRunDetail(eq(4L));
    }

    @Test
    void replayRunLogSummary_shouldDelegateService() {
        BookingRefundReplayRunLogSummaryRespVO summaryRespVO = new BookingRefundReplayRunLogSummaryRespVO();
        summaryRespVO.setRunId("RR003");
        summaryRespVO.setWarningCount(2);
        summaryRespVO.setTicketSyncSuccessCount(1);
        when(refundNotifyLogService.getReplayRunLogSummary(eq("RR003"))).thenReturn(summaryRespVO);

        CommonResult<BookingRefundReplayRunLogSummaryRespVO> result = controller.replayRunLogSummary("RR003");

        assertTrue(result.isSuccess());
        assertEquals("RR003", result.getData().getRunId());
        assertEquals(2, result.getData().getWarningCount());
        assertEquals(1, result.getData().getTicketSyncSuccessCount());
        verify(refundNotifyLogService).getReplayRunLogSummary(eq("RR003"));
    }

    @Test
    void replayRunLogSyncTickets_shouldDelegateService() {
        BookingRefundReplayRunLogSyncTicketReqVO reqVO = new BookingRefundReplayRunLogSyncTicketReqVO();
        reqVO.setRunId("RR004");
        reqVO.setOnlyFail(false);
        reqVO.setDryRun(true);
        reqVO.setForceResync(true);
        BookingRefundReplayRunLogSyncTicketRespVO respVO = new BookingRefundReplayRunLogSyncTicketRespVO();
        respVO.setRunId("RR004");
        respVO.setSuccessCount(3);
        when(refundNotifyLogService.syncReplayRunLogTickets(eq("RR004"), eq(false), eq(true), eq(true), isNull(), isNull()))
                .thenReturn(respVO);

        CommonResult<BookingRefundReplayRunLogSyncTicketRespVO> result = controller.replayRunLogSyncTickets(reqVO);

        assertTrue(result.isSuccess());
        assertEquals("RR004", result.getData().getRunId());
        assertEquals(3, result.getData().getSuccessCount());
        verify(refundNotifyLogService).syncReplayRunLogTickets(eq("RR004"), eq(false), eq(true), eq(true), isNull(), isNull());
    }

    @Test
    void replayRunLogSyncTickets_shouldDefaultOnlyFailTrue() {
        BookingRefundReplayRunLogSyncTicketReqVO reqVO = new BookingRefundReplayRunLogSyncTicketReqVO();
        reqVO.setRunId("RR005");
        BookingRefundReplayRunLogSyncTicketRespVO respVO = new BookingRefundReplayRunLogSyncTicketRespVO();
        respVO.setRunId("RR005");
        when(refundNotifyLogService.syncReplayRunLogTickets(eq("RR005"), eq(true), eq(false), eq(false), isNull(), isNull()))
                .thenReturn(respVO);

        CommonResult<BookingRefundReplayRunLogSyncTicketRespVO> result = controller.replayRunLogSyncTickets(reqVO);

        assertTrue(result.isSuccess());
        assertEquals("RR005", result.getData().getRunId());
        verify(refundNotifyLogService).syncReplayRunLogTickets(eq("RR005"), eq(true), eq(false), eq(false), isNull(), isNull());
    }
}
