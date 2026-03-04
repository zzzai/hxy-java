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

class AfterSaleReviewTicketNotifyDispatchJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AfterSaleReviewTicketNotifyDispatchJob job;

    @Mock
    private AfterSaleReviewTicketService afterSaleReviewTicketService;
    @Mock
    private ConfigApi configApi;

    @Test
    void execute_useDefaultLimitWhenParamInvalid() {
        when(configApi.getConfigValueByKey(anyString())).thenReturn(null);
        when(afterSaleReviewTicketService.dispatchPendingNotifyOutbox(200)).thenReturn(4);

        String result = job.execute("bad");

        assertEquals("分发人工复核工单通知出站 4 条", result);
        verify(afterSaleReviewTicketService).dispatchPendingNotifyOutbox(200);
    }

    @Test
    void execute_capLimitByConfiguredMax() {
        when(configApi.getConfigValueByKey("hxy.trade.review-ticket.sla.notify.job.batch-limit.default")).thenReturn("66");
        when(configApi.getConfigValueByKey("hxy.trade.review-ticket.sla.notify.job.batch-limit.max")).thenReturn("99");
        when(afterSaleReviewTicketService.dispatchPendingNotifyOutbox(99)).thenReturn(2);

        String result = job.execute("500");

        assertEquals("分发人工复核工单通知出站 2 条", result);
        verify(afterSaleReviewTicketService).dispatchPendingNotifyOutbox(99);
    }
}
