package cn.iocoder.yudao.module.pay.service.notify;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.pay.dal.dataobject.notify.PayNotifyTaskDO;
import cn.iocoder.yudao.module.pay.dal.mysql.notify.PayNotifyTaskMapper;
import cn.iocoder.yudao.module.pay.enums.notify.PayNotifyStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

class PayNotifyServicePolicyTest extends BaseMockitoUnitTest {

    @InjectMocks
    private PayNotifyServiceImpl notifyService;

    @Mock
    private PayNotifyTaskMapper notifyTaskMapper;

    @Test
    void shouldMarkSuccessWithoutSchedulingNextRetry() {
        PayNotifyTaskDO task = new PayNotifyTaskDO()
                .setId(1L)
                .setNotifyTimes(0);

        Integer status = notifyService.processNotifyResult(task, CommonResult.success("ok"), null);

        ArgumentCaptor<PayNotifyTaskDO> updateCaptor = ArgumentCaptor.forClass(PayNotifyTaskDO.class);
        verify(notifyTaskMapper).updateById(updateCaptor.capture());
        PayNotifyTaskDO updated = updateCaptor.getValue();
        assertEquals(PayNotifyStatusEnum.SUCCESS.getStatus(), status);
        assertEquals(PayNotifyStatusEnum.SUCCESS.getStatus(), updated.getStatus());
        assertEquals(1, updated.getNotifyTimes());
        assertNotNull(updated.getLastExecuteTime());
        assertNull(updated.getNextNotifyTime());
    }

    @Test
    void shouldScheduleRequestSuccessRetryOnBusinessFailure() {
        PayNotifyTaskDO task = new PayNotifyTaskDO()
                .setId(2L)
                .setNotifyTimes(0);
        LocalDateTime before = LocalDateTime.now();

        Integer status = notifyService.processNotifyResult(task, CommonResult.error(400, "BAD_REQUEST"), null);

        ArgumentCaptor<PayNotifyTaskDO> updateCaptor = ArgumentCaptor.forClass(PayNotifyTaskDO.class);
        verify(notifyTaskMapper).updateById(updateCaptor.capture());
        PayNotifyTaskDO updated = updateCaptor.getValue();
        assertEquals(PayNotifyStatusEnum.REQUEST_SUCCESS.getStatus(), status);
        assertEquals(PayNotifyStatusEnum.REQUEST_SUCCESS.getStatus(), updated.getStatus());
        assertEquals(1, updated.getNotifyTimes());
        assertNotNull(updated.getNextNotifyTime());
        long delaySeconds = Duration.between(before, updated.getNextNotifyTime()).getSeconds();
        assertTrue(delaySeconds >= 10 && delaySeconds <= 30,
                "retry delay should be around 15s but was " + delaySeconds + "s");
    }

    @Test
    void shouldScheduleRequestFailureRetryOnException() {
        PayNotifyTaskDO task = new PayNotifyTaskDO()
                .setId(3L)
                .setNotifyTimes(0);
        LocalDateTime before = LocalDateTime.now();

        Integer status = notifyService.processNotifyResult(task, CommonResult.error(500, "ERROR"),
                new RuntimeException("network"));

        ArgumentCaptor<PayNotifyTaskDO> updateCaptor = ArgumentCaptor.forClass(PayNotifyTaskDO.class);
        verify(notifyTaskMapper).updateById(updateCaptor.capture());
        PayNotifyTaskDO updated = updateCaptor.getValue();
        assertEquals(PayNotifyStatusEnum.REQUEST_FAILURE.getStatus(), status);
        assertEquals(PayNotifyStatusEnum.REQUEST_FAILURE.getStatus(), updated.getStatus());
        assertEquals(1, updated.getNotifyTimes());
        assertNotNull(updated.getNextNotifyTime());
        long delaySeconds = Duration.between(before, updated.getNextNotifyTime()).getSeconds();
        assertTrue(delaySeconds >= 10 && delaySeconds <= 30,
                "retry delay should be around 15s but was " + delaySeconds + "s");
    }

    @Test
    void shouldMarkFailureWhenRetryTimesExhausted() {
        PayNotifyTaskDO task = new PayNotifyTaskDO()
                .setId(4L)
                .setNotifyTimes(PayNotifyTaskDO.NOTIFY_FREQUENCY.length - 1);

        Integer status = notifyService.processNotifyResult(task, CommonResult.error(500, "FAIL"), null);

        ArgumentCaptor<PayNotifyTaskDO> updateCaptor = ArgumentCaptor.forClass(PayNotifyTaskDO.class);
        verify(notifyTaskMapper).updateById(updateCaptor.capture());
        PayNotifyTaskDO updated = updateCaptor.getValue();
        assertEquals(PayNotifyStatusEnum.FAILURE.getStatus(), status);
        assertEquals(PayNotifyStatusEnum.FAILURE.getStatus(), updated.getStatus());
        assertEquals(PayNotifyTaskDO.NOTIFY_FREQUENCY.length, updated.getNotifyTimes());
        assertNull(updated.getNextNotifyTime());
    }

}
