package cn.iocoder.yudao.module.trade.api.reviewticket;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketResolveReqDTO;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketSummaryQueryReqDTO;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketSummaryRespDTO;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketUpsertReqDTO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketDO;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleReviewTicketService;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleReviewTicketCreateReqBO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeReviewTicketApiImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TradeReviewTicketApiImpl api;

    @Mock
    private AfterSaleReviewTicketService afterSaleReviewTicketService;

    @Test
    void shouldMapDtoAndDelegateService() {
        TradeReviewTicketUpsertReqDTO reqDTO = new TradeReviewTicketUpsertReqDTO()
                .setTicketType(40)
                .setSourceBizNo("FOUR_ACCOUNT_RECONCILE:2026-03-05")
                .setRuleCode("FOUR_ACCOUNT_RECONCILE_WARN")
                .setDecisionReason("四账差异告警")
                .setSeverity("P1")
                .setActionCode("FOUR_ACCOUNT_RECONCILE_WARN");
        when(afterSaleReviewTicketService.upsertReviewTicketBySourceBizNo(
                org.mockito.ArgumentMatchers.any(AfterSaleReviewTicketCreateReqBO.class),
                eq("FOUR_ACCOUNT_RECONCILE_WARN"))).thenReturn(100L);

        Long id = api.upsertReviewTicket(reqDTO);

        assertEquals(100L, id);
        ArgumentCaptor<AfterSaleReviewTicketCreateReqBO> reqCaptor =
                ArgumentCaptor.forClass(AfterSaleReviewTicketCreateReqBO.class);
        verify(afterSaleReviewTicketService).upsertReviewTicketBySourceBizNo(
                reqCaptor.capture(), eq("FOUR_ACCOUNT_RECONCILE_WARN"));
        assertEquals(40, reqCaptor.getValue().getTicketType());
        assertEquals("FOUR_ACCOUNT_RECONCILE:2026-03-05", reqCaptor.getValue().getSourceBizNo());
        assertEquals("FOUR_ACCOUNT_RECONCILE_WARN", reqCaptor.getValue().getRuleCode());
    }

    @Test
    void shouldResolveBySourceAndDelegateService() {
        TradeReviewTicketResolveReqDTO reqDTO = new TradeReviewTicketResolveReqDTO()
                .setTicketType(40)
                .setSourceBizNo("FOUR_ACCOUNT_RECONCILE:2026-03-05")
                .setResolveActionCode("FOUR_ACCOUNT_RECONCILE_PASS")
                .setResolveBizNo("FOUR_ACCOUNT_RECONCILE:2026-03-05")
                .setResolveRemark("auto resolve");
        when(afterSaleReviewTicketService.resolveReviewTicketBySourceBizNo(
                eq(40), eq("FOUR_ACCOUNT_RECONCILE:2026-03-05"),
                eq(null), eq(null), eq("FOUR_ACCOUNT_RECONCILE_PASS"),
                eq("FOUR_ACCOUNT_RECONCILE:2026-03-05"), eq("auto resolve"))).thenReturn(true);

        boolean resolved = api.resolveReviewTicketBySourceBizNo(reqDTO);

        assertEquals(true, resolved);
        verify(afterSaleReviewTicketService).resolveReviewTicketBySourceBizNo(
                eq(40), eq("FOUR_ACCOUNT_RECONCILE:2026-03-05"),
                eq(null), eq(null), eq("FOUR_ACCOUNT_RECONCILE_PASS"),
                eq("FOUR_ACCOUNT_RECONCILE:2026-03-05"), eq("auto resolve"));
    }

    @Test
    void shouldListLatestTicketSummaryBySourceBizNos() {
        TradeReviewTicketSummaryQueryReqDTO reqDTO = new TradeReviewTicketSummaryQueryReqDTO()
                .setTicketType(40)
                .setSourceBizNos(Collections.singletonList("FOUR_ACCOUNT_RECONCILE:2026-03-05"));
        AfterSaleReviewTicketDO ticket = new AfterSaleReviewTicketDO();
        ticket.setId(9001L);
        ticket.setTicketType(40);
        ticket.setSourceBizNo("FOUR_ACCOUNT_RECONCILE:2026-03-05");
        ticket.setStatus(10);
        ticket.setSeverity("P1");
        when(afterSaleReviewTicketService.listLatestByTicketTypeAndSourceBizNos(
                eq(40), eq(Collections.singletonList("FOUR_ACCOUNT_RECONCILE:2026-03-05"))))
                .thenReturn(Collections.singletonList(ticket));

        List<TradeReviewTicketSummaryRespDTO> respList = api.listLatestTicketSummaryBySourceBizNos(reqDTO);

        assertEquals(1, respList.size());
        assertEquals(9001L, respList.get(0).getId());
        assertEquals(40, respList.get(0).getTicketType());
        assertEquals("FOUR_ACCOUNT_RECONCILE:2026-03-05", respList.get(0).getSourceBizNo());
        assertEquals(10, respList.get(0).getStatus());
        assertEquals("P1", respList.get(0).getSeverity());
        verify(afterSaleReviewTicketService).listLatestByTicketTypeAndSourceBizNos(
                eq(40), eq(Collections.singletonList("FOUR_ACCOUNT_RECONCILE:2026-03-05")));
    }
}
