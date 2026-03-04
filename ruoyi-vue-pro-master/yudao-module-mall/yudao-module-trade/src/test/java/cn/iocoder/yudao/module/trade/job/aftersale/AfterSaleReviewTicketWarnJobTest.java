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

class AfterSaleReviewTicketWarnJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AfterSaleReviewTicketWarnJob job;

    @Mock
    private AfterSaleReviewTicketService afterSaleReviewTicketService;
    @Mock
    private ConfigApi configApi;

    @Test
    void execute_useDefaultLimitWhenParamInvalid() {
        when(configApi.getConfigValueByKey(anyString())).thenReturn(null);
        when(afterSaleReviewTicketService.warnNearDeadlinePendingTickets(200)).thenReturn(3);

        String result = job.execute("bad");

        assertEquals("预警临近超时人工复核工单 3 条", result);
        verify(afterSaleReviewTicketService).warnNearDeadlinePendingTickets(200);
    }

    @Test
    void execute_capLimitByConfiguredMax() {
        when(configApi.getConfigValueByKey("hxy.trade.review-ticket.sla.warn.job.batch-limit.default")).thenReturn("80");
        when(configApi.getConfigValueByKey("hxy.trade.review-ticket.sla.warn.job.batch-limit.max")).thenReturn("120");
        when(afterSaleReviewTicketService.warnNearDeadlinePendingTickets(120)).thenReturn(1);

        String result = job.execute("999");

        assertEquals("预警临近超时人工复核工单 1 条", result);
        verify(afterSaleReviewTicketService).warnNearDeadlinePendingTickets(120);
    }
}
