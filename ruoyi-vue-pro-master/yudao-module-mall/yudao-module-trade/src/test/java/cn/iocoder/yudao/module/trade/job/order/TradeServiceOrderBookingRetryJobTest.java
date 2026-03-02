package cn.iocoder.yudao.module.trade.job.order;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.service.order.TradeServiceOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeServiceOrderBookingRetryJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TradeServiceOrderBookingRetryJob job;

    @Mock
    private TradeServiceOrderService tradeServiceOrderService;

    @Test
    void execute_useDefaultLimitWhenParamInvalid() {
        when(tradeServiceOrderService.retryCreateBookingPlaceholder(200)).thenReturn(2);

        String result = job.execute("abc");

        assertEquals("重试服务履约单预约占位成功 2 条", result);
        verify(tradeServiceOrderService).retryCreateBookingPlaceholder(200);
    }

    @Test
    void execute_capLimitWhenParamTooLarge() {
        when(tradeServiceOrderService.retryCreateBookingPlaceholder(1000)).thenReturn(5);

        String result = job.execute("5000");

        assertEquals("重试服务履约单预约占位成功 5 条", result);
        verify(tradeServiceOrderService).retryCreateBookingPlaceholder(1000);
    }

    @Test
    void execute_useParamLimitWhenValid() {
        when(tradeServiceOrderService.retryCreateBookingPlaceholder(37)).thenReturn(1);

        String result = job.execute("37");

        assertEquals("重试服务履约单预约占位成功 1 条", result);
        verify(tradeServiceOrderService).retryCreateBookingPlaceholder(37);
    }

}
