package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.api.reviewticket.TradeReviewTicketApi;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketSummaryQueryReqDTO;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketSummaryRespDTO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcilePageReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileRespVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileRunReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundAuditSummaryReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundAuditSummaryRespVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundCommissionAuditPageReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundCommissionAuditRespVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundCommissionAuditSyncReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundCommissionAuditSyncRespVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileSummaryReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileSummaryRespVO;
import com.hxy.module.booking.dal.dataobject.FourAccountReconcileDO;
import com.hxy.module.booking.service.FourAccountReconcileService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FourAccountReconcileControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private FourAccountReconcileController controller;

    @Mock
    private FourAccountReconcileService reconcileService;
    @Mock
    private TradeReviewTicketApi tradeReviewTicketApi;

    @Test
    void page_shouldReturnData() {
        FourAccountReconcileDO row = FourAccountReconcileDO.builder()
                .id(1L)
                .bizDate(LocalDate.of(2026, 3, 4))
                .tradeAmount(10000)
                .fulfillmentAmount(9800)
                .commissionAmount(2200)
                .splitAmount(500)
                .status(10)
                .issueCount(0)
                .refundEvidenceJson("{\"sample\":1}")
                .build();
        FourAccountReconcilePageReqVO reqVO = new FourAccountReconcilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        when(reconcileService.getReconcilePage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));
        when(tradeReviewTicketApi.listLatestTicketSummaryBySourceBizNos(any(TradeReviewTicketSummaryQueryReqDTO.class)))
                .thenReturn(Collections.singletonList(new TradeReviewTicketSummaryRespDTO()
                        .setId(101L)
                        .setTicketType(40)
                        .setSourceBizNo("FOUR_ACCOUNT_RECONCILE:2026-03-04")
                        .setStatus(10)
                        .setSeverity("P1")));

        CommonResult<PageResult<FourAccountReconcileRespVO>> result = controller.page(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1L, result.getData().getList().get(0).getId());
        assertEquals("FOUR_ACCOUNT_RECONCILE:2026-03-04", result.getData().getList().get(0).getSourceBizNo());
        assertEquals(101L, result.getData().getList().get(0).getRelatedTicketId());
        assertEquals(10, result.getData().getList().get(0).getRelatedTicketStatus());
        assertEquals("P1", result.getData().getList().get(0).getRelatedTicketSeverity());
        assertEquals(false, result.getData().getList().get(0).getRefundEvidenceJsonParseError());
        verify(reconcileService).getReconcilePage(reqVO);
        verify(tradeReviewTicketApi).listLatestTicketSummaryBySourceBizNos(
                argThat(param -> param != null
                        && Integer.valueOf(40).equals(param.getTicketType())
                        && param.getSourceBizNos() != null
                        && param.getSourceBizNos().contains("FOUR_ACCOUNT_RECONCILE:2026-03-04")));
    }

    @Test
    void page_shouldDegradeWhenTradeTicketApiThrows() {
        FourAccountReconcileDO row = FourAccountReconcileDO.builder()
                .id(2L)
                .bizDate(LocalDate.of(2026, 3, 5))
                .tradeAmount(10000)
                .status(10)
                .issueCount(0)
                .build();
        FourAccountReconcilePageReqVO reqVO = new FourAccountReconcilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        when(reconcileService.getReconcilePage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));
        when(tradeReviewTicketApi.listLatestTicketSummaryBySourceBizNos(any(TradeReviewTicketSummaryQueryReqDTO.class)))
                .thenThrow(new RuntimeException("trade-api timeout"));

        CommonResult<PageResult<FourAccountReconcileRespVO>> result = controller.page(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(2L, result.getData().getList().get(0).getId());
        assertEquals("FOUR_ACCOUNT_RECONCILE:2026-03-05", result.getData().getList().get(0).getSourceBizNo());
        assertNull(result.getData().getList().get(0).getRelatedTicketId());
        assertNull(result.getData().getList().get(0).getRelatedTicketStatus());
        assertNull(result.getData().getList().get(0).getRelatedTicketSeverity());
        verify(tradeReviewTicketApi).listLatestTicketSummaryBySourceBizNos(any(TradeReviewTicketSummaryQueryReqDTO.class));
    }

    @Test
    void get_shouldReturnData() {
        FourAccountReconcileDO row = FourAccountReconcileDO.builder()
                .id(3L)
                .bizDate(LocalDate.of(2026, 3, 6))
                .tradeAmount(12000)
                .fulfillmentAmount(10000)
                .status(20)
                .issueCount(1)
                .build();
        when(reconcileService.getReconcile(3L)).thenReturn(row);
        when(tradeReviewTicketApi.listLatestTicketSummaryBySourceBizNos(any(TradeReviewTicketSummaryQueryReqDTO.class)))
                .thenReturn(Collections.singletonList(new TradeReviewTicketSummaryRespDTO()
                        .setId(102L)
                        .setTicketType(40)
                        .setSourceBizNo("FOUR_ACCOUNT_RECONCILE:2026-03-06")
                        .setStatus(10)
                        .setSeverity("P0")));

        CommonResult<FourAccountReconcileRespVO> result = controller.get(3L);

        assertTrue(result.isSuccess());
        assertEquals(3L, result.getData().getId());
        assertEquals("FOUR_ACCOUNT_RECONCILE:2026-03-06", result.getData().getSourceBizNo());
        assertEquals(102L, result.getData().getRelatedTicketId());
        assertEquals(10, result.getData().getRelatedTicketStatus());
        assertEquals("P0", result.getData().getRelatedTicketSeverity());
        verify(reconcileService).getReconcile(3L);
        verify(tradeReviewTicketApi).listLatestTicketSummaryBySourceBizNos(
                argThat(param -> param != null
                        && Integer.valueOf(40).equals(param.getTicketType())
                        && param.getSourceBizNos() != null
                        && param.getSourceBizNos().contains("FOUR_ACCOUNT_RECONCILE:2026-03-06")));
    }

    @Test
    void get_shouldDegradeWhenRefundEvidenceJsonInvalid() {
        FourAccountReconcileDO row = FourAccountReconcileDO.builder()
                .id(4L)
                .bizDate(LocalDate.of(2026, 3, 7))
                .status(20)
                .refundEvidenceJson("{bad-json")
                .build();
        when(reconcileService.getReconcile(4L)).thenReturn(row);
        when(tradeReviewTicketApi.listLatestTicketSummaryBySourceBizNos(any(TradeReviewTicketSummaryQueryReqDTO.class)))
                .thenReturn(Collections.emptyList());

        CommonResult<FourAccountReconcileRespVO> result = controller.get(4L);

        assertTrue(result.isSuccess());
        assertEquals(4L, result.getData().getId());
        assertEquals(true, result.getData().getRefundEvidenceJsonParseError());
        assertEquals("{bad-json", result.getData().getRefundEvidenceJson());
    }

    @Test
    void run_shouldTriggerService() {
        FourAccountReconcileRunReqVO reqVO = new FourAccountReconcileRunReqVO();
        reqVO.setBizDate(LocalDate.of(2026, 3, 4));
        reqVO.setSource("MANUAL");
        when(reconcileService.runReconcile(eq(LocalDate.of(2026, 3, 4)), eq("MANUAL"), isNull())).thenReturn(9L);

        CommonResult<Long> result = controller.run(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(9L, result.getData());
        verify(reconcileService).runReconcile(eq(LocalDate.of(2026, 3, 4)), eq("MANUAL"), isNull());
    }

    @Test
    void summary_shouldDelegateService() {
        FourAccountReconcileSummaryReqVO reqVO = new FourAccountReconcileSummaryReqVO();
        reqVO.setStatus(20);
        reqVO.setRelatedTicketLinked(true);
        FourAccountReconcileSummaryRespVO summary = new FourAccountReconcileSummaryRespVO();
        summary.setTotalCount(8L);
        summary.setWarnCount(3L);
        summary.setUnresolvedTicketCount(2L);
        when(reconcileService.getReconcileSummary(any(FourAccountReconcileSummaryReqVO.class))).thenReturn(summary);

        CommonResult<FourAccountReconcileSummaryRespVO> result = controller.summary(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(8L, result.getData().getTotalCount());
        assertEquals(3L, result.getData().getWarnCount());
        assertEquals(2L, result.getData().getUnresolvedTicketCount());
        verify(reconcileService).getReconcileSummary(eq(reqVO));
    }

    @Test
    void refundAuditSummary_shouldDelegateService() {
        FourAccountRefundAuditSummaryReqVO reqVO = new FourAccountRefundAuditSummaryReqVO();
        reqVO.setRefundAuditStatus("WARN");
        FourAccountRefundAuditSummaryRespVO summary = new FourAccountRefundAuditSummaryRespVO();
        summary.setTotalCount(5L);
        summary.setDifferenceAmountSum(3600L);
        summary.setUnresolvedTicketCount(1L);
        when(reconcileService.getRefundAuditSummary(any(FourAccountRefundAuditSummaryReqVO.class))).thenReturn(summary);

        CommonResult<FourAccountRefundAuditSummaryRespVO> result = controller.refundAuditSummary(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(5L, result.getData().getTotalCount());
        assertEquals(3600L, result.getData().getDifferenceAmountSum());
        assertEquals(1L, result.getData().getUnresolvedTicketCount());
        verify(reconcileService).getRefundAuditSummary(eq(reqVO));
    }

    @Test
    void refundCommissionAuditPage_shouldDelegateService() {
        FourAccountRefundCommissionAuditPageReqVO reqVO = new FourAccountRefundCommissionAuditPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        FourAccountRefundCommissionAuditRespVO row = new FourAccountRefundCommissionAuditRespVO();
        row.setOrderId(99L);
        row.setMismatchType("REFUND_WITHOUT_REVERSAL");
        when(reconcileService.getRefundCommissionAuditPage(eq(reqVO)))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));

        CommonResult<PageResult<FourAccountRefundCommissionAuditRespVO>> result =
                controller.refundCommissionAuditPage(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(99L, result.getData().getList().get(0).getOrderId());
        assertEquals("REFUND_WITHOUT_REVERSAL", result.getData().getList().get(0).getMismatchType());
        verify(reconcileService).getRefundCommissionAuditPage(eq(reqVO));
    }

    @Test
    void syncRefundCommissionAuditTickets_shouldDelegateService() {
        FourAccountRefundCommissionAuditSyncReqVO reqVO = new FourAccountRefundCommissionAuditSyncReqVO();
        reqVO.setLimit(20);
        FourAccountRefundCommissionAuditSyncRespVO respVO = new FourAccountRefundCommissionAuditSyncRespVO();
        respVO.setTotalMismatchCount(12);
        respVO.setAttemptedCount(12);
        respVO.setSuccessCount(11);
        respVO.setFailedCount(1);
        when(reconcileService.syncRefundCommissionAuditTickets(eq(reqVO))).thenReturn(respVO);

        CommonResult<FourAccountRefundCommissionAuditSyncRespVO> result =
                controller.syncRefundCommissionAuditTickets(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(12, result.getData().getTotalMismatchCount());
        assertEquals(11, result.getData().getSuccessCount());
        verify(reconcileService).syncRefundCommissionAuditTickets(eq(reqVO));
    }
}
