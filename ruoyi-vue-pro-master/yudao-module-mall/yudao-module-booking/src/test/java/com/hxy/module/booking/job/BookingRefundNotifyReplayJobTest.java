package com.hxy.module.booking.job;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogReplayRespVO;
import com.hxy.module.booking.service.BookingRefundNotifyLogService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingRefundNotifyReplayJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BookingRefundNotifyReplayJob job;

    @Mock
    private BookingRefundNotifyLogService refundNotifyLogService;

    @Test
    void execute_shouldReplayDueWithParsedLimitAndExposeRunSummary() {
        BookingRefundNotifyLogReplayRespVO respVO = new BookingRefundNotifyLogReplayRespVO();
        respVO.setRunId("RR202603060001");
        respVO.setSuccessCount(3);
        respVO.setSkipCount(1);
        respVO.setFailCount(2);
        when(refundNotifyLogService.replayDueFailedLogs(eq(120), eq(false), isNull(), eq("SYSTEM_JOB"), eq("JOB")))
                .thenReturn(respVO);

        String result = job.execute("limit=120");

        assertTrue(result.contains("runId=RR202603060001"));
        assertTrue(result.contains("scanned=6"));
        assertTrue(result.contains("success=3"));
        assertTrue(result.contains("skip=1"));
        assertTrue(result.contains("fail=2"));
        verify(refundNotifyLogService).replayDueFailedLogs(eq(120), eq(false), isNull(), eq("SYSTEM_JOB"), eq("JOB"));
    }

    @Test
    void execute_shouldFallbackDefaultLimitForInvalidParam() {
        BookingRefundNotifyLogReplayRespVO respVO = new BookingRefundNotifyLogReplayRespVO();
        respVO.setRunId("RR202603060002");
        respVO.setSuccessCount(0);
        respVO.setSkipCount(4);
        respVO.setFailCount(1);
        when(refundNotifyLogService.replayDueFailedLogs(eq(200), eq(false), isNull(), eq("SYSTEM_JOB"), eq("JOB")))
                .thenReturn(respVO);

        String result = job.execute("bad-param");

        assertTrue(result.contains("runId=RR202603060002"));
        assertTrue(result.contains("scanned=5"));
        assertTrue(result.contains("limit=200"));
        verify(refundNotifyLogService).replayDueFailedLogs(eq(200), eq(false), isNull(), eq("SYSTEM_JOB"), eq("JOB"));
    }
}
