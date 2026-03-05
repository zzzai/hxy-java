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
import com.hxy.module.booking.dal.dataobject.FourAccountReconcileDO;
import com.hxy.module.booking.service.FourAccountReconcileService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals(101L, result.getData().getList().get(0).getRelatedTicketId());
        assertEquals(10, result.getData().getList().get(0).getRelatedTicketStatus());
        assertEquals("P1", result.getData().getList().get(0).getRelatedTicketSeverity());
        verify(reconcileService).getReconcilePage(reqVO);
        verify(tradeReviewTicketApi).listLatestTicketSummaryBySourceBizNos(
                argThat(param -> param != null
                        && Integer.valueOf(40).equals(param.getTicketType())
                        && param.getSourceBizNos() != null
                        && param.getSourceBizNos().contains("FOUR_ACCOUNT_RECONCILE:2026-03-04")));
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
}
