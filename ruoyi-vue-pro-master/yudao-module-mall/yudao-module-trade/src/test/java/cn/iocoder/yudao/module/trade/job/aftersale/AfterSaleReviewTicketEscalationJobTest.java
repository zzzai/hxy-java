package cn.iocoder.yudao.module.trade.job.aftersale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleReviewTicketService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfterSaleReviewTicketEscalationJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AfterSaleReviewTicketEscalationJob job;

    @Mock
    private AfterSaleReviewTicketService afterSaleReviewTicketService;
    @Mock
    private ConfigApi configApi;

    @Test
    void execute_useDefaultLimitWhenParamInvalid() {
        when(configApi.getConfigValueByKey(anyString())).thenReturn(null);
        when(afterSaleReviewTicketService.escalateOverduePendingTickets(200)).thenReturn(2);

        String result = job.execute("abc");

        assertEquals("升级逾期人工复核工单 2 条", result);
        verify(afterSaleReviewTicketService).escalateOverduePendingTickets(200);
    }

    @Test
    void execute_capLimitWhenParamTooLarge() {
        when(configApi.getConfigValueByKey(anyString())).thenReturn(null);
        when(afterSaleReviewTicketService.escalateOverduePendingTickets(1000)).thenReturn(5);

        String result = job.execute("5000");

        assertEquals("升级逾期人工复核工单 5 条", result);
        verify(afterSaleReviewTicketService).escalateOverduePendingTickets(1000);
    }

    @Test
    void execute_useParamLimitWhenValid() {
        when(configApi.getConfigValueByKey(anyString())).thenReturn(null);
        when(afterSaleReviewTicketService.escalateOverduePendingTickets(37)).thenReturn(1);

        String result = job.execute("37");

        assertEquals("升级逾期人工复核工单 1 条", result);
        verify(afterSaleReviewTicketService).escalateOverduePendingTickets(37);
    }

    @Test
    void execute_useConfiguredDefaultAndMaxLimit() {
        when(configApi.getConfigValueByKey("hxy.trade.review-ticket.sla.job.batch-limit.default")).thenReturn("88");
        when(configApi.getConfigValueByKey("hxy.trade.review-ticket.sla.job.batch-limit.max")).thenReturn("123");
        when(afterSaleReviewTicketService.escalateOverduePendingTickets(88)).thenReturn(3);

        String result = job.execute("bad");

        assertEquals("升级逾期人工复核工单 3 条", result);
        verify(afterSaleReviewTicketService).escalateOverduePendingTickets(88);
    }

    @Test
    void execute_capLimitByConfiguredMax() {
        when(configApi.getConfigValueByKey("hxy.trade.review-ticket.sla.job.batch-limit.default")).thenReturn("88");
        when(configApi.getConfigValueByKey("hxy.trade.review-ticket.sla.job.batch-limit.max")).thenReturn("123");
        when(afterSaleReviewTicketService.escalateOverduePendingTickets(123)).thenReturn(4);

        String result = job.execute("9999");

        assertEquals("升级逾期人工复核工单 4 条", result);
        verify(afterSaleReviewTicketService).escalateOverduePendingTickets(123);
    }

}
