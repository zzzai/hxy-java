package com.hxy.module.booking.job;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.service.TechnicianCommissionSettlementService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechnicianCommissionSettlementSlaWarnJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TechnicianCommissionSettlementSlaWarnJob job;

    @Mock
    private TechnicianCommissionSettlementService settlementService;

    @Test
    void shouldExecuteWithDefaultParam() {
        when(settlementService.warnNearDeadlinePending(30, 200)).thenReturn(2);
        when(settlementService.escalateOverduePendingToP0(30, 200)).thenReturn(1);

        String result = job.execute(null);

        assertTrue(result.contains("2"));
        assertTrue(result.contains("1"));
        verify(settlementService).warnNearDeadlinePending(30, 200);
        verify(settlementService).escalateOverduePendingToP0(30, 200);
    }

    @Test
    void shouldExecuteWithCustomParam() {
        when(settlementService.warnNearDeadlinePending(15, 50)).thenReturn(1);
        when(settlementService.escalateOverduePendingToP0(45, 50)).thenReturn(2);

        String result = job.execute("15,45,50");

        assertTrue(result.contains("1"));
        assertTrue(result.contains("2"));
        verify(settlementService).warnNearDeadlinePending(15, 50);
        verify(settlementService).escalateOverduePendingToP0(45, 50);
    }
}
