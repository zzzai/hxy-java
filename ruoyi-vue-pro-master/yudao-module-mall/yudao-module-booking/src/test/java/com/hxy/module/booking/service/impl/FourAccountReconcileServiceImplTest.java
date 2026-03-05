package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.api.reviewticket.TradeReviewTicketApi;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketResolveReqDTO;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketUpsertReqDTO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcilePageReqVO;
import com.hxy.module.booking.dal.dataobject.FourAccountReconcileDO;
import com.hxy.module.booking.dal.mysql.FourAccountReconcileMapper;
import com.hxy.module.booking.dal.mysql.FourAccountReconcileQueryMapper;
import com.hxy.module.booking.enums.FourAccountReconcileStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FourAccountReconcileServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private FourAccountReconcileServiceImpl service;

    @Mock
    private FourAccountReconcileMapper reconcileMapper;
    @Mock
    private FourAccountReconcileQueryMapper queryMapper;
    @Mock
    private TradeReviewTicketApi tradeReviewTicketApi;

    @Test
    void runReconcile_shouldCreatePassSnapshotWhenNoIssue() {
        LocalDate bizDate = LocalDate.of(2026, 3, 4);
        when(queryMapper.selectTradeNetAmount(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(10000);
        when(queryMapper.selectFulfillmentAmount(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(9800);
        when(queryMapper.selectCommissionAmount(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(2200);
        when(queryMapper.selectSplitAmount(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(500);
        when(reconcileMapper.selectByBizDate(bizDate)).thenReturn(null);
        when(reconcileMapper.insert(any(FourAccountReconcileDO.class))).thenAnswer(invocation -> {
            FourAccountReconcileDO data = invocation.getArgument(0);
            data.setId(1L);
            return 1;
        });

        Long id = service.runReconcile(bizDate, "MANUAL", "1001");

        assertEquals(1L, id);
        ArgumentCaptor<FourAccountReconcileDO> captor = ArgumentCaptor.forClass(FourAccountReconcileDO.class);
        verify(reconcileMapper).insert(captor.capture());
        assertEquals(bizDate, captor.getValue().getBizDate());
        assertEquals(FourAccountReconcileStatusEnum.PASS.getStatus(), captor.getValue().getStatus());
        assertEquals(0, captor.getValue().getIssueCount());
        assertEquals(200, captor.getValue().getTradeMinusFulfillment());
        assertEquals(7300, captor.getValue().getTradeMinusCommissionSplit());
        verify(tradeReviewTicketApi, never()).upsertReviewTicket(any(TradeReviewTicketUpsertReqDTO.class));
        ArgumentCaptor<TradeReviewTicketResolveReqDTO> resolveCaptor =
                ArgumentCaptor.forClass(TradeReviewTicketResolveReqDTO.class);
        verify(tradeReviewTicketApi).resolveReviewTicketBySourceBizNo(resolveCaptor.capture());
        assertEquals("FOUR_ACCOUNT_RECONCILE:2026-03-04", resolveCaptor.getValue().getSourceBizNo());
        assertEquals("FOUR_ACCOUNT_RECONCILE_PASS", resolveCaptor.getValue().getResolveActionCode());
    }

    @Test
    void runReconcile_shouldMarkWarnWhenCoreRulesBroken() {
        LocalDate bizDate = LocalDate.of(2026, 3, 3);
        when(queryMapper.selectTradeNetAmount(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(10000);
        when(queryMapper.selectFulfillmentAmount(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(12000);
        when(queryMapper.selectCommissionAmount(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(13000);
        when(queryMapper.selectSplitAmount(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(11000);
        when(reconcileMapper.selectByBizDate(bizDate)).thenReturn(null);
        when(reconcileMapper.insert(any(FourAccountReconcileDO.class))).thenAnswer(invocation -> {
            FourAccountReconcileDO data = invocation.getArgument(0);
            data.setId(2L);
            return 1;
        });

        Long id = service.runReconcile(bizDate, "JOB_DAILY", "SYSTEM");

        assertEquals(2L, id);
        ArgumentCaptor<FourAccountReconcileDO> captor = ArgumentCaptor.forClass(FourAccountReconcileDO.class);
        verify(reconcileMapper).insert(captor.capture());
        assertEquals(FourAccountReconcileStatusEnum.WARN.getStatus(), captor.getValue().getStatus());
        assertEquals(4, captor.getValue().getIssueCount());
        assertTrue(captor.getValue().getIssueCodes().contains("FULFILLMENT_GT_TRADE"));
        assertTrue(captor.getValue().getIssueCodes().contains("COMMISSION_GT_FULFILLMENT"));
        assertTrue(captor.getValue().getIssueCodes().contains("SPLIT_GT_TRADE"));
        assertTrue(captor.getValue().getIssueCodes().contains("COMMISSION_SPLIT_GT_TRADE"));
        ArgumentCaptor<TradeReviewTicketUpsertReqDTO> ticketCaptor =
                ArgumentCaptor.forClass(TradeReviewTicketUpsertReqDTO.class);
        verify(tradeReviewTicketApi).upsertReviewTicket(ticketCaptor.capture());
        assertEquals("FOUR_ACCOUNT_RECONCILE:2026-03-03", ticketCaptor.getValue().getSourceBizNo());
        assertEquals(40, ticketCaptor.getValue().getTicketType());
        assertEquals("FOUR_ACCOUNT_RECONCILE_WARN", ticketCaptor.getValue().getRuleCode());
        assertEquals("P1", ticketCaptor.getValue().getSeverity());
        verify(tradeReviewTicketApi, never()).resolveReviewTicketBySourceBizNo(any(TradeReviewTicketResolveReqDTO.class));
    }

    @Test
    void getReconcilePage_shouldDelegateMapper() {
        FourAccountReconcilePageReqVO reqVO = new FourAccountReconcilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        when(reconcileMapper.selectPage(reqVO))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));

        PageResult<FourAccountReconcileDO> page = service.getReconcilePage(reqVO);

        assertEquals(0L, page.getTotal());
        verify(reconcileMapper).selectPage(eq(reqVO));
    }
}
