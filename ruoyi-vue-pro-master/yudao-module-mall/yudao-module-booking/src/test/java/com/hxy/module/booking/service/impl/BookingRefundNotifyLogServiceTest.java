package com.hxy.module.booking.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.pay.enums.refund.PayRefundStatusEnum;
import cn.iocoder.yudao.module.trade.api.reviewticket.TradeReviewTicketApi;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogReplayRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogSummaryRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogSyncTicketRespVO;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.dal.dataobject.BookingRefundNotifyLogDO;
import com.hxy.module.booking.dal.dataobject.BookingRefundReplayRunDetailDO;
import com.hxy.module.booking.dal.dataobject.BookingRefundReplayRunLogDO;
import com.hxy.module.booking.dal.dataobject.BookingRefundRepairCandidateDO;
import com.hxy.module.booking.dal.mysql.BookingRefundNotifyLogMapper;
import com.hxy.module.booking.dal.mysql.BookingRefundReconcileQueryMapper;
import com.hxy.module.booking.dal.mysql.BookingRefundReplayRunDetailMapper;
import com.hxy.module.booking.dal.mysql.BookingRefundReplayRunLogMapper;
import com.hxy.module.booking.enums.BookingOrderStatusEnum;
import com.hxy.module.booking.service.BookingOrderService;
import com.hxy.module.booking.service.FourAccountReconcileService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.hxy.module.booking.enums.ErrorCodeConstants.BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingRefundNotifyLogServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BookingRefundNotifyLogServiceImpl service;

    @Mock
    private BookingRefundNotifyLogMapper refundNotifyLogMapper;
    @Mock
    private BookingRefundReconcileQueryMapper refundReconcileQueryMapper;
    @Mock
    private BookingRefundReplayRunDetailMapper refundReplayRunDetailMapper;
    @Mock
    private BookingRefundReplayRunLogMapper refundReplayRunLogMapper;
    @Mock
    private BookingOrderService bookingOrderService;
    @Mock
    private FourAccountReconcileService fourAccountReconcileService;
    @Mock
    private TradeReviewTicketApi tradeReviewTicketApi;

    @Test
    void replayFailedLogs_shouldMarkSuccessWhenReplayPassed() {
        BookingRefundNotifyLogDO logDO = new BookingRefundNotifyLogDO()
                .setId(1L)
                .setOrderId(1001L)
                .setPayRefundId(9001L)
                .setStatus("fail")
                .setRetryCount(1);
        when(refundNotifyLogMapper.selectById(eq(1L))).thenReturn(logDO);
        when(bookingOrderService.getOrder(eq(1001L))).thenReturn(buildRefundedOrder(1001L, 9001L));

        BookingRefundNotifyLogReplayRespVO respVO =
                service.replayFailedLogs(Collections.singletonList(1L), false, 99L, "ops-user");

        assertEquals(1, respVO.getSuccessCount());
        assertEquals(0, respVO.getFailCount());
        assertEquals("SUCCESS", respVO.getDetails().get(0).getResultStatus());
        verify(bookingOrderService).updateOrderRefunded(eq(1001L), eq(9001L));
        verify(refundNotifyLogMapper).updateReplaySuccess(eq(1L), eq("success"), eq(2),
                eq("ops-user"), any(LocalDateTime.class), eq("SUCCESS"), contains("REPLAY_SUCCESS"));
    }

    @Test
    void replayFailedLogs_shouldSkipWhenAlreadySuccessLog() {
        BookingRefundNotifyLogDO logDO = new BookingRefundNotifyLogDO()
                .setId(2L)
                .setOrderId(1002L)
                .setPayRefundId(9002L)
                .setStatus("success");
        when(refundNotifyLogMapper.selectById(eq(2L))).thenReturn(logDO);

        BookingRefundNotifyLogReplayRespVO respVO =
                service.replayFailedLogs(Collections.singletonList(2L), false, 99L, "ops-user");

        assertEquals(0, respVO.getSuccessCount());
        assertEquals(1, respVO.getSkipCount());
        assertEquals("SKIP", respVO.getDetails().get(0).getResultStatus());
        verify(refundNotifyLogMapper).updateReplayAudit(eq(2L), eq("ops-user"),
                any(LocalDateTime.class), eq("SKIP"), contains("REPLAY_SKIP_ALREADY_SUCCESS"));
        verify(bookingOrderService, never()).updateOrderRefunded(any(), any());
    }

    @Test
    void replayFailedLogs_shouldMarkFailureWhenReplayThrows() {
        BookingRefundNotifyLogDO logDO = new BookingRefundNotifyLogDO()
                .setId(3L)
                .setOrderId(1003L)
                .setPayRefundId(9003L)
                .setStatus("fail")
                .setRetryCount(2);
        when(refundNotifyLogMapper.selectById(eq(3L))).thenReturn(logDO);
        doThrow(exception(BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT))
                .when(bookingOrderService).updateOrderRefunded(eq(1003L), eq(9003L));

        BookingRefundNotifyLogReplayRespVO respVO =
                service.replayFailedLogs(Collections.singletonList(3L), false, 99L, "ops-user");

        assertEquals(0, respVO.getSuccessCount());
        assertEquals(1, respVO.getFailCount());
        assertEquals(String.valueOf(BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT.getCode()),
                respVO.getDetails().get(0).getResultCode());
        verify(refundNotifyLogMapper).updateReplayFailure(eq(3L), eq("fail"), eq(3),
                any(LocalDateTime.class),
                eq(String.valueOf(BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT.getCode())), any(),
                eq("ops-user"), any(LocalDateTime.class), eq("FAIL"), contains("REPLAY_FAIL"));
    }

    @Test
    void replayFailedLogs_shouldDryRunWithoutMutatingStatus() {
        BookingRefundNotifyLogDO logDO = new BookingRefundNotifyLogDO()
                .setId(4L)
                .setOrderId(1004L)
                .setPayRefundId(9004L)
                .setStatus("fail");
        when(refundNotifyLogMapper.selectById(eq(4L))).thenReturn(logDO);
        when(bookingOrderService.getOrder(eq(1004L))).thenReturn(buildPaidOrder(1004L));

        BookingRefundNotifyLogReplayRespVO respVO =
                service.replayFailedLogs(Collections.singletonList(4L), true, 99L, "ops-user");

        assertEquals(1, respVO.getSuccessCount());
        assertEquals("DRY_RUN_OK", respVO.getDetails().get(0).getResultCode());
        verify(refundNotifyLogMapper).updateReplayAudit(eq(4L), eq("ops-user"),
                any(LocalDateTime.class), eq("SUCCESS"), contains("DRY_RUN_PASS"));
        verify(refundNotifyLogMapper, never()).updateReplaySuccess(any(), any(), any(), any(), any(), any(), any());
        verify(refundNotifyLogMapper, never()).updateReplayFailure(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(bookingOrderService, never()).updateOrderRefunded(any(), any());
    }

    @Test
    void replayFailedLogs_shouldFailOpenWhenFourAccountRefreshThrows() {
        BookingRefundNotifyLogDO logDO = new BookingRefundNotifyLogDO()
                .setId(5L)
                .setOrderId(1005L)
                .setPayRefundId(9005L)
                .setStatus("fail")
                .setRetryCount(0);
        when(refundNotifyLogMapper.selectById(eq(5L))).thenReturn(logDO);
        when(bookingOrderService.getOrder(eq(1005L))).thenReturn(buildRefundedOrder(1005L, 9005L));
        doThrow(new RuntimeException("trade timeout"))
                .when(fourAccountReconcileService)
                .runReconcile(eq(LocalDate.now()), eq("REFUND_NOTIFY_REPLAY"), eq("ops-user"));

        BookingRefundNotifyLogReplayRespVO respVO =
                service.replayFailedLogs(Collections.singletonList(5L), false, 99L, "ops-user");

        assertEquals(1, respVO.getSuccessCount());
        assertTrue(respVO.getDetails().get(0).getResultMsg().contains("降级"));
        verify(refundNotifyLogMapper).updateReplaySuccess(eq(5L), eq("success"), eq(1),
                eq("ops-user"), any(LocalDateTime.class), eq("SUCCESS"), contains("FOUR_ACCOUNT_REFRESH_WARN"));
    }

    @Test
    void replayDueFailedLogs_shouldScanAndReplay() {
        when(refundReplayRunLogMapper.insert(any(BookingRefundReplayRunLogDO.class))).thenAnswer(invocation -> {
            BookingRefundReplayRunLogDO runLogDO = invocation.getArgument(0);
            runLogDO.setId(101L);
            return 1;
        });
        when(refundNotifyLogMapper.selectDueFailIds(any(LocalDateTime.class), eq(20), eq("fail")))
                .thenReturn(Collections.singletonList(6L));
        BookingRefundNotifyLogDO logDO = new BookingRefundNotifyLogDO()
                .setId(6L)
                .setOrderId(1006L)
                .setPayRefundId(9006L)
                .setStatus("success");
        when(refundNotifyLogMapper.selectById(eq(6L))).thenReturn(logDO);

        BookingRefundNotifyLogReplayRespVO respVO =
                service.replayDueFailedLogs(20, false, null, "SYSTEM_JOB", "JOB");

        assertEquals(1, respVO.getSkipCount());
        assertTrue(StrUtil.isNotBlank(respVO.getRunId()));
        verify(refundNotifyLogMapper).selectDueFailIds(any(LocalDateTime.class), eq(20), eq("fail"));
        verify(refundNotifyLogMapper).updateReplayAudit(eq(6L), eq("SYSTEM_JOB"), any(LocalDateTime.class),
                eq("SKIP"), contains("REPLAY_SKIP_ALREADY_SUCCESS"));
        verify(refundReplayRunLogMapper).updateRunResult(eq(101L), eq(1), eq(0), eq(1), eq(0),
                eq("success"), any(), any(LocalDateTime.class));
    }

    @Test
    void replayDueFailedLogs_shouldMarkPartialFailOnSkipAndFail() {
        when(refundReplayRunLogMapper.insert(any(BookingRefundReplayRunLogDO.class))).thenAnswer(invocation -> {
            BookingRefundReplayRunLogDO runLogDO = invocation.getArgument(0);
            runLogDO.setId(102L);
            return 1;
        });
        when(refundNotifyLogMapper.selectDueFailIds(any(LocalDateTime.class), eq(20), eq("fail")))
                .thenReturn(Arrays.asList(7L, 8L));
        when(refundNotifyLogMapper.selectById(eq(7L))).thenReturn(new BookingRefundNotifyLogDO()
                .setId(7L).setOrderId(2007L).setPayRefundId(92007L).setStatus("success"));
        when(refundNotifyLogMapper.selectById(eq(8L))).thenReturn(new BookingRefundNotifyLogDO()
                .setId(8L).setOrderId(2008L).setPayRefundId(92008L).setStatus("fail"));
        doThrow(exception(BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT))
                .when(bookingOrderService).updateOrderRefunded(eq(2008L), eq(92008L));

        BookingRefundNotifyLogReplayRespVO respVO =
                service.replayDueFailedLogs(20, false, null, "SYSTEM_JOB", "JOB");

        assertEquals(1, respVO.getSkipCount());
        assertEquals(1, respVO.getFailCount());
        verify(refundReplayRunLogMapper).updateRunResult(eq(102L), eq(2), eq(0), eq(1), eq(1),
                eq("partial_fail"), contains("FAIL#id=8"), any(LocalDateTime.class));
    }

    @Test
    void replayDueFailedLogs_shouldKeepStatusWhenDryRun() {
        when(refundReplayRunLogMapper.insert(any(BookingRefundReplayRunLogDO.class))).thenAnswer(invocation -> {
            BookingRefundReplayRunLogDO runLogDO = invocation.getArgument(0);
            runLogDO.setId(103L);
            return 1;
        });
        when(refundNotifyLogMapper.selectDueFailIds(any(LocalDateTime.class), eq(20), eq("fail")))
                .thenReturn(Collections.singletonList(9L));
        when(refundNotifyLogMapper.selectById(eq(9L))).thenReturn(new BookingRefundNotifyLogDO()
                .setId(9L).setOrderId(3009L).setPayRefundId(93009L).setStatus("fail"));
        when(bookingOrderService.getOrder(eq(3009L))).thenReturn(buildPaidOrder(3009L));

        BookingRefundNotifyLogReplayRespVO respVO =
                service.replayDueFailedLogs(20, true, null, "SYSTEM_JOB", "JOB");

        assertEquals(1, respVO.getSuccessCount());
        verify(refundNotifyLogMapper, never()).updateReplaySuccess(any(), any(), any(), any(), any(), any(), any());
        verify(refundReplayRunLogMapper).updateRunResult(eq(103L), eq(1), eq(1), eq(0), eq(0),
                eq("success"), eq(""), any(LocalDateTime.class));
    }

    @Test
    void getReplayRunLogSummary_shouldAggregateTopCodesAndWarnings() {
        BookingRefundReplayRunLogDO runLogDO = new BookingRefundReplayRunLogDO()
                .setId(201L)
                .setRunId("RR201")
                .setStatus("partial_fail")
                .setTriggerSource("JOB")
                .setOperator("SYSTEM_JOB")
                .setDryRun(false)
                .setScannedCount(4)
                .setSuccessCount(1)
                .setSkipCount(1)
                .setFailCount(2);
        when(refundReplayRunLogMapper.selectByRunId(eq("RR201"))).thenReturn(runLogDO);
        when(refundReplayRunDetailMapper.selectByRunId(eq("RR201"))).thenReturn(Arrays.asList(
                new BookingRefundReplayRunDetailDO().setRunId("RR201").setNotifyLogId(1L)
                        .setResultStatus("FAIL").setResultCode("E100").setWarningTag(""),
                new BookingRefundReplayRunDetailDO().setRunId("RR201").setNotifyLogId(2L)
                        .setResultStatus("FAIL").setResultCode("E100").setWarningTag("FOUR_ACCOUNT_REFRESH_WARN"),
                new BookingRefundReplayRunDetailDO().setRunId("RR201").setNotifyLogId(3L)
                        .setResultStatus("SKIP").setResultCode("SKIPPED").setWarningTag(""),
                new BookingRefundReplayRunDetailDO().setRunId("RR201").setNotifyLogId(4L)
                        .setResultStatus("SUCCESS").setResultCode("OK").setWarningTag("FOUR_ACCOUNT_REFRESH_WARN")
        ));

        BookingRefundReplayRunLogSummaryRespVO summary = service.getReplayRunLogSummary("RR201");

        assertEquals("RR201", summary.getRunId());
        assertEquals(2, summary.getWarningCount());
        assertNotNull(summary.getTopFailCodes());
        assertEquals("E100", summary.getTopFailCodes().get(0).getKey());
        assertEquals(2, summary.getTopFailCodes().get(0).getCount());
        assertEquals("FOUR_ACCOUNT_REFRESH_WARN", summary.getTopWarningTags().get(0).getKey());
    }

    @Test
    void syncReplayRunLogTickets_shouldFailOpenAndReturnFailedIds() {
        when(refundReplayRunLogMapper.selectByRunId(eq("RR301"))).thenReturn(
                new BookingRefundReplayRunLogDO().setId(301L).setRunId("RR301"));
        when(refundReplayRunDetailMapper.selectByRunIdAndResultStatus(eq("RR301"), eq("FAIL"))).thenReturn(Arrays.asList(
                new BookingRefundReplayRunDetailDO().setRunId("RR301").setNotifyLogId(11L)
                        .setOrderId(5001L).setResultStatus("FAIL").setResultCode("E101").setResultMsg("fail one"),
                new BookingRefundReplayRunDetailDO().setRunId("RR301").setNotifyLogId(12L)
                        .setOrderId(5002L).setResultStatus("FAIL").setResultCode("E102").setResultMsg("fail two")
        ));
        when(tradeReviewTicketApi.upsertReviewTicket(any()))
                .thenReturn(1001L)
                .thenThrow(new RuntimeException("trade down"));

        BookingRefundReplayRunLogSyncTicketRespVO respVO =
                service.syncReplayRunLogTickets("RR301", true, 99L, "ops-user");

        assertEquals("RR301", respVO.getRunId());
        assertEquals(2, respVO.getAttemptedCount());
        assertEquals(1, respVO.getSuccessCount());
        assertEquals(1, respVO.getFailedCount());
        assertEquals(1, respVO.getFailedIds().size());
        assertEquals(12L, respVO.getFailedIds().get(0));
        verify(tradeReviewTicketApi, times(2)).upsertReviewTicket(any());
    }

    @Test
    void getReplayRunLogPage_shouldNormalizeNewFilters() {
        BookingRefundReplayRunLogPageReqVO reqVO = new BookingRefundReplayRunLogPageReqVO();
        reqVO.setRunId("  RR401 ");
        reqVO.setOperator(" ops-user ");
        reqVO.setTriggerSource("manual");
        reqVO.setStatus("PARTIAL_FAIL");
        reqVO.setMinFailCount(-1);
        when(refundReplayRunLogMapper.selectPage(eq(reqVO)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));

        PageResult<BookingRefundReplayRunLogDO> pageResult = service.getReplayRunLogPage(reqVO);

        assertEquals(0L, pageResult.getTotal());
        assertEquals("RR401", reqVO.getRunId());
        assertEquals("ops-user", reqVO.getOperator());
        assertEquals("MANUAL", reqVO.getTriggerSource());
        assertEquals("partial_fail", reqVO.getStatus());
        assertEquals(0, reqVO.getMinFailCount());
        verify(refundReplayRunLogMapper).selectPage(eq(reqVO));
    }

    @Test
    void replayFailedLogs_shouldReturnFailWhenLogNotFound() {
        when(refundNotifyLogMapper.selectById(eq(999L))).thenReturn(null);

        BookingRefundNotifyLogReplayRespVO respVO =
                service.replayFailedLogs(Collections.singletonList(999L), false, 99L, "ops-user");

        assertEquals(1, respVO.getFailCount());
        assertEquals("FAIL", respVO.getDetails().get(0).getResultStatus());
        verify(refundNotifyLogMapper, never()).updateReplayAudit(any(), any(), any(), any(), any());
    }

    @Test
    void reconcileRefundedOrders_shouldFailOpen() {
        BookingRefundRepairCandidateDO c1 = new BookingRefundRepairCandidateDO();
        c1.setOrderId(2001L);
        c1.setPayRefundId(92001L);
        c1.setMerchantRefundId("2001-refund");
        BookingRefundRepairCandidateDO c2 = new BookingRefundRepairCandidateDO();
        c2.setOrderId(2002L);
        c2.setPayRefundId(92002L);
        c2.setMerchantRefundId("2002-refund");
        when(refundReconcileQueryMapper.selectRepairCandidates(
                eq(PayRefundStatusEnum.SUCCESS.getStatus()),
                eq(BookingOrderStatusEnum.REFUNDED.getStatus()),
                eq(20)))
                .thenReturn(Arrays.asList(c1, c2));
        when(bookingOrderService.getOrder(eq(2001L))).thenReturn(buildRefundedOrder(2001L, 92001L));
        lenient().doThrow(exception(BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT))
                .when(bookingOrderService).updateOrderRefunded(eq(2002L), eq(92002L));

        int repaired = service.reconcileRefundedOrders(20);

        assertEquals(1, repaired);
        ArgumentCaptor<BookingRefundNotifyLogDO> captor = ArgumentCaptor.forClass(BookingRefundNotifyLogDO.class);
        verify(refundNotifyLogMapper, times(2)).insert(captor.capture());
        assertEquals("success", captor.getAllValues().get(0).getStatus());
        assertEquals("fail", captor.getAllValues().get(1).getStatus());
        assertTrue(captor.getAllValues().get(1).getErrorCode().contains(
                String.valueOf(BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT.getCode())));
    }

    @Test
    void reconcileRefundedOrders_shouldReturnZeroWhenNoCandidates() {
        when(refundReconcileQueryMapper.selectRepairCandidates(
                eq(PayRefundStatusEnum.SUCCESS.getStatus()),
                eq(BookingOrderStatusEnum.REFUNDED.getStatus()),
                eq(30)))
                .thenReturn(Collections.emptyList());

        int repaired = service.reconcileRefundedOrders(30);

        assertEquals(0, repaired);
        verify(refundNotifyLogMapper, never()).insert(any(BookingRefundNotifyLogDO.class));
    }

    private BookingOrderDO buildRefundedOrder(Long orderId, Long payRefundId) {
        return BookingOrderDO.builder()
                .id(orderId)
                .status(BookingOrderStatusEnum.REFUNDED.getStatus())
                .payRefundId(payRefundId)
                .refundTime(LocalDateTime.now())
                .build();
    }

    private BookingOrderDO buildPaidOrder(Long orderId) {
        return BookingOrderDO.builder()
                .id(orderId)
                .status(BookingOrderStatusEnum.PAID.getStatus())
                .build();
    }
}
