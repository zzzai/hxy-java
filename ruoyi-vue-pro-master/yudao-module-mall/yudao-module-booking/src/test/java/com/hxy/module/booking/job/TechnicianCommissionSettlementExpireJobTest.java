package com.hxy.module.booking.job;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.service.TechnicianCommissionSettlementService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechnicianCommissionSettlementExpireJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TechnicianCommissionSettlementExpireJob job;

    @Mock
    private TechnicianCommissionSettlementService settlementService;

    @Test
    void shouldExecuteWithDefaultParam() {
        when(settlementService.expireOverduePending(200)).thenReturn(3);

        String result = job.execute(null);

        assertTrue(result.contains("3"));
        verify(settlementService).expireOverduePending(200);
    }

    @Test
    void shouldExecuteWithCustomParam() {
        when(settlementService.expireOverduePending(50)).thenReturn(1);

        String result = job.execute("50");

        assertTrue(result.contains("1"));
        verify(settlementService).expireOverduePending(50);
    }

    @Test
    void shouldFallbackToDefaultWhenParamInvalid() {
        when(settlementService.expireOverduePending(200)).thenReturn(2);

        String result = job.execute("abc");

        assertTrue(result.contains("2"));
        verify(settlementService).expireOverduePending(200);
    }

    @Test
    void shouldClampTooLargeLimit() {
        when(settlementService.expireOverduePending(1000)).thenReturn(4);

        String result = job.execute("99999");

        assertTrue(result.contains("4"));
        verify(settlementService).expireOverduePending(1000);
    }
}

