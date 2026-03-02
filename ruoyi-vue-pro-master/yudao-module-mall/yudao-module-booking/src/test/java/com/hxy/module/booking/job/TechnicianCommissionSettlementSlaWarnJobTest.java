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

    @Test
    void shouldFallbackToDefaultWhenParamInvalid() {
        when(settlementService.warnNearDeadlinePending(30, 200)).thenReturn(0);
        when(settlementService.escalateOverduePendingToP0(30, 200)).thenReturn(0);

        String result = job.execute("lead,delay,limit");

        assertTrue(result.contains("0"));
        verify(settlementService).warnNearDeadlinePending(30, 200);
        verify(settlementService).escalateOverduePendingToP0(30, 200);
    }

    @Test
    void shouldClampTooLargeParams() {
        when(settlementService.warnNearDeadlinePending(1440, 1000)).thenReturn(3);
        when(settlementService.escalateOverduePendingToP0(1440, 1000)).thenReturn(4);

        String result = job.execute("9999,8888,99999");

        assertTrue(result.contains("3"));
        assertTrue(result.contains("4"));
        verify(settlementService).warnNearDeadlinePending(1440, 1000);
        verify(settlementService).escalateOverduePendingToP0(1440, 1000);
    }

    @Test
    void shouldFallbackToDefaultWhenParamNonPositive() {
        when(settlementService.warnNearDeadlinePending(30, 200)).thenReturn(5);
        when(settlementService.escalateOverduePendingToP0(30, 200)).thenReturn(6);

        String result = job.execute("0,0,0");

        assertTrue(result.contains("5"));
        assertTrue(result.contains("6"));
        verify(settlementService).warnNearDeadlinePending(30, 200);
        verify(settlementService).escalateOverduePendingToP0(30, 200);
    }
}
