package com.hxy.module.booking.job;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.service.TechnicianCommissionSettlementService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechnicianCommissionSettlementNotifyDispatchJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TechnicianCommissionSettlementNotifyDispatchJob job;

    @Mock
    private TechnicianCommissionSettlementService settlementService;

    @Test
    void shouldExecuteWithDefaultParam() {
        when(settlementService.dispatchPendingNotifyOutbox(200)).thenReturn(3);

        String result = job.execute(null);

        assertTrue(result.contains("3"));
        verify(settlementService).dispatchPendingNotifyOutbox(200);
    }

    @Test
    void shouldExecuteWithCustomParam() {
        when(settlementService.dispatchPendingNotifyOutbox(50)).thenReturn(1);

        String result = job.execute("50");

        assertTrue(result.contains("1"));
        verify(settlementService).dispatchPendingNotifyOutbox(50);
    }
}

