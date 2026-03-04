package com.hxy.module.booking.job;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.service.FourAccountReconcileService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FourAccountReconcileDailyJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private FourAccountReconcileDailyJob job;

    @Mock
    private FourAccountReconcileService reconcileService;

    @Test
    void execute_shouldUseYesterdayWhenParamEmpty() {
        when(reconcileService.runReconcile(eq(LocalDate.now().minusDays(1)), eq("JOB_DAILY"), eq("SYSTEM"))).thenReturn(1L);

        String result = job.execute(null);

        assertTrue(result.contains("1"));
        verify(reconcileService).runReconcile(eq(LocalDate.now().minusDays(1)), eq("JOB_DAILY"), eq("SYSTEM"));
    }

    @Test
    void execute_shouldParseDateAndSource() {
        when(reconcileService.runReconcile(eq(LocalDate.of(2026, 3, 4)), eq("MANUAL"), eq("SYSTEM"))).thenReturn(2L);

        String result = job.execute("2026-03-04,MANUAL");

        assertTrue(result.contains("2"));
        verify(reconcileService).runReconcile(eq(LocalDate.of(2026, 3, 4)), eq("MANUAL"), eq("SYSTEM"));
    }

    @Test
    void execute_shouldFallbackOnBadDate() {
        when(reconcileService.runReconcile(eq(LocalDate.now().minusDays(1)), eq("JOB_DAILY"), eq("SYSTEM"))).thenReturn(3L);

        String result = job.execute("bad-date");

        assertTrue(result.contains("3"));
        verify(reconcileService).runReconcile(eq(LocalDate.now().minusDays(1)), eq("JOB_DAILY"), eq("SYSTEM"));
    }
}

