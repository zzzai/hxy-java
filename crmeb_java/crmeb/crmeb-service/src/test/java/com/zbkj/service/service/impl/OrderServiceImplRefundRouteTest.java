package com.zbkj.service.service.impl;

import com.zbkj.common.constants.Constants;
import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.request.OrderRefundApplyRequest;
import com.zbkj.service.service.SystemConfigService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Date;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplRefundRouteTest {

    @Mock
    private SystemConfigService systemConfigService;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl();
        ReflectionTestUtils.setField(service, "systemConfigService", systemConfigService);
    }

    @Test
    void evaluateRefundRouteShouldManualWhenUnshippedAutoRefundDisabled() {
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_REFUND_AUTO_ENABLE)).thenReturn("1");
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_REFUND_AUTO_ALLOW_UNSHIPPED)).thenReturn("0");

        Object decision = ReflectionTestUtils.invokeMethod(service, "evaluateRefundRoute", buildPaidUnshippedOrder(), new OrderRefundApplyRequest());
        Assertions.assertNotNull(decision);
        Assertions.assertEquals(Boolean.FALSE, ReflectionTestUtils.getField(decision, "autoPass"));
        String reason = (String) ReflectionTestUtils.getField(decision, "reason");
        Assertions.assertTrue(reason != null && reason.contains("未发货订单"));
    }

    @Test
    void evaluateRefundRouteShouldAutoWhenUnshippedAutoRefundEnabled() {
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_REFUND_AUTO_ENABLE)).thenReturn("1");
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_REFUND_AUTO_ALLOW_UNSHIPPED)).thenReturn("1");
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_REFUND_AUTO_MAX_AMOUNT)).thenReturn("200.00");
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_REFUND_AUTO_MAX_MINUTES)).thenReturn("120");
        when(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_REFUND_MANUAL_KEYWORDS)).thenReturn("");

        Object decision = ReflectionTestUtils.invokeMethod(service, "evaluateRefundRoute", buildPaidUnshippedOrder(), new OrderRefundApplyRequest());
        Assertions.assertNotNull(decision);
        Assertions.assertEquals(Boolean.TRUE, ReflectionTestUtils.getField(decision, "autoPass"));
    }

    private StoreOrder buildPaidUnshippedOrder() {
        StoreOrder order = new StoreOrder();
        order.setOrderId("order_refund_route_001");
        order.setPayType(Constants.PAY_TYPE_WE_CHAT);
        order.setPayPrice(new BigDecimal("0.01"));
        order.setStatus(Constants.ORDER_STATUS_INT_PAID);
        order.setOutTradeNo("wx_refund_route_001");
        order.setPayTime(new Date());
        return order;
    }
}
