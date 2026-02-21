package cn.iocoder.yudao.module.pay.controller.admin.notify;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.PayClient;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.dto.order.PayOrderRespDTO;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.dto.refund.PayRefundRespDTO;
import cn.iocoder.yudao.module.pay.service.app.PayAppService;
import cn.iocoder.yudao.module.pay.service.channel.PayChannelService;
import cn.iocoder.yudao.module.pay.service.notify.PayNotifyService;
import cn.iocoder.yudao.module.pay.service.order.PayOrderService;
import cn.iocoder.yudao.module.pay.service.refund.PayRefundService;
import cn.iocoder.yudao.module.pay.service.transfer.PayTransferService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PayNotifyController} 的单元测试
 */
class PayNotifyControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private PayNotifyController payNotifyController;

    @Mock
    private PayOrderService orderService;
    @Mock
    private PayRefundService refundService;
    @Mock
    private PayTransferService payTransferService;
    @Mock
    private PayNotifyService notifyService;
    @Mock
    private PayAppService appService;
    @Mock
    private PayChannelService channelService;
    @Mock
    private ThreadPoolTaskExecutor notifyThreadPoolTaskExecutor;
    @Mock
    private PayClient<?> payClient;

    @Test
    void shouldAck204AndProcessOrderNotifyAsync() {
        Long channelId = 1L;
        Map<String, String> params = Collections.singletonMap("id", "event_order");
        Map<String, String> headers = Collections.singletonMap("wechatpay-signature", "sign");
        String body = "{\"id\":\"event_order\"}";
        PayOrderRespDTO notify = new PayOrderRespDTO();
        notify.setOutTradeNo("out_trade_no_1");

        when(channelService.getPayClient(channelId)).thenReturn(payClient);
        when(payClient.parseOrderNotify(params, body, headers)).thenReturn(notify);
        when(payClient.getNotifySuccessHttpStatus()).thenReturn(204);
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(notifyThreadPoolTaskExecutor).execute(any(Runnable.class));

        ResponseEntity<String> response = payNotifyController.notifyOrder(channelId, params, body, headers);

        assertEquals(204, response.getStatusCodeValue());
        assertNull(response.getBody());
        verify(orderService).notifyOrder(channelId, notify);
    }

    @Test
    void shouldAck200SuccessAndProcessRefundNotifyAsync() {
        Long channelId = 2L;
        Map<String, String> params = Collections.singletonMap("id", "event_refund");
        Map<String, String> headers = Collections.singletonMap("wechatpay-signature", "sign");
        String body = "{\"id\":\"event_refund\"}";
        PayRefundRespDTO notify = PayRefundRespDTO.waitingOf("channel_refund_no_1", "out_refund_no_1", "raw");

        when(channelService.getPayClient(channelId)).thenReturn(payClient);
        when(payClient.parseRefundNotify(params, body, headers)).thenReturn(notify);
        when(payClient.getNotifySuccessHttpStatus()).thenReturn(200);
        when(payClient.getNotifySuccessBody()).thenReturn("success");
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(notifyThreadPoolTaskExecutor).execute(any(Runnable.class));

        ResponseEntity<String> response = payNotifyController.notifyRefund(channelId, params, body, headers);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("success", response.getBody());
        verify(refundService).notifyRefund(channelId, notify);
    }

    @Test
    void shouldFallbackToSyncWhenNotifyExecutorRejected() {
        Long channelId = 3L;
        Map<String, String> params = Collections.singletonMap("id", "event_order_reject");
        Map<String, String> headers = Collections.singletonMap("wechatpay-signature", "sign");
        String body = "{\"id\":\"event_order_reject\"}";
        PayOrderRespDTO notify = new PayOrderRespDTO();
        notify.setOutTradeNo("out_trade_no_reject");

        when(channelService.getPayClient(channelId)).thenReturn(payClient);
        when(payClient.parseOrderNotify(params, body, headers)).thenReturn(notify);
        when(payClient.getNotifySuccessHttpStatus()).thenReturn(204);
        doAnswer(invocation -> {
            throw new RejectedExecutionException("queue full");
        }).when(notifyThreadPoolTaskExecutor).execute(any(Runnable.class));

        ResponseEntity<String> response = payNotifyController.notifyOrder(channelId, params, body, headers);

        assertEquals(204, response.getStatusCodeValue());
        assertNull(response.getBody());
        verify(orderService).notifyOrder(channelId, notify);
    }

    @Test
    void shouldAckWithinSlaWhenAsyncTaskIsSlow() throws Exception {
        Long channelId = 4L;
        Map<String, String> params = Collections.singletonMap("id", "event_order_sla");
        Map<String, String> headers = Collections.singletonMap("wechatpay-signature", "sign");
        String body = "{\"id\":\"event_order_sla\"}";
        PayOrderRespDTO notify = new PayOrderRespDTO();
        notify.setOutTradeNo("out_trade_no_sla");

        int iterations = Integer.getInteger("pay.notify.sla.iterations", 40);
        long asyncSleepMillis = Long.getLong("pay.notify.sla.async-sleep-ms", 350L);
        long maxP95AckMillis = Long.getLong("pay.notify.sla.max-ack-ms", 180L);

        when(channelService.getPayClient(channelId)).thenReturn(payClient);
        when(payClient.parseOrderNotify(params, body, headers)).thenReturn(notify);
        when(payClient.getNotifySuccessHttpStatus()).thenReturn(204);

        CountDownLatch latch = new CountDownLatch(iterations);
        ExecutorService executorService = Executors.newFixedThreadPool(Math.min(8, iterations));
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            executorService.execute(() -> {
                try {
                    Thread.sleep(asyncSleepMillis);
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
            return null;
        }).when(notifyThreadPoolTaskExecutor).execute(any(Runnable.class));

        List<Long> ackCostMillisList = new ArrayList<>(iterations);
        for (int i = 0; i < iterations; i++) {
            long startNanos = System.nanoTime();
            ResponseEntity<String> response = payNotifyController.notifyOrder(channelId, params, body, headers);
            long ackCostMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            ackCostMillisList.add(ackCostMillis);
            assertEquals(204, response.getStatusCodeValue());
            assertNull(response.getBody());
        }
        assertTrue(latch.await(60, TimeUnit.SECONDS), "asynchronous tasks did not finish in time");
        executorService.shutdownNow();

        Collections.sort(ackCostMillisList);
        int p95Index = (int) Math.ceil(iterations * 0.95) - 1;
        p95Index = Math.max(p95Index, 0);
        long p95AckMillis = ackCostMillisList.get(p95Index);
        assertTrue(p95AckMillis <= maxP95AckMillis,
                "notify ack SLA violated, p95=" + p95AckMillis + "ms > " + maxP95AckMillis + "ms");
    }

}
