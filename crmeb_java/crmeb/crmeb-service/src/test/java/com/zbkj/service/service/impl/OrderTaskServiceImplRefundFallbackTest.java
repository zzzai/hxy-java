package com.zbkj.service.service.impl;

import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.vo.MyRecord;
import com.zbkj.common.vo.WeChatPayChannelConfig;
import com.zbkj.service.service.WechatNewService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTaskServiceImplRefundFallbackTest {

    @Mock
    private WechatNewService wechatNewService;

    private OrderTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderTaskServiceImpl();
        ReflectionTestUtils.setField(service, "wechatNewService", wechatNewService);
    }

    @Test
    void queryRefundRecordWithFallbackShouldReturnPrimaryResult() {
        StoreOrder order = new StoreOrder();
        order.setOrderId("order_refund_001");
        order.setOutTradeNo("wx_refund_001");
        WeChatPayChannelConfig config = new WeChatPayChannelConfig();
        MyRecord expected = new MyRecord().set("status", "SUCCESS");
        when(wechatNewService.payRefundQuery("order_refund_001", config)).thenReturn(expected);

        MyRecord actual = ReflectionTestUtils.invokeMethod(service, "queryRefundRecordWithFallback", order, config);

        Assertions.assertSame(expected, actual);
        verify(wechatNewService).payRefundQuery("order_refund_001", config);
        verify(wechatNewService, never()).payRefundQuery("wx_refund_001", config);
    }

    @Test
    void queryRefundRecordWithFallbackShouldRetryWithOutTradeNoWhenPrimaryNotFound() {
        StoreOrder order = new StoreOrder();
        order.setOrderId("order_refund_002");
        order.setOutTradeNo("wx_refund_002");
        WeChatPayChannelConfig config = new WeChatPayChannelConfig();
        MyRecord expected = new MyRecord().set("status", "PROCESSING");

        when(wechatNewService.payRefundQuery("order_refund_002", config))
                .thenThrow(new RuntimeException("ORDERNOTEXIST"));
        when(wechatNewService.payRefundQuery("wx_refund_002", config)).thenReturn(expected);

        MyRecord actual = ReflectionTestUtils.invokeMethod(service, "queryRefundRecordWithFallback", order, config);

        Assertions.assertSame(expected, actual);
        verify(wechatNewService).payRefundQuery("order_refund_002", config);
        verify(wechatNewService).payRefundQuery("wx_refund_002", config);
    }

    @Test
    void queryRefundRecordWithFallbackShouldPropagateErrorWhenFallbackUnavailable() {
        StoreOrder order = new StoreOrder();
        order.setOutTradeNo("wx_refund_003");
        WeChatPayChannelConfig config = new WeChatPayChannelConfig();

        when(wechatNewService.payRefundQuery("wx_refund_003", config))
                .thenThrow(new RuntimeException("RESOURCE_NOT_EXISTS"));

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "queryRefundRecordWithFallback", order, config));
        Assertions.assertTrue(exception.getMessage().contains("RESOURCE_NOT_EXISTS"));
        verify(wechatNewService).payRefundQuery("wx_refund_003", config);
    }
}
